package com.zn.nursing.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.zn.nursing.entity.NursingAccommodation;
import com.zn.nursing.entity.NursingPresentationType;
import com.zn.nursing.entity.NursingPricingConfig;

public interface INursingPricingConfigRepository extends JpaRepository<NursingPricingConfig, Long> {
	 // Valid custom query for registration only (no accommodation)
	  @Query("SELECT p FROM NursingPricingConfig p WHERE p.presentationType = :presentationType AND p.accommodationOption IS NULL")
	    Optional<NursingPricingConfig> findByPresentationTypeAndNoAccommodation(@Param("presentationType") NursingPresentationType presentationType);

	    // Query for PricingConfig with accommodation matching nights and guests
	  @Query("SELECT pc FROM NursingPricingConfig pc " +
		       "JOIN pc.accommodationOption a " +
		       "WHERE pc.presentationType = :presentationType " +
		       "AND a.nights = :nights AND a.guests = :guests")
		Optional<NursingPricingConfig> findByPresentationTypeAndAccommodationDetails(
		        @Param("presentationType") NursingPresentationType presentationType,
		        @Param("nights") int nights,
		        @Param("guests") int guests);

	Optional<NursingPricingConfig> findByPresentationTypeAndAccommodationOption(NursingPresentationType savedPresentationType,
			NursingAccommodation savedAccommodation);
	
	@Query("""
		    SELECT p FROM NursingPricingConfig p
		    LEFT JOIN p.accommodationOption ao
		    WHERE p.presentationType = :presentationType
		    AND (
		        p.accommodationOption IS NULL
		        OR (ao.nights = 0 AND ao.guests = 0)
		    )
		""")
		List<NursingPricingConfig> findAllByPresentationTypeAndNoAccommodation(
		    @Param("presentationType") NursingPresentationType presentationType
		);

	@Query("""
		    SELECT p FROM NursingPricingConfig p
		    WHERE p.presentationType = :presentationType
		    AND p.accommodationOption.nights = :nights
		    AND p.accommodationOption.guests = :guests
		""")
		List<NursingPricingConfig> findAllByPresentationTypeAndAccommodationDetails(
		    @Param("presentationType") NursingPresentationType presentationType,
		    @Param("nights") int nights,
		    @Param("guests") int guests);

	 // Find all configs by presentation type
	    List<NursingPricingConfig> findByPresentationType(NursingPresentationType presentationType);

	    // Find all configs by accommodation option
	    List<NursingPricingConfig> findByAccommodationOption(NursingAccommodation accommodation);
}
