/* Copyright (C) Red Hat 2023 */
package com.redhat.runtimes.inventory.models;

public sealed interface InsightsMessage permits RuntimesInstance, UpdateInstance {}
