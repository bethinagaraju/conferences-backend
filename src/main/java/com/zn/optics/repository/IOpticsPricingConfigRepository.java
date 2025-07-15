package com.zn.optics.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.zn.optics.entity.OpticsAccommodation;
import com.zn.optics.entity.OpticsPresentationType;
import com.zn.optics.entity.OpticsPricingConfig;

public interface IOpticsPricingConfigRepository extends JpaRepository<OpticsPricingConfig, Long> {
	 // Valid custom query for registration only (no accommodation)
	  @Query("SELECT p FROM OpticsPricingConfig p WHERE p.presentationType = :presentationType AND p.accommodationOption IS NULL")
	    Optional<OpticsPricingConfig> findByPresentationTypeAndNoAccommodation(@Param("presentationType") OpticsPresentationType presentationType);

	    // Query for PricingConfig with accommodation matching nights and guests
	  @Query("SELECT pc FROM OpticsPricingConfig pc " +
		       "JOIN pc.accommodationOption a " +
		       "WHERE pc.presentationType = :presentationType " +
		       "AND a.nights = :nights AND a.guests = :guests")
		Optional<OpticsPricingConfig> findByPresentationTypeAndAccommodationDetails(
		        @Param("presentationType") OpticsPresentationType presentationType,
		        @Param("nights") int nights,
		        @Param("guests") int guests);

	Optional<OpticsPricingConfig> findByPresentationTypeAndAccommodationOption(OpticsPresentationType savedPresentationType,
			OpticsAccommodation savedAccommodation);
	
	@Query("""
		    SELECT p FROM OpticsPricingConfig p
		    LEFT JOIN p.accommodationOption ao
		    WHERE p.presentationType = :presentationType
		    AND (
		        p.accommodationOption IS NULL
		        OR (ao.nights = 0 AND ao.guests = 0)
		    )
		""")
		List<OpticsPricingConfig> findAllByPresentationTypeAndNoAccommodation(
		    @Param("presentationType") OpticsPresentationType presentationType
		);

	@Query("""
		    SELECT p FROM OpticsPricingConfig p
		    WHERE p.presentationType = :presentationType
		    AND p.accommodationOption.nights = :nights
		    AND p.accommodationOption.guests = :guests
		""")
		List<OpticsPricingConfig> findAllByPresentationTypeAndAccommodationDetails(
		    @Param("presentationType") OpticsPresentationType presentationType,
		    @Param("nights") int nights,
		    @Param("guests") int guests);

	 // Find all configs by presentation type
	    List<OpticsPricingConfig> findByPresentationType(OpticsPresentationType presentationType);

	    // Find all configs by accommodation option
	    List<OpticsPricingConfig> findByAccommodationOption(OpticsAccommodation accommodation);
}
