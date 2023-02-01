package com.redhat.runtimes.inventory.models;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Entity
@Table(name = "instance")
public class RuntimesInstance {

  @Id
  private String id;

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
  private double heapMax;

  //////////////////////////////////////////////////////

  public void setId(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }
}
