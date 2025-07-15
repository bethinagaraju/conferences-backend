package com.zn.payment.controller;


import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zn.payment.dto.CreateDiscountSessionRequest;
import com.zn.payment.optics.service.OpticsDiscountsService;
import com.zn.payment.nursing.service.NursingDiscountsService;
import com.zn.payment.renewable.service.RenewableDiscountsService;

import jakarta.servlet.http.HttpServletRequest;



@RestController
@RequestMapping("/api/discounts")
public class DiscountsController {
    @Autowired
    private OpticsDiscountsService opticsDiscountsService;
    
    @Autowired
    private NursingDiscountsService nursingDiscountsService;
    
    @Autowired
    private RenewableDiscountsService renewableDiscountsService;

    // create stripe session
    @PostMapping("/create-session")
    public ResponseEntity<?> createSession(@RequestBody CreateDiscountSessionRequest request, HttpServletRequest httpRequest) {
        String origin = httpRequest.getHeader("Origin");
        String referer = httpRequest.getHeader("Referer");
        
        // Route based on domain
        if ((origin != null && origin.contains("globallopmeet.com")) || 
            (referer != null && referer.contains("globallopmeet.com"))) {
            // Route to Optics service
            Object result = opticsDiscountsService.createSession(request);
            return ResponseEntity.ok(result);
        } else if ((origin != null && origin.contains("nursingmeet2026.com")) || 
                   (referer != null && referer.contains("nursingmeet2026.com"))) {
            // Route to Nursing service
            Object result = nursingDiscountsService.createSession(request);
            return ResponseEntity.ok(result);
        } else if ((origin != null && origin.contains("globalrenewablemeet.com")) || 
                   (referer != null && referer.contains("globalrenewablemeet.com"))) {
            // Route to Renewable service
            Object result = renewableDiscountsService.createSession(request);
            return ResponseEntity.ok(result);
        } else {
            // Default to nursing service for backward compatibility
            Object result = nursingDiscountsService.createSession(request);
            return ResponseEntity.ok(result);
        }
    }
    
    // handle stripe webhook
    @PostMapping("/webhook")
    public ResponseEntity<?> handleStripeWebhook(HttpServletRequest request) throws IOException {
        String origin = request.getHeader("Origin");
        String referer = request.getHeader("Referer");
        
        // Route based on domain
        if ((origin != null && origin.contains("globallopmeet.com")) || 
            (referer != null && referer.contains("globallopmeet.com"))) {
            // Route to Optics service
            Object result = opticsDiscountsService.handleStripeWebhook(request);
            return ResponseEntity.ok(result);
        } else if ((origin != null && origin.contains("nursingmeet2026.com")) || 
                   (referer != null && referer.contains("nursingmeet2026.com"))) {
            // Route to Nursing service
            Object result = nursingDiscountsService.handleStripeWebhook(request);
            return ResponseEntity.ok(result);
        } else if ((origin != null && origin.contains("globalrenewablemeet.com")) || 
                   (referer != null && referer.contains("globalrenewablemeet.com"))) {
            // Route to Renewable service
            Object result = renewableDiscountsService.handleStripeWebhook(request);
            return ResponseEntity.ok(result);
        } else {
            // Default fallback - try nursing first, then optics, then renewable
            try {
                Object result = nursingDiscountsService.handleStripeWebhook(request);
                return ResponseEntity.ok(result);
            } catch (Exception e) {
                try {
                    Object result = opticsDiscountsService.handleStripeWebhook(request);
                    return ResponseEntity.ok(result);
                } catch (Exception e2) {
                    Object result = renewableDiscountsService.handleStripeWebhook(request);
                    return ResponseEntity.ok(result);
                }
            }
        }
    }



}