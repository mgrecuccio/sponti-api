package com.mgrtech.sponti_api.user.internal.repository;

import com.mgrtech.sponti_api.user.internal.domain.UserPreferenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserPreferenceRepository extends JpaRepository<UserPreferenceEntity, Long> {

    Optional<UserPreferenceEntity> findByUserId(Long userId);

    @Query("""
            select preferences.user.id
            from UserPreferenceEntity preferences
            where preferences.allowChat = true
               or preferences.allowCall = true
            """)
    List<Long> findMatchingEnabledUserIds();
}
