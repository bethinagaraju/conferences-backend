package com.zn.renewable.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zn.renewable.entity.RenewableSpeakers;

public interface IRenewableSpeakersRepository extends JpaRepository<RenewableSpeakers, Long> {
    // Define methods for database operations related to RenewableSpeakers
    // For example:
    // List<NursingSpeakers> findAll();
    // Optional<NursingSpeakers> findById(Long id);
    // NursingSpeakers save(NursingSpeakers speaker);
    // void deleteById(Long id); 
    // findTop8ByOrderByIdAsc
    public List<RenewableSpeakers> findTop8ByOrderByIdAsc();
}
