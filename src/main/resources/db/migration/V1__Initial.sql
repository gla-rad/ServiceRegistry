--
-- PostgreSQL database dump
--

-- Dumped from database version 16.9 (Ubuntu 16.9-0ubuntu0.24.04.1)
-- Dumped by pg_dump version 16.9 (Ubuntu 16.9-0ubuntu0.24.04.1)

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: postgis; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS postgis WITH SCHEMA public;


--
-- Name: EXTENSION postgis; Type: COMMENT; Schema: -; Owner:
--

COMMENT ON EXTENSION postgis IS 'PostGIS geometry, geography, and raster spatial types and functions';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: document; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.document (
    id bigint NOT NULL,
    comment character varying(255),
    filecontent oid NOT NULL,
    filecontent_content_type character varying(255) NOT NULL,
    mimetype character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    instance_id bigint
);


ALTER TABLE public.document OWNER TO admin;

--
-- Name: document_id_seq; Type: SEQUENCE; Schema: public; Owner: admin
--

CREATE SEQUENCE public.document_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.document_id_seq OWNER TO admin;

--
-- Name: document_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: admin
--

ALTER SEQUENCE public.document_id_seq OWNED BY public.document.id;


--
-- Name: instance; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.instance (
    id bigint NOT NULL,
    comment character varying(255) NOT NULL,
    endpoint_type character varying(255),
    endpoint_uri character varying(255),
    geometry public.geometry,
    geometry_content_type character varying(255),
    imo character varying(255),
    instance_id character varying(255) NOT NULL,
    last_updated_at timestamp(6) without time zone,
    mmsi character varying(255),
    name character varying(255) NOT NULL,
    organization_id character varying(255),
    published_at timestamp(6) without time zone,
    status character varying(30) DEFAULT 'provisional'::character varying NOT NULL,
    version character varying(255) NOT NULL,
    instance_as_doc_id bigint,
    instance_as_xml_id bigint,
    data_product_type integer
);


ALTER TABLE public.instance OWNER TO admin;

--
-- Name: instance_data_product_type; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.instance_data_product_type (
    instance_id bigint NOT NULL,
    data_product_type character varying(255)
);


ALTER TABLE public.instance_data_product_type OWNER TO admin;

--
-- Name: instance_designs; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.instance_designs (
    instance_id bigint NOT NULL,
    designs character varying(255),
    designs_key character varying(255) NOT NULL
);


ALTER TABLE public.instance_designs OWNER TO admin;

--
-- Name: instance_id_seq; Type: SEQUENCE; Schema: public; Owner: admin
--

CREATE SEQUENCE public.instance_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.instance_id_seq OWNER TO admin;

--
-- Name: instance_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: admin
--

ALTER SEQUENCE public.instance_id_seq OWNED BY public.instance.id;


--
-- Name: instance_keywords; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.instance_keywords (
    instance_id bigint NOT NULL,
    keywords character varying(255)
);


ALTER TABLE public.instance_keywords OWNER TO admin;

--
-- Name: instance_service_type; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.instance_service_type (
    instance_id bigint NOT NULL,
    service_type character varying(255)
);


ALTER TABLE public.instance_service_type OWNER TO admin;

--
-- Name: instance_specifications; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.instance_specifications (
    instance_id bigint NOT NULL,
    specifications character varying(255),
    specifications_key character varying(255) NOT NULL
);


ALTER TABLE public.instance_specifications OWNER TO admin;

--
-- Name: instance_unlocode; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.instance_unlocode (
    instance_id bigint NOT NULL,
    unlocode character varying(255)
);


ALTER TABLE public.instance_unlocode OWNER TO admin;

--
-- Name: ledgerrequest; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.ledgerrequest (
    id bigint NOT NULL,
    created_at timestamp(6) without time zone,
    last_updated_at timestamp(6) without time zone,
    reason character varying(255),
    status character varying(30) DEFAULT 'created'::character varying,
    instance_id bigint NOT NULL
);


ALTER TABLE public.ledgerrequest OWNER TO admin;

--
-- Name: ledgerrequest_id_seq; Type: SEQUENCE; Schema: public; Owner: admin
--

CREATE SEQUENCE public.ledgerrequest_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.ledgerrequest_id_seq OWNER TO admin;

--
-- Name: ledgerrequest_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: admin
--

ALTER SEQUENCE public.ledgerrequest_id_seq OWNED BY public.ledgerrequest.id;


--
-- Name: xml; Type: TABLE; Schema: public; Owner: admin
--

CREATE TABLE public.xml (
    id bigint NOT NULL,
    comment character varying(255),
    content text NOT NULL,
    content_content_type character varying(255) NOT NULL,
    name character varying(255) NOT NULL
);


ALTER TABLE public.xml OWNER TO admin;

--
-- Name: xml_id_seq; Type: SEQUENCE; Schema: public; Owner: admin
--

CREATE SEQUENCE public.xml_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.xml_id_seq OWNER TO admin;

--
-- Name: xml_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: admin
--

ALTER SEQUENCE public.xml_id_seq OWNED BY public.xml.id;


--
-- Name: document id; Type: DEFAULT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.document ALTER COLUMN id SET DEFAULT nextval('public.document_id_seq'::regclass);


--
-- Name: instance id; Type: DEFAULT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.instance ALTER COLUMN id SET DEFAULT nextval('public.instance_id_seq'::regclass);


--
-- Name: ledgerrequest id; Type: DEFAULT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.ledgerrequest ALTER COLUMN id SET DEFAULT nextval('public.ledgerrequest_id_seq'::regclass);


--
-- Name: xml id; Type: DEFAULT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.xml ALTER COLUMN id SET DEFAULT nextval('public.xml_id_seq'::regclass);


--
-- Name: document document_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.document
    ADD CONSTRAINT document_pkey PRIMARY KEY (id);


--
-- Name: instance_designs instance_designs_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.instance_designs
    ADD CONSTRAINT instance_designs_pkey PRIMARY KEY (instance_id, designs_key);


--
-- Name: instance instance_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.instance
    ADD CONSTRAINT instance_pkey PRIMARY KEY (id);


--
-- Name: instance_specifications instance_specifications_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.instance_specifications
    ADD CONSTRAINT instance_specifications_pkey PRIMARY KEY (instance_id, specifications_key);


--
-- Name: ledgerrequest ledgerrequest_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.ledgerrequest
    ADD CONSTRAINT ledgerrequest_pkey PRIMARY KEY (id);


--
-- Name: instance mrn_version_constraint; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.instance
    ADD CONSTRAINT mrn_version_constraint UNIQUE (instance_id, version);


--
-- Name: instance uk_gq32irmf5yu82sx86ih3fgsnt; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.instance
    ADD CONSTRAINT uk_gq32irmf5yu82sx86ih3fgsnt UNIQUE (instance_as_doc_id);


--
-- Name: instance uk_swfuwkflpfywn6qlwxlpe5bkb; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.instance
    ADD CONSTRAINT uk_swfuwkflpfywn6qlwxlpe5bkb UNIQUE (instance_as_xml_id);


--
-- Name: xml xml_pkey; Type: CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.xml
    ADD CONSTRAINT xml_pkey PRIMARY KEY (id);


--
-- Name: instance fk28hf3yf6p0k18v1epodb1mqmk; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.instance
    ADD CONSTRAINT fk28hf3yf6p0k18v1epodb1mqmk FOREIGN KEY (instance_as_doc_id) REFERENCES public.document(id);


--
-- Name: instance fk385xaqb9hb5lihe7gqrbh8s1v; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.instance
    ADD CONSTRAINT fk385xaqb9hb5lihe7gqrbh8s1v FOREIGN KEY (instance_as_xml_id) REFERENCES public.xml(id);


--
-- Name: instance_designs fk447t5jpf2nvff5wpwjqyll5in; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.instance_designs
    ADD CONSTRAINT fk447t5jpf2nvff5wpwjqyll5in FOREIGN KEY (instance_id) REFERENCES public.instance(id);


--
-- Name: document fk7lj6kapjrnq817tu3cqaddb68; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.document
    ADD CONSTRAINT fk7lj6kapjrnq817tu3cqaddb68 FOREIGN KEY (instance_id) REFERENCES public.instance(id);


--
-- Name: instance_service_type fk9jt9kmu6hfdpopje2tfru22la; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.instance_service_type
    ADD CONSTRAINT fk9jt9kmu6hfdpopje2tfru22la FOREIGN KEY (instance_id) REFERENCES public.instance(id);


--
-- Name: instance_specifications fkb1qhu7u3ax3y0u5q1wmluoff5; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.instance_specifications
    ADD CONSTRAINT fkb1qhu7u3ax3y0u5q1wmluoff5 FOREIGN KEY (instance_id) REFERENCES public.instance(id);


--
-- Name: instance_data_product_type fkba03pdlgvr1gfyjdve50tnl70; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.instance_data_product_type
    ADD CONSTRAINT fkba03pdlgvr1gfyjdve50tnl70 FOREIGN KEY (instance_id) REFERENCES public.instance(id);


--
-- Name: ledgerrequest fkh19ua4lgk2h63jhpyjqxcm6yf; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.ledgerrequest
    ADD CONSTRAINT fkh19ua4lgk2h63jhpyjqxcm6yf FOREIGN KEY (instance_id) REFERENCES public.instance(id);


--
-- Name: instance_keywords fkkg6qrgf97q3xlhwlujwku0n43; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.instance_keywords
    ADD CONSTRAINT fkkg6qrgf97q3xlhwlujwku0n43 FOREIGN KEY (instance_id) REFERENCES public.instance(id);


--
-- Name: instance_unlocode fkrkqiysf8pmo0pmuv505lv8unm; Type: FK CONSTRAINT; Schema: public; Owner: admin
--

ALTER TABLE ONLY public.instance_unlocode
    ADD CONSTRAINT fkrkqiysf8pmo0pmuv505lv8unm FOREIGN KEY (instance_id) REFERENCES public.instance(id);


--
-- Name: SCHEMA public; Type: ACL; Schema: -; Owner: postgres
--

REVOKE USAGE ON SCHEMA public FROM PUBLIC;
GRANT ALL ON SCHEMA public TO PUBLIC;


--
-- PostgreSQL database dump complete
--
