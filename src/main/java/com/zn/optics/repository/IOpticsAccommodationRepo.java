package com.zn.optics.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zn.optics.entity.OpticsAccommodation;

public interface IOpticsAccommodationRepo extends JpaRepository<OpticsAccommodation, Long>{

	 Optional<OpticsAccommodation> findByNightsAndGuests(int nights, int guests);
	 
}
