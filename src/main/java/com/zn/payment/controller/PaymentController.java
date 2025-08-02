
package com.zn.payment.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
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
import com.zn.nursing.entity.NursingRegistrationForm;
import com.zn.nursing.repository.INursingRegistrationFormRepository;
import com.zn.optics.entity.OpticsPricingConfig;
import com.zn.optics.entity.OpticsRegistrationForm;
import com.zn.optics.repository.IOpricsRegistrationFormRepository;
import com.zn.optics.repository.IOpticsPricingConfigRepository;
import com.zn.payment.dto.CheckoutRequest;
import com.zn.payment.dto.NursingPaymentResponseDTO;
import com.zn.payment.dto.OpticsPaymentResponseDTO;
import com.zn.payment.dto.RenewablePaymentResponseDTO;
import com.zn.payment.nursing.entity.NursingDiscounts;
import com.zn.payment.nursing.entity.NursingPaymentRecord;
import com.zn.payment.nursing.repository.NursingDiscountsRepository;
import com.zn.payment.nursing.service.NursingDiscountsService;
import com.zn.payment.nursing.service.NursingStripeService;
import com.zn.payment.optics.entity.OpticsDiscounts;
import com.zn.payment.optics.entity.OpticsPaymentRecord;
import com.zn.payment.optics.repository.OpticsDiscountsRepository;
import com.zn.payment.optics.service.OpticsDiscountsService;
import com.zn.payment.optics.service.OpticsStripeService;
import com.zn.payment.renewable.entity.RenewableDiscounts;
import com.zn.payment.renewable.entity.RenewablePaymentRecord;
import com.zn.payment.renewable.repository.RenewableDiscountsRepository;
import com.zn.payment.renewable.service.RenewableDiscountsService;
import com.zn.payment.renewable.service.RenewaleStripeService;
import com.zn.renewable.entity.RenewableRegistrationForm;
import com.zn.renewable.repository.IRenewableRegistrationFormRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/payment")
@Slf4j
public class PaymentController {

    // Nursing and Renewable registration repositories
    @Autowired
    private INursingRegistrationFormRepository nursingRegistrationFormRepository;

    @Autowired
    private IRenewableRegistrationFormRepository renewableRegistrationFormRepository;

    @Autowired
    private com.zn.nursing.repository.INursingPricingConfigRepository nursingPricingConfigRepository;

    @Autowired
    private com.zn.renewable.repository.IRenewablePricingConfigRepository renewablePricingConfigRepository;

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

    // --- POLYMERS REPOSITORIES & SERVICES ---
    @Autowired
    private com.zn.polymers.repository.IPolymersRegistrationFormRepository polymersRegistrationFormRepository;
    @Autowired
    private com.zn.polymers.repository.IPolymersPricingConfigRepository polymersPricingConfigRepository;
    @Autowired
    private com.zn.payment.polymers.service.PolymersStripeService polymersStripeService;


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
        } else if (origin.contains("polyscienceconference.com")) {
            log.info("Processing Polymer checkout for domain: {}", origin);
            return handlePolymerCheckout(request, pricingConfigId);
        } else {
            log.error("Unknown frontend domain: {}", origin);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse("unknown_frontend_domain"));
        }
    }

    // --- POLYMERS CHECKOUT LOGIC ---
    private ResponseEntity<com.zn.payment.polymers.dto.PolymersPaymentResponseDTO> handlePolymerCheckout(CheckoutRequest request, Long pricingConfigId) {
        try {
            if (pricingConfigId == null) {
                log.error("pricingConfigId is mandatory but not provided");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(createPolymersErrorResponse("pricing_config_id_required"));
            }

            com.zn.polymers.entity.PolymersPricingConfig pricingConfig = polymersPricingConfigRepository.findById(pricingConfigId)
                    .orElseThrow(() -> new IllegalArgumentException("Pricing config not found with ID: " + pricingConfigId));
            java.math.BigDecimal backendTotalPrice = pricingConfig.getTotalPrice();
            Long unitAmountInCents = backendTotalPrice.multiply(new java.math.BigDecimal(100)).longValue();
            request.setUnitAmount(unitAmountInCents); // Stripe expects cents

            // Save registration record before payment
            com.zn.polymers.entity.PolymersRegistrationForm registrationForm = new com.zn.polymers.entity.PolymersRegistrationForm();
            registrationForm.setName(request.getName() != null ? request.getName() : "");
            registrationForm.setPhone(request.getPhone() != null ? request.getPhone() : "");
            registrationForm.setEmail(request.getEmail());
            registrationForm.setInstituteOrUniversity(request.getInstituteOrUniversity() != null ? request.getInstituteOrUniversity() : "");
            registrationForm.setCountry(request.getCountry() != null ? request.getCountry() : "");
            registrationForm.setPricingConfig(pricingConfig);
            registrationForm.setAmountPaid(backendTotalPrice);
            com.zn.polymers.entity.PolymersRegistrationForm savedRegistration = polymersRegistrationFormRepository.save(registrationForm);
            log.info("✅ Polymers registration form created and saved with ID: {}", savedRegistration.getId());

            // Call polymers service - this will save to polymers_payment_records table
            com.zn.payment.polymers.dto.PolymersPaymentResponseDTO response = polymersStripeService.createCheckoutSessionWithPricingValidation(request, pricingConfigId);
            log.info("Polymers checkout session created successfully. Session ID: {}", response.getSessionId());
            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Validation error creating polymers checkout session: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createPolymersErrorResponse("validation_failed"));
        } catch (Exception e) {
            log.error("Error creating polymers checkout session: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createPolymersErrorResponse("failed"));
        }
    }

    private com.zn.payment.polymers.dto.PolymersPaymentResponseDTO createPolymersErrorResponse(String errorMessage) {
        // Use constructor instead of builder (assume fields: paymentStatus, description)
        return new com.zn.payment.polymers.dto.PolymersPaymentResponseDTO(errorMessage, "Error: " + errorMessage);
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
            } else if (origin != null && origin.contains("polyscienceconference.com")) {
                com.zn.payment.polymers.dto.PolymersPaymentResponseDTO responseDTO = polymersStripeService.retrieveSession(id);
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
            } else if (origin != null && origin.contains("polyscienceconference.com")) {
                com.zn.payment.polymers.dto.PolymersPaymentResponseDTO responseDTO = polymersStripeService.expireSession(id);
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
        log.info("#####################   Received payment webhook request ######################");
        String payload;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(request.getInputStream()))) {
            payload = reader.lines().collect(Collectors.joining("\n"));
        }

        String sigHeader = request.getHeader("Stripe-Signature");
        log.info("Webhook payload length: {}, Signature header present: {}", payload.length(), sigHeader != null);

        // EARLY DISCOUNT DETECTION: Check raw payload for discount metadata before any processing
        if (payload != null && (
            payload.contains("\"source\":\"discount-api\"") || 
            payload.contains("\"paymentType\":\"discount-registration\"") ||
            payload.contains("\"productName\":\"POLYMER SUMMIT 2026 DISCOUNT REGISTRATION\"") ||
            payload.contains("\"productName\":\"OPTICS 2026 DISCOUNT REGISTRATION\"") ||
            payload.contains("\"productName\":\"NURSING") ||
            payload.contains("\"productName\":\"RENEWABLE") ||
            payload.contains("POLYMER SUMMIT 2026 DISCOUNT REQUEST") ||
            payload.contains("OPTICS 2026 DISCOUNT REQUEST") ||
            payload.contains("NURSING") && payload.contains("DISCOUNT REQUEST") ||
            payload.contains("RENEWABLE") && payload.contains("DISCOUNT REQUEST") ||
            payload.contains("DISCOUNT-") // orderReference pattern
        )) {
            log.info("🛑 [EARLY DISCOUNT DETECTION] This webhook contains discount payment metadata. Returning 200 OK without processing in PaymentController.");
            log.info("🛑 [EARLY DISCOUNT DETECTION] Detected patterns: source=discount-api, paymentType=discount-registration, or discount product names");
            log.info("🛑 [EARLY DISCOUNT DETECTION] Discount webhooks should be handled by /api/discounts/webhook endpoint.");
            return ResponseEntity.ok("Discount payment webhook ignored in PaymentController - should be handled by DiscountsController");
        }

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

            if (event != null) {
                // 1. Try to extract productName, paymentType, and source from event metadata
                String productName = null;
                String paymentType = null;
                String source = null;
                
                // First try reflection-based extraction
                try {
                    java.util.Optional<com.stripe.model.StripeObject> stripeObjectOpt = event.getDataObjectDeserializer().getObject();
                    if (stripeObjectOpt.isPresent()) {
                        com.stripe.model.StripeObject stripeObject = stripeObjectOpt.get();
                        java.util.Map<String, String> metadata = null;
                        try {
                            java.lang.reflect.Method getMetadata = stripeObject.getClass().getMethod("getMetadata");
                            Object metaObj = getMetadata.invoke(stripeObject);
                            if (metaObj instanceof java.util.Map) {
                                metadata = (java.util.Map<String, String>) metaObj;
                                if (metadata != null) {
                                    productName = metadata.get("productName");
                                    paymentType = metadata.get("paymentType");
                                    source = metadata.get("source");
                                }
                            }
                        } catch (Exception ex) {
                            log.warn("[Webhook Debug] Could not extract metadata from object: {}", ex.getMessage());
                        }
                    }
                } catch (Exception ex) {
                    log.warn("[Webhook Debug] Could not extract productName/paymentType/source from event: {}", ex.getMessage());
                }

                // If reflection failed, try JSON parsing as fallback
                if (productName == null && paymentType == null && source == null) {
                    try {
                        String eventJson = event.toJson();
                        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(eventJson);
                        com.fasterxml.jackson.databind.JsonNode metadataNode = root.path("data").path("object").path("metadata");
                        if (metadataNode != null && !metadataNode.isMissingNode()) {
                            if (metadataNode.has("productName")) {
                                productName = metadataNode.get("productName").asText();
                            }
                            if (metadataNode.has("paymentType")) {
                                paymentType = metadataNode.get("paymentType").asText();
                            }
                            if (metadataNode.has("source")) {
                                source = metadataNode.get("source").asText();
                            }
                        }
                    } catch (Exception ex) {
                        log.warn("[Webhook Debug] JSON parsing for metadata also failed: {}", ex.getMessage());
                    }
                }

                // Debug the extracted metadata values
                log.info("[Webhook Debug] Extracted metadata - productName: {}, paymentType: {}, source: {}", productName, paymentType, source);

                // If paymentType is discount-registration or source is discount-api, DO NOT process in this webhook (handled by /api/discounts/webhook)
                if ((paymentType != null && paymentType.equalsIgnoreCase("discount-registration")) ||
                    (source != null && source.equalsIgnoreCase("discount-api")) ||
                    (productName != null && (
                        productName.contains("DISCOUNT REGISTRATION") ||
                        productName.contains("DISCOUNT REQUEST") ||
                        productName.toLowerCase().contains("discount")
                    ))) {
                    log.info("[Webhook Debug] Skipping discount payment (paymentType={}, source={}, productName={}) in /api/payment/webhook. Only /api/discounts/webhook should process discount payments.", paymentType, source, productName);
                    return ResponseEntity.ok("Discount payment ignored in payment webhook");
                }

                // 2. Try to extract productName for normal routing
                if (productName != null && !productName.isEmpty()) {
                    String productNameUpper = productName.toUpperCase();
                    log.info("[Webhook Debug] Found productName: {}", productName);
                    if (productNameUpper.contains("OPTICS")) {
                        log.info("[Webhook Debug] Routing to Optics service by productName match.");
                        opticsStripeService.processWebhookEvent(event);
                        log.info("✅ Webhook processed by Optics service by productName");
                        return ResponseEntity.ok().body("Webhook processed by Optics service by productName: " + productName);
                    } else if (productNameUpper.contains("NURSING")) {
                        log.info("[Webhook Debug] Routing to Nursing service by productName match.");
                        nursingStripeService.processWebhookEvent(event);
                        log.info("✅ Webhook processed by Nursing service by productName");
                        return ResponseEntity.ok().body("Webhook processed by Nursing service by productName: " + productName);
                    } else if (productNameUpper.contains("RENEWABLE")) {
                        log.info("[Webhook Debug] Routing to Renewable service by productName match.");
                        renewableStripeService.processWebhookEvent(event);
                        log.info("✅ Webhook processed by Renewable service by productName");
                        return ResponseEntity.ok().body("Webhook processed by Renewable service by productName: " + productName);
                    } else if (productNameUpper.contains("POLYMER") || productNameUpper.contains("POLYMERS")) {
                        log.info("[Webhook Debug] Routing to Polymers service by productName match.");
                        polymersStripeService.processWebhookEvent(event);
                        log.info("✅ Webhook processed by Polymers service by productName");
                        return ResponseEntity.ok().body("Webhook processed by Polymers service by productName: " + productName);
                    } else {
                        log.warn("[Webhook Debug] productName did not match any site, will try success_url fallback.");
                    }
                }
                // 3. Fallback: Try to extract success_url from event JSON and check for discount patterns in raw JSON
                String successUrl = null;
                String rawJsonEventData = null;
                try {
                    com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    rawJsonEventData = event.toJson();
                    com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(rawJsonEventData);
                    successUrl = findSuccessUrlRecursive(root);
                } catch (Exception ex) {
                    log.warn("[Webhook Debug] Could not extract success_url from event JSON: {}", ex.getMessage());
                }

                // CRITICAL: Check raw JSON for discount patterns even if metadata extraction failed
                if (rawJsonEventData != null) {
                    String jsonUpper = rawJsonEventData.toUpperCase();
                    if (jsonUpper.contains("DISCOUNT REGISTRATION") || 
                        jsonUpper.contains("DISCOUNT REQUEST") ||
                        jsonUpper.contains("\"PAYMENTTYPE\":\"DISCOUNT-REGISTRATION\"") ||
                        jsonUpper.contains("\"SOURCE\":\"DISCOUNT-API\"") ||
                        jsonUpper.contains("OPTICS 2026 DISCOUNT") ||
                        jsonUpper.contains("NURSING 2026 DISCOUNT") ||
                        jsonUpper.contains("RENEWABLE 2025 DISCOUNT") ||
                        jsonUpper.contains("POLYMER SUMMIT 2026 DISCOUNT")) {
                        log.info("[Webhook Debug] DISCOUNT PATTERN DETECTED in raw JSON. Skipping payment webhook processing. This should be handled by /api/discounts/webhook.");
                        return ResponseEntity.ok("Discount payment detected in raw JSON - ignored in payment webhook");
                    }
                }

                if (successUrl != null && !successUrl.isEmpty()) {
                    String urlLower = successUrl.toLowerCase();
                    if (urlLower.contains("globallopmeet.com") || urlLower.contains("optics")) {
                        log.info("[Webhook Debug] Routing to Optics service by success_url/domain match.");
                        opticsStripeService.processWebhookEvent(event);
                        log.info("✅ Webhook processed by Optics service by success_url");
                        return ResponseEntity.ok().body("Webhook processed by Optics service by success_url");
                    } else if (urlLower.contains("nursingmeet2026.com") || urlLower.contains("nursing")) {
                        log.info("[Webhook Debug] Routing to Nursing service by success_url/domain match.");
                        nursingStripeService.processWebhookEvent(event);
                        log.info("✅ Webhook processed by Nursing service by success_url");
                        return ResponseEntity.ok().body("Webhook processed by Nursing service by success_url");
                    } else if (urlLower.contains("globalrenewablemeet.com") || urlLower.contains("renewable")) {
                        log.info("[Webhook Debug] Routing to Renewable service by success_url/domain match.");
                        renewableStripeService.processWebhookEvent(event);
                        log.info("✅ Webhook processed by Renewable service by success_url");
                        return ResponseEntity.ok().body("Webhook processed by Renewable service by success_url");
                    } else if (urlLower.contains("polyscienceconference.com") || urlLower.contains("polymer")) {
                        log.info("[Webhook Debug] Routing to Polymers service by success_url/domain match.");
                        polymersStripeService.processWebhookEvent(event);
                        log.info("✅ Webhook processed by Polymers service by success_url");
                        return ResponseEntity.ok().body("Webhook processed by Polymers service by success_url");
                    } else {
                        log.warn("[Webhook Debug] success_url did not match any site. No table will be updated.");
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("success_url did not match any site. No table updated.");
                    }
                } else {
                    log.error("[Webhook Debug] No productName or success_url found in event. No table will be updated.");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No productName or success_url found. No table updated.");
                }
            }
            // If event is null
            log.error("❌ Stripe event could not be parsed");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook event parse failed");
        } catch (Exception e) {
            log.error("❌ Error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook processing failed");
        }
    }
    // Helper: Recursively search for success_url in a JsonNode
    private String findSuccessUrlRecursive(com.fasterxml.jackson.databind.JsonNode node) {
        if (node == null) return null;
        if (node.isObject()) {
            java.util.Iterator<java.util.Map.Entry<String, com.fasterxml.jackson.databind.JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                java.util.Map.Entry<String, com.fasterxml.jackson.databind.JsonNode> entry = fields.next();
                if ("success_url".equals(entry.getKey()) && entry.getValue().isTextual()) {
                    return entry.getValue().asText();
                }
                String found = findSuccessUrlRecursive(entry.getValue());
                if (found != null) return found;
            }
        } else if (node.isArray()) {
            for (com.fasterxml.jackson.databind.JsonNode item : node) {
                String found = findSuccessUrlRecursive(item);
                if (found != null) return found;
            }
        }
        return null;
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
            // log.info("✅ Optics webhook processed successfully. Event type: {}", event.getType()); // Removed event type log
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
            // log.info("✅ Nursing webhook processed successfully. Event type: {}", event.getType()); // Removed event type log
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
            // log.info("✅ Renewable webhook processed successfully. Event type: {}", event.getType()); // Removed event type log
            return ResponseEntity.ok().body("Renewable webhook processed successfully");
        } catch (Exception e) {
            log.error("❌ Error processing Renewable webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Renewable webhook processing failed");
        }
    }
    
    @PostMapping("/webhook/polymers")
    public ResponseEntity<String> handlePolymersWebhook(HttpServletRequest request) throws IOException {
        log.info("Received Polymers-specific webhook request");
        
        String payload = readPayload(request);
        String sigHeader = request.getHeader("Stripe-Signature");
        
        try {
            Event event = polymersStripeService.constructWebhookEvent(payload, sigHeader);
            polymersStripeService.processWebhookEvent(event);
            return ResponseEntity.ok().body("Polymers webhook processed successfully");
        } catch (Exception e) {
            log.error("❌ Error processing Polymers webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Polymers webhook processing failed");
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

            // Always use backend value for payment amount - CORE BUSINESS LOGIC
            com.zn.nursing.entity.NursingPricingConfig pricingConfig = nursingPricingConfigRepository.findById(pricingConfigId)
                    .orElseThrow(() -> new IllegalArgumentException("Pricing config not found with ID: " + pricingConfigId));
            java.math.BigDecimal backendTotalPrice = pricingConfig.getTotalPrice();
            Long unitAmountInCents = backendTotalPrice.multiply(new java.math.BigDecimal(100)).longValue();
            request.setUnitAmount(unitAmountInCents); // Stripe expects cents

            // Save registration record before payment (like optics)
            NursingRegistrationForm registrationForm = new NursingRegistrationForm();
            registrationForm.setName(request.getName() != null ? request.getName() : "");
            registrationForm.setPhone(request.getPhone() != null ? request.getPhone() : "");
            registrationForm.setEmail(request.getEmail());
            registrationForm.setInstituteOrUniversity(request.getInstituteOrUniversity() != null ? request.getInstituteOrUniversity() : "");
            registrationForm.setCountry(request.getCountry() != null ? request.getCountry() : "");
            registrationForm.setPricingConfig(pricingConfig);
            registrationForm.setAmountPaid(backendTotalPrice);
            NursingRegistrationForm savedRegistration = nursingRegistrationFormRepository.save(registrationForm);
            log.info("✅ Nursing registration form created and saved with ID: {}", savedRegistration.getId());

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

            // Always use backend value for payment amount - CORE BUSINESS LOGIC
            com.zn.renewable.entity.RenewablePricingConfig pricingConfig = renewablePricingConfigRepository.findById(pricingConfigId)
                    .orElseThrow(() -> new IllegalArgumentException("Pricing config not found with ID: " + pricingConfigId));
            java.math.BigDecimal backendTotalPrice = pricingConfig.getTotalPrice();
            Long unitAmountInCents = backendTotalPrice.multiply(new java.math.BigDecimal(100)).longValue();
            request.setUnitAmount(unitAmountInCents); // Stripe expects cents

            // Save registration record before payment (like optics)
            RenewableRegistrationForm registrationForm = new RenewableRegistrationForm();
            registrationForm.setName(request.getName() != null ? request.getName() : "");
            registrationForm.setPhone(request.getPhone() != null ? request.getPhone() : "");
            registrationForm.setEmail(request.getEmail());
            registrationForm.setInstituteOrUniversity(request.getInstituteOrUniversity() != null ? request.getInstituteOrUniversity() : "");
            registrationForm.setCountry(request.getCountry() != null ? request.getCountry() : "");
            registrationForm.setPricingConfig(pricingConfig);
            registrationForm.setAmountPaid(backendTotalPrice);
            RenewableRegistrationForm savedRegistration = renewableRegistrationFormRepository.save(registrationForm);
            log.info("✅ Renewable registration form created and saved with ID: {}", savedRegistration.getId());

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
     * Process discount webhook: update payment status in discount tables if session found
     */
    private ResponseEntity<String> processDiscountWebhook(Event event, com.stripe.model.checkout.Session session) {
        String sessionId = session.getId();
        log.info("[processDiscountWebhook] Checking discount tables for session_id: {}", sessionId);
        boolean updated = false;
        if (opticsDiscountsRepository.findBySessionId(sessionId) != null) {
            log.info("Session found in OpticsDiscounts, updating status...");
            opticsDiscountsService.processWebhookEvent(event);
            updated = true;
        } else if (nursingDiscountsRepository.findBySessionId(sessionId) != null) {
            log.info("Session found in NursingDiscounts, updating status...");
            nursingDiscountsService.processWebhookEvent(event);
            updated = true;
        } else if (renewableDiscountsRepository.findBySessionId(sessionId) != null) {
            log.info("Session found in RenewableDiscounts, updating status...");
            renewableDiscountsService.processWebhookEvent(event);
            updated = true;
        }
        if (updated) {
            return ResponseEntity.ok("Discount payment status updated in discount table");
        } else {
            log.info("Session not found in any discount table, processing as normal payment...");
            return processPaymentWebhook(event, session);
        }
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
            if (opticsDiscountsService != null) {
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
