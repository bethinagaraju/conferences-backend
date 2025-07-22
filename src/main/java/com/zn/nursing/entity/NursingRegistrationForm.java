package com.zn.nursing.entity;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.zn.payment.nursing.entity.NursingPaymentRecord;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import lombok.Data;

@Entity
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class NursingRegistrationForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String phone;
    private String email;
    private String instituteOrUniversity;
    private String country;

    @ManyToOne
    @JoinColumn(name = "pricing_config_id")
    private NursingPricingConfig pricingConfig;

    @Column(nullable = false)
    private BigDecimal amountPaid; // snapshot of totalPrice at registration time
    
    // One-to-One relationship with PaymentRecord
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "payment_record_id", referencedColumnName = "id")
    @JsonManagedReference
    private NursingPaymentRecord paymentRecord;
}
