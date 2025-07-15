package com.zn.optics.entity;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class OpticsSessionOption {

    public OpticsSessionOption(String sessionOption) {
    			this.sessionName = sessionOption;
    }

	@Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sessionName;  // e.g., "AI in Healthcare", "Pharma Trends"
}
