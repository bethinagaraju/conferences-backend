package com.zn.dto;


import lombok.Data;

@Data
public class RegistrationRequestDTO {
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

