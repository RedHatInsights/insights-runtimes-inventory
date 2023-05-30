/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public final class ArchiveAnnouncementParser {
  static final ObjectMapper objectMapper = getMapper();
  // FIXME Add schema support?
  //  private static final JsonSchema jsonSchema = getJsonSchema();

  private static ObjectMapper getMapper() {
    var mapper = new ObjectMapper();
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.registerModule(new JavaTimeModule());
    return mapper;
  }

  public ArchiveAnnouncement fromJsonString(String json) {
    try {
      return decode(json);
    } catch (JsonProcessingException jsonx) {
      throw new RuntimeException("Failed to unmarshal JSON: " + json, jsonx);
    }
  }

  private static ArchiveAnnouncement decode(String actionJson) throws JsonProcessingException {
    JsonNode action = objectMapper.readTree(actionJson);
    //    validate(action, jsonSchema);
    return objectMapper.treeToValue(action, ArchiveAnnouncement.class);
  }
}
