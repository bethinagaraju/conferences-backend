package com.zn.renewable.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "renewable_speakers")
@Data
public class RenewableSpeakers {
    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String university;
    private String bio;
    private String imageUrl;
    private String type;
    // Additional fields and methods can be added as needed
    
}
