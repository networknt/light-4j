---
date: 2016-10-08T22:14:59-04:00
title: Swagger Codegen
---

## Introduction

Many API projects were done without specifications and API specifications were
produced after APIs were on production. When using Light-Java-Rest Framework, 
design driven is encouraged so swagger specification must be produced before 
the development is started. Please see [Development Flow](https://networknt.github.io/light-java/management/flow/) 
for details.

When product owner starts an API/service, the first thing is to find the swagger
specification produced by data architect and generate a server skeleton with 
swagger-codegen.
 
## Specification

All our specifications including OAuth2 and examples are in the following repo.
https://github.com/networknt/swagger

Each API/service will have a folder in this repo and it might have versions in
each sub folders. 

For each API/service, there are three files must be there:

* swagger.yaml - the readable source file for [swagger-editor](https://networknt.github.io/light-java/tools/swagger-editor/)
* swagger.json - the aggregated spec in json format processed by [swagger-cli](https://networknt.github.io/light-java/tools/swagger-cli/) 
* config.json - the configuration file for swagger-codegen

Here is one example of specification folder.

https://github.com/networknt/swagger/tree/master/oauth2_client

## Local Generation

### Install

To install the swagger-codegen locally, go to your working directory and run

```
git clone git@github.com:networknt/swagger-codegen.git
cd swagger-codegen
mvn clean install -DskipTests
```

Now you have swagger-codegen built on your local and it is ready to be used. This
version of swagger-codegen is a fork of official [swagger-codegen](https://github.com/swagger-api/swagger-codegen)
as the official version doesn't support Java 8 yet. 



### Usage

Assume that you have just built your swagger-codegen and you are in the swagger-codegen
folder now you can generate petstore API in your home directory with the following command.

```
java -jar modules/swagger-codegen-cli/target/swagger-codegen-cli.jar generate -i http://petstore.swagger.io/v2/swagger.json -l light-java -o ~/petstore

```

Above command uses a swagger specification from the Internet with default configuration. 
If you have swagger file in your local, you can use:

```
-i ~/networknt/swagger/oauth2_client/swagger.json
```
If you want to customized the generated code, you can use:

```
-c ~/networknt/swagger/oauth2_client/config.json
```


## Docker Generation

### Install
If you have docker installed, you can run the docker container of the swagger-codegen
so that you don't need to install JDK8 and Maven on your local.

```
docker run -it networknt/swagger-codegen config-help -l light-java
```

Above command will list all the config options for light-java language generator.

### Usage

Mount a volume to `/swagger-api/out` for output.

Example:

```
docker run -it -v ~/temp/swagger-generated:/swagger-api/out \
    networknt/swagger-codegen generate \
    -i /swagger-api/yaml/petstore-with-fake-endpoints-models-for-testing.yaml \
    -l light-java \
    -o /swagger-api/out/petstore
```
Your generated code will now be accessible under `~/temp/swagger-generated/petstore`.


## Customization

The tool support customized invokerPackage, apiPackage, modelPackage, artifactId and groupId defined in a json config file. 
You will also specify the location of the swagger.json and the output folder of the generated code.

Here is an example of config.json

```
{
  "invokerPackage": "com.networknt.petstore",
  "apiPackage":"com.networknt.petstore.handler",
  "modelPackage":"com.networknt.petstore.model",
  "artifactId": "petstore",
  "groupId": "com.networknt"
}
```
 

General Command

```shell
java -jar modules/swagger-codegen-cli/target/swagger-codegen-cli.jar generate -c <path_to_config.json> -i <path_to_specification_file> -l light-java -o <output_folder>
```

Example
```shell
java -jar modules/swagger-codegen-cli/target/swagger-codegen-cli.jar generate -c ~/networknt/swagger/api_a/config.json -i ~/networknt/swagger/api_a/swagger.yaml -l light-java -o ~/networknt/generated
```

Docker Command

```
docker run -it -v <path of swagger and config>:/swagger-api/swagger -v <path of generated project>:/swagger-api/out networknt/swagger-codegen generate -c /swagger-api/swagger/config.json -i /swagger-api/swagger/swagger.yaml -l light-java -o /swagger-api/out/api_a

```

Example

```
docker run -it -v ~/networknt/swagger/api_a:/swagger-api/swagger -v ~/networknt/generated:/swagger-api/out networknt/swagger-codegen generate -c /swagger-api/swagger/config.json -i /swagger-api/swagger/swagger.yaml -l light-java -o /swagger-api/out/api_a

```
Note: the generated code in Linux might be owned by root user, so you have to change the owner before compiling it.

```
sudo chown -R steve:steve generated
```



## Shared swagger files

For organizations with multiple APIs, some of the common yaml files will be created that can be 
shared between APIs in specifications. In that case, you cannot use yaml file to generate the code as
the tool doesn't know how to locate the external reference files. Please find [swagger-cli](/tools/swager-cli)
to convert several yaml files into one Json file to feed the generator.


## Test

The swagger-codegen tool generates an application running against the Undertow Server.

  * Navigate to the <output_folder> for the generated code
  * Build the code using the generated Maven POM file
  * Start the application
    * By default, the application is available at port 8080

```shell
cd <output_folder>
mvn clean install exec:exec
```
