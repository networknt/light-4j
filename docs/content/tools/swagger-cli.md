---
date: 2016-10-08T22:14:47-04:00
title: Swagger cli to bundle and validate specification files
---

Note: that you don't need this tool if your specification file is self contained yaml file in
swagger-editor. You can export into json format and it is validated already during the export
process. 

### Introduction

When editing swagger specification file, naturally we will be using yaml format as it is very
easy to read and manipulate. Also, we might extract common definitions into separate files
so that there is no duplications for multiple APIs. For the light-codegen, however, it requires
one single swagger.json as model input. To generate a single json formatted specification file,
we can use the Swagger 2.0 command-line tool.


* Supports multi-file APIs via $ref pointers
* Validate Swagger 2.0 APIs in JSON or YAML format
* Bundle multiple Swagger files into one combined Swagger file
* Built-in HTTP server to serve your REST API

### Installation
The tool can be found at the following location: [swagger-cli](https://www.npmjs.com/package/swagger-cli)

Install using npm:
```shell
npm install -g swagger-cli
```

### General Usage

swagger <command> [options] <filename>

Commands:
* validate: Validates a Swagger API against the Swagger 2.0 schema and spec
* bundle: Bundles a multi-file Swagger API into a single file
* serve: Serves a Swagger API via the built-in HTTP REST server

Options:
* -h, --help: Show help for any command
* -V, --version: Output the CLI version number
* -d, --debug [filter]: Show debug output, optionally filtered (ex.: "*", "swagger:*", etc.)


As we have a [repo](https://github.com/networknt/model-config) of model and config for 
[light-codegen](https://github.com/networknt/light-codegen). It would be easier to provide
an example based on our folder structure.  


#### Command line

If you prefer to run the commands individually, please see the sequence listed below:

```shell
# clone the model-config GIT repository
git clone git@github.com:networknt/model-config.git

# switch to the rest folder
cd model-config/rest

# switch to one of the API folders
# ex.: cd petstore
cd <api name>

#copy common definitions to the api folder
cp -r ../common .

# execute the bundling functionlity
swagger bundle -o swagger.json -r swagger.yaml

# validate the definition against the Open API spec
swagger validate swagger.json

# clean-up
rm -r common

# use the generated swagger.json file with the swagger-codegen project to generate server stubs
```


#### Script

The above command line steps are very easy to understand but it is not the most convenient
way to bundle and validate yaml files. Here is another way that leverage a script in
model-config/rest folder. 

Note: the script assume that the common definition is defined in common folder and all API
specification are defined in a folder name as the API name. 

##### Run the bundle.sh script to bundle and validate

```shell
# clone the model-config repository
git clone git@github.com:networknt/model-config.git

# switch to the model-config/rest folder
cd model-config/rest

# run the bundle script
# ./bundle.sh <api name>
# ex.: ./bundle.sh petstore
./bundle.sh <api name>
```

