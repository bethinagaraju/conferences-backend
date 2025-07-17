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

            // Process discount-specific events
            boolean processed = false;
            String eventType = event.getType();
            
            log.info("🎯 Processing discount webhook event: {}", eventType);
            
            if ("checkout.session.completed".equals(eventType)) {
                processed = processDiscountCheckoutSessionCompleted(event);
            } else if ("payment_intent.succeeded".equals(eventType)) {
                processed = processDiscountPaymentIntentSucceeded(event);
            } else if ("payment_intent.payment_failed".equals(eventType)) {
                processed = processDiscountPaymentIntentFailed(event);
            } else {
                log.info("ℹ️ Unhandled discount event type: {}", eventType);
                processed = true; // Consider unhandled events as processed to avoid errors
            }

            if (processed) {
                log.info("✅ Discount webhook processed successfully. Event type: {}", eventType);
                return ResponseEntity.ok("Discount webhook processed successfully");
            } else {
                log.error("❌ Failed to process discount webhook. Event type: {}", eventType);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to process discount webhook");
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

    /**
     * Update discount payment status by sessionId or paymentIntentId from PaymentController webhook
     */
    public boolean updateDiscountStatusFromPaymentWebhook(String sessionId, String paymentIntentId, String status) {
        boolean updated = false;
        if (sessionId != null) {
            updated = opticsDiscountsService.updatePaymentStatusBySessionId(sessionId, status)
                || nursingDiscountsService.updatePaymentStatusBySessionId(sessionId, status)
                || renewableDiscountsService.updatePaymentStatusBySessionId(sessionId, status);
        }
        if (!updated && paymentIntentId != null) {
            updated = opticsDiscountsService.updatePaymentStatusByPaymentIntentId(paymentIntentId, status)
                || nursingDiscountsService.updatePaymentStatusByPaymentIntentId(paymentIntentId, status)
                || renewableDiscountsService.updatePaymentStatusByPaymentIntentId(paymentIntentId, status);
        }
        return updated;
    }



}