package com.zn.renewable.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.zn.renewable.entity.RenewableSpeakers;
import com.zn.renewable.repository.IRenewableSpeakersRepository;

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
    @Autowired
    private org.springframework.web.client.RestTemplate restTemplate;

    public void addSpeaker(RenewableSpeakers speaker, byte[] imageBytes) {
        String imageUrl = null;
        try {
            String imageName = speaker.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".jpg";
            String uploadUrl = SUPABASE_URL + "/storage/v1/object/" + BUCKET_NAME + "/renewablespeakers/" + imageName;
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "Bearer " + SUPABASE_API_KEY);
            headers.set("apikey", SUPABASE_API_KEY);
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
            org.springframework.http.HttpEntity<byte[]> entity = new org.springframework.http.HttpEntity<>(imageBytes, headers);
            org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(uploadUrl, org.springframework.http.HttpMethod.PUT, entity, String.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                imageUrl = SUPABASE_URL + "/storage/v1/object/public/" + BUCKET_NAME + "/renewablespeakers/" + imageName;
            } else {
                throw new RuntimeException("Image upload failed: " + response.getStatusCodeValue());
            }
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
        if (imageBytes != null && imageBytes.length > 0) {
            String imageUrl = null;
            try {
                String imageName = speaker.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".jpg";
                String uploadUrl = SUPABASE_URL + "/storage/v1/object/" + BUCKET_NAME + "/renewablespeakers/" + imageName;
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.set("Authorization", "Bearer " + SUPABASE_API_KEY);
                headers.set("apikey", SUPABASE_API_KEY);
                headers.setContentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
                org.springframework.http.HttpEntity<byte[]> entity = new org.springframework.http.HttpEntity<>(imageBytes, headers);
                org.springframework.http.ResponseEntity<String> response = restTemplate.exchange(uploadUrl, org.springframework.http.HttpMethod.PUT, entity, String.class);
                if (response.getStatusCode().is2xxSuccessful()) {
                    imageUrl = SUPABASE_URL + "/storage/v1/object/public/" + BUCKET_NAME + "/renewablespeakers/" + imageName;
                } else {
                    throw new RuntimeException("Image upload failed: " + response.getStatusCodeValue());
                }
            } catch (Exception e) {
                throw new RuntimeException("Image upload error: " + e.getMessage(), e);
            }
            speaker.setImageUrl(imageUrl);
        }
        renewableSpeakersRepository.save(speaker);
    }
    // get top 8 renewable speakers
    public List<?> getTopSpeakers() {
        return renewableSpeakersRepository.findTop8ByOrderByIdAsc();
    }
}