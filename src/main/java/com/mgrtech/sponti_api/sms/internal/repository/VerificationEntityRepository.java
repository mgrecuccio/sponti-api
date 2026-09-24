package com.mgrtech.sponti_api.sms.internal.repository;

import com.mgrtech.sponti_api.sms.internal.domain.VerificationEntity;
import com.mgrtech.sponti_api.sms.internal.domain.VerificationPurpose;
import com.mgrtech.sponti_api.sms.internal.domain.VerificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface VerificationEntityRepository extends JpaRepository<VerificationEntity, UUID> {

    boolean existsByPhoneNumberAndPurposeAndCreatedAtAfter(
            String phoneNumber,
            VerificationPurpose purpose,
            Instant createdAt
    );

    long countByUserIdAndPurposeAndCreatedAtAfter(
            Long userId,
            VerificationPurpose purpose,
            Instant createdAt
    );

    long countByClientIpAndPurposeAndCreatedAtAfter(
            String clientIp,
            VerificationPurpose purpose,
            Instant createdAt
    );

    Optional<VerificationEntity> findFirstByPhoneNumberAndPurposeAndStatusOrderByCreatedAtDesc(
            String phoneNumber,
            VerificationPurpose purpose,
            VerificationStatus status
    );
}
