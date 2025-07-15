package com.zn.dto;
import org.springframework.web.multipart.MultipartFile;

import lombok.Data;

@Data
public class AbstractSubmissionRequestDTO {

    private String titlePrefix;       // e.g., "Dr.", "Mr.", "Ms."
    private String name;              // Your Name
    private String email;             // Your Email
    private String phone;             // Your Phone
    private String organizationName;  // Organization Name

    private Long interestedInId;      // Selected option from InterestedInOption
    private Long sessionId;           // Selected session from SessionOption

    private String country;           // Selected country
    private MultipartFile abstractFile;  // Uploaded file
}
