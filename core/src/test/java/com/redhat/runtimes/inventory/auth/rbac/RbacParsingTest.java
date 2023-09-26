/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.auth.rbac;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Test RBAC parsing. Brought over from notifications-backend and extended */
class RbacParsingTest {

  private static final ObjectReader RBAC_RAW_READER = new ObjectMapper().readerFor(RbacRaw.class);
  private static final String BASE = "src/test/resources/rbac-examples/";
  public static final String ALL = "*";

  @Test
  void testParseExample() throws Exception {
    RbacRaw rbac = readValueFromFile("rbac_example.json");
    Assertions.assertEquals(2, rbac.data.size());

    Assertions.assertTrue(rbac.canWrite("bar", "resname"));
    Assertions.assertFalse(rbac.canRead("resname", ALL));
    Assertions.assertFalse(rbac.canWrite(ALL, ALL));
    Assertions.assertFalse(rbac.canWrite("no-perm", ALL));
  }

  @Test
  void testNoAccess() throws Exception {
    RbacRaw rbac = readValueFromFile("rbac_example_no_access.json");

    Assertions.assertFalse(rbac.canRead(ALL, ALL));
    Assertions.assertFalse(rbac.canWrite(ALL, ALL));
  }

  @Test
  void testFullAccess() throws Exception {
    RbacRaw rbac = readValueFromFile("rbac_example_full_access.json");

    Assertions.assertTrue(rbac.canRead("inventory", ALL));
    Assertions.assertFalse(rbac.canRead("dummy", ALL));
    Assertions.assertTrue(rbac.canWrite("inventory", ALL));
    Assertions.assertTrue(rbac.canWrite("inventory", "hosts"));
    Assertions.assertTrue(rbac.canWrite("inventory", "does-not-exist"));
    Assertions.assertFalse(rbac.canWrite("dummy", ALL));
  }

  @Test
  void testHostsRead() throws Exception {
    RbacRaw rbac = readValueFromFile("rbac_example_events_hosts_access_only.json");

    Assertions.assertTrue(rbac.canRead("inventory", "hosts"));
    Assertions.assertTrue(rbac.canDo("inventory", "hosts", "read"));
    Assertions.assertFalse(rbac.canRead("inventory", ALL));
    Assertions.assertFalse(rbac.canRead("inventory", "does-not-exist"));
    Assertions.assertFalse(rbac.canWrite("inventory", ALL));
    Assertions.assertFalse(rbac.canRead("dummy", ALL));
    Assertions.assertFalse(rbac.canWrite("dummy", ALL));
  }

  @Test
  void testInventoryRead() throws Exception {
    RbacRaw rbac = readValueFromFile("rbac_example_events_inventory_read_access_only.json");

    Assertions.assertTrue(rbac.canRead("inventory", "hosts"));
    Assertions.assertTrue(rbac.canDo("inventory", "hosts", "read"));
    Assertions.assertTrue(rbac.canRead("inventory", ALL));
    Assertions.assertTrue(rbac.canRead("inventory", "does-not-exist"));
    Assertions.assertFalse(rbac.canWrite("inventory", ALL));
    Assertions.assertFalse(rbac.canRead("dummy", ALL));
    Assertions.assertFalse(rbac.canWrite("dummy", ALL));
  }

  @Test
  void testPartialAccess() throws IOException {
    RbacRaw rbac = readValueFromFile("rbac_example_partial_access.json");

    Assertions.assertTrue(rbac.canRead("policies", ALL));
    Assertions.assertFalse(rbac.canRead("dummy", ALL));
    Assertions.assertFalse(rbac.canWrite("policies", ALL));
    Assertions.assertFalse(rbac.canWrite("dummy", ALL));
    Assertions.assertTrue(rbac.canDo("policies", ALL, "execute"));
    Assertions.assertFalse(rbac.canDo("policies", ALL, "list"));
  }

  @Test
  void testTwoApps() throws IOException {
    RbacRaw rbac = readValueFromFile("rbac_example_two_apps1.json");

    Assertions.assertFalse(rbac.canRead("inventory", ALL));
    Assertions.assertFalse(rbac.canWrite("inventory", ALL));
    Assertions.assertTrue(rbac.canDo("inventory", ALL, "execute"));

    Assertions.assertTrue(rbac.canRead("integrations", "endpoints"));
    Assertions.assertFalse(rbac.canWrite("integrations", "endpoints"));
    // We have no * item
    Assertions.assertFalse(rbac.canRead("integrations", ALL));
    Assertions.assertFalse(rbac.canWrite("integrations", ALL));

    Assertions.assertFalse(rbac.canDo("integrations", ALL, "read"));
    Assertions.assertFalse(rbac.canDo("integrations", ALL, "execute"));
    Assertions.assertTrue(rbac.canDo("integrations", "admin", "execute"));
  }

  private RbacRaw readValueFromFile(String fileName) throws IOException {
    try (InputStream is = new FileInputStream(new File(BASE + fileName))) {
      return RBAC_RAW_READER.readValue(is);
    }
  }
}
