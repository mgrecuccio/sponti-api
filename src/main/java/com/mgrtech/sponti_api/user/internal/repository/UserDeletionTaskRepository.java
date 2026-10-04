package com.mgrtech.sponti_api.user.internal.repository;

import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionModule;
import com.mgrtech.sponti_api.shared.api.deletion.UserDeletionTaskStatus;
import com.mgrtech.sponti_api.user.internal.domain.UserDeletionTaskEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface UserDeletionTaskRepository extends JpaRepository<UserDeletionTaskEntity, Long> {

    @Query("""
        select count(task) = 0
        from UserDeletionTaskEntity task
        where task.user.id = :userId
          and task.status <> :completedStatus
        """)
    boolean allCompletedForUser(Long userId, UserDeletionTaskStatus completedStatus);

    Optional<UserDeletionTaskEntity> findByUser_IdAndModule(Long userId, UserDeletionModule module);

    boolean existsByUser_IdAndModule(Long userId, UserDeletionModule module);
}
