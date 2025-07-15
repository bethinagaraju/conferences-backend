package com.zn.nursing.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zn.nursing.entity.NursingSessionOption;

public interface INursingSessionOption extends JpaRepository<NursingSessionOption, Long> {

}
