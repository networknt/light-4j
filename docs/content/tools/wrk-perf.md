---
date: 2016-10-08T21:47:07-04:00
title: wrk performance test
---

## Introduction

For most API frameworks in Java, the server can only handle up to ten thousands request per second and Apache JMeter
or Apache Bench will do the job. However, when we are talking about millions requests per second undertow-server
handles, they won't work as these tools will use up all the cpu resources. For high performance testing,
[wrk](https://github.com/wg/wrk) is the best as it can send tens of millions requests per seconds on a
commodity hardware.

Another limitation on high performance throughput test is network limitation. Unless you client and server a connected
with 10G network, chances are the network will be the bottle net and you cannot reach the full handling potential of
you http server. To work around it, most of my tests are running on the same computer in order to gauge the raw server
throughput and latency.

## Installation

[Install on Linux](https://github.com/wg/wrk/wiki/Installing-Wrk-on-Linux)

[Install on Mac](https://github.com/wg/wrk/wiki/Installing-wrk-on-OSX)


## Usage


### Basic

```
wrk -t12 -c400 -d30s http://localhost:8080/

```
12 threads

400 concurrent connections

30 seconds duration

### Pipeline

If you want to bump up number of requests, use pipeline


```
wrk -t4 -c128 -d30s http://localhost:8080 -s pipeline.lua --latency -- / 16
```

Here is the pipeline.lua script

```
init = function(args)
   request_uri = args[1]
   depth = tonumber(args[2]) or 1

   local r = {}
   for i=1,depth do
     r[i] = wrk.format(nil, request_uri)
   end
   req = table.concat(r)
end

request = function()
   return req
end
```

## Example
For some output examples, you can find at [undertow-server-example/performance](https://github.com/networknt/undertow-server-example/tree/master/performance)