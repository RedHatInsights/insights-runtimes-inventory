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

  public void setId(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }
}
