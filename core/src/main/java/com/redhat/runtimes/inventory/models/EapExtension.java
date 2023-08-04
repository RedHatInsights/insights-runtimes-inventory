/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "eap_extension")
public final class EapExtension {
  @Id @GeneratedValue private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  private EapConfiguration eapConfiguration;

  @Size(max = 255)
  private String module;

  @ElementCollection private Set<NameVersionPair> subsystems;

  public String getModule() {
    return module;
  }

  public void setModule(String module) {
    this.module = module;
  }

  public Set<NameVersionPair> getSubsystems() {
    return subsystems;
  }

  public void setSubsystems(Set<NameVersionPair> subsystems) {
    this.subsystems = subsystems;
  }
}