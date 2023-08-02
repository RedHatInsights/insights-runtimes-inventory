/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.models;

import jakarta.persistence.*;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "eap_instance")
public final class EapInstance extends RuntimesInstance {
  @Id @GeneratedValue private UUID id;

  /****************************************************************************
   *                            Complex Fields
   ***************************************************************************/

  @OneToMany(mappedBy = "eap_instance", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<JarHash> jars; // These seem to be only jboss jars. Maybe only one instead of a set?

  @OneToMany(mappedBy = "eap_instance", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<JarHash> modules;

  @OneToOne(
      mappedBy = "eap_instance",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private EapConfiguration configuration;

  @OneToMany(mappedBy = "eap_instance", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<EapDeployment> deployments;

  public EapConfiguration getConfiguration() {
    return this.configuration;
  }

  public void setConfiguration(EapConfiguration configuration) {
    this.configuration = configuration;
  }

  public Set<JarHash> getModules() {
    return this.modules;
  }

  public void setModules(Set<JarHash> modules) {
    this.modules = modules;
  }

  public Set<EapDeployment> getDeployments() {
    return this.deployments;
  }

  public void setDeployments(Set<EapDeployment> deployments) {
    this.deployments = deployments;
  }

  /****************************************************************************
   *                            Simple Fields
   ***************************************************************************/

  private String raw;

  public String getRaw() {
    return raw;
  }

  public void setRaw(String raw) {
    this.raw = raw;
  }

  private String appClientException;
  private String appName;
  private String appTransportCertHttps;
  private String appTransportTypeFile;
  private String appTransportTypeHttps;
  private String appUserDir;
  private String appUserName;
  private String javaClassPath;
  private String javaClassVersion;
  private String javaCommand;
  private String javaHome;
  private String javaLibraryPath;
  private String javaSpecificationVendor;
  private String javaVendor;
  private String javaVendorVersion;
  private String javaVmName;
  private String javaVmVendor;
  private String jvmHeapGcDetails;
  private String jvmHeapMin;
  private String jvmPid;
  private String jvmReportTime;
  private String systemHostname;
  private String systemOsName;

  private String eapVersion;
  private Boolean eapXp;
  private Boolean eapYamlExtension;
  private Boolean eapBootableJar;
  private Boolean eapUseGit;

  public String getAppName() {
    return appName;
  }

  public void setAppName(String appName) {
    this.appName = appName;
  }

  public String getAppTransportCertHttps() {
    return appTransportCertHttps;
  }

  public void setAppTransportCertHttps(String appTransportCertHttps) {
    this.appTransportCertHttps = appTransportCertHttps;
  }

  public String getAppUserDir() {
    return appUserDir;
  }

  public void setAppUserDir(String appUserDir) {
    this.appUserDir = appUserDir;
  }

  public String getAppUserName() {
    return appUserName;
  }

  public void setAppUserName(String appUserName) {
    this.appUserName = appUserName;
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

  public String getJvmHeapMin() {
    return jvmHeapMin;
  }

  public void setJvmHeapMin(String jvmHeapMin) {
    this.jvmHeapMin = jvmHeapMin;
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

  public String getSystemHostname() {
    return systemHostname;
  }

  public void setSystemHostname(String systemHostname) {
    this.systemHostname = systemHostname;
  }

  public String getSystemOsName() {
    return systemOsName;
  }

  public void setSystemOsName(String systemOsName) {
    this.systemOsName = systemOsName;
  }

  public String getAppTransportTypeFile() {
    return appTransportTypeFile;
  }

  public void setAppTransportTypeFile(String appTransportTypeFile) {
    this.appTransportTypeFile = appTransportTypeFile;
  }

  public String getAppTransportTypeHttps() {
    return appTransportTypeHttps;
  }

  public void setAppTransportTypeHttps(String appTransportTypeHttps) {
    this.appTransportTypeHttps = appTransportTypeHttps;
  }

  public String getAppClientException() {
    return appClientException;
  }

  public void setAppClientException(String appClientException) {
    this.appClientException = appClientException;
  }

  public String getEapVersion() {
    return eapVersion;
  }

  public void setEapVersion(String eapVersion) {
    this.eapVersion = eapVersion;
  }

  public Boolean getEapXp() {
    return eapXp;
  }

  public void setEapXp(Boolean eapXp) {
    this.eapXp = eapXp;
  }

  public Boolean getEapYamlExtension() {
    return eapYamlExtension;
  }

  public void setEapYamlExtension(Boolean eapYamlExtension) {
    this.eapYamlExtension = eapYamlExtension;
  }

  public Boolean getEapBootableJar() {
    return eapBootableJar;
  }

  public void setEapBootableJar(Boolean eapBootableJar) {
    this.eapBootableJar = eapBootableJar;
  }

  public Boolean getEapUseGit() {
    return eapUseGit;
  }

  public void setEapUseGit(Boolean eapUseGit) {
    this.eapUseGit = eapUseGit;
  }

  /********** These are in the RuntimesInstance **********/
  // private String javaVmSpecificationVendor;
  // private String javaVmSpecificationVersion;
  // private String javaRuntimeVersion;
  // private String javaVersion;
  // private String javaSpecificationVersion;
  // private String systemArch;
  // private String systemCoresLogical;
  // private String jvmHeapMax;

  @ElementCollection private List<String> jvmPackages;
}
