/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.auth.rbac;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;

/** Test RBAC parsing. Brought over from notifications-backend and extended */
class RbacParsingTest {

  private static final ObjectReader RBAC_RAW_READER = new ObjectMapper().readerFor(RbacRaw.class);
  private static final String BASE = "src/test/resources/rbac-examples/";
  public static final String ALL = "*";

  @Test
  void testParseExample() throws Exception {
    RbacRaw rbac = readValueFromFile("rbac_example.json");
    assert rbac.data.size() == 2;

    assert rbac.canWrite("bar", "resname");
    assert !rbac.canRead("resname", ALL);
    assert !rbac.canWrite(ALL, ALL);
    assert !rbac.canWrite("no-perm", ALL);
  }

  @Test
  void testNoAccess() throws Exception {
    RbacRaw rbac = readValueFromFile("rbac_example_no_access.json");

    assert !rbac.canRead(ALL, ALL);
    assert !rbac.canWrite(ALL, ALL);
  }

  @Test
  void testFullAccess() throws Exception {
    RbacRaw rbac = readValueFromFile("rbac_example_full_access.json");

    assert rbac.canRead("inventory", ALL);
    assert !rbac.canRead("dummy", ALL);
    assert rbac.canWrite("inventory", ALL);
    assert rbac.canWrite("inventory", "hosts");
    assert rbac.canWrite("inventory", "does-not-exist");
    assert !rbac.canWrite("dummy", ALL);
  }

  @Test
  void testHostsRead() throws Exception {
    RbacRaw rbac = readValueFromFile("rbac_example_events_hosts_access_only.json");

    assert rbac.canRead("inventory", "hosts");
    assert rbac.canDo("inventory", "hosts", "read");
    assert !rbac.canRead("inventory", ALL);
    assert !rbac.canRead("inventory", "does-not-exist");
    assert !rbac.canWrite("inventory", ALL);
    assert !rbac.canRead("dummy", ALL);
    assert !rbac.canWrite("dummy", ALL);
  }

  @Test
  void testInventoryRead() throws Exception {
    RbacRaw rbac = readValueFromFile("rbac_example_events_inventory_read_access_only.json");

    assert rbac.canRead("inventory", "hosts");
    assert rbac.canDo("inventory", "hosts", "read");
    assert rbac.canRead("inventory", ALL);
    assert rbac.canRead("inventory", "does-not-exist");
    assert !rbac.canWrite("inventory", ALL);
    assert !rbac.canRead("dummy", ALL);
    assert !rbac.canWrite("dummy", ALL);
  }

  @Test
  void testPartialAccess() throws IOException {
    RbacRaw rbac = readValueFromFile("rbac_example_partial_access.json");

    assert rbac.canRead("policies", ALL);
    assert !rbac.canRead("dummy", ALL);
    assert !rbac.canWrite("policies", ALL);
    assert !rbac.canWrite("dummy", ALL);
    assert rbac.canDo("policies", ALL, "execute");
    assert !rbac.canDo("policies", ALL, "list");
  }

  @Test
  void testTwoApps() throws IOException {
    RbacRaw rbac = readValueFromFile("rbac_example_two_apps1.json");

    assert !rbac.canRead("inventory", ALL);
    assert !rbac.canWrite("inventory", ALL);
    assert rbac.canDo("inventory", ALL, "execute");

    assert rbac.canRead("integrations", "endpoints");
    assert !rbac.canWrite("integrations", "endpoints");
    // We have no * item
    assert !rbac.canRead("integrations", ALL);
    assert !rbac.canWrite("integrations", ALL);

    assert !rbac.canDo("integrations", ALL, "read");
    assert !rbac.canDo("integrations", ALL, "execute");
    assert rbac.canDo("integrations", "admin", "execute");
  }

  private RbacRaw readValueFromFile(String fileName) throws IOException {
    try (InputStream is = new FileInputStream(new File(BASE + fileName))) {
      return RBAC_RAW_READER.readValue(is);
    }
  }
}
