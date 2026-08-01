package com.emme.identity.adapter.out.persistence.entity;

import com.emme.identity.domain.model.SocialProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** JPA representation of the Identity-owned customer identity aggregate. */
@Entity
@Table(name = "customer_identity", schema = "emme_core")
public class CustomerIdentityEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false, length = 255)
  private String email;

  @Column(nullable = false, length = 255)
  private String name;

  @Column(length = 20)
  private String phone;

  @Column(nullable = false, length = 20)
  @Enumerated(EnumType.STRING)
  private SocialProvider provider;

  @Column(name = "provider_id", nullable = false, length = 255)
  private String providerId;

  @Column(name = "avatar_url", length = 500)
  private String avatarUrl;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt = Instant.now();

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt = Instant.now();

  public CustomerIdentityEntity() {}

  public CustomerIdentityEntity(
      UUID id,
      String email,
      String name,
      String phone,
      SocialProvider provider,
      String providerId,
      String avatarUrl,
      Instant createdAt,
      Instant updatedAt) {
    this.id = id;
    this.email = email;
    this.name = name;
    this.phone = phone;
    this.provider = provider;
    this.providerId = providerId;
    this.avatarUrl = avatarUrl;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public UUID getId() {
    return id;
  }

  public String getEmail() {
    return email;
  }

  public String getName() {
    return name;
  }

  public String getPhone() {
    return phone;
  }

  public SocialProvider getProvider() {
    return provider;
  }

  public String getProviderId() {
    return providerId;
  }

  public String getAvatarUrl() {
    return avatarUrl;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
