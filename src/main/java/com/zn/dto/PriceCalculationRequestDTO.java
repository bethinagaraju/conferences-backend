package com.zn.dto;

import lombok.Data;

@Data
public class PriceCalculationRequestDTO {
    private String registrationType;      // e.g., REGISTRATION_ONLY or REGISTRATION_AND_ACCOMMODATION

    private String presentationType;    // e.g., SPEAKER, STUDENT, etc.

    private int numberOfNights;                     // e.g., 2, 3, 4

    private int numberOfGuests;
    
}
