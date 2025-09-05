package com.zn.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zn.dto.AbstractSubmissionRequestDTO;
import com.zn.nursing.service.NursingFormSubmissionService;
import com.zn.optics.service.OpticsFormSubmissionService;
import com.zn.renewable.service.RenewableFormSubmissionService;
import com.zn.polymers.service.PolymersFormSubmissionService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/form-submission")
public class FormSubmission {
	
	
	

	@Autowired
	private OpticsFormSubmissionService opticsFormSubmissionService;

	@Autowired
	private NursingFormSubmissionService nursingFormSubmissionService;

	@Autowired
	private RenewableFormSubmissionService renewableFormSubmissionService;
	
	@Autowired
	private PolymersFormSubmissionService polymersFormSubmissionService;
	
	// handle form submission logic here
	@PostMapping("/submit")
	public ResponseEntity<String> submitForm(@ModelAttribute AbstractSubmissionRequestDTO request, HttpServletRequest httpRequest) {
		String origin = httpRequest.getHeader("Origin");
		if (origin == null) {
			origin = httpRequest.getHeader("Referer");
		}
		if (request.getAbstractFile() == null || request.getAbstractFile().isEmpty()) {
			return ResponseEntity.badRequest().body("Abstract file is required.");
		}
		if (origin != null) {
			if (origin.contains("globallopmeet.com")) {
				opticsFormSubmissionService.saveSubmission(request);
			} else if (origin.contains("nursingmeet2026.com")) {
				nursingFormSubmissionService.saveSubmission(request);
			} else if (origin.contains("globalrenewablemeet.com")) {
				renewableFormSubmissionService.saveSubmission(request);
			}else if (origin.contains("polyscienceconference.com")) {
				polymersFormSubmissionService.saveSubmission(request);
			} 
			else {
				return ResponseEntity.badRequest().body("Unknown frontend domain: " + origin);
			}
		} else {
			return ResponseEntity.badRequest().body("Origin or Referer header is missing");
		}
		return ResponseEntity.ok("Form submitted successfully.");
	}

	// get all interested in options find all interested in options
	@GetMapping("/get-interested-in-options")
	public ResponseEntity<?> getInterestedInOptions(HttpServletRequest httpRequest) {
		String origin = httpRequest.getHeader("Origin");
		if (origin == null) {
			origin = httpRequest.getHeader("Referer");
		}
		List<?> interestedInOptions;
		if (origin != null) {
			if (origin.contains("globallopmeet.com")) {
				interestedInOptions = opticsFormSubmissionService.getInterestedInOptions();
			} else if (origin.contains("nursingmeet2026.com")) {
				interestedInOptions = nursingFormSubmissionService.getInterestedInOptions();
			} else if (origin.contains("globalrenewablemeet.com")) {
				interestedInOptions = renewableFormSubmissionService.getInterestedInOptions();
			} else if (origin.contains("polyscienceconference.com")) {
				interestedInOptions = polymersFormSubmissionService.getInterestedInOptions();
			} else {
				return ResponseEntity.badRequest().body("Unknown frontend domain: " + origin);
			}
		} else {
			return ResponseEntity.badRequest().body("Origin or Referer header is missing");
		}
		return ResponseEntity.ok(interestedInOptions);    
	}
	// get all session options
	@GetMapping("/get-session-options")
	public ResponseEntity<?> getSessionOptions(HttpServletRequest httpRequest) {
		String origin = httpRequest.getHeader("Origin");
		if (origin == null) {
			origin = httpRequest.getHeader("Referer");
		}
		List<?> sessionOptions;
		if (origin != null) {
			if (origin.contains("globallopmeet.com")) {
				sessionOptions = opticsFormSubmissionService.getSessionOptions();
			} else if (origin.contains("nursingmeet2026.com")) {
				sessionOptions = nursingFormSubmissionService.getSessionOptions();
			} else if (origin.contains("globalrenewablemeet.com")) {
				sessionOptions = renewableFormSubmissionService.getSessionOptions();
			} else if (origin.contains("polyscienceconference.com")) {
				sessionOptions = polymersFormSubmissionService.getSessionOptions();
			} else {
				return ResponseEntity.badRequest().body("Unknown frontend domain: " + origin);
			}
		} else {
			return ResponseEntity.badRequest().body("Origin or Referer header is missing");
		}
		return ResponseEntity.ok(sessionOptions);
	}

	// Get all form submissions for renewable
	@GetMapping("/get-submissions")
	public ResponseEntity<?> getFormSubmissions(HttpServletRequest httpRequest) {
		String origin = httpRequest.getHeader("Origin");
		if (origin == null) {
			origin = httpRequest.getHeader("Referer");
		}
		if (origin != null && origin.contains("globalrenewablemeet.com")) {
			List<?> submissions = renewableFormSubmissionService.getAllFormSubmissions();
			return ResponseEntity.ok(submissions);
		} else {
			return ResponseEntity.badRequest().body("Access denied for this domain");
		}
	}

}
