-- enkelt repository

-- För oracle, data i default tablespace, index i "KSAMSOK_INDX"
-- för tex Derby eller postgresql krävs små ändringar (postgres stöds ej fn, se javakod)

--drop table harvestservices

CREATE TABLE apikeys {
apikey varchar2(30) NOT NULL PRIMARY KEY,
owner varchar2(100) NOT NULL,
total INTEGER
};

CREATE TABLE searches {
apikey varchar2(39) NOT NULL PRIMARY KEY,
searchstring varchar2(100) NOT NULL PRIMARY KEY,
param varchar2(30) NOT NULL PRIMARY KEY,
count NUMBER
};

CREATE TABLE organisation {
kortnamn varchar2(20) NOT NULL PRIMARY KEY,
beskrivswe varchar2(2000),
beskriveng varchar2(2000),
adress1 varchar2(32),
adress2 varchar2(32),
postadress varchar2(32),
kontaktperson varchar2(32),
epostkontaktperson varchar2(256),
websida varchar2(256),
websidaks varchar2(256),
lowressurl varchar2(256),
thumbnailurl varchar2(256),
namnswe varchar2(100),
namneng varchar2(100),
pass varchar2(30)
serv_org varchar2(20)
};

create table harvestservices (
serviceId varchar2(20) NOT NULL,
serviceType varchar2(20),
name varchar2(200),
harvestURL varchar2(4000),
harvestSetSpec varchar2(50),
cronstring varchar2(50),
lastHarvestDate timestamp(6),
firstIndexDate timestamp(6),
kortnamn varchar2(20),
alwayseverything integer -- boolean
);

alter table harvestservices add constraint pk_harvestservices primary key (serviceId) using index tablespace KSAMSOK_INDX;

--drop table content
create table content (
uri varchar2(1024),
oaiuri varchar2(1024),
serviceId varchar2(20),
changed timestamp(6),
xmldata clob,
added timestamp(6),
deleted timestamp(6),
datestamp timestamp(6) not null,
status integer not null
);

alter table content add constraint pk_content primary key (uri) using index tablespace KSAMSOK_INDX;
create index ix_content_serv_oai on content (serviceId, oaiURI) tablespace KSAMSOK_INDX;
create index ix_content_serv on content (serviceId) tablespace KSAMSOK_INDX;
create index ix_content_oai on content (oaiURI) tablespace KSAMSOK_INDX;
create index ix_content_serv_status on content (serviceId, status) tablespace KSAMSOK_INDX;
create index ix_content_serv_deleted on content (serviceId, deleted) tablespace KSAMSOK_INDX;
create index ix_content_uri_serv on content (uri, serviceId) tablespace KSAMSOK_INDX;
create index ix_content_date on content (datestamp) tablespace KSAMSOK_INDX;
create index ix_content_deleted on content (deleted, ' ') tablespace KSAMSOK_INDX;
CREATE INDEX ix_apikeys_apikey ON apikeys (apikey) tablespace KSAMSOK_INDEX;
CREATE INDEX ix_organisation_kortnamn ON organisation (kortnamn) tablespace KSAMSOK_INDEX;
CREATE INDEX ix_searches_pk ON searches (apikey, searchstring, param) tablespace KSAMSOK_INDEX;

ALTER TABLE harvestservices ADD CONSTRAINT fk_organisation_kortnamn FOREIGN KEY(kortnamn) REFERENCES organisation(kortnamn);
ALTER TABLE searches ADD CONSTRAINT fk_apikeys FOREIGN KEY(apikey) REFERENCES apikeys(apikey);


--drop table servicelog
create table servicelog (
eventId integer not null,
serviceId varchar2(20),
eventType integer,
eventStep varchar2(20),
eventTs timestamp(6),
message varchar2(4000)
);


alter table servicelog add constraint pk_servicelog primary key (eventId) using index tablespace KSAMSOK_INDX;
create index ix_servicelog_serv on servicelog (serviceId) tablespace KSAMSOK_INDX;
create index ix_servicelog_ts on servicelog (eventTs) tablespace KSAMSOK_INDX;

-- trigger och sekvens för att få till en räknare i oracle 
create sequence servicelog_seq MINVALUE 1 START WITH 1 INCREMENT BY 1 CACHE 20;
CREATE OR REPLACE TRIGGER servicelog_before_insert 
BEFORE INSERT ON servicelog FOR EACH ROW
BEGIN
	select servicelog_seq.nextval into :NEW.eventId from dual;
END;

-- spatiala data
--drop table geometries
create table geometries (
uri varchar2(1024) not null,
serviceId varchar2(20) not null,
name varchar2(1024),
geometry "MDSYS"."SDO_GEOMETRY" NOT NULL
);

create index ix_geometries_uri on geometries (uri) tablespace KSAMSOK_INDX;
create index ix_geometries_serv on geometries (serviceId) tablespace KSAMSOK_INDX;

-- tala om spatial-info för oracle (3006 är sweref99 tm)
insert into USER_SDO_GEOM_METADATA
values(
'GEOMETRIES',
'GEOMETRY',
MDSYS.SDO_DIM_ARRAY(
 MDSYS.SDO_DIM_ELEMENT('X', 217000, 1090000, 5.0000E-10),
 MDSYS.SDO_DIM_ELEMENT('Y', 6110000, 7700000, 5.0000E-10)
),
3006);

-- spatialt index
CREATE INDEX IX_GEOMETRIES_GEOMETRY ON GEOMETRIES (GEOMETRY) 
   INDEXTYPE IS "MDSYS"."SPATIAL_INDEX" PARAMETERS ('tablespace=KSAMSOK_INDX')

-- skapa motsvarande vy för geoserver (måste heta samma som feature type i geoservers conf)
-- notera att man skulle kunna gå direkt mot tabellen geometries men iom att vi gör
-- en vy skulle man kunna stoppa in fler värden som hämtas/genereras utifrån annat
-- data, exempelvis gtype mm
create or replace view KSAMSOK_WMS as select * from geometries;

-- och tala om spatial-info för oracle för denna vy också (samma som för tabellen)
insert into USER_SDO_GEOM_METADATA
values(
'KSAMSOK_WMS',
'GEOMETRY',
MDSYS.SDO_DIM_ARRAY(
 MDSYS.SDO_DIM_ELEMENT('X', 217000, 1090000, 5.0000E-10),
 MDSYS.SDO_DIM_ELEMENT('Y', 6110000, 7700000, 5.0000E-10)
),
3006);

