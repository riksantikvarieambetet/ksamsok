#! /bin/sh
#
#RAA Parametrar
export LC_ALL=en_US.utf-8
export LANG=en_US.utf-8

#CATALINA_OPTS
export CATALINA_OPTS="-d64 -Dsun.net.client.defaultReadTimeout=5000000 -Dsun.net.client.defaultConnectTimeout=60000 -Djava.awt.headless=true -Dkms.ojb.lrucache.size=100000"
