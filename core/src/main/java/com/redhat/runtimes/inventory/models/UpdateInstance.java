/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.models;

import java.util.List;

public final class UpdateInstance implements InsightsMessage {
  private final String linkingHash;
  private final List<JarHash> updates;

  public UpdateInstance(String linkingHash, List<JarHash> updates) {
    this.linkingHash = linkingHash;
    this.updates = updates;
  }

  public String getLinkingHash() {
    return linkingHash;
  }

  public List<JarHash> getUpdates() {
    return updates;
  }

  @Override
  public void sanitize() {
    // No-op
  }
}
