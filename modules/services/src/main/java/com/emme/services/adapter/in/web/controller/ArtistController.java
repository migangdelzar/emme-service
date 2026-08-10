package com.emme.services.adapter.in.web.controller;

import static com.emme.kernel.context.TenantContextHolder.withCurrentTenant;

import com.emme.services.adapter.in.web.request.AddArtistCapabilityRequest;
import com.emme.services.adapter.in.web.request.CreateArtistRequest;
import com.emme.services.adapter.in.web.request.UpdateArtistRequest;
import com.emme.services.adapter.in.web.response.ArtistCapabilityResponse;
import com.emme.services.adapter.in.web.response.ArtistResponse;
import com.emme.services.api.result.ArtistCapabilityDetails;
import com.emme.services.api.result.ArtistDetails;
import com.emme.services.api.usecase.AddArtistCapabilityUseCase;
import com.emme.services.api.usecase.CreateArtistUseCase;
import com.emme.services.api.usecase.DeactivateArtistUseCase;
import com.emme.services.api.usecase.GetArtistUseCase;
import com.emme.services.api.usecase.ListTenantArtistsUseCase;
import com.emme.services.api.usecase.RemoveArtistCapabilityUseCase;
import com.emme.services.api.usecase.UpdateArtistUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/artists", version = "1.0")
@Tag(name = "Artists")
public class ArtistController {

  private final ListTenantArtistsUseCase listArtists;
  private final CreateArtistUseCase createArtist;
  private final GetArtistUseCase getArtist;
  private final UpdateArtistUseCase updateArtist;
  private final DeactivateArtistUseCase deactivateArtist;
  private final AddArtistCapabilityUseCase addArtistCapability;
  private final RemoveArtistCapabilityUseCase removeArtistCapability;

  public ArtistController(
      ListTenantArtistsUseCase listArtists,
      CreateArtistUseCase createArtist,
      GetArtistUseCase getArtist,
      UpdateArtistUseCase updateArtist,
      DeactivateArtistUseCase deactivateArtist,
      AddArtistCapabilityUseCase addArtistCapability,
      RemoveArtistCapabilityUseCase removeArtistCapability) {
    this.listArtists = listArtists;
    this.createArtist = createArtist;
    this.getArtist = getArtist;
    this.updateArtist = updateArtist;
    this.deactivateArtist = deactivateArtist;
    this.addArtistCapability = addArtistCapability;
    this.removeArtistCapability = removeArtistCapability;
  }

  @GetMapping
  @Operation(summary = "List artists for current tenant")
  public ResponseEntity<List<ArtistResponse>> list() {
    return withCurrentTenant(
        tenantId ->
            ResponseEntity.ok(
                listArtists.list(tenantId).stream().map(ArtistResponse::from).toList()));
  }

  @PostMapping
  @Operation(summary = "Create an artist")
  public ResponseEntity<ArtistResponse> create(@RequestBody CreateArtistRequest request) {
    return withCurrentTenant(
        tenantId -> {
          ArtistDetails artist = createArtist.create(tenantId, request.name());
          var location = URI.create("/api/artists/" + artist.id());
          return ResponseEntity.created(location).body(ArtistResponse.from(artist));
        });
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get artist by ID")
  public ResponseEntity<ArtistResponse> get(@PathVariable UUID id) {
    return getArtist
        .get(id)
        .map(a -> ResponseEntity.ok(ArtistResponse.from(a)))
        .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update an artist")
  public ResponseEntity<ArtistResponse> update(
      @PathVariable UUID id, @RequestBody UpdateArtistRequest request) {
    ArtistDetails artist = updateArtist.update(id, request.name());
    return ResponseEntity.ok(ArtistResponse.from(artist));
  }

  @PostMapping("/{id}/deactivate")
  @Operation(summary = "Deactivate an artist")
  public ResponseEntity<ArtistResponse> deactivate(@PathVariable UUID id) {
    ArtistDetails artist = deactivateArtist.deactivate(id);
    return ResponseEntity.ok(ArtistResponse.from(artist));
  }

  @PostMapping("/{id}/capabilities")
  @Operation(summary = "Add a capability to an artist")
  public ResponseEntity<ArtistCapabilityResponse> addCapability(
      @PathVariable UUID id, @RequestBody AddArtistCapabilityRequest request) {
    return withCurrentTenant(
        tenantId -> {
          ArtistCapabilityDetails capability =
              addArtistCapability.add(id, request.serviceId(), tenantId);
          return ResponseEntity.ok(ArtistCapabilityResponse.from(capability));
        });
  }

  @DeleteMapping("/capabilities/{id}")
  @Operation(summary = "Remove a capability")
  public ResponseEntity<ArtistCapabilityResponse> removeCapability(@PathVariable UUID id) {
    ArtistCapabilityDetails capability = removeArtistCapability.remove(id);
    return ResponseEntity.ok(ArtistCapabilityResponse.from(capability));
  }
}
