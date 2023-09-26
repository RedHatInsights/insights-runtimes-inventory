/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.auth.rbac;

import java.util.List;
import java.util.Map;

// Derived from notifications-backend
public class RbacRaw {
  public Map<String, String> links;
  public Map<String, Integer> meta;
  public List<Map<String, Object>> data;

  public boolean canRead(String application, String item) {
    return findPermission(application, item, "read");
  }

  public boolean canWrite(String application, String item) {
    return findPermission(application, item, "write");
  }

  public boolean canDo(String application, String item, String permission) {
    return findPermission(application, item, permission);
  }

  private boolean findPermission(String application, String item, String what) {
    if (data == null || data.size() == 0) {
      return false;
    }

    for (Map<String, Object> permissionEntry : data) {
      String perms = (String) permissionEntry.get("permission");
      String[] fields = perms.split(":");
      if (fields.length < 3) {
        return false;
      }
      if (fields[0].equals(application)) {
        if (fields[1].equals(item) || fields[1].equals("*")) {
          if (fields[2].equals(what) || fields[2].equals("*")) {
            return true;
          }
        }
      }
    }
    return false;
  }
}
