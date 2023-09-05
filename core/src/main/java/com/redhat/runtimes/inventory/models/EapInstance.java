/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "eap_instance")
public final class EapInstance extends JvmInstance {
  @Id @GeneratedValue private UUID id;

  /****************************************************************************
   *                            Complex Fields
   ***************************************************************************/

  @ManyToMany(cascade = CascadeType.PERSIST)
  @JoinTable(
      name = "eap_instance_jar_hash",
      joinColumns = {@JoinColumn(name = "eap_instance_id")},
      inverseJoinColumns = {@JoinColumn(name = "jar_hash_id")})
  private Set<JarHash> jars; // These seem to be only jboss jars. Maybe only one instead of a set?

  @ManyToMany(cascade = CascadeType.PERSIST)
  @JoinTable(
      name = "eap_instance_module_jar_hash",
      joinColumns = {@JoinColumn(name = "eap_instance_id")},
      inverseJoinColumns = {@JoinColumn(name = "jar_hash_id")})
  private Set<JarHash> modules;

  @OneToOne(
      mappedBy = "eapInstance",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  private EapConfiguration configuration;

  @OneToMany(mappedBy = "eapInstance", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<EapDeployment> deployments;

  @Lob private String javaCommand;

  @Lob private String jvmPackages;

  @Lob private String jvmArgs;

  @Lob private String raw;

  public Set<JarHash> getJars() {
    return this.jars;
  }

  public void setJars(Set<JarHash> jars) {
    this.jars = jars;
  }

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

  public String getRaw() {
    return raw;
  }

  public void setRaw(String raw) {
    this.raw = raw;
  }

  public String getJvmPackages() {
    return jvmPackages;
  }

  public void setJvmPackages(String jvmPackages) {
    this.jvmPackages = raw;
  }

  public String getJvmArgs() {
    return jvmArgs;
  }

  public void setJvmArgs(String jvmArgs) {
    this.jvmArgs = raw;
  }

  /****************************************************************************
   *                            Simple Fields
   ***************************************************************************/

  @NotNull
  @Size(max = 255)
  private String appClientException;

  @NotNull
  @Size(max = 255)
  private String appName;

  @NotNull
  @Size(max = 255)
  private String appTransportCertHttps;

  @NotNull
  @Size(max = 255)
  private String appTransportTypeFile;

  @NotNull
  @Size(max = 255)
  private String appTransportTypeHttps;

  @NotNull
  @Size(max = 255)
  private String appUserDir;

  @NotNull
  @Size(max = 255)
  private String appUserName;

  @NotNull
  @Size(max = 255)
  private String javaClassPath;

  @NotNull
  @Size(max = 255)
  private String javaClassVersion;

  @NotNull
  @Size(max = 255)
  private String javaHome;

  @NotNull
  @Size(max = 255)
  private String javaLibraryPath;

  @NotNull
  @Size(max = 255)
  private String javaSpecificationVendor;

  @NotNull
  @Size(max = 255)
  private String javaVendor;

  @NotNull
  @Size(max = 255)
  private String javaVendorVersion;

  @NotNull
  @Size(max = 255)
  private String javaVmName;

  @NotNull
  @Size(max = 255)
  private String javaVmVendor;

  @NotNull
  @Size(max = 255)
  private String jvmHeapGcDetails;

  @NotNull
  @Size(max = 255)
  private String jvmHeapMin;

  @NotNull
  @Size(max = 255)
  private String jvmPid;

  @NotNull
  @Size(max = 255)
  private String jvmReportTime;

  @NotNull
  @Size(max = 255)
  private String systemHostname;

  @NotNull
  @Size(max = 255)
  private String systemOsName;

  @NotNull
  @Size(max = 255)
  private String systemOsVersion;

  @NotNull
  @Size(max = 255)
  private String eapVersion;

  @NotNull private Boolean eapXp;

  @NotNull private Boolean eapYamlExtension;

  @NotNull private Boolean eapBootableJar;

  @NotNull private Boolean eapUseGit;

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

  public String getSystemOsVersion() {
    return systemOsVersion;
  }

  public void setSystemOsVersion(String systemOsVersion) {
    this.systemOsVersion = systemOsVersion;
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

  /********** These are in the JvmInstance **********/
  // private String javaVmSpecificationVendor;
  // private String javaVmSpecificationVersion;
  // private String javaRuntimeVersion;
  // private String javaVersion;
  // private String javaSpecificationVersion;
  // private String systemArch;
  // private String systemCoresLogical;
  // private String jvmHeapMax;

}
