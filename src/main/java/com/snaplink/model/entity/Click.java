package com.snaplink.model.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "clicks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Click {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url_id", nullable = false)
    private Long urlId;

    @Column(name = "clicked_at")
    private OffsetDateTime clickedAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "referrer", columnDefinition = "TEXT")
    private String referrer;

    @Column(name = "country", length = 2)
    private String country;

    @Column(name = "device_type", length = 20)
    private String deviceType;

    @Column(name = "browser", length = 50)
    private String browser;

    @PrePersist
    protected void onCreate() {
        if (clickedAt == null) {
            clickedAt = OffsetDateTime.now();
        }
    }
}
