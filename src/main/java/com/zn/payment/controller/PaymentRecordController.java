// package com.zn.payment.controller;

// import java.util.List;
// import java.util.Map;
// import java.util.Optional;

// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.http.HttpStatus;
// import org.springframework.http.ResponseEntity;
// import org.springframework.security.access.prepost.PreAuthorize;
// import org.springframework.security.core.Authentication;
// import org.springframework.security.core.context.SecurityContextHolder;
// import org.springframework.web.bind.annotation.GetMapping;
// import org.springframework.web.bind.annotation.PathVariable;
// import org.springframework.web.bind.annotation.RequestMapping;
// import org.springframework.web.bind.annotation.RestController;

// import com.zn.payment.nursing.entity.NursingPaymentRecord.PaymentStatus;
// import com.zn.payment.nursing.service.NursingStripeService;
// import com.zn.payment.optics.entity.OpticsPaymentRecord;
// import com.zn.payment.optics.service.OpticsPaymentRecordService;
// import com.zn.payment.renewable.entity.RenewablePaymentRecord;
// import com.zn.payment.renewable.service.RenewablePaymentRecordService;

// import jakarta.servlet.http.HttpServletRequest;


// /**
//  * REST Controller for payment record operations - ADMIN ACCESS ONLY
//  */
// @RestController
// @RequestMapping("/api/payments")
// @PreAuthorize("hasRole('ADMIN')") // Require ADMIN role for all endpoints in this controller
// public class PaymentRecordController {
    
//     private static final Logger logger = LoggerFactory.getLogger(PaymentRecordController.class);

//     @Autowired
//     private NursingStripeService nursingStripeService;
    
//     @Autowired
//     private OpticsPaymentRecordService opticsPaymentRecordService;
    
//     @Autowired
//     private RenewablePaymentRecordService renewablePaymentRecordService;

//     /**
//      * Helper method to determine which service to use based on request headers
//      */
//     private String getDomainFromRequest(HttpServletRequest request) {
//         String origin = request.getHeader("Origin");
//         String referer = request.getHeader("Referer");
        
//         if ((origin != null && origin.contains("globallopmeet.com")) || 
//             (referer != null && referer.contains("globallopmeet.com"))) {
//             return "optics";
//         } else if ((origin != null && origin.contains("globalrenewablemeet.com")) || 
//                    (referer != null && referer.contains("globalrenewablemeet.com"))) {
//             return "renewable";
//         } else {
//             // Default to nursing service for backward compatibility
//             return "nursing";
//         }
//     }

//     /**
//      * Utility method to get current authenticated admin user for logging
//      */
//     private String getCurrentAdminUser() {
//         try {
//             Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//             return authentication != null ? authentication.getName() : "Unknown";
//         } catch (Exception e) {
//             return "Unknown";
//         }
//     }

//     /**
//      * Get payment record by session ID - ADMIN ONLY
//      * Requires JWT authentication with ADMIN role
//      */
//     @GetMapping("/session/{sessionId}")
//     @PreAuthorize("hasRole('ADMIN')")
//     public ResponseEntity<?> getPaymentBySessionId(@PathVariable String sessionId, HttpServletRequest request) {
//         String adminUser = getCurrentAdminUser();
//         String domain = getDomainFromRequest(request);
//         logger.info("ADMIN {}: Retrieving payment record for session: {} from domain: {}", adminUser, sessionId, domain);
        
//         try {
//             if ("optics".equals(domain)) {
//                 Optional<OpticsPaymentRecord> payment = opticsPaymentRecordService.findBySessionId(sessionId);
//                 if (payment.isPresent()) {
//                     logger.info("ADMIN {}: Found optics payment record for session: {}", adminUser, sessionId);
//                     PaymentResponseDTO response = PaymentResponseDTO.fromOpticsEntity(payment.get());
//                     return ResponseEntity.ok(response);
//                 } else {
//                     logger.warn("ADMIN {}: Optics payment record not found for session: {}", adminUser, sessionId);
//                     return ResponseEntity.ok().body(Map.of(
//                         "message", "Payment record not found",
//                         "sessionId", sessionId,
//                         "domain", "optics",
//                         "found", false
//                     ));
//                 }
//             } else if ("renewable".equals(domain)) {
//                 Optional<RenewablePaymentRecord> payment = renewablePaymentRecordService.findBySessionId(sessionId);
//                 if (payment.isPresent()) {
//                     logger.info("ADMIN {}: Found renewable payment record for session: {}", adminUser, sessionId);
//                     PaymentResponseDTO response = PaymentResponseDTO.fromRenewableEntity(payment.get());
//                     return ResponseEntity.ok(response);
//                 } else {
//                     logger.warn("ADMIN {}: Renewable payment record not found for session: {}", adminUser, sessionId);
//                     return ResponseEntity.ok().body(Map.of(
//                         "message", "Payment record not found",
//                         "sessionId", sessionId,
//                         "domain", "renewable",
//                         "found", false
//                     ));
//                 }
//             } else {
//                 // Default to nursing - Note: This would need a proper nursing PaymentRecordService
//                 // For now, we'll return a not implemented response for nursing until the service is available
//                 logger.warn("ADMIN {}: Nursing payment record service not yet implemented", adminUser);
//                 return ResponseEntity.ok().body(Map.of(
//                     "message", "Nursing payment record service not yet implemented",
//                     "sessionId", sessionId,
//                     "domain", "nursing",
//                     "found", false
//                 ));
//             }
//         } catch (Exception e) {
//             logger.error("ADMIN {}: Error retrieving payment for session {} from domain {}: {}", adminUser, sessionId, domain, e.getMessage(), e);
//             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
//                 "error", "Error retrieving payment record",
//                 "message", e.getMessage(),
//                 "domain", domain
//             ));
//         }
//     }

//     /**
//      * Get payment records by customer email - ADMIN ONLY
//      * Requires JWT authentication with ADMIN role
//      */
//     @GetMapping("/customer/{email}")
//     @PreAuthorize("hasRole('ADMIN')")
//     public ResponseEntity<List<PaymentResponseDTO>> getPaymentsByCustomer(@PathVariable String email) {
//         String adminUser = getCurrentAdminUser();
//         logger.info("ADMIN {}: Retrieving payment records for customer: {}", adminUser, email);
        
//         try {
//             List<PaymentRecord> payments = paymentRecordService.findByCustomerEmail(email);
//             List<PaymentResponseDTO> response = payments.stream()
//                 .map(PaymentResponseDTO::fromEntity)
//                 .collect(java.util.stream.Collectors.toList());
//             logger.info("ADMIN {}: Found {} payment records for customer: {}", adminUser, payments.size(), email);
//             return ResponseEntity.ok(response);
//         } catch (Exception e) {
//             logger.error("ADMIN {}: Error retrieving payments for customer {}: {}", adminUser, email, e.getMessage(), e);
//             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//         }
//     }

//     /**
//      * Get payment records by status - ADMIN ONLY
//      */
//     @GetMapping("/status/{status}")
//     @PreAuthorize("hasRole('ADMIN')")
//     public ResponseEntity<List<PaymentResponseDTO>> getPaymentsByStatus(@PathVariable PaymentStatus status) {
//         String adminUser = getCurrentAdminUser();
//         logger.info("ADMIN {}: Retrieving payment records with status: {}", adminUser, status);
        
//         try {
//             List<PaymentRecord> payments = paymentRecordService.findByStatus(status);
//             List<PaymentResponseDTO> response = payments.stream()
//                 .map(PaymentResponseDTO::fromEntity)
//                 .collect(java.util.stream.Collectors.toList());
//             logger.info("ADMIN {}: Found {} payment records with status: {}", adminUser, payments.size(), status);
//             return ResponseEntity.ok(response);
//         } catch (Exception e) {
//             logger.error("ADMIN {}: Error retrieving payments by status {}: {}", adminUser, status, e.getMessage(), e);
//             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//         }
//     }

//     /**
//      * Get all expired payment records - ADMIN ONLY
//      */
//     @GetMapping("/expired")
//     @PreAuthorize("hasRole('ADMIN')")
//     public ResponseEntity<List<PaymentResponseDTO>> getExpiredPayments() {
//         String adminUser = getCurrentAdminUser();
//         logger.info("ADMIN {}: Retrieving expired payment records", adminUser);
        
//         try {
//             List<PaymentRecord> expiredPayments = paymentRecordService.findExpiredRecords();
//             List<PaymentResponseDTO> response = expiredPayments.stream()
//                 .map(PaymentResponseDTO::fromEntity)
//                 .collect(java.util.stream.Collectors.toList());
//             logger.info("ADMIN {}: Found {} expired payment records", adminUser, expiredPayments.size());
//             return ResponseEntity.ok(response);
//         } catch (Exception e) {
//             logger.error("ADMIN {}: Error retrieving expired payments: {}", adminUser, e.getMessage(), e);
//             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//         }
//     }

//     /**
//      * Mark expired records as EXPIRED - ADMIN ONLY
//      */
//     @GetMapping("/expire-stale")
//     @PreAuthorize("hasRole('ADMIN')")
//     public ResponseEntity<String> markExpiredRecords() {
//         logger.info("ADMIN: Marking expired payment records");
        
//         try {
//             int count = paymentRecordService.markExpiredRecords();
//             String message = "ADMIN: Marked " + count + " payment records as expired";
//             logger.info(message);
//             return ResponseEntity.ok(message);
//         } catch (Exception e) {
//             logger.error("ADMIN: Error marking expired records: {}", e.getMessage(), e);
//             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
//                     .body("ADMIN: Error marking expired records: " + e.getMessage());
//         }
//     }

//     /**
//      * Get payment statistics - ADMIN ONLY
//      */
//     @GetMapping("/statistics")
//     @PreAuthorize("hasRole('ADMIN')")
//     public ResponseEntity<PaymentStatistics> getPaymentStatistics() {
//         logger.info("ADMIN: Retrieving payment statistics");
        
//         try {
//             PaymentStatistics stats = paymentRecordService.getPaymentStatistics();
//             logger.info("ADMIN: Retrieved payment statistics successfully");
//             return ResponseEntity.ok(stats);
//         } catch (Exception e) {
//             logger.error("ADMIN: Error retrieving payment statistics: {}", e.getMessage(), e);
//             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//         }
//     }

//     /**
//      * Health check endpoint for admin - ADMIN ONLY
//      */
//     @GetMapping("/admin/health")
//     @PreAuthorize("hasRole('ADMIN')")
//     public ResponseEntity<String> adminHealthCheck() {
//         logger.info("ADMIN: Health check requested");
//         return ResponseEntity.ok("ADMIN: Payment Records API is healthy and secured");
//     }

//     /**
//      * Get payment statistics - ADMIN ONLY
//      */
//     @GetMapping("/admin/stats")
//     @PreAuthorize("hasRole('ADMIN')")
//     public ResponseEntity<PaymentStatistics> getPaymentStats() {
//         String adminUser = getCurrentAdminUser();
//         logger.info("ADMIN {}: Retrieving payment statistics", adminUser);
        
//         try {
//             PaymentStatistics stats = paymentRecordService.getPaymentStatistics();
//             logger.info("ADMIN: Retrieved enhanced payment statistics successfully");
//             return ResponseEntity.ok(stats);
//         } catch (Exception e) {
//             logger.error("ADMIN: Error retrieving payment statistics: {}", e.getMessage(), e);
//             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//         }
//     }

//     /**
//      * Test admin authentication - ADMIN ONLY  
//      * Use this endpoint to verify JWT authentication is working
//      */
//     @GetMapping("/admin/test")
//     @PreAuthorize("hasRole('ADMIN')")
//     public ResponseEntity<?> testAdminAuth() {
//         String adminUser = getCurrentAdminUser();
//         logger.info("ADMIN {}: Authentication test successful", adminUser);
        
//         return ResponseEntity.ok(Map.of(
//             "message", "Admin authentication successful",
//             "adminUser", adminUser,
//             "timestamp", java.time.LocalDateTime.now(),
//             "status", "authenticated"
//         ));
//     }

//     /**
//      * Get all payment records with pagination - ADMIN ONLY
//      * Perfect for dashboard main table view
//      */
//     @GetMapping("/all")
//     @PreAuthorize("hasRole('ADMIN')")
//     public ResponseEntity<List<PaymentResponseDTO>> getAllPayments() {
//         String adminUser = getCurrentAdminUser();
//         logger.info("ADMIN {}: Retrieving all payment records", adminUser);
        
//         try {
//             List<PaymentRecord> payments = paymentRecordService.findAllPayments();
//             List<PaymentResponseDTO> response = payments.stream()
//                 .map(PaymentResponseDTO::fromEntity)
//                 .collect(java.util.stream.Collectors.toList());
//             logger.info("ADMIN {}: Retrieved {} total payment records", adminUser, payments.size());
//             return ResponseEntity.ok(response);
//         } catch (Exception e) {
//             logger.error("ADMIN {}: Error retrieving all payments: {}", adminUser, e.getMessage(), e);
//             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//         }
//     }

//     /**
//      * Get recent payment records (last 24 hours) - ADMIN ONLY
//      * Perfect for dashboard recent activity widget
//      */
//     @GetMapping("/recent")
//     @PreAuthorize("hasRole('ADMIN')")
//     public ResponseEntity<List<PaymentResponseDTO>> getRecentPayments() {
//         String adminUser = getCurrentAdminUser();
//         logger.info("ADMIN {}: Retrieving recent payment records", adminUser);
        
//         try {
//             List<PaymentRecord> payments = paymentRecordService.findRecentPayments();
//             List<PaymentResponseDTO> response = payments.stream()
//                 .map(PaymentResponseDTO::fromEntity)
//                 .collect(java.util.stream.Collectors.toList());
//             logger.info("ADMIN {}: Retrieved {} recent payment records", adminUser, payments.size());
//             return ResponseEntity.ok(response);
//         } catch (Exception e) {
//             logger.error("ADMIN {}: Error retrieving recent payments: {}", adminUser, e.getMessage(), e);
//             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
//         }
//     }

//     /**
//      * Get completed payments with registration details - ADMIN ONLY
//      * Shows successful payments with associated registration information
//      */
//     @GetMapping("/completed-with-registrations")
//     @PreAuthorize("hasRole('ADMIN')")
//     public ResponseEntity<?> getCompletedPaymentsWithRegistrations() {
//         String adminUser = getCurrentAdminUser();
//         logger.info("ADMIN {}: Retrieving completed payments with registration details", adminUser);
        
//         try {
//             List<PaymentRecord> completedPayments = paymentRecordService.findByStatus(PaymentStatus.COMPLETED);
            
//             List<Map<String, Object>> response = completedPayments.stream()
//                 .map(payment -> {
//                     Map<String, Object> paymentWithRegistration = Map.of(
//                         "paymentId", payment.getId(),
//                         "sessionId", payment.getSessionId(),
//                         "customerEmail", payment.getCustomerEmail(),
//                         "amountTotal", payment.getAmountTotal(),
//                         "currency", payment.getCurrency(),
//                         "paymentStatus", payment.getPaymentStatus(),
//                         "createdAt", payment.getCreatedAt(),
//                         "hasRegistration", payment.getRegistrationForm() != null,
//                         "registrationId", payment.getRegistrationForm() != null ? 
//                             payment.getRegistrationForm().getId() : null
//                     );
//                     return paymentWithRegistration;
//                 })
//                 .collect(java.util.stream.Collectors.toList());
                
//             logger.info("ADMIN {}: Retrieved {} completed payments with registration status", 
//                        adminUser, completedPayments.size());
//             return ResponseEntity.ok(response);
//         } catch (Exception e) {
//             logger.error("ADMIN {}: Error retrieving completed payments with registrations: {}", 
//                         adminUser, e.getMessage(), e);
//             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
//                 "error", "Error retrieving payment and registration data",
//                 "message", e.getMessage()
//             ));
//         }
//     }

//     /**
//      * Get payment record by ID - ADMIN ONLY
//      * For detailed view in dashboard
//      */
//     @GetMapping("/{id}")
//     @PreAuthorize("hasRole('ADMIN')")
//     public ResponseEntity<?> getPaymentById(@PathVariable Long id) {
//         String adminUser = getCurrentAdminUser();
//         logger.info("ADMIN {}: Retrieving payment record by ID: {}", adminUser, id);
        
//         try {
//             Optional<PaymentRecord> payment = paymentRecordService.findById(id);
            
//             if (payment.isPresent()) {
//                 PaymentResponseDTO response = PaymentResponseDTO.fromEntity(payment.get());
//                 logger.info("ADMIN {}: Found payment record ID: {}", adminUser, id);
//                 return ResponseEntity.ok(response);
//             } else {
//                 logger.warn("ADMIN {}: Payment record not found for ID: {}", adminUser, id);
//                 return ResponseEntity.ok().body(Map.of(
//                     "message", "Payment record not found",
//                     "id", id,
//                     "found", false
//                 ));
//             }
//         } catch (Exception e) {
//             logger.error("ADMIN {}: Error retrieving payment ID {}: {}", adminUser, id, e.getMessage(), e);
//             return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
//                 "error", "Error retrieving payment record",
//                 "message", e.getMessage()
//             ));
//         }
//     }
// }
