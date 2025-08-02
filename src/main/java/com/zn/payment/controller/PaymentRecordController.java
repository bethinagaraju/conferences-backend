
package com.zn.payment.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zn.payment.dto.PaymentResponseDTO;
import com.zn.payment.nursing.entity.NursingPaymentRecord;
import com.zn.payment.nursing.service.NursingPaymentRecordService;
import com.zn.payment.nursing.service.NursingStripeService;
import com.zn.payment.optics.entity.OpticsPaymentRecord;
import com.zn.payment.optics.service.OpticsPaymentRecordService;
import com.zn.payment.renewable.entity.RenewablePaymentRecord;
import com.zn.payment.renewable.service.RenewablePaymentRecordService;


/**
 * REST Controller for payment record operations - ADMIN ACCESS ONLY
 */
@RestController
@RequestMapping("/api/payments")
@PreAuthorize("hasRole('ADMIN')") // Require ADMIN role for all endpoints in this controller
public class PaymentRecordController {
    
    private static final Logger logger = LoggerFactory.getLogger(PaymentRecordController.class);

    @Autowired
    private NursingStripeService nursingStripeService;
    
    @Autowired
    private OpticsPaymentRecordService opticsPaymentRecordService;
    
    @Autowired
    private RenewablePaymentRecordService renewablePaymentRecordService;

    // Add NursingPaymentRecordService for nursing vertical
    @Autowired
    private NursingPaymentRecordService nursingPaymentRecordService;
    
    // Add PolymersPaymentRecordService for polymers vertical
    @Autowired
    private com.zn.payment.polymers.service.PolymersPaymentRecordService polymersPaymentRecordService;







    /**
     * Utility method to get current authenticated admin user for logging
     */
    private String getCurrentAdminUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            return authentication != null ? authentication.getName() : "Unknown";
        } catch (Exception e) {
            return "Unknown";
        }
    }

    // Get payment record by session ID for Optics
    @GetMapping("/session/optics/{sessionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getOpticsPaymentBySessionId(@PathVariable String sessionId) {
        String adminUser = getCurrentAdminUser();
        logger.info("ADMIN {}: Retrieving payment record for session: {} for optics", adminUser, sessionId);
        try {
            Optional<OpticsPaymentRecord> payment = opticsPaymentRecordService.findBySessionId(sessionId);
            if (payment.isPresent()) {
                return ResponseEntity.ok(PaymentResponseDTO.fromEntity(payment.get()));
            } else {
                return ResponseEntity.ok().body(Map.of(
                    "message", "Payment record not found",
                    "sessionId", sessionId,
                    "website", "optics",
                    "found", false
                ));
            }
        } catch (Exception e) {
            logger.error("ADMIN {}: Error retrieving payment for session {} for optics: {}", adminUser, sessionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error retrieving payment record",
                "message", e.getMessage(),
                "website", "optics"
            ));
        }
    }

    // Get payment record by session ID for Renewable
    @GetMapping("/session/renewable/{sessionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getRenewablePaymentBySessionId(@PathVariable String sessionId) {
        String adminUser = getCurrentAdminUser();
        logger.info("ADMIN {}: Retrieving payment record for session: {} for renewable", adminUser, sessionId);
        try {
            Optional<RenewablePaymentRecord> payment = renewablePaymentRecordService.findBySessionId(sessionId);
            if (payment.isPresent()) {
                return ResponseEntity.ok(PaymentResponseDTO.fromEntity(payment.get()));
            } else {
                return ResponseEntity.ok().body(Map.of(
                    "message", "Payment record not found",
                    "sessionId", sessionId,
                    "website", "renewable",
                    "found", false
                ));
            }
        } catch (Exception e) {
            logger.error("ADMIN {}: Error retrieving payment for session {} for renewable: {}", adminUser, sessionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error retrieving payment record",
                "message", e.getMessage(),
                "website", "renewable"
            ));
        }
    }

    // Get payment record by session ID for Nursing
    @GetMapping("/session/nursing/{sessionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getNursingPaymentBySessionId(@PathVariable String sessionId) {
        String adminUser = getCurrentAdminUser();
        logger.info("ADMIN {}: Retrieving payment record for session: {} for nursing", adminUser, sessionId);
        try {
            Optional<NursingPaymentRecord> payment = nursingPaymentRecordService.findBySessionId(sessionId);
            if (payment.isPresent()) {
                return ResponseEntity.ok(PaymentResponseDTO.fromEntity(payment.get()));
            } else {
                return ResponseEntity.ok().body(Map.of(
                    "message", "Payment record not found",
                    "sessionId", sessionId,
                    "website", "nursing",
                    "found", false
                ));
            }
        } catch (Exception e) {
            logger.error("ADMIN {}: Error retrieving payment for session {} for nursing: {}", adminUser, sessionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error retrieving payment record",
                "message", e.getMessage(),
                "website", "nursing"
            ));
        }
    }

    // Get payment record by session ID for Polymers
    @GetMapping("/session/polymers/{sessionId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPolymersPaymentBySessionId(@PathVariable String sessionId) {
        String adminUser = getCurrentAdminUser();
        logger.info("ADMIN {}: Retrieving payment record for session: {} for polymers", adminUser, sessionId);
        try {
            Optional<com.zn.payment.polymers.entity.PolymersPaymentRecord> payment = polymersPaymentRecordService.findBySessionId(sessionId);
            if (payment.isPresent()) {
                return ResponseEntity.ok(PaymentResponseDTO.fromEntity(payment.get()));
            } else {
                return ResponseEntity.ok().body(Map.of(
                    "message", "Payment record not found",
                    "sessionId", sessionId,
                    "website", "polymers",
                    "found", false
                ));
            }
        } catch (Exception e) {
            logger.error("ADMIN {}: Error retrieving payment for session {} for polymers: {}", adminUser, sessionId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error retrieving payment record",
                "message", e.getMessage(),
                "website", "polymers"
            ));
        }
    }
        // Get payment record by ID for Optics
    @GetMapping("/optics/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getOpticsPaymentById(@PathVariable Long id) {
        String adminUser = getCurrentAdminUser();
        logger.info("ADMIN {}: Retrieving optics payment record by ID: {}", adminUser, id);
        try {
            Optional<OpticsPaymentRecord> payment = opticsPaymentRecordService.findById(id);
            if (payment.isPresent()) {
                return ResponseEntity.ok(PaymentResponseDTO.fromEntity(payment.get()));
            } else {
                return ResponseEntity.ok().body(Map.of(
                    "message", "Optics payment record not found",
                    "id", id,
                    "website", "optics",
                    "found", false
                ));
            }
        } catch (Exception e) {
            logger.error("ADMIN {}: Error retrieving optics payment by ID {}: {}", adminUser, id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error retrieving optics payment record",
                "message", e.getMessage(),
                "website", "optics"
            ));
        }
    }

    // Get payment record by ID for Renewable
    @GetMapping("/renewable/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getRenewablePaymentById(@PathVariable Long id) {
        String adminUser = getCurrentAdminUser();
        logger.info("ADMIN {}: Retrieving renewable payment record by ID: {}", adminUser, id);
        try {
            Optional<RenewablePaymentRecord> payment = renewablePaymentRecordService.findById(id);
            if (payment.isPresent()) {
                return ResponseEntity.ok(PaymentResponseDTO.fromEntity(payment.get()));
            } else {
                return ResponseEntity.ok().body(Map.of(
                    "message", "Renewable payment record not found",
                    "id", id,
                    "website", "renewable",
                    "found", false
                ));
            }
        } catch (Exception e) {
            logger.error("ADMIN {}: Error retrieving renewable payment by ID {}: {}", adminUser, id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error retrieving renewable payment record",
                "message", e.getMessage(),
                "website", "renewable"
            ));
        }
    }

    // Get payment record by ID for Nursing
    @GetMapping("/nursing/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getNursingPaymentById(@PathVariable Long id) {
        String adminUser = getCurrentAdminUser();
        logger.info("ADMIN {}: Retrieving nursing payment record by ID: {}", adminUser, id);
        try {
            Optional<NursingPaymentRecord> payment = nursingPaymentRecordService.findById(id);
            if (payment.isPresent()) {
                return ResponseEntity.ok(PaymentResponseDTO.fromEntity(payment.get()));
            } else {
                return ResponseEntity.ok().body(Map.of(
                    "message", "Nursing payment record not found",
                    "id", id,
                    "website", "nursing",
                    "found", false
                ));
            }
        } catch (Exception e) {
            logger.error("ADMIN {}: Error retrieving nursing payment by ID {}: {}", adminUser, id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error retrieving nursing payment record",
                "message", e.getMessage(),
                "website", "nursing"
            ));
        }
    }

    // Get payment record by ID for Polymers
    @GetMapping("/polymers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPolymersPaymentById(@PathVariable Long id) {
        String adminUser = getCurrentAdminUser();
        logger.info("ADMIN {}: Retrieving polymers payment record by ID: {}", adminUser, id);
        try {
            Optional<com.zn.payment.polymers.entity.PolymersPaymentRecord> payment = polymersPaymentRecordService.findById(id);
            if (payment.isPresent()) {
                return ResponseEntity.ok(PaymentResponseDTO.fromEntity(payment.get()));
            } else {
                return ResponseEntity.ok().body(Map.of(
                    "message", "Polymers payment record not found",
                    "id", id,
                    "website", "polymers",
                    "found", false
                ));
            }
        } catch (Exception e) {
            logger.error("ADMIN {}: Error retrieving polymers payment by ID {}: {}", adminUser, id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error retrieving polymers payment record",
                "message", e.getMessage(),
                "website", "polymers"
            ));
        }
    }
    // Get payment records by customer email for Optics
    @GetMapping("/customer/optics/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDTO>> getOpticsPaymentsByCustomer(@PathVariable String email) {
        String adminUser = getCurrentAdminUser();
        logger.info("ADMIN {}: Retrieving payment records for customer: {} for optics", adminUser, email);
        try {
            List<OpticsPaymentRecord> opticsPayments = opticsPaymentRecordService.findByCustomerEmail(email);
            List<PaymentResponseDTO> response = new java.util.ArrayList<>();
            opticsPayments.forEach(p -> response.add(PaymentResponseDTO.fromEntity(p)));
            logger.info("ADMIN {}: Found {} payment records for customer: {} for optics", adminUser, response.size(), email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("ADMIN {}: Error retrieving payments for customer {} for optics: {}", adminUser, email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get payment records by customer email for Renewable
    @GetMapping("/customer/renewable/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDTO>> getRenewablePaymentsByCustomer(@PathVariable String email) {
        String adminUser = getCurrentAdminUser();
        logger.info("ADMIN {}: Retrieving payment records for customer: {} for renewable", adminUser, email);
        try {
            List<RenewablePaymentRecord> renewablePayments = renewablePaymentRecordService.findByCustomerEmail(email);
            List<PaymentResponseDTO> response = new java.util.ArrayList<>();
            renewablePayments.forEach(p -> response.add(PaymentResponseDTO.fromEntity(p)));
            logger.info("ADMIN {}: Found {} payment records for customer: {} for renewable", adminUser, response.size(), email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("ADMIN {}: Error retrieving payments for customer {} for renewable: {}", adminUser, email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get payment records by customer email for Nursing
    @GetMapping("/customer/nursing/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDTO>> getNursingPaymentsByCustomer(@PathVariable String email) {
        String adminUser = getCurrentAdminUser();
        logger.info("ADMIN {}: Retrieving payment records for customer: {} for nursing", adminUser, email);
        try {
            List<NursingPaymentRecord> nursingPayments = nursingPaymentRecordService.findByCustomerEmail(email);
            List<PaymentResponseDTO> response = new java.util.ArrayList<>();
            nursingPayments.forEach(p -> response.add(PaymentResponseDTO.fromEntity(p)));
            logger.info("ADMIN {}: Found {} payment records for customer: {} for nursing", adminUser, response.size(), email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("ADMIN {}: Error retrieving payments for customer {} for nursing: {}", adminUser, email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get payment records by customer email for Polymers
    @GetMapping("/customer/polymers/{email}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDTO>> getPolymersPaymentsByCustomer(@PathVariable String email) {
        String adminUser = getCurrentAdminUser();
        logger.info("ADMIN {}: Retrieving payment records for customer: {} for polymers", adminUser, email);
        try {
            List<com.zn.payment.polymers.entity.PolymersPaymentRecord> polymersPayments = polymersPaymentRecordService.findByCustomerEmail(email);
            List<PaymentResponseDTO> response = new java.util.ArrayList<>();
            polymersPayments.forEach(p -> response.add(PaymentResponseDTO.fromEntity(p)));
            logger.info("ADMIN {}: Found {} payment records for customer: {} for polymers", adminUser, response.size(), email);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("ADMIN {}: Error retrieving payments for customer {} for polymers: {}", adminUser, email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get payment records by status for Optics
    @GetMapping("/status/optics/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDTO>> getOpticsPaymentsByStatus(@PathVariable com.zn.payment.optics.entity.OpticsPaymentRecord.PaymentStatus status) {
        String adminUser = getCurrentAdminUser();
        logger.info("ADMIN {}: Retrieving payment records with status: {} for optics", adminUser, status);
        try {
            List<OpticsPaymentRecord> opticsPayments = opticsPaymentRecordService.findByStatus(status);
            List<PaymentResponseDTO> response = new java.util.ArrayList<>();
            opticsPayments.forEach(p -> response.add(PaymentResponseDTO.fromEntity(p)));
            logger.info("ADMIN {}: Found {} payment records with status: {} for optics", adminUser, response.size(), status);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("ADMIN {}: Error retrieving payments by status {} for optics: {}", adminUser, status, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get payment records by status for Renewable
    @GetMapping("/status/renewable/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDTO>> getRenewablePaymentsByStatus(@PathVariable com.zn.payment.renewable.entity.RenewablePaymentRecord.PaymentStatus status) {
        String adminUser = getCurrentAdminUser();
        logger.info("ADMIN {}: Retrieving payment records with status: {} for renewable", adminUser, status);
        try {
            List<RenewablePaymentRecord> renewablePayments = renewablePaymentRecordService.findByStatus(status);
            List<PaymentResponseDTO> response = new java.util.ArrayList<>();
            renewablePayments.forEach(p -> response.add(PaymentResponseDTO.fromEntity(p)));
            logger.info("ADMIN {}: Found {} payment records with status: {} for renewable", adminUser, response.size(), status);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("ADMIN {}: Error retrieving payments by status {} for renewable: {}", adminUser, status, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get payment records by status for Nursing
    @GetMapping("/status/nursing/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDTO>> getNursingPaymentsByStatus(@PathVariable com.zn.payment.nursing.entity.NursingPaymentRecord.PaymentStatus status) {
        String adminUser = getCurrentAdminUser();
        logger.info("ADMIN {}: Retrieving payment records with status: {} for nursing", adminUser, status);
        try {
            List<NursingPaymentRecord> nursingPayments = nursingPaymentRecordService.findByStatus(status);
            List<PaymentResponseDTO> response = new java.util.ArrayList<>();
            nursingPayments.forEach(p -> response.add(PaymentResponseDTO.fromEntity(p)));
            logger.info("ADMIN {}: Found {} payment records with status: {} for nursing", adminUser, response.size(), status);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("ADMIN {}: Error retrieving payments by status {} for nursing: {}", adminUser, status, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get payment records by status for Polymers
    @GetMapping("/status/polymers/{status}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDTO>> getPolymersPaymentsByStatus(@PathVariable com.zn.payment.polymers.entity.PolymersPaymentRecord.PaymentStatus status) {
        String adminUser = getCurrentAdminUser();
        logger.info("ADMIN {}: Retrieving payment records with status: {} for polymers", adminUser, status);
        try {
            List<com.zn.payment.polymers.entity.PolymersPaymentRecord> polymersPayments = polymersPaymentRecordService.findByStatus(status);
            List<PaymentResponseDTO> response = new java.util.ArrayList<>();
            polymersPayments.forEach(p -> response.add(PaymentResponseDTO.fromEntity(p)));
            logger.info("ADMIN {}: Found {} payment records with status: {} for polymers", adminUser, response.size(), status);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("ADMIN {}: Error retrieving payments by status {} for polymers: {}", adminUser, status, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    /**
     * Get all expired payment records for Optics - ADMIN ONLY
     */
    @GetMapping("/expired/optics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDTO>> getExpiredOpticsPayments() {
        String adminUser = getCurrentAdminUser();
        logger.info("ADMIN {}: Retrieving expired payment records for optics", adminUser);
        try {
            List<OpticsPaymentRecord> opticsPayments = opticsPaymentRecordService.findExpiredRecords();
            List<PaymentResponseDTO> response = new java.util.ArrayList<>();
            opticsPayments.forEach(p -> response.add(PaymentResponseDTO.fromEntity(p)));
            logger.info("ADMIN {}: Found {} expired payment records for optics", adminUser, response.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("ADMIN {}: Error retrieving expired payments for optics: {}", adminUser, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all expired payment records for Renewable - ADMIN ONLY
     */
    @GetMapping("/expired/renewable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDTO>> getExpiredRenewablePayments() {
        String adminUser = getCurrentAdminUser();
        logger.info("ADMIN {}: Retrieving expired payment records for renewable", adminUser);
        try {
            List<RenewablePaymentRecord> renewablePayments = renewablePaymentRecordService.findExpiredRecords();
            List<PaymentResponseDTO> response = new java.util.ArrayList<>();
            renewablePayments.forEach(p -> response.add(PaymentResponseDTO.fromEntity(p)));
            logger.info("ADMIN {}: Found {} expired payment records for renewable", adminUser, response.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("ADMIN {}: Error retrieving expired payments for renewable: {}", adminUser, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all expired payment records for Nursing - ADMIN ONLY
     */
    @GetMapping("/expired/nursing")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDTO>> getExpiredNursingPayments() {
        String adminUser = getCurrentAdminUser();
        logger.info("ADMIN {}: Retrieving expired payment records for nursing", adminUser);
        try {
            List<NursingPaymentRecord> nursingPayments = nursingPaymentRecordService.findExpiredRecords();
            List<PaymentResponseDTO> response = new java.util.ArrayList<>();
            nursingPayments.forEach(p -> response.add(PaymentResponseDTO.fromEntity(p)));
            logger.info("ADMIN {}: Found {} expired payment records for nursing", adminUser, response.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("ADMIN {}: Error retrieving expired payments for nursing: {}", adminUser, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all expired payment records for Polymers - ADMIN ONLY
     */
    @GetMapping("/expired/polymers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDTO>> getExpiredPolymersPayments() {
        String adminUser = getCurrentAdminUser();
        logger.info("ADMIN {}: Retrieving expired payment records for polymers", adminUser);
        try {
            List<com.zn.payment.polymers.entity.PolymersPaymentRecord> polymersPayments = polymersPaymentRecordService.findExpiredRecords();
            List<PaymentResponseDTO> response = new java.util.ArrayList<>();
            polymersPayments.forEach(p -> response.add(PaymentResponseDTO.fromEntity(p)));
            logger.info("ADMIN {}: Found {} expired payment records for polymers", adminUser, response.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("ADMIN {}: Error retrieving expired payments for polymers: {}", adminUser, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Mark expired records as EXPIRED - ADMIN ONLY
     */
    @GetMapping("/expire-stale")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> markExpiredRecords() {
        logger.info("ADMIN: Marking expired payment records");
        try {
            int opticsCount = opticsPaymentRecordService.markExpiredRecords();
            int renewableCount = renewablePaymentRecordService.markExpiredRecords();
            int nursingCount = nursingPaymentRecordService.markExpiredRecords();
            int polymersCount = polymersPaymentRecordService.markExpiredRecords();
            int total = opticsCount + renewableCount + nursingCount + polymersCount;
            String message = "ADMIN: Marked " + total + " payment records as expired (Optics: " + opticsCount + ", Renewable: " + renewableCount + ", Nursing: " + nursingCount + ", Polymers: " + polymersCount + ")";
            logger.info(message);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            logger.error("ADMIN: Error marking expired records: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("ADMIN: Error marking expired records: " + e.getMessage());
        }
    }

    /**
     * Get payment statistics - ADMIN ONLY
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getPaymentStatistics() {
        logger.info("ADMIN: Retrieving payment statistics");
        try {
            var opticsStats = opticsPaymentRecordService.getPaymentStatistics();
            var renewableStats = renewablePaymentRecordService.getPaymentStatistics();
            var nursingStats = nursingPaymentRecordService.getPaymentStatistics();
            var polymersStats = polymersPaymentRecordService.getPaymentStatistics();
            Map<String, Object> totalStats = Map.of(
                "optics", opticsStats,
                "renewable", renewableStats,
                "nursing", nursingStats,
                "polymers", polymersStats
            );
            logger.info("ADMIN: Retrieved payment statistics successfully");
            return ResponseEntity.ok(totalStats);
        } catch (Exception e) {
            logger.error("ADMIN: Error retrieving payment statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Health check endpoint for admin - ADMIN ONLY
     */
    @GetMapping("/admin/health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> adminHealthCheck() {
        logger.info("ADMIN: Health check requested");
        return ResponseEntity.ok("ADMIN: Payment Records API is healthy and secured");
    }

    /**
     * Get payment statistics - ADMIN ONLY
     */
    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getPaymentStats() {
        String adminUser = getCurrentAdminUser();
        logger.info("ADMIN {}: Retrieving payment statistics", adminUser);
        try {
            var opticsStats = opticsPaymentRecordService.getPaymentStatistics();
            var renewableStats = renewablePaymentRecordService.getPaymentStatistics();
            var nursingStats = nursingPaymentRecordService.getPaymentStatistics();
            var polymersStats = polymersPaymentRecordService.getPaymentStatistics();
            Map<String, Object> totalStats = Map.of(
                "optics", opticsStats,
                "renewable", renewableStats,
                "nursing", nursingStats,
                "polymers", polymersStats
            );
            logger.info("ADMIN: Retrieved enhanced payment statistics successfully");
            return ResponseEntity.ok(totalStats);
        } catch (Exception e) {
            logger.error("ADMIN: Error retrieving payment statistics: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Test admin authentication - ADMIN ONLY  
     * Use this endpoint to verify JWT authentication is working
     */
    @GetMapping("/admin/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> testAdminAuth() {
        String adminUser = getCurrentAdminUser();
        logger.info("ADMIN {}: Authentication test successful", adminUser);
        
        return ResponseEntity.ok(Map.of(
            "message", "Admin authentication successful",
            "adminUser", adminUser,
            "timestamp", java.time.LocalDateTime.now(),
            "status", "authenticated"
        ));
    }

    // Get all payment records for Optics
    @GetMapping("/all/optics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDTO>> getAllOpticsPayments() {
        String adminUser = getCurrentAdminUser();
        logger.info("ADMIN {}: Retrieving all payment records for optics", adminUser);
        try {
            List<OpticsPaymentRecord> payments = opticsPaymentRecordService.findAllPayments();
            List<PaymentResponseDTO> response = new java.util.ArrayList<>();
            for (OpticsPaymentRecord payment : payments) {
                response.add(PaymentResponseDTO.fromEntity(payment));
            }
            logger.info("ADMIN {}: Retrieved {} total payment records for optics", adminUser, response.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("ADMIN {}: Error retrieving all payments for optics: {}", adminUser, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get all payment records for Renewable
    @GetMapping("/all/renewable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDTO>> getAllRenewablePayments() {
        String adminUser = getCurrentAdminUser();
        logger.info("ADMIN {}: Retrieving all payment records for renewable", adminUser);
        try {
            List<RenewablePaymentRecord> payments = renewablePaymentRecordService.findAllPayments();
            List<PaymentResponseDTO> response = new java.util.ArrayList<>();
            for (RenewablePaymentRecord payment : payments) {
                response.add(PaymentResponseDTO.fromEntity(payment));
            }
            logger.info("ADMIN {}: Retrieved {} total payment records for renewable", adminUser, response.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("ADMIN {}: Error retrieving all payments for renewable: {}", adminUser, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get all payment records for Nursing
    @GetMapping("/all/nursing")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDTO>> getAllNursingPayments() {
        String adminUser = getCurrentAdminUser();
        logger.info("ADMIN {}: Retrieving all payment records for nursing", adminUser);
        try {
            List<NursingPaymentRecord> payments = nursingPaymentRecordService.findAllPayments();
            List<PaymentResponseDTO> response = new java.util.ArrayList<>();
            for (NursingPaymentRecord payment : payments) {
                response.add(PaymentResponseDTO.fromEntity(payment));
            }
            logger.info("ADMIN {}: Retrieved {} total payment records for nursing", adminUser, response.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("ADMIN {}: Error retrieving all payments for nursing: {}", adminUser, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Get all payment records for Polymers
    @GetMapping("/all/polymers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDTO>> getAllPolymersPayments() {
        String adminUser = getCurrentAdminUser();
        logger.info("ADMIN {}: Retrieving all payment records for polymers", adminUser);
        try {
            List<com.zn.payment.polymers.entity.PolymersPaymentRecord> payments = polymersPaymentRecordService.findAllPayments();
            List<PaymentResponseDTO> response = new java.util.ArrayList<>();
            for (com.zn.payment.polymers.entity.PolymersPaymentRecord payment : payments) {
                response.add(PaymentResponseDTO.fromEntity(payment));
            }
            logger.info("ADMIN {}: Retrieved {} total payment records for polymers", adminUser, response.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("ADMIN {}: Error retrieving all payments for polymers: {}", adminUser, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get recent payment records (last 24 hours) - ADMIN ONLY
     * Perfect for dashboard recent activity widget
     */
    @GetMapping("/recent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<PaymentResponseDTO>> getRecentPayments() {
        String adminUser = getCurrentAdminUser();
        logger.info("ADMIN {}: Retrieving recent payment records", adminUser);
        try {
            List<PaymentResponseDTO> response = new java.util.ArrayList<>();
            // Optics
            List<OpticsPaymentRecord> opticsPayments = opticsPaymentRecordService.findRecentPayments();
            opticsPayments.forEach(p -> response.add(PaymentResponseDTO.fromEntity(p)));
            // Renewable
            List<RenewablePaymentRecord> renewablePayments = renewablePaymentRecordService.findRecentPayments();
            renewablePayments.forEach(p -> response.add(PaymentResponseDTO.fromEntity(p)));
            // Nursing
            List<com.zn.payment.nursing.entity.NursingPaymentRecord> nursingPayments = nursingPaymentRecordService.findRecentPayments();
            nursingPayments.forEach(p -> response.add(PaymentResponseDTO.fromEntity(p)));
            // Polymers
            List<com.zn.payment.polymers.entity.PolymersPaymentRecord> polymersPayments = polymersPaymentRecordService.findRecentPayments();
            polymersPayments.forEach(p -> response.add(PaymentResponseDTO.fromEntity(p)));
            logger.info("ADMIN {}: Retrieved {} recent payment records", adminUser, response.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("ADMIN {}: Error retrieving recent payments: {}", adminUser, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get completed payments with registration details - ADMIN ONLY
     * Shows successful payments with associated registration information
     */
    @GetMapping("/completed-with-registrations")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getCompletedPaymentsWithRegistrations() {
        String adminUser = getCurrentAdminUser();
        logger.info("ADMIN {}: Retrieving completed payments with registration details", adminUser);
        try {
            List<Map<String, Object>> response = new java.util.ArrayList<>();
            // Optics
            List<OpticsPaymentRecord> opticsPayments = opticsPaymentRecordService.findByStatus(com.zn.payment.optics.entity.OpticsPaymentRecord.PaymentStatus.COMPLETED);
            for (OpticsPaymentRecord payment : opticsPayments) {
                Map<String, Object> paymentWithRegistration = Map.of(
                    "paymentId", payment.getId(),
                    "sessionId", payment.getSessionId(),
                    "customerEmail", payment.getCustomerEmail(),
                    "amountTotal", payment.getAmountTotal(),
                    "currency", payment.getCurrency(),
                    "paymentStatus", payment.getPaymentStatus(),
                    "createdAt", payment.getCreatedAt(),
                    "hasRegistration", payment.getRegistrationForm() != null,
                    "registrationId", payment.getRegistrationForm() != null ? payment.getRegistrationForm().getId() : null
                );
                response.add(paymentWithRegistration);
            }
            // Renewable
            List<RenewablePaymentRecord> renewablePayments = renewablePaymentRecordService.findByStatus(com.zn.payment.renewable.entity.RenewablePaymentRecord.PaymentStatus.COMPLETED);
            for (RenewablePaymentRecord payment : renewablePayments) {
                Map<String, Object> paymentWithRegistration = Map.of(
                    "paymentId", payment.getId(),
                    "sessionId", payment.getSessionId(),
                    "customerEmail", payment.getCustomerEmail(),
                    "amountTotal", payment.getAmountTotal(),
                    "currency", payment.getCurrency(),
                    "paymentStatus", payment.getPaymentStatus(),
                    "createdAt", payment.getCreatedAt(),
                    "hasRegistration", payment.getRegistrationForm() != null,
                    "registrationId", payment.getRegistrationForm() != null ? payment.getRegistrationForm().getId() : null
                );
                response.add(paymentWithRegistration);
            }
            // Nursing
            List<com.zn.payment.nursing.entity.NursingPaymentRecord> nursingPayments = nursingPaymentRecordService.findByStatus(com.zn.payment.nursing.entity.NursingPaymentRecord.PaymentStatus.COMPLETED);
            for (com.zn.payment.nursing.entity.NursingPaymentRecord payment : nursingPayments) {
                Map<String, Object> paymentWithRegistration = Map.of(
                    "paymentId", payment.getId(),
                    "sessionId", payment.getSessionId(),
                    "customerEmail", payment.getCustomerEmail(),
                    "amountTotal", payment.getAmountTotal(),
                    "currency", payment.getCurrency(),
                    "paymentStatus", payment.getPaymentStatus(),
                    "createdAt", payment.getCreatedAt(),
                    "hasRegistration", payment.getRegistrationForm() != null,
                    "registrationId", payment.getRegistrationForm() != null ? payment.getRegistrationForm().getId() : null
                );
                response.add(paymentWithRegistration);
            }
            // Polymers
            List<com.zn.payment.polymers.entity.PolymersPaymentRecord> polymersPayments = polymersPaymentRecordService.findByStatus(com.zn.payment.polymers.entity.PolymersPaymentRecord.PaymentStatus.COMPLETED);
            for (com.zn.payment.polymers.entity.PolymersPaymentRecord payment : polymersPayments) {
                Map<String, Object> paymentWithRegistration = Map.of(
                    "paymentId", payment.getId(),
                    "sessionId", payment.getSessionId(),
                    "customerEmail", payment.getCustomerEmail(),
                    "amountTotal", payment.getAmountTotal(),
                    "currency", payment.getCurrency(),
                    "paymentStatus", payment.getPaymentStatus(),
                    "createdAt", payment.getCreatedAt(),
                    "hasRegistration", payment.getRegistrationForm() != null,
                    "registrationId", payment.getRegistrationForm() != null ? payment.getRegistrationForm().getId() : null
                );
                response.add(paymentWithRegistration);
            }
            logger.info("ADMIN {}: Retrieved {} completed payments with registration status", adminUser, response.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("ADMIN {}: Error retrieving completed payments with registrations: {}", adminUser, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error retrieving payment and registration data",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * Get payment record by ID - ADMIN ONLY
     * For detailed view in dashboard
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPaymentById(@PathVariable Long id) {
        String adminUser = getCurrentAdminUser();
        logger.info("ADMIN {}: Retrieving payment record by ID: {}", adminUser, id);
        try {
            // Try to find by ID in all four services
            Optional<OpticsPaymentRecord> opticsPayment = opticsPaymentRecordService.findById(id);
            if (opticsPayment.isPresent()) {
                PaymentResponseDTO response = PaymentResponseDTO.fromEntity(opticsPayment.get());
                logger.info("ADMIN {}: Found optics payment record ID: {}", adminUser, id);
                return ResponseEntity.ok(response);
            }
            Optional<RenewablePaymentRecord> renewablePayment = renewablePaymentRecordService.findById(id);
            if (renewablePayment.isPresent()) {
                PaymentResponseDTO response = PaymentResponseDTO.fromEntity(renewablePayment.get());
                logger.info("ADMIN {}: Found renewable payment record ID: {}", adminUser, id);
                return ResponseEntity.ok(response);
            }
            Optional<com.zn.payment.nursing.entity.NursingPaymentRecord> nursingPayment = nursingPaymentRecordService.findById(id);
            if (nursingPayment.isPresent()) {
                PaymentResponseDTO response = PaymentResponseDTO.fromEntity(nursingPayment.get());
                logger.info("ADMIN {}: Found nursing payment record ID: {}", adminUser, id);
                return ResponseEntity.ok(response);
            }
            Optional<com.zn.payment.polymers.entity.PolymersPaymentRecord> polymersPayment = polymersPaymentRecordService.findById(id);
            if (polymersPayment.isPresent()) {
                PaymentResponseDTO response = PaymentResponseDTO.fromEntity(polymersPayment.get());
                logger.info("ADMIN {}: Found polymers payment record ID: {}", adminUser, id);
                return ResponseEntity.ok(response);
            }
            logger.warn("ADMIN {}: Payment record not found for ID: {}", adminUser, id);
            return ResponseEntity.ok().body(Map.of(
                "message", "Payment record not found",
                "id", id,
                "found", false
            ));
        } catch (Exception e) {
            logger.error("ADMIN {}: Error retrieving payment ID {}: {}", adminUser, id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Error retrieving payment record",
                "message", e.getMessage()
            ));
        }
    }
}
