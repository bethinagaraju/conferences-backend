package com.zn.controller;
import com.zn.dto.SpeakerAddRequestDTO;
import com.zn.nursing.entity.NursingSpeakers;
import com.zn.nursing.service.NursingSpeakersService;
import com.zn.optics.entity.OpticsSpeakers;
import com.zn.optics.service.OpticsSpeakersService;
import com.zn.polymers.entity.PolymersSpeakers;
import com.zn.polymers.service.PolymersSpeakersService;
import com.zn.renewable.entity.RenewableSpeakers;
import com.zn.renewable.service.RenewableSpeakersService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/speakers")
@Slf4j
public class SpeakersController {
    // get all renewable speakers
    @GetMapping("/renewable")
    public List<?> getRenewableSpeakers() {
        List<?> speakers = renewableSpeakersService.getAllSpeakers();
        log.info("Fetched {} renewable speakers", speakers.size());
        return speakers;
    }

    // get all nursing speakers
    @GetMapping("/nursing")
    public List<?> getNursingSpeakers() {
        List<?> speakers = nursingSpeakersService.getAllSpeakers();
        log.info("Fetched {} nursing speakers", speakers.size());
        return speakers;
    }

    // get all optics speakers
    @GetMapping("/optics")
    public List<?> getOpticsSpeakers() {
        List<?> speakers = opticsSpeakersService.getAllSpeakers();
        log.info("Fetched {} optics speakers", speakers.size());
        return speakers;
    }

    // get all polymers speakers
 
    @Autowired
    private RenewableSpeakersService renewableSpeakersService;

    @Autowired
    private NursingSpeakersService nursingSpeakersService;

    @Autowired
    private OpticsSpeakersService opticsSpeakersService;

    @Autowired
    private PolymersSpeakersService polymersSpeakersService;

   
   // get all renewable speakers
    @PutMapping("/nursing/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> editNursingSpeaker(@ModelAttribute SpeakerAddRequestDTO speakerAddRequestDTO) {
        log.info("[ENTRY] editNursingSpeaker called with DTO: {}", speakerAddRequestDTO);
        try {
            NursingSpeakers speaker = new NursingSpeakers();
            speaker.setId(speakerAddRequestDTO.getId());
            speaker.setName(speakerAddRequestDTO.getName());
            speaker.setBio(speakerAddRequestDTO.getBio());
            speaker.setUniversity(speakerAddRequestDTO.getUniversity());
            speaker.setType(speakerAddRequestDTO.getType());

            MultipartFile image = speakerAddRequestDTO.getImage();
            log.info("[EDIT] Nursing Speaker edit request: id={}, name={}, university={}, type={}, bio={}, imagePresent={}",
                speaker.getId(), speaker.getName(), speaker.getUniversity(), speaker.getType(), speaker.getBio(), (image != null));
            if (image != null) {
                log.info("[EDIT] Nursing Speaker image details: name={}, size={}, contentType={}", image.getOriginalFilename(), image.getSize(), image.getContentType());
            }
            byte[] imageBytes = (image != null) ? image.getBytes() : null;
            nursingSpeakersService.editSpeaker(speaker, imageBytes);
            log.info("[EDIT] Nursing Speaker edited successfully: id={}, name={}, university={}, type={}, bio={}",
                speaker.getId(), speaker.getName(), speaker.getUniversity(), speaker.getType(), speaker.getBio());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("[EDIT] Error editing Nursing Speaker: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Image upload failed: " + e.getMessage());
        }
    }
    @PutMapping("/renewable/edit")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> editRenewableSpeaker(@ModelAttribute SpeakerAddRequestDTO speakerAddRequestDTO) {
        log.info("[ENTRY] editRenewableSpeaker called with DTO: {}", speakerAddRequestDTO);
        try {
            RenewableSpeakers speaker = new RenewableSpeakers();
            speaker.setId(speakerAddRequestDTO.getId());
            speaker.setName(speakerAddRequestDTO.getName());
            speaker.setBio(speakerAddRequestDTO.getBio());
            speaker.setUniversity(speakerAddRequestDTO.getUniversity());
            speaker.setType(speakerAddRequestDTO.getType());

            MultipartFile image = speakerAddRequestDTO.getImage();
            log.info("[EDIT] Renewable Speaker edit request: id={}, name={}, university={}, type={}, bio={}, imagePresent={}",
                speaker.getId(), speaker.getName(), speaker.getUniversity(), speaker.getType(), speaker.getBio(), (image != null));
            if (image != null) {
                log.info("[EDIT] Renewable Speaker image details: name={}, size={}, contentType={}", image.getOriginalFilename(), image.getSize(), image.getContentType());
            }
            byte[] imageBytes = (image != null) ? image.getBytes() : null;
            renewableSpeakersService.editSpeaker(speaker, imageBytes);
            log.info("[EDIT] Renewable Speaker edited successfully: id={}, name={}, university={}, type={}, bio={}",
                speaker.getId(), speaker.getName(), speaker.getUniversity(), speaker.getType(), speaker.getBio());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("[EDIT] Error editing Renewable Speaker: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Image upload failed: " + e.getMessage());
        }
    }
    // get all polymers speakers
    @GetMapping("/polymers")
    public List<?> getPolymersSpeakers() {
        List<?> speakers = polymersSpeakersService.getAllSpeakers();
        log.info("Fetched {} polymers speakers", speakers.size());
        return speakers;
    }

    // get top polymers speakers
    @GetMapping("/polymers/top")
    public List<?> getTopPolymersSpeakers() {
        return polymersSpeakersService.getTopSpeakers();
    }
    // get top optics speakers
    @GetMapping("/optics/top")
    public List<?> getTopOpticsSpeakers() {
        return opticsSpeakersService.getTopSpeakers();
    }
    // get top nursing speakers
    @GetMapping("/nursing/top")
    public List<?> getTopNursingSpeakers() {
        return nursingSpeakersService.getTopSpeakers();
    }
    // get top renewable speakers
    @GetMapping("/renewable/top")
    public List<?> getTopRenewableSpeakers() {
        return renewableSpeakersService.getTopSpeakers();
    }
    // while adding speakers first upload the image and then add the speaker url in the database
    @PostMapping("/renewable/add")
//   @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addRenewableSpeaker(@ModelAttribute SpeakerAddRequestDTO speakerAddRequestDTO) {
        log.info("[ENTRY] addRenewableSpeaker called with DTO: {}", speakerAddRequestDTO);
        try {
            RenewableSpeakers speaker = new RenewableSpeakers();
            speaker.setName(speakerAddRequestDTO.getName());
            speaker.setBio(speakerAddRequestDTO.getBio());
            speaker.setUniversity(speakerAddRequestDTO.getUniversity());
            speaker.setType(speakerAddRequestDTO.getType());

            MultipartFile image = speakerAddRequestDTO.getImage();
            log.info("[ADD] Renewable Speaker request received: name={}, university={}, type={}, bio={}, imagePresent={}",
                speaker.getName(), speaker.getUniversity(), speaker.getType(), speaker.getBio(), (image != null));
            if (image != null) {
                log.info("[ADD] Renewable Speaker image details: name={}, size={}, contentType={}", image.getOriginalFilename(), image.getSize(), image.getContentType());
            }
            byte[] imageBytes = (image != null) ? image.getBytes() : null;
            renewableSpeakersService.addSpeaker(speaker, imageBytes);
            log.info("[ADD] Renewable Speaker added successfully: name={}, university={}, type={}, bio={}",
                speaker.getName(), speaker.getUniversity(), speaker.getType(), speaker.getBio());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("[ADD] Error adding Renewable Speaker: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Image upload failed: " + e.getMessage());
        }
    }

    @PostMapping("/nursing/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addNursingSpeaker(@ModelAttribute SpeakerAddRequestDTO speakerAddRequestDTO) {
        log.info("[ENTRY] addNursingSpeaker called with DTO: {}", speakerAddRequestDTO);
        try {
            NursingSpeakers speaker = new NursingSpeakers();
            speaker.setName(speakerAddRequestDTO.getName());
            speaker.setBio(speakerAddRequestDTO.getBio());
            speaker.setUniversity(speakerAddRequestDTO.getUniversity());
            speaker.setType(speakerAddRequestDTO.getType());

            MultipartFile image = speakerAddRequestDTO.getImage();
            log.info("[ADD] Nursing Speaker request received: name={}, university={}, type={}, bio={}, imagePresent={}",
                speaker.getName(), speaker.getUniversity(), speaker.getType(), speaker.getBio(), (image != null));
            if (image != null) {
                log.info("[ADD] Nursing Speaker image details: name={}, size={}, contentType={}", image.getOriginalFilename(), image.getSize(), image.getContentType());
            }
            byte[] imageBytes = (image != null) ? image.getBytes() : null;
            nursingSpeakersService.addSpeaker(speaker, imageBytes);
            log.info("[ADD] Nursing Speaker added successfully: name={}, university={}, type={}, bio={}",
                speaker.getName(), speaker.getUniversity(), speaker.getType(), speaker.getBio());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("[ADD] Error adding Nursing Speaker: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Image upload failed: " + e.getMessage());
        }
    }
    @PostMapping("/optics/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addOpticsSpeaker(@ModelAttribute SpeakerAddRequestDTO speakerAddRequestDTO) {
        log.info("[ENTRY] addOpticsSpeaker called with DTO: {}", speakerAddRequestDTO);
        try {
            OpticsSpeakers speaker = new OpticsSpeakers();
            speaker.setName(speakerAddRequestDTO.getName());
            speaker.setBio(speakerAddRequestDTO.getBio());
            speaker.setUniversity(speakerAddRequestDTO.getUniversity());
            speaker.setType(speakerAddRequestDTO.getType());

            MultipartFile image = speakerAddRequestDTO.getImage();
            log.info("[ADD] Optics Speaker request received: name={}, university={}, type={}, bio={}, imagePresent={}",
                speaker.getName(), speaker.getUniversity(), speaker.getType(), speaker.getBio(), (image != null));
            if (image != null) {
                log.info("[ADD] Optics Speaker image details: name={}, size={}, contentType={}", image.getOriginalFilename(), image.getSize(), image.getContentType());
            }
            byte[] imageBytes = (image != null) ? image.getBytes() : null;
            opticsSpeakersService.addSpeaker(speaker, imageBytes);
            log.info("[ADD] Optics Speaker added successfully: name={}, university={}, type={}, bio={}",
                speaker.getName(), speaker.getUniversity(), speaker.getType(), speaker.getBio());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("[ADD] Error adding Optics Speaker: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Image upload failed: " + e.getMessage());
        }
    }

    @PostMapping(value = "polymers/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addPolymersSpeaker(@ModelAttribute SpeakerAddRequestDTO speakerAddRequestDTO) {
        log.info("[ENTRY] addPolymersSpeaker called with DTO: {}", speakerAddRequestDTO);
        try {

            PolymersSpeakers speaker = new PolymersSpeakers();
            speaker.setName(speakerAddRequestDTO.getName());
            speaker.setBio(speakerAddRequestDTO.getBio());
            speaker.setUniversity(speakerAddRequestDTO.getUniversity());
            speaker.setType(speakerAddRequestDTO.getType());

            MultipartFile image = speakerAddRequestDTO.getImage();
            log.info("[ADD] Polymers Speaker request received: name={}, university={}, type={}, bio={}, imagePresent={}",
                speaker.getName(), speaker.getUniversity(), speaker.getType(), speaker.getBio(), (image != null));
            if (image != null) {
                log.info("[ADD] Polymers Speaker image details: name={}, size={}, contentType={}", image.getOriginalFilename(), image.getSize(), image.getContentType());
            }
            byte[] imageBytes = (image != null) ? image.getBytes() : null;
            polymersSpeakersService.addSpeaker(speaker, speakerAddRequestDTO.getImage());
            log.info("[ADD] Polymers Speaker added successfully: name={}, university={}, type={}, bio={}",
                speaker.getName(), speaker.getUniversity(), speaker.getType(), speaker.getBio());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("[ADD] Error adding Polymers Speaker: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Image upload failed: " + e.getMessage());
        }
    }

    // edit and delete methods can be added similarly



    @PutMapping("/optics/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> editOpticsSpeaker(@ModelAttribute SpeakerAddRequestDTO speakerAddRequestDTO) {
        log.info("[ENTRY] editOpticsSpeaker called with DTO: {}", speakerAddRequestDTO);
        try {
            OpticsSpeakers speaker = new OpticsSpeakers();
            speaker.setId(speakerAddRequestDTO.getId());
            speaker.setName(speakerAddRequestDTO.getName());
            speaker.setBio(speakerAddRequestDTO.getBio());
            speaker.setUniversity(speakerAddRequestDTO.getUniversity());
            speaker.setType(speakerAddRequestDTO.getType());

            MultipartFile image = speakerAddRequestDTO.getImage();
            log.info("[EDIT] Optics Speaker edit request: id={}, name={}, university={}, type={}, bio={}, imagePresent={}",
                speaker.getId(), speaker.getName(), speaker.getUniversity(), speaker.getType(), speaker.getBio(), (image != null));
            if (image != null) {
                log.info("[EDIT] Optics Speaker image details: name={}, size={}, contentType={}", image.getOriginalFilename(), image.getSize(), image.getContentType());
            }
            byte[] imageBytes = (image != null) ? image.getBytes() : null;
            opticsSpeakersService.editSpeaker(speaker, imageBytes);
            log.info("[EDIT] Optics Speaker edited successfully: id={}, name={}, university={}, type={}, bio={}",
                speaker.getId(), speaker.getName(), speaker.getUniversity(), speaker.getType(), speaker.getBio());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("[EDIT] Error editing Optics Speaker: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Image upload failed: " + e.getMessage());
        }
    }

    @PutMapping("/polymers/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> editPolymersSpeaker(@ModelAttribute SpeakerAddRequestDTO speakerAddRequestDTO) {
        log.info("[ENTRY] editPolymersSpeaker called with DTO: {}", speakerAddRequestDTO);
        try {
            PolymersSpeakers speaker = new PolymersSpeakers();
            speaker.setId(speakerAddRequestDTO.getId());
            speaker.setName(speakerAddRequestDTO.getName());
            speaker.setBio(speakerAddRequestDTO.getBio());
            speaker.setUniversity(speakerAddRequestDTO.getUniversity());
            speaker.setType(speakerAddRequestDTO.getType());

            MultipartFile image = speakerAddRequestDTO.getImage();
            log.info("[EDIT] Polymers Speaker edit request: id={}, name={}, university={}, type={}, bio={}, imagePresent={}",
                speaker.getId(), speaker.getName(), speaker.getUniversity(), speaker.getType(), speaker.getBio(), (image != null));
            if (image != null) {
                log.info("[EDIT] Polymers Speaker image details: name={}, size={}, contentType={}", image.getOriginalFilename(), image.getSize(), image.getContentType());
            }
            byte[] imageBytes = (image != null) ? image.getBytes() : null;
            polymersSpeakersService.editSpeaker(speaker, imageBytes);
            log.info("[EDIT] Polymers Speaker edited successfully: id={}, name={}, university={}, type={}, bio={}",
                speaker.getId(), speaker.getName(), speaker.getUniversity(), speaker.getType(), speaker.getBio());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("[EDIT] Error editing Polymers Speaker: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("Image upload failed: " + e.getMessage());
        }
    }
    @DeleteMapping("/renewable/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteRenewableSpeaker(@RequestBody RenewableSpeakers speaker) {
        renewableSpeakersService.deleteSpeaker(speaker);
        return ResponseEntity.ok().build();
    }
    @DeleteMapping("/nursing/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteNursingSpeaker(@RequestBody NursingSpeakers speaker) {
        nursingSpeakersService.deleteSpeaker(speaker);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/optics/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteOpticsSpeaker(@RequestBody OpticsSpeakers speaker) {
        opticsSpeakersService.deleteSpeaker(speaker);
        return ResponseEntity.ok().build();
    }
    @DeleteMapping("/polymers/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deletePolymersSpeaker(@RequestBody PolymersSpeakers speaker) {
        polymersSpeakersService.deleteSpeaker(speaker);
        return ResponseEntity.ok().build();
    }
    
    

}