package com.zn.payment.optics.service;

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
import com.zn.payment.optics.entity.OpticsDiscounts;
import com.zn.payment.optics.entity.OpticsPaymentRecord;
import com.zn.payment.optics.repository.OpticsDiscountsRepository;

import jakarta.servlet.http.HttpServletRequest;


@Service
public class OpticsDiscountsService {
      @Value("${stripe.api.secret.key}")
    private String secretKey;

    @Value("${stripe.discount.webhook}")
    private String webhookSecret;

    @Autowired
    private OpticsDiscountsRepository discountsRepository;

    public Object createSession(CreateDiscountSessionRequest request) {
        // Validate request
        if (request.getUnitAmount() == null || request.getUnitAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Map.of("error", "Unit amount must be positive");
        }
        if (request.getCurrency() == null || request.getCurrency().isEmpty()) {
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
        System.out.println("🎯 Processing optics discount webhook event: " + eventType);
        
        try {
            switch (eventType) {
                case "checkout.session.completed" -> handleDiscountCheckoutSessionCompleted(event);
                case "payment_intent.succeeded" -> handleDiscountPaymentIntentSucceeded(event);
                case "payment_intent.payment_failed" -> handleDiscountPaymentIntentFailed(event);
                default -> System.out.println("ℹ️ Unhandled optics discount event type: " + eventType);
            }
        } catch (Exception e) {
            System.err.println("❌ Error processing optics discount webhook event: " + e.getMessage());
            throw new RuntimeException("Failed to process optics discount webhook event", e);
        }
    }
    
    private void handleDiscountCheckoutSessionCompleted(Event event) {
        System.out.println("🎯 Handling optics discount checkout.session.completed");
        
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
                    System.out.println("✅ Updated OpticsDiscounts status to COMPLETED for session: " + sessionId);
                } else {
                    System.out.println("⚠️ No OpticsDiscounts record found for session: " + sessionId);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error handling optics discount checkout.session.completed: " + e.getMessage());
            throw new RuntimeException("Failed to handle optics discount checkout session completed", e);
        }
    }
    
    private void handleDiscountPaymentIntentSucceeded(Event event) {
        System.out.println("🎯 Handling optics discount payment_intent.succeeded");
        
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
                    System.out.println("✅ Updated OpticsDiscounts status to COMPLETED for payment intent: " + paymentIntentId);
                } else {
                    System.out.println("⚠️ No OpticsDiscounts record found for payment intent: " + paymentIntentId);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error handling optics discount payment_intent.succeeded: " + e.getMessage());
            throw new RuntimeException("Failed to handle optics discount payment intent succeeded", e);
        }
    }
    
    private void handleDiscountPaymentIntentFailed(Event event) {
        System.out.println("🎯 Handling optics discount payment_intent.payment_failed");
        
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
                    System.out.println("✅ Updated OpticsDiscounts status to FAILED for payment intent: " + paymentIntentId);
                } else {
                    System.out.println("⚠️ No OpticsDiscounts record found for payment intent: " + paymentIntentId);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Error handling optics discount payment_intent.payment_failed: " + e.getMessage());
            throw new RuntimeException("Failed to handle optics discount payment intent failed", e);
        }
    }
    /**
     * Update payment status in OpticsDiscounts by Stripe session ID
     */
    public boolean updatePaymentStatusBySessionId(String sessionId, String status) {
        OpticsDiscounts discount = discountsRepository.findBySessionId(sessionId);
        if (discount != null) {
            System.out.println("[OpticsDiscountsService] Found discount for sessionId: " + sessionId);
            System.out.println("[OpticsDiscountsService] Updating paymentStatus to: " + status);
            discount.setPaymentStatus(status);
            try {
                discount.setStatus(com.zn.payment.optics.entity.OpticsPaymentRecord.PaymentStatus.valueOf(status.toUpperCase()));
            } catch (Exception e) {
                System.out.println("[OpticsDiscountsService] Invalid status for enum: " + status + ", defaulting to PENDING");
                discount.setStatus(com.zn.payment.optics.entity.OpticsPaymentRecord.PaymentStatus.PENDING);
            }
            discount.setUpdatedAt(java.time.LocalDateTime.now());
            discountsRepository.save(discount);
            System.out.println("[OpticsDiscountsService] Discount updated and saved for sessionId: " + sessionId);
            return true;
        } else {
            System.out.println("[OpticsDiscountsService] No discount found for sessionId: " + sessionId);
            // Debug: print all session IDs in the table
            System.out.println("[OpticsDiscountsService] Existing session IDs in table:");
            for (OpticsDiscounts d : discountsRepository.findAll()) {
                System.out.println("  - " + d.getSessionId());
            }
        }
        return false;
    }

    /**
     * Update payment status in OpticsDiscounts by Stripe payment intent ID
     */
    public boolean updatePaymentStatusByPaymentIntentId(String paymentIntentId, String status) {
        java.util.Optional<OpticsDiscounts> discountOpt = discountsRepository.findByPaymentIntentId(paymentIntentId);
        if (discountOpt.isPresent()) {
            OpticsDiscounts discount = discountOpt.get();
            System.out.println("[OpticsDiscountsService] Found discount for paymentIntentId: " + paymentIntentId);
            System.out.println("[OpticsDiscountsService] Updating paymentStatus to: " + status);
            discount.setPaymentStatus(status);
            try {
                discount.setStatus(com.zn.payment.optics.entity.OpticsPaymentRecord.PaymentStatus.valueOf(status.toUpperCase()));
            } catch (Exception e) {
                System.out.println("[OpticsDiscountsService] Invalid status for enum: " + status + ", defaulting to PENDING");
                discount.setStatus(com.zn.payment.optics.entity.OpticsPaymentRecord.PaymentStatus.PENDING);
            }
            discount.setUpdatedAt(java.time.LocalDateTime.now());
            discountsRepository.save(discount);
            System.out.println("[OpticsDiscountsService] Discount updated and saved for paymentIntentId: " + paymentIntentId);
            return true;
        } else {
            System.out.println("[OpticsDiscountsService] No discount found for paymentIntentId: " + paymentIntentId);
            // Debug: print all payment intent IDs in the table
            System.out.println("[OpticsDiscountsService] Existing paymentIntent IDs in table:");
            for (OpticsDiscounts d : discountsRepository.findAll()) {
                System.out.println("  - " + d.getPaymentIntentId());
            }
        }
        return false;
    }
    
    /**
     * Find and update OpticsDiscounts record by a specific session ID, with logging.
     */
    public boolean findAndUpdateBySessionId(String sessionId, String newStatus) {
        System.out.println("[OpticsDiscountsService] Searching for sessionId: " + sessionId);
        OpticsDiscounts discount = discountsRepository.findBySessionId(sessionId);
        if (discount != null) {
            System.out.println("[OpticsDiscountsService] Found discount for sessionId: " + sessionId);
            discount.setPaymentStatus(newStatus);
            try {
                discount.setStatus(com.zn.payment.optics.entity.OpticsPaymentRecord.PaymentStatus.valueOf(newStatus.toUpperCase()));
            } catch (Exception e) {
                System.out.println("[OpticsDiscountsService] Invalid status for enum: " + newStatus + ", defaulting to PENDING");
                discount.setStatus(com.zn.payment.optics.entity.OpticsPaymentRecord.PaymentStatus.PENDING);
            }
            discount.setUpdatedAt(java.time.LocalDateTime.now());
            discountsRepository.save(discount);
            System.out.println("[OpticsDiscountsService] Discount updated and saved for sessionId: " + sessionId);
            return true;
        } else {
            System.out.println("[OpticsDiscountsService] No discount found for sessionId: " + sessionId);
            // Debug: print all session IDs in the table
            System.out.println("[OpticsDiscountsService] Existing session IDs in table:");
            for (OpticsDiscounts d : discountsRepository.findAll()) {
                System.out.println("  - " + d.getSessionId());
            }
        }
        return false;
    }
}
