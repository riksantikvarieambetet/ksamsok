-- enkelt repository

-- För oracle, data i default tablespace, index i "KSAMSOK_INDX"
-- för tex Derby eller postgresql krävs små ändringar

--drop table harvestservices
create table harvestservices (
serviceId varchar(20),
serviceType varchar(20),
name varchar(200),
harvestURL varchar(4000),
harvestSetSpec varchar(50),
cronstring varchar(50),
lastHarvestDate timestamp,
alwayseverything integer -- boolean
);

alter table harvestservices add constraint pk_harvestservices primary key (serviceId) using index tablespace KSAMSOK_INDX;

--drop table content
create table content (
uri varchar(1024),
oaiuri varchar(1024),
serviceId varchar(20),
changed timestamp,
xmldata clob
);

alter table content add constraint pk_content primary key (uri) using index tablespace KSAMSOK_INDX;
create index ix_content_serv_oai on content (serviceId, oaiURI) tablespace KSAMSOK_INDX;
create index ix_content_serv on content (serviceId) tablespace KSAMSOK_INDX;

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
