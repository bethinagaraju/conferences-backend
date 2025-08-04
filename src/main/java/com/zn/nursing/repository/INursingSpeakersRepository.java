package com.zn.nursing.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zn.nursing.entity.NursingSpeakers;

public interface INursingSpeakersRepository extends JpaRepository<NursingSpeakers, Long> {
    // Define methods for database operations related to NursingSpeakers
    // For example:
    // List<NursingSpeakers> findAll();
    // Optional<NursingSpeakers> findById(Long id);
    // NursingSpeakers save(NursingSpeakers speaker);
    // void deleteById(Long id); 

    // findTop8ByOrderByIdAsc
    
    public List<NursingSpeakers> findTop8ByOrderByIdAsc();
    
}
