package com.emme.e2eprovisioner;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Writes local-only Playwright inputs for one provisioned tenant. */
final class ProvisionedAuthArtifactWriter {

  private static final Set<PosixFilePermission> OWNER_ONLY_PERMISSIONS =
      Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);

  private final ObjectMapper objectMapper;
  private final Path outputDirectory;

  ProvisionedAuthArtifactWriter(ObjectMapper objectMapper, Path outputDirectory) {
    this.objectMapper = objectMapper;
    this.outputDirectory = outputDirectory.toAbsolutePath().normalize();
  }

  Path write(String tenantSlug, Map<String, Credentials> users) throws IOException {
    if (tenantSlug == null || !tenantSlug.matches("[a-z0-9]+(?:-[a-z0-9]+)*")) {
      throw new IllegalArgumentException("Invalid tenant slug for auth artifact: " + tenantSlug);
    }
    Files.createDirectories(outputDirectory);
    var artifact = outputDirectory.resolve(tenantSlug + ".json").normalize();
    if (!artifact.startsWith(outputDirectory)) {
      throw new IllegalArgumentException("Auth artifact path escaped output directory");
    }

    var root = objectMapper.createObjectNode();
    root.put("version", 1);
    root.put("tenantSlug", tenantSlug);
    var userNodes = root.putObject("users");
    for (var entry : new TreeMap<>(users).entrySet()) {
      var user = entry.getValue();
      var userNode = userNodes.putObject(entry.getKey());
      userNode
          .putObject("credentials")
          .put("username", user.username())
          .put("password", user.password());
      var storageState = userNode.putObject("storageState");
      storageState.putArray("cookies");
      storageState.putArray("origins");
    }

    Files.writeString(
        artifact, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root));
    setOwnerOnlyPermissions(artifact);
    return artifact;
  }

  private static void setOwnerOnlyPermissions(Path artifact) {
    try {
      Files.setPosixFilePermissions(artifact, OWNER_ONLY_PERMISSIONS);
    } catch (UnsupportedOperationException ignored) {
      // Windows and other non-POSIX filesystems enforce permissions differently.
    } catch (IOException ignored) {
      // The artifact remains usable; callers still receive the path and can report it.
    }
  }

  record Credentials(String username, String password) {}
}
