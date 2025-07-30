package com.zn.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zn.dto.ContactFormDto;
import com.zn.nursing.service.NursingEmailService;
import com.zn.optics.service.OpticsEmailService;
import com.zn.renewable.service.RenewableEmailService;
import com.zn.polymers.service.PolymersEmailService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/contact")
public class ContactController {


    @Autowired
    private OpticsEmailService opticsEmailService;

    @Autowired
    private NursingEmailService nursingEmailService;

    @Autowired
    private RenewableEmailService renewableEmailService;

    @Autowired
    private PolymersEmailService polymersEmailService;

    @PostMapping
    public String sendContactMessage(@RequestBody ContactFormDto dto, HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin == null) {
            origin = request.getHeader("Referer");
        }
        if (origin != null) {
            if (origin.contains("globallopmeet.com")) {
                opticsEmailService.sendContactMessage(dto.getName(), dto.getEmail(), dto.getSubject(), dto.getMessage());
            } else if (origin.contains("nursingmeet2026.com")) {
                nursingEmailService.sendContactMessage(dto.getName(), dto.getEmail(), dto.getSubject(), dto.getMessage());
            } else if (origin.contains("globalrenewablemeet.com")) {
                renewableEmailService.sendContactMessage(dto.getName(), dto.getEmail(), dto.getSubject(), dto.getMessage());

            } else if (origin.contains("polyscienceconference.com")){
                polymersEmailService.sendContactMessage(dto.getName(), dto.getEmail(), dto.getSubject(), dto.getMessage());
                
            } 
            else {
                throw new IllegalArgumentException("Unknown frontend domain: " + origin);
            }
        } else {
            throw new IllegalArgumentException("Origin or Referer header is missing");
        }
        return "Message sent successfully";
    }
}
