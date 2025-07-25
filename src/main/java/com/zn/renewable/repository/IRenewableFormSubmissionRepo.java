package com.zn.renewable.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zn.renewable.entity.RenewableForm;

public interface IRenewableFormSubmissionRepo extends JpaRepository<RenewableForm,Long> {

}
