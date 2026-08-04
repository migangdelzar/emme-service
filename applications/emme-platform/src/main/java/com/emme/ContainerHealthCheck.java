package com.emme;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Minimal shell-free health probe used by the JVM container runtime. */
public final class ContainerHealthCheck {

  private static final URI DEFAULT_ENDPOINT = URI.create("http://127.0.0.1:8081/actuator/health");
  private static final Duration TIMEOUT = Duration.ofSeconds(2);

  private ContainerHealthCheck() {}

  public static void main(String[] arguments) {
    var endpoint = arguments.length == 0 ? DEFAULT_ENDPOINT : URI.create(arguments[0]);
    var request = HttpRequest.newBuilder(endpoint).timeout(TIMEOUT).GET().build();

    try {
      var response =
          HttpClient.newBuilder()
              .connectTimeout(TIMEOUT)
              .build()
              .send(request, HttpResponse.BodyHandlers.ofString());
      if (!isHealthy(response.statusCode(), response.body())) {
        System.exit(1);
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      System.exit(1);
    } catch (Exception exception) {
      System.exit(1);
    }
  }

  static boolean isHealthy(int statusCode, String body) {
    return statusCode == 200
        && body != null
        && body.replaceAll("\\s", "").contains("\"status\":\"UP\"");
  }
}
