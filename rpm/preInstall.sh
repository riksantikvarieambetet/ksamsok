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