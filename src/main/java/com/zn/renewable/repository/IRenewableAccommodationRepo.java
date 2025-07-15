package com.zn.renewable.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zn.renewable.entity.RenewableAccommodation;

public interface IRenewableAccommodationRepo extends JpaRepository<RenewableAccommodation, Long>{

	 Optional<RenewableAccommodation> findByNightsAndGuests(int nights, int guests);
	 
}
