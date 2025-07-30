package com.zn.polymers.repository;

import com.zn.polymers.entity.PolymersForm;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IPolymersFormSubmissionRepo extends JpaRepository<PolymersForm,Long> {

}
