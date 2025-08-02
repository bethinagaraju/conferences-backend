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
            metadata.put("productName", request.getProductName());
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
                                            .setName(request.getProductName())
                                            .setDescription(request.getDescription())
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
            // Use EventDataObjectDeserializer to get the Session object - same pattern as PolymersDiscountsService
            com.stripe.model.EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            log.info("🔍 [OpticsDiscountsService][WEBHOOK] EventDataObjectDeserializer available: {}", dataObjectDeserializer != null);
            
            if (dataObjectDeserializer != null && dataObjectDeserializer.getObject().isPresent()) {
                Object deserializedObject = dataObjectDeserializer.getObject().get();
                log.info("📋 [OpticsDiscountsService][WEBHOOK] Event data object type: {}", deserializedObject.getClass().getSimpleName());
                
                if (deserializedObject instanceof com.stripe.model.checkout.Session session) {
                    String sessionId = session.getId();
                    log.info("✅ [OpticsDiscountsService][WEBHOOK] Successfully retrieved Session: {}", sessionId);
                    
                    // Log key data from the webhook for debugging
                    log.info("📊 [OpticsDiscountsService][WEBHOOK] Webhook Session Data:");
                    log.info("   - Session ID: {}", session.getId());
                    log.info("   - Amount Total: {} cents", session.getAmountTotal());
                    log.info("   - Currency: {}", session.getCurrency());
                    log.info("   - Customer Email: {}", session.getCustomerEmail());
                    log.info("   - Payment Intent: {}", session.getPaymentIntent());
                    log.info("   - Payment Status: {}", session.getPaymentStatus());
                    log.info("   - Session Status: {}", session.getStatus());
                    
                    // Find the discount record by session ID
                    OpticsDiscounts discount = discountsRepository.findBySessionId(sessionId);
                    if (discount != null) {
                        log.info("✅ [OpticsDiscountsService][WEBHOOK] Found existing discount record with ID: {}, current status: {}", discount.getId(), discount.getStatus());
                        discount.setStatus(OpticsPaymentRecord.PaymentStatus.COMPLETED);
                        discount.setPaymentStatus("paid");
                        if (session.getPaymentIntent() != null) {
                            discount.setPaymentIntentId(session.getPaymentIntent());
                            log.info("✅ [OpticsDiscountsService][WEBHOOK] Updated paymentIntentId to: {}", session.getPaymentIntent());
                        }
                        discount.setUpdatedAt(java.time.LocalDateTime.now());
                        discountsRepository.save(discount);
                        log.info("✅ [OpticsDiscountsService][WEBHOOK] Updated OpticsDiscounts status to COMPLETED for session: {}", sessionId);
                        
                        // Log the updated record details
                        log.info("✅ [OpticsDiscountsService][WEBHOOK] Final record state - ID: {}, status: {}, paymentStatus: {}, paymentIntentId: {}", 
                                discount.getId(), discount.getStatus(), discount.getPaymentStatus(), discount.getPaymentIntentId());
                    } else {
                        log.warn("⚠️ [OpticsDiscountsService][WEBHOOK] No OpticsDiscounts record found for session: {}", sessionId);
                        log.warn("⚠️ [OpticsDiscountsService][WEBHOOK] This should not happen! The record should have been created during session creation.");
                        
                        // List all records to debug
                        java.util.List<OpticsDiscounts> allDiscounts = discountsRepository.findAll();
                        log.info("🔍 [OpticsDiscountsService][WEBHOOK] Total discount records in database: {}", allDiscounts.size());
                        for (OpticsDiscounts d : allDiscounts) {
                            log.info("🔍 [OpticsDiscountsService][WEBHOOK] Existing record - ID: {}, sessionId: {}, paymentIntentId: {}", 
                                    d.getId(), d.getSessionId(), d.getPaymentIntentId());
                        }
                    }
                } else {
                    log.error("❌ [OpticsDiscountsService][WEBHOOK] Event data object is not a Session! Type: {}", deserializedObject.getClass().getName());
                    // Fallback: Try to extract session data manually from event
                    log.info("🔄 [OpticsDiscountsService][WEBHOOK] Attempting manual session data extraction from event...");
                    extractAndProcessSessionDataFromEvent(event);
                }
            } else {
                log.error("❌ [OpticsDiscountsService][WEBHOOK] Failed to deserialize checkout.session.completed event data");
                // Fallback: Try to extract session data manually from event
                log.info("🔄 [OpticsDiscountsService][WEBHOOK] Attempting manual session data extraction from event...");
                extractAndProcessSessionDataFromEvent(event);
            }
        } catch (Exception e) {
            log.error("❌ [OpticsDiscountsService][WEBHOOK] Error handling optics discount checkout.session.completed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to handle optics discount checkout session completed", e);
        }
    }
    
    private void handleDiscountPaymentIntentSucceeded(Event event) {
        log.info("🎯 [OpticsDiscountsService][WEBHOOK] Handling optics discount payment_intent.succeeded");
        try {
            // Use EventDataObjectDeserializer to get the PaymentIntent object - same pattern as PolymersDiscountsService
            com.stripe.model.EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            log.info("🔍 [OpticsDiscountsService][WEBHOOK] EventDataObjectDeserializer available: {}", dataObjectDeserializer != null);
            
            if (dataObjectDeserializer != null && dataObjectDeserializer.getObject().isPresent()) {
                Object deserializedObject = dataObjectDeserializer.getObject().get();
                log.info("📋 [OpticsDiscountsService][WEBHOOK] Event data object type: {}", deserializedObject.getClass().getSimpleName());
                
                if (deserializedObject instanceof com.stripe.model.PaymentIntent paymentIntent) {
                    String paymentIntentId = paymentIntent.getId();
                    log.info("✅ [OpticsDiscountsService][WEBHOOK] Successfully retrieved PaymentIntent: {}", paymentIntentId);
                    
                    // Log key data from the webhook for debugging
                    log.info("📊 [OpticsDiscountsService][WEBHOOK] Webhook PaymentIntent Data:");
                    log.info("   - Payment Intent ID: {}", paymentIntent.getId());
                    log.info("   - Amount: {} cents", paymentIntent.getAmount());
                    log.info("   - Currency: {}", paymentIntent.getCurrency());
                    log.info("   - Status: {}", paymentIntent.getStatus());
                    
                    // Try to find by session ID first (since paymentIntentId might be null in our records)
                    // Look for records with matching metadata
                    java.util.List<OpticsDiscounts> allDiscounts = discountsRepository.findAll();
                    log.info("🔍 [OpticsDiscountsService][WEBHOOK] Total discount records in database: {}", allDiscounts.size());
                    
                    OpticsDiscounts matchingDiscount = null;
                    for (OpticsDiscounts discount : allDiscounts) {
                        log.info("🔍 [OpticsDiscountsService][WEBHOOK] Checking discount ID: {}, sessionId: {}, paymentIntentId: {}", 
                                discount.getId(), discount.getSessionId(), discount.getPaymentIntentId());
                        if (paymentIntentId.equals(discount.getPaymentIntentId())) {
                            matchingDiscount = discount;
                            break;
                        }
                    }
                    
                    // Find the discount record by payment intent ID
                    java.util.Optional<OpticsDiscounts> discountOpt = discountsRepository.findByPaymentIntentId(paymentIntentId);
                    if (discountOpt.isPresent()) {
                        OpticsDiscounts discount = discountOpt.get();
                        log.info("✅ [OpticsDiscountsService][WEBHOOK] Found existing discount record with ID: {}, current status: {}", discount.getId(), discount.getStatus());
                        discount.setStatus(OpticsPaymentRecord.PaymentStatus.COMPLETED);
                        discount.setPaymentStatus("paid");
                        discount.setUpdatedAt(java.time.LocalDateTime.now());
                        discountsRepository.save(discount);
                        log.info("✅ [OpticsDiscountsService][WEBHOOK] Updated OpticsDiscounts status to COMPLETED for payment intent: {}", paymentIntentId);
                    } else {
                        log.warn("⚠️ [OpticsDiscountsService][WEBHOOK] No OpticsDiscounts record found for payment intent: {}", paymentIntentId);
                        log.warn("⚠️ [OpticsDiscountsService][WEBHOOK] Manual search result: {}", matchingDiscount != null ? "Found match with ID: " + matchingDiscount.getId() : "No match found");
                        
                        // If we found a match manually, update it
                        if (matchingDiscount != null) {
                            log.info("✅ [OpticsDiscountsService][WEBHOOK] Using manually found discount record with ID: {}", matchingDiscount.getId());
                            matchingDiscount.setStatus(OpticsPaymentRecord.PaymentStatus.COMPLETED);
                            matchingDiscount.setPaymentStatus("paid");
                            matchingDiscount.setPaymentIntentId(paymentIntentId); // Update the paymentIntentId
                            matchingDiscount.setUpdatedAt(java.time.LocalDateTime.now());
                            discountsRepository.save(matchingDiscount);
                            log.info("✅ [OpticsDiscountsService][WEBHOOK] Updated OpticsDiscounts status to COMPLETED for manually found record");
                        } else {
                            // Fallback: Try to extract payment intent data manually from event
                            log.info("🔄 [OpticsDiscountsService][WEBHOOK] Attempting manual payment intent data extraction from event...");
                            extractAndProcessPaymentIntentDataFromEvent(event);
                        }
                    }
                } else {
                    log.error("❌ [OpticsDiscountsService][WEBHOOK] Event data object is not a PaymentIntent! Type: {}", deserializedObject.getClass().getName());
                    // Fallback: Try to extract payment intent data manually from event
                    log.info("🔄 [OpticsDiscountsService][WEBHOOK] Attempting manual payment intent data extraction from event...");
                    extractAndProcessPaymentIntentDataFromEvent(event);
                }
            } else {
                log.error("❌ [OpticsDiscountsService][WEBHOOK] Failed to deserialize payment_intent.succeeded event data");
                // Fallback: Try to extract payment intent data manually from event
                log.info("🔄 [OpticsDiscountsService][WEBHOOK] Attempting manual payment intent data extraction from event...");
                extractAndProcessPaymentIntentDataFromEvent(event);
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
    
    /**
     * Fallback method to manually extract session data from the event when deserialization fails
     * This handles the case where EventDataObjectDeserializer doesn't work properly
     */
    private void extractAndProcessSessionDataFromEvent(Event event) {
        try {
            log.info("🔄 [OpticsDiscountsService][WEBHOOK] Starting manual extraction from Event object...");
            
            // Try to get the raw JSON string from the event directly
            String rawEventJson = event.toJson();
            log.info("📋 [OpticsDiscountsService][WEBHOOK] Full Event JSON: {}", rawEventJson);
            
            // Extract key fields manually from the JSON
            String sessionId = extractJsonField(rawEventJson, "id");
            String customerEmail = extractJsonField(rawEventJson, "customer_email");
            String paymentIntent = extractJsonField(rawEventJson, "payment_intent");
            String paymentStatus = extractJsonField(rawEventJson, "payment_status");
            String sessionStatus = extractJsonField(rawEventJson, "status");
            
            log.info("🔍 [OpticsDiscountsService][WEBHOOK] Manually extracted session data:");
            log.info("   - Session ID: {}", sessionId);
            log.info("   - Customer Email: {}", customerEmail);
            log.info("   - Payment Intent: {}", paymentIntent);
            log.info("   - Payment Status: {}", paymentStatus);
            log.info("   - Session Status: {}", sessionStatus);
            
            if (sessionId != null && !sessionId.isEmpty()) {
                // Find the discount record by session ID
                OpticsDiscounts discount = discountsRepository.findBySessionId(sessionId);
                if (discount != null) {
                    log.info("✅ [OpticsDiscountsService][WEBHOOK] Found existing discount record with manual extraction - ID: {}, current status: {}", discount.getId(), discount.getStatus());
                    discount.setStatus(OpticsPaymentRecord.PaymentStatus.COMPLETED);
                    discount.setPaymentStatus("paid");
                    if (paymentIntent != null && !paymentIntent.isEmpty()) {
                        discount.setPaymentIntentId(paymentIntent);
                        log.info("✅ [OpticsDiscountsService][WEBHOOK] Updated paymentIntentId to: {}", paymentIntent);
                    }
                    discount.setUpdatedAt(java.time.LocalDateTime.now());
                    discountsRepository.save(discount);
                    log.info("✅ [OpticsDiscountsService][WEBHOOK] Updated OpticsDiscounts status to COMPLETED for session (manual): {}", sessionId);
                } else {
                    log.warn("⚠️ [OpticsDiscountsService][WEBHOOK] No OpticsDiscounts record found for session (manual): {}", sessionId);
                }
            } else {
                log.error("❌ [OpticsDiscountsService][WEBHOOK] Could not extract session ID from event JSON");
            }
        } catch (Exception e) {
            log.error("❌ [OpticsDiscountsService][WEBHOOK] Error in manual session data extraction: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Extract a field value from JSON string
     */
    private String extractJsonField(String json, String fieldName) {
        try {
            // Simple JSON field extraction - look for "fieldName":"value"
            String pattern = "\"" + fieldName + "\"\\s*:\\s*\"([^\"]+)\"";
            java.util.regex.Pattern regexPattern = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher matcher = regexPattern.matcher(json);
            if (matcher.find()) {
                return matcher.group(1);
            }
            
            // Also try without quotes (for numbers, booleans)
            String patternNoQuotes = "\"" + fieldName + "\"\\s*:\\s*([^,\\s}]+)";
            java.util.regex.Pattern regexPatternNoQuotes = java.util.regex.Pattern.compile(patternNoQuotes);
            java.util.regex.Matcher matcherNoQuotes = regexPatternNoQuotes.matcher(json);
            if (matcherNoQuotes.find()) {
                return matcherNoQuotes.group(1);
            }
        } catch (Exception e) {
            log.warn("⚠️ [OpticsDiscountsService][WEBHOOK] Could not extract field '{}' from JSON: {}", fieldName, e.getMessage());
        }
        return null;
    }
    
    /**
     * Fallback method to manually extract payment intent data from the event when deserialization fails
     */
    private void extractAndProcessPaymentIntentDataFromEvent(Event event) {
        try {
            log.info("🔄 [OpticsDiscountsService][WEBHOOK] Starting manual payment intent extraction from Event object...");
            
            // Try to get the raw JSON string from the event directly
            String rawEventJson = event.toJson();
            log.info("📋 [OpticsDiscountsService][WEBHOOK] Full Event JSON: {}", rawEventJson);
            
            // Extract key fields manually from the JSON
            String paymentIntentId = extractJsonField(rawEventJson, "id");
            String status = extractJsonField(rawEventJson, "status");
            String amount = extractJsonField(rawEventJson, "amount");
            String currency = extractJsonField(rawEventJson, "currency");
            
            log.info("🔍 [OpticsDiscountsService][WEBHOOK] Manually extracted payment intent data:");
            log.info("   - Payment Intent ID: {}", paymentIntentId);
            log.info("   - Status: {}", status);
            log.info("   - Amount: {}", amount);
            log.info("   - Currency: {}", currency);
            
            if (paymentIntentId != null && !paymentIntentId.isEmpty()) {
                // First try to find by payment intent ID
                java.util.Optional<OpticsDiscounts> discountOpt = discountsRepository.findByPaymentIntentId(paymentIntentId);
                if (discountOpt.isPresent()) {
                    OpticsDiscounts discount = discountOpt.get();
                    log.info("✅ [OpticsDiscountsService][WEBHOOK] Found existing discount record with manual extraction - ID: {}, current status: {}", discount.getId(), discount.getStatus());
                    discount.setStatus(OpticsPaymentRecord.PaymentStatus.COMPLETED);
                    discount.setPaymentStatus("paid");
                    discount.setUpdatedAt(java.time.LocalDateTime.now());
                    discountsRepository.save(discount);
                    log.info("✅ [OpticsDiscountsService][WEBHOOK] Updated OpticsDiscounts status to COMPLETED for payment intent (manual): {}", paymentIntentId);
                } else {
                    // Try to find by checking all records manually
                    java.util.List<OpticsDiscounts> allDiscounts = discountsRepository.findAll();
                    for (OpticsDiscounts discount : allDiscounts) {
                        if (paymentIntentId.equals(discount.getPaymentIntentId())) {
                            log.info("✅ [OpticsDiscountsService][WEBHOOK] Found matching discount via manual search - ID: {}", discount.getId());
                            discount.setStatus(OpticsPaymentRecord.PaymentStatus.COMPLETED);
                            discount.setPaymentStatus("paid");
                            discount.setUpdatedAt(java.time.LocalDateTime.now());
                            discountsRepository.save(discount);
                            log.info("✅ [OpticsDiscountsService][WEBHOOK] Updated OpticsDiscounts status to COMPLETED via manual search");
                            return;
                        }
                    }
                    log.warn("⚠️ [OpticsDiscountsService][WEBHOOK] No OpticsDiscounts record found for payment intent (manual): {}", paymentIntentId);
                }
            } else {
                log.error("❌ [OpticsDiscountsService][WEBHOOK] Could not extract payment intent ID from event JSON");
            }
        } catch (Exception e) {
            log.error("❌ [OpticsDiscountsService][WEBHOOK] Error in manual payment intent data extraction: {}", e.getMessage(), e);
        }
    }
}
