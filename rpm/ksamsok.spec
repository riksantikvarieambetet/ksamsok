%define ver 1.0.1
%define rel 31

Summary: Ra� K-Sams�k, centralnod (@RPM_SUFFIX@)
Name: raa-ksamsok_app_@RPM_SUFFIX@
Version: %{ver}
Release: %{rel}
Packager: Borje Lewin <borje.lewin@raa.com>
Vendor: Raa 
URL: http://www.raa.se
License: (C) 2009 RA� 
Group: System Environment/Daemons
# provar att kommentera bort BuildArchitectures: noarch
BuildRoot: %{_tmppath}/%{name}-%{version}-buildroot

Requires: raa-tomcat8080 >= 6.0.18, raa-ksamsok_solr_@RPM_SUFFIX@ >= 1.0.0
Provides: raa-ksamsok_applic

%description
Ra� K-Samsok, centralnod (@RPM_SUFFIX@)

%install
rm -rf $RPM_BUILD_ROOT

mkdir -p -m755 $RPM_BUILD_ROOT/usr/local/tomcat8080/webapps
mkdir -p -m755 $RPM_BUILD_ROOT/usr/local/tomcat8080/conf
mkdir -p -m755 $RPM_BUILD_ROOT/usr/local/tomcat8080/lib
mkdir -p -m755 $RPM_BUILD_ROOT/usr/local/tomcat8080/bin

install -m755 $RPM_SOURCE_DIR/ksamsok.war $RPM_BUILD_ROOT/usr/local/tomcat8080/webapps

# install -m755 $RPM_SOURCE_DIR/context.xml $RPM_BUILD_ROOT/usr/local/tomcat8080/conf
install -m755 $RPM_SOURCE_DIR/tomcat-users.xml $RPM_BUILD_ROOT/usr/local/tomcat8080/conf
install -m755 $RPM_SOURCE_DIR/postgresql-9.0-801.jdbc4.jar $RPM_BUILD_ROOT/usr/local/tomcat8080/lib
install -m755 $RPM_SOURCE_DIR/oracle-10.2.0.4.jar $RPM_BUILD_ROOT/usr/local/tomcat8080/lib
install -m755 $RPM_SOURCE_DIR/ora10-sdoapi.jar $RPM_BUILD_ROOT/usr/local/tomcat8080/lib
install -m755 $RPM_SOURCE_DIR/ora10-sdoutl.jar $RPM_BUILD_ROOT/usr/local/tomcat8080/lib
install -m755 $RPM_SOURCE_DIR/jstl.jar $RPM_BUILD_ROOT/usr/local/tomcat8080/lib
install -m755 $RPM_SOURCE_DIR/standard.jar $RPM_BUILD_ROOT/usr/local/tomcat8080/lib
install -m755 $RPM_SOURCE_DIR/catalina.sh $RPM_BUILD_ROOT/usr/local/tomcat8080/bin
install -m755 $RPM_SOURCE_DIR/ora10-xmlparserv2-noservices.jar $RPM_BUILD_ROOT/usr/local/tomcat8080/lib

%clean
rm -rf $RPM_BUILD_ROOT

%pre
# stoppa tomcat
/sbin/service tomcat8080.init stop
sleep 5
rm -rf /usr/local/tomcat8080/webapps/ksamsok

%post
/sbin/service tomcat8080.init start

%preun


%postun
rm -rf /usr/local/tomcat8080/webapps/ksamsok

%files
%defattr(-,tomcat,nobody)
%attr(0644,tomcat,nobody) /usr/local/tomcat8080/webapps/ksamsok.war
# %attr(0644,tomcat,nobody) /usr/local/tomcat8080/conf/context.xml
%attr(0644,tomcat,nobody) /usr/local/tomcat8080/conf/tomcat-users.xml
%attr(0755,tomcat,nobody) /usr/local/tomcat8080/bin/catalina.sh
# oracle-drivrutiner f�r jdbc och spatialut�kningar
# obs, se till att ha samma matchningar h�r som i build.xml
%attr(0644,tomcat,nobody) /usr/local/tomcat8080/lib/oracle-*.jar
%attr(0644,tomcat,nobody) /usr/local/tomcat8080/lib/ora10-*.jar
%attr(0644,tomcat,nobody) /usr/local/tomcat8080/lib/postgresql-9.0-801.jdbc4.jar
%attr(0644,tomcat,nobody) /usr/local/tomcat8080/lib/standard.jar
%attr(0644,tomcat,nobody) /usr/local/tomcat8080/lib/jstl.jar

%changelog
* Mon Dec 6 2010 ant
- Master/slave
* Wed Dec 1 2010 ant
- Beroende p� ksamok-solr och bort med lucenes indexkatalog
* Fri Dec 11 2009 ant
- Uppdaterat till nya RPM-metodiken
* Tue Jul 28 2009 ant
- La till extra jar-filer f�r hantering av spatiala data
* Fri Feb 13 2009 ant
- Nya tj�nsterelaterade index mm
* Tue Feb 3 2009 ant
- Tecken- och indexfixar
* Tue Feb 3 2009 ant
- Hantera too many boolean clauses b�ttre
* Mon Feb 2 2009 ant
- Flyttade om gr�nssnitt en aning
* Mon Feb 2 2009 ant
- Diverse buggfixar
* Mon Jan 19 2009 ant
- F�rsta version
