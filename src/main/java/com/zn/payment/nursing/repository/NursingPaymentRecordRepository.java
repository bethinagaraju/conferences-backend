package com.zn.payment.nursing.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.zn.payment.nursing.entity.NursingPaymentRecord;
import com.zn.payment.nursing.entity.NursingPaymentRecord.PaymentStatus;

@Repository
public interface NursingPaymentRecordRepository extends JpaRepository<NursingPaymentRecord, Long> {
    
    Optional<NursingPaymentRecord> findBySessionId(String sessionId);
    
    Optional<NursingPaymentRecord> findByPaymentIntentId(String paymentIntentId);
    
    List<NursingPaymentRecord> findByStatus(PaymentStatus status);
    
    List<NursingPaymentRecord> findByCustomerEmail(String customerEmail);
    
    long countByStatus(PaymentStatus status);
    
    List<NursingPaymentRecord> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail);
    
    List<NursingPaymentRecord> findByStatusOrderByCreatedAtDesc(PaymentStatus status);
    
    @Query("SELECT pr FROM NursingPaymentRecord pr WHERE pr.stripeExpiresAt < :now AND pr.status = 'PENDING'")
    List<NursingPaymentRecord> findExpiredRecords(@Param("now") LocalDateTime now);
    
    @Query("SELECT SUM(pr.amountTotal) FROM NursingPaymentRecord pr WHERE pr.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") PaymentStatus status);
    
    // Pricing Config related queries (individual for Nursing)
    List<NursingPaymentRecord> findByPricingConfigId(Long pricingConfigId);
    
    List<NursingPaymentRecord> findByPricingConfigIdAndStatus(Long pricingConfigId, PaymentStatus status);
    
    long countByPricingConfigIdAndStatus(Long pricingConfigId, PaymentStatus status);
    
    @Query("SELECT COUNT(pr) FROM NursingPaymentRecord pr WHERE pr.pricingConfig.id = :pricingConfigId AND pr.status = 'COMPLETED'")
    long countSuccessfulPaymentsByPricingConfig(@Param("pricingConfigId") Long pricingConfigId);
    
    @Query("SELECT pr FROM NursingPaymentRecord pr WHERE pr.amountTotal != pr.pricingConfig.totalPrice")
    List<NursingPaymentRecord> findPaymentsWithAmountMismatch();
    
    // Additional methods for admin dashboard
    List<NursingPaymentRecord> findAllByOrderByCreatedAtDesc();
    
    List<NursingPaymentRecord> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime createdAt);
    
    // Database sync function - synchronizes discount records with payment records
    @Query(value = "SELECT sync_nursing_by_session_id(:sessionId)", nativeQuery = true)
    String syncNursingBySessionId(@Param("sessionId") String sessionId);
}
