/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.NaturalId;

@Entity
@Table(name = "eap_deployment")
public class EapDeployment {
  @Id @GeneratedValue private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @NaturalId
  private EapInstance eapInstance;

  @NotNull
  @Size(max = 255)
  private String name;

  @ManyToMany(cascade = CascadeType.PERSIST)
  @JoinTable(
      name = "eap_deployment_archive_jar_hash",
      joinColumns = {@JoinColumn(name = "eap_deployment_id")},
      inverseJoinColumns = {@JoinColumn(name = "jar_hash_id")})
  private Set<JarHash> archives;

  public EapDeployment() {}

  public EapDeployment(
      UUID id,
      EapInstance eapInstance,
      @NotNull @Size(max = 255) String name,
      Set<JarHash> archives) {
    this.id = id;
    this.eapInstance = eapInstance;
    this.name = name;
    this.archives = archives;
  }

  @Override
  public int hashCode() {
    return Objects.hash(archives, eapInstance.getId(), id, name);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    EapDeployment other = (EapDeployment) obj;
    return Objects.equals(archives, other.archives)
        && Objects.equals(eapInstance, other.eapInstance)
        && Objects.equals(id, other.id)
        && Objects.equals(name, other.name);
  }

  @Override
  public String toString() {
    return "EapDeployment [id="
        + id
        + ", eapInstance="
        + eapInstance.getId()
        + ", name="
        + name
        + ", archives="
        + archives
        + "]";
  }

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
