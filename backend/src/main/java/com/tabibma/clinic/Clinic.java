package com.tabibma.clinic;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "clinics")
public class Clinic {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "admin_user_id", nullable = false, unique = true)
    private UUID adminUserId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String city;

    @Column
    private String address;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Clinic() {
        // JPA
    }

    public Clinic(UUID adminUserId, String name, String city, String address) {
        this.adminUserId = adminUserId;
        this.name = name;
        this.city = city;
        this.address = address;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getAdminUserId() {
        return adminUserId;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public String getAddress() {
        return address;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
