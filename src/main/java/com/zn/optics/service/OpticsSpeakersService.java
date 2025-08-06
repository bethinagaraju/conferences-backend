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
    @Autowired
    private org.springframework.web.client.RestTemplate restTemplate;

    public void addSpeaker(OpticsSpeakers speaker, byte[] imageBytes) {
        String imageUrl = null;
        try {
            String imageName = speaker.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".jpg";
            String uploadUrl = SUPABASE_URL + "/storage/v1/object/" + BUCKET_NAME + "/opticsspeakers/" + imageName;
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + SUPABASE_API_KEY);
            headers.set("apikey", SUPABASE_API_KEY);
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
            org.springframework.http.HttpEntity<byte[]> entity = new org.springframework.http.HttpEntity<>(imageBytes, headers);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(uploadUrl, org.springframework.http.HttpMethod.PUT, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                imageUrl = SUPABASE_URL + "/storage/v1/object/public/" + BUCKET_NAME + "/opticsspeakers/" + imageName;
            } else {
                throw new RuntimeException("Image upload failed: " + response.getStatusCode().value());
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
        OpticsSpeakers existing = opticsSpeakersRepository.findById(speaker.getId()).orElseThrow(() -> new RuntimeException("Speaker not found"));
        existing.setName(speaker.getName());
        existing.setBio(speaker.getBio());
        existing.setUniversity(speaker.getUniversity());
        existing.setType(speaker.getType());
        if (imageBytes != null && imageBytes.length > 0) {
            String imageUrl = null;
            try {
                String imageName = speaker.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".jpg";
                String uploadUrl = SUPABASE_URL + "/storage/v1/object/" + BUCKET_NAME + "/opticsspeakers/" + imageName;
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.set("Authorization", "Bearer " + SUPABASE_API_KEY);
                headers.set("apikey", SUPABASE_API_KEY);
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
                org.springframework.http.HttpEntity<byte[]> entity = new org.springframework.http.HttpEntity<>(imageBytes, headers);
                org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(uploadUrl, org.springframework.http.HttpMethod.PUT, entity, String.class);
                if (response.getStatusCode().is2xxSuccessful()) {
                    imageUrl = SUPABASE_URL + "/storage/v1/object/public/" + BUCKET_NAME + "/opticsspeakers/" + imageName;
                } else {
                    throw new RuntimeException("Image upload failed: " + response.getStatusCode().value());
                }
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
