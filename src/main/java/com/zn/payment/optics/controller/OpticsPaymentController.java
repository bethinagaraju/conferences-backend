package com.zn.payment.optics.controller;

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
import com.zn.payment.dto.OpticsPaymentResponseDTO;
import com.zn.payment.optics.service.OpticsStripeService;
import com.zn.optics.entity.OpticsRegistrationForm;
import com.zn.optics.entity.OpticsPricingConfig;
import com.zn.optics.repository.IOpricsRegistrationFormRepository;
import com.zn.optics.repository.IOpticsPricingConfigRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/payment/optics")
@Slf4j
public class OpticsPaymentController {

    @Autowired
    private OpticsStripeService opticsStripeService;

    @Autowired
    private IOpricsRegistrationFormRepository opticsRegistrationFormRepository;
    
    @Autowired
    private IOpticsPricingConfigRepository opticsPricingConfigRepository;

    @PostMapping("/create-checkout-session")
    public ResponseEntity<OpticsPaymentResponseDTO> createCheckoutSession(@RequestBody CheckoutRequest request, @RequestParam Long pricingConfigId) {
        log.info("Received Optics checkout session request: {} with pricingConfigId: {}", request, pricingConfigId);
        
        try {
            // Add detailed debugging for email field
            log.info("🔍 DEBUG - Request email field: '{}'", request.getEmail());
            log.info("🔍 DEBUG - Request name field: '{}'", request.getName());
            log.info("🔍 DEBUG - Request phone field: '{}'", request.getPhone());
            log.info("🔍 DEBUG - Full request object: {}", request);
            
            // Validate that pricingConfigId is provided (now mandatory)
            if (pricingConfigId == null) {
                log.error("pricingConfigId is mandatory but not provided");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createOpticsErrorResponse("pricing_config_id_required"));
            }
            
            // Validate incoming request currency is EUR only
            if (request.getCurrency() == null) {
                request.setCurrency("eur"); // Default to EUR if not provided
                log.info("Currency not provided, defaulting to EUR");
            } else if (!"eur".equalsIgnoreCase(request.getCurrency())) {
                log.error("Invalid currency provided: {}. Only EUR is supported", request.getCurrency());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createOpticsErrorResponse("invalid_currency_only_eur_supported"));
            }
            
            // Validate required customer fields for registration
            if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
                log.error("Customer email is required for registration. Request email: '{}', Request object: {}", 
                         request.getEmail(), request);
                log.error("❌ VALIDATION FAILED: Email field is missing or empty");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createOpticsErrorResponse("customer_email_required"));
            }
            
            if (request.getName() == null || request.getName().trim().isEmpty()) {
                log.error("Customer name is required for registration");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createOpticsErrorResponse("customer_name_required"));
            }
            
            if (request.getQuantity() == null || request.getQuantity() <= 0) {
                log.error("Invalid quantity: {}. Must be positive value", request.getQuantity());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createOpticsErrorResponse("invalid_quantity_must_be_positive"));
            }
            
            // Always use backend value for payment amount - CORE BUSINESS LOGIC
            OpticsPricingConfig pricingConfig = opticsPricingConfigRepository.findById(pricingConfigId)
                    .orElseThrow(() -> new IllegalArgumentException("Pricing config not found with ID: " + pricingConfigId));
            
            java.math.BigDecimal backendTotalPrice = pricingConfig.getTotalPrice();
            Long unitAmountInCents = backendTotalPrice.multiply(new java.math.BigDecimal(100)).longValue();
            request.setUnitAmount(unitAmountInCents); // Stripe expects cents
            log.info("Using backend total price for payment: {} EUR ({} cents)", backendTotalPrice, unitAmountInCents);
            
            // Set pricingConfigId in the request object (now mandatory)
            request.setPricingConfigId(pricingConfigId);
            log.info("Setting mandatory pricingConfigId: {}", pricingConfigId);
            
            // Create and save registration form - CORE BUSINESS LOGIC
            OpticsRegistrationForm registrationForm = new OpticsRegistrationForm();
            registrationForm.setName(request.getName() != null ? request.getName() : "");
            registrationForm.setPhone(request.getPhone() != null ? request.getPhone() : "");
            registrationForm.setEmail(request.getEmail());
            registrationForm.setInstituteOrUniversity(request.getInstituteOrUniversity() != null ? request.getInstituteOrUniversity() : "");
            registrationForm.setCountry(request.getCountry() != null ? request.getCountry() : "");
            registrationForm.setPricingConfig(pricingConfig);
            registrationForm.setAmountPaid(pricingConfig.getTotalPrice());
            
            OpticsRegistrationForm savedRegistration = opticsRegistrationFormRepository.save(registrationForm);
            log.info("✅ Optics registration form created and saved with ID: {}", savedRegistration.getId());
            
            // Call optics service with pricing validation
            OpticsPaymentResponseDTO response = opticsStripeService.createCheckoutSessionWithPricingValidation(request, pricingConfigId);
            log.info("Optics checkout session created successfully with pricing validation. Session ID: {}", response.getSessionId());
            
            // Link registration to payment - CORE BUSINESS LOGIC
            opticsStripeService.linkRegistrationToPayment(savedRegistration.getId(), response.getSessionId());
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Validation error creating optics checkout session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createOpticsErrorResponse("validation_failed"));
        } catch (Exception e) {
            log.error("Error creating optics checkout session: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createOpticsErrorResponse("failed"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<OpticsPaymentResponseDTO> getCheckoutSession(@PathVariable String id) {
        log.info("Retrieving Optics checkout session with ID: {}", id);
        
        try {
            OpticsPaymentResponseDTO responseDTO = opticsStripeService.retrieveSession(id);
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error retrieving Optics checkout session: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createOpticsErrorResponse("failed"));
        }
    }
    
    @PostMapping("/{id}/expire")
    public ResponseEntity<OpticsPaymentResponseDTO> expireSession(@PathVariable String id) {
        log.info("Expiring Optics checkout session with ID: {}", id);
        
        try {
            OpticsPaymentResponseDTO responseDTO = opticsStripeService.expireSession(id);
            return ResponseEntity.ok(responseDTO);
        } catch (Exception e) {
            log.error("Error expiring Optics checkout session: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createOpticsErrorResponse("failed"));
        }
    }

    /**
     * Dedicated webhook endpoint for Optics payments
     * URL: /api/payment/optics/webhook
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleOpticsWebhook(HttpServletRequest request) throws IOException {
        log.info("Received Optics-specific webhook request");
        
        String payload = readPayload(request);
        String sigHeader = request.getHeader("Stripe-Signature");
        
        log.info("Optics webhook payload length: {}, Signature header present: {}", 
                payload.length(), sigHeader != null);
        
        try {
            Event event = opticsStripeService.constructWebhookEvent(payload, sigHeader);
            opticsStripeService.processWebhookEvent(event);
            
            log.info("✅ Optics webhook processed successfully. Event type: {}", event.getType());
            return ResponseEntity.ok().body("Optics webhook processed successfully");
            
        } catch (Exception e) {
            log.error("❌ Error processing Optics webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Optics webhook processing failed");
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
    
    private OpticsPaymentResponseDTO createOpticsErrorResponse(String errorMessage) {
        return OpticsPaymentResponseDTO.builder()
                .paymentStatus(errorMessage)
                .description("Error: " + errorMessage)
                .build();
    }
}
