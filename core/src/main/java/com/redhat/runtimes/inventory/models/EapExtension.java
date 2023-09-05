/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "eap_extension")
public final class EapExtension {
  @Id @GeneratedValue private UUID id;

  @NotNull
  @Size(max = 255)
  private String module;

  @ElementCollection
  @CollectionTable(name = "eap_extension_subsystems")
  private Set<NameVersionPair> subsystems;

  public EapExtension() {}

  public EapExtension(
      UUID id, @NotNull @Size(max = 255) String module, Set<NameVersionPair> subsystems) {
    this.id = id;
    this.module = module;
    this.subsystems = subsystems;
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, module, subsystems);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    EapExtension other = (EapExtension) obj;
    return Objects.equals(id, other.id)
        && Objects.equals(module, other.module)
        && Objects.equals(subsystems, other.subsystems);
  }

  @Override
  public String toString() {
    return "EapExtension [id=" + id + ", module=" + module + ", subsystems=" + subsystems + "]";
  }

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
