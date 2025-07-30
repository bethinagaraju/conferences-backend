package com.zn.payment.polymers.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.zn.payment.nursing.entity.NursingDiscounts;
import com.zn.payment.polymers.entity.PolymersDiscounts;
public interface PolymersDiscountsRepository extends JpaRepository<PolymersDiscounts, Long> {
    // Custom query methods can be defined here if needed
	PolymersDiscounts findBySessionId(String sessionId);
	Optional<PolymersDiscounts> findByPaymentIntentId(String paymentIntentId);
}
