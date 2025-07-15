package com.zn.payment.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {
    private String productName;
    private String description;
    private String orderReference;
    private Long unitAmount; // in cents (EURO only - e.g., 4500 for €45.00)
    private Long quantity;
    private String currency; // Optional: defaults to "eur" if not provided. Only "eur" is supported.
    private String successUrl;
    private String cancelUrl;
    private Long pricingConfigId; // Required for price validation
    
    // Customer and registration details (matching RegistrationRequestDTO naming)
    private String name;
    private String phone;
    private String email;
    private String instituteOrUniversity;
    private String country;
    private String registrationType;
    private String presentationType;
    private boolean accompanyingPerson;
    private int extraNights;
    private int accommodationNights;
    private int accommodationGuests;
}