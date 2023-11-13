/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ArchiveAnnouncement {
  @JsonProperty("version")
  private String version = "1.0.0";

  @JsonProperty("application")
  @JsonPropertyDescription("Application name as used during application registration")
  private String application;

  @JsonProperty("content_type")
  @JsonPropertyDescription("Application content-type")
  private String contentType;

  @JsonProperty("timestamp")
  @JsonPropertyDescription(
      "ISO-8601 formatted date (per platform convention) when the message was sent in UTC. Dates"
          + " with timezones/offsets are rejected.")
  private Instant timestamp;

  @JsonProperty("account_id")
  private String accountId;

  @JsonProperty("org_id")
  private String orgId;

  @JsonProperty("request_id")
  private String requestId;

  @JsonProperty("url")
  private String url;

  @JsonProperty("platform_metadata")
  private Map<String, Object> platformMetadata;

  @JsonProperty("host")
  private Map<String, Object> host;

  public ArchiveAnnouncement() {}

  // For egg uploads, we identify them via the is_runtimes field in the platform_metadata
  public boolean isRuntimes() {
    if (platformMetadata == null) {
      return false;
    }
    var isRuntimes = String.valueOf(platformMetadata.get("is_runtimes"));
    return isRuntimes == "true" ? true : false;
  }

  // For egg we need to look in the platform_metadata for the URL
  public String getUrl() {
    if (platformMetadata == null) {
      return url;
    }
    return String.valueOf(platformMetadata.get("url"));
  }

  @JsonProperty("url")
  public void setUrl(String url) {
    this.url = url;
  }

  @JsonProperty("org_id")
  public String getOrgId() {
    if (host == null) {
      return this.orgId;
    }
    return String.valueOf(host.get("org_id"));
  }

  @JsonProperty("org_id")
  public void setOrgId(String orgId) {
    this.orgId = orgId;
  }

  @JsonProperty("account")
  public String getAccountId() {
    if (host == null) {
      return this.accountId;
    }
    return String.valueOf(host.get("account"));
  }

  @JsonProperty("account")
  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  ////////////////////////////////////////////////////////////////////////

  @JsonProperty("version")
  public String getVersion() {
    return this.version;
  }

  @JsonProperty("version")
  public void setVersion(String version) {
    this.version = version;
  }

  @JsonProperty("application")
  public String getApplication() {
    return this.application;
  }

  @JsonProperty("application")
  public void setApplication(String application) {
    this.application = application;
  }

  @JsonProperty("timestamp")
  public Instant getTimestamp() {
    return this.timestamp;
  }

  @JsonProperty("timestamp")
  public void setTimestamp(Instant timestamp) {
    this.timestamp = timestamp;
  }

  @JsonProperty("request_id")
  public String getRequestId() {
    return requestId;
  }

  @JsonProperty("request_id")
  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  @JsonProperty("content_type")
  public String getContentType() {
    return contentType;
  }

  @JsonProperty("content_type")
  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  @JsonProperty("platform_metadata")
  public Map<String, Object> getPlatformMetadata() {
    return platformMetadata;
  }

  @JsonProperty("platform_metadata")
  public void setPlatformMetadata(Map<String, Object> platformMetadata) {
    this.platformMetadata = platformMetadata;
  }

  @JsonProperty("host")
  public Map<String, Object> getHost() {
    return host;
  }

  @JsonProperty("host")
  public void setHost(Map<String, Object> host) {
    this.host = host;
  }
}
