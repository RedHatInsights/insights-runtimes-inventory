package com.redhat.runtimes.inventory.models;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "runtimes_instance")
public class RuntimesInstance {

  @Id
  @GeneratedValue
  private UUID id;

  @Size(max = 50)
  private String accountId;

  @NotNull
  @Size(max = 50)
  private String orgId;

  @NotNull
  @Size(max = 50)
  private String hostname;

//   "java.vm.specification.vendor" : "Oracle Corporation",
  @NotNull
  @Size(max = 255)
  private String vendor;

//      "java.runtime.version" : "17.0.1+12",
  @NotNull
  @Size(max = 255)
  private String versionString;

//      "java.version" : "17.0.1",
  @NotNull
  @Size(max = 255)
  private String version;

//    "java.vm.specification.version" : "17",
  @NotNull
  private int majorVersion;

//  "os.arch" : "x86_64",
  @NotNull
  @Size(max = 50)
  private String osArch;

//      "Logical Processors" : 12,
  @NotNull
  private int processors;

//  "Heap max (MB)" : 8192.0,
  @NotNull
  private int heapMax;

  //////////////////////////////////////////////////////


  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getAccountId() {
    return accountId;
  }

  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  public String getOrgId() {
    return orgId;
  }

  public void setOrgId(String orgId) {
    this.orgId = orgId;
  }

  public String getHostname() {
    return hostname;
  }

  public void setHostname(String hostname) {
    this.hostname = hostname;
  }

  public String getVendor() {
    return vendor;
  }

  public void setVendor(String vendor) {
    this.vendor = vendor;
  }

  public String getVersionString() {
    return versionString;
  }

  public void setVersionString(String versionString) {
    this.versionString = versionString;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public int getMajorVersion() {
    return majorVersion;
  }

  public void setMajorVersion(int majorVersion) {
    this.majorVersion = majorVersion;
  }

  public String getOsArch() {
    return osArch;
  }

  public void setOsArch(String osArch) {
    this.osArch = osArch;
  }

  public int getProcessors() {
    return processors;
  }

  public void setProcessors(int processors) {
    this.processors = processors;
  }

  public int getHeapMax() {
    return heapMax;
  }

  public void setHeapMax(int heapMax) {
    this.heapMax = heapMax;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    RuntimesInstance that = (RuntimesInstance) o;
    return majorVersion == that.majorVersion && processors == that.processors && heapMax == that.heapMax && Objects.equals(id, that.id) && Objects.equals(accountId, that.accountId) && Objects.equals(orgId, that.orgId) && Objects.equals(hostname, that.hostname) && Objects.equals(vendor, that.vendor) && Objects.equals(versionString, that.versionString) && Objects.equals(version, that.version) && Objects.equals(osArch, that.osArch);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, accountId, orgId, hostname, vendor, versionString, version, majorVersion, osArch, processors, heapMax);
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("RuntimesInstance{");
    sb.append("id='").append(id).append('\'');
    sb.append(", accountId='").append(accountId).append('\'');
    sb.append(", orgId='").append(orgId).append('\'');
    sb.append(", hostname='").append(hostname).append('\'');
    sb.append(", vendor='").append(vendor).append('\'');
    sb.append(", versionString='").append(versionString).append('\'');
    sb.append(", version='").append(version).append('\'');
    sb.append(", majorVersion=").append(majorVersion);
    sb.append(", osArch='").append(osArch).append('\'');
    sb.append(", processors=").append(processors);
    sb.append(", heapMax=").append(heapMax);
    sb.append('}');
    return sb.toString();
  }
}
