package com.zn.nursing.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zn.nursing.entity.NursingAccommodation;

public interface INursingAccommodationRepo extends JpaRepository<NursingAccommodation, Long>{

	 Optional<NursingAccommodation> findByNightsAndGuests(int nights, int guests);
	 
}
