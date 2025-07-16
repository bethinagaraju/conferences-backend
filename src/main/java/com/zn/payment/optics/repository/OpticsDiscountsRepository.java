package com.zn.payment.optics.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.zn.payment.optics.entity.OpticsDiscounts;
public interface OpticsDiscountsRepository extends JpaRepository<OpticsDiscounts, Long> {
    // Custom query methods can be defined here if needed
	OpticsDiscounts findBySessionId(String sessionId);
	Optional<OpticsDiscounts> findByPaymentIntentId(String paymentIntentId);
}
