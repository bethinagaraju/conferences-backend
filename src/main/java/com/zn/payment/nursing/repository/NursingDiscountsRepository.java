package com.zn.payment.nursing.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.zn.payment.nursing.entity.NursingDiscounts;
public interface NursingDiscountsRepository extends JpaRepository<NursingDiscounts, Long> {
    // Custom query methods can be defined here if needed
	NursingDiscounts findBySessionId(String sessionId);
	Optional<NursingDiscounts> findByPaymentIntentId(String paymentIntentId);
}
