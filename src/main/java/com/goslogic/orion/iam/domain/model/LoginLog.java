package com.goslogic.orion.iam.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "login_logs")
@Getter
@Setter
@NoArgsConstructor
public class LoginLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "tenant_id")
    private Tenant tenant;

    @Column(name = "login_at", nullable = false)
    private LocalDateTime loginAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LoginStatus status;

    @PrePersist
    void prePersist() {
        if (loginAt == null) loginAt = LocalDateTime.now();
    }

    public LoginLog(User user, Tenant tenant, String ipAddress, String userAgent, LoginStatus status) {
        this.user = user;
        this.tenant = tenant;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.status = status;
    }
}
