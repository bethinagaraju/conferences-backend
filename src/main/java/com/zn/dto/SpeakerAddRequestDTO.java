package com.zn.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class SpeakerAddRequestDTO {
    private Long id;
    private String name;
    private String university;
    private String bio;
    private String type;
    private MultipartFile image;
}
