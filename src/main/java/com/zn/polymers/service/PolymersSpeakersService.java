package com.zn.polymers.service;

import com.zn.config.HostingerFtpClient;
import com.zn.polymers.entity.PolymersSpeakers;
import com.zn.polymers.repository.IPolymersSpeakersRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PolymersSpeakersService {

    @Autowired
    private IPolymersSpeakersRepository polymersSpeakersRepository;

    @Autowired
    private HostingerFtpClient hostingerFtpClient;

    @Value("${hostinger.public.url}")
    private String HOSTINGER_PUBLIC_URL;

    public List<?> getAllSpeakers() {
        return polymersSpeakersRepository.findAll();
    }

    // Save speaker with image upload to Hostinger FTP

    public void addSpeaker(PolymersSpeakers speaker, org.springframework.web.multipart.MultipartFile image) throws Exception {
        String imageName = speaker.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".jpg";
        // To avoid caching issues, append a timestamp query param to image URL
        hostingerFtpClient.uploadFile(imageName, image.getInputStream());
        String publicUrl = HOSTINGER_PUBLIC_URL + "/" + imageName + "?t=" + System.currentTimeMillis();
        speaker.setImageUrl(publicUrl);
        polymersSpeakersRepository.save(speaker);
    }

    public void deleteSpeaker(PolymersSpeakers speaker) {
        polymersSpeakersRepository.delete(speaker);
    }
    // Enhanced: Optionally update image if imageBytes is provided
    public void editSpeaker(PolymersSpeakers speaker, byte[] imageBytes) throws Exception {
        PolymersSpeakers existing = polymersSpeakersRepository.findById(speaker.getId()).orElseThrow(() -> new RuntimeException("Speaker not found"));
        existing.setName(speaker.getName());
        existing.setBio(speaker.getBio());
        existing.setUniversity(speaker.getUniversity());
        existing.setType(speaker.getType());
        if (imageBytes != null && imageBytes.length > 0) {
            String imageName = speaker.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".jpg";
            java.io.ByteArrayInputStream inputStream = new java.io.ByteArrayInputStream(imageBytes);
            // To avoid caching issues, append a timestamp query param to image URL
            hostingerFtpClient.uploadFile(imageName, inputStream);
            String publicUrl = HOSTINGER_PUBLIC_URL + "/" + imageName + "?t=" + System.currentTimeMillis();
            existing.setImageUrl(publicUrl);
        }
        polymersSpeakersRepository.save(existing);
    }

    // get top 8 polymers speakers
    public List<?> getTopSpeakers() {
        return polymersSpeakersRepository.findTop8ByOrderByIdAsc();
    }
}
