package com.zn.polymers.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zn.polymers.entity.PolymersAccommodation;

public interface IPolymersAccommodationRepo extends JpaRepository<PolymersAccommodation, Long>{

	 Optional<PolymersAccommodation> findByNightsAndGuests(int nights, int guests);
	 
}
