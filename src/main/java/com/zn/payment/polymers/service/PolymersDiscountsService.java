package com.zn.payment.polymers.service;

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
import com.zn.payment.polymers.entity.PolymersDiscounts;
// ...existing code...
import com.zn.payment.polymers.entity.PolymersPaymentRecord.PaymentStatus;
import com.zn.payment.polymers.repository.PolymersDiscountsRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class PolymersDiscountsService {
    /**
     * Update payment status in PolymersDiscounts by Stripe session ID
     * Following same pattern as payment webhook processing
     */
    public boolean updatePaymentStatusBySessionId(String sessionId, String status) {
        log.info("[PolymersDiscountsService][WEBHOOK] Attempting to update payment status for sessionId: {} to {}", sessionId, status);
        PolymersDiscounts discount = discountsRepository.findBySessionId(sessionId);
        if (discount != null) {
            log.info("[PolymersDiscountsService][WEBHOOK] Found discount record ID: {} for sessionId: {}", discount.getId(), sessionId);
            log.info("[PolymersDiscountsService][WEBHOOK] Current status: {}, updating paymentStatus to: {}", discount.getStatus(), status);
            
            // Update payment status
            discount.setPaymentStatus(status);
            
            // Update main status based on payment status - same logic as payment webhook
            if ("COMPLETED".equals(status) || "paid".equals(status)) {
                discount.setStatus(PaymentStatus.COMPLETED);
            } else if ("SUCCEEDED".equals(status)) {
                discount.setStatus(PaymentStatus.COMPLETED);
                discount.setPaymentStatus("paid"); // Override to use "paid" instead of "SUCCEEDED"
            } else if ("FAILED".equals(status)) {
                discount.setStatus(PaymentStatus.FAILED);
            }
            
            // Update timestamp
            discount.setUpdatedAt(java.time.LocalDateTime.now());
            
            discountsRepository.save(discount);
            log.info("[PolymersDiscountsService][WEBHOOK] Discount updated and saved for sessionId: {}", sessionId);
            return true;
        } else {
            log.warn("[PolymersDiscountsService][WEBHOOK] No discount found for sessionId: {}", sessionId);
        }
        return false;
    }

    /**
     * Update payment status in PolymersDiscounts by Stripe payment intent ID
     * Following same pattern as payment webhook processing
     */
    public boolean updatePaymentStatusByPaymentIntentId(String paymentIntentId, String status) {
        log.info("[PolymersDiscountsService][WEBHOOK] Attempting to update payment status for paymentIntentId: {} to {}", paymentIntentId, status);
        java.util.Optional<PolymersDiscounts> discountOpt = discountsRepository.findByPaymentIntentId(paymentIntentId);
        if (discountOpt.isPresent()) {
            PolymersDiscounts discount = discountOpt.get();
            log.info("[PolymersDiscountsService][WEBHOOK] Found discount record ID: {} for paymentIntentId: {}", discount.getId(), paymentIntentId);
            log.info("[PolymersDiscountsService][WEBHOOK] Current status: {}, updating paymentStatus to: {}", discount.getStatus(), status);
            
            // Update payment status
            discount.setPaymentStatus(status);
            
            // Update main status based on payment status - same logic as payment webhook
            if ("COMPLETED".equals(status) || "paid".equals(status)) {
                discount.setStatus(PaymentStatus.COMPLETED);
            } else if ("SUCCEEDED".equals(status)) {
                discount.setStatus(PaymentStatus.COMPLETED);
                discount.setPaymentStatus("paid"); // Override to use "paid" instead of "SUCCEEDED"
            } else if ("FAILED".equals(status)) {
                discount.setStatus(PaymentStatus.FAILED);
            }
            
            // Update timestamp
            discount.setUpdatedAt(java.time.LocalDateTime.now());
            
            discountsRepository.save(discount);
            log.info("[PolymersDiscountsService][WEBHOOK] Discount updated and saved for paymentIntentId: {}", paymentIntentId);
            return true;
        } else {
            log.warn("[PolymersDiscountsService][WEBHOOK] No discount found for paymentIntentId: {}", paymentIntentId);
        }
        return false;
    }
      @Value("${stripe.api.secret.key}")
    private String secretKey;

    @Value("${stripe.discount.webhook}")
    private String webhookSecret;

    @Autowired
    private PolymersDiscountsRepository discountsRepository;

    public Object createSession(CreateDiscountSessionRequest request) {
        // Validate request
        if (request.getUnitAmount() == null || request.getUnitAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Map.of("error", "Unit amount must be positive");
        }
        try {
            Stripe.apiKey = secretKey;
            PolymersDiscounts discount = new PolymersDiscounts();
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
            discount.setName(request.getName()); // Set customer name
            
            // Set additional fields if available
            if (request.getProductName() != null) {
                // You might want to store productName in a field if your entity supports it
                // For now, we'll include it in metadata
            }

            // Create metadata to identify this as a discount session
            Map<String, String> metadata = new HashMap<>();
            metadata.put("source", "discount-api");
            metadata.put("paymentType", "discount-registration");
            metadata.put("customerName", request.getName());
            metadata.put("customerEmail", request.getCustomerEmail());
            if (request.getProductName() != null) {
                metadata.put("productName", request.getProductName());
            }
            if (request.getOrderReference() != null) {
                metadata.put("orderReference", request.getOrderReference());
            }
            if (request.getDescription() != null) {
                metadata.put("description", request.getDescription());
            }
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
                                            .setName(request.getProductName() != null ? request.getProductName() : request.getName())
                                            .setDescription(request.getDescription() != null ? request.getDescription() : "Polymer Summit 2026 Discount Registration")
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
            
            log.info("✅ [PolymersDiscountsService][SESSION] Created discount record with sessionId: {} and paymentIntentId: {}", session.getId(), session.getPaymentIntent());

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
    private PaymentStatus safeMapStripeStatus(String status) {
        if (status == null) return PaymentStatus.PENDING;
        try {
            return PaymentStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return PaymentStatus.PENDING;
        }
    }
    public Object handleStripeWebhook(HttpServletRequest request) throws IOException {
        String payload = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        String sigHeader = request.getHeader("Stripe-Signature");

        log.info("[PolymersDiscountsService][WEBHOOK] Received Stripe webhook. Signature header present: {}", sigHeader != null);
        if (sigHeader == null || sigHeader.isEmpty()) {
            log.error("[PolymersDiscountsService][WEBHOOK] Missing Stripe-Signature header in webhook");
            return Map.of("error", "Missing signature header");
        }
        try {
            // Verify the webhook signature
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
            log.info("[PolymersDiscountsService][WEBHOOK] Webhook event type: {}", event.getType());

            // Handle the event
            if ("checkout.session.completed".equals(event.getType())) {
                Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
                String sessionId = session.getId();
                log.info("[PolymersDiscountsService][WEBHOOK] Processing checkout.session.completed for sessionId: {}", sessionId);
                PolymersDiscounts discount = discountsRepository.findBySessionId(sessionId);
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
                    log.info("[PolymersDiscountsService][WEBHOOK] Discount updated and saved for sessionId: {}", sessionId);
                    return Map.of(
                        "message", "Discounts record updated for sessionId: " + sessionId,
                        "status", session.getStatus(),
                        "paymentStatus", session.getPaymentStatus()
                    );
                } else {
                    log.warn("[PolymersDiscountsService][WEBHOOK] No discount found for sessionId: {}", sessionId);
                    return Map.of("error", "No Discounts record found for sessionId: " + sessionId);
                }
            } else {
                log.info("[PolymersDiscountsService][WEBHOOK] Unhandled event type: {}", event.getType());
                return Map.of("message", "Unhandled event type: " + event.getType());
            }
        } catch (SignatureVerificationException e) {
            log.error("[PolymersDiscountsService][WEBHOOK] Webhook signature verification failed: {}", e.getMessage(), e);
            return Map.of("error", "Webhook signature verification failed: " + e.getMessage());
        } catch (JsonSyntaxException e) {
            log.error("[PolymersDiscountsService][WEBHOOK] Invalid JSON payload: {}", e.getMessage(), e);
            return Map.of("error", "Invalid JSON payload: " + e.getMessage());
        } catch (Exception e) {
            log.error("[PolymersDiscountsService][WEBHOOK] Error processing webhook: {}", e.getMessage(), e);
            return Map.of("error", "Error processing webhook: " + e.getMessage());
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
        log.info("🎯 [PolymersDiscountsService][WEBHOOK] Processing polymers discount webhook event: {}", eventType);
        try {
            switch (eventType) {
                case "checkout.session.completed":
                    handleDiscountCheckoutSessionCompleted(event);
                    break;
                case "payment_intent.succeeded":
                    handleDiscountPaymentIntentSucceeded(event);
                    break;
                case "payment_intent.payment_failed":
                    handleDiscountPaymentIntentFailed(event);
                    break;
                default:
                    log.info("ℹ️ [PolymersDiscountsService][WEBHOOK] Unhandled polymers discount event type: {}", eventType);
            }
        } catch (Exception e) {
            log.error("❌ [PolymersDiscountsService][WEBHOOK] Error processing polymers discount webhook event: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process polymers discount webhook event", e);
        }
    }
    
    private void handleDiscountCheckoutSessionCompleted(Event event) {
        log.info("🎯 [PolymersDiscountsService][WEBHOOK] Handling polymers discount checkout.session.completed");
        try {
            // Use EventDataObjectDeserializer to get the Session object - same pattern as OpticsStripeService
            com.stripe.model.EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            log.info("🔍 [PolymersDiscountsService][WEBHOOK] EventDataObjectDeserializer available: {}", dataObjectDeserializer != null);
            
            if (dataObjectDeserializer != null && dataObjectDeserializer.getObject().isPresent()) {
                Object deserializedObject = dataObjectDeserializer.getObject().get();
                log.info("� [PolymersDiscountsService][WEBHOOK] Event data object type: {}", deserializedObject.getClass().getSimpleName());
                
                if (deserializedObject instanceof com.stripe.model.checkout.Session session) {
                    String sessionId = session.getId();
                    log.info("✅ [PolymersDiscountsService][WEBHOOK] Successfully retrieved Session: {}", sessionId);
                    
                    // Log key data from the webhook for debugging
                    log.info("� [PolymersDiscountsService][WEBHOOK] Webhook Session Data:");
                    log.info("   - Session ID: {}", session.getId());
                    log.info("   - Amount Total: {} cents", session.getAmountTotal());
                    log.info("   - Currency: {}", session.getCurrency());
                    log.info("   - Customer Email: {}", session.getCustomerEmail());
                    log.info("   - Payment Intent: {}", session.getPaymentIntent());
                    log.info("   - Payment Status: {}", session.getPaymentStatus());
                    log.info("   - Session Status: {}", session.getStatus());
                    
                    // Find the discount record by session ID
                    PolymersDiscounts discount = discountsRepository.findBySessionId(sessionId);
                    if (discount != null) {
                        log.info("✅ [PolymersDiscountsService][WEBHOOK] Found existing discount record with ID: {}, current status: {}", discount.getId(), discount.getStatus());
                        discount.setStatus(PaymentStatus.COMPLETED);
                        discount.setPaymentStatus("paid");
                        if (session.getPaymentIntent() != null) {
                            discount.setPaymentIntentId(session.getPaymentIntent());
                            log.info("✅ [PolymersDiscountsService][WEBHOOK] Updated paymentIntentId to: {}", session.getPaymentIntent());
                        }
                        discount.setUpdatedAt(java.time.LocalDateTime.now());
                        discountsRepository.save(discount);
                        log.info("✅ [PolymersDiscountsService][WEBHOOK] Updated PolymersDiscounts status to COMPLETED for session: {}", sessionId);
                        
                        // Log the updated record details
                        log.info("✅ [PolymersDiscountsService][WEBHOOK] Final record state - ID: {}, status: {}, paymentStatus: {}, paymentIntentId: {}", 
                                discount.getId(), discount.getStatus(), discount.getPaymentStatus(), discount.getPaymentIntentId());
                    } else {
                        log.warn("⚠️ [PolymersDiscountsService][WEBHOOK] No PolymersDiscounts record found for session: {}", sessionId);
                        log.warn("⚠️ [PolymersDiscountsService][WEBHOOK] This should not happen! The record should have been created during session creation.");
                        
                        // List all records to debug
                        java.util.List<PolymersDiscounts> allDiscounts = discountsRepository.findAll();
                        log.info("🔍 [PolymersDiscountsService][WEBHOOK] Total discount records in database: {}", allDiscounts.size());
                        for (PolymersDiscounts d : allDiscounts) {
                            log.info("🔍 [PolymersDiscountsService][WEBHOOK] Existing record - ID: {}, sessionId: {}, paymentIntentId: {}", 
                                    d.getId(), d.getSessionId(), d.getPaymentIntentId());
                        }
                    }
                } else {
                    log.error("❌ [PolymersDiscountsService][WEBHOOK] Event data object is not a Session! Type: {}", deserializedObject.getClass().getName());
                    // Fallback: Try to extract session data manually from event
                    log.info("🔄 [PolymersDiscountsService][WEBHOOK] Attempting manual session data extraction from event...");
                    extractAndProcessSessionDataFromEvent(event);
                }
            } else {
                log.error("❌ [PolymersDiscountsService][WEBHOOK] Failed to deserialize checkout.session.completed event data");
                // Fallback: Try to extract session data manually from event
                log.info("🔄 [PolymersDiscountsService][WEBHOOK] Attempting manual session data extraction from event...");
                extractAndProcessSessionDataFromEvent(event);
            }
        } catch (Exception e) {
            log.error("❌ [PolymersDiscountsService][WEBHOOK] Error handling polymers discount checkout.session.completed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to handle polymers discount checkout session completed", e);
        }
    }
    
    private void handleDiscountPaymentIntentSucceeded(Event event) {
        log.info("🎯 [PolymersDiscountsService][WEBHOOK] Handling polymers discount payment_intent.succeeded");
        try {
            // Use EventDataObjectDeserializer to get the PaymentIntent object - same pattern as OpticsStripeService
            com.stripe.model.EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            log.info("🔍 [PolymersDiscountsService][WEBHOOK] EventDataObjectDeserializer available: {}", dataObjectDeserializer != null);
            
            if (dataObjectDeserializer != null && dataObjectDeserializer.getObject().isPresent()) {
                Object deserializedObject = dataObjectDeserializer.getObject().get();
                log.info("� [PolymersDiscountsService][WEBHOOK] Event data object type: {}", deserializedObject.getClass().getSimpleName());
                
                if (deserializedObject instanceof com.stripe.model.PaymentIntent paymentIntent) {
                    String paymentIntentId = paymentIntent.getId();
                    log.info("✅ [PolymersDiscountsService][WEBHOOK] Successfully retrieved PaymentIntent: {}", paymentIntentId);
                    
                    // Log key data from the webhook for debugging
                    log.info("� [PolymersDiscountsService][WEBHOOK] Webhook PaymentIntent Data:");
                    log.info("   - Payment Intent ID: {}", paymentIntent.getId());
                    log.info("   - Amount: {} cents", paymentIntent.getAmount());
                    log.info("   - Currency: {}", paymentIntent.getCurrency());
                    log.info("   - Status: {}", paymentIntent.getStatus());
                    
                    // Try to find by session ID first (since paymentIntentId might be null in our records)
                    // Look for records with matching metadata
                    java.util.List<PolymersDiscounts> allDiscounts = discountsRepository.findAll();
                    log.info("🔍 [PolymersDiscountsService][WEBHOOK] Total discount records in database: {}", allDiscounts.size());
                    
                    PolymersDiscounts matchingDiscount = null;
                    for (PolymersDiscounts discount : allDiscounts) {
                        log.info("🔍 [PolymersDiscountsService][WEBHOOK] Checking discount ID: {}, sessionId: {}, paymentIntentId: {}", 
                                discount.getId(), discount.getSessionId(), discount.getPaymentIntentId());
                        if (paymentIntentId.equals(discount.getPaymentIntentId())) {
                            matchingDiscount = discount;
                            break;
                        }
                    }
                    
                    // Find the discount record by payment intent ID
                    java.util.Optional<PolymersDiscounts> discountOpt = discountsRepository.findByPaymentIntentId(paymentIntentId);
                    if (discountOpt.isPresent()) {
                        PolymersDiscounts discount = discountOpt.get();
                        log.info("✅ [PolymersDiscountsService][WEBHOOK] Found existing discount record with ID: {}, current status: {}", discount.getId(), discount.getStatus());
                        discount.setStatus(PaymentStatus.COMPLETED);
                        discount.setPaymentStatus("paid");
                        discount.setUpdatedAt(java.time.LocalDateTime.now());
                        discountsRepository.save(discount);
                        log.info("✅ [PolymersDiscountsService][WEBHOOK] Updated PolymersDiscounts status to COMPLETED for payment intent: {}", paymentIntentId);
                    } else {
                        log.warn("⚠️ [PolymersDiscountsService][WEBHOOK] No PolymersDiscounts record found for payment intent: {}", paymentIntentId);
                        log.warn("⚠️ [PolymersDiscountsService][WEBHOOK] Manual search result: {}", matchingDiscount != null ? "Found match with ID: " + matchingDiscount.getId() : "No match found");
                        
                        // If we found a match manually, update it
                        if (matchingDiscount != null) {
                            log.info("✅ [PolymersDiscountsService][WEBHOOK] Using manually found discount record with ID: {}", matchingDiscount.getId());
                            matchingDiscount.setStatus(PaymentStatus.COMPLETED);
                            matchingDiscount.setPaymentStatus("paid");
                            matchingDiscount.setPaymentIntentId(paymentIntentId); // Update the paymentIntentId
                            matchingDiscount.setUpdatedAt(java.time.LocalDateTime.now());
                            discountsRepository.save(matchingDiscount);
                            log.info("✅ [PolymersDiscountsService][WEBHOOK] Updated PolymersDiscounts status to COMPLETED for manually found record");
                        } else {
                            // Fallback: Try to extract payment intent data manually from event
                            log.info("🔄 [PolymersDiscountsService][WEBHOOK] Attempting manual payment intent data extraction from event...");
                            extractAndProcessPaymentIntentDataFromEvent(event);
                        }
                    }
                } else {
                    log.error("❌ [PolymersDiscountsService][WEBHOOK] Event data object is not a PaymentIntent! Type: {}", deserializedObject.getClass().getName());
                    // Fallback: Try to extract payment intent data manually from event
                    log.info("🔄 [PolymersDiscountsService][WEBHOOK] Attempting manual payment intent data extraction from event...");
                    extractAndProcessPaymentIntentDataFromEvent(event);
                }
            } else {
                log.error("❌ [PolymersDiscountsService][WEBHOOK] Failed to deserialize payment_intent.succeeded event data");
                // Fallback: Try to extract payment intent data manually from event
                log.info("🔄 [PolymersDiscountsService][WEBHOOK] Attempting manual payment intent data extraction from event...");
                extractAndProcessPaymentIntentDataFromEvent(event);
            }
        } catch (Exception e) {
            log.error("❌ [PolymersDiscountsService][WEBHOOK] Error handling polymers discount payment_intent.succeeded: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to handle polymers discount payment intent succeeded", e);
        }
    }
    
    private void handleDiscountPaymentIntentFailed(Event event) {
        log.info("🎯 [PolymersDiscountsService][WEBHOOK] Handling polymers discount payment_intent.payment_failed");
        try {
            java.util.Optional<com.stripe.model.StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
            if (stripeObjectOpt.isPresent() && stripeObjectOpt.get() instanceof com.stripe.model.PaymentIntent) {
                com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) stripeObjectOpt.get();
                String paymentIntentId = paymentIntent.getId();
                // Find the discount record by payment intent ID
                java.util.Optional<PolymersDiscounts> discountOpt = discountsRepository.findByPaymentIntentId(paymentIntentId);
                if (discountOpt.isPresent()) {
                    PolymersDiscounts discount = discountOpt.get();
                    discount.setStatus(PaymentStatus.FAILED);
                    discount.setUpdatedAt(java.time.LocalDateTime.now());
                    discountsRepository.save(discount);
                    log.info("✅ [PolymersDiscountsService][WEBHOOK] Updated PolymersDiscounts status to FAILED for payment intent: {}", paymentIntentId);
                } else {
                    log.warn("⚠️ [PolymersDiscountsService][WEBHOOK] No PolymersDiscounts record found for payment intent: {}", paymentIntentId);
                }
            }
        } catch (Exception e) {
            log.error("❌ [PolymersDiscountsService][WEBHOOK] Error handling polymers discount payment_intent.payment_failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to handle polymers discount payment intent failed", e);
        }
    }
    
    /**
     * Fallback method to manually extract session data from the event when deserialization fails
     * This handles the case where EventDataObjectDeserializer doesn't work properly
     */
    private void extractAndProcessSessionDataFromEvent(Event event) {
        try {
            log.info("🔄 [PolymersDiscountsService][WEBHOOK] Starting manual extraction from Event object...");
            
            // Try to get the raw JSON string from the event directly
            String rawEventJson = event.toJson();
            log.info("📋 [PolymersDiscountsService][WEBHOOK] Full Event JSON: {}", rawEventJson);
            
            // Extract key fields manually from the JSON
            String sessionId = extractJsonField(rawEventJson, "id");
            String customerEmail = extractJsonField(rawEventJson, "customer_email");
            String paymentIntent = extractJsonField(rawEventJson, "payment_intent");
            String paymentStatus = extractJsonField(rawEventJson, "payment_status");
            String sessionStatus = extractJsonField(rawEventJson, "status");
            
            log.info("🔍 [PolymersDiscountsService][WEBHOOK] Manually extracted session data:");
            log.info("   - Session ID: {}", sessionId);
            log.info("   - Customer Email: {}", customerEmail);
            log.info("   - Payment Intent: {}", paymentIntent);
            log.info("   - Payment Status: {}", paymentStatus);
            log.info("   - Session Status: {}", sessionStatus);
            
            if (sessionId != null && !sessionId.isEmpty()) {
                // Find the discount record by session ID
                PolymersDiscounts discount = discountsRepository.findBySessionId(sessionId);
                if (discount != null) {
                    log.info("✅ [PolymersDiscountsService][WEBHOOK] Found existing discount record with manual extraction - ID: {}, current status: {}", discount.getId(), discount.getStatus());
                    discount.setStatus(PaymentStatus.COMPLETED);
                    discount.setPaymentStatus("paid");
                    if (paymentIntent != null && !paymentIntent.isEmpty()) {
                        discount.setPaymentIntentId(paymentIntent);
                        log.info("✅ [PolymersDiscountsService][WEBHOOK] Updated paymentIntentId to: {}", paymentIntent);
                    }
                    discount.setUpdatedAt(java.time.LocalDateTime.now());
                    discountsRepository.save(discount);
                    log.info("✅ [PolymersDiscountsService][WEBHOOK] Updated PolymersDiscounts status to COMPLETED for session (manual): {}", sessionId);
                } else {
                    log.warn("⚠️ [PolymersDiscountsService][WEBHOOK] No PolymersDiscounts record found for session (manual): {}", sessionId);
                }
            } else {
                log.error("❌ [PolymersDiscountsService][WEBHOOK] Could not extract session ID from event JSON");
            }
        } catch (Exception e) {
            log.error("❌ [PolymersDiscountsService][WEBHOOK] Error in manual session data extraction: {}", e.getMessage(), e);
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
            log.warn("⚠️ [PolymersDiscountsService][WEBHOOK] Could not extract field '{}' from JSON: {}", fieldName, e.getMessage());
        }
        return null;
    }
    
    /**
     * Fallback method to manually extract payment intent data from the event when deserialization fails
     */
    private void extractAndProcessPaymentIntentDataFromEvent(Event event) {
        try {
            log.info("🔄 [PolymersDiscountsService][WEBHOOK] Starting manual payment intent extraction from Event object...");
            
            // Try to get the raw JSON string from the event directly
            String rawEventJson = event.toJson();
            log.info("📋 [PolymersDiscountsService][WEBHOOK] Full Event JSON: {}", rawEventJson);
            
            // Extract key fields manually from the JSON
            String paymentIntentId = extractJsonField(rawEventJson, "id");
            String status = extractJsonField(rawEventJson, "status");
            String amount = extractJsonField(rawEventJson, "amount");
            String currency = extractJsonField(rawEventJson, "currency");
            
            log.info("🔍 [PolymersDiscountsService][WEBHOOK] Manually extracted payment intent data:");
            log.info("   - Payment Intent ID: {}", paymentIntentId);
            log.info("   - Status: {}", status);
            log.info("   - Amount: {}", amount);
            log.info("   - Currency: {}", currency);
            
            if (paymentIntentId != null && !paymentIntentId.isEmpty()) {
                // First try to find by payment intent ID
                java.util.Optional<PolymersDiscounts> discountOpt = discountsRepository.findByPaymentIntentId(paymentIntentId);
                if (discountOpt.isPresent()) {
                    PolymersDiscounts discount = discountOpt.get();
                    log.info("✅ [PolymersDiscountsService][WEBHOOK] Found existing discount record with manual extraction - ID: {}, current status: {}", discount.getId(), discount.getStatus());
                    discount.setStatus(PaymentStatus.COMPLETED);
                    discount.setPaymentStatus("paid");
                    discount.setUpdatedAt(java.time.LocalDateTime.now());
                    discountsRepository.save(discount);
                    log.info("✅ [PolymersDiscountsService][WEBHOOK] Updated PolymersDiscounts status to COMPLETED for payment intent (manual): {}", paymentIntentId);
                } else {
                    // Try to find by checking all records manually
                    java.util.List<PolymersDiscounts> allDiscounts = discountsRepository.findAll();
                    for (PolymersDiscounts discount : allDiscounts) {
                        if (paymentIntentId.equals(discount.getPaymentIntentId())) {
                            log.info("✅ [PolymersDiscountsService][WEBHOOK] Found matching discount via manual search - ID: {}", discount.getId());
                            discount.setStatus(PaymentStatus.COMPLETED);
                            discount.setPaymentStatus("paid");
                            discount.setUpdatedAt(java.time.LocalDateTime.now());
                            discountsRepository.save(discount);
                            log.info("✅ [PolymersDiscountsService][WEBHOOK] Updated PolymersDiscounts status to COMPLETED via manual search");
                            return;
                        }
                    }
                    log.warn("⚠️ [PolymersDiscountsService][WEBHOOK] No PolymersDiscounts record found for payment intent (manual): {}", paymentIntentId);
                }
            } else {
                log.error("❌ [PolymersDiscountsService][WEBHOOK] Could not extract payment intent ID from event JSON");
            }
        } catch (Exception e) {
            log.error("❌ [PolymersDiscountsService][WEBHOOK] Error in manual payment intent data extraction: {}", e.getMessage(), e);
        }
    }
}
