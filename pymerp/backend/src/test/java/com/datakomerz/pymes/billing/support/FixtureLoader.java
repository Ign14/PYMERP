package com.datakomerz.pymes.billing.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class FixtureLoader {

  private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();

  private FixtureLoader() {
  }

  public static ObjectNode objectNode(String path) {
    JsonNode node = json(path);
    if (!node.isObject()) {
      throw new IllegalArgumentException("Fixture at " + path + " is not a JSON object");
    }
    return (ObjectNode) node.deepCopy();
  }

  public static JsonNode json(String path) {
    try (InputStream input = resourceStream(path)) {
      return MAPPER.readTree(input);
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to read fixture at " + path, ex);
    }
  }

  public static String text(String path) {
    try (InputStream input = resourceStream(path)) {
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException ex) {
      throw new IllegalStateException("Unable to read fixture at " + path, ex);
    }
  }

  private static InputStream resourceStream(String path) {
    InputStream stream = FixtureLoader.class.getClassLoader().getResourceAsStream(path);
    if (stream == null) {
      throw new IllegalArgumentException("Fixture not found on classpath: " + path);
    }
    return stream;
  }
}
