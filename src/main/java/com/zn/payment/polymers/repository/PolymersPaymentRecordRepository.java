package com.zn.payment.polymers.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.zn.payment.polymers.entity.PolymersPaymentRecord;
import com.zn.payment.polymers.entity.PolymersPaymentRecord.PaymentStatus;

@Repository
public interface PolymersPaymentRecordRepository extends JpaRepository<PolymersPaymentRecord, Long> {
    
    Optional<PolymersPaymentRecord> findBySessionId(String sessionId);
    
    Optional<PolymersPaymentRecord> findByPaymentIntentId(String paymentIntentId);
    
    List<PolymersPaymentRecord> findByStatus(PaymentStatus status);
    
    List<PolymersPaymentRecord> findByCustomerEmail(String customerEmail);
    
    long countByStatus(PaymentStatus status);
    
    List<PolymersPaymentRecord> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail);
    
    List<PolymersPaymentRecord> findByStatusOrderByCreatedAtDesc(PaymentStatus status);
    
    @Query("SELECT pr FROM PolymersPaymentRecord pr WHERE pr.stripeExpiresAt < :now AND pr.status = 'PENDING'")
    List<PolymersPaymentRecord> findExpiredRecords(@Param("now") LocalDateTime now);
    
    @Query("SELECT SUM(pr.amountTotal) FROM PolymersPaymentRecord pr WHERE pr.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") PaymentStatus status);
    
    // Pricing Config related queries (individual for Nursing)
    List<PolymersPaymentRecord> findByPricingConfigId(Long pricingConfigId);
    
    List<PolymersPaymentRecord> findByPricingConfigIdAndStatus(Long pricingConfigId, PaymentStatus status);
    
    long countByPricingConfigIdAndStatus(Long pricingConfigId, PaymentStatus status);
    
    @Query("SELECT COUNT(pr) FROM PolymersPaymentRecord pr WHERE pr.pricingConfig.id = :pricingConfigId AND pr.status = 'COMPLETED'")
    long countSuccessfulPaymentsByPricingConfig(@Param("pricingConfigId") Long pricingConfigId);
    
    @Query("SELECT pr FROM PolymersPaymentRecord pr WHERE pr.amountTotal != pr.pricingConfig.totalPrice")
    List<PolymersPaymentRecord> findPaymentsWithAmountMismatch();
    
    // Additional methods for admin dashboard
    List<PolymersPaymentRecord> findAllByOrderByCreatedAtDesc();
    
    List<PolymersPaymentRecord> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime createdAt);
    
    // Database sync function - synchronizes discount records with payment records
    @Query(value = "SELECT sync_polymers_by_session_id(:sessionId)", nativeQuery = true)
    String syncPolymersBySessionId(@Param("sessionId") String sessionId);

}
