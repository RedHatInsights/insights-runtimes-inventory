ALTER TABLE IF EXISTS jvm_instance
  ADD COLUMN workload character varying(255)
    DEFAULT 'Unidentified';

UPDATE jvm_instance SET workload = 'EAP';

ALTER TABLE jvm_instance
  ALTER COLUMN workload SET NOT NULL;
