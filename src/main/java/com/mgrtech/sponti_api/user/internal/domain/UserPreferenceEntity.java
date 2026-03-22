package com.mgrtech.sponti_api.user.internal.domain;

import jakarta.persistence.*;

import java.time.LocalTime;

@Entity
@Table(name = "user_preferences")
public class UserPreferenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity user;

    @Column(name = "allow_chat")
    private boolean allowChat = true;

    @Column(name = "allow_call")
    private boolean allowCall = true;

    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart;

    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd;
}
