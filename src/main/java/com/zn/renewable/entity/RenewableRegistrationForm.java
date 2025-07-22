package com.zn.renewable.entity;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.zn.payment.renewable.entity.RenewablePaymentRecord;

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
public class RenewableRegistrationForm {

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
    private RenewablePricingConfig pricingConfig;

    @Column(nullable = false)
    private BigDecimal amountPaid; // snapshot of totalPrice at registration time
    
    // One-to-One relationship with RenewablePaymentRecord
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "payment_record_id", referencedColumnName = "id")
    @JsonManagedReference
    private RenewablePaymentRecord renewablePaymentRecord;
}
