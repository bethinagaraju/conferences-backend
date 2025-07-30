package com.zn.polymers.repository;

import com.zn.polymers.entity.PolymersPresentationType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IPolymersPresentationTypeRepo extends JpaRepository<PolymersPresentationType,Long> {
	  Optional<PolymersPresentationType> findByType(String type);


}
