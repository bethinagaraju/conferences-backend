package com.zn.renewable.service;



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
import org.springframework.web.multipart.MultipartFile;

import com.zn.config.HostingerFtpClient;
import com.zn.dto.AbstractSubmissionRequestDTO;
import com.zn.renewable.entity.RenewableForm;
import com.zn.renewable.repository.IRenewableFormSubmissionRepo;
import com.zn.renewable.repository.IRenewableIntrestedInOptionsRepo;
import com.zn.renewable.repository.IRenewableSessionOption;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class  RenewableFormSubmissionService {

    @Value("${hostinger.public.url}")
    private String hostingerPublicUrl;

    @Autowired
    private HostingerFtpClient hostingerFtpClient;

    @Autowired
    private IRenewableFormSubmissionRepo formSubmissionRepo;

    @Autowired
    private IRenewableIntrestedInOptionsRepo interestedInRepo;

    @Autowired
    private IRenewableSessionOption sessionOptionsRepo;

    public RenewableForm saveSubmission(AbstractSubmissionRequestDTO request) {
        RenewableForm formSubmission = new RenewableForm();

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

        // Upload file to Hostinger FTP
        MultipartFile file = request.getAbstractFile();
        if (file != null && !file.isEmpty()) {
            String fileUrl = uploadFileToHostinger(file, request.getEmail());
            formSubmission.setAbstractFilePath(fileUrl);
        }

        return formSubmissionRepo.save(formSubmission);
    }

    public String uploadFileToHostinger(MultipartFile file, String userId) {
        try {
            if (file.isEmpty()) {
                return "Upload failed: File is empty";
            }

            String fileName = file.getOriginalFilename();
            // Store in the same location as speakers images
            String remoteFileName = "renewable_" + userId.replace("@", "_").replace(".", "_") + "_" + fileName;

            hostingerFtpClient.uploadFile(remoteFileName, file.getInputStream());

            // Construct the public URL for the uploaded file
            return hostingerPublicUrl + "/" + remoteFileName;
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

    public List<RenewableForm> getAllFormSubmissions() {
        try {
            return formSubmissionRepo.findAll();
        } catch (Exception e) {
            log.error("Error retrieving all form submissions: ", e);
            return null;
        }
    }
}
