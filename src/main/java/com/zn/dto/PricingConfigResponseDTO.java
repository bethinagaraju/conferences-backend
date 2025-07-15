package com.zn.dto;

import java.math.BigDecimal;

import com.zn.Ientity.IAccommodation;
import com.zn.Ientity.IPresentationType;

import lombok.Data;

@Data
public class PricingConfigResponseDTO {
	  private Long id;
	    private BigDecimal totalPrice;
	    private double processingFeePercent;
	    private IPresentationType presentationType;
	    private IAccommodation accommodationOption;
}
