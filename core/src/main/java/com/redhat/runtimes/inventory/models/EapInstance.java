/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.models;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Objects;
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
  private String eapVersion;

  @NotNull private Boolean eapXp;

  @NotNull private Boolean eapYamlExtension;

  @NotNull private Boolean eapBootableJar;

  @NotNull private Boolean eapUseGit;

  /****************************************************************************
   *                            Raw JSON Dumps
   ***************************************************************************/
  @NotNull private String raw;

  public EapInstance() {}

  public EapInstance(
      UUID id,
      Set<JarHash> jars,
      Set<JarHash> modules,
      EapConfiguration configuration,
      Set<EapDeployment> deployments,
      @NotNull @Size(max = 255) String appClientException,
      @NotNull @Size(max = 255) String appName,
      @NotNull @Size(max = 255) String appTransportCertHttps,
      @NotNull @Size(max = 255) String appTransportTypeFile,
      @NotNull @Size(max = 255) String appTransportTypeHttps,
      @NotNull @Size(max = 255) String appUserDir,
      @NotNull @Size(max = 255) String appUserName,
      @NotNull @Size(max = 255) String eapVersion,
      @NotNull Boolean eapXp,
      @NotNull Boolean eapYamlExtension,
      @NotNull Boolean eapBootableJar,
      @NotNull Boolean eapUseGit,
      String raw) {
    this.id = id;
    this.jars = jars;
    this.modules = modules;
    this.configuration = configuration;
    this.deployments = deployments;
    this.appClientException = appClientException;
    this.appName = appName;
    this.appTransportCertHttps = appTransportCertHttps;
    this.appTransportTypeFile = appTransportTypeFile;
    this.appTransportTypeHttps = appTransportTypeHttps;
    this.appUserDir = appUserDir;
    this.appUserName = appUserName;
    this.eapVersion = eapVersion;
    this.eapXp = eapXp;
    this.eapYamlExtension = eapYamlExtension;
    this.eapBootableJar = eapBootableJar;
    this.eapUseGit = eapUseGit;
    this.raw = raw;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result =
        prime * result
            + Objects.hash(
                id,
                appClientException,
                appName,
                appTransportCertHttps,
                appTransportTypeFile,
                appTransportTypeHttps,
                appUserDir,
                appUserName,
                configuration,
                deployments,
                eapBootableJar,
                eapUseGit,
                eapVersion,
                eapXp,
                eapYamlExtension,
                jars,
                modules,
                raw);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    EapInstance other = (EapInstance) obj;
    return Objects.equals(appClientException, other.appClientException)
        && Objects.equals(appName, other.appName)
        && Objects.equals(appTransportCertHttps, other.appTransportCertHttps)
        && Objects.equals(appTransportTypeFile, other.appTransportTypeFile)
        && Objects.equals(appTransportTypeHttps, other.appTransportTypeHttps)
        && Objects.equals(appUserDir, other.appUserDir)
        && Objects.equals(appUserName, other.appUserName)
        && Objects.equals(configuration, other.configuration)
        && Objects.equals(deployments, other.deployments)
        && Objects.equals(eapBootableJar, other.eapBootableJar)
        && Objects.equals(eapUseGit, other.eapUseGit)
        && Objects.equals(eapVersion, other.eapVersion)
        && Objects.equals(eapXp, other.eapXp)
        && Objects.equals(eapYamlExtension, other.eapYamlExtension)
        && Objects.equals(id, other.id)
        && Objects.equals(jars, other.jars)
        && Objects.equals(modules, other.modules)
        && Objects.equals(raw, other.raw);
  }

  @Override
  public String toString() {
    final StringBuffer sb = new StringBuffer("EapInstance{");
    sb.append("id=").append(id);
    sb.append(", accountId='").append(accountId).append('\'');
    sb.append(", orgId='").append(orgId).append('\'');
    sb.append(", hostname='").append(hostname).append('\'');
    sb.append(", launchTime=").append(launchTime);
    sb.append(", vendor='").append(vendor).append('\'');
    sb.append(", versionString='").append(versionString).append('\'');
    sb.append(", version='").append(version).append('\'');
    sb.append(", majorVersion=").append(majorVersion);
    sb.append(", osArch='").append(osArch).append('\'');
    sb.append(", processors=").append(processors);
    sb.append(", heapMin=").append(heapMin);
    sb.append(", heapMax=").append(heapMax);
    sb.append(", details=").append(details);
    sb.append(", created=").append(created);
    sb.append(", javaClassPath=").append(javaClassPath);
    sb.append(", javaClassVersion=").append(javaClassVersion);
    sb.append(", javaHome=").append(javaHome);
    sb.append(", javaLibraryPath=").append(javaLibraryPath);
    sb.append(", javaSpecificationVendor=").append(javaSpecificationVendor);
    sb.append(", javaVendor=").append(javaVendor);
    sb.append(", javaVendorVersion=").append(javaVendorVersion);
    sb.append(", javaVmName=").append(javaVmName);
    sb.append(", javaVmVendor=").append(javaVmVendor);
    sb.append(", jvmHeapGcDetails=").append(jvmHeapGcDetails);
    sb.append(", jvmPid=").append(jvmPid);
    sb.append(", jvmReportTime=").append(jvmReportTime);
    sb.append(", systemOsName=").append(systemOsName);
    sb.append(", systemOsVersion=").append(systemOsVersion);
    sb.append(", javaCommand=").append(javaCommand);
    sb.append(", jvmPackages=").append(jvmPackages);
    sb.append(", jvmArgs=").append(jvmArgs);
    sb.append(", jars=").append(jars);
    sb.append(", modules=").append(modules);
    sb.append(", configuration=").append(configuration);
    sb.append(", deployments=").append(deployments);
    sb.append(", appClientException=").append(appClientException);
    sb.append(", appName=").append(appName);
    sb.append(", appTransportCertHttps=").append(appTransportCertHttps);
    sb.append(", appTransportTypeFile=").append(appTransportTypeFile);
    sb.append(", appTransportTypeHttps=").append(appTransportTypeHttps);
    sb.append(", appUserDir=").append(appUserDir);
    sb.append(", appUserName=").append(appUserName);
    sb.append(", eapVersion=").append(eapVersion);
    sb.append(", eapXp=").append(eapXp);
    sb.append(", eapYamlExtension=").append(eapYamlExtension);
    sb.append(", eapBootableJar=").append(eapBootableJar);
    sb.append(", eapUseGit=").append(eapUseGit);
    sb.append(", raw=").append(raw);
    sb.append('}');
    return sb.toString();
  }

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
}
