CREATE TABLE public.runtimes_instance(
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
    heap_max integer NOT NULL,
    created timestamp with time zone DEFAULT now() NOT NULL,
    details jsonb,
    PRIMARY KEY (id)
);

CREATE TABLE public.jar_hash(
    id uuid NOT NULL,
    instance_id uuid NOT NULL,
    name character varying(255) NOT NULL,
    group_id character varying(255) NOT NULL,
    vendor character varying(255) NOT NULL,
    version character varying(255) NOT NULL,
    sha1checksum character varying(255) NOT NULL,
    sha256checksum character varying(255) NOT NULL,
    sha512checksum character varying(255) NOT NULL,
    PRIMARY KEY (id)
);

ALTER TABLE IF EXISTS runtimes_instance
  ADD CONSTRAINT U_LINK_HASH
    UNIQUE (linking_hash);

ALTER TABLE IF EXISTS jar_hash
  ADD CONSTRAINT FK_JAR_HASH_LINK_RUNTIMES_INSTANCE
    FOREIGN KEY (instance_id) REFERENCES runtimes_instance(id)

