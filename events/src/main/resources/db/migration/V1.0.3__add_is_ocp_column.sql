-- This script is used to add the is_ocp column to the jvm_instance table.

-- is_ocp is a boolean column that indicates whether the workload is running on OpenShift.
-- This will be true if the payload was generated from an agent running in OpenShift and false
-- otherwise (until we support EAP in OCP or other combinations).
ALTER TABLE IF EXISTS jvm_instance
  ADD COLUMN is_ocp boolean
    DEFAULT FALSE;

UPDATE jvm_instance SET is_ocp = FALSE;

ALTER TABLE jvm_instance
  ALTER COLUMN is_ocp SET NOT NULL;
