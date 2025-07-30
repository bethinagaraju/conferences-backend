package com.zn.polymers.repository;

import com.zn.polymers.entity.PolymersRegistrationForm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IPolymersRegistrationFormRepository extends JpaRepository<PolymersRegistrationForm, Long> {
	
	/**
	 * Find the most recent registration form by email (to link with payment record)
	 */
	PolymersRegistrationForm findTopByEmailOrderByIdDesc(String email);
	
}
