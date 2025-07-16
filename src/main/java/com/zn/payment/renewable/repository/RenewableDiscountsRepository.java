package com.zn.payment.renewable.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.zn.payment.renewable.entity.RenewableDiscounts;
public interface RenewableDiscountsRepository extends JpaRepository<RenewableDiscounts, Long> {
    // Custom query methods can be defined here if needed
	RenewableDiscounts findBySessionId(String sessionId);
	Optional<RenewableDiscounts> findByPaymentIntentId(String paymentIntentId);
}
