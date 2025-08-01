package com.zn.polymers.repository;

import com.zn.polymers.entity.PolymersAccommodation;
import com.zn.polymers.entity.PolymersPresentationType;
import com.zn.polymers.entity.PolymersPricingConfig;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IPolymersPricingConfigRepository extends JpaRepository<PolymersPricingConfig, Long> {
	 // Valid custom query for registration only (no accommodation)
	  @Query("SELECT p FROM PolymersPricingConfig p WHERE p.presentationType = :presentationType AND p.accommodationOption IS NULL")
		Optional<PolymersPricingConfig> findByPresentationTypeAndNoAccommodation(@Param("presentationType") PolymersPresentationType presentationType);

		// Query for PricingConfig with accommodation matching nights and guests
	  @Query("SELECT pc FROM PolymersPricingConfig pc " +
			   "JOIN pc.accommodationOption a " +
			   "WHERE pc.presentationType = :presentationType " +
			   "AND a.nights = :nights AND a.guests = :guests")
		Optional<PolymersPricingConfig> findByPresentationTypeAndAccommodationDetails(
				@Param("presentationType") PolymersPresentationType presentationType,
				@Param("nights") int nights,
				@Param("guests") int guests);

	Optional<PolymersPricingConfig> findByPresentationTypeAndAccommodationOption(PolymersPresentationType savedPresentationType,
			PolymersAccommodation savedAccommodation);

	@Query("""
			SELECT p FROM PolymersPricingConfig p
			LEFT JOIN p.accommodationOption ao
			WHERE p.presentationType = :presentationType
			AND (
				p.accommodationOption IS NULL
				OR (ao.nights = 0 AND ao.guests = 0)
			)
		""")
		List<PolymersPricingConfig> findAllByPresentationTypeAndNoAccommodation(
			@Param("presentationType") PolymersPresentationType presentationType
		);

	@Query("""
			SELECT p FROM PolymersPricingConfig p
			WHERE p.presentationType = :presentationType
			AND p.accommodationOption.nights = :nights
			AND p.accommodationOption.guests = :guests
		""")
		List<PolymersPricingConfig> findAllByPresentationTypeAndAccommodationDetails(
			@Param("presentationType") PolymersPresentationType presentationType,
			@Param("nights") int nights,
			@Param("guests") int guests);

	 // Find all configs by presentation type
		List<PolymersPricingConfig> findByPresentationType(PolymersPresentationType presentationType);

		// Find all configs by accommodation option
		List<PolymersPricingConfig> findByAccommodationOption(PolymersAccommodation accommodation);
}
