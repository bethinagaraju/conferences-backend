package com.zn.optics.service;



import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.zn.dto.AbstractSubmissionRequestDTO;
import com.zn.optics.entity.OpticsForm;
import com.zn.optics.repository.IOpticsFromSubmissionRepo;
import com.zn.optics.repository.IOpticsIntrestedInOptionsRepo;
import com.zn.optics.repository.IOpticsSessionOption;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class OpticsFormSubmissionService {

    @Value("${supabase.url}")
    private String SUPABASE_URL;

    @Value("${supabase.bucket}")
    private String BUCKET_NAME;

    @Value("${supabase.api.key}")
    private String SUPABASE_API_KEY;

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private IOpticsFromSubmissionRepo formSubmissionRepo;

    @Autowired
    private IOpticsIntrestedInOptionsRepo interestedInRepo;

    @Autowired
    private IOpticsSessionOption sessionOptionsRepo;

    public OpticsForm saveSubmission(AbstractSubmissionRequestDTO request) {
        OpticsForm formSubmission = new OpticsForm();

        // Set basic fields
        formSubmission.setTitlePrefix(request.getTitlePrefix());
        formSubmission.setName(request.getName());
        formSubmission.setEmail(request.getEmail());
        formSubmission.setPhone(request.getPhone());
        formSubmission.setOrganizationName(request.getOrganizationName());
        formSubmission.setCountry(request.getCountry());

        // Fetch and set InterestedIn
        Optional.ofNullable(request.getInterestedInId())
            .flatMap(interestedInRepo::findById)
            .ifPresent(formSubmission::setInterestedIn);

        // Fetch and set SessionOption
        Optional.ofNullable(request.getSessionId())
            .flatMap(sessionOptionsRepo::findById)
            .ifPresent(formSubmission::setSession);

        // Upload file to Supabase
        MultipartFile file = request.getAbstractFile();
        if (file != null && !file.isEmpty()) {
            String fileUrl = uploadFileToSupabase(file, request.getEmail());
            formSubmission.setAbstractFilePath(fileUrl);
        }

        return formSubmissionRepo.save(formSubmission);
    }

    public String uploadFileToSupabase(MultipartFile file, String userId) {
        try {
            if (file.isEmpty()) {
                return "Upload failed: File is empty";
            }

            String fileName = file.getOriginalFilename();
            // Store in a folder named after the service: 'optics/{userId}/{fileName}'
            String pathInBucket = "optics/" + userId + "/" + fileName;

            String uploadUrl = SUPABASE_URL + "/storage/v1/object/" + BUCKET_NAME + "/" + pathInBucket;

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + SUPABASE_API_KEY);
            headers.set("apikey", SUPABASE_API_KEY);
            headers.setContentType(MediaType.parseMediaType(file.getContentType()));

            HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);

            ResponseEntity<String> response = restTemplate.exchange(uploadUrl, HttpMethod.PUT, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return SUPABASE_URL + "/storage/v1/object/public/" + BUCKET_NAME + "/" + pathInBucket;
            } else {
                return "Upload failed: " + response.getStatusCode();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return "Upload error: " + e.getMessage();
        }
    }

    public List<?> getInterestedInOptions() {
        try {
            log.info("Retrieving interested in options from repository");
            return interestedInRepo.findAll();
        } catch (Exception e) {
            log.error("Error retrieving interested in options: ", e);
            e.printStackTrace();
            return null; // or handle the error appropriately
        }
        
    }

    public List<?> getSessionOptions() {
        
        
        try {
            return sessionOptionsRepo.findAll();
        } catch (Exception e) {
            e.printStackTrace();
            return null; // or handle the error appropriately
        }
        
    }
    //Get all form submissions 
    
    
    
    
}
