package com.zn.adminrepo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.zn.adminentity.Admin;

public interface IAdminRepo extends JpaRepository<Admin, Integer> {
		
	
	Admin findByEmailAndPassword(String email, String password);
	Admin findByEmail(String email);
}
