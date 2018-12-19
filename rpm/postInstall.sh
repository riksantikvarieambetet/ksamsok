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