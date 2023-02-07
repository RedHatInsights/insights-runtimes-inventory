/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArchiveAnnouncement {
  @JsonProperty("version")
  private String version = "1.0.0";

  //  @JsonProperty("bundle")
  //  @JsonPropertyDescription("Bundle name as used during application registration")
  //  private String bundle;

  @JsonProperty("application")
  @JsonPropertyDescription("Application name as used during application registration")
  private String application;

  @JsonProperty("timestamp")
  @JsonPropertyDescription(
      "ISO-8601 formatted date (per platform convention) when the message was sent in UTC. Dates"
          + " with timezones/offsets are rejected.")
  private LocalDateTime timestamp;

  @JsonProperty("account_id")
  private String accountId;

  @JsonProperty("org_id")
  private String orgId;

  @JsonProperty("request_id")
  private String requestId;

  @JsonProperty("url")
  private String url;

  public ArchiveAnnouncement() {}

  ////////////////////////////////////////////////////////////////////////

  @JsonProperty("version")
  public String getVersion() {
    return this.version;
  }

  @JsonProperty("version")
  public void setVersion(String version) {
    this.version = version;
  }

  //  @JsonProperty("bundle")
  //  public String getBundle() {
  //    return this.bundle;
  //  }
  //
  //  @JsonProperty("bundle")
  //  public void setBundle(String bundle) {
  //    this.bundle = bundle;
  //  }

  @JsonProperty("application")
  public String getApplication() {
    return this.application;
  }

  @JsonProperty("application")
  public void setApplication(String application) {
    this.application = application;
  }

  @JsonProperty("timestamp")
  public LocalDateTime getTimestamp() {
    return this.timestamp;
  }

  @JsonProperty("timestamp")
  public void setTimestamp(LocalDateTime timestamp) {
    this.timestamp = timestamp;
  }

  @JsonProperty("account")
  public String getAccountId() {
    return this.accountId;
  }

  @JsonProperty("account")
  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  @JsonProperty("org_id")
  public String getOrgId() {
    return this.orgId;
  }

  @JsonProperty("org_id")
  public void setOrgId(String orgId) {
    this.orgId = orgId;
  }

  @JsonProperty("request_id")
  public String getRequestId() {
    return requestId;
  }

  @JsonProperty("request_id")
  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  @JsonProperty("url")
  public String getUrl() {
    return url;
  }

  @JsonProperty("url")
  public void setUrl(String url) {
    this.url = url;
  }
}
