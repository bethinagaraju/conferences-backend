package com.zn.polymers.service;

import com.zn.polymers.entity.PolymersSpeakers;
import com.zn.polymers.repository.IPolymersSpeakersRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PolymersSpeakersService {

    @org.springframework.beans.factory.annotation.Autowired
    private IPolymersSpeakersRepository polymersSpeakersRepository;

    @org.springframework.beans.factory.annotation.Autowired
    private com.zn.config.SupabaseStorageClient storageClient;

    @Value("${supabase.url}")
    private String SUPABASE_URL;

    @Value("${supabase.bucket}")
    private String BUCKET_NAME;

    @Value("${supabase.api.key}")
    private String SUPABASE_API_KEY;

    public List<?> getAllSpeakers() {
        return polymersSpeakersRepository.findAll();
    }

    // Save speaker with image upload to Supabase bucket
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.web.client.RestTemplate restTemplate;

    public void addSpeaker(PolymersSpeakers speaker, org.springframework.web.multipart.MultipartFile image) throws Exception {
        String imageName = speaker.getName().replaceAll("[^a-zA-Z0-9]", "_") + ".jpg";
        String publicUrl = storageClient.uploadFile("polymersspeakers/" + imageName, image.getInputStream(), image.getSize());
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
            String publicUrl = storageClient.uploadFile("polymersspeakers/" + imageName, new java.io.ByteArrayInputStream(imageBytes), imageBytes.length);
            existing.setImageUrl(publicUrl);
        }
        polymersSpeakersRepository.save(existing);
    }

    // get top 8 polymers speakers
    public List<?> getTopSpeakers() {
        return polymersSpeakersRepository.findTop8ByOrderByIdAsc();
    }
}
