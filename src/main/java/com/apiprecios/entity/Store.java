package com.apiprecios.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "stores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;


    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "base_url", length = 500)
    private String baseUrl;

    /**
     * Backward-compatible alias used by older services/tests. No extra DB column:
     * both url and baseUrl refer to the same storage field.
     */
    @Deprecated
    @com.fasterxml.jackson.annotation.JsonIgnore
    public String getUrl() { return baseUrl; }

    @Deprecated
    public void setUrl(String url) { this.baseUrl = url; }

    public static class StoreBuilder {
        @Deprecated
        public StoreBuilder url(String url) {
            this.baseUrl = url;
            return this;
        }
    }


    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(length = 300)
    private String address;

    private Double latitude;

    private Double longitude;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonIgnore
    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Product> products;

    @JsonIgnore
    @OneToMany(mappedBy = "store", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ScrapingJob> scrapingJobs;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
