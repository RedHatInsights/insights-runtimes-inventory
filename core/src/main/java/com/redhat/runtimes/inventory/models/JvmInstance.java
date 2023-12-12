/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.models;

import static org.hibernate.type.SqlTypes.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.ZonedDateTime;
import java.util.*;
import org.hibernate.annotations.JdbcTypeCode;

@Entity
@Table(name = "jvm_instance")
@Inheritance(strategy = InheritanceType.JOINED)
public non-sealed class JvmInstance implements InsightsMessage {

  @Id @GeneratedValue protected UUID id;

  @NotNull
  @Size(max = 255)
  protected String linkingHash;

  @Size(max = 50)
  protected String accountId;

  @NotNull
  @Size(max = 50)
  protected String orgId;

  @NotNull
  @Size(max = 50)
  protected String hostname;

  // Process launched at
  @NotNull protected long launchTime;

  @NotNull
  @Size(max = 255)
  protected String vendor;

  @NotNull
  @Size(max = 255)
  protected String versionString;

  //      "java.version" : "17.0.1",
  @NotNull
  @Size(max = 255)
  protected String version;

  @NotNull protected int majorVersion;

  @NotNull
  @Size(max = 50)
  protected String osArch;

  @NotNull protected int processors;

  @NotNull protected int heapMin;

  @NotNull protected int heapMax;

  @NotNull
  @JdbcTypeCode(JSON)
  @Column(columnDefinition = "jsonb")
  protected Map<String, Object> details;

  // Data record created
  @NotNull protected ZonedDateTime created;

  @ManyToMany(cascade = CascadeType.PERSIST)
  @JoinTable(
      name = "jvm_instance_jar_hash",
      joinColumns = {@JoinColumn(name = "jvm_instance_id")},
      inverseJoinColumns = {@JoinColumn(name = "jar_hash_id")})
  protected Set<JarHash> jarHashes;

  @NotNull
  @Size(max = 255)
  protected String javaClassVersion;

  @NotNull
  @Size(max = 255)
  protected String javaSpecificationVendor;

  @NotNull
  @Size(max = 255)
  protected String javaVendor;

  @NotNull
  @Size(max = 255)
  protected String javaVendorVersion;

  @NotNull
  @Size(max = 255)
  protected String javaVmName;

  @NotNull
  @Size(max = 255)
  protected String javaVmVendor;

  @NotNull
  @Size(max = 255)
  protected String jvmHeapGcDetails;

  @NotNull
  @Size(max = 255)
  protected String jvmPid;

  @NotNull
  @Size(max = 255)
  protected String jvmReportTime;

  @NotNull
  @Size(max = 255)
  protected String systemOsName;

  @NotNull
  @Size(max = 255)
  protected String systemOsVersion;

  @NotNull protected String javaHome;

  @NotNull protected String javaLibraryPath;

  @NotNull protected String javaCommand;

  @NotNull protected String javaClassPath;

  @NotNull protected String jvmPackages;

  @NotNull protected String jvmArgs;

  @NotNull protected String workload = "Unidentified";

  //////////////////////////////////////////////////////

  public JvmInstance() {}

  @SuppressWarnings("unchecked")
  public JvmInstance(
      UUID id,
      String accountId,
      String orgId,
      String hostname,
      long launchTime,
      String vendor,
      String versionString,
      String version,
      int majorVersion,
      String osArch,
      int processors,
      int heapMin,
      int heapMax,
      Object details,
      ZonedDateTime created,
      String javaClassPath,
      String javaClassVersion,
      String javaHome,
      String javaLibraryPath,
      String javaSpecificationVendor,
      String javaVendor,
      String javaVendorVersion,
      String javaVmName,
      String javaVmVendor,
      String jvmHeapGcDetails,
      String jvmPid,
      String jvmReportTime,
      String systemOsName,
      String systemOsVersion,
      String javaCommand,
      String jvmPackages,
      String jvmArgs) {
    this.id = id;
    this.accountId = accountId;
    this.orgId = orgId;
    this.hostname = hostname;
    this.launchTime = launchTime;
    this.vendor = vendor;
    this.versionString = versionString;
    this.version = version;
    this.majorVersion = majorVersion;
    this.osArch = osArch;
    this.processors = processors;
    this.heapMin = heapMin;
    this.heapMax = heapMax;
    this.details = (Map<String, Object>) details;
    this.created = created;
    this.javaClassPath = javaClassPath;
    this.javaClassVersion = javaClassVersion;
    this.javaHome = javaHome;
    this.javaLibraryPath = javaLibraryPath;
    this.javaSpecificationVendor = javaSpecificationVendor;
    this.javaVendor = javaVendor;
    this.javaVendorVersion = javaVendorVersion;
    this.javaVmName = javaVmName;
    this.javaVmVendor = javaVmVendor;
    this.jvmHeapGcDetails = jvmHeapGcDetails;
    this.jvmPid = jvmPid;
    this.jvmReportTime = jvmReportTime;
    this.systemOsName = systemOsName;
    this.systemOsVersion = systemOsVersion;
    this.javaCommand = javaCommand;
    this.jvmPackages = jvmPackages;
    this.jvmArgs = jvmArgs;
    this.workload = workload;
  }

  //////////////////////////////////////////////////////

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public void setLinkingHash(String linkingHash) {
    this.linkingHash = linkingHash;
  }

  public String getLinkingHash() {
    return linkingHash;
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

  public int getHeapMin() {
    return heapMin;
  }

  public void setHeapMin(int heapMin) {
    this.heapMin = heapMin;
  }

  public int getHeapMax() {
    return heapMax;
  }

  public void setHeapMax(int heapMax) {
    this.heapMax = heapMax;
  }

  public ZonedDateTime getCreated() {
    return created;
  }

  public void setCreated(ZonedDateTime created) {
    this.created = created;
  }

  public long getLaunchTime() {
    return launchTime;
  }

  public void setLaunchTime(long launchTime) {
    this.launchTime = launchTime;
  }

  public Map<String, Object> getDetails() {
    return details;
  }

  public void setDetails(Map<String, Object> details) {
    this.details = details;
  }

  public Set<JarHash> getJarHashes() {
    return jarHashes;
  }

  public void setJarHashes(Set<JarHash> jarHashes) {
    this.jarHashes = jarHashes;
  }

  public String getJavaClassPath() {
    return javaClassPath;
  }

  public void setJavaClassPath(String javaClassPath) {
    this.javaClassPath = javaClassPath;
  }

  public String getJavaClassVersion() {
    return javaClassVersion;
  }

  public void setJavaClassVersion(String javaClassVersion) {
    this.javaClassVersion = javaClassVersion;
  }

  public String getJavaCommand() {
    return javaCommand;
  }

  public void setJavaCommand(String javaCommand) {
    this.javaCommand = javaCommand;
  }

  public String getJavaHome() {
    return javaHome;
  }

  public void setJavaHome(String javaHome) {
    this.javaHome = javaHome;
  }

  public String getJavaLibraryPath() {
    return javaLibraryPath;
  }

  public void setJavaLibraryPath(String javaLibraryPath) {
    this.javaLibraryPath = javaLibraryPath;
  }

  public String getJavaSpecificationVendor() {
    return javaSpecificationVendor;
  }

  public void setJavaSpecificationVendor(String javaSpecificationVendor) {
    this.javaSpecificationVendor = javaSpecificationVendor;
  }

  public String getJavaVendor() {
    return javaVendor;
  }

  public void setJavaVendor(String javaVendor) {
    this.javaVendor = javaVendor;
  }

  public String getJavaVendorVersion() {
    return javaVendorVersion;
  }

  public void setJavaVendorVersion(String javaVendorVersion) {
    this.javaVendorVersion = javaVendorVersion;
  }

  public String getJavaVmName() {
    return javaVmName;
  }

  public void setJavaVmName(String javaVmName) {
    this.javaVmName = javaVmName;
  }

  public String getJavaVmVendor() {
    return javaVmVendor;
  }

  public void setJavaVmVendor(String javaVmVendor) {
    this.javaVmVendor = javaVmVendor;
  }

  public String getJvmHeapGcDetails() {
    return jvmHeapGcDetails;
  }

  public void setJvmHeapGcDetails(String jvmHeapGcDetails) {
    this.jvmHeapGcDetails = jvmHeapGcDetails;
  }

  public String getJvmPid() {
    return jvmPid;
  }

  public void setJvmPid(String jvmPid) {
    this.jvmPid = jvmPid;
  }

  public String getJvmReportTime() {
    return jvmReportTime;
  }

  public void setJvmReportTime(String jvmReportTime) {
    this.jvmReportTime = jvmReportTime;
  }

  public String getSystemOsName() {
    return systemOsName;
  }

  public void setSystemOsName(String systemOsName) {
    this.systemOsName = systemOsName;
  }

  public String getSystemOsVersion() {
    return systemOsVersion;
  }

  public void setSystemOsVersion(String systemOsVersion) {
    this.systemOsVersion = systemOsVersion;
  }

  public String getJvmPackages() {
    return jvmPackages;
  }

  public void setJvmPackages(String jvmPackages) {
    this.jvmPackages = jvmPackages;
  }

  public String getJvmArgs() {
    return jvmArgs;
  }

  public void setJvmArgs(String jvmArgs) {
    this.jvmArgs = jvmArgs;
  }

  public String getWorkload() {
    return workload;
  }

  public void setWorkload(String workload) {
    this.workload = workload;
  }

  //////////////////////////////////////////////////////

  @Override
  public void sanitize() {
    setJvmArgs(InsightsMessage.sanitizeJavaParameters(getJvmArgs()));
    setJavaCommand(InsightsMessage.sanitizeJavaParameters(getJavaCommand()));
  }

  //////////////////////////////////////////////////////

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    JvmInstance that = (JvmInstance) o;
    return launchTime == that.launchTime
        && majorVersion == that.majorVersion
        && processors == that.processors
        && heapMin == that.heapMin
        && heapMax == that.heapMax
        && Objects.equals(id, that.id)
        && Objects.equals(linkingHash, that.linkingHash)
        && Objects.equals(accountId, that.accountId)
        && Objects.equals(orgId, that.orgId)
        && Objects.equals(hostname, that.hostname)
        && Objects.equals(vendor, that.vendor)
        && Objects.equals(versionString, that.versionString)
        && Objects.equals(version, that.version)
        && Objects.equals(osArch, that.osArch)
        && Objects.equals(details, that.details)
        && Objects.equals(created, that.created)
        && Objects.equals(jarHashes, that.jarHashes)
        && Objects.equals(javaClassVersion, that.javaClassVersion)
        && Objects.equals(javaSpecificationVendor, that.javaSpecificationVendor)
        && Objects.equals(javaVendor, that.javaVendor)
        && Objects.equals(javaVendorVersion, that.javaVendorVersion)
        && Objects.equals(javaVmName, that.javaVmName)
        && Objects.equals(javaVmVendor, that.javaVmVendor)
        && Objects.equals(jvmHeapGcDetails, that.jvmHeapGcDetails)
        && Objects.equals(jvmPid, that.jvmPid)
        && Objects.equals(jvmReportTime, that.jvmReportTime)
        && Objects.equals(systemOsName, that.systemOsName)
        && Objects.equals(systemOsVersion, that.systemOsVersion)
        && Objects.equals(javaHome, that.javaHome)
        && Objects.equals(javaLibraryPath, that.javaLibraryPath)
        && Objects.equals(javaCommand, that.javaCommand)
        && Objects.equals(javaClassPath, that.javaClassPath)
        && Objects.equals(jvmPackages, that.jvmPackages)
        && Objects.equals(jvmArgs, that.jvmArgs)
        && Objects.equals(workload, that.workload);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        id,
        linkingHash,
        accountId,
        orgId,
        hostname,
        launchTime,
        vendor,
        versionString,
        version,
        majorVersion,
        osArch,
        processors,
        heapMin,
        heapMax,
        details,
        created,
        jarHashes,
        javaClassVersion,
        javaSpecificationVendor,
        javaVendor,
        javaVendorVersion,
        javaVmName,
        javaVmVendor,
        jvmHeapGcDetails,
        jvmPid,
        jvmReportTime,
        systemOsName,
        systemOsVersion,
        javaHome,
        javaLibraryPath,
        javaCommand,
        javaClassPath,
        jvmPackages,
        jvmArgs,
        workload);
  }

  @Override
  public String toString() {
    return "JvmInstance{"
        + "id="
        + id
        + ", linkingHash='"
        + linkingHash
        + '\''
        + ", accountId='"
        + accountId
        + '\''
        + ", orgId='"
        + orgId
        + '\''
        + ", hostname='"
        + hostname
        + '\''
        + ", launchTime="
        + launchTime
        + ", vendor='"
        + vendor
        + '\''
        + ", versionString='"
        + versionString
        + '\''
        + ", version='"
        + version
        + '\''
        + ", majorVersion="
        + majorVersion
        + ", osArch='"
        + osArch
        + '\''
        + ", processors="
        + processors
        + ", heapMin="
        + heapMin
        + ", heapMax="
        + heapMax
        + ", details="
        + details
        + ", created="
        + created
        + ", jarHashes="
        + jarHashes
        + ", javaClassVersion='"
        + javaClassVersion
        + '\''
        + ", javaSpecificationVendor='"
        + javaSpecificationVendor
        + '\''
        + ", javaVendor='"
        + javaVendor
        + '\''
        + ", javaVendorVersion='"
        + javaVendorVersion
        + '\''
        + ", javaVmName='"
        + javaVmName
        + '\''
        + ", javaVmVendor='"
        + javaVmVendor
        + '\''
        + ", jvmHeapGcDetails='"
        + jvmHeapGcDetails
        + '\''
        + ", jvmPid='"
        + jvmPid
        + '\''
        + ", jvmReportTime='"
        + jvmReportTime
        + '\''
        + ", systemOsName='"
        + systemOsName
        + '\''
        + ", systemOsVersion='"
        + systemOsVersion
        + '\''
        + ", javaHome='"
        + javaHome
        + '\''
        + ", javaLibraryPath='"
        + javaLibraryPath
        + '\''
        + ", javaCommand='"
        + javaCommand
        + '\''
        + ", javaClassPath='"
        + javaClassPath
        + '\''
        + ", jvmPackages='"
        + jvmPackages
        + '\''
        + ", jvmArgs='"
        + jvmArgs
        + '\''
        + ", workload='"
        + workload
        + '\''
        + '}';
  }
}
