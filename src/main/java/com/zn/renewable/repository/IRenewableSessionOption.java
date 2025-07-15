package com.zn.renewable.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zn.renewable.entity.RenewableSessionOption;

public interface IRenewableSessionOption extends JpaRepository<RenewableSessionOption, Long> {

}
