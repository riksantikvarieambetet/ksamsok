#! /bin/sh
#
#RAA Parametrar
export LC_ALL=en_US.utf-8
export LANG=en_US.utf-8

# Används javas interna TransformerFactory, som hanterar emoticons i xml bättre än vad xalan gör
#CATALINA_OPTS
export CATALINA_OPTS="-d64 -Djavax.xml.transform.TransformerFactory=com.sun.org.apache.xalan.internal.xsltc.trax.TransformerFactoryImpl -Dsun.net.client.defaultReadTimeout=5000000 -Dsun.net.client.defaultConnectTimeout=60000 -Djava.awt.headless=true -Dkms.ojb.lrucache.size=100000"
