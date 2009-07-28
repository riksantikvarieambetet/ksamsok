%define ver @@KSAMSOKVERSION@@
%define rel @@KSAMSOKRELEASE@@

Summary: Raä K-Samsök, centralnod
Name: raa-ksamsok-8080
Version: %{ver}
Release: %{rel}
Packager: ant build
Vendor: Riksantikvarieämbetet 
URL: http://www.raa.se
License: (C) 2009 RAÄ 
Group: System Environment/Daemons
BuildArchitectures: noarch

Requires: raa-tomcat8080 >= 6.0.18

%description
Raä K-Samsok, centralnod

%install

%pre
# stoppa tomcat
/sbin/service tomcat8080.init stop
sleep 5
# kopiera undan orginalen vid ny install
if [ "$1" -eq 1 ]; then
  if [ -f /usr/local/tomcat8080/conf/context.xml ]; then
    cp -p /usr/local/tomcat8080/conf/context.xml /usr/local/tomcat8080/conf/context.xml.bkup.ksamsok
  fi
  if [ -f /usr/local/tomcat8080/conf/tomcat-users.xml ]; then
    cp -p /usr/local/tomcat8080/conf/tomcat-users.xml /usr/local/tomcat8080/conf/tomcat-users.xml.bkup.ksamsok
  fi
fi
# ta bort en tidigare uppackad webapp om vi uppgraderar
if [ "$1" -ge 2 ]; then
  rm -rf /usr/local/tomcat8080/webapps/ksamsok
fi

%post
# skapa indexkatalogen och sätt rättigheter
mkdir -p -m755 /var/lucene-index/ksamsok
chown tomcat:nobody /var/lucene-index/ksamsok
# disable och sen enable av tomcat i relevanta runlevels
# taget från Peters exempel, kanske inte behövs då tomcat-rpm:en egentligen borde göra detta(?)
/sbin/chkconfig tomcat8080.init off
/sbin/chkconfig tomcat8080.init on
# starta eller starta om tomcat
/sbin/service tomcat8080.init restart

%preun
# stoppa tomcat om vi avinstallerar allt
if [ "$1" -eq 0 ]; then
  /sbin/service tomcat8080.init stop
  sleep 5
fi

%postun
# återställ orginalen, ta bort kopiorna och den uppackade webappen om vi avinstallerar allt
if [ "$1" -eq 0 ]; then
  if [ -f /usr/local/tomcat8080/conf/context.xml.bkup.ksamsok ]; then
    cp -p /usr/local/tomcat8080/conf/context.xml.bkup.ksamsok /usr/local/tomcat8080/conf/context.xml
    rm /usr/local/tomcat8080/conf/context.xml.bkup.ksamsok
  fi
  if [ -f /usr/local/tomcat8080/conf/tomcat-users.xml.bkup.ksamsok ]; then
    cp -p /usr/local/tomcat8080/conf/tomcat-users.xml.bkup.ksamsok /usr/local/tomcat8080/conf/tomcat-users.xml
    rm /usr/local/tomcat8080/conf/tomcat-users.xml.bkup.ksamsok
  fi
  rm -rf /usr/local/tomcat8080/webapps/ksamsok
  # starta tomcat igen (borde kanske inte starta igen?)
  /sbin/service tomcat8080.init start
  sleep 5
fi

%files
%defattr(-,tomcat,nobody)
%attr(0644,tomcat,nobody) /usr/local/tomcat8080/webapps/ksamsok.war
%attr(0644,tomcat,nobody) /usr/local/tomcat8080/conf/context.xml
%attr(0644,tomcat,nobody) /usr/local/tomcat8080/conf/tomcat-users.xml
# oracle-drivrutiner för jdbc och spatialutökningar
# obs, se till att ha samma matchningar här som i build.xml
%attr(0644,tomcat,nobody) /usr/local/tomcat8080/lib/oracle-*.jar
%attr(0644,tomcat,nobody) /usr/local/tomcat8080/lib/ora10-*.jar

%changelog
* Tue Jul 28 2009 ant
- La till extra jar-filer för hantering av spatiala data
* Fri Feb 13 2009 ant
- Nya tjänsterelaterade index mm
* Tue Feb 3 2009 ant
- Tecken- och indexfixar
* Tue Feb 3 2009 ant
- Hantera too many boolean clauses bättre
* Mon Feb 2 2009 ant
- Flyttade om gränssnitt en aning
* Mon Feb 2 2009 ant
- Diverse buggfixar
* Mon Jan 19 2009 ant
- Första version
