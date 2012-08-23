%define ver 1.0.2
%define rel 3

Summary: Raö K-Samsök, centralnod (@RPM_SUFFIX@)
Name: raa-ksamsok_app_@RPM_SUFFIX@
Version: %{ver}
Release: %{rel}
Packager: Börje Lewin <borje.lewin@raa.com>
Vendor: Raa 
URL: http://www.raa.se
License: (C) 2009 RAÄ 
Group: System Environment/Daemons
# provar att kommentera bort BuildArchitectures: noarch
BuildRoot: %{_tmppath}/%{name}-%{version}-buildroot

Requires: raa-tomcat8080 >= 7.0.0, raa-ksamsok_solr_@RPM_SUFFIX@ >= 1.0.2

%description
Raä K-Samsok, centralnod (@RPM_SUFFIX@)

%install
rm -rf $RPM_BUILD_ROOT

mkdir -p -m755 $RPM_BUILD_ROOT/usr/local/tomcat8080/webapps
mkdir -p -m755 $RPM_BUILD_ROOT/usr/local/tomcat8080/conf


install -m755 $RPM_SOURCE_DIR/ksamsok.war $RPM_BUILD_ROOT/usr/local/tomcat8080/webapps

install -m755 $RPM_SOURCE_DIR/tomcat-users.xml $RPM_BUILD_ROOT/usr/local/tomcat8080/conf

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
%attr(0644,tomcat,nobody) /usr/local/tomcat8080/conf/tomcat-users.xml

%changelog
* Mon Dec 6 2010 ant
- Master/slave
* Wed Dec 1 2010 ant
- Beroende på ksamok-solr och bort med lucenes indexkatalog
* Fri Dec 11 2009 ant
- Uppdaterat till nya RPM-metodiken
* Tue Jul 28 2009 ant
- La till extra jar-filer för hantering av spatiala data
* Fri Feb 13 2009 ant
- Nya tjänsterelaterade index mm
* Tue Feb 3 2009 ant
- Tecken- och indexfixar
* Tue Feb 3 2009 ant
- Hantera too many boolean clauses bï¿½ttre
* Mon Feb 2 2009 ant
- Flyttade om gränssnitt en aning
* Mon Feb 2 2009 ant
- Diverse buggfixar
* Mon Jan 19 2009 ant
- Första version
