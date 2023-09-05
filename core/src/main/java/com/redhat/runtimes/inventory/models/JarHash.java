/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "jar_hash")
public final class JarHash {

  @Id @GeneratedValue private UUID id;

  @NotNull
  @Size(max = 255)
  private String name;

  @NotNull
  @Size(max = 255)
  private String groupId;

  // Should groupId & artifactId be nullable?

  @NotNull
  @Size(max = 255)
  private String vendor;

  @NotNull
  @Size(max = 255)
  private String version;

  @NotNull
  @Size(max = 255)
  private String sha1Checksum;

  @NotNull
  @Size(max = 255)
  private String sha256Checksum;

  @NotNull
  @Size(max = 255)
  private String sha512Checksum;

  ///////////////////////////////

  public JarHash() {}

  public JarHash(
      UUID id,
      String name,
      String groupId,
      String vendor,
      String version,
      String sha1Checksum,
      String sha256Checksum,
      String sha512Checksum) {
    this.id = id;
    this.name = name;
    this.groupId = groupId;
    this.vendor = vendor;
    this.version = version;
    this.sha1Checksum = sha1Checksum;
    this.sha256Checksum = sha256Checksum;
    this.sha512Checksum = sha512Checksum;
  }

  ///////////////////////////////

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getGroupId() {
    return groupId;
  }

  public void setGroupId(String groupId) {
    this.groupId = groupId;
  }

  public String getVendor() {
    return vendor;
  }

  public void setVendor(String vendor) {
    this.vendor = vendor;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getSha1Checksum() {
    return sha1Checksum;
  }

  public void setSha1Checksum(String sha1Checksum) {
    this.sha1Checksum = sha1Checksum;
  }

  public String getSha256Checksum() {
    return sha256Checksum;
  }

  public void setSha256Checksum(String sha256Checksum) {
    this.sha256Checksum = sha256Checksum;
  }

  public String getSha512Checksum() {
    return sha512Checksum;
  }

  public void setSha512Checksum(String sha512Checksum) {
    this.sha512Checksum = sha512Checksum;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    JarHash jarHash = (JarHash) o;
    return Objects.equals(id, jarHash.id)
        && Objects.equals(name, jarHash.name)
        && Objects.equals(groupId, jarHash.groupId)
        && Objects.equals(vendor, jarHash.vendor)
        && Objects.equals(version, jarHash.version)
        && Objects.equals(sha1Checksum, jarHash.sha1Checksum)
        && Objects.equals(sha256Checksum, jarHash.sha256Checksum)
        && Objects.equals(sha512Checksum, jarHash.sha512Checksum);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id, name, groupId, vendor, version, sha1Checksum, sha256Checksum, sha512Checksum);
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("JarHash{");
    sb.append("id=").append(id);
    sb.append(", name='").append(name).append('\'');
    sb.append(", groupId='").append(groupId).append('\'');
    sb.append(", vendor='").append(vendor).append('\'');
    sb.append(", version='").append(version).append('\'');
    sb.append(", sha1Checksum='").append(sha1Checksum).append('\'');
    sb.append(", sha256Checksum='").append(sha256Checksum).append('\'');
    sb.append(", sha512Checksum='").append(sha512Checksum).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
