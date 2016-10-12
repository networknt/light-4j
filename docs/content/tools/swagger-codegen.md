---
date: 2016-10-08T22:14:59-04:00
title: Swagger codegen to generate server stub skeleton
---

## Introduction

Many API projects were done without specifications and API specifications were
produced after APIs were on production. When using Undertow Framework, design driven
is encouraged so swagger specification must be produced before the development is
started. Please see [Development Flow]() for details.

When Product Onwer starts the API, the first thing is find the swagger specification
produced by data architect and generate a server skeleton with swagger codegen.
 
## Installation
 
#### Install locally 
To install the tool locally, go to your working directory and run

```
git clone git@github.com:networknt/swagger-codegen.git
cd swagger-codegen
mvn clean install -DskipTests
```

Now you have swagger-codegen built on your local and it is ready to be used. This
version of swagger-codegen is a fork of official [swagger-codegen](https://github.com/swagger-api/swagger-codegen)
as the official version doesn't support Java 8.

#### Docker
If you have docker installed, you can run the docker container of the swagger-codegen
so that you don't need to install JDK8 and Maven on your local.

```

```

## Usage

Assume that you have just built your swagger-codegen and you are in the swagger-codegen
folder now you can generate petstore API in your home directory with the following command.

```
java -jar modules/swagger-codegen-cli/target/swagger-codegen-cli.jar generate -i http://petstore.swagger.io/v2/swagger.json -l undertow -o ~/petstore

```

Above command uses a swagger specification from the Internet. If you have swagger file in your local,
you can use:

```
-i ~/project/swagger/api_a/swagger.json
```

Note: For organizations with multiple APIs, some of the common yaml files will be created that can be 
shared between APIs in specifications. In that case, you cannot use yaml file to generate the code as
the tool doesn't know how to locate the external reference files. Please find [swagger-cli](/tools/swager-cli)
to convert several yaml files into one Json file to feed the generator.


## Customization

The tool support customized apiPackage and modelPackage defined as System properties. You will also
specify the location of the swagger.json and the output folder of the generated code.

```shell
# set as system properties
swagger.codegen.undertow.apipackage = package for the handlers
swagger.codegen.undertow.modelpackage = package for the models

# set in the path
path_to_specification_file = local path or url of swagger.json
output_folder = output folder for generated artifacts
```

General Command

```shell
java -Dswagger.codegen.undertow.apipackage=<api_package_name> -Dswagger.codegen.undertow.modelpackage=<model_package_name> -jar modules/swagger-codegen-cli/target/swagger-codegen-cli.jar generate -i <path_to_specification_file> -l undertow -o <output_folder>
```

Example
```shell
java -Dswagger.codegen.undertow.apipackage=com.networknt.handler -Dswagger.codegen.undertow.modelpackage=com.networknt.model -jar modules/swagger-codegen-cli/target/swagger-codegen-cli.jar generate -i ~/networknt/swagger/api_a/swagger.json -l undertow -o ~/projectc/generated
```


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
