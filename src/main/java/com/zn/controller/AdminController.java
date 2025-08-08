
package com.zn.controller;


import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zn.adminentity.Admin;
import com.zn.adminservice.AdminService;
import com.zn.dto.AdminLoginRequestDTO;
import com.zn.dto.AdminResponseDTO;
import com.zn.dto.InterestedInOptionDTO;
import com.zn.dto.SessionOptionDTO;
import com.zn.exception.AdminAuthenticationException;
import com.zn.exception.DataProcessingException;
import com.zn.exception.ResourceNotFoundException;
import com.zn.security.JwtUtil;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/admin")
public class AdminController {	@Autowired
	private AdminService adminService;
	
	@Autowired
	private JwtUtil jwtUtil;
	

@Autowired private com.zn.optics.repository.IOpticsPresentationTypeRepo opticsPresentationTypeRepo;
@Autowired private com.zn.renewable.repository.IRenewablePresentationTypeRepo renewablePresentationTypeRepo;
@Autowired private com.zn.nursing.repository.INursingPresentationTypeRepo nursingPresentationTypeRepo;
@Autowired private com.zn.polymers.repository.IPolymersPresentationTypeRepo polymersPresentationTypeRepo;

@Autowired private com.zn.optics.repository.IOpticsAccommodationRepo opticsAccommodationRepo;
@Autowired private com.zn.renewable.repository.IRenewableAccommodationRepo renewableAccommodationRepo;
@Autowired private com.zn.nursing.repository.INursingAccommodationRepo nursingAccommodationRepo;
@Autowired private com.zn.polymers.repository.IPolymersAccommodationRepo polymersAccommodationRepo;

@Autowired private com.zn.optics.repository.IOpticsPricingConfigRepository opticsPricingConfigRepository;
@Autowired private com.zn.renewable.repository.IRenewablePricingConfigRepository renewablePricingConfigRepository;
@Autowired private com.zn.nursing.repository.INursingPricingConfigRepository nursingPricingConfigRepository;
@Autowired private com.zn.polymers.repository.IPolymersPricingConfigRepository polymersPricingConfigRepository;

	// login admin	
	@PostMapping("/api/admin/login")
	public ResponseEntity<?> loginAdmin(@RequestBody AdminLoginRequestDTO loginRequest, HttpServletResponse response) {
		try {
			if (loginRequest == null || loginRequest.getEmail() == null || loginRequest.getPassword() == null) {
				throw new IllegalArgumentException("Email and password are required");
			}

			if (loginRequest.getEmail().trim().isEmpty() || loginRequest.getPassword().trim().isEmpty()) {
				throw new IllegalArgumentException("Email and password cannot be empty");
			}

			// Create admin object for authentication
			Admin adminCredentials = new Admin();
			adminCredentials.setEmail(loginRequest.getEmail());
			adminCredentials.setPassword(loginRequest.getPassword());

			Admin admin = adminService.loginAdmin(adminCredentials);
			if (admin == null) {
				throw new AdminAuthenticationException("Invalid email or password");
			}			// Generate JWT token with role
			String token = jwtUtil.generateToken(admin.getEmail(), admin.getRole());			// Set JWT as HttpOnly cookie with production-ready settings
			ResponseCookie cookie = ResponseCookie.from("admin_jwt", token)
				.httpOnly(false)
				.secure(true) // Always true for production HTTPS
				.path("/")
				.maxAge(24 * 60 * 60) // 1 day
				.sameSite("None") // Required for cross-origin cookies
				.build();
			response.addHeader("Set-Cookie", cookie.toString());

			// Create response DTO with user info and token for production compatibility
			AdminResponseDTO adminResponse = new AdminResponseDTO(
				admin.getId().longValue(),
				admin.getEmail(),
				admin.getName(),
				admin.getRole()
			);
			
			// Create response with both user data and token for production
			Map<String, Object> responseBody = new HashMap<>();
			responseBody.put("user", adminResponse);
			responseBody.put("token", token); // Include token for production use
			
			return ResponseEntity.ok(responseBody);
		} catch (Exception e) {
			throw new AdminAuthenticationException("Login failed: " + e.getMessage(), e);
		}
	}

   // Optics session
   @PostMapping("/sessions/optics")
   @PreAuthorize("hasRole('ADMIN')")
   public ResponseEntity<String> insertOpticsSession(@RequestBody SessionOptionDTO dto) {
		   try {
				   if (dto == null || dto.getSessionOption() == null || dto.getSessionOption().trim().isEmpty()) {
						   throw new IllegalArgumentException("Session option is required and cannot be empty");
				   }
				   String result = adminService.insertOpticsSession(dto.getSessionOption().trim());
				   return ResponseEntity.ok(result);
		   } catch (Exception e) {
				   throw new DataProcessingException("Failed to insert session: " + e.getMessage(), e);
		   }
   }

   // Renewable session
   @PostMapping("/sessions/renewable")
   @PreAuthorize("hasRole('ADMIN')")
   public ResponseEntity<String> insertRenewableSession(@RequestBody SessionOptionDTO dto) {
		   try {
				   if (dto == null || dto.getSessionOption() == null || dto.getSessionOption().trim().isEmpty()) {
						   throw new IllegalArgumentException("Session option is required and cannot be empty");
				   }
				   String result = adminService.insertRenewableSession(dto.getSessionOption().trim());
				   return ResponseEntity.ok(result);
		   } catch (Exception e) {
				   throw new DataProcessingException("Failed to insert session: " + e.getMessage(), e);
		   }
   }

   // Nursing session
   @PostMapping("/sessions/nursing")
   @PreAuthorize("hasRole('ADMIN')")
   public ResponseEntity<String> insertNursingSession(@RequestBody SessionOptionDTO dto) {
		   try {
				   if (dto == null || dto.getSessionOption() == null || dto.getSessionOption().trim().isEmpty()) {
						   throw new IllegalArgumentException("Session option is required and cannot be empty");
				   }
				   String result = adminService.insertNursingSession(dto.getSessionOption().trim());
				   return ResponseEntity.ok(result);
		   } catch (Exception e) {
				   throw new DataProcessingException("Failed to insert session: " + e.getMessage(), e);
		   }
   }

   // Polymers session
   @PostMapping("/sessions/polymers")
   @PreAuthorize("hasRole('ADMIN')")
   public ResponseEntity<String> insertPolymersSession(@RequestBody SessionOptionDTO dto) {
		   try {
				   if (dto == null || dto.getSessionOption() == null || dto.getSessionOption().trim().isEmpty()) {
						   throw new IllegalArgumentException("Session option is required and cannot be empty");
				   }
				   String result = adminService.insertPolymersSession(dto.getSessionOption().trim());
				   return ResponseEntity.ok(result);
		   } catch (Exception e) {
				   throw new DataProcessingException("Failed to insert session: " + e.getMessage(), e);
		   }
   }
	// Optics interested-in
	@PostMapping("/interested-in/optics")
	public ResponseEntity<String> insertOpticsInterestedInOption(@RequestBody InterestedInOptionDTO dto) {
		try {
			if (dto == null || dto.getInterestedInOption() == null || dto.getInterestedInOption().trim().isEmpty()) {
				throw new IllegalArgumentException("Interested In option is required and cannot be empty");
			}
			adminService.insertOpticsInterestedInOption(dto.getInterestedInOption().trim());
			return ResponseEntity.ok("Interested In option inserted successfully (optics).");
		} catch (Exception e) {
			throw new DataProcessingException("Failed to insert interested in option: " + e.getMessage(), e);
		}
	}

	// Renewable interested-in
	@PostMapping("/interested-in/renewable")
	public ResponseEntity<String> insertRenewableInterestedInOption(@RequestBody InterestedInOptionDTO dto) {
		try {
			if (dto == null || dto.getInterestedInOption() == null || dto.getInterestedInOption().trim().isEmpty()) {
				throw new IllegalArgumentException("Interested In option is required and cannot be empty");
			}
			adminService.insertRenewableInterestedInOption(dto.getInterestedInOption().trim());
			return ResponseEntity.ok("Interested In option inserted successfully (renewable).");
		} catch (Exception e) {
			throw new DataProcessingException("Failed to insert interested in option: " + e.getMessage(), e);
		}
	}

	// Nursing interested-in
	@PostMapping("/interested-in/nursing")
	public ResponseEntity<String> insertNursingInterestedInOption(@RequestBody InterestedInOptionDTO dto) {
		try {
			if (dto == null || dto.getInterestedInOption() == null || dto.getInterestedInOption().trim().isEmpty()) {
				throw new IllegalArgumentException("Interested In option is required and cannot be empty");
			}
			adminService.insertNursingInterestedInOption(dto.getInterestedInOption().trim());
			return ResponseEntity.ok("Interested In option inserted successfully (nursing).");
		} catch (Exception e) {
			throw new DataProcessingException("Failed to insert interested in option: " + e.getMessage(), e);
		}
	}

	// Polymers interested-in
	@PostMapping("/interested-in/polymers")
	public ResponseEntity<String> insertPolymersInterestedInOption(@RequestBody InterestedInOptionDTO dto) {
		try {
			if (dto == null || dto.getInterestedInOption() == null || dto.getInterestedInOption().trim().isEmpty()) {
				throw new IllegalArgumentException("Interested In option is required and cannot be empty");
			}
			adminService.insertPolymersInterestedInOption(dto.getInterestedInOption().trim());
			return ResponseEntity.ok("Interested In option inserted successfully (polymers).");
		} catch (Exception e) {
			throw new DataProcessingException("Failed to insert interested in option: " + e.getMessage(), e);
		}
	}
		// insert acoommodation in Accommodation table
	// Optics accommodation
	@PostMapping("/api/admin/accommodation/optics")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<String> insertOpticsAccommodation(@RequestBody com.zn.optics.entity.OpticsAccommodation accommodation) {
		try {
			if (accommodation == null) {
				throw new IllegalArgumentException("Accommodation is required");
			}
			adminService.insertOpticsAccommodation(accommodation);
			return ResponseEntity.ok("Accommodation inserted successfully (optics).");
		} catch (Exception e) {
			throw new DataProcessingException("Failed to insert accommodation: " + e.getMessage(), e);
		}
	}

	// Renewable accommodation
	@PostMapping("/api/admin/accommodation/renewable")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<String> insertRenewableAccommodation(@RequestBody com.zn.renewable.entity.RenewableAccommodation accommodation) {
		try {
			if (accommodation == null) {
				throw new IllegalArgumentException("Accommodation is required");
			}
			adminService.insertRenewableAccommodation(accommodation);
			return ResponseEntity.ok("Accommodation inserted successfully (renewable).");
		} catch (Exception e) {
			throw new DataProcessingException("Failed to insert accommodation: " + e.getMessage(), e);
		}
	}

	// Nursing accommodation
	@PostMapping("/api/admin/accommodation/nursing")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<String> insertNursingAccommodation(@RequestBody com.zn.nursing.entity.NursingAccommodation accommodation) {
		try {
			if (accommodation == null) {
				throw new IllegalArgumentException("Accommodation is required");
			}
			adminService.insertNursingAccommodation(accommodation);
			return ResponseEntity.ok("Accommodation inserted successfully (nursing).");
		} catch (Exception e) {
			throw new DataProcessingException("Failed to insert accommodation: " + e.getMessage(), e);
		}
	}

	// Polymers accommodation
	@PostMapping("/api/admin/accommodation/polymers")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<String> insertPolymersAccommodation(@RequestBody com.zn.polymers.entity.PolymersAccommodation accommodation) {
		try {
			if (accommodation == null) {
				throw new IllegalArgumentException("Accommodation is required");
			}
			adminService.insertPolymersAccommodation(accommodation);
			return ResponseEntity.ok("Accommodation inserted successfully (polymers).");
		} catch (Exception e) {
			throw new DataProcessingException("Failed to insert accommodation: " + e.getMessage(), e);
		}
	}
	// Optics pricing config
	@PostMapping("/api/admin/pricing-config/optics")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> insertOpticsPricingConfig(@RequestBody com.zn.optics.entity.OpticsPricingConfig config) {
		try {
			adminService.insertOpticsPricingConfig(config);
			return ResponseEntity.ok("Pricing config inserted successfully (optics).");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("Failed to insert pricing config: " + e.getMessage());
		}
	}

	// Renewable pricing config
	@PostMapping("/api/admin/pricing-config/renewable")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> insertRenewablePricingConfig(@RequestBody com.zn.renewable.entity.RenewablePricingConfig config) {
		try {
			adminService.insertRenewablePricingConfig(config);
			return ResponseEntity.ok("Pricing config inserted successfully (renewable).");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("Failed to insert pricing config: " + e.getMessage());
		}
	}

	// Nursing pricing config
	@PostMapping("/api/admin/pricing-config/nursing")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> insertNursingPricingConfig(@RequestBody com.zn.nursing.entity.NursingPricingConfig config) {
		try {
			adminService.insertNursingPricingConfig(config);
			return ResponseEntity.ok("Pricing config inserted successfully (nursing).");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("Failed to insert pricing config: " + e.getMessage());
		}
	}

	// Polymers pricing config
	@PostMapping("/api/admin/pricing-config/polymers")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> insertPolymersPricingConfig(@RequestBody com.zn.polymers.entity.PolymersPricingConfig config) {
		try {
			adminService.insertPolymersPricingConfig(config);
			return ResponseEntity.ok("Pricing config inserted successfully (polymers).");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body("Failed to insert pricing config: " + e.getMessage());
		}
	}

	

		
		
//	    Long presentationTypeId = config.getPresentationType().getId();
//	    Long accommodationId = config.getAccommodationOption() != null
//	        ? config.getAccommodationOption().getId()
//	        : null;
//
//	    PricingConfig saved = adminService.insertPricingConfig(config, presentationTypeId, accommodationId);
//	    return ResponseEntity.ok(saved);
// get the pricing config details by id for each vertical
@PostMapping("/api/admin/pricing-config/details/optics/{id}")
public ResponseEntity<?> getOpticsPricingConfigDetails(@PathVariable Long id) {
	try {
		if (id == null) {
			throw new IllegalArgumentException("Pricing config ID is required");
		}
		com.zn.optics.entity.OpticsPricingConfig config = adminService.getOpticsPricingConfigById(id);
		if (config == null) {
			throw new ResourceNotFoundException("Optics pricing config not found with ID: " + id);
		}
		return ResponseEntity.ok(config);
	} catch (Exception e) {
		throw new DataProcessingException("Failed to retrieve optics pricing config: " + e.getMessage(), e);
	}
}

@PostMapping("/api/admin/pricing-config/details/renewable/{id}")
public ResponseEntity<?> getRenewablePricingConfigDetails(@PathVariable Long id) {
	try {
		if (id == null) {
			throw new IllegalArgumentException("Pricing config ID is required");
		}
		com.zn.renewable.entity.RenewablePricingConfig config = adminService.getRenewablePricingConfigById(id);
		if (config == null) {
			throw new ResourceNotFoundException("Renewable pricing config not found with ID: " + id);
		}
		return ResponseEntity.ok(config);
	} catch (Exception e) {
		throw new DataProcessingException("Failed to retrieve renewable pricing config: " + e.getMessage(), e);
	}
}

@PostMapping("/api/admin/pricing-config/details/nursing/{id}")
public ResponseEntity<?> getNursingPricingConfigDetails(@PathVariable Long id) {
	try {
		if (id == null) {
			throw new IllegalArgumentException("Pricing config ID is required");
		}
		com.zn.nursing.entity.NursingPricingConfig config = adminService.getNursingPricingConfigById(id);
		if (config == null) {
			throw new ResourceNotFoundException("Nursing pricing config not found with ID: " + id);
		}
		return ResponseEntity.ok(config);
	} catch (Exception e) {
		throw new DataProcessingException("Failed to retrieve nursing pricing config: " + e.getMessage(), e);
	}
}

@PostMapping("/api/admin/pricing-config/details/polymers/{id}")
public ResponseEntity<?> getPolymersPricingConfigDetails(@PathVariable Long id) {
	try {
		if (id == null) {
			throw new IllegalArgumentException("Pricing config ID is required");
		}
		com.zn.polymers.entity.PolymersPricingConfig config = adminService.getPolymersPricingConfigById(id);
		if (config == null) {
			throw new ResourceNotFoundException("Polymers pricing config not found with ID: " + id);
		}
		return ResponseEntity.ok(config);
	} catch (Exception e) {
		throw new DataProcessingException("Failed to retrieve polymers pricing config: " + e.getMessage(), e);
	}
}

// get all registration forms for each vertical
@PostMapping("/api/admin/registration-forms/optics")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> getAllOpticsRegistrationForms() {
	try {
		return ResponseEntity.ok(adminService.getAllOpticsRegistrationForms());
	} catch (Exception e) {
		throw new DataProcessingException("Failed to retrieve optics registration forms: " + e.getMessage(), e);
	}
}

@PostMapping("/api/admin/registration-forms/renewable")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> getAllRenewableRegistrationForms() {
	try {
		return ResponseEntity.ok(adminService.getAllRenewableRegistrationForms());
	} catch (Exception e) {
		throw new DataProcessingException("Failed to retrieve renewable registration forms: " + e.getMessage(), e);
	}
}

@PostMapping("/api/admin/registration-forms/nursing")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> getAllNursingRegistrationForms() {
	try {
		return ResponseEntity.ok(adminService.getAllNursingRegistrationForms());
	} catch (Exception e) {
		throw new DataProcessingException("Failed to retrieve nursing registration forms: " + e.getMessage(), e);
	}
}

@PostMapping("/api/admin/registration-forms/polymers")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> getAllPolymersRegistrationForms() {
	try {
		return ResponseEntity.ok(adminService.getAllPolymersRegistrationForms());
	} catch (Exception e) {
		throw new DataProcessingException("Failed to retrieve polymers registration forms: " + e.getMessage(), e);
	}
}
	


// get all abstract form submissions for each vertical

@GetMapping("/api/admin/abstract-submissions/optics")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> getAllOpticsAbstractSubmissions() {
	try {
		return ResponseEntity.ok(adminService.getAllOpticsFormSubmissions());
	} catch (Exception e) {
		throw new DataProcessingException("Failed to retrieve optics abstract submissions: " + e.getMessage(), e);
	}
}

@GetMapping("/api/admin/abstract-submissions/renewable")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> getAllRenewableAbstractSubmissions() {
	try {
		return ResponseEntity.ok(adminService.getAllRenewableFormSubmissions());
	} catch (Exception e) {
		throw new DataProcessingException("Failed to retrieve renewable abstract submissions: " + e.getMessage(), e);
	}
}

@GetMapping("/api/admin/abstract-submissions/nursing")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> getAllNursingAbstractSubmissions() {
	try {
		return ResponseEntity.ok(adminService.getAllNursingFormSubmissions());
	} catch (Exception e) {
		throw new DataProcessingException("Failed to retrieve nursing abstract submissions: " + e.getMessage(), e);
	}
}

@GetMapping("/api/admin/abstract-submissions/polymers")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> getAllPolymersAbstractSubmissions() {
	try {
		return ResponseEntity.ok(adminService.getAllPolymersFormSubmissions());
	} catch (Exception e) {
		throw new DataProcessingException("Failed to retrieve polymers abstract submissions: " + e.getMessage(), e);
	}
}
	
	// logout admin
	@PostMapping("/api/admin/logout")
	public ResponseEntity<String> logoutAdmin(HttpServletResponse response) {
		try {
			// Clear the JWT cookie
			ResponseCookie cookie = ResponseCookie.from("admin_jwt", "")
				.httpOnly(true)
				.secure(false) // set to true in production
				.path("/")
				.maxAge(0) // immediately expire
				.sameSite("Lax")
				.build();
			response.addHeader("Set-Cookie", cookie.toString());
			
			return ResponseEntity.ok("Logged out successfully");
		} catch (Exception e) {
			return ResponseEntity.status(500).body("Logout failed");
		}
	}
	// write a method to edit accomidation combo 
	 
	// Get all interested-in options for each vertical (admin)
	@GetMapping("/api/admin/interested-in/optics")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> getAllOpticsInterestedInOptions() {
		try {
			return ResponseEntity.ok(adminService.getAllOpticsInterestedInOptions());
		} catch (Exception e) {
			
			throw new DataProcessingException("Failed to retrieve optics interested-in options: " + e.getMessage(), e);
		}
	}

	@GetMapping("/api/admin/interested-in/renewable")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> getAllRenewableInterestedInOptions() {
		try {
			return ResponseEntity.ok(adminService.getAllRenewableInterestedInOptions());
		} catch (Exception e) {
			throw new DataProcessingException("Failed to retrieve renewable interested-in options: " + e.getMessage(), e);
		}
	}

	@GetMapping("/api/admin/interested-in/nursing")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> getAllNursingInterestedInOptions() {
		try {
			return ResponseEntity.ok(adminService.getAllNursingInterestedInOptions());
		} catch (Exception e) {
			throw new DataProcessingException("Failed to retrieve nursing interested-in options: " + e.getMessage(), e);
		}
	}

	@GetMapping("/api/admin/interested-in/polymers")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> getAllPolymersInterestedInOptions() {
		try {
			return ResponseEntity.ok(adminService.getAllPolymersInterestedInOptions());
		} catch (Exception e) {
			throw new DataProcessingException("Failed to retrieve polymers interested-in options: " + e.getMessage(), e);
		}
	}

	// Get all session options for each vertical (admin)
	@GetMapping("/api/admin/session-options/optics")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> getAllOpticsSessionOptions() {
		try {
			return ResponseEntity.ok(adminService.getAllOpticsSessionOptions());
		} catch (Exception e) {
			throw new DataProcessingException("Failed to retrieve optics session options: " + e.getMessage(), e);
		}
	}

	@GetMapping("/api/admin/session-options/renewable")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> getAllRenewableSessionOptions() {
		try {
			return ResponseEntity.ok(adminService.getAllRenewableSessionOptions());
		} catch (Exception e) {
			throw new DataProcessingException("Failed to retrieve renewable session options: " + e.getMessage(), e);
		}
	}

	@GetMapping("/api/admin/session-options/nursing")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> getAllNursingSessionOptions() {
		try {
			return ResponseEntity.ok(adminService.getAllNursingSessionOptions());
		} catch (Exception e) {
			throw new DataProcessingException("Failed to retrieve nursing session options: " + e.getMessage(), e);
		}
	}

	@GetMapping("/api/admin/session-options/polymers")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> getAllPolymersSessionOptions() {
		try {
			return ResponseEntity.ok(adminService.getAllPolymersSessionOptions());
		} catch (Exception e) {
			throw new DataProcessingException("Failed to retrieve polymers session options: " + e.getMessage(), e);
		}
	}
	

// Edit accommodation combo for each vertical
@PostMapping("/api/admin/accommodation/edit/optics/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> editOpticsAccommodation(@PathVariable Long id, @RequestBody com.zn.optics.entity.OpticsAccommodation updatedAccommodation) {
	try {
		Optional<com.zn.optics.entity.OpticsAccommodation> optionalAccommodation = opticsAccommodationRepo.findById(id);
		if (optionalAccommodation.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Optics Accommodation not found with ID: " + id);
		}
		com.zn.optics.entity.OpticsAccommodation accommodation = optionalAccommodation.get();
		accommodation.setNights(updatedAccommodation.getNights());
		accommodation.setGuests(updatedAccommodation.getGuests());
		accommodation.setPrice(updatedAccommodation.getPrice());
		opticsAccommodationRepo.save(accommodation);

		var pricingConfigs = opticsPricingConfigRepository.findByAccommodationOption(accommodation);
		for (com.zn.optics.entity.OpticsPricingConfig config : pricingConfigs) {
			config.calculateTotalPrice();
			opticsPricingConfigRepository.save(config);
		}
		return ResponseEntity.ok("Optics accommodation and related pricing configs updated successfully.");
	} catch (Exception ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update optics accommodation: " + ex.getMessage());
	}
}

@PostMapping("/api/admin/accommodation/edit/renewable/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> editRenewableAccommodation(@PathVariable Long id, @RequestBody com.zn.renewable.entity.RenewableAccommodation updatedAccommodation) {
	try {
		Optional<com.zn.renewable.entity.RenewableAccommodation> optionalAccommodation = renewableAccommodationRepo.findById(id);
		if (optionalAccommodation.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Renewable Accommodation not found with ID: " + id);
		}
		com.zn.renewable.entity.RenewableAccommodation accommodation = optionalAccommodation.get();
		accommodation.setNights(updatedAccommodation.getNights());
		accommodation.setGuests(updatedAccommodation.getGuests());
		accommodation.setPrice(updatedAccommodation.getPrice());
		renewableAccommodationRepo.save(accommodation);

		var pricingConfigs = renewablePricingConfigRepository.findByAccommodationOption(accommodation);
		for (com.zn.renewable.entity.RenewablePricingConfig config : pricingConfigs) {
			config.calculateTotalPrice();
			renewablePricingConfigRepository.save(config);
		}
		return ResponseEntity.ok("Renewable accommodation and related pricing configs updated successfully.");
	} catch (Exception ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update renewable accommodation: " + ex.getMessage());
	}
}

@PostMapping("/api/admin/accommodation/edit/nursing/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> editNursingAccommodation(@PathVariable Long id, @RequestBody com.zn.nursing.entity.NursingAccommodation updatedAccommodation) {
	try {
		Optional<com.zn.nursing.entity.NursingAccommodation> optionalAccommodation = nursingAccommodationRepo.findById(id);
		if (optionalAccommodation.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nursing Accommodation not found with ID: " + id);
		}
		com.zn.nursing.entity.NursingAccommodation accommodation = optionalAccommodation.get();
		accommodation.setNights(updatedAccommodation.getNights());
		accommodation.setGuests(updatedAccommodation.getGuests());
		accommodation.setPrice(updatedAccommodation.getPrice());
		nursingAccommodationRepo.save(accommodation);

		var pricingConfigs = nursingPricingConfigRepository.findByAccommodationOption(accommodation);
		for (com.zn.nursing.entity.NursingPricingConfig config : pricingConfigs) {
			config.calculateTotalPrice();
			nursingPricingConfigRepository.save(config);
		}
		return ResponseEntity.ok("Nursing accommodation and related pricing configs updated successfully.");
	} catch (Exception ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update nursing accommodation: " + ex.getMessage());
	}
}

@PostMapping("/api/admin/accommodation/edit/polymers/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> editPolymersAccommodation(@PathVariable Long id, @RequestBody com.zn.polymers.entity.PolymersAccommodation updatedAccommodation) {
	try {
		Optional<com.zn.polymers.entity.PolymersAccommodation> optionalAccommodation = polymersAccommodationRepo.findById(id);
		if (optionalAccommodation.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Polymers Accommodation not found with ID: " + id);
		}
		com.zn.polymers.entity.PolymersAccommodation accommodation = optionalAccommodation.get();
		accommodation.setNights(updatedAccommodation.getNights());
		accommodation.setGuests(updatedAccommodation.getGuests());
		accommodation.setPrice(updatedAccommodation.getPrice());
		polymersAccommodationRepo.save(accommodation);

		var pricingConfigs = polymersPricingConfigRepository.findByAccommodationOption(accommodation);
		for (com.zn.polymers.entity.PolymersPricingConfig config : pricingConfigs) {
			config.calculateTotalPrice();
			polymersPricingConfigRepository.save(config);
		}
		return ResponseEntity.ok("Polymers accommodation and related pricing configs updated successfully.");
	} catch (Exception ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update polymers accommodation: " + ex.getMessage());
	}
}

// Delete accommodation combo for each vertical
@PostMapping("/api/admin/accommodation/delete/optics/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> deleteOpticsAccommodation(@PathVariable Long id) {
	try {
		if (!opticsAccommodationRepo.existsById(id)) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Optics Accommodation not found with ID: " + id);
		}
		opticsAccommodationRepo.deleteById(id);
		return ResponseEntity.ok("Optics accommodation deleted successfully.");
	} catch (Exception e) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete optics accommodation: " + e.getMessage());
	}
}

@PostMapping("/api/admin/accommodation/delete/renewable/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> deleteRenewableAccommodation(@PathVariable Long id) {
	try {
		if (!renewableAccommodationRepo.existsById(id)) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Renewable Accommodation not found with ID: " + id);
		}
		renewableAccommodationRepo.deleteById(id);
		return ResponseEntity.ok("Renewable accommodation deleted successfully.");
	} catch (Exception e) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete renewable accommodation: " + e.getMessage());
	}
}

@PostMapping("/api/admin/accommodation/delete/nursing/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> deleteNursingAccommodation(@PathVariable Long id) {
	try {
		if (!nursingAccommodationRepo.existsById(id)) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nursing Accommodation not found with ID: " + id);
		}
		nursingAccommodationRepo.deleteById(id);
		return ResponseEntity.ok("Nursing accommodation deleted successfully.");
	} catch (Exception e) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete nursing accommodation: " + e.getMessage());
	}
}

@PostMapping("/api/admin/accommodation/delete/polymers/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> deletePolymersAccommodation(@PathVariable Long id) {
	try {
		if (!polymersAccommodationRepo.existsById(id)) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Polymers Accommodation not found with ID: " + id);
		}
		polymersAccommodationRepo.deleteById(id);
		return ResponseEntity.ok("Polymers accommodation deleted successfully.");
	} catch (Exception e) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete polymers accommodation: " + e.getMessage());
	}
}

// get all presentation types for each vertical
@GetMapping("/api/admin/presentation-types/optics")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> getAllOpticsPresentationTypes() {
	try {
		return ResponseEntity.ok(opticsPresentationTypeRepo.findAll());
	} catch (Exception e) {
		throw new DataProcessingException("Failed to retrieve optics presentation types: " + e.getMessage(), e);
	}
}

@GetMapping("/api/admin/presentation-types/renewable")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> getAllRenewablePresentationTypes() {
	try {
		return ResponseEntity.ok(renewablePresentationTypeRepo.findAll());
	} catch (Exception e) {
		throw new DataProcessingException("Failed to retrieve renewable presentation types: " + e.getMessage(), e);
	}
}

@GetMapping("/api/admin/presentation-types/nursing")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> getAllNursingPresentationTypes() {
	try {
		return ResponseEntity.ok(nursingPresentationTypeRepo.findAll());
	} catch (Exception e) {
		throw new DataProcessingException("Failed to retrieve nursing presentation types: " + e.getMessage(), e);
	}
}

@GetMapping("/api/admin/presentation-types/polymers")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> getAllPolymersPresentationTypes() {
	try {
		return ResponseEntity.ok(polymersPresentationTypeRepo.findAll());
	} catch (Exception e) {
		throw new DataProcessingException("Failed to retrieve polymers presentation types: " + e.getMessage(), e);
	}
}

// edit presentation type for each vertical
@PostMapping("/api/admin/presentation-type/edit/optics/{id}/{price}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> editOpticsPresentationType(@PathVariable Long id, @PathVariable BigDecimal price) {
	try {
		Optional<com.zn.optics.entity.OpticsPresentationType> optionalType = opticsPresentationTypeRepo.findById(id);
		if (optionalType.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Optics Presentation type not found with ID: " + id);
		}
		com.zn.optics.entity.OpticsPresentationType type = optionalType.get();
		type.setPrice(price);
		opticsPresentationTypeRepo.save(type);

		var pricingConfigs = opticsPricingConfigRepository.findByPresentationType(type);
		for (com.zn.optics.entity.OpticsPricingConfig config : pricingConfigs) {
			config.calculateTotalPrice();
			opticsPricingConfigRepository.save(config);
		}
		return ResponseEntity.ok("Optics presentation type and related pricing configs updated successfully.");
	} catch (Exception ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update optics presentation type: " + ex.getMessage());
	}
}

@PostMapping("/api/admin/presentation-type/edit/renewable/{id}/{price}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> editRenewablePresentationType(@PathVariable Long id, @PathVariable BigDecimal price) {
	try {
		Optional<com.zn.renewable.entity.RenewablePresentationType> optionalType = renewablePresentationTypeRepo.findById(id);
		if (optionalType.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Renewable Presentation type not found with ID: " + id);
		}
		com.zn.renewable.entity.RenewablePresentationType type = optionalType.get();
		type.setPrice(price);
		renewablePresentationTypeRepo.save(type);

		var pricingConfigs = renewablePricingConfigRepository.findByPresentationType(type);
		for (com.zn.renewable.entity.RenewablePricingConfig config : pricingConfigs) {
			config.calculateTotalPrice();
			renewablePricingConfigRepository.save(config);
		}
		return ResponseEntity.ok("Renewable presentation type and related pricing configs updated successfully.");
	} catch (Exception ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update renewable presentation type: " + ex.getMessage());
	}
}

@PostMapping("/api/admin/presentation-type/edit/nursing/{id}/{price}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> editNursingPresentationType(@PathVariable Long id, @PathVariable BigDecimal price) {
	try {
		Optional<com.zn.nursing.entity.NursingPresentationType> optionalType = nursingPresentationTypeRepo.findById(id);
		if (optionalType.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Nursing Presentation type not found with ID: " + id);
		}
		com.zn.nursing.entity.NursingPresentationType type = optionalType.get();
		type.setPrice(price);
		nursingPresentationTypeRepo.save(type);

		var pricingConfigs = nursingPricingConfigRepository.findByPresentationType(type);
		for (com.zn.nursing.entity.NursingPricingConfig config : pricingConfigs) {
			config.calculateTotalPrice();
			nursingPricingConfigRepository.save(config);
		}
		return ResponseEntity.ok("Nursing presentation type and related pricing configs updated successfully.");
	} catch (Exception ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update nursing presentation type: " + ex.getMessage());
	}
}

@PostMapping("/api/admin/presentation-type/edit/polymers/{id}/{price}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> editPolymersPresentationType(@PathVariable Long id, @PathVariable BigDecimal price) {
	try {
		Optional<com.zn.polymers.entity.PolymersPresentationType> optionalType = polymersPresentationTypeRepo.findById(id);
		if (optionalType.isEmpty()) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Polymers Presentation type not found with ID: " + id);
		}
		com.zn.polymers.entity.PolymersPresentationType type = optionalType.get();
		type.setPrice(price);
		polymersPresentationTypeRepo.save(type);

		var pricingConfigs = polymersPricingConfigRepository.findByPresentationType(type);
		for (com.zn.polymers.entity.PolymersPricingConfig config : pricingConfigs) {
			config.calculateTotalPrice();
			polymersPricingConfigRepository.save(config);
		}
		return ResponseEntity.ok("Polymers presentation type and related pricing configs updated successfully.");
	} catch (Exception ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update polymers presentation type: " + ex.getMessage());
	}
}

// Recalculate totalPrice for all PricingConfig rows for each vertical
@PostMapping("/api/admin/pricing-config/recalculate-all/optics")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> recalculateAllOpticsPricingConfigs() {
	try {
		Iterable<com.zn.optics.entity.OpticsPricingConfig> allConfigs = opticsPricingConfigRepository.findAll();
		int updatedCount = 0;
		for (com.zn.optics.entity.OpticsPricingConfig config : allConfigs) {
			opticsPricingConfigRepository.save(config);
			updatedCount++;
		}
		return ResponseEntity.ok("Recalculated totalPrice for " + updatedCount + " optics pricing configs.");
	} catch (Exception e) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to recalculate optics pricing configs: " + e.getMessage());
	}
}

@PostMapping("/api/admin/pricing-config/recalculate-all/renewable")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> recalculateAllRenewablePricingConfigs() {
	try {
		Iterable<com.zn.renewable.entity.RenewablePricingConfig> allConfigs = renewablePricingConfigRepository.findAll();
		int updatedCount = 0;
		for (com.zn.renewable.entity.RenewablePricingConfig config : allConfigs) {
			renewablePricingConfigRepository.save(config);
			updatedCount++;
		}
		return ResponseEntity.ok("Recalculated totalPrice for " + updatedCount + " renewable pricing configs.");
	} catch (Exception e) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to recalculate renewable pricing configs: " + e.getMessage());
	}
}

@PostMapping("/api/admin/pricing-config/recalculate-all/nursing")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> recalculateAllNursingPricingConfigs() {
	try {
		Iterable<com.zn.nursing.entity.NursingPricingConfig> allConfigs = nursingPricingConfigRepository.findAll();
		int updatedCount = 0;
		for (com.zn.nursing.entity.NursingPricingConfig config : allConfigs) {
			nursingPricingConfigRepository.save(config);
			updatedCount++;
		}
		return ResponseEntity.ok("Recalculated totalPrice for " + updatedCount + " nursing pricing configs.");
	} catch (Exception e) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to recalculate nursing pricing configs: " + e.getMessage());
	}
}

@PostMapping("/api/admin/pricing-config/recalculate-all/polymers")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<?> recalculateAllPolymersPricingConfigs() {
	try {
		Iterable<com.zn.polymers.entity.PolymersPricingConfig> allConfigs = polymersPricingConfigRepository.findAll();
		int updatedCount = 0;
		for (com.zn.polymers.entity.PolymersPricingConfig config : allConfigs) {
			polymersPricingConfigRepository.save(config);
			updatedCount++;
		}
		return ResponseEntity.ok("Recalculated totalPrice for " + updatedCount + " polymers pricing configs.");
	} catch (Exception e) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to recalculate polymers pricing configs: " + e.getMessage());
	}
}
	// Get all accommodation combos for each vertical (admin)
	@GetMapping("/api/admin/accommodation/optics")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> getAllOpticsAccommodations() {
		try {
			return ResponseEntity.ok(opticsAccommodationRepo.findAll());
		} catch (Exception e) {
			throw new DataProcessingException("Failed to retrieve optics accommodations: " + e.getMessage(), e);
		}
	}

	@GetMapping("/api/admin/accommodation/renewable")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> getAllRenewableAccommodations() {
		try {
			return ResponseEntity.ok(renewableAccommodationRepo.findAll());
		} catch (Exception e) {
			throw new DataProcessingException("Failed to retrieve renewable accommodations: " + e.getMessage(), e);
		}
	}

	@GetMapping("/api/admin/accommodation/nursing")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> getAllNursingAccommodations() {
		try {
			return ResponseEntity.ok(nursingAccommodationRepo.findAll());
		} catch (Exception e) {
			throw new DataProcessingException("Failed to retrieve nursing accommodations: " + e.getMessage(), e);
		}
	}

	@GetMapping("/api/admin/accommodation/polymers")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> getAllPolymersAccommodations() {
		try {
			return ResponseEntity.ok(polymersAccommodationRepo.findAll());
		} catch (Exception e) {
			throw new DataProcessingException("Failed to retrieve polymers accommodations: " + e.getMessage(), e);
		}
	}
		// Edit interested-in option for each vertical (admin)
	@PostMapping("/api/admin/interested-in/edit/optics/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> editOpticsInterestedInOption(@PathVariable Long id, @RequestBody InterestedInOptionDTO dto) {
		try {
			if (dto == null || dto.getInterestedInOption() == null || dto.getInterestedInOption().trim().isEmpty()) {
				return ResponseEntity.badRequest().body("Invalid interested-in option");
			}
			adminService.editOpticsInterestedInOption(id, dto.getInterestedInOption().trim());
			return ResponseEntity.ok("Optics interested-in option updated successfully.");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update optics interested-in option: " + e.getMessage());
		}
	}
	
	// Get all discount records


	@PostMapping("/api/admin/interested-in/edit/renewable/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> editRenewableInterestedInOption(@PathVariable Long id, @RequestBody InterestedInOptionDTO dto) {
		try {
			if (dto == null || dto.getInterestedInOption() == null || dto.getInterestedInOption().trim().isEmpty()) {
				return ResponseEntity.badRequest().body("Invalid interested-in option");
			}
			adminService.editRenewableInterestedInOption(id, dto.getInterestedInOption().trim());
			return ResponseEntity.ok("Renewable interested-in option updated successfully.");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update renewable interested-in option: " + e.getMessage());
		}
	}

	@PostMapping("/api/admin/interested-in/edit/nursing/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> editNursingInterestedInOption(@PathVariable Long id, @RequestBody InterestedInOptionDTO dto) {
		try {
			if (dto == null || dto.getInterestedInOption() == null || dto.getInterestedInOption().trim().isEmpty()) {
				return ResponseEntity.badRequest().body("Invalid interested-in option");
			}
			adminService.editNursingInterestedInOption(id, dto.getInterestedInOption().trim());
			return ResponseEntity.ok("Nursing interested-in option updated successfully.");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update nursing interested-in option: " + e.getMessage());
		}
	}

	@PostMapping("/api/admin/interested-in/edit/polymers/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> editPolymersInterestedInOption(@PathVariable Long id, @RequestBody InterestedInOptionDTO dto) {
		try {
			if (dto == null || dto.getInterestedInOption() == null || dto.getInterestedInOption().trim().isEmpty()) {
				return ResponseEntity.badRequest().body("Invalid interested-in option");
			}
			adminService.editPolymersInterestedInOption(id, dto.getInterestedInOption().trim());
			return ResponseEntity.ok("Polymers interested-in option updated successfully.");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to update polymers interested-in option: " + e.getMessage());
		}
	}

	// Delete interested-in option for each vertical (admin)
	@PostMapping("/api/admin/interested-in/delete/optics/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> deleteOpticsInterestedInOption(@PathVariable Long id) {
		try {
			adminService.deleteOpticsInterestedInOption(id);
			return ResponseEntity.ok("Optics interested-in option deleted successfully.");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete optics interested-in option: " + e.getMessage());
		}
	}

	@PostMapping("/api/admin/interested-in/delete/renewable/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> deleteRenewableInterestedInOption(@PathVariable Long id) {
		try {
			adminService.deleteRenewableInterestedInOption(id);
			return ResponseEntity.ok("Renewable interested-in option deleted successfully.");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete renewable interested-in option: " + e.getMessage());
		}
	}

	@PostMapping("/api/admin/interested-in/delete/nursing/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> deleteNursingInterestedInOption(@PathVariable Long id) {
		try {
			adminService.deleteNursingInterestedInOption(id);
			return ResponseEntity.ok("Nursing interested-in option deleted successfully.");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete nursing interested-in option: " + e.getMessage());
		}
	}

	@PostMapping("/api/admin/interested-in/delete/polymers/{id}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> deletePolymersInterestedInOption(@PathVariable Long id) {
		try {
			adminService.deletePolymersInterestedInOption(id);
			return ResponseEntity.ok("Polymers interested-in option deleted successfully.");
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete polymers interested-in option: " + e.getMessage());
		}
	}
	
	// --- Admin Session Option Edit/Delete (POST for compatibility) ---
	@PostMapping("/api/admin/sessions/optics/{id}/edit")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> editOpticsSession(@PathVariable Long id, @RequestBody SessionOptionDTO dto) {
		try {
			if (dto == null || dto.getSessionOption() == null || dto.getSessionOption().trim().isEmpty()) {
				throw new IllegalArgumentException("Session option is required and cannot be empty");
			}
			adminService.editOpticsSession(id, dto.getSessionOption().trim());
			return ResponseEntity.ok("Session option updated successfully (optics).");
		} catch (Exception e) {
			throw new DataProcessingException("Failed to update session option: " + e.getMessage(), e);
		}
	}

	@PostMapping("/api/admin/sessions/optics/{id}/delete")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> deleteOpticsSession(@PathVariable Long id) {
		try {
			adminService.deleteOpticsSession(id);
			return ResponseEntity.ok("Session option deleted successfully (optics).");
		} catch (Exception e) {
			throw new DataProcessingException("Failed to delete session option: " + e.getMessage(), e);
		}
	}
	

	   @PostMapping("/api/admin/sessions/renewable/{id}/edit")
	   @PreAuthorize("hasRole('ADMIN')")
	   public ResponseEntity<?> editRenewableSession(@PathVariable Long id, @RequestBody SessionOptionDTO dto) {
			   try {
					   if (dto == null || dto.getSessionOption() == null || dto.getSessionOption().trim().isEmpty()) {
							   throw new IllegalArgumentException("Session option is required and cannot be empty");
					   }
					   adminService.editRenewableSession(id, dto.getSessionOption().trim());
					   return ResponseEntity.ok("Session option updated successfully (renewable).");
			   } catch (Exception e) {
					   throw new DataProcessingException("Failed to update session option: " + e.getMessage(), e);
			   }
	   }

	   @PostMapping("/api/admin/sessions/polymers/{id}/edit")
	   @PreAuthorize("hasRole('ADMIN')")
	   public ResponseEntity<?> editPolymersSession(@PathVariable Long id, @RequestBody SessionOptionDTO dto) {
			   try {
					   if (dto == null || dto.getSessionOption() == null || dto.getSessionOption().trim().isEmpty()) {
							   throw new IllegalArgumentException("Session option is required and cannot be empty");
					   }
					   adminService.editPolymersSession(id, dto.getSessionOption().trim());
					   return ResponseEntity.ok("Session option updated successfully (polymers).");
			   } catch (Exception e) {
					   throw new DataProcessingException("Failed to update session option: " + e.getMessage(), e);
			   }
	   }
	// Get all discount records for Optics
	@GetMapping("/api/admin/discounts/optics")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> getAllOpticsDiscounts() {
		try {
			return ResponseEntity.ok(adminService.getAllOpticsDiscounts());
		} catch (Exception e) {
			throw new DataProcessingException("Failed to retrieve optics discounts: " + e.getMessage(), e);
		}
	}

	// Get all discount records for Renewable
	@GetMapping("/api/admin/discounts/renewable")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> getAllRenewableDiscounts() {
		try {
			return ResponseEntity.ok(adminService.getAllRenewableDiscounts());
		} catch (Exception e) {
			throw new DataProcessingException("Failed to retrieve renewable discounts: " + e.getMessage(), e);
		}
	}

	// Get all discount records for Nursing
	@GetMapping("/api/admin/discounts/nursing")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> getAllNursingDiscounts() {
		try {
			return ResponseEntity.ok(adminService.getAllNursingDiscounts());
		} catch (Exception e) {
			throw new DataProcessingException("Failed to retrieve nursing discounts: " + e.getMessage(), e);
		}
	}

	// Get all discount records for Polymers
	@GetMapping("/api/admin/discounts/polymers")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> getAllPolymersDiscounts() {
		try {
			return ResponseEntity.ok(adminService.getAllPolymersDiscounts());
		} catch (Exception e) {
			throw new DataProcessingException("Failed to retrieve polymers discounts: " + e.getMessage(), e);
		}
	}

	@PostMapping("/api/admin/sessions/renewable/{id}/delete")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> deleteRenewableSession(@PathVariable Long id) {
		try {
			adminService.deleteRenewableSession(id);
			return ResponseEntity.ok("Session option deleted successfully (renewable).");
		} catch (Exception e) {
			throw new DataProcessingException("Failed to delete session option: " + e.getMessage(), e);
		}
	}

	@PostMapping("/api/admin/sessions/polymers/{id}/delete")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> deletePolymersSession(@PathVariable Long id) {
		try {
			adminService.deletePolymersSession(id);
			return ResponseEntity.ok("Session option deleted successfully (polymers).");
		} catch (Exception e) {
			throw new DataProcessingException("Failed to delete session option: " + e.getMessage(), e);
		}
	}

	   @PostMapping("/api/admin/sessions/nursing/{id}/edit")
	   @PreAuthorize("hasRole('ADMIN')")
	   public ResponseEntity<?> editNursingSession(@PathVariable Long id, @RequestBody SessionOptionDTO dto) {
			   try {
					   if (dto == null || dto.getSessionOption() == null || dto.getSessionOption().trim().isEmpty()) {
							   throw new IllegalArgumentException("Session option is required and cannot be empty");
					   }
					   adminService.editNursingSession(id, dto.getSessionOption().trim());
					   return ResponseEntity.ok("Session option updated successfully (nursing).");
			   } catch (Exception e) {
					   throw new DataProcessingException("Failed to update session option: " + e.getMessage(), e);
			   }
	   }

	@PostMapping("/api/admin/sessions/nursing/{id}/delete")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> deleteNursingSession(@PathVariable Long id) {
		try {
			adminService.deleteNursingSession(id);
			return ResponseEntity.ok("Session option deleted successfully (nursing).");
		} catch (Exception e) {
			throw new DataProcessingException("Failed to delete session option: " + e.getMessage(), e);
		}
	}


}
