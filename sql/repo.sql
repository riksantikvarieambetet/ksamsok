-- enkelt repository

-- För oracle, data i default tablespace, index i "KSAMSOK_INDX"
-- för tex Derby eller postgresql krävs små ändringar (postgres stöds ej fn, se javakod)

--drop table harvestservices
create table harvestservices (
serviceId varchar(20),
serviceType varchar(20),
name varchar(200),
harvestURL varchar(4000),
harvestSetSpec varchar(50),
cronstring varchar(50),
lastHarvestDate timestamp,
firstIndexDate timestamp,
alwayseverything integer -- boolean
);

alter table harvestservices add constraint pk_harvestservices primary key (serviceId) using index tablespace KSAMSOK_INDX;

--drop table content
create table content (
uri varchar(1024),
oaiuri varchar(1024),
serviceId varchar(20),
changed timestamp,
xmldata clob,
added timestamp,
deleted timestamp,
datestamp timestamp not null,
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

--drop table servicelog
create table servicelog (
eventId integer not null,
serviceId varchar(20),
eventType integer,
eventStep varchar(20),
eventTs timestamp,
message varchar(4000)
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
uri varchar(1024) not null,
serviceId varchar(20) not null,
name varchar(1024),
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

