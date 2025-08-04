package com.zn.polymers.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zn.polymers.entity.PolymersSpeakers;

public interface IPolymersSpeakersRepository extends JpaRepository<PolymersSpeakers, Long> {
 

    // findTop8ByOrderByIdAsc
    public List<PolymersSpeakers> findTop8ByOrderByIdAsc();
}
