%define ver 1.0.2
%define rel 32

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

Requires: raa-tomcat8080 = 7.0.25, raa-ksamsok_solr_@RPM_SUFFIX@ = 1.0.2

%description
Raä K-Samsok, centralnod (@RPM_SUFFIX@)

%install
rm -rf $RPM_BUILD_ROOT

mkdir -p -m755 $RPM_BUILD_ROOT/usr/local/tomcat8080/webapps
mkdir -p -m755 $RPM_BUILD_ROOT/usr/local/tomcat8080/conf
mkdir -p -m755 $RPM_BUILD_ROOT/usr/local/tomcat8080/bin
mkdir -p -m755 $RPM_BUILD_ROOT/usr/local/tomcat8080/bin/appconfig


install -m755 $RPM_SOURCE_DIR/ksamsok.war $RPM_BUILD_ROOT/usr/local/tomcat8080/webapps
install -m755 $RPM_SOURCE_DIR/tomcat-users.xml $RPM_BUILD_ROOT/usr/local/tomcat8080/conf
install -m755 $RPM_SOURCE_DIR/setenv.sh $RPM_BUILD_ROOT/usr/local/tomcat8080/bin
install -m755 $RPM_SOURCE_DIR/ksamsok.javaopts $RPM_BUILD_ROOT/usr/local/tomcat8080/bin/appconfig

%clean
rm -rf $RPM_BUILD_ROOT

%pre
#Check if tomcat is running, if not start it
tomcatStatus=$(/sbin/service tomcat8080.init status)
case $tomcatStatus in
	*"pid"*) COUNTER=60
		echo "Tomcat is already running";;

	*) 	/sbin/service tomcat8080.init start
		echo "Starting tomcat. Waiting 60 s for tomcat to start..."
		COUNTER=0;;

esac

#Wait for tomcat to start
while [  $COUNTER -lt 60 ]; do
	tomcatStatus=$(/sbin/service tomcat8080.init status)
	case $tomcatStatus in
		*"pid"*) COUNTER=60
			echo "Tomcat is now running."
			sleep 5;;

		*) 	let COUNTER=COUNTER+1
			echo "Still waiting for Tomcat to start"
			sleep 1;;
	esac
done

#Check if tomcat is up and running otherwise exit
tomcatStatus=$(/sbin/service tomcat8080.init status)
case $tomcatStatus in
	*"pid"*) echo "Tomcat is up and running";;

	*) 	echo "Has been waiting for 60 s and tomcat is not up and running. Check your tomcat installation. Will now exit"
		exit 2;;
esac

#Remove previous installation
if [ -e /usr/local/tomcat8080/webapps/ksamsok.war ] ; then
	echo "Removing previous ksamsok.war"
	rm -f /usr/local/tomcat8080/webapps/ksamsok.war
else
	echo "No previous ksamsok.war was found"
fi
 
#Wait untill tomcat has removed the webapps or timer has timeout
echo "Waiting 60 s for tomcat to remove webapplication"
COUNTER=0
while [  $COUNTER -lt 60 ]; do
	if [ -d /usr/local/tomcat8080/webapps/ksamsok ] ; then
		echo "Web application directory still exist. Waiting for Tomcat to remove directory"
		let COUNTER=COUNTER+1
		sleep 1s
	else
		echo "Web application directory has been removed"
		let COUNTER=60
	fi 
done

#Check if the web application has been removed
if [ -d /usr/local/tomcat8080/webapps/ksamsok ] ; then
	echo "Has been waiting for 60 s and tomcat has not removed the web application. Check your tomcat installation. Will now exit"
	exit 1
fi

%post
echo "Waiting 60 s for tomcat to start web application"
COUNTER=0
while [  $COUNTER -lt 60 ]; do
	if [ -d /usr/local/tomcat8080/webapps/ksamsok ] ; then
		echo "Still waiting for Tomcat to start web application" 
		let COUNTER=COUNTER+1
		sleep 1s
	else
		let COUNTER=60
		sleep 1s
	fi 
done
echo "Restarting tomcat"
/sbin/service tomcat8080.init restart

%preun


%postun
rm -rf /usr/local/tomcat8080/webapps/ksamsok

%files
%defattr(-,tomcat,raagroup)
%attr(0644,tomcat,raagroup) /usr/local/tomcat8080/webapps/ksamsok.war
%attr(0644,tomcat,raagroup) /usr/local/tomcat8080/conf/tomcat-users.xml
%attr(0644,tomcat,raagroup) /usr/local/tomcat8080/bin/setenv.sh
%attr(0644,tomcat,raagroup) /usr/local/tomcat8080/bin/appconfig/ksamsok.javaopts

%changelog
* Thu Dec 6 2012 alwik
- Flyttar in variabler för tomcat och java i detta paket
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
- Hantera too many boolean clauses bättre
* Mon Feb 2 2009 ant
- Flyttade om gränssnitt en aning
* Mon Feb 2 2009 ant
- Diverse buggfixar
* Mon Jan 19 2009 ant
- Första version
