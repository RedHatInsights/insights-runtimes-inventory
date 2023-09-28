/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.auth;

import com.redhat.runtimes.inventory.auth.principal.ConsoleIdentity;
import com.redhat.runtimes.inventory.auth.principal.rhid.RhIdentity;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Test RhIdentity parsing. Resource files brought over from notifications-backend and extended */
class RhIdentityParsingTest {

  private static final String BASE = "src/test/resources/rhid-examples/";

  @Test
  void testParseRhId() throws Exception {
    String rhid = readValueFromFile("rhid.txt");
    RhIdentity rhIdent = assertRhIdentity(rhid);

    Assertions.assertEquals("1234", rhIdent.getAccountNumber());
    Assertions.assertNull(rhIdent.getOrgId());
    Assertions.assertEquals("joe-doe-user", rhIdent.getName());
    Assertions.assertEquals("joe-doe-user", rhIdent.getUser().getUsername());
  }

  @Test
  void testParseRhIdOrgId() throws Exception {
    String rhid = readValueFromFile("rhid_org_id.txt");
    RhIdentity rhIdent = assertRhIdentity(rhid);

    Assertions.assertEquals("1234", rhIdent.getAccountNumber());
    Assertions.assertEquals("12345", rhIdent.getOrgId());
    Assertions.assertEquals("joe-doe-user", rhIdent.getName());
    Assertions.assertEquals("joe-doe-user", rhIdent.getUser().getUsername());
  }

  @Test
  void testParseRhIdNoAccount() throws Exception {
    String rhid = readValueFromFile("rhid_no_account.txt");
    RhIdentity rhIdent = assertRhIdentity(rhid);

    Assertions.assertEquals("", rhIdent.getAccountNumber());
    Assertions.assertNull(rhIdent.getOrgId());
    Assertions.assertEquals("", rhIdent.getName());
    Assertions.assertEquals("", rhIdent.getUser().getUsername());
  }

  private RhIdentity assertRhIdentity(String rhid) {
    ConsoleIdentity ident = ConsoleIdentityProvider.getRhIdentityFromString(rhid);

    Assertions.assertEquals("User", ident.type);
    Assertions.assertEquals(rhid, ident.rawIdentity);
    Assertions.assertInstanceOf(RhIdentity.class, ident);

    return (RhIdentity) ident;
  }

  private String readValueFromFile(String fileName) throws IOException {
    return Files.readString(Paths.get(BASE, fileName)).trim();
  }
}
