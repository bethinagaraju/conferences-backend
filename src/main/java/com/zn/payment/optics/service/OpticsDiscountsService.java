package com.zn.payment.optics.service;

import com.google.gson.JsonSyntaxException;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import com.zn.payment.dto.CreateDiscountSessionRequest;
import com.zn.payment.optics.entity.OpticsDiscounts;
import com.zn.payment.optics.entity.OpticsPaymentRecord;
import com.zn.payment.optics.repository.OpticsDiscountsRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class OpticsDiscountsService {
      @Value("${stripe.api.secret.key}")
    private String secretKey;

    @Value("${stripe.discount.webhook}")
    private String webhookSecret;

    @Autowired
    private OpticsDiscountsRepository discountsRepository;

    public Object createSession(CreateDiscountSessionRequest request) {
        // Validate request
        log.info("[OpticsDiscountsService] Creating discount session for: name={}, email={}, amount={}, currency={}", request.getName(), request.getCustomerEmail(), request.getUnitAmount(), request.getCurrency());
        if (request.getUnitAmount() == null || request.getUnitAmount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("[OpticsDiscountsService] Invalid unit amount: {}", request.getUnitAmount());
            return Map.of("error", "Unit amount must be positive");
        }
        if (request.getCurrency() == null || request.getCurrency().isEmpty()) {
            log.warn("[OpticsDiscountsService] Currency not provided");
            return Map.of("error", "Currency must be provided");
        }

        OpticsDiscounts discount = new OpticsDiscounts();
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
            // Create metadata to identify this as a discount session
            Map<String, String> metadata = new HashMap<>();
            metadata.put("source", "discount-api");
            metadata.put("paymentType", "discount-registration");
            metadata.put("customerName", request.getName());
            metadata.put("customerEmail", request.getCustomerEmail());
            if (request.getPhone() != null) {
                metadata.put("customerPhone", request.getPhone());
            }
            if (request.getInstituteOrUniversity() != null) {
                metadata.put("customerInstitute", request.getInstituteOrUniversity());
            }
            if (request.getCountry() != null) {
                metadata.put("customerCountry", request.getCountry());
            }
            log.info("[OpticsDiscountsService] Creating Stripe session for {} (amount: {}, currency: {})", request.getCustomerEmail(), unitAmountCents, request.getCurrency());
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
                    .putAllMetadata(metadata)
                    .build();

            // Create the session
            Session session = Session.create(params);
            log.info("[OpticsDiscountsService] Stripe session created: id={}, paymentIntent={}, status={}, paymentStatus={}", session.getId(), session.getPaymentIntent(), session.getStatus(), session.getPaymentStatus());
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
            log.info("[OpticsDiscountsService] Discount record saved for sessionId: {}", session.getId());

            // Return payment link and details as a JSON object
            Map<String, Object> response = new HashMap<>();
            response.put("sessionId", session.getId());
            response.put("paymentIntentId", session.getPaymentIntent());
            response.put("url", session.getUrl());
            response.put("status", session.getStatus());
            response.put("paymentStatus", session.getPaymentStatus());
            return response;
        } catch (StripeException e) {
            log.error("[OpticsDiscountsService] Error creating Stripe session: {}", e.getMessage(), e);
            return Map.of("error", "Error creating session: " + e.getMessage());
        }
    }

    // Helper for robust enum mapping
    private OpticsPaymentRecord.PaymentStatus safeMapStripeStatus(String status) {
        if (status == null) return OpticsPaymentRecord.PaymentStatus.PENDING;
        try {
            return OpticsPaymentRecord.PaymentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return OpticsPaymentRecord.PaymentStatus.PENDING;
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
                Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
                if (session == null) {
                    return Map.of("error", "Could not deserialize checkout session");
                }
                // Update OpticsDiscounts record in DB based on Stripe session
                String sessionId = session.getId();
                OpticsDiscounts discount = discountsRepository.findBySessionId(sessionId);
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
                        "message", "OpticsDiscounts record updated for sessionId: " + sessionId,
                        "status", session.getStatus(),
                        "paymentStatus", session.getPaymentStatus()
                    );
                } else {
                    return Map.of("error", "No OpticsDiscounts record found for sessionId: " + sessionId);
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
    
    /**
     * Construct webhook event from payload and signature
     */
    public Event constructWebhookEvent(String payload, String sigHeader) throws com.stripe.exception.SignatureVerificationException {
        return Webhook.constructEvent(payload, sigHeader, webhookSecret);
    }
    
    /**
     * Process webhook event - updates discount status in database
     */
    public void processWebhookEvent(Event event) {
        String eventType = event.getType();
        log.info("🎯 [OpticsDiscountsService][WEBHOOK] Processing optics discount webhook event: {}", eventType);
        try {
            switch (eventType) {
                case "checkout.session.completed" -> {
                    log.info("[OpticsDiscountsService][WEBHOOK] Handling event: checkout.session.completed");
                    handleDiscountCheckoutSessionCompleted(event);
                }
                case "payment_intent.succeeded" -> {
                    log.info("[OpticsDiscountsService][WEBHOOK] Handling event: payment_intent.succeeded");
                    handleDiscountPaymentIntentSucceeded(event);
                }
                case "payment_intent.payment_failed" -> {
                    log.info("[OpticsDiscountsService][WEBHOOK] Handling event: payment_intent.payment_failed");
                    handleDiscountPaymentIntentFailed(event);
                }
                default -> log.warn("[OpticsDiscountsService][WEBHOOK] Unhandled event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("❌ [OpticsDiscountsService][WEBHOOK] Error processing optics discount webhook event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process optics discount webhook event", e);
        }
    }
    
    private void handleDiscountCheckoutSessionCompleted(Event event) {
        log.info("🎯 [OpticsDiscountsService][WEBHOOK] Handling optics discount checkout.session.completed");
        try {
            java.util.Optional<com.stripe.model.StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
            if (stripeObjectOpt.isPresent() && stripeObjectOpt.get() instanceof com.stripe.model.checkout.Session) {
                com.stripe.model.checkout.Session session = (com.stripe.model.checkout.Session) stripeObjectOpt.get();
                String sessionId = session.getId();
                // Find the discount record by session ID
                OpticsDiscounts discount = discountsRepository.findBySessionId(sessionId);
                if (discount != null) {
                    discount.setStatus(OpticsPaymentRecord.PaymentStatus.COMPLETED);
                    discount.setPaymentIntentId(session.getPaymentIntent());
                    discount.setUpdatedAt(java.time.LocalDateTime.now());
                    discountsRepository.save(discount);
                    log.info("[OpticsDiscountsService][WEBHOOK] Updated OpticsDiscounts status to COMPLETED for session: {}", sessionId);
                } else {
                    log.warn("[OpticsDiscountsService][WEBHOOK] No OpticsDiscounts record found for session: {}", sessionId);
                }
            }
        } catch (Exception e) {
            log.error("❌ [OpticsDiscountsService][WEBHOOK] Error handling optics discount checkout.session.completed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to handle optics discount checkout session completed", e);
        }
    }
    
    private void handleDiscountPaymentIntentSucceeded(Event event) {
        log.info("🎯 [OpticsDiscountsService][WEBHOOK] Handling optics discount payment_intent.succeeded");
        try {
            java.util.Optional<com.stripe.model.StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
            if (stripeObjectOpt.isPresent() && stripeObjectOpt.get() instanceof com.stripe.model.PaymentIntent) {
                com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) stripeObjectOpt.get();
                String paymentIntentId = paymentIntent.getId();
                // Find the discount record by payment intent ID
                java.util.Optional<OpticsDiscounts> discountOpt = discountsRepository.findByPaymentIntentId(paymentIntentId);
                if (discountOpt.isPresent()) {
                    OpticsDiscounts discount = discountOpt.get();
                    discount.setStatus(OpticsPaymentRecord.PaymentStatus.COMPLETED);
                    discount.setUpdatedAt(java.time.LocalDateTime.now());
                    discountsRepository.save(discount);
                    log.info("[OpticsDiscountsService][WEBHOOK] Updated OpticsDiscounts status to COMPLETED for payment intent: {}", paymentIntentId);
                } else {
                    log.warn("[OpticsDiscountsService][WEBHOOK] No OpticsDiscounts record found for payment intent: {}", paymentIntentId);
                }
            }
        } catch (Exception e) {
            log.error("❌ [OpticsDiscountsService][WEBHOOK] Error handling optics discount payment_intent.succeeded: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to handle optics discount payment intent succeeded", e);
        }
    }
    
    private void handleDiscountPaymentIntentFailed(Event event) {
        log.info("🎯 [OpticsDiscountsService][WEBHOOK] Handling optics discount payment_intent.payment_failed");
        try {
            java.util.Optional<com.stripe.model.StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
            if (stripeObjectOpt.isPresent() && stripeObjectOpt.get() instanceof com.stripe.model.PaymentIntent) {
                com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) stripeObjectOpt.get();
                String paymentIntentId = paymentIntent.getId();
                // Find the discount record by payment intent ID
                java.util.Optional<OpticsDiscounts> discountOpt = discountsRepository.findByPaymentIntentId(paymentIntentId);
                if (discountOpt.isPresent()) {
                    OpticsDiscounts discount = discountOpt.get();
                    discount.setStatus(OpticsPaymentRecord.PaymentStatus.FAILED);
                    discount.setUpdatedAt(java.time.LocalDateTime.now());
                    discountsRepository.save(discount);
                    log.info("[OpticsDiscountsService][WEBHOOK] Updated OpticsDiscounts status to FAILED for payment intent: {}", paymentIntentId);
                } else {
                    log.warn("[OpticsDiscountsService][WEBHOOK] No OpticsDiscounts record found for payment intent: {}", paymentIntentId);
                }
            }
        } catch (Exception e) {
            log.error("❌ [OpticsDiscountsService][WEBHOOK] Error handling optics discount payment_intent.payment_failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to handle optics discount payment intent failed", e);
        }
    }
    /**
     * Update payment status in OpticsDiscounts by Stripe session ID
     */
    public boolean updatePaymentStatusBySessionId(String sessionId, String status) {
        log.info("[OpticsDiscountsService][WEBHOOK] Attempting to update payment status for sessionId: {} to {}", sessionId, status);
        OpticsDiscounts discount = discountsRepository.findBySessionId(sessionId);
        if (discount != null) {
            log.info("[OpticsDiscountsService][WEBHOOK] Found discount for sessionId: {}", sessionId);
            log.info("[OpticsDiscountsService][WEBHOOK] Previous paymentStatus: {}, Previous status(enum): {}", discount.getPaymentStatus(), discount.getStatus());
            discount.setPaymentStatus(status);
            try {
                discount.setStatus(com.zn.payment.optics.entity.OpticsPaymentRecord.PaymentStatus.valueOf(status.toUpperCase()));
            } catch (Exception e) {
                log.warn("[OpticsDiscountsService][WEBHOOK] Invalid status for enum: {}, defaulting to PENDING", status);
                discount.setStatus(com.zn.payment.optics.entity.OpticsPaymentRecord.PaymentStatus.PENDING);
            }
            discount.setUpdatedAt(java.time.LocalDateTime.now());
            discountsRepository.save(discount);
            log.info("[OpticsDiscountsService][WEBHOOK] Discount updated and saved for sessionId: {}. New paymentStatus: {}, New status(enum): {}", sessionId, discount.getPaymentStatus(), discount.getStatus());
            return true;
        } else {
            log.warn("[OpticsDiscountsService][WEBHOOK] No discount found for sessionId: {}", sessionId);
            // Debug: print all session IDs in the table
            log.info("[OpticsDiscountsService][WEBHOOK] Existing session IDs in table:");
            for (OpticsDiscounts d : discountsRepository.findAll()) {
                log.info("  - {}", d.getSessionId());
            }
        }
        return false;
    }

    /**
     * Update payment status in OpticsDiscounts by Stripe payment intent ID
     */
    public boolean updatePaymentStatusByPaymentIntentId(String paymentIntentId, String status) {
        log.info("[OpticsDiscountsService][WEBHOOK] Attempting to update payment status for paymentIntentId: {} to {}", paymentIntentId, status);
        java.util.Optional<OpticsDiscounts> discountOpt = discountsRepository.findByPaymentIntentId(paymentIntentId);
        if (discountOpt.isPresent()) {
            OpticsDiscounts discount = discountOpt.get();
            log.info("[OpticsDiscountsService][WEBHOOK] Found discount for paymentIntentId: {}", paymentIntentId);
            log.info("[OpticsDiscountsService][WEBHOOK] Previous paymentStatus: {}, Previous status(enum): {}", discount.getPaymentStatus(), discount.getStatus());
            discount.setPaymentStatus(status);
            try {
                discount.setStatus(com.zn.payment.optics.entity.OpticsPaymentRecord.PaymentStatus.valueOf(status.toUpperCase()));
            } catch (Exception e) {
                log.warn("[OpticsDiscountsService][WEBHOOK] Invalid status for enum: {}, defaulting to PENDING", status);
                discount.setStatus(com.zn.payment.optics.entity.OpticsPaymentRecord.PaymentStatus.PENDING);
            }
            discount.setUpdatedAt(java.time.LocalDateTime.now());
            discountsRepository.save(discount);
            log.info("[OpticsDiscountsService][WEBHOOK] Discount updated and saved for paymentIntentId: {}. New paymentStatus: {}, New status(enum): {}", paymentIntentId, discount.getPaymentStatus(), discount.getStatus());
            return true;
        } else {
            log.warn("[OpticsDiscountsService][WEBHOOK] No discount found for paymentIntentId: {}", paymentIntentId);
            // Debug: print all payment intent IDs in the table
            log.info("[OpticsDiscountsService][WEBHOOK] Existing paymentIntent IDs in table:");
            for (OpticsDiscounts d : discountsRepository.findAll()) {
                log.info("  - {}", d.getPaymentIntentId());
            }
        }
        return false;
    }
    
    /**
     * Find and update OpticsDiscounts record by a specific session ID, with logging.
     */
    public boolean findAndUpdateBySessionId(String sessionId, String newStatus) {
        log.info("[OpticsDiscountsService] Searching for sessionId: {}", sessionId);
        OpticsDiscounts discount = discountsRepository.findBySessionId(sessionId);
        if (discount != null) {
            log.info("[OpticsDiscountsService] Found discount for sessionId: {}", sessionId);
            discount.setPaymentStatus(newStatus);
            try {
                discount.setStatus(com.zn.payment.optics.entity.OpticsPaymentRecord.PaymentStatus.valueOf(newStatus.toUpperCase()));
            } catch (Exception e) {
                log.warn("[OpticsDiscountsService] Invalid status for enum: {}, defaulting to PENDING", newStatus);
                discount.setStatus(com.zn.payment.optics.entity.OpticsPaymentRecord.PaymentStatus.PENDING);
            }
            discount.setUpdatedAt(java.time.LocalDateTime.now());
            discountsRepository.save(discount);
            log.info("[OpticsDiscountsService] Discount updated and saved for sessionId: {}", sessionId);
            return true;
        } else {
            log.warn("[OpticsDiscountsService] No discount found for sessionId: {}", sessionId);
            // Debug: print all session IDs in the table
            log.info("[OpticsDiscountsService] Existing session IDs in table:");
            for (OpticsDiscounts d : discountsRepository.findAll()) {
                log.info("  - {}", d.getSessionId());
            }
        }
        return false;
    }
}
