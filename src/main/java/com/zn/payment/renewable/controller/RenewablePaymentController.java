package com.zn.payment.renewable.controller;

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
import com.zn.payment.dto.RenewablePaymentResponseDTO;
import com.zn.payment.renewable.service.RenewaleStripeService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/payment/renewable")
@Slf4j
public class RenewablePaymentController {

    @Autowired
    private RenewaleStripeService renewableStripeService;

    @PostMapping("/create-checkout-session")
    public ResponseEntity<RenewablePaymentResponseDTO> createCheckoutSession(@RequestBody CheckoutRequest request, @RequestParam Long pricingConfigId) {
        log.info("Received Renewable checkout session request: {} with pricingConfigId: {}", request, pricingConfigId);
        
        try {
            // Validate request
            if (pricingConfigId == null) {
                log.error("pricingConfigId is mandatory but not provided");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createRenewableErrorResponse("pricing_config_id_required"));
            }
            
            // Call renewable service
            RenewablePaymentResponseDTO response = renewableStripeService.createCheckoutSessionWithPricingValidation(request, pricingConfigId);
            log.info("Renewable checkout session created successfully. Session ID: {}", response.getSessionId());
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Validation error creating renewable checkout session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createRenewableErrorResponse("validation_failed"));
        } catch (Exception e) {
            log.error("Error creating renewable checkout session: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createRenewableErrorResponse("failed"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<RenewablePaymentResponseDTO> getCheckoutSession(@PathVariable String id) {
        log.info("Retrieving Renewable checkout session with ID: {}", id);
        
        try {
            RenewablePaymentResponseDTO responseDTO = renewableStripeService.retrieveSession(id);
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error retrieving Renewable checkout session: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createRenewableErrorResponse("failed"));
        }
    }
    
    @PostMapping("/{id}/expire")
    public ResponseEntity<RenewablePaymentResponseDTO> expireSession(@PathVariable String id) {
        log.info("Expiring Renewable checkout session with ID: {}", id);
        
        try {
            RenewablePaymentResponseDTO responseDTO = renewableStripeService.expireSession(id);
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error expiring Renewable checkout session: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createRenewableErrorResponse("failed"));
        }
    }

    /**
     * Dedicated webhook endpoint for Renewable payments
     * URL: /api/payment/renewable/webhook
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleRenewableWebhook(HttpServletRequest request) throws IOException {
        log.info("Received Renewable-specific webhook request");
        
        String payload = readPayload(request);
        String sigHeader = request.getHeader("Stripe-Signature");
        
        log.info("Renewable webhook payload length: {}, Signature header present: {}", 
                payload.length(), sigHeader != null);
        
        try {
            Event event = renewableStripeService.constructWebhookEvent(payload, sigHeader);
            renewableStripeService.processWebhookEvent(event);
            
            log.info("✅ Renewable webhook processed successfully. Event type: {}", event.getType());
            return ResponseEntity.ok().body("Renewable webhook processed successfully");
            
        } catch (Exception e) {
            log.error("❌ Error processing Renewable webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Renewable webhook processing failed");
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
    
    private RenewablePaymentResponseDTO createRenewableErrorResponse(String errorMessage) {
        return RenewablePaymentResponseDTO.builder()
                .paymentStatus(errorMessage)
                .description("Error: " + errorMessage)
                .build();
    }
}
