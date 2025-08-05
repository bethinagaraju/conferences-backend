package com.zn.renewable.service;

import com.zn.renewable.entity.RenewableSpeakers;
import com.zn.renewable.repository.IRenewableSpeakersRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class RenewableSpeakersService {
    @Autowired
    private IRenewableSpeakersRepository renewableSpeakersRepository;
     @Value("${supabase.url}")
    private String SUPABASE_URL;

    @Value("${supabase.bucket}")
    private String BUCKET_NAME;

    @Value("${supabase.api.key}")
    private String SUPABASE_API_KEY;
    public List<?> getAllSpeakers() {
        return renewableSpeakersRepository.findAll();
    }

    // while adding speakers first upload the image and then add the speaker url in the database
    public void addSpeaker(RenewableSpeakers speaker, byte[] imageBytes) {
        // 1. Upload image to Supabase bucket
        String imageUrl = null;
        try {
            // Use speaker name as image filename, sanitized
            String imageName = speaker.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".jpg";
            java.net.URL url = new java.net.URL(SUPABASE_URL + "/storage/v1/object/" + BUCKET_NAME + "/renewablespeakers/" + imageName);
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
                imageUrl = SUPABASE_URL + "/storage/v1/object/public/" + BUCKET_NAME + "/renewablespeakers/" + imageName;
            } else {
                throw new RuntimeException("Image upload failed: " + responseCode);
            }
        } catch (Exception e) {
            throw new RuntimeException("Image upload error: " + e.getMessage(), e);
        }

        // 2. Set image URL in speaker and save
        speaker.setImageUrl(imageUrl);
        renewableSpeakersRepository.save(speaker);
    }
    public void deleteSpeaker(RenewableSpeakers speaker) {
        // add error handling if necessary
        renewableSpeakersRepository.delete(speaker);
    }
    // Enhanced: Optionally update image if imageBytes is provided
    public void editSpeaker(RenewableSpeakers speaker, byte[] imageBytes) {
        if (imageBytes != null && imageBytes.length > 0) {
            String imageUrl = null;
            try {
                String imageName = speaker.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".jpg";
                java.net.URL url = new java.net.URL(SUPABASE_URL + "/storage/v1/object/" + BUCKET_NAME + "/renewablespeakers/" + imageName);
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
                    imageUrl = SUPABASE_URL + "/storage/v1/object/public/" + BUCKET_NAME + "/renewablespeakers/" + imageName;
                } else {
                    throw new RuntimeException("Image upload failed: " + responseCode);
                }
            } catch (Exception e) {
                throw new RuntimeException("Image upload error: " + e.getMessage(), e);
            }
            speaker.setImageUrl(imageUrl);
        }
        // If imageBytes is null or empty, keep the old imageUrl
        renewableSpeakersRepository.save(speaker);
    }
    // get top 8 renewable speakers
    public List<?> getTopSpeakers() {
        return renewableSpeakersRepository.findTop8ByOrderByIdAsc();
    }
}