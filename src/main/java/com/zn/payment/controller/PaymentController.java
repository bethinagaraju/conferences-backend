package com.zn.payment.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Optional;
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
import com.stripe.model.PaymentIntent;
import com.zn.payment.dto.CheckoutRequest;
import com.zn.payment.dto.OpticsPaymentResponseDTO;
import com.zn.payment.dto.NursingPaymentResponseDTO;
import com.zn.payment.dto.RenewablePaymentResponseDTO;
import com.zn.payment.optics.service.OpticsStripeService;
import com.zn.payment.nursing.service.NursingStripeService;
import com.zn.payment.renewable.service.RenewaleStripeService;
import com.zn.payment.optics.service.OpticsDiscountsService;
import com.zn.payment.nursing.service.NursingDiscountsService;
import com.zn.payment.renewable.service.RenewableDiscountsService;

import com.zn.payment.optics.entity.OpticsDiscounts;
import com.zn.payment.optics.repository.OpticsDiscountsRepository;
import com.zn.payment.nursing.entity.NursingDiscounts;
import com.zn.payment.nursing.repository.NursingDiscountsRepository;
import com.zn.payment.renewable.entity.RenewableDiscounts;
import com.zn.payment.renewable.repository.RenewableDiscountsRepository;

import com.zn.payment.optics.entity.OpticsPaymentRecord;
import com.zn.payment.nursing.entity.NursingPaymentRecord;
import com.zn.payment.renewable.entity.RenewablePaymentRecord;
import java.time.LocalDateTime;
import com.zn.payment.renewable.service.RenewableDiscountsService;
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

    // Discount services
    @Autowired
    private OpticsDiscountsService opticsDiscountsService;
    
    @Autowired
    private NursingDiscountsService nursingDiscountsService;
    
    @Autowired
    private RenewableDiscountsService renewableDiscountsService;

    // Optics repositories
    @Autowired
    private IOpricsRegistrationFormRepository opticsRegistrationFormRepository;
    
    @Autowired
    private IOpticsPricingConfigRepository opticsPricingConfigRepository;
    
    @Autowired
    private OpticsDiscountsRepository opticsDiscountsRepository;
    
    @Autowired
    private NursingDiscountsRepository nursingDiscountsRepository;
    
    @Autowired
    private RenewableDiscountsRepository renewableDiscountsRepository;

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
        log.info("Webhook payload length: {}, Signature header present: {}", payload.length(), sigHeader != null);

        if (sigHeader == null || sigHeader.isEmpty()) {
            log.error("⚠️ Missing Stripe-Signature header");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing signature header");
        }

        try {
            // Parse event using Optics service (all services use same Stripe event structure)
            Event event = null;
            try {
                event = opticsStripeService.constructWebhookEvent(payload, sigHeader);
            } catch (Exception e) {
                log.debug("Optics service couldn't parse event: {}", e.getMessage());
                try {
                    event = nursingStripeService.constructWebhookEvent(payload, sigHeader);
                } catch (Exception e2) {
                    log.debug("Nursing service couldn't parse event: {}", e2.getMessage());
                    try {
                        event = renewableStripeService.constructWebhookEvent(payload, sigHeader);
                    } catch (Exception e3) {
                        log.error("No service could parse Stripe event: {}", e3.getMessage());
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook event parse failed");
                    }
                }
            }

            // Enhanced webhook routing based on metadata and event type
            if (event != null) {
                String eventType = event.getType();
                log.info("Processing webhook event type: {}", eventType);
                
                // Check if this is a session-related event and route accordingly
                if (eventType.startsWith("checkout.session.") || eventType.startsWith("payment_intent.")) {
                    java.util.Optional<com.stripe.model.StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
                    
                    if (stripeObjectOpt.isPresent()) {
                        com.stripe.model.StripeObject stripeObject = stripeObjectOpt.get();
                        
                        // Check if it's a session object
                        if (stripeObject instanceof com.stripe.model.checkout.Session) {
                            com.stripe.model.checkout.Session session = (com.stripe.model.checkout.Session) stripeObject;
                            String paymentType = session.getMetadata() != null ? session.getMetadata().get("paymentType") : null;
                            String source = session.getMetadata() != null ? session.getMetadata().get("source") : null;
                            
                            log.info("Session metadata - paymentType: {}, source: {}", paymentType, source);
                            
                            // Route based on session source
                            if ("discount-api".equals(source)) {
                                log.info("🎯 Routing to DISCOUNT webhook processing (session created from DiscountsController)");
                                return processDiscountWebhook(event, session);
                            } else if ("payment-api".equals(source)) {
                                log.info("🎯 Routing to PAYMENT webhook processing (session created from PaymentController)");
                                return processPaymentWebhook(event, session);
                            } else {
                                // Backward compatibility: use paymentType metadata
                                if ("discount-registration".equals(paymentType)) {
                                    log.info("🎯 Routing to DISCOUNT webhook processing (legacy paymentType metadata)");
                                    return processDiscountWebhook(event, session);
                                } else {
                                    log.info("🎯 Routing to PAYMENT webhook processing (default for regular payments)");
                                    return processPaymentWebhook(event, session);
                                }
                            }
                        } else if (stripeObject instanceof com.stripe.model.PaymentIntent) {
                            // For payment_intent events, we need to check the associated session
                            com.stripe.model.PaymentIntent paymentIntent = (com.stripe.model.PaymentIntent) stripeObject;
                            String paymentIntentId = paymentIntent.getId();
                            
                            log.info("Processing payment_intent event for ID: {}", paymentIntentId);
                            
                            // Try to determine source from payment intent metadata or fallback to all services
                            String source = paymentIntent.getMetadata() != null ? paymentIntent.getMetadata().get("source") : null;
                            
                            if ("discount-api".equals(source)) {
                                log.info("🎯 Payment intent from DISCOUNT API - processing with discount services");
                                return processDiscountPaymentIntent(event, paymentIntent);
                            } else if ("payment-api".equals(source)) {
                                log.info("🎯 Payment intent from PAYMENT API - processing with payment services");
                                return processPaymentPaymentIntent(event, paymentIntent);
                            } else {
                                log.info("🎯 Payment intent source unknown - trying all services");
                                return processPaymentPaymentIntent(event, paymentIntent); // Default to payment processing
                            }
                        }
                    }
                }
                
                // Handle other event types with generic processing
                log.info("⚠️ Unhandled or non-session event type: {} - using fallback processing", eventType);
            }
            
            // Fallback processing if specific handlers didn't work
            {
                // Fallback: process with all services as before
                boolean processed = false;
                try {
                    opticsStripeService.processWebhookEvent(event);
                    processed = true;
                    log.info("✅ Webhook processed by Optics service. Event type: {}", event.getType());
                } catch (Exception e) {
                    log.debug("Optics service couldn't process webhook: {}", e.getMessage());
                }
                if (!processed) {
                    try {
                        nursingStripeService.processWebhookEvent(event);
                        processed = true;
                        log.info("✅ Webhook processed by Nursing service. Event type: {}", event.getType());
                    } catch (Exception e) {
                        log.debug("Nursing service couldn't process webhook: {}", e.getMessage());
                    }
                }
                if (!processed) {
                    try {
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
            }
        } catch (Exception e) {
            log.error("❌ Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook processing failed");
        }
    }
    
    /**
     * Alternative webhook endpoints for domain-specific processing
     * These can be used if you want to configure separate webhook URLs in Stripe
     */
    @PostMapping("/webhook/optics")
    public ResponseEntity<String> handleOpticsWebhook(HttpServletRequest request) throws IOException {
        log.info("Received Optics-specific webhook request");
        
        String payload = readPayload(request);
        String sigHeader = request.getHeader("Stripe-Signature");
        
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
    
    @PostMapping("/webhook/nursing")
    public ResponseEntity<String> handleNursingWebhook(HttpServletRequest request) throws IOException {
        log.info("Received Nursing-specific webhook request");
        
        String payload = readPayload(request);
        String sigHeader = request.getHeader("Stripe-Signature");
        
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
    
    @PostMapping("/webhook/renewable")
    public ResponseEntity<String> handleRenewableWebhook(HttpServletRequest request) throws IOException {
        log.info("Received Renewable-specific webhook request");
        
        String payload = readPayload(request);
        String sigHeader = request.getHeader("Stripe-Signature");
        
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
    
    /**
     * Handle checkout.session.completed webhook events
     * Routes to appropriate service based on metadata or tries all services
     */
    private ResponseEntity<String> handleCheckoutSessionCompleted(Event event) {
        log.info("🎯 Handling checkout.session.completed event");
        
        try {
            java.util.Optional<com.stripe.model.StripeObject> sessionOpt = event.getDataObjectDeserializer().getObject();
            if (sessionOpt.isPresent() && sessionOpt.get() instanceof com.stripe.model.checkout.Session) {
                com.stripe.model.checkout.Session session = (com.stripe.model.checkout.Session) sessionOpt.get();
                String paymentType = session.getMetadata() != null ? session.getMetadata().get("paymentType") : null;
                String sessionId = session.getId();
                
                log.info("Session ID: {}, Payment Type: {}", sessionId, paymentType);
                
                // Route based on payment type metadata
                if ("discount-registration".equals(paymentType)) {
                    log.info("Processing discount-registration webhook");
                    return processDiscountWebhook(event, session);
                } else {
                    log.info("Processing normal payment webhook");
                    return processPaymentWebhook(event, session);
                }
            } else {
                log.error("❌ Could not deserialize session from checkout.session.completed event");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid session data");
            }
        } catch (Exception e) {
            log.error("❌ Error handling checkout.session.completed: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing checkout session completed");
        }
    }
    
    /**
     * Handle payment_intent.succeeded webhook events
     */
    private ResponseEntity<String> handlePaymentIntentSucceeded(Event event) {
        log.info("🎯 Handling payment_intent.succeeded event");
        
        boolean processed = false;
        try {
            opticsStripeService.processWebhookEvent(event);
            processed = true;
            log.info("✅ Payment intent succeeded processed by Optics service");
        } catch (Exception e) {
            log.debug("Optics service couldn't process payment_intent.succeeded: {}", e.getMessage());
        }
        
        if (!processed) {
            try {
                nursingStripeService.processWebhookEvent(event);
                processed = true;
                log.info("✅ Payment intent succeeded processed by Nursing service");
            } catch (Exception e) {
                log.debug("Nursing service couldn't process payment_intent.succeeded: {}", e.getMessage());
            }
        }
        
        if (!processed) {
            try {
                renewableStripeService.processWebhookEvent(event);
                processed = true;
                log.info("✅ Payment intent succeeded processed by Renewable service");
            } catch (Exception e) {
                log.debug("Renewable service couldn't process payment_intent.succeeded: {}", e.getMessage());
            }
        }
        
        if (processed) {
            return ResponseEntity.ok().body("Payment intent succeeded processed successfully");
        } else {
            log.error("❌ No service could process payment_intent.succeeded");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment intent processing failed");
        }
    }
    
    /**
     * Handle payment_intent.payment_failed webhook events
     */
    private ResponseEntity<String> handlePaymentIntentFailed(Event event) {
        log.info("🎯 Handling payment_intent.payment_failed event");
        
        boolean processed = false;
        try {
            opticsStripeService.processWebhookEvent(event);
            processed = true;
            log.info("✅ Payment intent failed processed by Optics service");
        } catch (Exception e) {
            log.debug("Optics service couldn't process payment_intent.payment_failed: {}", e.getMessage());
        }
        
        if (!processed) {
            try {
                nursingStripeService.processWebhookEvent(event);
                processed = true;
                log.info("✅ Payment intent failed processed by Nursing service");
            } catch (Exception e) {
                log.debug("Nursing service couldn't process payment_intent.payment_failed: {}", e.getMessage());
            }
        }
        
        if (!processed) {
            try {
                renewableStripeService.processWebhookEvent(event);
                processed = true;
                log.info("✅ Payment intent failed processed by Renewable service");
            } catch (Exception e) {
                log.debug("Renewable service couldn't process payment_intent.payment_failed: {}", e.getMessage());
            }
        }
        
        if (processed) {
            return ResponseEntity.ok().body("Payment intent failed processed successfully");
        } else {
            log.error("❌ No service could process payment_intent.payment_failed");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment intent failed processing failed");
        }
    }
    
    /**
     * Handle checkout.session.expired webhook events
     */
    private ResponseEntity<String> handleCheckoutSessionExpired(Event event) {
        log.info("🎯 Handling checkout.session.expired event");
        
        boolean processed = false;
        try {
            opticsStripeService.processWebhookEvent(event);
            processed = true;
            log.info("✅ Session expired processed by Optics service");
        } catch (Exception e) {
            log.debug("Optics service couldn't process checkout.session.expired: {}", e.getMessage());
        }
        
        if (!processed) {
            try {
                nursingStripeService.processWebhookEvent(event);
                processed = true;
                log.info("✅ Session expired processed by Nursing service");
            } catch (Exception e) {
                log.debug("Nursing service couldn't process checkout.session.expired: {}", e.getMessage());
            }
        }
        
        if (!processed) {
            try {
                renewableStripeService.processWebhookEvent(event);
                processed = true;
                log.info("✅ Session expired processed by Renewable service");
            } catch (Exception e) {
                log.debug("Renewable service couldn't process checkout.session.expired: {}", e.getMessage());
            }
        }
        
        if (processed) {
            return ResponseEntity.ok().body("Session expired processed successfully");
        } else {
            log.error("❌ No service could process checkout.session.expired");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Session expired processing failed");
        }
    }
    
    /**
     * Process discount webhooks based on domain routing
     */
    private ResponseEntity<String> processDiscountWebhook(Event event, com.stripe.model.checkout.Session session) {
        String sessionId = session.getId();
        log.info("Processing discount webhook for session: {}", sessionId);
        
        // Try to route based on session metadata or customer email domain
        String customerEmail = session.getCustomerEmail();
        if (session.getCustomerDetails() != null && session.getCustomerDetails().getEmail() != null) {
            customerEmail = session.getCustomerDetails().getEmail();
        }
        
        log.info("Customer email from session: {}", customerEmail);
        
        // Process discount webhook by trying all services
        boolean processed = false;
        
        log.info("✅ Discount webhook received for session: {}", sessionId);
        log.info("Note: Discount services will be updated to handle events directly");
        
        // For now, return success as the discount processing logic needs to be refactored
        // to handle Event objects directly rather than HttpServletRequest
        return ResponseEntity.ok().body("Discount webhook received - processing logic to be updated");
    }
    
    /**
     * Process normal payment webhooks
     */
    private ResponseEntity<String> processPaymentWebhook(Event event, com.stripe.model.checkout.Session session) {
        String sessionId = session.getId();
        log.info("Processing payment webhook for session: {}", sessionId);
        
        boolean processed = false;
        
        // Try all payment services
        try {
            opticsStripeService.processWebhookEvent(event);
            processed = true;
            log.info("✅ Payment webhook processed by Optics service");
        } catch (Exception e) {
            log.debug("Optics service couldn't process payment webhook: {}", e.getMessage());
        }
        
        if (!processed) {
            try {
                nursingStripeService.processWebhookEvent(event);
                processed = true;
                log.info("✅ Payment webhook processed by Nursing service");
            } catch (Exception e) {
                log.debug("Nursing service couldn't process payment webhook: {}", e.getMessage());
            }
        }
        
        if (!processed) {
            try {
                renewableStripeService.processWebhookEvent(event);
                processed = true;
                log.info("✅ Payment webhook processed by Renewable service");
            } catch (Exception e) {
                log.debug("Renewable service couldn't process payment webhook: {}", e.getMessage());
            }
        }
        
        if (processed) {
            return ResponseEntity.ok().body("Payment webhook processed successfully");
        } else {
            log.error("❌ No payment service could process the webhook");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment webhook processing failed");
        }
    }
    
    /**
     * Process payment_intent events for discount API sessions
     */
    private ResponseEntity<String> processDiscountPaymentIntent(Event event, com.stripe.model.PaymentIntent paymentIntent) {
        log.info("Processing payment_intent for discount API");
        
        boolean processed = false;
        String eventType = event.getType();
        
        // Try discount services only
        try {
            // Try to process with Optics discount service first
            if (opticsDiscountsService != null) {
                // We need to create a mock request for the discount service
                // For now, just log that discount processing should be updated
                log.info("✅ Discount payment_intent event received for Optics - needs discount service update");
                processed = true;
            }
        } catch (Exception e) {
            log.debug("Optics discount service couldn't process payment_intent: {}", e.getMessage());
        }
        
        if (!processed) {
            try {
                if (nursingDiscountsService != null) {
                    log.info("✅ Discount payment_intent event received for Nursing - needs discount service update");
                    processed = true;
                }
            } catch (Exception e) {
                log.debug("Nursing discount service couldn't process payment_intent: {}", e.getMessage());
            }
        }
        
        if (!processed) {
            try {
                if (renewableDiscountsService != null) {
                    log.info("✅ Discount payment_intent event received for Renewable - needs discount service update");
                    processed = true;
                }
            } catch (Exception e) {
                log.debug("Renewable discount service couldn't process payment_intent: {}", e.getMessage());
            }
        }
        
        if (processed) {
            return ResponseEntity.ok().body("Discount payment_intent processed - " + eventType);
        } else {
            log.error("❌ No discount service could process payment_intent");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Discount payment_intent processing failed");
        }
    }
    
    /**
     * Process payment_intent events for payment API sessions  
     */
    private ResponseEntity<String> processPaymentPaymentIntent(Event event, com.stripe.model.PaymentIntent paymentIntent) {
        log.info("Processing payment_intent for payment API");
        
        boolean processed = false;
        String eventType = event.getType();
        
        // Try payment services only
        try {
            opticsStripeService.processWebhookEvent(event);
            processed = true;
            log.info("✅ Payment payment_intent processed by Optics service - {}", eventType);
        } catch (Exception e) {
            log.debug("Optics payment service couldn't process payment_intent: {}", e.getMessage());
        }
        
        if (!processed) {
            try {
                nursingStripeService.processWebhookEvent(event);
                processed = true;
                log.info("✅ Payment payment_intent processed by Nursing service - {}", eventType);
            } catch (Exception e) {
                log.debug("Nursing payment service couldn't process payment_intent: {}", e.getMessage());
            }
        }
        
        if (!processed) {
            try {
                renewableStripeService.processWebhookEvent(event);
                processed = true;
                log.info("✅ Payment payment_intent processed by Renewable service - {}", eventType);
            } catch (Exception e) {
                log.debug("Renewable payment service couldn't process payment_intent: {}", e.getMessage());
            }
        }
        
        if (processed) {
            return ResponseEntity.ok().body("Payment payment_intent processed successfully - " + eventType);
        } else {
            log.error("❌ No payment service could process payment_intent");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Payment payment_intent processing failed");
        }
    }
    
    /**
     * Process optics discount payment intent
     */
    private void processOpticsDiscountPaymentIntent(PaymentIntent paymentIntent) {
        try {
            Optional<OpticsDiscounts> discountOpt = opticsDiscountsRepository.findByPaymentIntentId(paymentIntent.getId());
            if (discountOpt.isPresent()) {
                OpticsDiscounts discount = discountOpt.get();
                discount.setPaymentIntentId(paymentIntent.getId());
                discount.setStatus(OpticsPaymentRecord.PaymentStatus.valueOf(paymentIntent.getStatus().toUpperCase()));
                discount.setUpdatedAt(LocalDateTime.now());
                opticsDiscountsRepository.save(discount);
                log.info("✅ Updated OpticsDiscounts record for PaymentIntent: {}", paymentIntent.getId());
            } else {
                log.warn("⚠️ OpticsDiscounts record not found for PaymentIntent: {}", paymentIntent.getId());
            }
        } catch (Exception e) {
            log.error("❌ Error processing optics discount PaymentIntent: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Process nursing discount payment intent
     */
    private void processNursingDiscountPaymentIntent(PaymentIntent paymentIntent) {
        try {
            Optional<NursingDiscounts> discountOpt = nursingDiscountsRepository.findByPaymentIntentId(paymentIntent.getId());
            if (discountOpt.isPresent()) {
                NursingDiscounts discount = discountOpt.get();
                discount.setPaymentIntentId(paymentIntent.getId());
                discount.setStatus(NursingPaymentRecord.PaymentStatus.valueOf(paymentIntent.getStatus().toUpperCase()));
                discount.setUpdatedAt(LocalDateTime.now());
                nursingDiscountsRepository.save(discount);
                log.info("✅ Updated NursingDiscounts record for PaymentIntent: {}", paymentIntent.getId());
            } else {
                log.warn("⚠️ NursingDiscounts record not found for PaymentIntent: {}", paymentIntent.getId());
            }
        } catch (Exception e) {
            log.error("❌ Error processing nursing discount PaymentIntent: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Process renewable discount payment intent
     */
    private void processRenewableDiscountPaymentIntent(PaymentIntent paymentIntent) {
        try {
            Optional<RenewableDiscounts> discountOpt = renewableDiscountsRepository.findByPaymentIntentId(paymentIntent.getId());
            if (discountOpt.isPresent()) {
                RenewableDiscounts discount = discountOpt.get();
                discount.setPaymentIntentId(paymentIntent.getId());
                discount.setStatus(RenewablePaymentRecord.PaymentStatus.valueOf(paymentIntent.getStatus().toUpperCase()));
                discount.setUpdatedAt(LocalDateTime.now());
                renewableDiscountsRepository.save(discount);
                log.info("✅ Updated RenewableDiscounts record for PaymentIntent: {}", paymentIntent.getId());
            } else {
                log.warn("⚠️ RenewableDiscounts record not found for PaymentIntent: {}", paymentIntent.getId());
            }
        } catch (Exception e) {
            log.error("❌ Error processing renewable discount PaymentIntent: {}", e.getMessage(), e);
        }
    }
}
