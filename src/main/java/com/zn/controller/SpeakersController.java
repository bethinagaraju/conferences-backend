package com.zn.controller;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.zn.nursing.entity.NursingSpeakers;
import com.zn.nursing.service.NursingSpeakersService;
import com.zn.optics.entity.OpticsSpeakers;
import com.zn.optics.service.OpticsSpeakersService;
import com.zn.polymers.entity.PolymersSpeakers;
import com.zn.polymers.service.PolymersSpeakersService;
import com.zn.renewable.entity.RenewableSpeakers;
import com.zn.renewable.service.RenewableSpeakersService;

@RestController
@RequestMapping("/api/speakers")
public class SpeakersController {
    @Autowired
    private RenewableSpeakersService renewableSpeakersService;

    @Autowired
    private NursingSpeakersService nursingSpeakersService;

    @Autowired
    private OpticsSpeakersService opticsSpeakersService;

    @Autowired
    private PolymersSpeakersService polymersSpeakersService;

   
   // get all renewable speakers
    @GetMapping("/renewable")
    public List<?> getRenewableSpeakers() {
        return renewableSpeakersService.getAllSpeakers();
    }

    // get all nursing speakers
    @GetMapping("/nursing")
    public List<?> getNursingSpeakers() {
        return nursingSpeakersService.getAllSpeakers();
    }

    // get all optics speakers
    @GetMapping("/optics")
    public List<?> getOpticsSpeakers() {
        return opticsSpeakersService.getAllSpeakers();
    }

    // get all polymers speakers
    @GetMapping("/polymers")
    public List<?> getPolymersSpeakers() {
        return polymersSpeakersService.getAllSpeakers();
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
    // while adding speakers first updload the image and then add the speaker url in the database
    @PostMapping("renewable/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addRenewableSpeaker(
        @RequestPart("speaker") RenewableSpeakers speaker,
        @RequestPart("image") MultipartFile image
    ) {
        try {
            renewableSpeakersService.addSpeaker(speaker, image.getBytes());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Image upload failed: " + e.getMessage());
        }
    }

    @PostMapping(value = "nursing/add" )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addNursingSpeaker(
        @RequestPart("speaker") NursingSpeakers speaker,
        @RequestPart("image") MultipartFile image
    ) {
        try {
            nursingSpeakersService.addSpeaker(speaker, image.getBytes());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Image upload failed: " + e.getMessage());
        }
    }

    @PostMapping("optics/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addOpticsSpeaker(
        @RequestPart("speaker") OpticsSpeakers speaker,
        @RequestPart("image") MultipartFile image
    ) {
        try {
            opticsSpeakersService.addSpeaker(speaker, image.getBytes());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Image upload failed: " + e.getMessage());
        }
    }

    @PostMapping("polymers/add")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addPolymersSpeaker(
        @RequestPart("speaker") PolymersSpeakers speaker,
        @RequestPart("image") MultipartFile image
    ) {
        try {
            polymersSpeakersService.addSpeaker(speaker, image.getBytes());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Image upload failed: " + e.getMessage());
        }
    }

    // edit and delete methods can be added similarly

    @PutMapping("/renewable/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> editRenewableSpeaker(
        @RequestPart("speaker") RenewableSpeakers speaker,
        @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        try {
            byte[] imageBytes = (image != null) ? image.getBytes() : null;
            renewableSpeakersService.editSpeaker(speaker, imageBytes);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Image upload failed: " + e.getMessage());
        }
    }

    @PutMapping("/nursing/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> editNursingSpeaker(
        @RequestPart("speaker") NursingSpeakers speaker,
        @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        try {
            byte[] imageBytes = (image != null) ? image.getBytes() : null;
            nursingSpeakersService.editSpeaker(speaker, imageBytes);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Image upload failed: " + e.getMessage());
        }
    }

    @PutMapping("/optics/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> editOpticsSpeaker(
        @RequestPart("speaker") OpticsSpeakers speaker,
        @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        try {
            byte[] imageBytes = (image != null) ? image.getBytes() : null;
            opticsSpeakersService.editSpeaker(speaker, imageBytes);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Image upload failed: " + e.getMessage());
        }
    }

    @PutMapping("/polymers/edit")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> editPolymersSpeaker(
        @RequestPart("speaker") PolymersSpeakers speaker,
        @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        try {
            byte[] imageBytes = (image != null) ? image.getBytes() : null;
            polymersSpeakersService.editSpeaker(speaker, imageBytes);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
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