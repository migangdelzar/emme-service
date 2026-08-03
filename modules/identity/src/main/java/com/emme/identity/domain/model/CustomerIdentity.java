package com.emme.identity.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Framework-free customer identity aggregate owned by Identity. */
public final class CustomerIdentity {

  private final UUID id;
  private final String email;
  private String name;
  private String phone;
  private final SocialProvider provider;
  private final String providerId;
  private String avatarUrl;
  private final Instant createdAt;
  private Instant updatedAt;

  private CustomerIdentity(
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
    this.provider = Objects.requireNonNull(provider, "provider must not be null");
    this.providerId = Objects.requireNonNull(providerId, "providerId must not be null");
    this.avatarUrl = avatarUrl;
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
  }

  public static CustomerIdentity create(
      String email, String name, SocialProvider provider, String providerId, String avatarUrl) {
    Instant now = Instant.now();
    return new CustomerIdentity(null, email, name, null, provider, providerId, avatarUrl, now, now);
  }

  public static CustomerIdentity rehydrate(
      UUID id,
      String email,
      String name,
      String phone,
      SocialProvider provider,
      String providerId,
      String avatarUrl,
      Instant createdAt,
      Instant updatedAt) {
    return new CustomerIdentity(
        Objects.requireNonNull(id, "id must not be null"),
        email,
        name,
        phone,
        provider,
        providerId,
        avatarUrl,
        createdAt,
        updatedAt);
  }

  public UUID id() {
    return id;
  }

  public String email() {
    return email;
  }

  public String name() {
    return name;
  }

  public String phone() {
    return phone;
  }

  public SocialProvider provider() {
    return provider;
  }

  public String providerId() {
    return providerId;
  }

  public String avatarUrl() {
    return avatarUrl;
  }

  public Instant createdAt() {
    return createdAt;
  }

  public Instant updatedAt() {
    return updatedAt;
  }

  public boolean updateProfile(String name, String avatarUrl) {
    boolean changed = false;
    if (name != null && !name.equals(this.name)) {
      this.name = name;
      changed = true;
    }
    if (avatarUrl != null && !avatarUrl.equals(this.avatarUrl)) {
      this.avatarUrl = avatarUrl;
      changed = true;
    }
    if (changed) {
      this.updatedAt = Instant.now();
    }
    return changed;
  }

  public void updatePhone(String phone) {
    this.phone = Objects.requireNonNull(phone, "phone must not be null");
    this.updatedAt = Instant.now();
  }

  public boolean needsPhone() {
    return phone == null || phone.isBlank();
  }
}
