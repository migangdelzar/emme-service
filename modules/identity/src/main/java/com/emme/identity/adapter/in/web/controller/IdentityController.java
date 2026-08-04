package com.emme.identity.adapter.in.web.controller;

import com.emme.identity.adapter.in.web.mapper.IdentityWebMapper;
import com.emme.identity.adapter.in.web.request.AssignMembershipRequest;
import com.emme.identity.adapter.in.web.response.MembershipResponse;
import com.emme.identity.adapter.in.web.security.UserContextHolder;
import com.emme.identity.api.command.AssignMembershipCommand;
import com.emme.identity.api.command.RevokeMembershipCommand;
import com.emme.identity.api.query.GetCurrentUserMembershipsQuery;
import com.emme.identity.api.result.MembershipDetails;
import com.emme.identity.api.usecase.AssignMembershipUseCase;
import com.emme.identity.api.usecase.GetCurrentUserMembershipsUseCase;
import com.emme.identity.api.usecase.GetUserPermissionsUseCase;
import com.emme.identity.api.usecase.RevokeMembershipUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/identity", version = "1.0")
public class IdentityController {

  private final AssignMembershipUseCase assignMembership;
  private final GetCurrentUserMembershipsUseCase currentMemberships;
  private final GetUserPermissionsUseCase permissions;
  private final RevokeMembershipUseCase revokeMembership;

  public IdentityController(
      AssignMembershipUseCase assignMembership,
      GetCurrentUserMembershipsUseCase currentMemberships,
      GetUserPermissionsUseCase permissions,
      RevokeMembershipUseCase revokeMembership) {
    this.assignMembership = assignMembership;
    this.currentMemberships = currentMemberships;
    this.permissions = permissions;
    this.revokeMembership = revokeMembership;
  }

  @GetMapping("/me")
  @Operation(summary = "Get current user memberships")
  public ResponseEntity<List<MembershipResponse>> currentMemberships() {
    return UserContextHolder.withCurrentUser(
        user -> {
          List<MembershipResponse> members =
              currentMemberships
                  .getMemberships(new GetCurrentUserMembershipsQuery(user.subject()))
                  .stream()
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
        user -> ResponseEntity.ok(permissions.getPermissions(user.subject(), tenantId)));
  }

  @PostMapping("/memberships")
  @PreAuthorize("hasAnyRole('platform_admin', 'tenant_owner')")
  @Operation(
      summary = "Assign a membership to a user",
      tags = {"Identity"})
  @Tag(name = "Identity")
  public ResponseEntity<MembershipResponse> assignMembership(
      @Valid @RequestBody AssignMembershipRequest request) {
    return UserContextHolder.withCurrentUser(
        user -> {
          UserContextHolder.requireTenantAccess(request.tenantId());
          MembershipDetails membership =
              assignMembership.assign(
                  new AssignMembershipCommand(
                      request.tenantId(), request.roleId(), request.userReference()));
          URI location = URI.create("/api/identity/memberships/" + membership.id());
          return ResponseEntity.created(location)
              .body(IdentityWebMapper.toMembershipResponse(membership));
        });
  }

  @DeleteMapping("/memberships/{id}")
  @PreAuthorize("hasAnyRole('platform_admin', 'tenant_owner')")
  @Operation(
      summary = "Revoke a membership",
      tags = {"Identity"})
  public ResponseEntity<MembershipResponse> revokeMembership(@PathVariable UUID id) {
    return UserContextHolder.withCurrentUser(
        user -> {
          UserContextHolder.requireTenantAccess(user.tenantId());
          MembershipDetails membership =
              revokeMembership.revoke(new RevokeMembershipCommand(id, user.tenantId()));
          return ResponseEntity.ok(IdentityWebMapper.toMembershipResponse(membership));
        });
  }
}
