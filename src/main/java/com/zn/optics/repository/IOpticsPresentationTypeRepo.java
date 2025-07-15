package com.zn.optics.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zn.optics.entity.OpticsPresentationType;

public interface IOpticsPresentationTypeRepo extends JpaRepository<OpticsPresentationType,Long> {
	  Optional<OpticsPresentationType> findByType(String type);


}
