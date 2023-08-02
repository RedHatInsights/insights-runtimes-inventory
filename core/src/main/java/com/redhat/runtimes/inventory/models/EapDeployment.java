/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "eap_deployment")
public final class EapDeployment {
  @Id @GeneratedValue private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  private EapInstance eapInstance;

  @Size(max = 255)
  private String name;

  @ElementCollection private Set<JarHash> archives;

  public EapInstance getEapInstance() {
    return eapInstance;
  }

  public void setEapInstance(EapInstance eapInstance) {
    this.eapInstance = eapInstance;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Set<JarHash> getArchives() {
    return archives;
  }

  public void setArchives(Set<JarHash> archives) {
    this.archives = archives;
  }
}
