package com.zn.payment.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

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
import com.zn.payment.dto.NursingPaymentResponseDTO;
import com.zn.payment.dto.RenewablePaymentResponseDTO;
import com.zn.payment.optics.service.OpticsStripeService;
import com.zn.payment.nursing.service.NursingStripeService;
import com.zn.payment.renewable.service.RenewaleStripeService;
import com.zn.optics.entity.OpticsRegistrationForm;
import com.zn.optics.entity.OpticsPricingConfig;
import com.zn.optics.repository.IOpricsRegistrationFormRepository;
import com.zn.optics.repository.IOpticsPricingConfigRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/payment")
@Slf4j
public class PaymentController {

    @Autowired
    private OpticsStripeService opticsStripeService;

    @Autowired
    private NursingStripeService nursingStripeService;

    @Autowired
    private RenewaleStripeService renewableStripeService;

    // Optics repositories
    @Autowired
    private IOpricsRegistrationFormRepository opticsRegistrationFormRepository;
    
    @Autowired
    private IOpticsPricingConfigRepository opticsPricingConfigRepository;

    @PostMapping("/create-checkout-session")
    public ResponseEntity<?> createCheckoutSession(@RequestBody CheckoutRequest request, @RequestParam Long pricingConfigId, HttpServletRequest httpRequest) {
        log.info("Received request to create checkout session: {} with pricingConfigId: {}", request, pricingConfigId);
        
        String origin = httpRequest.getHeader("Origin");
        if (origin == null) {
            origin = httpRequest.getHeader("Referer");
        }
        
        if (origin == null) {
            log.error("Origin or Referer header is missing");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("origin_or_referer_missing"));
        }
        
        // Route to appropriate service based on domain and handle internally
        if (origin.contains("globallopmeet.com")) {
            log.info("Processing Optics checkout for domain: {}", origin);
            return handleOpticsCheckout(request, pricingConfigId);
        } else if (origin.contains("nursingmeet2026.com")) {
            log.info("Processing Nursing checkout for domain: {}", origin);
            return handleNursingCheckout(request, pricingConfigId);
        } else if (origin.contains("globalrenewablemeet.com")) {
            log.info("Processing Renewable checkout for domain: {}", origin);
            return handleRenewableCheckout(request, pricingConfigId);
        } else {
            log.error("Unknown frontend domain: {}", origin);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("unknown_frontend_domain"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getCheckoutSession(@PathVariable String id, HttpServletRequest httpRequest) {
        log.info("Retrieving checkout session with ID: {}", id);
        
        String origin = httpRequest.getHeader("Origin");
        if (origin == null) {
            origin = httpRequest.getHeader("Referer");
        }
        
        try {
            if (origin != null && origin.contains("globallopmeet.com")) {
                OpticsPaymentResponseDTO responseDTO = opticsStripeService.retrieveSession(id);
                return ResponseEntity.ok(responseDTO);
            } else if (origin != null && origin.contains("nursingmeet2026.com")) {
                NursingPaymentResponseDTO responseDTO = nursingStripeService.retrieveSession(id);
                return ResponseEntity.ok(responseDTO);
            } else if (origin != null && origin.contains("globalrenewablemeet.com")) {
                RenewablePaymentResponseDTO responseDTO = renewableStripeService.retrieveSession(id);
                return ResponseEntity.ok(responseDTO);
            } else {
                log.error("Unknown or missing domain origin: {}", origin);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("unknown_domain_or_missing_origin"));
            }
        } catch (Exception e) {
            log.error("Error retrieving checkout session: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("failed"));
        }
    }
    @PostMapping("/{id}/expire")
    public ResponseEntity<?> expireSession(@PathVariable String id, HttpServletRequest httpRequest) {
        log.info("Expiring checkout session with ID: {}", id);
        
        String origin = httpRequest.getHeader("Origin");
        if (origin == null) {
            origin = httpRequest.getHeader("Referer");
        }
        
        try {
            if (origin != null && origin.contains("globallopmeet.com")) {
                OpticsPaymentResponseDTO responseDTO = opticsStripeService.expireSession(id);
                return ResponseEntity.ok(responseDTO);
            } else if (origin != null && origin.contains("nursingmeet2026.com")) {
                NursingPaymentResponseDTO responseDTO = nursingStripeService.expireSession(id);
                return ResponseEntity.ok(responseDTO);
            } else if (origin != null && origin.contains("globalrenewablemeet.com")) {
                RenewablePaymentResponseDTO responseDTO = renewableStripeService.expireSession(id);
                return ResponseEntity.ok(responseDTO);
            } else {
                log.error("Unknown or missing domain origin: {}", origin);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createErrorResponse("unknown_domain_or_missing_origin"));
            }
        } catch (Exception e) {
            log.error("Error expiring checkout session: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("failed"));
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(HttpServletRequest request) throws IOException {
        log.info("Received webhook request");
        String payload;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()))) {
            payload = reader.lines().collect(Collectors.joining("\n"));
        }
        
        String sigHeader = request.getHeader("Stripe-Signature");
        log.info("Webhook payload length: {}, Signature header present: {}", 
                payload.length(), sigHeader != null);
        
        if (sigHeader == null || sigHeader.isEmpty()) {
            log.error("⚠️ Missing Stripe-Signature header");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing signature header");
        }
        
        try {
            // For webhooks, we try all services until one handles it successfully
            boolean processed = false;
            
            // Try optics service first
            try {
                Event event = opticsStripeService.constructWebhookEvent(payload, sigHeader);
                opticsStripeService.processWebhookEvent(event);
                processed = true;
                log.info("✅ Webhook processed by Optics service. Event type: {}", event.getType());
            } catch (Exception e) {
                log.debug("Optics service couldn't process webhook: {}", e.getMessage());
            }
            
            // Try nursing service if not processed
            if (!processed) {
                try {
                    Event event = nursingStripeService.constructWebhookEvent(payload, sigHeader);
                    nursingStripeService.processWebhookEvent(event);
                    processed = true;
                    log.info("✅ Webhook processed by Nursing service. Event type: {}", event.getType());
                } catch (Exception e) {
                    log.debug("Nursing service couldn't process webhook: {}", e.getMessage());
                }
            }
            
            // Try renewable service if not processed
            if (!processed) {
                try {
                    Event event = renewableStripeService.constructWebhookEvent(payload, sigHeader);
                    renewableStripeService.processWebhookEvent(event);
                    processed = true;
                    log.info("✅ Webhook processed by Renewable service. Event type: {}", event.getType());
                } catch (Exception e) {
                    log.debug("Renewable service couldn't process webhook: {}", e.getMessage());
                }
            }
            
            if (processed) {
                return ResponseEntity.ok().body("Webhook processed successfully");
            } else {
                log.error("❌ No service could process the webhook");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook processing failed");
            }
            
        } catch (Exception e) {
            log.error("❌ Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook processing failed");
        }
    }
    
    /**
     * Dedicated webhook endpoint for Optics payments
     * URL: /api/payment/optics/webhook
     */
    @PostMapping("/optics/webhook")
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
     * Dedicated webhook endpoint for Nursing payments
     * URL: /api/payment/nursing/webhook
     */
    @PostMapping("/nursing/webhook")
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
     * Dedicated webhook endpoint for Renewable payments
     * URL: /api/payment/renewable/webhook
     */
    @PostMapping("/renewable/webhook")
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
    
    // Generic error response for cases where we don't know the vertical
    private Object createErrorResponse(String errorMessage) {
        return java.util.Map.of(
            "success", false,
            "error", errorMessage,
            "paymentStatus", errorMessage
        );
    }
    
    private ResponseEntity<OpticsPaymentResponseDTO> handleOpticsCheckout(CheckoutRequest request, Long pricingConfigId) {
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
    
    private ResponseEntity<NursingPaymentResponseDTO> handleNursingCheckout(CheckoutRequest request, Long pricingConfigId) {
        try {
            // Validate request similar to optics
            if (pricingConfigId == null) {
                log.error("pricingConfigId is mandatory but not provided");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createNursingErrorResponse("pricing_config_id_required"));
            }
            
            // Call nursing service - this will save to nursing_payment_records table
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
    
    private ResponseEntity<RenewablePaymentResponseDTO> handleRenewableCheckout(CheckoutRequest request, Long pricingConfigId) {
        try {
            // Validate request similar to optics
            if (pricingConfigId == null) {
                log.error("pricingConfigId is mandatory but not provided");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createRenewableErrorResponse("pricing_config_id_required"));
            }
            
            // Call renewable service - this will save to renewable_payment_records table
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
    
    // Helper methods to create error responses for each vertical
    private OpticsPaymentResponseDTO createOpticsErrorResponse(String errorMessage) {
        return OpticsPaymentResponseDTO.builder()
                .paymentStatus(errorMessage)
                .description("Error: " + errorMessage)
                .build();
    }
    
    private NursingPaymentResponseDTO createNursingErrorResponse(String errorMessage) {
        return NursingPaymentResponseDTO.builder()
                .paymentStatus(errorMessage)
                .description("Error: " + errorMessage)
                .build();
    }
    
    private RenewablePaymentResponseDTO createRenewableErrorResponse(String errorMessage) {
        return RenewablePaymentResponseDTO.builder()
                .paymentStatus(errorMessage)
                .description("Error: " + errorMessage)
                .build();
    }
}
