package com.zn.nursing.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "nursing_speakers")
@Data
public class NursingSpeakers {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String university;
    private String bio;
    private String imageUrl;
    private String type;
    // Additional fields and methods can be added as needed
    
}
