--
-- PostgreSQL database ksamsok
--

CREATE DATABASE ksamsok
  WITH OWNER = "ksamsok_adm"
       ENCODING = 'UTF8'
       LC_COLLATE = 'sv_SE.UTF-8'
       LC_CTYPE = 'sv_SE.UTF-8'
    TABLESPACE = pg_default
    CONNECTION LIMIT = -1;
REVOKE ALL ON DATABASE ksamsok FROM public; -- see notes below!
GRANT CONNECT ON DATABASE ksamsok TO ksamsok_read; -- others inherit

\connect ksamsok;


-- CREATE SCHEMA ksamsok AUTHORIZATION lamning_adm;
DROP SCHEMA public;
CREATE SCHEMA ksamsok AUTHORIZATION ksamsok_adm;
SET search_path = ksamsok;

-- CREATE SCHEMA ksamsok AUTHORIZATION ksamsok;

ALTER ROLE ksamsok_adm IN DATABASE ksamsok SET search_path = ksamsok;  
ALTER ROLE ksamsok_read IN DATABASE ksamsok SET search_path = ksamsok;  
ALTER ROLE ksamsok_usr IN DATABASE ksamsok SET search_path = ksamsok;  
GRANT USAGE ON SCHEMA ksamsok TO ksamsok_read;
GRANT CREATE ON SCHEMA ksamsok TO ksamsok_adm;
ALTER DEFAULT PRIVILEGES FOR ROLE ksamsok_adm
GRANT SELECT ON TABLES TO ksamsok_read;  
ALTER DEFAULT PRIVILEGES FOR ROLE ksamsok_adm
GRANT INSERT, UPDATE, DELETE, TRUNCATE ON TABLES TO ksamsok_usr;
ALTER DEFAULT PRIVILEGES FOR ROLE ksamsok_adm
GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO ksamsok_usr;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
 
SET statement_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = off;
SET check_function_bodies = false;
SET client_min_messages = warning;
SET escape_string_warning = off; 
SET search_path = public, pg_catalog; 
SET search_path = public, pg_catalog; 
SET default_tablespace = ''; 
SET default_with_oids = false;
  
--
-- Name: content; Type: TABLE; Schema: ksamsok; Owner: ksamsok_adm; Tablespace:
--
 
CREATE TABLE ksamsok.content (
    uri character varying(1024) NOT NULL,
    oaiuri character varying(1024),
    serviceid character varying(20),
    xmldata text,
    changed timestamp without time zone,
    added timestamp without time zone,
    deleted timestamp without time zone,
    datestamp timestamp without time zone NOT NULL,
    status bigint,
    idnum bigint NOT NULL,
    nativeurl character varying(1024)
);
 
 
ALTER TABLE ksamsok.content OWNER TO ksamsok_adm;
 
--
-- Name: ksamsok.content_idnum_seq; Type: SEQUENCE; Schema: ksamsok; Owner: ksamsok_adm
--
 
CREATE SEQUENCE ksamsok.content_idnum_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
 
 
ALTER TABLE ksamsok.content_idnum_seq OWNER TO ksamsok_adm;
 
--
-- Name: ksamsok.content_idnum_seq; Type: SEQUENCE OWNED BY; Schema: ksamsok; Owner: ksamsok_adm
--
 
ALTER SEQUENCE ksamsok.content_idnum_seq OWNED BY ksamsok.content.idnum;
 
--
-- Name: harvestservices; Type: TABLE; Schema: ksamsok; Owner: ksamsok_adm; Tablespace:
--
 
CREATE TABLE ksamsok.harvestservices (
    serviceid character varying(20) NOT NULL,
    servicetype character varying(20),
    name character varying(200),
    harvesturl character varying(4000),
    harvestsetspec character varying(50),
    cronstring character varying(50),
    lastharvestdate timestamp without time zone,
    alwayseverything boolean,
    firstindexdate timestamp without time zone,
    kortnamn character varying(20),
    beskrivning character varying(2000),
    paused boolean
);
 
 
ALTER TABLE ksamsok.harvestservices OWNER TO ksamsok_adm;
 
--
-- Name: organisation; Type: TABLE; Schema: ksamsok; Owner: ksamsok_adm; Tablespace:
--
 
CREATE TABLE ksamsok.organisation (
    kortnamn character varying(20) NOT NULL,
    beskrivswe character varying(2000),
    beskriveng character varying(2000),
    adress1 character varying(32),
    adress2 character varying(32),
    postadress character varying(32),
    kontaktperson character varying(32),
    epostkontaktperson character varying(256),
    websida character varying(256),
    websidaks character varying(256),
    lowressurl character varying(256),
    thumbnailurl character varying(256),
    namnswe character varying(100),
    namneng character varying(100),
    pass character varying(30),
    serv_org character varying(20)
);
 
 
ALTER TABLE ksamsok.organisation OWNER TO ksamsok_adm;

--
-- Name: servicelog; Type: TABLE; Schema: ksamsok; Owner: ksamsok_adm; Tablespace:
--
 
CREATE TABLE ksamsok.servicelog (
    serviceid character varying(20),
    eventtype bigint,
    eventstep character varying(20),
    eventts timestamp without time zone,
    message character varying(4000),
    eventid bigint NOT NULL
);
 
 
ALTER TABLE ksamsok.servicelog OWNER TO ksamsok_adm;
 
--
-- Name: servicelog_eventid_seq; Type: SEQUENCE; Schema: ksamsok; Owner: ksamsok_adm
--
 
CREATE SEQUENCE ksamsok.servicelog_eventid_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;
 
 
ALTER TABLE ksamsok.servicelog_eventid_seq OWNER TO ksamsok_adm;
 
--
-- Name: servicelog_eventid_seq; Type: SEQUENCE OWNED BY; Schema: ksamsok; Owner: ksamsok_adm
--
 
ALTER SEQUENCE ksamsok.servicelog_eventid_seq OWNED BY ksamsok.servicelog.eventid;
 
 
--
-- Name: idnum; Type: DEFAULT; Schema: ksamsok; Owner: ksamsok_adm
--
 
ALTER TABLE ksamsok.content ALTER COLUMN idnum SET DEFAULT nextval('ksamsok.content_idnum_seq'::regclass);
 
 
--
-- Name: eventid; Type: DEFAULT; Schema: ksamsok; Owner: ksamsok_adm
--
 
ALTER TABLE ksamsok.servicelog ALTER COLUMN eventid SET DEFAULT nextval('ksamsok.servicelog_eventid_seq'::regclass);
 
  
--
-- Name: content_pkey; Type: CONSTRAINT; Schema: ksamsok; Owner: ksamsok_adm; Tablespace:
--
 
ALTER TABLE ONLY ksamsok.content
    ADD CONSTRAINT content_pkey PRIMARY KEY (uri);
 
 
--
-- Name: organisation_pkey; Type: CONSTRAINT; Schema: ksamsok; Owner: ksamsok_adm; Tablespace:
--
 
ALTER TABLE ONLY ksamsok.organisation
    ADD CONSTRAINT organisation_pkey PRIMARY KEY (kortnamn);
 
 
--
-- Name: pk_harvestservices; Type: CONSTRAINT; Schema: ksamsok; Owner: ksamsok_adm; Tablespace:
--
 
ALTER TABLE ONLY ksamsok.harvestservices
    ADD CONSTRAINT pk_harvestservices PRIMARY KEY (serviceid);
 
 
--
-- Name: pk_servicelog; Type: CONSTRAINT; Schema: ksamsok; Owner: ksamsok_adm; Tablespace:
--
 
ALTER TABLE ONLY ksamsok.servicelog
    ADD CONSTRAINT pk_servicelog PRIMARY KEY (eventid);
 
 
--
-- Name: ix_content_deleted; Type: INDEX; Schema: ksamsok; Owner: ksamsok_adm; Tablespace:
--
 
CREATE INDEX ix_content_deleted ON ksamsok.content USING btree (deleted);
 
 
--
-- Name: ix_content_oai; Type: INDEX; Schema: ksamsok; Owner: ksamsok_adm; Tablespace:
--
 
CREATE INDEX ix_content_oai ON ksamsok.content USING btree (oaiuri);
 
 
--
-- Name: ix_content_serv; Type: INDEX; Schema: ksamsok; Owner: ksamsok_adm; Tablespace:
--
 
CREATE INDEX ix_content_serv ON ksamsok.content USING btree (serviceid);
 
 
--
-- Name: ix_content_serv_changed; Type: INDEX; Schema: ksamsok; Owner: ksamsok_adm; Tablespace:
--
 
CREATE INDEX ix_content_serv_changed ON ksamsok.content USING btree (serviceid, changed);
 
 
--
-- Name: ix_content_serv_deleted; Type: INDEX; Schema: ksamsok; Owner: ksamsok_adm; Tablespace:
--
 
CREATE INDEX ix_content_serv_deleted ON ksamsok.content USING btree (serviceid, deleted);
 
 
--
-- Name: ix_content_serv_deleted_uri; Type: INDEX; Schema: ksamsok; Owner: ksamsok_adm; Tablespace:
--
 
CREATE INDEX ix_content_serv_deleted_uri ON ksamsok.content USING btree (serviceid, uri) WHERE (deleted IS NULL);
 
 
--
-- Name: ix_content_uri_serv; Type: INDEX; Schema: ksamsok; Owner: ksamsok_adm; Tablespace:
--
 
CREATE INDEX ix_content_uri_serv ON ksamsok.content USING btree (uri, serviceid);
 
 
--
-- Name: ix_servicelog_serv; Type: INDEX; Schema: ksamsok; Owner: ksamsok_adm; Tablespace:
--
 
CREATE INDEX ix_servicelog_serv ON ksamsok.servicelog USING btree (serviceid);
  
 
--
-- Name: fk_kortnamn_organisation; Type: FK CONSTRAINT; Schema: ksamsok; Owner: ksamsok_adm
--
 
ALTER TABLE ONLY ksamsok.harvestservices
    ADD CONSTRAINT fk_kortnamn_organisation FOREIGN KEY (kortnamn) REFERENCES ksamsok.organisation(kortnamn);
 
 
--
-- Name: ksamsok; Type: ACL; Schema: -; Owner: ksamsok_adm
--
 
REVOKE ALL ON SCHEMA ksamsok FROM ksamsok_adm;
GRANT ALL ON SCHEMA ksamsok TO ksamsok_adm;
  
--
-- Name: content; Type: ACL; Schema: ksamsok; Owner: ksamsok_adm
--
 
-- REVOKE ALL ON TABLE content FROM ksamsok;
REVOKE ALL ON TABLE ksamsok.content FROM ksamsok_adm;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE ksamsok.content TO ksamsok_adm;
GRANT ALL ON TABLE ksamsok.content TO ksamsok_adm;
 
 
--
-- Name: ksamsok.content_idnum_seq; Type: ACL; Schema: ksamsok; Owner: ksamsok_adm
--
 
REVOKE ALL ON SEQUENCE ksamsok.content_idnum_seq FROM ksamsok_adm;
GRANT ALL ON SEQUENCE ksamsok.content_idnum_seq TO ksamsok_adm;
GRANT SELECT,UPDATE ON SEQUENCE ksamsok.content_idnum_seq TO ksamsok_adm;
 
 
--
-- Name: harvestservices; Type: ACL; Schema: ksamsok; Owner: ksamsok_adm
--
 
-- REVOKE ALL ON TABLE harvestservices FROM ksamsok;
REVOKE ALL ON TABLE ksamsok.harvestservices FROM ksamsok_adm;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE ksamsok.harvestservices TO ksamsok_adm;
GRANT ALL ON TABLE ksamsok.harvestservices TO ksamsok_adm;
 
 
--
-- Name: organisation; Type: ACL; Schema: ksamsok; Owner: ksamsok_adm
--
 
REVOKE ALL ON TABLE ksamsok.organisation FROM ksamsok_adm;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE ksamsok.organisation TO ksamsok_adm;
GRANT ALL ON TABLE ksamsok.organisation TO ksamsok_adm; 
  
 
--
-- Name: servicelog; Type: ACL; Schema: ksamsok; Owner: ksamsok_adm
--
 
REVOKE ALL ON TABLE ksamsok.servicelog FROM ksamsok_adm;
GRANT SELECT,INSERT,DELETE,UPDATE ON TABLE ksamsok.servicelog TO ksamsok_adm;
GRANT ALL ON TABLE ksamsok.servicelog TO ksamsok_adm;
 
 
--
-- Name: servicelog_eventid_seq; Type: ACL; Schema: ksamsok; Owner: ksamsok_adm
--
 
REVOKE ALL ON SEQUENCE ksamsok.servicelog_eventid_seq FROM ksamsok_adm;
GRANT ALL ON SEQUENCE ksamsok.servicelog_eventid_seq TO ksamsok_adm;
GRANT SELECT,UPDATE ON SEQUENCE ksamsok.servicelog_eventid_seq TO ksamsok_adm;
 
 
--
-- Name: DEFAULT PRIVILEGES FOR SEQUENCES; Type: DEFAULT ACL; Schema: -; Owner: ksamsok_adm
--
 
ALTER DEFAULT PRIVILEGES FOR ROLE ksamsok_adm GRANT SELECT,UPDATE ON SEQUENCES  TO ksamsok_adm;
 
 
--
-- Name: DEFAULT PRIVILEGES FOR FUNCTIONS; Type: DEFAULT ACL; Schema: -; Owner: ksamsok_adm
--
 
ALTER DEFAULT PRIVILEGES FOR ROLE ksamsok_adm REVOKE ALL ON FUNCTIONS  FROM ksamsok_adm;
ALTER DEFAULT PRIVILEGES FOR ROLE ksamsok_adm GRANT ALL ON FUNCTIONS  TO ksamsok_adm;
 
 
--
-- Name: DEFAULT PRIVILEGES FOR TABLES; Type: DEFAULT ACL; Schema: -; Owner: ksamsok_adm
--
 
ALTER DEFAULT PRIVILEGES FOR ROLE ksamsok_adm GRANT SELECT,INSERT,DELETE,UPDATE ON TABLES  TO ksamsok_adm;
 
 
--
-- PostgreSQL database ksamsok complete
--


GRANT SELECT ON ALL TABLES IN SCHEMA ksamsok TO ksamsok_read;
GRANT INSERT, UPDATE, DELETE, TRUNCATE ON ALL TABLES IN SCHEMA ksamsok TO ksamsok_usr;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA ksamsok TO ksamsok_usr;
