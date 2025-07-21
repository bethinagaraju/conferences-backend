
package com.zn.adminservice;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.zn.adminentity.Admin;
import com.zn.adminrepo.IAdminRepo;
import com.zn.dto.AdminResponseDTO;
import com.zn.exception.AdminAuthenticationException;
import com.zn.exception.DataProcessingException;
import com.zn.nursing.repository.INursingAccommodationRepo;
import com.zn.nursing.repository.INursingIntrestedInOptionsRepo;
import com.zn.nursing.repository.INursingPricingConfigRepository;
import com.zn.nursing.repository.INursingRegistrationFormRepository;
import com.zn.nursing.repository.INursingSessionOption;
import com.zn.optics.repository.IOpricsRegistrationFormRepository;
import com.zn.optics.repository.IOpticsAccommodationRepo;
import com.zn.optics.repository.IOpticsIntrestedInOptionsRepo;
import com.zn.optics.repository.IOpticsPricingConfigRepository;
import com.zn.optics.repository.IOpticsSessionOption;
import com.zn.renewable.repository.IRenewableAccommodationRepo;
import com.zn.renewable.repository.IRenewableIntrestedInOptionsRepo;
import com.zn.renewable.repository.IRenewablePricingConfigRepository;
import com.zn.renewable.repository.IRenewableRegistrationFormRepository;
import com.zn.renewable.repository.IRenewableSessionOption;

@Service
public class AdminService {
	   @Value("${supabase.url}")
		private String SUPABASE_URL;

		@Value("${supabase.bucket}")
		private String BUCKET_NAME;

		@Value("${supabase.api.key}")
		private String SUPABASE_API_KEY;
	@Autowired
	 private IOpticsSessionOption opticsSessionOption;
	@Autowired 

	private IOpticsIntrestedInOptionsRepo opticsInterestedInOptionRepo;
	
	@Autowired 
	private IRenewableSessionOption renewableSessionOption;
	@Autowired 
	private IRenewableIntrestedInOptionsRepo renewableInterestedInOptionRepo;
	@Autowired private INursingSessionOption nursingSessionOption;
	@Autowired private INursingIntrestedInOptionsRepo nursingInterestedInOptionRepo;
	// Removed missing InterestedInOption repositories
	@Autowired private IOpticsPricingConfigRepository opticsPricingConfigRepository;
	@Autowired private IRenewablePricingConfigRepository renewablePricingConfigRepository;
	@Autowired private INursingPricingConfigRepository nursingPricingConfigRepository;
	@Autowired private IOpricsRegistrationFormRepository opticsRegistrationFormRepository;
	@Autowired private IRenewableRegistrationFormRepository renewableRegistrationFormRepository;
	@Autowired private INursingRegistrationFormRepository nursingRegistrationFormRepository;
	@Autowired private IOpticsAccommodationRepo opticsAccommodationRepo;
	@Autowired private IRenewableAccommodationRepo renewableAccommodationRepo;
	@Autowired private INursingAccommodationRepo nursingAccommodationRepo;
	@Autowired private IAdminRepo adminRepo;	
	  private final RestTemplate restTemplate = new RestTemplate();

	/**
	 * Register a new admin with encrypted password
	 * @param registrationDTO the admin registration data
	 * @return the created admin (without password)
	 * @throws DataProcessingException if registration fails
	 */
	public Admin registerAdmin(String mail, String password, String name, String role) {
		// Validate input
		if (mail == null || mail.trim().isEmpty()) {
			throw new IllegalArgumentException("Email is required");
		}
		if (password == null || password.trim().isEmpty()) {
			throw new IllegalArgumentException("Password is required");
		}
		if (name == null || name.trim().isEmpty()) {
			throw new IllegalArgumentException("Name is required");
		}
		if (role == null || role.trim().isEmpty()) {
			throw new IllegalArgumentException("Role is required");
		}
		// Check if admin already exists
		Admin existingAdmin = adminRepo.findByEmail(mail.trim());
		if (existingAdmin != null) {
			throw new DataProcessingException("Admin with this email already exists");
		}
		// Create new admin with encrypted password (replace with real encryption in production)
		Admin admin = new Admin();
		admin.setName(name.trim());
		admin.setEmail(mail.trim().toLowerCase());
		admin.setPassword(password); // Store plain password for now
		admin.setRole(role != null ? role.trim() : "ADMIN");
		// Save admin
		Admin savedAdmin = adminRepo.save(admin);
		// Return response without password
		return savedAdmin;
	}

	/**
	 * Authenticate admin login with encrypted password verification
	 * @param adminCredentials the login credentials
	 * @return authenticated admin (without password)
	 * @throws AdminAuthenticationException if authentication fails
	 */
	public Admin loginAdmin(Admin adminCredentials) {
		if (adminCredentials == null || adminCredentials.getEmail() == null || adminCredentials.getPassword() == null) {
			throw new AdminAuthenticationException("Email and password are required");
		}
		// Find admin by email
		Admin admin = adminRepo.findByEmail(adminCredentials.getEmail().trim().toLowerCase());
		if (admin == null) {
			throw new AdminAuthenticationException("Invalid email or password");
		}
		// Compare plain passwords (replace with encryption in production)
		if (!adminCredentials.getPassword().equals(admin.getPassword())) {
			throw new AdminAuthenticationException("Invalid email or password");
		}
		return admin;
	}

	/**
	 * Convert Admin entity to AdminResponseDTO (without password)
	 * @param admin the admin entity
	 * @return AdminResponseDTO without sensitive information
	 */
	public AdminResponseDTO convertToAdminResponseDTO(Admin admin) {
		if (admin == null) {
			return null;
		}
		
		return new AdminResponseDTO(
			admin.getId() != null ? admin.getId().longValue() : 0L,
			admin.getEmail(),			admin.getName(),
			admin.getRole()
		);
	}


	// Optics vertical
	public String insertOpticsSession(String sessionOption) {
		if (sessionOption == null || sessionOption.isEmpty()) {
			return "Session option is required.";
		}
		com.zn.optics.entity.OpticsSessionOption option = new com.zn.optics.entity.OpticsSessionOption(sessionOption);
		opticsSessionOption.save(option);
		return "Session option inserted successfully (optics).";
	}

	// Renewable vertical
	public String insertRenewableSession(String sessionOption) {
		if (sessionOption == null || sessionOption.isEmpty()) {
			return "Session option is required.";
		}
		com.zn.renewable.entity.RenewableSessionOption option = new com.zn.renewable.entity.RenewableSessionOption(sessionOption);
		renewableSessionOption.save(option);
		return "Session option inserted successfully (renewable).";
	}

	// Nursing vertical
	public String insertNursingSession(String sessionOption) {
		if (sessionOption == null || sessionOption.isEmpty()) {
			return "Session option is required.";
		}
		com.zn.nursing.entity.NursingSessionOption option = new com.zn.nursing.entity.NursingSessionOption(sessionOption);
		nursingSessionOption.save(option);
		return "Session option inserted successfully (nursing).";
	}


	// Optics vertical
	public String insertOpticsInterestedInOption(String interestedInOption) {
		if (interestedInOption == null || interestedInOption.isEmpty()) {
			return "Interested In option is required.";
		}
		com.zn.optics.entity.OpticsInterestedInOption option = new com.zn.optics.entity.OpticsInterestedInOption(interestedInOption);
		opticsInterestedInOptionRepo.save(option);
		return "Interested In option inserted successfully (optics).";
	}

	// Renewable vertical
	public String insertRenewableInterestedInOption(String interestedInOption) {
		if (interestedInOption == null || interestedInOption.isEmpty()) {
			return "Interested In option is required.";
		}
		com.zn.renewable.entity.RenewableInterestedInOption option = new com.zn.renewable.entity.RenewableInterestedInOption(interestedInOption);
		renewableInterestedInOptionRepo.save(option);
		return "Interested In option inserted successfully (renewable).";
	}

	// Nursing vertical
	public String insertNursingInterestedInOption(String interestedInOption) {
		if (interestedInOption == null || interestedInOption.isEmpty()) {
			return "Interested In option is required.";
		}
		com.zn.nursing.entity.NursingInterestedInOption option = new com.zn.nursing.entity.NursingInterestedInOption(interestedInOption);
		nursingInterestedInOptionRepo.save(option);
		return "Interested In option inserted successfully (nursing).";
	}

	// Optics vertical
	public List<com.zn.optics.entity.OpticsRegistrationForm> getAllOpticsFormSubmissions() {
		return opticsRegistrationFormRepository.findAll();
	}

	// Renewable vertical
	public List<com.zn.renewable.entity.RenewableRegistrationForm> getAllRenewableFormSubmissions() {
		return renewableRegistrationFormRepository.findAll();
	}

	// Nursing vertical
	public List<com.zn.nursing.entity.NursingRegistrationForm> getAllNursingFormSubmissions() {
		return nursingRegistrationFormRepository.findAll();
	}


	// Optics vertical
	public String insertOpticsAccommodation(com.zn.optics.entity.OpticsAccommodation accommodation) {
		if (accommodation == null) {
			return "Accommodation is required.";
		}
		opticsAccommodationRepo.save(accommodation);
		return "Accommodation inserted successfully (optics).";
	}

	// Renewable vertical
	public String insertRenewableAccommodation(com.zn.renewable.entity.RenewableAccommodation accommodation) {
		if (accommodation == null) {
			return "Accommodation is required.";
		}
		renewableAccommodationRepo.save(accommodation);
		return "Accommodation inserted successfully (renewable).";
	}

	// Nursing vertical
	public String insertNursingAccommodation(com.zn.nursing.entity.NursingAccommodation accommodation) {
		if (accommodation == null) {
			return "Accommodation is required.";
		}
		nursingAccommodationRepo.save(accommodation);
		return "Accommodation inserted successfully (nursing).";
	}


// public String insertPresentationType(PresentationType presentationType) {
//     if (presentationType == null || presentationType.getType() == null) {
//         return "Presentation type is required.";
//     }
//     // presentationTypeRepo.save(presentationType);
//     return "Presentation type inserted successfully.";        
// }


	// Optics vertical
	public com.zn.optics.entity.OpticsPricingConfig insertOpticsPricingConfig(com.zn.optics.entity.OpticsPricingConfig config) {
		if (config == null) {
			throw new IllegalArgumentException("PricingConfig cannot be null.");
		}
		config.calculateTotalPrice();
		return opticsPricingConfigRepository.save(config);
	}

	// Renewable vertical
	public com.zn.renewable.entity.RenewablePricingConfig insertRenewablePricingConfig(com.zn.renewable.entity.RenewablePricingConfig config) {
		if (config == null) {
			throw new IllegalArgumentException("PricingConfig cannot be null.");
		}
		config.calculateTotalPrice();
		return renewablePricingConfigRepository.save(config);
	}

	// Nursing vertical
	public com.zn.nursing.entity.NursingPricingConfig insertNursingPricingConfig(com.zn.nursing.entity.NursingPricingConfig config) {
		if (config == null) {
			throw new IllegalArgumentException("PricingConfig cannot be null.");
		}
		config.calculateTotalPrice();
		return nursingPricingConfigRepository.save(config);
	}
		
		
		
		
		
		
		/* PresentationType presentationType = presentationTypeRepo.findById(presentationTypeId)
			.orElseThrow(() -> new IllegalArgumentException("Invalid presentation type ID"));
		
		if (presentationType.getPrice() == null) {
			throw new IllegalStateException("Presentation type price cannot be null.");
		}
		
		config.setPresentationType(presentationType);
		
		if (accommodationId != null) {
			Accommodation accommodation = accommodationRepo.findById(accommodationId)
				.orElseThrow(() -> new IllegalArgumentException("Invalid accommodation ID"));
		
			if (accommodation.getPrice() == null) {
				throw new IllegalStateException("Accommodation price cannot be null.");
			}
		
			config.setAccommodationOption(accommodation);
		}
		
		return pricingConfigRepo.save(config); // ✅ returning saved entity
			}
		}*/


	// Optics vertical
	public com.zn.optics.entity.OpticsPricingConfig getOpticsPricingConfigById(Long id) {
		if (id == null) {
			return null;
		}
		return opticsPricingConfigRepository.findById(id).orElse(null);
	}

	// Renewable vertical
	public com.zn.renewable.entity.RenewablePricingConfig getRenewablePricingConfigById(Long id) {
		if (id == null) {
			return null;
		}
		return renewablePricingConfigRepository.findById(id).orElse(null);
	}

	// Nursing vertical
	public com.zn.nursing.entity.NursingPricingConfig getNursingPricingConfigById(Long id) {
		if (id == null) {
			return null;
		}
		return nursingPricingConfigRepository.findById(id).orElse(null);
	}



	// Optics vertical
	public List<com.zn.optics.entity.OpticsRegistrationForm> getAllOpticsRegistrationForms() {
		return opticsRegistrationFormRepository.findAll();
	}


	// Renewable vertical
	public List<com.zn.renewable.entity.RenewableRegistrationForm> getAllRenewableRegistrationForms() {
		return renewableRegistrationFormRepository.findAll();
	}

	// Nursing vertical
	public List<com.zn.nursing.entity.NursingRegistrationForm> getAllNursingRegistrationForms() {
		return nursingRegistrationFormRepository.findAll();
	}


// public List<Form> getAllAbstractSubmissions() {
//     try {
//         return fromSubmissionRepo.findAll();
//     } catch (Exception e) {
//         e.printStackTrace();
//         return null; // or handle the error appropriately
//     }
// }
	
}
