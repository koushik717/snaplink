package com.snaplink.model.entity;

import com.snaplink.model.enums.ApiKeyTier;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "api_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "key_hash", nullable = false, unique = true, length = 64)
    private String keyHash;

    @Column(name = "name", length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "tier", length = 20)
    @Builder.Default
    private ApiKeyTier tier = ApiKeyTier.FREE;

    @Column(name = "rate_limit")
    @Builder.Default
    private Integer rateLimit = 100;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
