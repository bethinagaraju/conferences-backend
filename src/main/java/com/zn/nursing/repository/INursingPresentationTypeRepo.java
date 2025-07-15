package com.zn.nursing.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zn.nursing.entity.NursingPresentationType;

public interface INursingPresentationTypeRepo extends JpaRepository<NursingPresentationType,Long> {
	  Optional<NursingPresentationType> findByType(String type);


}
