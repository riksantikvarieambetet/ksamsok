---Till ksamsok prod

alter table content add(idnum number);

create sequence idnum_increment
start with 1
increment by 1;

create or replace trigger idnum_increment_trigger
before insert on content
for each row
begin
select idnum_increment.nextval
into :new.idnum
from dual;
end;

update content set idnum=idnum_increment.nextval;


create unique index idnum_ix on content(idnum) tablespace KSAMSOK_INDX;