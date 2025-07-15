package com.zn.nursing.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zn.nursing.entity.NursingRegistrationForm;

public interface INursingRegistrationFormRepository extends JpaRepository<NursingRegistrationForm, Long> {
	
	/**
	 * Find the most recent registration form by email (to link with payment record)
	 */
	NursingRegistrationForm findTopByEmailOrderByIdDesc(String email);
	
}
