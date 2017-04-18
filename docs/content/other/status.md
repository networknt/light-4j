---
date: 2016-10-23T13:20:51-04:00
title: Status
---

In the scenario that error happens on the server, a Status object is designed
to encapsulate standard http response 4xx and 5xx as well as application specific
error code ERRXXXXX (prefixed with ERR with another 5 digits) and error message.
Additionally, an description of the error will be available for more info about
the error.

# Data Elements

Here are the four fields in the Status object.
```
    int statusCode;
    String code;
    String message;
    String description;
```

# Construct the object from status.yml
status.yml is a configuration file that contains all the status error defined
for the application and it has the structure like this.

```
ERR10000:
  statusCode: 401
  code: ERR10000
  message: INVALID_AUTH_TOKEN
  description: Incorrect signature or malformed token in authorization header
ERR10001:
  statusCode: 401
  code: ERR10001
  message: AUTH_TOKEN_EXPIRED
  description: Jwt token in authorization header expired
  .
  .
  .
}
```

To construct the object from this config

```
    static final String STATUS_METHOD_NOT_ALLOWED = "ERR10008";
    .
    .
    .
    Status status = new Status(STATUS_METHOD_NOT_ALLOWED);

```
To construct the object with arguments to have a description with context information.

```
   return new Status("ERR11000", queryParameter.getName(), swaggerOperation.getPathString().original());

```

# Convert to JSON response.

There are several way to serialize the object to JSON in response. And string
concat is almost 10 times faster than Jackson ObjectMapper. For one million
objects:

```
Jackson Perf 503
ToString Perf 65

```
# Error code range allocation
The error code prefixed with ERR with another 5 digits so that it can be easily
scanned in log files. Also, certain error code can be used to trigger an alert
such as email or pager notification on system wide issues.

In order to make sure there is no conflict for error code allocation between
teams, here is the rule

10000-19999 reserved for the framework/system.
   * 10000-10100 security

   * 11000-11999 validation

   * 12000-12999 light-oauth2

20000-29999 common error codes within your business domain.
90000-99999 customize error code that cannot be found in common range.

# Send the JSON as response

```
    Status status = new Status(STATUS_METHOD_NOT_ALLOWED);
    exchange.setStatusCode(status.getStatusCode());
    exchange.getResponseSender().send(status.toString());

```
