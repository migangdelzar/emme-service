package com.emme;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import org.junit.jupiter.api.Test;

class ContainerHealthCheckTest {

  @Test
  void exitsSuccessfullyWhenActuatorIsUp() throws Exception {
    var server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        "/actuator/health",
        exchange -> {
          var body = "{ \"status\": \"UP\" }".getBytes();
          exchange.sendResponseHeaders(200, body.length);
          exchange.getResponseBody().write(body);
          exchange.close();
        });
    server.start();

    try {
      var endpoint =
          java.net.URI.create(
              "http://127.0.0.1:" + server.getAddress().getPort() + "/actuator/health");
      assertThat(ContainerHealthCheck.execute(endpoint, HttpClient.newHttpClient())).isZero();
    } finally {
      server.stop(0);
    }
  }

  @Test
  void rejectsNonSuccessfulOrNonUpResponses() {
    assertThat(ContainerHealthCheck.isHealthy(503, "{\"status\":\"DOWN\"}")).isFalse();
    assertThat(ContainerHealthCheck.isHealthy(200, "{\"status\":\"DEGRADED\"}")).isFalse();
  }

  @Test
  void exitsWithFailureWhenTheEndpointCannotBeReached() {
    var endpoint = java.net.URI.create("http://127.0.0.1:1/actuator/health");
    assertThat(ContainerHealthCheck.execute(endpoint, HttpClient.newHttpClient())).isOne();
  }
}
