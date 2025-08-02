package com.zn.payment.controller;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.model.Event;
import com.zn.payment.dto.CreateDiscountSessionRequest;
import com.zn.payment.nursing.service.NursingDiscountsService;
import com.zn.payment.optics.service.OpticsDiscountsService;
import com.zn.payment.renewable.service.RenewableDiscountsService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;



@RestController
@RequestMapping("/api/discounts")
@Slf4j
public class DiscountsController {
    @Autowired
    private OpticsDiscountsService opticsDiscountsService;
    
    @Autowired
    private NursingDiscountsService nursingDiscountsService;
    
    @Autowired
    private RenewableDiscountsService renewableDiscountsService;

    @Autowired
    private com.zn.payment.polymers.service.PolymersDiscountsService polymersDiscountsService;

    // create stripe session
    @PostMapping("/create-session")
    public ResponseEntity<?> createSession(@RequestBody CreateDiscountSessionRequest request, HttpServletRequest httpRequest) {
        String origin = httpRequest.getHeader("Origin");
        String referer = httpRequest.getHeader("Referer");
        
        // Route based on domain
        if ((origin != null && origin.contains("globallopmeet.com")) || 
            (referer != null && referer.contains("globallopmeet.com"))) {
            // Route to Optics service
            Object result = opticsDiscountsService.createSession(request);
            return ResponseEntity.ok(result);
        } else if ((origin != null && origin.contains("polyscienceconference.com")) || 
                   (referer != null && referer.contains("polyscienceconference.com"))) {
            // Route to Polymers service
            Object result = polymersDiscountsService.createSession(request);
            return ResponseEntity.ok(result);
        } else if ((origin != null && origin.contains("nursingmeet2026.com")) || 
                   (referer != null && referer.contains("nursingmeet2026.com"))) {
            // Route to Nursing service
            Object result = nursingDiscountsService.createSession(request);
            return ResponseEntity.ok(result);
        } else if ((origin != null && origin.contains("globalrenewablemeet.com")) || 
                   (referer != null && referer.contains("globalrenewablemeet.com"))) {
            // Route to Renewable service
            Object result = renewableDiscountsService.createSession(request);
            return ResponseEntity.ok(result);
        } else {
            // Default to nursing service for backward compatibility
            Object result = nursingDiscountsService.createSession(request);
            return ResponseEntity.ok(result);
        }
    }
    
    // handle stripe webhook - ONLY for discount payments
    @PostMapping("/webhook")
    public ResponseEntity<String> handleStripeWebhook(HttpServletRequest request) throws IOException {
        log.info("##############    Received DISCOUNT webhook request  ##################");
        String payload;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()))) {
            payload = reader.lines().collect(Collectors.joining("\n"));
        }
        String sigHeader = request.getHeader("Stripe-Signature");
        log.info("Discount webhook received. Signature header present: {}", sigHeader != null);
        if (sigHeader == null || sigHeader.isEmpty()) {
            log.error("Missing Stripe-Signature header in discount webhook");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing signature header");
        }
        try {
            // Try to construct event using polymers service first since it's most likely to be polymers
            Event event = null;
            
            // First, try polymers (most common for discount webhooks)
            try {
                event = polymersDiscountsService.constructWebhookEvent(payload, sigHeader);
                log.info("✅ Webhook event successfully constructed using PolymersDiscountsService");
            } catch (Exception e) {
                log.warn("Polymers constructWebhookEvent failed: {}. Trying other services.", e.getMessage());
            }
            
            // If polymers failed, try other services
            if (event == null) {
                try {
                    event = opticsDiscountsService.constructWebhookEvent(payload, sigHeader);
                    log.info("✅ Webhook event successfully constructed using OpticsDiscountsService");
                } catch (Exception e) {
                    try {
                        event = nursingDiscountsService.constructWebhookEvent(payload, sigHeader);
                        log.info("✅ Webhook event successfully constructed using NursingDiscountsService");
                    } catch (Exception e2) {
                        event = renewableDiscountsService.constructWebhookEvent(payload, sigHeader);
                        log.info("✅ Webhook event successfully constructed using RenewableDiscountsService");
                    }
                }
            }
            
            if (event == null) {
                log.error("❌ Failed to parse webhook event with any service");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to parse event");
            }

            // Extract metadata to determine which service to use for processing
            String source = null;
            String paymentType = null;
            String productName = null;
            
            try {
                java.util.Optional<com.stripe.model.StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
                if (stripeObjectOpt.isPresent()) {
                    com.stripe.model.StripeObject stripeObject = stripeObjectOpt.get();
                    java.util.Map<String, String> metadata = null;
                    try {
                        java.lang.reflect.Method getMetadata = stripeObject.getClass().getMethod("getMetadata");
                        Object metaObj = getMetadata.invoke(stripeObject);
                        if (metaObj instanceof java.util.Map) {
                            @SuppressWarnings("unchecked")
                            java.util.Map<String, String> metadataMap = (java.util.Map<String, String>) metaObj;
                            metadata = metadataMap;
                            if (metadata != null) {
                                source = metadata.get("source");
                                paymentType = metadata.get("paymentType");
                                productName = metadata.get("productName");
                                log.info("📋 [Discount Webhook] Extracted metadata - source: {}, paymentType: {}, productName: {}", source, paymentType, productName);
                            }
                        }
                    } catch (Exception ex) {
                        log.warn("Could not extract metadata from stripe object: {}", ex.getMessage());
                    }
                }
            } catch (Exception ex) {
                log.warn("Could not extract metadata from event: {}", ex.getMessage());
            }

            // Process the webhook event
            String eventType = event.getType();
            log.info("🎯 Processing discount webhook event: {}", eventType);
            boolean updated = false;

            // Determine which service to use based on metadata and try polymers first
            boolean usePolymers = true; // Default to polymers for discount webhooks
            
            if (productName != null) {
                String productUpper = productName.toUpperCase();
                if (productUpper.contains("OPTICS")) {
                    usePolymers = false;
                } else if (productUpper.contains("NURSING")) {
                    usePolymers = false;
                } else if (productUpper.contains("RENEWABLE")) {
                    usePolymers = false;
                }
            }
            
            // Try polymers first (most discount webhooks are polymers)
            if (usePolymers) {
                // Use the comprehensive processWebhookEvent method in PolymersDiscountsService
                try {
                    polymersDiscountsService.processWebhookEvent(event);
                    log.info("✅ [PolymersDiscountsService] Successfully processed discount webhook event: {}", eventType);
                    
                    // Additional direct update logic based on event type - same pattern as payment webhook
                    if ("checkout.session.completed".equals(eventType)) {
                        java.util.Optional<com.stripe.model.StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
                        if (stripeObjectOpt.isPresent() && stripeObjectOpt.get() instanceof com.stripe.model.checkout.Session) {
                            com.stripe.model.checkout.Session session = (com.stripe.model.checkout.Session) stripeObjectOpt.get();
                            if (polymersDiscountsService.updatePaymentStatusBySessionId(session.getId(), "paid")) {
                                log.info("✅ [PolymersDiscountsService] Additional direct update completed for sessionId: {}", session.getId());
                            }
                        }
                    } else if ("payment_intent.succeeded".equals(eventType)) {
                        java.util.Optional<com.stripe.model.StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
                        if (stripeObjectOpt.isPresent() && stripeObjectOpt.get() instanceof com.stripe.model.PaymentIntent) {
                            com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) stripeObjectOpt.get();
                            if (polymersDiscountsService.updatePaymentStatusByPaymentIntentId(paymentIntent.getId(), "paid")) {
                                log.info("✅ [PolymersDiscountsService] Additional direct update completed for paymentIntentId: {}", paymentIntent.getId());
                            }
                        }
                    } else if ("payment_intent.payment_failed".equals(eventType)) {
                        java.util.Optional<com.stripe.model.StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
                        if (stripeObjectOpt.isPresent() && stripeObjectOpt.get() instanceof com.stripe.model.PaymentIntent) {
                            com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) stripeObjectOpt.get();
                            if (polymersDiscountsService.updatePaymentStatusByPaymentIntentId(paymentIntent.getId(), "FAILED")) {
                                log.info("✅ [PolymersDiscountsService] Additional direct update completed for paymentIntentId: {}", paymentIntent.getId());
                            }
                        }
                    }
                    
                    updated = true;
                } catch (Exception e) {
                    log.warn("⚠️ [PolymersDiscountsService] Failed to process webhook event {}: {}", eventType, e.getMessage());
                    // If polymers service fails, this might not be a polymers discount, continue to other services
                }
            } else {
                // Use other services if not polymers based on productName metadata
                log.info("🔄 Using non-polymers services for discount webhook processing");
                
                // Try to process with each service using their comprehensive processWebhookEvent methods
                boolean processed = false;
                
                // Try Optics first
                if (!processed && productName != null && productName.toUpperCase().contains("OPTICS")) {
                    try {
                        opticsDiscountsService.processWebhookEvent(event);
                        log.info("✅ [OpticsDiscountsService] Successfully processed discount webhook event: {}", eventType);
                        
                        // Additional direct update logic - same pattern as payment webhook
                        if ("checkout.session.completed".equals(eventType)) {
                            java.util.Optional<com.stripe.model.StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
                            if (stripeObjectOpt.isPresent() && stripeObjectOpt.get() instanceof com.stripe.model.checkout.Session) {
                                com.stripe.model.checkout.Session session = (com.stripe.model.checkout.Session) stripeObjectOpt.get();
                                if (opticsDiscountsService.updatePaymentStatusBySessionId(session.getId(), "paid")) {
                                    log.info("✅ [OpticsDiscountsService] Additional direct update completed for sessionId: {}", session.getId());
                                }
                            }
                        } else if ("payment_intent.succeeded".equals(eventType)) {
                            java.util.Optional<com.stripe.model.StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
                            if (stripeObjectOpt.isPresent() && stripeObjectOpt.get() instanceof com.stripe.model.PaymentIntent) {
                                com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) stripeObjectOpt.get();
                                if (opticsDiscountsService.updatePaymentStatusByPaymentIntentId(paymentIntent.getId(), "paid")) {
                                    log.info("✅ [OpticsDiscountsService] Additional direct update completed for paymentIntentId: {}", paymentIntent.getId());
                                }
                            }
                        }
                        
                        processed = true;
                        updated = true;
                    } catch (Exception e) {
                        log.warn("⚠️ [OpticsDiscountsService] Failed to process webhook event {}: {}", eventType, e.getMessage());
                    }
                }
                
                // Try Nursing if not processed
                if (!processed && productName != null && productName.toUpperCase().contains("NURSING")) {
                    try {
                        nursingDiscountsService.processWebhookEvent(event);
                        log.info("✅ [NursingDiscountsService] Successfully processed discount webhook event: {}", eventType);
                        
                        // Additional direct update logic - same pattern as payment webhook
                        if ("checkout.session.completed".equals(eventType)) {
                            java.util.Optional<com.stripe.model.StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
                            if (stripeObjectOpt.isPresent() && stripeObjectOpt.get() instanceof com.stripe.model.checkout.Session) {
                                com.stripe.model.checkout.Session session = (com.stripe.model.checkout.Session) stripeObjectOpt.get();
                                if (nursingDiscountsService.updatePaymentStatusBySessionId(session.getId(), "paid")) {
                                    log.info("✅ [NursingDiscountsService] Additional direct update completed for sessionId: {}", session.getId());
                                }
                            }
                        } else if ("payment_intent.succeeded".equals(eventType)) {
                            java.util.Optional<com.stripe.model.StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
                            if (stripeObjectOpt.isPresent() && stripeObjectOpt.get() instanceof com.stripe.model.PaymentIntent) {
                                com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) stripeObjectOpt.get();
                                if (nursingDiscountsService.updatePaymentStatusByPaymentIntentId(paymentIntent.getId(), "paid")) {
                                    log.info("✅ [NursingDiscountsService] Additional direct update completed for paymentIntentId: {}", paymentIntent.getId());
                                }
                            }
                        }
                        
                        processed = true;
                        updated = true;
                    } catch (Exception e) {
                        log.warn("⚠️ [NursingDiscountsService] Failed to process webhook event {}: {}", eventType, e.getMessage());
                    }
                }
                
                // Try Renewable if not processed
                if (!processed && productName != null && productName.toUpperCase().contains("RENEWABLE")) {
                    try {
                        renewableDiscountsService.processWebhookEvent(event);
                        log.info("✅ [RenewableDiscountsService] Successfully processed discount webhook event: {}", eventType);
                        
                        // Additional direct update logic - same pattern as payment webhook
                        if ("checkout.session.completed".equals(eventType)) {
                            java.util.Optional<com.stripe.model.StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
                            if (stripeObjectOpt.isPresent() && stripeObjectOpt.get() instanceof com.stripe.model.checkout.Session) {
                                com.stripe.model.checkout.Session session = (com.stripe.model.checkout.Session) stripeObjectOpt.get();
                                if (renewableDiscountsService.updatePaymentStatusBySessionId(session.getId(), "paid")) {
                                    log.info("✅ [RenewableDiscountsService] Additional direct update completed for sessionId: {}", session.getId());
                                }
                            }
                        } else if ("payment_intent.succeeded".equals(eventType)) {
                            java.util.Optional<com.stripe.model.StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
                            if (stripeObjectOpt.isPresent() && stripeObjectOpt.get() instanceof com.stripe.model.PaymentIntent) {
                                com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) stripeObjectOpt.get();
                                if (renewableDiscountsService.updatePaymentStatusByPaymentIntentId(paymentIntent.getId(), "paid")) {
                                    log.info("✅ [RenewableDiscountsService] Additional direct update completed for paymentIntentId: {}", paymentIntent.getId());
                                }
                            }
                        }
                        
                        processed = true;
                        updated = true;
                    } catch (Exception e) {
                        log.warn("⚠️ [RenewableDiscountsService] Failed to process webhook event {}: {}", eventType, e.getMessage());
                    }
                }
                
                // If still not processed, try all services as fallback
                if (!processed) {
                    log.info("🔄 Trying all services as fallback for discount webhook processing");
                    try {
                        opticsDiscountsService.processWebhookEvent(event);
                        log.info("✅ [OpticsDiscountsService] Successfully processed discount webhook as fallback: {}", eventType);
                        
                        // Try direct update as well
                        if ("checkout.session.completed".equals(eventType)) {
                            java.util.Optional<com.stripe.model.StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
                            if (stripeObjectOpt.isPresent() && stripeObjectOpt.get() instanceof com.stripe.model.checkout.Session) {
                                com.stripe.model.checkout.Session session = (com.stripe.model.checkout.Session) stripeObjectOpt.get();
                                opticsDiscountsService.updatePaymentStatusBySessionId(session.getId(), "paid");
                            }
                        }
                        
                        updated = true;
                    } catch (Exception e) {
                        try {
                            nursingDiscountsService.processWebhookEvent(event);
                            log.info("✅ [NursingDiscountsService] Successfully processed discount webhook as fallback: {}", eventType);
                            
                            // Try direct update as well
                            if ("checkout.session.completed".equals(eventType)) {
                                java.util.Optional<com.stripe.model.StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
                                if (stripeObjectOpt.isPresent() && stripeObjectOpt.get() instanceof com.stripe.model.checkout.Session) {
                                    com.stripe.model.checkout.Session session = (com.stripe.model.checkout.Session) stripeObjectOpt.get();
                                    nursingDiscountsService.updatePaymentStatusBySessionId(session.getId(), "paid");
                                }
                            }
                            
                            updated = true;
                        } catch (Exception e2) {
                            try {
                                renewableDiscountsService.processWebhookEvent(event);
                                log.info("✅ [RenewableDiscountsService] Successfully processed discount webhook as fallback: {}", eventType);
                                
                                // Try direct update as well
                                if ("checkout.session.completed".equals(eventType)) {
                                    java.util.Optional<com.stripe.model.StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
                                    if (stripeObjectOpt.isPresent() && stripeObjectOpt.get() instanceof com.stripe.model.checkout.Session) {
                                        com.stripe.model.checkout.Session session = (com.stripe.model.checkout.Session) stripeObjectOpt.get();
                                        renewableDiscountsService.updatePaymentStatusBySessionId(session.getId(), "paid");
                                    }
                                }
                                
                                updated = true;
                            } catch (Exception e3) {
                                try {
                                    polymersDiscountsService.processWebhookEvent(event);
                                    log.info("✅ [PolymersDiscountsService] Successfully processed discount webhook as fallback: {}", eventType);
                                    
                                    // Try direct update as well
                                    if ("checkout.session.completed".equals(eventType)) {
                                        java.util.Optional<com.stripe.model.StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
                                        if (stripeObjectOpt.isPresent() && stripeObjectOpt.get() instanceof com.stripe.model.checkout.Session) {
                                            com.stripe.model.checkout.Session session = (com.stripe.model.checkout.Session) stripeObjectOpt.get();
                                            polymersDiscountsService.updatePaymentStatusBySessionId(session.getId(), "paid");
                                        }
                                    }
                                    
                                    updated = true;
                                } catch (Exception e4) {
                                    log.warn("⚠️ All discount services failed to process webhook event: {}", eventType);
                                }
                            }
                        }
                    }
                }
            }
            if (updated) {
                log.info("✅ Discount webhook processed and discount table updated.");
                return ResponseEntity.ok("Discount webhook processed and discount table updated");
            } else {
                return ResponseEntity.ok("Webhook received but no discount record found - likely a regular payment sent to wrong endpoint");
            }
        } catch (Exception e) {
            log.error("Error processing discount webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Discount webhook processing failed");
        }
    }

}