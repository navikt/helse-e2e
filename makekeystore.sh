#!/bin/sh

mkdir -p mockkeys
cd mockkeys
if [ ! -f cert.pem ]; then
    openssl req -x509 -newkey rsa:2048 -keyout key.pem -out cert.pem -days 365 -nodes -subj '/CN=vtpmock'

    # local-host SSL
    openssl pkcs12 -export -name localhost-ssl -in cert.pem -inkey key.pem -out serverkeystore.p12 -password pass:changeit
    keytool -importkeystore -destkeystore keystore.jks -srckeystore serverkeystore.p12 -srcstoretype pkcs12 -alias localhost-ssl -storepass changeit -keypass changeit -srcstorepass changeit

    # app-key (jwt uststeder bl.a. i mocken, vi bruker samme noekkel per naa):
    openssl pkcs12 -export -name app-key -in cert.pem -inkey key.pem -out serverkeystore2.p12 -password pass:changeit
    keytool -importkeystore -destkeystore keystore.jks -srckeystore serverkeystore2.p12 -srcstoretype pkcs12 -alias app-key -storepass changeit -keypass changeit -srcstorepass changeit

    # truststore for SSL:
    keytool -import -trustcacerts -alias localhost-ssl -file cert.pem -keystore truststore.jks -storepass changeit -noprompt

fi