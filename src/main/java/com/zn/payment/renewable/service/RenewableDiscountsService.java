package com.zn.payment.renewable.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.google.gson.JsonSyntaxException;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import com.zn.payment.dto.CreateDiscountSessionRequest;
import com.zn.payment.renewable.entity.RenewableDiscounts;
import com.zn.payment.renewable.repository.RenewableDiscountsRepository;

import jakarta.servlet.http.HttpServletRequest;


@Service
public class RenewableDiscountsService {
      @Value("${stripe.api.secret.key}")
    private String secretKey;

    @Value("${stripe.discount.webhook}")
    private String webhookSecret;

    @Autowired
    private RenewableDiscountsRepository discountsRepository;

    public Object createSession(CreateDiscountSessionRequest request) {
        // Validate request
        if (request.getUnitAmount() == null || request.getUnitAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Map.of("error", "Unit amount must be positive");
        }
        if (request.getCurrency() == null || request.getCurrency().isEmpty()) {
            return Map.of("error", "Currency must be provided");
        }

        RenewableDiscounts discount = new RenewableDiscounts();
        discount.setName(request.getName());
        discount.setPhone(request.getPhone());
        discount.setInstituteOrUniversity(request.getInstituteOrUniversity());
        discount.setCountry(request.getCountry());
        
        // Convert euro to cents for Stripe if currency is EUR
        long unitAmountCents;
        if ("EUR".equalsIgnoreCase(request.getCurrency())) {
            unitAmountCents = request.getUnitAmount().multiply(new BigDecimal(100)).longValue();
        } else {
            unitAmountCents = request.getUnitAmount().longValue();
        }
        discount.setAmountTotal(request.getUnitAmount()); // Save original euro amount for dashboard
        discount.setCurrency(request.getCurrency());
        discount.setCustomerEmail(request.getCustomerEmail());

        try {
            Stripe.apiKey = secretKey;
            SessionCreateParams params = SessionCreateParams.builder()
                    .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                    .addLineItem(
                        SessionCreateParams.LineItem.builder()
                            .setPriceData(
                                SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency(request.getCurrency())
                                    .setUnitAmount(unitAmountCents)
                                    .setProductData(
                                        SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName(request.getName())
                                            .build()
                                    )
                                    .build()
                            )
                            .setQuantity(1L)
                            .build()
                    )
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(request.getSuccessUrl())
                    .setCancelUrl(request.getCancelUrl())
                    .setCustomerEmail(request.getCustomerEmail())
                    .build();

            // Create the session
            Session session = Session.create(params);
            // Set Stripe details after session creation
            discount.setSessionId(session.getId());
            discount.setPaymentIntentId(session.getPaymentIntent());
            // Robust enum mapping for Stripe status
            discount.setStatus(safeMapStripeStatus(session.getStatus()));
            if (session.getCreated() != null) {
                discount.setStripeCreatedAt(java.time.LocalDateTime.ofEpochSecond(session.getCreated(), 0, java.time.ZoneOffset.UTC));
            }
            if (session.getExpiresAt() != null) {
                discount.setStripeExpiresAt(java.time.LocalDateTime.ofEpochSecond(session.getExpiresAt(), 0, java.time.ZoneOffset.UTC));
            }
            discount.setPaymentStatus(session.getPaymentStatus());
            discountsRepository.save(discount);

            // Return payment link and details as a JSON object
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", session.getId());
            response.put("paymentIntentId", session.getPaymentIntent());
            response.put("url", session.getUrl());
            response.put("status", session.getStatus());
            response.put("paymentStatus", session.getPaymentStatus());
            return response;
        } catch (StripeException e) {
            return Map.of("error", "Error creating session: " + e.getMessage());
        }
    }

    // Helper for robust enum mapping
    private com.zn.payment.renewable.entity.RenewablePaymentRecord.PaymentStatus safeMapStripeStatus(String status) {
        if (status == null) return com.zn.payment.renewable.entity.RenewablePaymentRecord.PaymentStatus.PENDING;
        try {
            return com.zn.payment.renewable.entity.RenewablePaymentRecord.PaymentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return com.zn.payment.renewable.entity.RenewablePaymentRecord.PaymentStatus.PENDING;
        }
    }
    public Object handleStripeWebhook(HttpServletRequest request) throws IOException {
        String payload = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        String sigHeader = request.getHeader("Stripe-Signature");

        try {
            // Verify the webhook signature
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

            // Handle the event
            if ("checkout.session.completed".equals(event.getType())) {
                Session session = (Session) event.getData().getObject();
                // Update Discounts record in DB based on Stripe session
                String sessionId = session.getId();
                RenewableDiscounts discount = discountsRepository.findBySessionId(sessionId);
                if (discount != null) {
                    discount.setStatus(safeMapStripeStatus(session.getStatus()));
                    discount.setPaymentStatus(session.getPaymentStatus());
                    if (session.getPaymentIntent() != null) {
                        discount.setPaymentIntentId(session.getPaymentIntent());
                    }
                    if (session.getCreated() != null) {
                        discount.setStripeCreatedAt(java.time.LocalDateTime.ofEpochSecond(session.getCreated(), 0, java.time.ZoneOffset.UTC));
                    }
                    if (session.getExpiresAt() != null) {
                        discount.setStripeExpiresAt(java.time.LocalDateTime.ofEpochSecond(session.getExpiresAt(), 0, java.time.ZoneOffset.UTC));
                    }
                    discountsRepository.save(discount);
                    return Map.of(
                        "message", "Discounts record updated for sessionId: " + sessionId,
                        "status", session.getStatus(),
                        "paymentStatus", session.getPaymentStatus()
                    );
                } else {
                    return Map.of("error", "No Discounts record found for sessionId: " + sessionId);
                }
            } else {
                return Map.of("message", "Unhandled event type: " + event.getType());
            }
        } catch (SignatureVerificationException e) {
            return Map.of("error", "Webhook signature verification failed: " + e.getMessage());
        } catch (JsonSyntaxException e) {
            return Map.of("error", "Invalid JSON payload: " + e.getMessage());
        }
        
    }
    
}
