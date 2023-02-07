CREATE TABLE public.runtimes_instance(
    id uuid NOT NULL,
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
    details jsonb
);
