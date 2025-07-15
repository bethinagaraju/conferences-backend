package com.zn.nursing.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import lombok.Data;

@Entity
@Data
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class NursingForm {
	 @Id
	    @GeneratedValue(strategy = GenerationType.IDENTITY)
	    private Long id;

	    private String titlePrefix;        
	    
	    @Column(nullable = false)
	    private String name;

	    @Column(nullable = false)
	    private String email;

	    @Column(nullable = false)
	    private String phone;

	    private String organizationName;

	    @ManyToOne
	    private NursingInterestedInOption interestedIn;

	    @ManyToOne
	    private NursingSessionOption session;

	    private String country;

	    private String abstractFilePath;

}
