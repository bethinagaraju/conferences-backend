package com.zn.optics.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.zn.optics.entity.OpticsSpeakers;
import com.zn.optics.repository.IOpticsSpeakersRepository;

@Service
public class OpticsSpeakersService {
    @Autowired
    private IOpticsSpeakersRepository opticsSpeakersRepository;

    @Value("${supabase.url}")
    private String SUPABASE_URL;

    @Value("${supabase.bucket}")
    private String BUCKET_NAME;

    @Value("${supabase.api.key}")
    private String SUPABASE_API_KEY;

    public List<?> getAllSpeakers() {
        return opticsSpeakersRepository.findAll();
    }

    // Save speaker with image upload to Supabase bucket
    public void addSpeaker(OpticsSpeakers speaker, byte[] imageBytes) {
        String imageUrl = null;
        try {
            String imageName = speaker.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".jpg";
            java.net.URL url = new java.net.URL(SUPABASE_URL + "/storage/v1/object/" + BUCKET_NAME + "/opticsspeakers/" + imageName);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_API_KEY);
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setDoOutput(true);
            try (java.io.OutputStream os = conn.getOutputStream()) {
                os.write(imageBytes);
            }
            int responseCode = conn.getResponseCode();
            if (responseCode == 200 || responseCode == 201) {
                imageUrl = SUPABASE_URL + "/storage/v1/object/public/" + BUCKET_NAME + "/opticsspeakers/" + imageName;
            } else {
                throw new RuntimeException("Image upload failed: " + responseCode);
            }
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
        if (imageBytes != null && imageBytes.length > 0) {
            String imageUrl = null;
            try {
                String imageName = speaker.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".jpg";
                java.net.URL url = new java.net.URL(SUPABASE_URL + "/storage/v1/object/" + BUCKET_NAME + "/opticsspeakers/" + imageName);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Authorization", "Bearer " + SUPABASE_API_KEY);
                conn.setRequestProperty("Content-Type", "application/octet-stream");
                conn.setDoOutput(true);
                try (java.io.OutputStream os = conn.getOutputStream()) {
                    os.write(imageBytes);
                }
                int responseCode = conn.getResponseCode();
                if (responseCode == 200 || responseCode == 201) {
                    imageUrl = SUPABASE_URL + "/storage/v1/object/public/" + BUCKET_NAME + "/opticsspeakers/" + imageName;
                } else {
                    throw new RuntimeException("Image upload failed: " + responseCode);
                }
            } catch (Exception e) {
                throw new RuntimeException("Image upload error: " + e.getMessage(), e);
            }
            speaker.setImageUrl(imageUrl);
        }
        // If imageBytes is null or empty, keep the old imageUrl
        opticsSpeakersRepository.save(speaker);
    }
    // get top 8 optics speakers
    public List<?> getTopSpeakers() {
        return opticsSpeakersRepository.findTop8ByOrderByIdAsc();
    }
}
