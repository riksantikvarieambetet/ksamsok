-- patch-fil för att uppdatera ett schema för att komma ihåg borttagna poster för europeana

alter table content add (added timestamp);
alter table content add (deleted timestamp);
alter table content add (datestamp timestamp);
alter table content add (status integer);

update content set status = 0;
commit;

alter table content modify status not null;

create index ix_content_serv_status on content (serviceId, status) tablespace KSAMSOK_INDX;

