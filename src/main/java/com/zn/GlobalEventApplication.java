package com.zn;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@SpringBootApplication
@EnableMethodSecurity
@EnableWebSecurity
public class GlobalEventApplication {

	public static void main(String[] args) {
		SpringApplication.run(GlobalEventApplication.class, args);
		// ApplicationContext context = SpringApplication.run(GlobalEventApplication.class, args);
		// // Use type-based lookup for AdminService bean (default bean name is 'adminService')
		// com.zn.adminservice.AdminService adminService = context.getBean(com.zn.adminservice.AdminService.class);
		// adminService.registerAdmin("admin@globalrenewablemeet.com", "Admin@123", "Admin User", "ADMIN");

	}

}
