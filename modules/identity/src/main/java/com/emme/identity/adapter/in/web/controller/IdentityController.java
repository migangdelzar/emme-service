package com.emme.identity.adapter.in.web.controller;

import com.emme.identity.UserContextHolder;
import com.emme.identity.adapter.in.web.mapper.IdentityWebMapper;
import com.emme.identity.adapter.in.web.request.AssignMembershipRequest;
import com.emme.identity.adapter.in.web.response.MembershipResponse;
import com.emme.identity.adapter.out.persistence.entity.Membership;
import com.emme.identity.application.IdentityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/identity")
public class IdentityController {

  private final IdentityService service;

  public IdentityController(IdentityService service) {
    this.service = service;
  }

  @GetMapping("/me")
  @Operation(summary = "Get current user memberships")
  public ResponseEntity<List<MembershipResponse>> currentMemberships() {
    return UserContextHolder.withCurrentUser(
        user -> {
          List<MembershipResponse> members =
              service.getCurrentUserMemberships(user.subject()).stream()
                  .map(IdentityWebMapper::toMembershipResponse)
                  .toList();
          return ResponseEntity.ok(members);
        });
  }

  @GetMapping("/me/permissions")
  @Operation(summary = "Get current user permissions for a tenant")
  public ResponseEntity<Set<String>> currentPermissions(
      @NotNull @org.springframework.web.bind.annotation.RequestParam UUID tenantId) {
    return UserContextHolder.withCurrentUser(
        user -> ResponseEntity.ok(service.getPermissionsForUser(user.subject(), tenantId)));
  }

  @PostMapping("/memberships")
  @Operation(
      summary = "Assign a membership to a user",
      tags = {"Identity"})
  @Tag(name = "Identity")
  public ResponseEntity<MembershipResponse> assignMembership(
      @Valid @RequestBody AssignMembershipRequest request) {
    Membership m =
        service.assignMembership(request.tenantId(), request.roleId(), request.userReference());
    URI location = URI.create("/api/v1/identity/memberships/" + m.getId());
    return ResponseEntity.created(location).body(IdentityWebMapper.toMembershipResponse(m));
  }

  @DeleteMapping("/memberships/{id}")
  @Operation(
      summary = "Revoke a membership",
      tags = {"Identity"})
  public ResponseEntity<MembershipResponse> revokeMembership(@PathVariable UUID id) {
    Membership m = service.revokeMembership(id);
    return ResponseEntity.ok(IdentityWebMapper.toMembershipResponse(m));
  }
}
