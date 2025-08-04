package com.zn.optics.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zn.optics.entity.OpticsSpeakers;

public interface IOpticsSpeakersRepository extends JpaRepository<OpticsSpeakers, Long> {

      // findTop8ByOrderByIdAsc
      public List<OpticsSpeakers> findTop8ByOrderByIdAsc();
}
