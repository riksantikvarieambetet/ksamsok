-- enkelt repository för k-samsök i pg
-- kan läggas i eget schema eller i public

CREATE TABLE apikeys (
apikey varchar PRIMARY KEY not null,
owner varchar not null,
total int8);

CREATE TABLE organisation (
kortnamn varchar PRIMARY KEY not null,
beskrivswe varchar,
beskriveng varchar,
adress1 varchar,
adress2 varchar,
postadress varchar,
kontaktperson varchar,
epostkontaktperson varchar,
websida varchar,
websidaks varchar,
lowressurl varchar,
thumbnailurl varchar,
namnswe varchar,
namneng varchar,
pass varchar,
serv_org varchar);

CREATE TABLE content (
uri varchar PRIMARY KEY not null,
oaiuri varchar,
serviceid varchar,
xmldata text,
changed timestamp,
added timestamp,
deleted timestamp,
datestamp timestamp not null,
status int8,
idnum bigserial unique,
nativeurl varchar);

CREATE TABLE harvestservices (
serviceid varchar PRIMARY KEY not null,
servicetype varchar,
name varchar,
harvesturl varchar,
harvestsetspec varchar,
cronstring varchar,
lastharvestdate timestamp,
alwayseverything bool,
firstindexdate timestamp,
kortnamn varchar,
beskrivning varchar,
FOREIGN KEY (kortnamn) REFERENCES organisation(kortnamn));


-- oklart om denna används fn?
CREATE TABLE relations (
relid int8,
object1 varchar,
object2 varchar,
relationtype varchar,
createdate timestamp,
changedate timestamp,
creator varchar);

CREATE TABLE searches (
apikey varchar not null,
searchstring varchar not null,
param varchar not null,
count int8,
FOREIGN KEY (apikey) REFERENCES apikeys(apikey));

CREATE TABLE servicelog (
serviceid varchar,
eventtype int8,
eventstep varchar,
eventts timestamp,
message varchar,
eventid bigserial unique);

-- dessa index är i princip kopierade från oracle-scriptet och det är möjligt att de ska
-- se lite annorlunda ut i pg, fler/färre etc 
create index ix_content_serv_oai on content (serviceId, oaiURI);
create index ix_content_serv on content (serviceId);
create index ix_content_oai on content (oaiURI);
create index ix_content_serv_status on content (serviceId, status);
create index ix_content_serv_deleted on content (serviceId, deleted);
create index ix_content_uri_serv on content (uri, serviceId);
create index ix_content_date on content (datestamp);
create index ix_content_deleted on content (deleted);
CREATE INDEX ix_apikeys_apikey ON apikeys (apikey);
CREATE INDEX ix_organisation_kortnamn ON organisation (kortnamn);
CREATE INDEX ix_searches_pk ON searches (apikey, searchstring, param);

create index ix_servicelog_serv on servicelog (serviceId);
create index ix_servicelog_ts on servicelog (eventTs);

-- spatialdata

CREATE TABLE geometries (
uri varchar not null,
serviceid varchar not null,
name varchar);

SELECT AddGeometryColumn('geometries', 'geometry', 3006, 'GEOMETRY', 2 );

create index ix_geometries_uri on geometries (uri);
create index ix_geometries_serv on geometries (serviceId);


-- spatialt index
CREATE INDEX IX_GEOMETRIES_GEOMETRY ON GEOMETRIES using GIST (GEOMETRY);

-- skapa motsvarande vy för geoserver (måste heta samma som feature type i geoservers conf)
-- notera att man skulle kunna gå direkt mot tabellen geometries men iom att vi gör
-- en vy skulle man kunna stoppa in fler värden som hämtas/genereras utifrån annat
-- data, exempelvis gtype mm
-- notera att geoserver är skiflägeskänslig
create or replace view KSAMSOK_WMS as select * from geometries;

-- möjligt att man måste lägga in vyns geometrikolumn manuellt i geometry_columns men det verkar inte behövas, se:
-- http://postgis.refractions.net/documentation/manual-1.5/ch04.html#Manual_Register_Spatial_Column
