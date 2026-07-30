package com.emme.client;

import java.util.UUID;

/**
 * Pre-created E2E test users in the Keycloak realm.
 * Each entry maps to a real user in infra/keycloak/emme-realm.json.
 * Used by E2eUserPool to pre-login at class-init.
 * Naming: {role}@{tenant-slug}
 */
public enum PoolUser {
    ADMIN("admin", "admin123", Role.PLATFORM_ADMIN,
        UUID.fromString("25a79af6-efa0-4f04-b34b-b2d2fed997c4"), "admin@emme.app"),

    OWNER_DEMO("owner@demo-salon", "owner123", Role.BUSINESS_OWNER,
        UUID.fromString("00000000-0000-0000-0000-100000000000"), "owner@demo-salon.emme.app"),

    MANAGER_DEMO("manager@demo-salon", "manager123", Role.BUSINESS_MANAGER,
        UUID.fromString("00000000-0000-0000-0000-100000000000"), "manager@demo-salon.emme.app"),

    FRONT_DESK_DEMO("front-desk@demo-salon", "desk123", Role.FRONT_DESK,
        UUID.fromString("00000000-0000-0000-0000-100000000000"), "desk@demo-salon.emme.app"),

    NAIL_ARTIST_DEMO("nail-artist@demo-salon", "artist123", Role.NAIL_ARTIST,
        UUID.fromString("00000000-0000-0000-0000-100000000000"), "artist@demo-salon.emme.app"),

    ACCOUNTANT_DEMO("accountant@demo-salon", "account123", Role.ACCOUNTANT,
        UUID.fromString("00000000-0000-0000-0000-100000000000"), "account@demo-salon.emme.app"),

    READER_DEMO("reader@demo-salon", "reader123", Role.READ_ONLY,
        UUID.fromString("00000000-0000-0000-0000-100000000000"), "reader@demo-salon.emme.app"),

    OWNER_STUDIO_A("owner@studio-a", "sa-owner123", Role.BUSINESS_OWNER,
        UUID.fromString("25a79af6-efa0-4f04-b34b-b2d2fed997c4"), "sa-owner@studio-a.emme.app"),

    FRONT_DESK_STUDIO_A("front-desk@studio-a", "sa-desk123", Role.FRONT_DESK,
        UUID.fromString("25a79af6-efa0-4f04-b34b-b2d2fed997c4"), "sa-desk@studio-a.emme.app"),

    NAIL_ARTIST_STUDIO_A("nail-artist@studio-a", "sa-artist123", Role.NAIL_ARTIST,
        UUID.fromString("25a79af6-efa0-4f04-b34b-b2d2fed997c4"), "sa-artist@studio-a.emme.app");

    final String username;
    final String password;
    final Role role;
    final UUID tenantId;
    final String email;

    PoolUser(String username, String password, Role role, UUID tenantId, String email) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.tenantId = tenantId;
        this.email = email;
    }
}
