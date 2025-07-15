package com.zn.optics.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zn.optics.entity.OpticsRegistrationForm;

public interface IOpricsRegistrationFormRepository extends JpaRepository<OpticsRegistrationForm, Long> {
	
	/**
	 * Find the most recent registration form by email (to link with payment record)
	 */
	OpticsRegistrationForm findTopByEmailOrderByIdDesc(String email);
	
}
