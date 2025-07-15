package com.zn.renewable.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zn.renewable.entity.RenewableRegistrationForm;

public interface IRenewableRegistrationFormRepository extends JpaRepository<RenewableRegistrationForm, Long> {
	
	/**
	 * Find the most recent registration form by email (to link with payment record)
	 */
	RenewableRegistrationForm findTopByEmailOrderByIdDesc(String email);
	
}
