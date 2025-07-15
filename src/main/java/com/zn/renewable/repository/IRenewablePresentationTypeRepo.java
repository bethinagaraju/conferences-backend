package com.zn.renewable.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zn.renewable.entity.RenewablePresentationType;

public interface IRenewablePresentationTypeRepo extends JpaRepository<RenewablePresentationType,Long> {
	  Optional<RenewablePresentationType> findByType(String type);


}
