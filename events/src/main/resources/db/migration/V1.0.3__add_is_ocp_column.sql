-- This script is used to add the is_ocp column to the jvm_instance table.

-- is_ocp is a boolean column that indicates whether the workload is running on OpenShift.
-- This will be true if the payload was generated from an agent running in OpenShift and false
-- otherwise (until we support EAP in OCP or other combinations).
ALTER TABLE IF EXISTS jvm_instance
  ADD COLUMN is_ocp boolean
    DEFAULT FALSE;

UPDATE jvm_instance SET is_ocp = FALSE;

-- Cleanup from small amount of Cryostat data that was inserted before the column was added.
UPDATE jvm_instance SET workload = 'Quarkus'
                    FROM jvm_instance jvm, jar_hash hash, jvm_instance_jar_hash link
                    WHERE jvm.id = link.jvm_instance_id
                      AND link.jar_hash_id = hash.id
                      AND hash.name = 'quarkus-run.jar';

UPDATE jvm_instance SET is_ocp = TRUE
           FROM jvm_instance jvm, jar_hash hash, jvm_instance_jar_hash link
           WHERE jvm.id = link.jvm_instance_id
             AND link.jar_hash_id = hash.id
             AND hash.name = 'quarkus-run.jar';

UPDATE jvm_instance SET is_ocp = TRUE
                    FROM jvm_instance jvm, jar_hash hash, jvm_instance_jar_hash link
                    WHERE jvm.id = link.jvm_instance_id
                      AND link.jar_hash_id = hash.id
                      AND hash.name = 'jenkins.war';

ALTER TABLE jvm_instance
  ALTER COLUMN is_ocp SET NOT NULL;
