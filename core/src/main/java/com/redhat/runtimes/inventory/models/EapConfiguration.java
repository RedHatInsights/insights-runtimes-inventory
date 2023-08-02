/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.models;

import jakarta.persistence.*;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "eap_configuration")
public final class EapConfiguration {
  @Id @GeneratedValue private UUID id;

  /****************************************************************************
   *                            Complex Fields
   ***************************************************************************/

  @OneToOne(fetch = FetchType.LAZY)
  private EapInstance eapInstance;

  @ManyToMany
  @JoinTable(
      name = "eap_configuration_extensions",
      joinColumns = {@JoinColumn(name = "eap_configuration_id")},
      inverseJoinColumns = {@JoinColumn(name = "eap_extension_id")})
  public Set<EapExtension> extensions;

  // Each subsystem name maps to a json dump of its config
  @ElementCollection private Map<String, String> subsystems;

  // Each deployment name maps to a json dump of its config
  @ElementCollection private Map<String, String> deployments;

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

  /****************************************************************************
   *                            Simple Fields
   ***************************************************************************/

  private String version;

  private String launchType;
  private String name;
  private String organization;
  private String processType;
  private String productName;
  private String productVersion;
  private String profileName;
  private String releaseCodename;
  private String releaseVersion;
  private String runningMode;
  private String runtimeConfigurationState;
  private String serverState;
  private String suspendState;

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

  /****************************************************************************
   *                            Raw JSON Dumps
   ***************************************************************************/

  private String socketBindingGroups;

  private String paths;
  private String interfaces;
  private String coreServices;

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
