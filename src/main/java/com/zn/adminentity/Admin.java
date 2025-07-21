package com.zn.adminentity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "admins")
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Admin {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer id;
	
	private String email;
	
	@JsonIgnore // This will exclude password from JSON responses
	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY) // Allow writing but not reading
	private String password;
	
	private String name;
	private String role = "ADMIN";

}
