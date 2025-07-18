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
        log.info("🎯 Received DISCOUNT webhook request");
        
        String payload;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()))) {
            payload = reader.lines().collect(Collectors.joining("\n"));
        }

        String sigHeader = request.getHeader("Stripe-Signature");
        log.info("Discount webhook payload length: {}, Signature header present: {}", payload.length(), sigHeader != null);

        if (sigHeader == null || sigHeader.isEmpty()) {
            log.error("⚠️ Missing Stripe-Signature header in discount webhook");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing signature header");
        }

        try {
            // Parse event using any service (all use same Stripe event structure)
            Event event = null;
            try {
                event = opticsDiscountsService.constructWebhookEvent(payload, sigHeader);
                log.info("✅ Event parsed successfully using optics service. Event type: {}", event.getType());
            } catch (Exception e) {
                log.warn("Failed to parse with optics service, trying nursing: {}", e.getMessage());
                try {
                    event = nursingDiscountsService.constructWebhookEvent(payload, sigHeader);
                    log.info("✅ Event parsed successfully using nursing service. Event type: {}", event.getType());
                } catch (Exception e2) {
                    log.warn("Failed to parse with nursing service, trying renewable: {}", e2.getMessage());
                    event = renewableDiscountsService.constructWebhookEvent(payload, sigHeader);
                    log.info("✅ Event parsed successfully using renewable service. Event type: {}", event.getType());
                }
            }

            if (event == null) {
                log.error("❌ Failed to parse webhook event");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Failed to parse event");
            }

            // Directly update discount tables based on event type
            String eventType = event.getType();
            log.info("🎯 Processing discount webhook event: {}", eventType);

            boolean updated = false;
            String sessionId = null;
            String paymentIntentId = null;
            if ("checkout.session.completed".equals(eventType)) {
                java.util.Optional<com.stripe.model.StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
                if (stripeObjectOpt.isPresent() && stripeObjectOpt.get() instanceof com.stripe.model.checkout.Session) {
                    com.stripe.model.checkout.Session session = (com.stripe.model.checkout.Session) stripeObjectOpt.get();
                    sessionId = session.getId();
                    log.info("[DiscountsController] Updating discount table for sessionId: {}", sessionId);
                    if (opticsDiscountsService.updatePaymentStatusBySessionId(sessionId, "COMPLETED")) {
                        log.info("[DiscountsController] Updated OpticsDiscounts for sessionId: {}", sessionId);
                        updated = true;
                    } else if (nursingDiscountsService.updatePaymentStatusBySessionId(sessionId, "COMPLETED")) {
                        log.info("[DiscountsController] Updated NursingDiscounts for sessionId: {}", sessionId);
                        updated = true;
                    } else if (renewableDiscountsService.updatePaymentStatusBySessionId(sessionId, "COMPLETED")) {
                        log.info("[DiscountsController] Updated RenewableDiscounts for sessionId: {}", sessionId);
                        updated = true;
                    } else {
                        log.warn("[DiscountsController] No discount record found for sessionId: {}", sessionId);
                    }
                }
            } else if ("payment_intent.succeeded".equals(eventType)) {
                java.util.Optional<com.stripe.model.StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
                if (stripeObjectOpt.isPresent() && stripeObjectOpt.get() instanceof com.stripe.model.PaymentIntent) {
                    com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) stripeObjectOpt.get();
                    paymentIntentId = paymentIntent.getId();
                    log.info("[DiscountsController] Updating discount table for paymentIntentId: {}", paymentIntentId);
                    if (opticsDiscountsService.updatePaymentStatusByPaymentIntentId(paymentIntentId, "SUCCEEDED")) {
                        log.info("[DiscountsController] Updated OpticsDiscounts for paymentIntentId: {}", paymentIntentId);
                        updated = true;
                    } else if (nursingDiscountsService.updatePaymentStatusByPaymentIntentId(paymentIntentId, "SUCCEEDED")) {
                        log.info("[DiscountsController] Updated NursingDiscounts for paymentIntentId: {}", paymentIntentId);
                        updated = true;
                    } else if (renewableDiscountsService.updatePaymentStatusByPaymentIntentId(paymentIntentId, "SUCCEEDED")) {
                        log.info("[DiscountsController] Updated RenewableDiscounts for paymentIntentId: {}", paymentIntentId);
                        updated = true;
                    } else {
                        log.warn("[DiscountsController] No discount record found for paymentIntentId: {}", paymentIntentId);
                        // Try to find discount record by session ID if payment intent has one
                        if (paymentIntent.getMetadata() != null && paymentIntent.getMetadata().containsKey("sessionId")) {
                            String associatedSessionId = paymentIntent.getMetadata().get("sessionId");
                            log.info("[DiscountsController] Trying to update discount table using sessionId from payment intent metadata: {}", associatedSessionId);
                            if (opticsDiscountsService.updatePaymentStatusBySessionId(associatedSessionId, "SUCCEEDED")) {
                                log.info("[DiscountsController] Updated OpticsDiscounts for sessionId: {} (from payment intent metadata)", associatedSessionId);
                                updated = true;
                            } else if (nursingDiscountsService.updatePaymentStatusBySessionId(associatedSessionId, "SUCCEEDED")) {
                                log.info("[DiscountsController] Updated NursingDiscounts for sessionId: {} (from payment intent metadata)", associatedSessionId);
                                updated = true;
                            } else if (renewableDiscountsService.updatePaymentStatusBySessionId(associatedSessionId, "SUCCEEDED")) {
                                log.info("[DiscountsController] Updated RenewableDiscounts for sessionId: {} (from payment intent metadata)", associatedSessionId);
                                updated = true;
                            } else {
                                log.warn("[DiscountsController] No discount record found for sessionId: {} (from payment intent metadata)", associatedSessionId);
                            }
                        } else {
                            log.info("[DiscountsController] No sessionId metadata found in payment intent, trying to force update via services");
                            // Try to process the payment intent event directly through each service
                            try {
                                opticsDiscountsService.processWebhookEvent(event);
                                log.info("[DiscountsController] Processed payment intent with OpticsDiscountsService: {}", paymentIntentId);
                                updated = true;
                            } catch (Exception e) {
                                log.debug("OpticsDiscountsService couldn't process payment intent {}: {}", paymentIntentId, e.getMessage());
                                try {
                                    nursingDiscountsService.processWebhookEvent(event);
                                    log.info("[DiscountsController] Processed payment intent with NursingDiscountsService: {}", paymentIntentId);
                                    updated = true;
                                } catch (Exception e2) {
                                    log.debug("NursingDiscountsService couldn't process payment intent {}: {}", paymentIntentId, e2.getMessage());
                                    try {
                                        renewableDiscountsService.processWebhookEvent(event);
                                        log.info("[DiscountsController] Processed payment intent with RenewableDiscountsService: {}", paymentIntentId);
                                        updated = true;
                                    } catch (Exception e3) {
                                        log.debug("RenewableDiscountsService couldn't process payment intent {}: {}", paymentIntentId, e3.getMessage());
                                    }
                                }
                            }
                        }
                    }
                }
            } else if ("payment_intent.payment_failed".equals(eventType)) {
                java.util.Optional<com.stripe.model.StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
                if (stripeObjectOpt.isPresent() && stripeObjectOpt.get() instanceof com.stripe.model.PaymentIntent) {
                    com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) stripeObjectOpt.get();
                    paymentIntentId = paymentIntent.getId();
                    log.info("[DiscountsController] Updating discount table for paymentIntentId (FAILED): {}", paymentIntentId);
                    if (opticsDiscountsService.updatePaymentStatusByPaymentIntentId(paymentIntentId, "FAILED")) {
                        log.info("[DiscountsController] Updated OpticsDiscounts for paymentIntentId: {}", paymentIntentId);
                        updated = true;
                    } else if (nursingDiscountsService.updatePaymentStatusByPaymentIntentId(paymentIntentId, "FAILED")) {
                        log.info("[DiscountsController] Updated NursingDiscounts for paymentIntentId: {}", paymentIntentId);
                        updated = true;
                    } else if (renewableDiscountsService.updatePaymentStatusByPaymentIntentId(paymentIntentId, "FAILED")) {
                        log.info("[DiscountsController] Updated RenewableDiscounts for paymentIntentId: {}", paymentIntentId);
                        updated = true;
                    } else {
                        log.warn("[DiscountsController] No discount record found for paymentIntentId: {}", paymentIntentId);
                        // Try to find discount record by session ID if payment intent has one
                        if (paymentIntent.getMetadata() != null && paymentIntent.getMetadata().containsKey("sessionId")) {
                            String associatedSessionId = paymentIntent.getMetadata().get("sessionId");
                            log.info("[DiscountsController] Trying to update discount table using sessionId from payment intent metadata: {}", associatedSessionId);
                            if (opticsDiscountsService.updatePaymentStatusBySessionId(associatedSessionId, "FAILED")) {
                                log.info("[DiscountsController] Updated OpticsDiscounts for sessionId: {} (from payment intent metadata)", associatedSessionId);
                                updated = true;
                            } else if (nursingDiscountsService.updatePaymentStatusBySessionId(associatedSessionId, "FAILED")) {
                                log.info("[DiscountsController] Updated NursingDiscounts for sessionId: {} (from payment intent metadata)", associatedSessionId);
                                updated = true;
                            } else if (renewableDiscountsService.updatePaymentStatusBySessionId(associatedSessionId, "FAILED")) {
                                log.info("[DiscountsController] Updated RenewableDiscounts for sessionId: {} (from payment intent metadata)", associatedSessionId);
                                updated = true;
                            } else {
                                log.warn("[DiscountsController] No discount record found for sessionId: {} (from payment intent metadata)", associatedSessionId);
                            }
                        } else {
                            log.info("[DiscountsController] No sessionId metadata found in payment intent, trying to force update via services");
                            // Try to process the payment intent event directly through each service
                            try {
                                opticsDiscountsService.processWebhookEvent(event);
                                log.info("[DiscountsController] Processed payment intent failure with OpticsDiscountsService: {}", paymentIntentId);
                                updated = true;
                            } catch (Exception e) {
                                log.debug("OpticsDiscountsService couldn't process payment intent failure {}: {}", paymentIntentId, e.getMessage());
                                try {
                                    nursingDiscountsService.processWebhookEvent(event);
                                    log.info("[DiscountsController] Processed payment intent failure with NursingDiscountsService: {}", paymentIntentId);
                                    updated = true;
                                } catch (Exception e2) {
                                    log.debug("NursingDiscountsService couldn't process payment intent failure {}: {}", paymentIntentId, e2.getMessage());
                                    try {
                                        renewableDiscountsService.processWebhookEvent(event);
                                        log.info("[DiscountsController] Processed payment intent failure with RenewableDiscountsService: {}", paymentIntentId);
                                        updated = true;
                                    } catch (Exception e3) {
                                        log.debug("RenewableDiscountsService couldn't process payment intent failure {}: {}", paymentIntentId, e3.getMessage());
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                log.info("ℹ️ Unhandled discount event type: {}", eventType);
                updated = true; // Consider unhandled events as processed to avoid errors
            }

            if (updated) {
                log.info("✅ Discount webhook processed and discount table updated. Event type: {}", eventType);
                return ResponseEntity.ok("Discount webhook processed and discount table updated");
            } else {
                String identifier = sessionId != null ? "Session ID: " + sessionId : 
                                  paymentIntentId != null ? "Payment Intent ID: " + paymentIntentId : "Unknown";
                log.warn("⚠️ No discount record found for webhook. Event type: {}, {} - This might be a regular payment webhook sent to discount endpoint instead of /api/payments/webhook", eventType, identifier);
                // Return success to avoid webhook retry loops for regular payments sent to discount endpoint
                return ResponseEntity.ok("Webhook received but no discount record found - likely a regular payment sent to wrong endpoint");
            }

        } catch (Exception e) {
            log.error("❌ Error processing discount webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Discount webhook processing failed");
        }
    }

    /**
     * Process checkout.session.completed events for discount payments
     */
    private boolean processDiscountCheckoutSessionCompleted(Event event) {
        log.info("🎯 Processing discount checkout.session.completed");
        
        try {
            java.util.Optional<com.stripe.model.StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
            if (stripeObjectOpt.isPresent() && stripeObjectOpt.get() instanceof com.stripe.model.checkout.Session) {
                com.stripe.model.checkout.Session session = (com.stripe.model.checkout.Session) stripeObjectOpt.get();
                String sessionId = session.getId();
                
                log.info("Processing discount session completed: {}", sessionId);
                
                // Try to process with each discount service
                boolean processed = false;
                
                // Try Optics discount service first
                try {
                    opticsDiscountsService.processWebhookEvent(event);
                    log.info("✅ Processed discount session with Optics service: {}", sessionId);
                    processed = true;
                } catch (Exception e) {
                    log.debug("Optics service couldn't process session {}: {}", sessionId, e.getMessage());
                }
                
                // Try Nursing discount service if not processed
                if (!processed) {
                    try {
                        nursingDiscountsService.processWebhookEvent(event);
                        log.info("✅ Processed discount session with Nursing service: {}", sessionId);
                        processed = true;
                    } catch (Exception e) {
                        log.debug("Nursing service couldn't process session {}: {}", sessionId, e.getMessage());
                    }
                }
                
                // Try Renewable discount service if not processed
                if (!processed) {
                    try {
                        renewableDiscountsService.processWebhookEvent(event);
                        log.info("✅ Processed discount session with Renewable service: {}", sessionId);
                        processed = true;
                    } catch (Exception e) {
                        log.debug("Renewable service couldn't process session {}: {}", sessionId, e.getMessage());
                    }
                }
                
                return processed;
            }
        } catch (Exception e) {
            log.error("❌ Error processing discount checkout.session.completed: {}", e.getMessage(), e);
        }
        
        return false;
    }

    /**
     * Process payment_intent.succeeded events for discount payments
     */
    private boolean processDiscountPaymentIntentSucceeded(Event event) {
        log.info("🎯 Processing discount payment_intent.succeeded");
        
        try {
            java.util.Optional<com.stripe.model.StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
            if (stripeObjectOpt.isPresent() && stripeObjectOpt.get() instanceof com.stripe.model.PaymentIntent) {
                com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) stripeObjectOpt.get();
                String paymentIntentId = paymentIntent.getId();
                
                log.info("Processing discount payment intent succeeded: {}", paymentIntentId);
                
                // Try to process with each discount service
                boolean processed = false;
                
                // Try Optics discount service first
                try {
                    opticsDiscountsService.processWebhookEvent(event);
                    log.info("✅ Processed discount payment intent with Optics service: {}", paymentIntentId);
                    processed = true;
                } catch (Exception e) {
                    log.debug("Optics service couldn't process payment intent {}: {}", paymentIntentId, e.getMessage());
                }
                
                // Try Nursing discount service if not processed
                if (!processed) {
                    try {
                        nursingDiscountsService.processWebhookEvent(event);
                        log.info("✅ Processed discount payment intent with Nursing service: {}", paymentIntentId);
                        processed = true;
                    } catch (Exception e) {
                        log.debug("Nursing service couldn't process payment intent {}: {}", paymentIntentId, e.getMessage());
                    }
                }
                
                // Try Renewable discount service if not processed
                if (!processed) {
                    try {
                        renewableDiscountsService.processWebhookEvent(event);
                        log.info("✅ Processed discount payment intent with Renewable service: {}", paymentIntentId);
                        processed = true;
                    } catch (Exception e) {
                        log.debug("Renewable service couldn't process payment intent {}: {}", paymentIntentId, e.getMessage());
                    }
                }
                
                return processed;
            }
        } catch (Exception e) {
            log.error("❌ Error processing discount payment_intent.succeeded: {}", e.getMessage(), e);
        }
        
        return false;
    }

    /**
     * Process payment_intent.payment_failed events for discount payments
     */
    private boolean processDiscountPaymentIntentFailed(Event event) {
        log.info("🎯 Processing discount payment_intent.payment_failed");
        
        try {
            java.util.Optional<com.stripe.model.StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
            if (stripeObjectOpt.isPresent() && stripeObjectOpt.get() instanceof com.stripe.model.PaymentIntent) {
                com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) stripeObjectOpt.get();
                String paymentIntentId = paymentIntent.getId();
                
                log.info("Processing discount payment intent failed: {}", paymentIntentId);
                
                // Try to process with each discount service
                boolean processed = false;
                
                // Try Optics discount service first
                try {
                    opticsDiscountsService.processWebhookEvent(event);
                    log.info("✅ Processed discount payment intent failure with Optics service: {}", paymentIntentId);
                    processed = true;
                } catch (Exception e) {
                    log.debug("Optics service couldn't process payment intent failure {}: {}", paymentIntentId, e.getMessage());
                }
                
                // Try Nursing discount service if not processed
                if (!processed) {
                    try {
                        nursingDiscountsService.processWebhookEvent(event);
                        log.info("✅ Processed discount payment intent failure with Nursing service: {}", paymentIntentId);
                        processed = true;
                    } catch (Exception e) {
                        log.debug("Nursing service couldn't process payment intent failure {}: {}", paymentIntentId, e.getMessage());
                    }
                }
                
                // Try Renewable discount service if not processed
                if (!processed) {
                    try {
                        renewableDiscountsService.processWebhookEvent(event);
                        log.info("✅ Processed discount payment intent failure with Renewable service: {}", paymentIntentId);
                        processed = true;
                    } catch (Exception e) {
                        log.debug("Renewable service couldn't process payment intent failure {}: {}", paymentIntentId, e.getMessage());
                    }
                }
                
                return processed;
            }
        } catch (Exception e) {
            log.error("❌ Error processing discount payment_intent.payment_failed: {}", e.getMessage(), e);
        }
        
        return false;
    }



}