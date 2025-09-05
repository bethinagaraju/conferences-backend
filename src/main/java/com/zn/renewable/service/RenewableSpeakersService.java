package com.zn.renewable.service;

import com.zn.config.HostingerFtpClient;
import com.zn.renewable.entity.RenewableSpeakers;
import com.zn.renewable.repository.IRenewableSpeakersRepository;
import java.io.ByteArrayInputStream;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RenewableSpeakersService {
    @Autowired
    private IRenewableSpeakersRepository renewableSpeakersRepository;

    @Autowired
    private HostingerFtpClient hostingerFtpClient;

    @Value("${hostinger.public.url}")
    private String HOSTINGER_PUBLIC_URL;
    public List<?> getAllSpeakers() {
        return renewableSpeakersRepository.findAll();
    }

    // while adding speakers first upload the image and then add the speaker url in the database
    public void addSpeaker(RenewableSpeakers speaker, byte[] imageBytes) {
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
        renewableSpeakersRepository.save(speaker);
    }
    public void deleteSpeaker(RenewableSpeakers speaker) {
        // add error handling if necessary
        renewableSpeakersRepository.delete(speaker);
    }
    // Enhanced: Optionally update image if imageBytes is provided
    public void editSpeaker(RenewableSpeakers speaker, byte[] imageBytes) {
        RenewableSpeakers existing = renewableSpeakersRepository.findById(speaker.getId()).orElseThrow(() -> new RuntimeException("Speaker not found"));
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
        renewableSpeakersRepository.save(existing);
    }
    // get top 8 renewable speakers
    public List<?> getTopSpeakers() {
        return renewableSpeakersRepository.findTop8ByOrderByIdAsc();
    }
}