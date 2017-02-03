---
date: 2017-02-03T14:12:47-05:00
title: keytool
---

This is a Java command line tool to generate and manipulate keys.

To create a server.keystore for TLS.

```
keytool -genkey -alias mycert -keyalg RSA -sigalg MD5withRSA -keystore server.keystore -storepass secret  -keypass secret -validity 9999
```
And then copy this file to light-java/src/main/resources/config/tls folder. At 
the same time, update server.json for keystoreName, keystorePass, keyPass.

```
{
  "description": "server config",
  "ip": "0.0.0.0",
  "httpPort": 8080,
  "enableHttp": true,
  "httpsPort": 8443,
  "enableHttps": true,
  "keystoreName": "tls/server.keystore",
  "keystorePass": "secret",
  "keyPass": "secret",
  "enableTwoWayTls": false,
  "truststoreName": "tls/server.truststore",
  "truststorePass": "password",
  "serviceId": "com.networknt.petstore-1.0.0",
  "enableRegistry": false
}

```
