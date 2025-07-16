package com.zn.payment.nursing.controller;

import java.io.BufferedReader;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stripe.model.Event;
import com.zn.payment.dto.CheckoutRequest;
import com.zn.payment.dto.NursingPaymentResponseDTO;
import com.zn.payment.nursing.service.NursingStripeService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/payment/nursing")
@Slf4j
public class NursingPaymentController {

    @Autowired
    private NursingStripeService nursingStripeService;

    @PostMapping("/create-checkout-session")
    public ResponseEntity<NursingPaymentResponseDTO> createCheckoutSession(@RequestBody CheckoutRequest request, @RequestParam Long pricingConfigId) {
        log.info("Received Nursing checkout session request: {} with pricingConfigId: {}", request, pricingConfigId);
        
        try {
            // Validate request
            if (pricingConfigId == null) {
                log.error("pricingConfigId is mandatory but not provided");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createNursingErrorResponse("pricing_config_id_required"));
            }
            
            // Call nursing service
            NursingPaymentResponseDTO response = nursingStripeService.createCheckoutSessionWithPricingValidation(request, pricingConfigId);
            log.info("Nursing checkout session created successfully. Session ID: {}", response.getSessionId());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Validation error creating nursing checkout session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createNursingErrorResponse("validation_failed"));
        } catch (Exception e) {
            log.error("Error creating nursing checkout session: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createNursingErrorResponse("failed"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<NursingPaymentResponseDTO> getCheckoutSession(@PathVariable String id) {
        log.info("Retrieving Nursing checkout session with ID: {}", id);
        
        try {
            NursingPaymentResponseDTO responseDTO = nursingStripeService.retrieveSession(id);
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error retrieving Nursing checkout session: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createNursingErrorResponse("failed"));
        }
    }
    
    @PostMapping("/{id}/expire")
    public ResponseEntity<NursingPaymentResponseDTO> expireSession(@PathVariable String id) {
        log.info("Expiring Nursing checkout session with ID: {}", id);
        
        try {
            NursingPaymentResponseDTO responseDTO = nursingStripeService.expireSession(id);
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error expiring Nursing checkout session: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createNursingErrorResponse("failed"));
        }
    }

    /**
     * Dedicated webhook endpoint for Nursing payments
     * URL: /api/payment/nursing/webhook
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleNursingWebhook(HttpServletRequest request) throws IOException {
        log.info("Received Nursing-specific webhook request");
        
        String payload = readPayload(request);
        String sigHeader = request.getHeader("Stripe-Signature");
        
        log.info("Nursing webhook payload length: {}, Signature header present: {}", 
                payload.length(), sigHeader != null);
        
        try {
            Event event = nursingStripeService.constructWebhookEvent(payload, sigHeader);
            nursingStripeService.processWebhookEvent(event);
            
            log.info("✅ Nursing webhook processed successfully. Event type: {}", event.getType());
            return ResponseEntity.ok().body("Nursing webhook processed successfully");
            
        } catch (Exception e) {
            log.error("❌ Error processing Nursing webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Nursing webhook processing failed");
        }
    }
    
    /**
     * Helper method to read the request payload
     */
    private String readPayload(HttpServletRequest request) throws IOException {
        try (BufferedReader reader = request.getReader()) {
            StringBuilder payload = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                payload.append(line);
            }
            return payload.toString();
        }
    }
    
    private NursingPaymentResponseDTO createNursingErrorResponse(String errorMessage) {
        return NursingPaymentResponseDTO.builder()
                .paymentStatus(errorMessage)
                .description("Error: " + errorMessage)
                .build();
    }
}
