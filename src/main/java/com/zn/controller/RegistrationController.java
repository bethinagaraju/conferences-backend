package com.zn.controller;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zn.dto.PriceCalculationRequestDTO;
import com.zn.dto.PricingConfigResponseDTO;
import com.zn.nursing.entity.NursingPresentationType;
import com.zn.nursing.entity.NursingPricingConfig;
import com.zn.nursing.entity.NursingRegistrationForm;
import com.zn.nursing.repository.INursingPresentationTypeRepo;
import com.zn.nursing.repository.INursingPricingConfigRepository;
import com.zn.nursing.repository.INursingRegistrationFormRepository;
import com.zn.optics.entity.OpticsPresentationType;
// Vertical-specific entities and repositories for domain-based routing
import com.zn.optics.entity.OpticsPricingConfig;
import com.zn.optics.entity.OpticsRegistrationForm;
import com.zn.optics.repository.IOpricsRegistrationFormRepository;
import com.zn.optics.repository.IOpticsPresentationTypeRepo;
import com.zn.optics.repository.IOpticsPricingConfigRepository;
import com.zn.polymers.entity.PolymersPricingConfig;
import com.zn.polymers.entity.PolymersRegistrationForm;
import com.zn.polymers.repository.IPolymersPresentationTypeRepo;
import com.zn.polymers.repository.IPolymersPricingConfigRepository;
import com.zn.polymers.repository.IPolymersRegistrationFormRepository;
import com.zn.renewable.entity.RenewablePresentationType;
import com.zn.renewable.entity.RenewablePricingConfig;
import com.zn.renewable.entity.RenewableRegistrationForm;
import com.zn.renewable.repository.IRenewablePresentationTypeRepo;
import com.zn.renewable.repository.IRenewablePricingConfigRepository;
import com.zn.renewable.repository.IRenewableRegistrationFormRepository;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/registration")
@Slf4j
public class RegistrationController {

    // Vertical-specific repositories for domain-based routing
    @Autowired
    private IOpticsPricingConfigRepository opticsPricingConfigRepo;
    @Autowired
    private IOpricsRegistrationFormRepository opticsRegistrationFormRepository;
    @Autowired
    private IOpticsPresentationTypeRepo opticsPresentationTypeRepo;
    
    @Autowired
    private INursingPricingConfigRepository nursingPricingConfigRepo;
    @Autowired
    private INursingRegistrationFormRepository nursingRegistrationFormRepository;
    @Autowired
    private INursingPresentationTypeRepo nursingPresentationTypeRepo;
    @Autowired
private IPolymersPricingConfigRepository polymerPricingConfigRepo;
@Autowired
private IPolymersPresentationTypeRepo polymerPresentationTypeRepo;
@Autowired
private IPolymersRegistrationFormRepository polymerRegistrationFormRepository;

    @Autowired
    private IRenewablePricingConfigRepository renewablePricingConfigRepo;
    @Autowired
    private IRenewableRegistrationFormRepository renewableRegistrationFormRepository;
    @Autowired
    private IRenewablePresentationTypeRepo renewablePresentationTypeRepo;

    /**
     * Helper method to determine which domain the request is coming from
     * Domain-based routing: globallopmeet.com→optics, nursingmeet2026.com→nursing, globalrenewablemeet.com→renewable
     */
    private String getDomainFromRequest(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");
        
        if ((origin != null && origin.contains("globallopmeet.com")) || 
            (referer != null && referer.contains("globallopmeet.com"))) {
            return "optics";
        } else if ((origin != null && origin.contains("nursingmeet2026.com")) || 
                   (referer != null && referer.contains("nursingmeet2026.com"))) {
            return "nursing";
        } else if ((origin != null && origin.contains("globalrenewablemeet.com")) || 
                   (referer != null && referer.contains("globalrenewablemeet.com"))) {
            return "renewable";
        }else if ((origin != null && origin.contains("polyscienceconference.com")) || 
                   (referer != null && referer.contains("polyscienceconference.com"))) {
            return "polymers";
        }  else {
            // Default to nursing for backward compatibility
            return "nursing";
        }
    }

    
    @PostMapping("/get-pricing-config")
    public ResponseEntity<?> getPricingConfigs(@RequestBody PriceCalculationRequestDTO request, HttpServletRequest httpRequest) {
        if (request == null) {
            log.warn("Received null price calculation request.");
            return ResponseEntity.badRequest().body("Price calculation request is required.");
        }
        
        String domain = getDomainFromRequest(httpRequest);
        log.info("Received request: {} from domain: {}", request, domain);
        
        // Route to appropriate vertical service based on domain
        switch (domain) {
            case "optics":
                return handleOpticsRequest(request);
            case "nursing":
                return handleNursingRequest(request);
            case "renewable":
                return handleRenewableRequest(request);
            case "polymers":
                return handlePolymersRequest(request);
            default:
                log.warn("Unknown domain: {}, defaulting to nursing", domain);
                return handleNursingRequest(request);
        }
    }
    
    private ResponseEntity<?> handleOpticsRequest(PriceCalculationRequestDTO request) {
        log.info("Handling optics pricing request with optics-specific repository");
        
        Optional<OpticsPresentationType> ptOpt = opticsPresentationTypeRepo.findByType(request.getPresentationType());
        if (ptOpt.isEmpty()) {
            log.warn("Invalid presentation type: {}", request.getPresentationType());
            return ResponseEntity.badRequest().body("Invalid presentation type: " + request.getPresentationType());
        }
        
        OpticsPresentationType ptEntity = ptOpt.get();
        log.info("Resolved OpticsPresentationType entity: {}", ptEntity);
        
        List<OpticsPricingConfig> results;
        switch (request.getRegistrationType()) {
            case "REGISTRATION_ONLY":
                log.info("Fetching optics pricing config with NO accommodation for presentation type: {}", ptEntity.getType());
                results = opticsPricingConfigRepo.findAllByPresentationTypeAndNoAccommodation(ptEntity);
                break;
            case "REGISTRATION_AND_ACCOMMODATION":
                log.info("Fetching optics pricing config WITH accommodation: nights={}, guests={}, type={}",
                        request.getNumberOfNights(), request.getNumberOfGuests(), ptEntity.getType());
                results = opticsPricingConfigRepo.findAllByPresentationTypeAndAccommodationDetails(
                        ptEntity, request.getNumberOfNights(), request.getNumberOfGuests());
                break;
            default:
                log.warn("Invalid registration type: {}", request.getRegistrationType());
                return ResponseEntity.badRequest().body("Invalid registration type.");
        }
        
        if (results.isEmpty()) {
            log.warn("No optics pricing configurations found for type={}, registrationType={}, nights={}, guests={}",
                    request.getPresentationType(), request.getRegistrationType(),
                    request.getNumberOfNights(), request.getNumberOfGuests());
            return ResponseEntity.status(404).body("No pricing configurations found for the provided criteria.");
        }
        
        List<PricingConfigResponseDTO> dtoList = results.stream().map(p -> {
            PricingConfigResponseDTO dto = new PricingConfigResponseDTO();
            dto.setId(p.getId());
            dto.setTotalPrice(p.getTotalPrice());
            dto.setProcessingFeePercent(p.getProcessingFeePercent());
            // Note: Type casting may be needed based on interface hierarchy
            dto.setPresentationType((com.zn.Ientity.IPresentationType) p.getPresentationType());
            dto.setAccommodationOption((com.zn.Ientity.IAccommodation) p.getAccommodationOption());
            return dto;
        }).collect(Collectors.toList());
        
        log.info("Returning {} optics pricing config(s).", dtoList.size());
        return ResponseEntity.ok(dtoList);
    }
    
    private ResponseEntity<?> handleNursingRequest(PriceCalculationRequestDTO request) {
        log.info("Handling nursing pricing request with nursing-specific repository");
        
        Optional<NursingPresentationType> ptOpt = nursingPresentationTypeRepo.findByType(request.getPresentationType());
        if (ptOpt.isEmpty()) {
            log.warn("Invalid presentation type: {}", request.getPresentationType());
            return ResponseEntity.badRequest().body("Invalid presentation type: " + request.getPresentationType());
        }
        
        NursingPresentationType ptEntity = ptOpt.get();
        log.info("Resolved NursingPresentationType entity: {}", ptEntity);
        
        List<NursingPricingConfig> results;
        switch (request.getRegistrationType()) {
            case "REGISTRATION_ONLY":
                log.info("Fetching nursing pricing config with NO accommodation for presentation type: {}", ptEntity.getType());
                results = nursingPricingConfigRepo.findAllByPresentationTypeAndNoAccommodation(ptEntity);
                break;
            case "REGISTRATION_AND_ACCOMMODATION":
                log.info("Fetching nursing pricing config WITH accommodation: nights={}, guests={}, type={}",
                        request.getNumberOfNights(), request.getNumberOfGuests(), ptEntity.getType());
                results = nursingPricingConfigRepo.findAllByPresentationTypeAndAccommodationDetails(
                        ptEntity, request.getNumberOfNights(), request.getNumberOfGuests());
                break;
            default:
                log.warn("Invalid registration type: {}", request.getRegistrationType());
                return ResponseEntity.badRequest().body("Invalid registration type.");
        }
        
        if (results.isEmpty()) {
            log.warn("No nursing pricing configurations found for type={}, registrationType={}, nights={}, guests={}",
                    request.getPresentationType(), request.getRegistrationType(),
                    request.getNumberOfNights(), request.getNumberOfGuests());
            return ResponseEntity.status(404).body("No pricing configurations found for the provided criteria.");
        }
        
        List<PricingConfigResponseDTO> dtoList = results.stream().map(p -> {
            PricingConfigResponseDTO dto = new PricingConfigResponseDTO();
            dto.setId(p.getId());
            dto.setTotalPrice(p.getTotalPrice());
            dto.setProcessingFeePercent(p.getProcessingFeePercent());
            // Note: Type casting may be needed based on interface hierarchy
            dto.setPresentationType((com.zn.Ientity.IPresentationType) p.getPresentationType());
            dto.setAccommodationOption((com.zn.Ientity.IAccommodation) p.getAccommodationOption());
            return dto;
        }).collect(Collectors.toList());
        
        log.info("Returning {} nursing pricing config(s).", dtoList.size());
        return ResponseEntity.ok(dtoList);
    }
    
    private ResponseEntity<?> handleRenewableRequest(PriceCalculationRequestDTO request) {
        log.info("Handling renewable pricing request with renewable-specific repository");
        
        Optional<RenewablePresentationType> ptOpt = renewablePresentationTypeRepo.findByType(request.getPresentationType());
        if (ptOpt.isEmpty()) {
            log.warn("Invalid presentation type: {}", request.getPresentationType());
            return ResponseEntity.badRequest().body("Invalid presentation type: " + request.getPresentationType());
        }
        
        RenewablePresentationType ptEntity = ptOpt.get();
        log.info("Resolved RenewablePresentationType entity: {}", ptEntity);
        
        List<RenewablePricingConfig> results;
        switch (request.getRegistrationType()) {
            case "REGISTRATION_ONLY":
                log.info("Fetching renewable pricing config with NO accommodation for presentation type: {}", ptEntity.getType());
                results = renewablePricingConfigRepo.findAllByPresentationTypeAndNoAccommodation(ptEntity);
                break;
            case "REGISTRATION_AND_ACCOMMODATION":
                log.info("Fetching renewable pricing config WITH accommodation: nights={}, guests={}, type={}",
                        request.getNumberOfNights(), request.getNumberOfGuests(), ptEntity.getType());
                results = renewablePricingConfigRepo.findAllByPresentationTypeAndAccommodationDetails(
                        ptEntity, request.getNumberOfNights(), request.getNumberOfGuests());
                break;
            default:
                log.warn("Invalid registration type: {}", request.getRegistrationType());
                return ResponseEntity.badRequest().body("Invalid registration type.");
        }
        
        if (results.isEmpty()) {
            log.warn("No renewable pricing configurations found for type={}, registrationType={}, nights={}, guests={}",
                    request.getPresentationType(), request.getRegistrationType(),
                    request.getNumberOfNights(), request.getNumberOfGuests());
            return ResponseEntity.status(404).body("No pricing configurations found for the provided criteria.");
        }
        
        List<PricingConfigResponseDTO> dtoList = results.stream().map(p -> {
            PricingConfigResponseDTO dto = new PricingConfigResponseDTO();
            dto.setId(p.getId());
            dto.setTotalPrice(p.getTotalPrice());
            dto.setProcessingFeePercent(p.getProcessingFeePercent());
            // Note: Type casting may be needed based on interface hierarchy
            dto.setPresentationType((com.zn.Ientity.IPresentationType) p.getPresentationType());
            dto.setAccommodationOption((com.zn.Ientity.IAccommodation) p.getAccommodationOption());
            return dto;
        }).collect(Collectors.toList());
        
        log.info("Returning {} renewable pricing config(s).", dtoList.size());
        return ResponseEntity.ok(dtoList);
    }


    @GetMapping("/get-all-presentation-types")
    public ResponseEntity<?> getAllPresentationTypes(HttpServletRequest httpRequest) {
        String domain = getDomainFromRequest(httpRequest);
        log.info("Getting presentation types for domain: {}", domain);
        
        switch (domain) {
            case "optics":
                return ResponseEntity.ok(opticsPresentationTypeRepo.findAll());
            case "nursing":
                return ResponseEntity.ok(nursingPresentationTypeRepo.findAll());
            case "renewable":
                return ResponseEntity.ok(renewablePresentationTypeRepo.findAll());
            default:
                log.warn("Unknown domain: {}, defaulting to nursing", domain);
                return ResponseEntity.ok(nursingPresentationTypeRepo.findAll());
        }
    }
    
    @GetMapping("/get-all-pricing-configs")
    public ResponseEntity<?> getAllPricingConfigs(HttpServletRequest httpRequest) {
        String domain = getDomainFromRequest(httpRequest);
        log.info("Getting pricing configs for domain: {}", domain);
        
        switch (domain) {
            case "optics":
                return ResponseEntity.ok(opticsPricingConfigRepo.findAll());
            case "nursing":
                return ResponseEntity.ok(nursingPricingConfigRepo.findAll());
            case "renewable":
                return ResponseEntity.ok(renewablePricingConfigRepo.findAll());
            default:
                log.warn("Unknown domain: {}, defaulting to nursing", domain);
                return ResponseEntity.ok(nursingPricingConfigRepo.findAll());
        }
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody OpticsRegistrationForm request, HttpServletRequest httpRequest) {
        String domain = getDomainFromRequest(httpRequest);
        log.info("Registering user for domain: {}", domain);
        
        switch (domain) {
            case "optics":
                return handleOpticsRegistration(request);
            case "nursing":
                return handleNursingRegistration(request);
            case "renewable":
                return handleRenewableRegistration(request);
            case "polymers":
                return handlePolymersRegistration(request);
            default:
                log.warn("Unknown domain: {}, defaulting to nursing", domain);
                return handleNursingRegistration(request);
        }
    }


private ResponseEntity<?> handlePolymersRequest(PriceCalculationRequestDTO request) {
    log.info("Handling polymers pricing request");
    Optional<com.zn.polymers.entity.PolymersPresentationType> ptOpt = polymerPresentationTypeRepo.findByType(request.getPresentationType());
    if (ptOpt.isEmpty()) {
        log.warn("Invalid presentation type: {}", request.getPresentationType());
        return ResponseEntity.badRequest().body("Invalid presentation type: " + request.getPresentationType());
    }
    com.zn.polymers.entity.PolymersPresentationType ptEntity = ptOpt.get();
    List<com.zn.polymers.entity.PolymersPricingConfig> results;
    switch (request.getRegistrationType()) {
        case "REGISTRATION_ONLY":
            log.info("Fetching polymer pricing config with NO accommodation for presentation type: {}", ptEntity.getType());
            results = polymerPricingConfigRepo.findAllByPresentationTypeAndNoAccommodation(ptEntity);
            break;
        case "REGISTRATION_AND_ACCOMMODATION":
            log.info("Fetching polymer pricing config WITH accommodation: nights={}, guests={}, type={}",
                    request.getNumberOfNights(), request.getNumberOfGuests(), ptEntity.getType());
            results = polymerPricingConfigRepo.findAllByPresentationTypeAndAccommodationDetails(
                    ptEntity, request.getNumberOfNights(), request.getNumberOfGuests());
            break;
        default:
            log.warn("Invalid registration type: {}", request.getRegistrationType());
            return ResponseEntity.badRequest().body("Invalid registration type.");
    }
    if (results.isEmpty()) {    
        log.warn("No polymer pricing configurations found for type={}, registrationType={}, nights={}, guests={}",
                request.getPresentationType(), request.getRegistrationType(),
                request.getNumberOfNights(), request.getNumberOfGuests());
        return ResponseEntity.status(404).body("No pricing configurations found for the provided criteria.");
    }
    List<PricingConfigResponseDTO> dtoList = results.stream().map(p -> {
        PricingConfigResponseDTO dto = new PricingConfigResponseDTO();
        dto.setId(p.getId());
        dto.setTotalPrice(p.getTotalPrice());
        dto.setProcessingFeePercent(p.getProcessingFeePercent());
        dto.setPresentationType((com.zn.Ientity.IPresentationType) p.getPresentationType());
        dto.setAccommodationOption((com.zn.Ientity.IAccommodation) p.getAccommodationOption());
        return dto;
    }).collect(Collectors.toList());
    log.info("Returning {} polymer pricing config(s).", dtoList.size());
    return ResponseEntity.ok(dtoList);
}

private ResponseEntity<?> handlePolymersRegistration(Object request) {
    log.info("Handling polymers registration");
    try {
        PolymersRegistrationForm polymerRequest = (PolymersRegistrationForm) request;
        Optional<PolymersPricingConfig> pcOpt = polymerPricingConfigRepo.findById(polymerRequest.getPricingConfig().getId());
        if (pcOpt.isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid pricingConfig ID.");
        }
        PolymersPricingConfig pc = pcOpt.get();
        if (polymerRequest.getAmountPaid() == null ||
            polymerRequest.getAmountPaid().compareTo(pc.getTotalPrice()) != 0) {
            return ResponseEntity.badRequest().body("AmountPaid must match the PricingConfig totalPrice.");
        }
        polymerRequest.setPricingConfig(pc);
        PolymersRegistrationForm saved = polymerRegistrationFormRepository.save(polymerRequest);
        return ResponseEntity.ok(saved);
    } catch (ClassCastException e) {
        log.error("Failed to cast request to PolymersRegistrationForm", e);
        return ResponseEntity.badRequest().body("Invalid request format for polymers registration");
    } catch (Exception e) {
        log.error("Error processing polymer registration", e);
        return ResponseEntity.status(500).body("Error processing registration");
    }
// Removed extra closing brace to fix syntax error
    }
    
    private ResponseEntity<?> handleOpticsRegistration(Object request) {
        log.info("Handling optics registration with optics-specific repository");
        
        try {
            // Cast to the appropriate entiy type
            OpticsRegistrationForm opticsRequest = (OpticsRegistrationForm) request;
            
            Optional<OpticsPricingConfig> pcOpt = opticsPricingConfigRepo.findById(opticsRequest.getPricingConfig().getId());
            if (pcOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Invalid pricingConfig ID.");
            }

            OpticsPricingConfig pc = pcOpt.get();

            // Validate amount
            if (opticsRequest.getAmountPaid() == null ||
                opticsRequest.getAmountPaid().compareTo(pc.getTotalPrice()) != 0) {
                return ResponseEntity.badRequest().body("AmountPaid must match the PricingConfig totalPrice.");
            }

            opticsRequest.setPricingConfig(pc);
            OpticsRegistrationForm saved = opticsRegistrationFormRepository.save(opticsRequest);
            return ResponseEntity.ok(saved);
            
        } catch (ClassCastException e) {
            log.error("Failed to cast request to OpticsRegistrationForm", e);
            return ResponseEntity.badRequest().body("Invalid request format for optics registration");
        } catch (Exception e) {
            log.error("Error processing optics registration", e);
            return ResponseEntity.status(500).body("Error processing registration");
        }
    }
    
    private ResponseEntity<?> handleNursingRegistration(Object request) {
        log.info("Handling nursing registration with nursing-specific repository");
        
        try {
            // Cast to the appropriate entity type
            NursingRegistrationForm nursingRequest = (NursingRegistrationForm) request;
            
            Optional<NursingPricingConfig> pcOpt = nursingPricingConfigRepo.findById(nursingRequest.getPricingConfig().getId());
            if (pcOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Invalid pricingConfig ID.");
            }

            NursingPricingConfig pc = pcOpt.get();

            // Validate amount
            if (nursingRequest.getAmountPaid() == null ||
                nursingRequest.getAmountPaid().compareTo(pc.getTotalPrice()) != 0) {
                return ResponseEntity.badRequest().body("AmountPaid must match the PricingConfig totalPrice.");
            }

            nursingRequest.setPricingConfig(pc);
            NursingRegistrationForm saved = nursingRegistrationFormRepository.save(nursingRequest);
            return ResponseEntity.ok(saved);
            
        } catch (ClassCastException e) {
            log.error("Failed to cast request to NursingRegistrationForm", e);
            return ResponseEntity.badRequest().body("Invalid request format for nursing registration");
        } catch (Exception e) {
            log.error("Error processing nursing registration", e);
            return ResponseEntity.status(500).body("Error processing registration");
        }
    }
    
    private ResponseEntity<?> handleRenewableRegistration(Object request) {
        log.info("Handling renewable registration with renewable-specific repository");
        
        try {
            // Cast to the appropriate entity type
            RenewableRegistrationForm renewableRequest = (RenewableRegistrationForm) request;
            
            Optional<RenewablePricingConfig> pcOpt = renewablePricingConfigRepo.findById(renewableRequest.getPricingConfig().getId());
            if (pcOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Invalid pricingConfig ID.");
            }

            RenewablePricingConfig pc = pcOpt.get();

            // Validate amount
            if (renewableRequest.getAmountPaid() == null ||
                renewableRequest.getAmountPaid().compareTo(pc.getTotalPrice()) != 0) {
                return ResponseEntity.badRequest().body("AmountPaid must match the PricingConfig totalPrice.");
            }

            renewableRequest.setPricingConfig(pc);
            RenewableRegistrationForm saved = renewableRegistrationFormRepository.save(renewableRequest);
            return ResponseEntity.ok(saved);
            
        } catch (ClassCastException e) {
            log.error("Failed to cast request to RenewableRegistrationForm", e);
            return ResponseEntity.badRequest().body("Invalid request format for renewable registration");
        } catch (Exception e) {
            log.error("Error processing renewable registration", e);
            return ResponseEntity.status(500).body("Error processing registration");
        }
    }

    // get all accommodation options
    @GetMapping("/get-all-accommodation-options")
    public ResponseEntity<?> getAllRegistrationForms(HttpServletRequest httpRequest) {
        String domain = getDomainFromRequest(httpRequest);
        log.info("Getting accommodation options for domain: {}", domain);
        
        switch (domain) {
            case "optics":
                return ResponseEntity.ok("Optics accommodation options - implementation needed");
            case "nursing":
                return ResponseEntity.ok("Nursing accommodation options - implementation needed");
            case "renewable":
                return ResponseEntity.ok("Renewable accommodation options - implementation needed");
            default:
                log.warn("Unknown domain: {}, defaulting to nursing", domain);
                return ResponseEntity.ok("Default accommodation options - implementation needed");
        }
    }
}
