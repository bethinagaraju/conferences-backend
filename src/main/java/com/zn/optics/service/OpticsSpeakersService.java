package com.zn.optics.service;

import java.io.ByteArrayInputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.zn.config.HostingerFtpClient;
import com.zn.optics.entity.OpticsSpeakers;
import com.zn.optics.repository.IOpticsSpeakersRepository;

@Service
public class OpticsSpeakersService {
    @Autowired
    private IOpticsSpeakersRepository opticsSpeakersRepository;

    @Autowired
    private HostingerFtpClient hostingerFtpClient;

    @Value("${hostinger.public.url}")
    private String HOSTINGER_PUBLIC_URL;

    public List<?> getAllSpeakers() {
        return opticsSpeakersRepository.findAll();
    }

    // Save speaker with image upload to Hostinger FTP

    public void addSpeaker(OpticsSpeakers speaker, byte[] imageBytes) {
        String imageUrl = null;
        try {
            String imageName = speaker.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".jpg";
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
            // To avoid caching issues, append a timestamp query param to image URL
            hostingerFtpClient.uploadFile(imageName, inputStream);
            imageUrl = HOSTINGER_PUBLIC_URL + "/" + imageName + "?t=" + System.currentTimeMillis();
        } catch (Exception e) {
            throw new RuntimeException("Image upload error: " + e.getMessage(), e);
        }
        speaker.setImageUrl(imageUrl);
        opticsSpeakersRepository.save(speaker);
    }

    public void deleteSpeaker(OpticsSpeakers speaker) {
        opticsSpeakersRepository.delete(speaker);
    }
    // Enhanced: Optionally update image if imageBytes is provided
    public void editSpeaker(OpticsSpeakers speaker, byte[] imageBytes) {
        OpticsSpeakers existing = opticsSpeakersRepository.findById(speaker.getId()).orElseThrow(() -> new RuntimeException("Speaker not found"));
        existing.setName(speaker.getName());
        existing.setBio(speaker.getBio());
        existing.setUniversity(speaker.getUniversity());
        existing.setType(speaker.getType());
        if (imageBytes != null && imageBytes.length > 0) {
            String imageUrl = null;
            try {
                String imageName = speaker.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".jpg";
                ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes);
                // To avoid caching issues, append a timestamp query param to image URL
                hostingerFtpClient.uploadFile(imageName, inputStream);
                imageUrl = HOSTINGER_PUBLIC_URL + "/" + imageName + "?t=" + System.currentTimeMillis();
            } catch (Exception e) {
                throw new RuntimeException("Image upload error: " + e.getMessage(), e);
            }
            existing.setImageUrl(imageUrl);
        }
        opticsSpeakersRepository.save(existing);
    }
    // get top 8 optics speakers
    public List<?> getTopSpeakers() {
        return opticsSpeakersRepository.findTop8ByOrderByIdAsc();
    }
}
