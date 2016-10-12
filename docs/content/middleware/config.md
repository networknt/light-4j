---
date: 2016-10-12T19:00:54-04:00
title: Config
---

A configuration module that supports externalized config in official standalone 
deployment or docker container

## Introduction

Externalized configuration from the application package is very important. It 
allows us to deploy the same package
to DEV/SIT/UAT/PROD environment with different configuration packages without 
reopening the delivery package. For
development, embedded configuration give us the flexibility to take default 
values out of the box.


When dockerizing our application, the external configuration can be mapped to a 
volume which can be updated and
restarted the container to take effect.

For now, only file system configuration is supported but in a long run, this 
module can be extended to support
configurations from database, http/https server and config API served by config 
server. Given the risk of single
point failure, file system config is still the foundation. Only when the server 
start, it will contact external
config sever to load (first time) or update local file system so that config 
files are cached. Except first time
start the server, the server can use the local cache to start if config service 
is not available.

## Singleton

For one JVM, there should be only one instance of Config and it should be 
shared by all the modules within the application. The class is designed as a 
singleton class and has several abstract methods that should be implemented 
in sub classes. A default File System based implementation is provided. Other 
implementation will be provided in the future when it is needed.
 
## Interface

The following abstract methods are implemented in the default service provider. 

```
// Returns a HashMap by a config name (without .json)     
public abstract Map<String, Object> getJsonMapConfig(String configName);

// Load config for server info so that the result can be masked with out impact the cached verison
public abstract Map<String, Object> getJsonMapConfigNoCache(String configName);

// Return a JsonNode by a config name (without .json)
public abstract JsonNode getJsonNodeConfig(String configName);

// Map the json to a POJO from a config name and class name
public abstract Object getJsonObjectConfig(String configName, Class clazz);

// Load config into a string by providing the full filename
public abstract String getStringFromFile(String filename);

// Load config into a InputStream by providing full filename
public abstract InputStream getInputStreamFromFile(String filename);

// A cached Jackson ObjectMapper that is shared to other modules
public abstract ObjectMapper getMapper();

// Clean up the config cache if there are any
public abstract void clear();

```

## Usage

It is encouraged to create a POJO for your config if the JSON structure is static. 
However, some of the config file have repeatable elements, a map is the only 
options to represent the data in this case. 

The two methods return String and InputStream is for loading files such as text 
file with different format and X509 certificates etc.

getMapper() is a convenient way to get ObjectMapper instead of create one which 
is too heavy.


## Cache

Loading configuration files from file system takes time so the config file will 
be cached once it is loaded the first time. All subsequent access to the same 
config file (maybe different key/value pair) will read from the cached copy. 
In order to make sure that server start up won't be impacted by all modules to 
load config files, lazy loading is encouraged unless the module must be 
initialized during server start up.

## Loading sequence

Each module should have a config file that named the same as the module name. 
For example, Security module as a config file named security.json and it is 
reside in the module /src/main/resources/config folder by default.

For application that uses Security module, it can override the default module 
level config by provide security.json in application's resource 
folder /src/main/resources/config.

Config will also be loaded from class path of the application and this is 
mainly used for testing as it is easy to inject different combination of 
config copies in test cases into class path for unit testing.

For official runtime environment, config files should be externalized to a 
separate folder outside of the package by a system property 
"undertow-server-config-dir". You can start the server with 
a "-Dundertow-server-config-dir=/config" option and put all your files 
into /config. Once in docker image, it can be mapped to a host volume 
with -v /etc/undertow:/config in docker run command.

Given above explanation, the loading sequence is:

1. System property "undertow-server-config-dir" specified directory
2. Class Path
3. Application resources/config folder
4. Module resources/config folder

## Example

Load config into static variable

```
static final Map<String, Object> config = Config.getInstance().getJsonMapConfig(JwtHelper.SECURITY_CONFIG);
```

Load config when it is used 

```
ValidatorConfig config = (ValidatorConfig)Config.getInstance().getJsonObjectConfig(CONFIG_NAME, ValidatorConfig.class);

```