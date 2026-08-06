package com.apiprecios.entity;


import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "scrapings_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScrapingLog {

    public enum Level {
        INFO, WARN, ERROR, DEBUG
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default
    private Level level = Level.INFO;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scraping_job_id", nullable = false)
    private ScrapingJob scrapingJob;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
