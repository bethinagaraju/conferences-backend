package com.zn.payment.nursing.service;

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
import com.zn.payment.nursing.entity.NursingDiscounts;
import com.zn.payment.nursing.entity.NursingPaymentRecord;
import com.zn.payment.nursing.entity.NursingPaymentRecord.PaymentStatus;
import com.zn.payment.nursing.repository.NursingDiscountsRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
public class NursingDiscountsService {
    /**
     * Update payment status in NursingDiscounts by Stripe session ID
     */
    public boolean updatePaymentStatusBySessionId(String sessionId, String status) {
        NursingDiscounts discount = discountsRepository.findBySessionId(sessionId);
        if (discount != null) {
            System.out.println("[NursingDiscountsService] Found discount for sessionId: " + sessionId);
            System.out.println("[NursingDiscountsService] Updating paymentStatus to: " + status);
            discount.setPaymentStatus(status);
            discountsRepository.save(discount);
            System.out.println("[NursingDiscountsService] Discount updated and saved for sessionId: " + sessionId);
            return true;
        } else {
            System.out.println("[NursingDiscountsService] No discount found for sessionId: " + sessionId);
        }
        return false;
    }

    /**
     * Update payment status in NursingDiscounts by Stripe payment intent ID
     */
    public boolean updatePaymentStatusByPaymentIntentId(String paymentIntentId, String status) {
        java.util.Optional<NursingDiscounts> discountOpt = discountsRepository.findByPaymentIntentId(paymentIntentId);
        if (discountOpt.isPresent()) {
            NursingDiscounts discount = discountOpt.get();
            System.out.println("[NursingDiscountsService] Found discount for paymentIntentId: " + paymentIntentId);
            System.out.println("[NursingDiscountsService] Updating paymentStatus to: " + status);
            discount.setPaymentStatus(status);
            discountsRepository.save(discount);
            System.out.println("[NursingDiscountsService] Discount updated and saved for paymentIntentId: " + paymentIntentId);
            return true;
        } else {
            System.out.println("[NursingDiscountsService] No discount found for paymentIntentId: " + paymentIntentId);
        }
        return false;
    }
      @Value("${stripe.api.secret.key}")
    private String secretKey;

    @Value("${stripe.discount.webhook}")
    private String webhookSecret;

    @Autowired
    private NursingDiscountsRepository discountsRepository;

    public Object createSession(CreateDiscountSessionRequest request) {
        // Validate request
        if (request.getUnitAmount() == null || request.getUnitAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Map.of("error", "Unit amount must be positive");
        }
        if (request.getCurrency() == null || request.getCurrency().isEmpty()) {
            return Map.of("error", "Currency must be provided");
        }

        NursingDiscounts discount = new NursingDiscounts();
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

        try {
            // Verify the webhook signature
            Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

            // Handle the event
            if ("checkout.session.completed".equals(event.getType())) {
                Session session = (Session) event.getDataObjectDeserializer().getObject().orElse(null);
                // Update Discounts record in DB based on Stripe session
                String sessionId = session.getId();
                NursingDiscounts discount = discountsRepository.findBySessionId(sessionId);
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
        System.out.println("🎯 Processing nursing discount webhook event: " + eventType);
        
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
                    System.out.println("ℹ️ Unhandled nursing discount event type: " + eventType);
            }
        } catch (Exception e) {
            System.err.println("❌ Error processing nursing discount webhook event: " + e.getMessage());
            throw new RuntimeException("Failed to process nursing discount webhook event", e);
        }
    }
    
    private void handleDiscountCheckoutSessionCompleted(Event event) {
        log.info("🎯 [NursingDiscountsService][WEBHOOK] Handling nursing discount checkout.session.completed");
        
        try {
            // Try using EventDataObjectDeserializer for robust event processing
            com.stripe.model.EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            log.info("🔍 [NursingDiscountsService][WEBHOOK] Created EventDataObjectDeserializer for event type: {}", event.getType());
            
            java.util.Optional<com.stripe.model.StripeObject> stripeObjectOpt = dataObjectDeserializer.getObject();
            log.info("🔍 [NursingDiscountsService][WEBHOOK] Deserializer returned object present: {}", stripeObjectOpt.isPresent());
            
            if (stripeObjectOpt.isPresent() && stripeObjectOpt.get() instanceof com.stripe.model.checkout.Session) {
                com.stripe.model.checkout.Session session = (com.stripe.model.checkout.Session) stripeObjectOpt.get();
                String sessionId = session.getId();
                String customerEmail = session.getCustomerEmail();
                String paymentIntent = session.getPaymentIntent();
                String paymentStatus = session.getPaymentStatus() != null ? session.getPaymentStatus() : "unknown";
                
                log.info("🔍 [NursingDiscountsService][WEBHOOK] Successfully extracted session data:");
                log.info("   - Session ID: {}", sessionId);
                log.info("   - Customer Email: {}", customerEmail);
                log.info("   - Payment Intent: {}", paymentIntent);
                log.info("   - Payment Status: {}", paymentStatus);
                log.info("   - Session Status: {}", session.getStatus());
                
                // Find the discount record by session ID
                NursingDiscounts discount = discountsRepository.findBySessionId(sessionId);
                if (discount != null) {
                    log.info("✅ [NursingDiscountsService][WEBHOOK] Found existing discount record - ID: {}, current status: {}", discount.getId(), discount.getStatus());
                    discount.setStatus(NursingPaymentRecord.PaymentStatus.COMPLETED);
                    discount.setPaymentStatus("paid");
                    if (paymentIntent != null && !paymentIntent.isEmpty()) {
                        discount.setPaymentIntentId(paymentIntent);
                        log.info("✅ [NursingDiscountsService][WEBHOOK] Updated paymentIntentId to: {}", paymentIntent);
                    }
                    discount.setUpdatedAt(java.time.LocalDateTime.now());
                    discountsRepository.save(discount);
                    log.info("✅ [NursingDiscountsService][WEBHOOK] Updated NursingDiscounts status to COMPLETED for session: {}", sessionId);
                } else {
                    log.warn("⚠️ [NursingDiscountsService][WEBHOOK] No NursingDiscounts record found for session: {}", sessionId);
                }
            } else {
                log.warn("⚠️ [NursingDiscountsService][WEBHOOK] Failed to deserialize session object, attempting fallback extraction...");
                extractAndProcessSessionDataFromEvent(event);
            }
        } catch (Exception e) {
            log.error("❌ [NursingDiscountsService][WEBHOOK] Error handling nursing discount checkout.session.completed: {}", e.getMessage(), e);
            log.info("🔄 [NursingDiscountsService][WEBHOOK] Attempting fallback extraction due to error...");
            extractAndProcessSessionDataFromEvent(event);
        }
    }
    
    private void handleDiscountPaymentIntentSucceeded(Event event) {
        log.info("🎯 [NursingDiscountsService][WEBHOOK] Handling nursing discount payment_intent.succeeded");
        
        try {
            // Try using EventDataObjectDeserializer for robust event processing
            com.stripe.model.EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
            log.info("🔍 [NursingDiscountsService][WEBHOOK] Created EventDataObjectDeserializer for payment_intent event");
            
            java.util.Optional<com.stripe.model.StripeObject> stripeObjectOpt = dataObjectDeserializer.getObject();
            log.info("🔍 [NursingDiscountsService][WEBHOOK] Deserializer returned object present: {}", stripeObjectOpt.isPresent());
            
            if (stripeObjectOpt.isPresent() && stripeObjectOpt.get() instanceof com.stripe.model.PaymentIntent) {
                com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) stripeObjectOpt.get();
                String paymentIntentId = paymentIntent.getId();
                String status = paymentIntent.getStatus();
                Long amount = paymentIntent.getAmount();
                String currency = paymentIntent.getCurrency();
                
                log.info("🔍 [NursingDiscountsService][WEBHOOK] Successfully extracted payment intent data:");
                log.info("   - Payment Intent ID: {}", paymentIntentId);
                log.info("   - Status: {}", status);
                log.info("   - Amount: {}", amount);
                log.info("   - Currency: {}", currency);
                
                // First try to find by payment intent ID
                java.util.Optional<NursingDiscounts> discountOpt = discountsRepository.findByPaymentIntentId(paymentIntentId);
                if (discountOpt.isPresent()) {
                    NursingDiscounts discount = discountOpt.get();
                    log.info("✅ [NursingDiscountsService][WEBHOOK] Found existing discount record - ID: {}, current status: {}", discount.getId(), discount.getStatus());
                    discount.setStatus(NursingPaymentRecord.PaymentStatus.COMPLETED);
                    discount.setPaymentStatus("paid");
                    discount.setUpdatedAt(java.time.LocalDateTime.now());
                    discountsRepository.save(discount);
                    log.info("✅ [NursingDiscountsService][WEBHOOK] Updated NursingDiscounts status to COMPLETED for payment intent: {}", paymentIntentId);
                } else {
                    log.warn("⚠️ [NursingDiscountsService][WEBHOOK] No NursingDiscounts record found by payment intent ID: {}", paymentIntentId);
                    
                    // Try to find by checking all records manually
                    log.info("🔍 [NursingDiscountsService][WEBHOOK] Attempting manual search through all discount records...");
                    java.util.List<NursingDiscounts> allDiscounts = discountsRepository.findAll();
                    log.info("🔍 [NursingDiscountsService][WEBHOOK] Found {} total discount records to search", allDiscounts.size());
                    
                    for (NursingDiscounts discount : allDiscounts) {
                        if (paymentIntentId.equals(discount.getPaymentIntentId())) {
                            log.info("✅ [NursingDiscountsService][WEBHOOK] Found matching discount via manual search - ID: {}", discount.getId());
                            discount.setStatus(NursingPaymentRecord.PaymentStatus.COMPLETED);
                            discount.setPaymentStatus("paid");
                            discount.setUpdatedAt(java.time.LocalDateTime.now());
                            discountsRepository.save(discount);
                            log.info("✅ [NursingDiscountsService][WEBHOOK] Updated NursingDiscounts status to COMPLETED via manual search");
                            return;
                        }
                    }
                    log.warn("⚠️ [NursingDiscountsService][WEBHOOK] No matching discount found even with manual search for payment intent: {}", paymentIntentId);
                }
            } else {
                log.warn("⚠️ [NursingDiscountsService][WEBHOOK] Failed to deserialize payment intent object, attempting fallback extraction...");
                extractAndProcessPaymentIntentDataFromEvent(event);
            }
        } catch (Exception e) {
            log.error("❌ [NursingDiscountsService][WEBHOOK] Error handling nursing discount payment_intent.succeeded: {}", e.getMessage(), e);
            log.info("🔄 [NursingDiscountsService][WEBHOOK] Attempting fallback extraction due to error...");
            extractAndProcessPaymentIntentDataFromEvent(event);
        }
    }
    
    private void handleDiscountPaymentIntentFailed(Event event) {
        log.info("🎯 Handling nursing discount payment_intent.payment_failed");
        
        try {
            java.util.Optional<com.stripe.model.StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
            if (stripeObjectOpt.isPresent() && stripeObjectOpt.get() instanceof com.stripe.model.PaymentIntent) {
                com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) stripeObjectOpt.get();
                String paymentIntentId = paymentIntent.getId();
                
                // Find the discount record by payment intent ID
                java.util.Optional<NursingDiscounts> discountOpt = discountsRepository.findByPaymentIntentId(paymentIntentId);
                if (discountOpt.isPresent()) {
                    NursingDiscounts discount = discountOpt.get();
                    discount.setStatus(NursingPaymentRecord.PaymentStatus.FAILED);
                    discount.setUpdatedAt(java.time.LocalDateTime.now());
                    discountsRepository.save(discount);
                    log.info("✅ Updated NursingDiscounts status to FAILED for payment intent: {}", paymentIntentId);
                } else {
                    log.warn("⚠️ No NursingDiscounts record found for payment intent: {}", paymentIntentId);
                }
            }
        } catch (Exception e) {
            log.error("❌ Error handling nursing discount payment_intent.payment_failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to handle nursing discount payment intent failed", e);
        }
    }
    
    /**
     * Fallback method to manually extract session data from the event when deserialization fails
     * This handles the case where EventDataObjectDeserializer doesn't work properly
     */
    private void extractAndProcessSessionDataFromEvent(Event event) {
        try {
            log.info("🔄 [NursingDiscountsService][WEBHOOK] Starting manual extraction from Event object...");
            
            // Try to get the raw JSON string from the event directly
            String rawEventJson = event.toJson();
            log.info("📋 [NursingDiscountsService][WEBHOOK] Full Event JSON: {}", rawEventJson);
            
            // Extract key fields manually from the JSON
            String sessionId = extractJsonField(rawEventJson, "id");
            String customerEmail = extractJsonField(rawEventJson, "customer_email");
            String paymentIntent = extractJsonField(rawEventJson, "payment_intent");
            String paymentStatus = extractJsonField(rawEventJson, "payment_status");
            String sessionStatus = extractJsonField(rawEventJson, "status");
            
            log.info("🔍 [NursingDiscountsService][WEBHOOK] Manually extracted session data:");
            log.info("   - Session ID: {}", sessionId);
            log.info("   - Customer Email: {}", customerEmail);
            log.info("   - Payment Intent: {}", paymentIntent);
            log.info("   - Payment Status: {}", paymentStatus);
            log.info("   - Session Status: {}", sessionStatus);
            
            if (sessionId != null && !sessionId.isEmpty()) {
                // Find the discount record by session ID
                NursingDiscounts discount = discountsRepository.findBySessionId(sessionId);
                if (discount != null) {
                    log.info("✅ [NursingDiscountsService][WEBHOOK] Found existing discount record with manual extraction - ID: {}, current status: {}", discount.getId(), discount.getStatus());
                    discount.setStatus(NursingPaymentRecord.PaymentStatus.COMPLETED);
                    discount.setPaymentStatus("paid");
                    if (paymentIntent != null && !paymentIntent.isEmpty()) {
                        discount.setPaymentIntentId(paymentIntent);
                        log.info("✅ [NursingDiscountsService][WEBHOOK] Updated paymentIntentId to: {}", paymentIntent);
                    }
                    discount.setUpdatedAt(java.time.LocalDateTime.now());
                    discountsRepository.save(discount);
                    log.info("✅ [NursingDiscountsService][WEBHOOK] Updated NursingDiscounts status to COMPLETED for session (manual): {}", sessionId);
                } else {
                    log.warn("⚠️ [NursingDiscountsService][WEBHOOK] No NursingDiscounts record found for session (manual): {}", sessionId);
                }
            } else {
                log.error("❌ [NursingDiscountsService][WEBHOOK] Could not extract session ID from event JSON");
            }
        } catch (Exception e) {
            log.error("❌ [NursingDiscountsService][WEBHOOK] Error in manual session data extraction: {}", e.getMessage(), e);
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
            log.warn("⚠️ [NursingDiscountsService][WEBHOOK] Could not extract field '{}' from JSON: {}", fieldName, e.getMessage());
        }
        return null;
    }
    
    /**
     * Fallback method to manually extract payment intent data from the event when deserialization fails
     */
    private void extractAndProcessPaymentIntentDataFromEvent(Event event) {
        try {
            log.info("🔄 [NursingDiscountsService][WEBHOOK] Starting manual payment intent extraction from Event object...");
            
            // Try to get the raw JSON string from the event directly
            String rawEventJson = event.toJson();
            log.info("📋 [NursingDiscountsService][WEBHOOK] Full Event JSON: {}", rawEventJson);
            
            // Extract key fields manually from the JSON
            String paymentIntentId = extractJsonField(rawEventJson, "id");
            String status = extractJsonField(rawEventJson, "status");
            String amount = extractJsonField(rawEventJson, "amount");
            String currency = extractJsonField(rawEventJson, "currency");
            
            log.info("🔍 [NursingDiscountsService][WEBHOOK] Manually extracted payment intent data:");
            log.info("   - Payment Intent ID: {}", paymentIntentId);
            log.info("   - Status: {}", status);
            log.info("   - Amount: {}", amount);
            log.info("   - Currency: {}", currency);
            
            if (paymentIntentId != null && !paymentIntentId.isEmpty()) {
                // First try to find by payment intent ID
                java.util.Optional<NursingDiscounts> discountOpt = discountsRepository.findByPaymentIntentId(paymentIntentId);
                if (discountOpt.isPresent()) {
                    NursingDiscounts discount = discountOpt.get();
                    log.info("✅ [NursingDiscountsService][WEBHOOK] Found existing discount record with manual extraction - ID: {}, current status: {}", discount.getId(), discount.getStatus());
                    discount.setStatus(NursingPaymentRecord.PaymentStatus.COMPLETED);
                    discount.setPaymentStatus("paid");
                    discount.setUpdatedAt(java.time.LocalDateTime.now());
                    discountsRepository.save(discount);
                    log.info("✅ [NursingDiscountsService][WEBHOOK] Updated NursingDiscounts status to COMPLETED for payment intent (manual): {}", paymentIntentId);
                } else {
                    // Try to find by checking all records manually
                    java.util.List<NursingDiscounts> allDiscounts = discountsRepository.findAll();
                    for (NursingDiscounts discount : allDiscounts) {
                        if (paymentIntentId.equals(discount.getPaymentIntentId())) {
                            log.info("✅ [NursingDiscountsService][WEBHOOK] Found matching discount via manual search - ID: {}", discount.getId());
                            discount.setStatus(NursingPaymentRecord.PaymentStatus.COMPLETED);
                            discount.setPaymentStatus("paid");
                            discount.setUpdatedAt(java.time.LocalDateTime.now());
                            discountsRepository.save(discount);
                            log.info("✅ [NursingDiscountsService][WEBHOOK] Updated NursingDiscounts status to COMPLETED via manual search");
                            return;
                        }
                    }
                    log.warn("⚠️ [NursingDiscountsService][WEBHOOK] No NursingDiscounts record found for payment intent (manual): {}", paymentIntentId);
                }
            } else {
                log.error("❌ [NursingDiscountsService][WEBHOOK] Could not extract payment intent ID from event JSON");
            }
        } catch (Exception e) {
            log.error("❌ [NursingDiscountsService][WEBHOOK] Error in manual payment intent data extraction: {}", e.getMessage(), e);
        }
    }
}
