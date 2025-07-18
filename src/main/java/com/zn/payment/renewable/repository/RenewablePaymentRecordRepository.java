package com.zn.payment.renewable.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.zn.payment.renewable.entity.RenewablePaymentRecord;
import com.zn.payment.renewable.entity.RenewablePaymentRecord.PaymentStatus;

@Repository
public interface RenewablePaymentRecordRepository extends JpaRepository<RenewablePaymentRecord, Long> {
    
    Optional<RenewablePaymentRecord> findBySessionId(String sessionId);
    
    Optional<RenewablePaymentRecord> findByPaymentIntentId(String paymentIntentId);
    
    List<RenewablePaymentRecord> findByStatus(PaymentStatus status);
    
    List<RenewablePaymentRecord> findByCustomerEmail(String customerEmail);
    
    long countByStatus(PaymentStatus status);
    
    List<RenewablePaymentRecord> findByCustomerEmailOrderByCreatedAtDesc(String customerEmail);
    
    List<RenewablePaymentRecord> findByStatusOrderByCreatedAtDesc(PaymentStatus status);
    
    @Query("SELECT pr FROM RenewablePaymentRecord pr WHERE pr.stripeExpiresAt < :now AND pr.status = 'PENDING'")
    List<RenewablePaymentRecord> findExpiredRecords(@Param("now") LocalDateTime now);
    
    @Query("SELECT SUM(pr.amountTotal) FROM RenewablePaymentRecord pr WHERE pr.status = :status")
    BigDecimal sumAmountByStatus(@Param("status") PaymentStatus status);
    
    // Pricing Config related queries (individual for Renewable)
    List<RenewablePaymentRecord> findByPricingConfigId(Long pricingConfigId);
    
    List<RenewablePaymentRecord> findByPricingConfigIdAndStatus(Long pricingConfigId, PaymentStatus status);
    
    long countByPricingConfigIdAndStatus(Long pricingConfigId, PaymentStatus status);
    
    @Query("SELECT COUNT(pr) FROM RenewablePaymentRecord pr WHERE pr.pricingConfig.id = :pricingConfigId AND pr.status = 'COMPLETED'")
    long countSuccessfulPaymentsByPricingConfig(@Param("pricingConfigId") Long pricingConfigId);
    
    @Query("SELECT pr FROM RenewablePaymentRecord pr WHERE pr.amountTotal != pr.pricingConfig.totalPrice")
    List<RenewablePaymentRecord> findPaymentsWithAmountMismatch();
    
    // Additional methods for admin dashboard
    List<RenewablePaymentRecord> findAllByOrderByCreatedAtDesc();
    
    List<RenewablePaymentRecord> findByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime createdAt);
    
    // Database sync function - synchronizes discount records with payment records
    @Query(value = "SELECT sync_renewable_by_session_id(:sessionId)", nativeQuery = true)
    String syncRenewableBySessionId(@Param("sessionId") String sessionId);
}
