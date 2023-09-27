/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.NaturalId;

@Entity
@Table(name = "eap_configuration")
public class EapConfiguration {
  @Id @GeneratedValue private UUID id;

  /****************************************************************************
   *                            Complex Fields
   ***************************************************************************/

  @OneToOne(fetch = FetchType.LAZY)
  @NaturalId
  @JsonBackReference
  private EapInstance eapInstance;

  @ManyToMany(cascade = CascadeType.PERSIST)
  @JoinTable(
      name = "eap_configuration_eap_extension",
      joinColumns = {@JoinColumn(name = "eap_configuration_id")},
      inverseJoinColumns = {@JoinColumn(name = "eap_extension_id")})
  public Set<EapExtension> extensions;

  // Each subsystem name maps to a json dump of its config
  @ElementCollection
  @CollectionTable(name = "eap_configuration_subsystems")
  private Map<String, String> subsystems;

  // Each deployment name maps to a json dump of its config
  @ElementCollection
  @CollectionTable(name = "eap_configuration_deployments")
  private Map<String, String> deployments;

  /****************************************************************************
   *                            Simple Fields
   ***************************************************************************/
  @NotNull
  @Size(max = 255)
  private String version;

  @NotNull
  @Size(max = 255)
  private String launchType;

  @NotNull
  @Size(max = 255)
  private String name;

  @NotNull
  @Size(max = 255)
  private String organization;

  @NotNull
  @Size(max = 255)
  private String processType;

  @NotNull
  @Size(max = 255)
  private String productName;

  @NotNull
  @Size(max = 255)
  private String productVersion;

  @NotNull
  @Size(max = 255)
  private String profileName;

  @NotNull
  @Size(max = 255)
  private String releaseCodename;

  @NotNull
  @Size(max = 255)
  private String releaseVersion;

  @NotNull
  @Size(max = 255)
  private String runningMode;

  @NotNull
  @Size(max = 255)
  private String runtimeConfigurationState;

  @NotNull
  @Size(max = 255)
  private String serverState;

  @NotNull
  @Size(max = 255)
  private String suspendState;

  /****************************************************************************
   *                            Raw JSON Dumps
   ***************************************************************************/
  @NotNull
  @Size(max = org.hibernate.Length.LOB_DEFAULT)
  private String socketBindingGroups;

  @NotNull
  @Size(max = org.hibernate.Length.LOB_DEFAULT)
  private String paths;

  @NotNull
  @Size(max = org.hibernate.Length.LOB_DEFAULT)
  private String interfaces;

  @NotNull
  @Size(max = org.hibernate.Length.LOB_DEFAULT)
  private String coreServices;

  public EapConfiguration() {}

  public EapConfiguration(
      UUID id,
      EapInstance eapInstance,
      Set<EapExtension> extensions,
      Map<String, String> subsystems,
      Map<String, String> deployments,
      @NotNull @Size(max = 255) String version,
      @NotNull @Size(max = 255) String launchType,
      @NotNull @Size(max = 255) String name,
      @NotNull @Size(max = 255) String organization,
      @NotNull @Size(max = 255) String processType,
      @NotNull @Size(max = 255) String productName,
      @NotNull @Size(max = 255) String productVersion,
      @NotNull @Size(max = 255) String profileName,
      @NotNull @Size(max = 255) String releaseCodename,
      @NotNull @Size(max = 255) String releaseVersion,
      @NotNull @Size(max = 255) String runningMode,
      @NotNull @Size(max = 255) String runtimeConfigurationState,
      @NotNull @Size(max = 255) String serverState,
      @NotNull @Size(max = 255) String suspendState,
      String socketBindingGroups,
      String paths,
      String interfaces,
      String coreServices) {
    this.id = id;
    this.eapInstance = eapInstance;
    this.extensions = extensions;
    this.subsystems = subsystems;
    this.deployments = deployments;
    this.version = version;
    this.launchType = launchType;
    this.name = name;
    this.organization = organization;
    this.processType = processType;
    this.productName = productName;
    this.productVersion = productVersion;
    this.profileName = profileName;
    this.releaseCodename = releaseCodename;
    this.releaseVersion = releaseVersion;
    this.runningMode = runningMode;
    this.runtimeConfigurationState = runtimeConfigurationState;
    this.serverState = serverState;
    this.suspendState = suspendState;
    this.socketBindingGroups = socketBindingGroups;
    this.paths = paths;
    this.interfaces = interfaces;
    this.coreServices = coreServices;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        coreServices,
        deployments,
        eapInstance.getId(),
        extensions,
        id,
        interfaces,
        launchType,
        name,
        organization,
        paths,
        processType,
        productName,
        productVersion,
        profileName,
        releaseCodename,
        releaseVersion,
        runningMode,
        runtimeConfigurationState,
        serverState,
        socketBindingGroups,
        subsystems,
        suspendState,
        version);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    EapConfiguration other = (EapConfiguration) obj;
    return Objects.equals(coreServices, other.coreServices)
        && Objects.equals(deployments, other.deployments)
        && Objects.equals(eapInstance, other.eapInstance)
        && Objects.equals(extensions, other.extensions)
        && Objects.equals(id, other.id)
        && Objects.equals(interfaces, other.interfaces)
        && Objects.equals(launchType, other.launchType)
        && Objects.equals(name, other.name)
        && Objects.equals(organization, other.organization)
        && Objects.equals(paths, other.paths)
        && Objects.equals(processType, other.processType)
        && Objects.equals(productName, other.productName)
        && Objects.equals(productVersion, other.productVersion)
        && Objects.equals(profileName, other.profileName)
        && Objects.equals(releaseCodename, other.releaseCodename)
        && Objects.equals(releaseVersion, other.releaseVersion)
        && Objects.equals(runningMode, other.runningMode)
        && Objects.equals(runtimeConfigurationState, other.runtimeConfigurationState)
        && Objects.equals(serverState, other.serverState)
        && Objects.equals(socketBindingGroups, other.socketBindingGroups)
        && Objects.equals(subsystems, other.subsystems)
        && Objects.equals(suspendState, other.suspendState)
        && Objects.equals(version, other.version);
  }

  @Override
  public String toString() {
    return "EapConfiguration [id="
        + id
        + ", eapInstance="
        + eapInstance.getId()
        + ", extensions="
        + extensions
        + ", subsystems="
        + subsystems
        + ", deployments="
        + deployments
        + ", version="
        + version
        + ", launchType="
        + launchType
        + ", name="
        + name
        + ", organization="
        + organization
        + ", processType="
        + processType
        + ", productName="
        + productName
        + ", productVersion="
        + productVersion
        + ", profileName="
        + profileName
        + ", releaseCodename="
        + releaseCodename
        + ", releaseVersion="
        + releaseVersion
        + ", runningMode="
        + runningMode
        + ", runtimeConfigurationState="
        + runtimeConfigurationState
        + ", serverState="
        + serverState
        + ", suspendState="
        + suspendState
        + ", socketBindingGroups="
        + socketBindingGroups
        + ", paths="
        + paths
        + ", interfaces="
        + interfaces
        + ", coreServices="
        + coreServices
        + "]";
  }

  public EapInstance getEapInstance() {
    return eapInstance;
  }

  public void setEapInstance(EapInstance eapInstance) {
    this.eapInstance = eapInstance;
  }

  public Set<EapExtension> getExtensions() {
    return extensions;
  }

  public void setExtensions(Set<EapExtension> extensions) {
    this.extensions = extensions;
  }

  public Map<String, String> getSubsystems() {
    return subsystems;
  }

  public void setSubsystems(Map<String, String> subsystems) {
    this.subsystems = subsystems;
  }

  public Map<String, String> getDeployments() {
    return deployments;
  }

  public void setDeployments(Map<String, String> deployments) {
    this.deployments = deployments;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getLaunchType() {
    return launchType;
  }

  public void setLaunchType(String launchType) {
    this.launchType = launchType;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

  public String getProcessType() {
    return processType;
  }

  public void setProcessType(String processType) {
    this.processType = processType;
  }

  public String getProductName() {
    return productName;
  }

  public void setProductName(String productName) {
    this.productName = productName;
  }

  public String getProductVersion() {
    return productVersion;
  }

  public void setProductVersion(String productVersion) {
    this.productVersion = productVersion;
  }

  public String getProfileName() {
    return profileName;
  }

  public void setProfileName(String profileName) {
    this.profileName = profileName;
  }

  public String getReleaseCodename() {
    return releaseCodename;
  }

  public void setReleaseCodename(String releaseCodename) {
    this.releaseCodename = releaseCodename;
  }

  public String getReleaseVersion() {
    return releaseVersion;
  }

  public void setReleaseVersion(String releaseVersion) {
    this.releaseVersion = releaseVersion;
  }

  public String getRunningMode() {
    return runningMode;
  }

  public void setRunningMode(String runningMode) {
    this.runningMode = runningMode;
  }

  public String getRuntimeConfigurationState() {
    return runtimeConfigurationState;
  }

  public void setRuntimeConfigurationState(String runtimeConfigurationState) {
    this.runtimeConfigurationState = runtimeConfigurationState;
  }

  public String getServerState() {
    return serverState;
  }

  public void setServerState(String serverState) {
    this.serverState = serverState;
  }

  public String getSuspendState() {
    return suspendState;
  }

  public void setSuspendState(String suspendState) {
    this.suspendState = suspendState;
  }

  public String getSocketBindingGroups() {
    return socketBindingGroups;
  }

  public void setSocketBindingGroups(String socketBindingGroups) {
    this.socketBindingGroups = socketBindingGroups;
  }

  public String getPaths() {
    return paths;
  }

  public void setPaths(String paths) {
    this.paths = paths;
  }

  public String getInterfaces() {
    return interfaces;
  }

  public void setInterfaces(String interfaces) {
    this.interfaces = interfaces;
  }

  public String getCoreServices() {
    return coreServices;
  }

  public void setCoreServices(String coreServices) {
    this.coreServices = coreServices;
  }
}
