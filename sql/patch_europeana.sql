-- patch-fil för att uppdatera ett schema för att komma ihåg borttagna poster för europeana

alter table content add (added timestamp);
alter table content add (deleted timestamp);
alter table content add (datestamp timestamp);
alter table content add (status integer);

create index ix_content_serv_status on content (serviceId, status) tablespace KSAMSOK_INDX;
create index ix_content_serv_deleted on content (serviceId, deleted) tablespace KSAMSOK_INDX;
create index ix_content_uri_serv on content (uri, serviceId) tablespace KSAMSOK_INDX;
create index ix_content_date on content (datestamp) tablespace KSAMSOK_INDX;
create index ix_content_deleted on content (deleted, ' ') tablespace KSAMSOK_INDX;

-- uppdatera status (tar nog en stund)
update content set status = 0;
commit;

-- och gör den till not null
alter table content modify status not null;

-- ful-sql för att parsa ut datum från xml (vilket verkar funka i utvdb i alla fall) och sätta datestamp
-- tar nog mellan 6 och 8h att köra...
update content set datestamp = (
case when INSTR(xmldata, 'lastChangedDate>') > 0 then
  case when to_char(substr(xmldata, INSTR(xmldata, 'lastChangedDate>') + 20, 1)) = '-' then
      to_date(substr(xmldata, INSTR(xmldata, 'lastChangedDate>') + 16, 10), 'YYYY-MM-DD')
  else
     changed
  end
else
  changed
end);

commit;

-- och gör datestamp till not null
alter table content modify datestamp not null;
