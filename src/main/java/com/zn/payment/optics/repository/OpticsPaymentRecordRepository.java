package com.zn.payment.optics.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.zn.payment.optics.entity.OpticsPaymentRecord;
import com.zn.payment.optics.entity.OpticsPaymentRecord.PaymentStatus;

@Repository
public interface OpticsPaymentRecordRepository extends JpaRepository<OpticsPaymentRecord, Long> {
    
    Optional<OpticsPaymentRecord> findBySessionId(String sessionId);
    
    Optional<OpticsPaymentRecord> findByPaymentIntentId(String paymentIntentId);
    
    List<OpticsPaymentRecord> findByStatus(PaymentStatus status);
    
    List<OpticsPaymentRecord> findByCustomerEmail(String customerEmail);
    
    long countByStatus(PaymentStatus status);
    
    List<OpticsPaymentRecord> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail);
    
    List<OpticsPaymentRecord> findByStatusOrderByCreatedAtDesc(PaymentStatus status);
    
    @Query("SELECT pr FROM OpticsPaymentRecord pr WHERE pr.stripeExpiresAt < :now AND pr.status = 'PENDING'")
    List<OpticsPaymentRecord> findExpiredRecords(@Param("now") LocalDateTime now);
    
    @Query("SELECT SUM(pr.amountTotal) FROM OpticsPaymentRecord pr WHERE pr.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") PaymentStatus status);
    
    // Pricing Config related queries (individual for Optics)
    List<OpticsPaymentRecord> findByPricingConfigId(Long pricingConfigId);
    
    List<OpticsPaymentRecord> findByPricingConfigIdAndStatus(Long pricingConfigId, PaymentStatus status);
    
    long countByPricingConfigIdAndStatus(Long pricingConfigId, PaymentStatus status);
    
    @Query("SELECT COUNT(pr) FROM OpticsPaymentRecord pr WHERE pr.pricingConfig.id = :pricingConfigId AND pr.status = 'COMPLETED'")
    long countSuccessfulPaymentsByPricingConfig(@Param("pricingConfigId") Long pricingConfigId);
    
    @Query("SELECT pr FROM OpticsPaymentRecord pr WHERE pr.amountTotal != pr.pricingConfig.totalPrice")
    List<OpticsPaymentRecord> findPaymentsWithAmountMismatch();
    
    // Additional methods for admin dashboard
    List<OpticsPaymentRecord> findAllByOrderByCreatedAtDesc();
    
    List<OpticsPaymentRecord> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime createdAt);
    
    // Database sync function - synchronizes discount records with payment records
    @Query(value = "SELECT sync_optics_by_session_id(:sessionId)", nativeQuery = true)
    String syncOpticsBySessionId(@Param("sessionId") String sessionId);
}
