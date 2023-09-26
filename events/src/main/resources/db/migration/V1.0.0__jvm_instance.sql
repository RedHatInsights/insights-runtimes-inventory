CREATE TABLE public.jvm_instance(
    id uuid NOT NULL,
    linking_hash character varying(255) NOT NULL,
    account_id character varying(50) NOT NULL,
    org_id character varying(50) NOT NULL,
    hostname character varying(50) NOT NULL,
    launch_time bigint NOT NULL,
    vendor character varying(255) NOT NULL,
    version_string character varying(255) NOT NULL,
    version character varying(255) NOT NULL,
    major_version integer NOT NULL,
    os_arch character varying(50) NOT NULL,
    processors integer NOT NULL,
    heap_min integer NOT NULL,
    heap_max integer NOT NULL,
    java_class_version character varying(255) NOT NULL,
    java_specification_vendor character varying(255) NOT NULL,
    java_vendor character varying(255) NOT NULL,
    java_vendor_version character varying(255) NOT NULL,
    java_vm_name character varying(255) NOT NULL,
    java_vm_vendor character varying(255) NOT NULL,
    jvm_heap_gc_details character varying(255) NOT NULL,
    jvm_pid character varying(255) NOT NULL,
    jvm_report_time character varying(255) NOT NULL,
    system_os_name character varying(255) NOT NULL,
    system_os_version character varying(255) NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    java_home text NOT NULL,
    java_library_path text NOT NULL,
    java_command text NOT NULL,
    java_class_path text NOT NULL,
    jvm_packages text NOT NULL,
    jvm_args text NOT NULL,
    details jsonb,
    PRIMARY KEY (id)
);

ALTER TABLE IF EXISTS jvm_instance
  ADD CONSTRAINT U_LINK_HASH
    UNIQUE (linking_hash);

CREATE TABLE public.jar_hash(
    id uuid NOT NULL,
    name character varying(255) NOT NULL,
    group_id character varying(255) NOT NULL,
    vendor character varying(255) NOT NULL,
    version character varying(255) NOT NULL,
    sha1checksum character varying(255) NOT NULL,
    sha256checksum character varying(255) NOT NULL,
    sha512checksum character varying(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE public.eap_instance(
    id uuid NOT NULL,
    app_client_exception character varying(255) NOT NULL,
    app_name character varying(255) NOT NULL,
    app_transport_cert_https character varying(255) NOT NULL,
    app_transport_type_file character varying(255) NOT NULL,
    app_transport_type_https character varying(255) NOT NULL,
    app_user_dir character varying(255) NOT NULL,
    app_user_name character varying(255) NOT NULL,
    eap_version character varying(255) NOT NULL,
    eap_xp boolean NOT NULL,
    eap_yaml_extension boolean NOT NULL,
    eap_bootable_jar boolean NOT NULL,
    eap_use_git boolean NOT NULL,
    raw text NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE public.eap_configuration(
    id uuid NOT NULL,
    eap_instance_id uuid NOT NULL,
    version character varying(255) NOT NULL,
    launch_type character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    organization character varying(255) NOT NULL,
    process_type character varying(255) NOT NULL,
    product_name character varying(255) NOT NULL,
    product_version character varying(255) NOT NULL,
    profile_name character varying(255) NOT NULL,
    release_codename character varying(255) NOT NULL,
    release_version character varying(255) NOT NULL,
    running_mode character varying(255) NOT NULL,
    runtime_configuration_state character varying(255) NOT NULL,
    server_state character varying(255) NOT NULL,
    suspend_state character varying(255) NOT NULL,
    socket_binding_groups character varying NOT NULL,
    paths text NOT NULL,
    interfaces text NOT NULL,
    core_services text NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE public.eap_configuration_deployments(
    eap_configuration_id uuid NOT NULL,
    deployments_key character varying(255) NOT NULL,
    deployments text NOT NULL,
    PRIMARY KEY (eap_configuration_id, deployments_key)
);

CREATE TABLE public.eap_configuration_subsystems(
    eap_configuration_id uuid NOT NULL,
    subsystems_key character varying(255) NOT NULL,
    subsystems text NOT NULL,
    PRIMARY KEY (eap_configuration_id, subsystems_key)
);

CREATE TABLE public.eap_extension(
    id uuid NOT NULL,
    module character varying(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE public.eap_extension_subsystems(
    eap_extension_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    version character varying(255) NOT NULL,
    PRIMARY KEY (eap_extension_id, name, version)
);

CREATE TABLE public.eap_configuration_eap_extension(
    eap_configuration_id uuid NOT NULL,
    eap_extension_id uuid NOT NULL,
    PRIMARY KEY (eap_configuration_id, eap_extension_id),
    CONSTRAINT FK_EAP_CONFIGURATION FOREIGN KEY (eap_configuration_id) REFERENCES eap_configuration (id),
    CONSTRAINT FK_EAP_EXTENSION FOREIGN KEY (eap_extension_id) REFERENCES eap_extension (id)
);

ALTER TABLE IF EXISTS eap_configuration
  ADD CONSTRAINT FK_EAP_CONFIGURATION_LINK_EAP_INSTANCE
    FOREIGN KEY (eap_instance_id) REFERENCES eap_instance(id);

CREATE TABLE public.eap_deployment(
    id uuid NOT NULL,
    eap_instance_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    PRIMARY KEY (id)
);

ALTER TABLE IF EXISTS eap_deployment
  ADD CONSTRAINT FK_EAP_DEPLOYMENT_LINK_EAP_INSTANCE
    FOREIGN KEY (eap_instance_id) REFERENCES eap_instance(id);

-- JarHash Tables
CREATE TABLE public.jvm_instance_jar_hash(
    jvm_instance_id uuid NOT NULL,
    jar_hash_id uuid NOT NULL,
    PRIMARY KEY (jvm_instance_id, jar_hash_id),
    CONSTRAINT FK_JVM_INSTANCE FOREIGN KEY (jvm_instance_id) REFERENCES jvm_instance (id),
    CONSTRAINT FK_JAR_HASH FOREIGN KEY (jar_hash_id) REFERENCES jar_hash (id)
);
-- Don't need an eap_instance_jar_hash because it already uses
-- jvm_instance_jar_hash since it is a child class of it
-- CREATE TABLE public.eap_instance_jar_hash(
    -- eap_instance_id uuid NOT NULL,
    -- jar_hash_id uuid NOT NULL,
    -- PRIMARY KEY (eap_instance_id, jar_hash_id),
    -- CONSTRAINT FK_EAP_INSTANCE FOREIGN KEY (eap_instance_id) REFERENCES eap_instance (id),
    -- CONSTRAINT FK_JAR_HASH FOREIGN KEY (jar_hash_id) REFERENCES jar_hash (id)
-- );
CREATE TABLE public.eap_instance_module_jar_hash(
    eap_instance_id uuid NOT NULL,
    jar_hash_id uuid NOT NULL,
    PRIMARY KEY (eap_instance_id, jar_hash_id),
    CONSTRAINT FK_EAP_INSTANCE FOREIGN KEY (eap_instance_id) REFERENCES eap_instance (id),
    CONSTRAINT FK_JAR_HASH FOREIGN KEY (jar_hash_id) REFERENCES jar_hash (id)
);
CREATE TABLE public.eap_deployment_archive_jar_hash(
    eap_deployment_id uuid NOT NULL,
    jar_hash_id uuid NOT NULL,
    PRIMARY KEY (eap_deployment_id, jar_hash_id),
    CONSTRAINT FK_EAP_DEPLOYMENT FOREIGN KEY (eap_deployment_id) REFERENCES eap_deployment (id),
    CONSTRAINT FK_JAR_HASH FOREIGN KEY (jar_hash_id) REFERENCES jar_hash (id)
);

-- Many To Many relationships can leave orphans, so we'll create some triggers
-- to handle cleaning them up

/*
 *CREATE OR REPLACE FUNCTION clean_jar_hash_orphans()
 *    RETURNS TRIGGER
 *    LANGUAGE plpgsql VOLATILE PARALLEL UNSAFE
 *    AS $$
 *    BEGIN
 *        DELETE FROM jar_hash WHERE
 *        id NOT IN (
 *            SELECT DISTINCT jar_hash_id FROM jvm_instance_jar_hash
 *            UNION
 *            SELECT DISTINCT jar_hash_id FROM eap_instance_module_jar_hash
 *            UNION
 *            SELECT DISTINCT jar_hash_id FROM eap_deployment_archive_jar_hash
 *        );
 *        RETURN NULL;
 *    END
 *    $$;
 *
 *CREATE OR REPLACE TRIGGER tr_clean_jar_hash_orphens
 *AFTER DELETE ON jvm_instance_jar_hash
 *FOR EACH STATEMENT
 *EXECUTE FUNCTION clean_jar_hash_orphans();
 */
