package com.zn.optics.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zn.optics.entity.OpticsForm;

public interface IOpticsFromSubmissionRepo extends JpaRepository<OpticsForm,Long> {

}
