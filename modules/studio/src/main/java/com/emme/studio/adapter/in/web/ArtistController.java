package com.emme.studio.adapter.in.web;

import static com.emme.kernel.context.TenantContextHolder.withCurrentTenant;

import com.emme.studio.application.service.ArtistService;
import com.emme.studio.entity.Artist;
import com.emme.studio.entity.ArtistCapability;
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
@RequestMapping("/api/v1/artists")
@Tag(name = "Artists")
public class ArtistController {

  private final ArtistService artistService;

  public ArtistController(ArtistService artistService) {
    this.artistService = artistService;
  }

  @GetMapping
  @Operation(summary = "List artists for current tenant")
  public ResponseEntity<List<ArtistResponse>> list() {
    return withCurrentTenant(
        tenantId ->
            ResponseEntity.ok(
                artistService.findByTenantId(tenantId).stream()
                    .map(ArtistResponse::from)
                    .toList()));
  }

  @PostMapping
  @Operation(summary = "Create an artist")
  public ResponseEntity<ArtistResponse> create(@RequestBody CreateArtistRequest request) {
    return withCurrentTenant(
        tenantId -> {
          Artist artist = artistService.create(tenantId, request.name());
          var location = URI.create("/api/v1/artists/" + artist.getId());
          return ResponseEntity.created(location).body(ArtistResponse.from(artist));
        });
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get artist by ID")
  public ResponseEntity<ArtistResponse> get(@PathVariable UUID id) {
    return artistService
        .findById(id)
        .map(a -> ResponseEntity.ok(ArtistResponse.from(a)))
        .orElse(ResponseEntity.notFound().build());
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update an artist")
  public ResponseEntity<ArtistResponse> update(
      @PathVariable UUID id, @RequestBody UpdateArtistRequest request) {
    Artist artist = artistService.update(id, request.name());
    return ResponseEntity.ok(ArtistResponse.from(artist));
  }

  @PostMapping("/{id}/deactivate")
  @Operation(summary = "Deactivate an artist")
  public ResponseEntity<ArtistResponse> deactivate(@PathVariable UUID id) {
    Artist artist = artistService.deactivate(id);
    return ResponseEntity.ok(ArtistResponse.from(artist));
  }

  @PostMapping("/{id}/capabilities")
  @Operation(summary = "Add a capability to an artist")
  public ResponseEntity<ArtistCapabilityResponse> addCapability(
      @PathVariable UUID id, @RequestBody AddCapabilityRequest request) {
    return withCurrentTenant(
        tenantId -> {
          ArtistCapability capability =
              artistService.addCapability(id, request.serviceId(), tenantId);
          return ResponseEntity.ok(ArtistCapabilityResponse.from(capability));
        });
  }

  @DeleteMapping("/capabilities/{id}")
  @Operation(summary = "Remove a capability")
  public ResponseEntity<ArtistCapabilityResponse> removeCapability(@PathVariable UUID id) {
    ArtistCapability capability = artistService.removeCapability(id);
    return ResponseEntity.ok(ArtistCapabilityResponse.from(capability));
  }

  // --- DTOs ---

  public record ArtistResponse(UUID id, String name, String status) {
    public static ArtistResponse from(Artist a) {
      return new ArtistResponse(a.getId(), a.getName(), a.getStatus().name());
    }
  }

  public record ArtistCapabilityResponse(
      UUID id,
      UUID artistId,
      String artistName,
      UUID serviceId,
      String serviceName,
      boolean active) {
    public static ArtistCapabilityResponse from(ArtistCapability ac) {
      return new ArtistCapabilityResponse(
          ac.getId(),
          ac.getArtist().getId(),
          ac.getArtist().getName(),
          ac.getService().getId(),
          ac.getService().getName(),
          ac.isActive());
    }
  }

  public record CreateArtistRequest(String name) {}

  public record UpdateArtistRequest(String name) {}

  public record AddCapabilityRequest(UUID serviceId) {}
}
