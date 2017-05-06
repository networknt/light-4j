---
date: 2017-02-03T14:12:47-05:00
title: keytool
---

This is a Java command line tool to generate and manipulate keys.

To create a server.keystore for TLS.

```
keytool -genkey -alias mycert -keyalg RSA -sigalg MD5withRSA -keystore server.keystore -storepass secret  -keypass secret -validity 9999
```
And then copy this file to light-4j/src/main/resources/config/tls folder. At 
the same time, update server.yml for keystoreName, keystorePass, keyPass.

```
# Server configuration
---
# This is the default binding address if the service is dockerized.
ip: 0.0.0.0

# Http port if enableHttp is true.
httpPort: 8080

# Enable HTTP should be false on official environment.
enableHttp: true

# Https port if enableHttps is true.
httpsPort: 8443

# Enable HTTPS should be true on official environment.
enableHttps: true

# Http/2 is enabled. When Http2 is enable, enableHttps is true and enableHttp is false by default.
# If you want to have http enabled, enableHttp2 must be false.
enableHttp2: false

# Keystore file name in config folder. KeystorePass is in secret.yml to access it.
keystoreName: tls/server.keystore

# Flag that indicate if two way TLS is enabled. Not recommended in docker container.
enableTwoWayTls: false

# Truststore file name in config folder. TruststorePass is in secret.yml to access it.
truststoreName: tls/server.truststore

# Unique service identifier. Used in service registration and discovery etc.
serviceId: com.networknt.petstore-1.0.0

# Flag to enable service registration. Only be true if running as standalone Java jar.
enableRegistry: false
```

And update secret.yml for passwords

```
# This file contains all the secrets for the server in order to manage and
# secure all of them in the same place. In Kubernetes, this file will be
# mapped to Secrets and all other config files will be mapped to mapConfig

---
# Key store password, the path of keystore is defined in server.yml
keystorePass: secret

# Key password, the key is in keystore
keyPass: secret

# Trust store password, the path of truststore is defined in server.yml
truststorePass: password

# Client secret for OAuth2 server
clientSecret: f6h1FTI8Q3-7UScPZDzfXA
```
