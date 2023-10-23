/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.events;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.function.BooleanSupplier;
import org.awaitility.core.ConditionTimeoutException;

public final class TestUtils {
  private TestUtils() {}

  public static Long lastResult;

  public static String readFromResources(String fName) throws IOException {
    return new String(readBytesFromResources(fName));
  }

  public static byte[] readBytesFromResources(String fName) throws IOException {
    try (final InputStream is =
            ArchiveAnnouncementParserTest.class.getClassLoader().getResourceAsStream(fName);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      byte[] buffer = new byte[4096];
      for (; ; ) {
        int nread = is.read(buffer);
        if (nread <= 0) {
          break;
        }
        baos.write(buffer, 0, nread);
      }
      return baos.toByteArray();
    }
  }

  public static InputStream inputStreamFromResources(String fName) throws IOException {
    return ArchiveAnnouncementParserTest.class.getClassLoader().getResourceAsStream(fName);
  }

  public static void await_entity_count(EntityManager entityManager, String entity, Long expected) {
    try {
      await_result(
          () -> {
            TestUtils.lastResult = entity_count(entityManager, entity);
            return lastResult == expected;
          },
          10);
    } catch (ConditionTimeoutException e) {
      fail(
          "Entity count for ["
              + entity
              + "] was ["
              + lastResult
              + "]. Expected ["
              + expected
              + "]");
    }
  }

  @Transactional
  public static void await_result(BooleanSupplier f, int timeout) {
    await()
        .atMost(Duration.ofSeconds(timeout))
        .until(
            () -> {
              return f.getAsBoolean();
            });
  }

  @Transactional
  public static Long entity_count(EntityManager entityManager, String entity) {
    // I don't know why, but but hibernate throws a ParsingException
    // when I try a named or positional query parameter
    return entityManager
        .createQuery("SELECT COUNT (*) FROM " + entity, Long.class)
        .getSingleResult();
  }

  @Transactional
  public static Long table_count(EntityManager entityManager, String table) {
    // I don't know why, but but hibernate throws a ParsingException
    // when I try a named or positional query parameter
    return (Long)
        entityManager
            .createNativeQuery("SELECT COUNT (*) FROM " + table, Long.class)
            .getSingleResult();
  }

  @Transactional
  public static void clearTables(EntityManager entityManager) {
    // Order is important here
    entityManager.createNativeQuery("DELETE FROM jvm_instance_jar_hash").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM eap_instance_module_jar_hash").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM eap_deployment_archive_jar_hash").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM jar_hash").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM jvm_instance").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM eap_deployment").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM eap_configuration_eap_extension").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM eap_configuration").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM eap_instance").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM eap_configuration_deployments").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM eap_configuration_subsystems").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM eap_extension").executeUpdate();
    entityManager.createNativeQuery("DELETE FROM eap_extension_subsystems").executeUpdate();
  }
}
