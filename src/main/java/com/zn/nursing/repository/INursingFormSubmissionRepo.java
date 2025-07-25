package com.zn.nursing.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zn.nursing.entity.NursingForm;

public interface INursingFormSubmissionRepo extends JpaRepository<NursingForm,Long> {

}
