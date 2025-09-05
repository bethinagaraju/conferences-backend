package com.zn.nursing.service;

import com.zn.config.HostingerFtpClient;
import com.zn.nursing.entity.NursingSpeakers;
import com.zn.nursing.repository.INursingSpeakersRepository;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NursingSpeakersService {
    @Autowired
    private INursingSpeakersRepository nursingSpeakersRepository;

    @Autowired
    private HostingerFtpClient hostingerFtpClient;

    @Value("${hostinger.public.url}")
    private String HOSTINGER_PUBLIC_URL;

    public List<?> getAllSpeakers() {
        return nursingSpeakersRepository.findAll();
    }

    // Save speaker with image upload to Hostinger FTP

    public void addSpeaker(NursingSpeakers speaker, byte[] imageBytes) {
        String imageUrl = null;
        try {
            String imageName = speaker.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".jpg";
            java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(imageBytes);
            // To avoid caching issues, append a timestamp query param to image URL
            hostingerFtpClient.uploadFile(imageName, inputStream);
            imageUrl = HOSTINGER_PUBLIC_URL + "/" + imageName + "?t=" + System.currentTimeMillis();
        } catch (Exception e) {
            throw new RuntimeException("Image upload error: " + e.getMessage(), e);
        }
        speaker.setImageUrl(imageUrl);
        nursingSpeakersRepository.save(speaker);
    }

    public void deleteSpeaker(NursingSpeakers speaker) {
        nursingSpeakersRepository.delete(speaker);
    }
    // Enhanced: Optionally update image if imageBytes is provided
    public void editSpeaker(NursingSpeakers speaker, byte[] imageBytes) {
        NursingSpeakers existing = nursingSpeakersRepository.findById(speaker.getId()).orElseThrow(() -> new RuntimeException("Speaker not found"));
        existing.setName(speaker.getName());
        existing.setBio(speaker.getBio());
        existing.setUniversity(speaker.getUniversity());
        existing.setType(speaker.getType());
        if (imageBytes != null && imageBytes.length > 0) {
            String imageUrl = null;
            try {
                String imageName = speaker.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".jpg";
                java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(imageBytes);
                // To avoid caching issues, append a timestamp query param to image URL
                hostingerFtpClient.uploadFile(imageName, inputStream);
                imageUrl = HOSTINGER_PUBLIC_URL + "/" + imageName + "?t=" + System.currentTimeMillis();
            } catch (Exception e) {
                throw new RuntimeException("Image upload error: " + e.getMessage(), e);
            }
            existing.setImageUrl(imageUrl);
        }
        nursingSpeakersRepository.save(existing);
    }
    // get top 8 nursing speakers
    public List<?> getTopSpeakers() {
        return nursingSpeakersRepository.findTop8ByOrderByIdAsc();
    }
}
