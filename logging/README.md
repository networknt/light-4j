# Logging

Logging is a module in Light-4j framework which will use to get the loggers and their current logging levels, And also can able to
change the logging level for given logger at runtime (Example: Change logging level to DEBUG for “com.networknt” logger for troubleshooting purpose).

There are three LoggerHandlers available in logging module.
 
 * LoggerGetHandler : This handler will provide the current logging level for given logger.
  
     End point    : /api/{serviceName}/loggers/{loggerName}.
   
     Method       : GET
   
     Example URL  : https://localhost:8443/api/customers/loggers/com.networknt
  
 * LoggersGetHandler : This handler will provide the all available loggers & their current logging levels.
    
     End point     : /api/{serviceName}/loggers
     
     Method        : GET
     
     Example URL   : https://localhost:8443/api/customers/loggers
  
 * LoggerPostHandler : Using this handler we can change the logging level for given
  logger e.g. change logging level to DEBUG for “com.networknt” logger for troubleshooting purpose.
     
      End point     : /api/{serviceName}/loggers/{loggerName}
      
      Method        : POST
      
      Example Input format : {"loggingLevel":"DEBUG"}
      
      Example URL    : https://localhost:8443/api/customers/loggers/com.networknt
 
 Note : Input is mandatory, If you provide the input in request body, it will change
 and return the updated logging level for given logger else it will throw an error.
           
 
### Usage

Add dependency.

```xml
<dependency>
  <groupId>com.networknt</groupId>
  <artifactId>logging</artifactId>
  <version>${version.light-4j}</version>
</dependency>
```

#### Configuration for handler.yml.

Add these handlers.

```
- com.networknt.logging.handler.LoggerGetHandler@getlogger
- com.networknt.logging.handler.LoggersGetHandler@getloggers
- com.networknt.logging.handler.LoggerPostHandler@postlogger
```
Add the below end points under path.

```
- path: '/api/customers/loggers'
  method: 'get'
  exec:
  - getloggers

- path: '/api/customers/loggers/{loggerName}'
  method: 'get'
  exec:
  - getlogger

- path: '/api/customers/loggers/{loggerName}'
  method: 'post'
  exec:
  - body
  - postlogger
  
```


By default logging is enabled in logging.yml. if you want to disable it,
just add the logging.yml file in your resource folder and make enabled property to false in logging.yml.


