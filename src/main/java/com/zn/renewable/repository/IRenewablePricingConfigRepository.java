package com.zn.renewable.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.zn.renewable.entity.RenewableAccommodation;
import com.zn.renewable.entity.RenewablePresentationType;
import com.zn.renewable.entity.RenewablePricingConfig;

public interface IRenewablePricingConfigRepository extends JpaRepository<RenewablePricingConfig, Long> {
	 // Valid custom query for registration only (no accommodation)
	  @Query("SELECT p FROM RenewablePricingConfig p WHERE p.presentationType = :presentationType AND p.accommodationOption IS NULL")
	    Optional<RenewablePricingConfig> findByPresentationTypeAndNoAccommodation(@Param("presentationType") RenewablePresentationType presentationType);

	    // Query for PricingConfig with accommodation matching nights and guests
	  @Query("SELECT pc FROM RenewablePricingConfig pc " +
		       "JOIN pc.accommodationOption a " +
		       "WHERE pc.presentationType = :presentationType " +
		       "AND a.nights = :nights AND a.guests = :guests")
		Optional<RenewablePricingConfig> findByPresentationTypeAndAccommodationDetails(
		        @Param("presentationType") RenewablePresentationType presentationType,
		        @Param("nights") int nights,
		        @Param("guests") int guests);

	Optional<RenewablePricingConfig> findByPresentationTypeAndAccommodationOption(RenewablePresentationType savedPresentationType,
			RenewableAccommodation savedAccommodation);
	
	@Query("""
		    SELECT p FROM RenewablePricingConfig p
		    LEFT JOIN p.accommodationOption ao
		    WHERE p.presentationType = :presentationType
		    AND (
		        p.accommodationOption IS NULL
		        OR (ao.nights = 0 AND ao.guests = 0)
		    )
		""")
		List<RenewablePricingConfig> findAllByPresentationTypeAndNoAccommodation(
		    @Param("presentationType") RenewablePresentationType presentationType
		);

	@Query("""
		    SELECT p FROM RenewablePricingConfig p
		    WHERE p.presentationType = :presentationType
		    AND p.accommodationOption.nights = :nights
		    AND p.accommodationOption.guests = :guests
		""")
		List<RenewablePricingConfig> findAllByPresentationTypeAndAccommodationDetails(
		    @Param("presentationType") RenewablePresentationType presentationType,
		    @Param("nights") int nights,
		    @Param("guests") int guests);

	 // Find all configs by presentation type
	    List<RenewablePricingConfig> findByPresentationType(RenewablePresentationType presentationType);

	    // Find all configs by accommodation option
	    List<RenewablePricingConfig> findByAccommodationOption(RenewableAccommodation accommodation);
}
