package com.emme.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * JSON template engine. Load templates from src/e2eTest/resources/templates/ and substitute
 * placeholders with test-specific values.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * var body = Template.load("tenant-create.json")
 *     .set("slug", "my-tenant")
 *     .set("name", "My Tenant")
 *     .render();
 * }</pre>
 *
 * <p>Templates use {{key}} syntax:
 *
 * <pre>{@code
 * {"slug":"{{slug}}","name":"{{name}}","email":"{{slug}}@test.com"}
 * }</pre>
 */
public final class Template {

  private final String raw;
  private final Map<String, String> values = new HashMap<>();

  private Template(String raw) {
    this.raw = raw;
  }

  /** Load template file from classpath: templates/name.json */
  public static Template load(String name) {
    var path = Path.of("src/e2eTest/resources/templates", name);
    try {
      return new Template(Files.readString(path));
    } catch (IOException e) {
      throw new RuntimeException("Failed to load template: " + name, e);
    }
  }

  /** Set a placeholder value. */
  public Template set(String key, Object value) {
    values.put(key, value.toString());
    return this;
  }

  /** Render the template with all set values. Unknown placeholders left as-is. */
  public String render() {
    var result = raw;
    for (var entry : values.entrySet()) {
      result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
    }
    return result;
  }

  /** Shortcut: load + set + render in one call. */
  public static String render(String name, Map<String, Object> vars) {
    var t = load(name);
    vars.forEach(t::set);
    return t.render();
  }
}
