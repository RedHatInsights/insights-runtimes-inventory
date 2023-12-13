-- This script is used to patch some data that was incorrectly updated in a previous TO
UPDATE jvm_instance SET workload = 'TMP-EAP', is_ocp = FALSE
                    WHERE workload = 'Quarkus' AND is_ocp = TRUE;

UPDATE jvm_instance SET workload = 'Quarkus', is_ocp = TRUE
                    WHERE workload = 'EAP' AND is_ocp = FALSE;

UPDATE jvm_instance SET workload = 'EAP' WHERE workload = 'TMP-EAP';

UPDATE jvm_instance SET is_ocp = TRUE
                    WHERE workload = 'Unidentified' AND is_ocp = FALSE;

