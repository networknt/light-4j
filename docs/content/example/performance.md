---
date: 2016-10-22T20:55:36-04:00
title: Performance
---

# 

This is an example to compare performance between other microservices 
platforms and Light Java framework. Source code can be found [here](https://github.com/networknt/light-java-example/tree/master/performance)
with the testing result on my desktop.

## Test Results

Here is the light-java server performance.

```
steve@joy:~/tool/wrk$  wrk -t4 -c128 -d30s http://localhost:8080 -s pipeline.lua --latency -- / 16
Running 30s test @ http://localhost:8080
  4 threads and 128 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     2.46ms    3.83ms  85.33ms   89.36%
    Req/Sec   366.70k    62.79k  714.24k    79.06%
  Latency Distribution
     50%    1.05ms
     75%    2.69ms
     90%    6.58ms
     99%   17.43ms
  43814624 requests in 30.07s, 4.33GB read
Requests/sec: 1457257.99
Transfer/sec:    147.31MB

```

Here is the spring-boot-tomcat (tomcat embedded) performance.

```
steve@joy:~/tool/wrk$  wrk -t4 -c128 -d30s http://localhost:8080 -s pipeline.lua --latency -- / 16
Running 30s test @ http://localhost:8080
  4 threads and 128 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    82.93ms  108.77ms   1.58s    89.45%
    Req/Sec     8.40k     3.68k   22.19k    68.54%
  Latency Distribution
     50%   45.66ms
     75%  101.59ms
     90%  197.72ms
     99%  542.87ms
  995431 requests in 30.09s, 119.79MB read
Requests/sec:  33086.22
Transfer/sec:      3.98MB

```

Here is the spring-boot-undertow (undertow embedded) performance.

```
steve@joy:~/tool/wrk$ wrk -t4 -c128 -d30s http://localhost:8080 -s pipeline.lua --latency -- / 16
Running 30s test @ http://localhost:8080
  4 threads and 128 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    38.94ms   39.29ms 456.82ms   89.28%
    Req/Sec    11.21k     4.97k   28.16k    68.14%
  Latency Distribution
     50%   27.58ms
     75%   49.62ms
     90%   80.73ms
     99%  201.87ms
  1331312 requests in 30.08s, 192.98MB read
Requests/sec:  44260.61
Transfer/sec:      6.42MB
```

Basically, light-java is 44 times faster then sprint-boot with tomcat embedded just
for the raw performance to serve Hello World! 

In order to have a closer comparison, I have created another project spring-boot-undertow with embedded
undertow servlet container (light-java is using undertow core only) and the 
performance is getting a little better. Light-Java is about 33 times faster than spring-boot with undertow embedded.


Upon requests from the community, I have added nodejs and golang examples and here are the testing result.

Node express framework.
To start the server

```
cd node-express
npm install
node server.js

```
The test result.

```
steve@joy:~/tool/wrk$ wrk -t4 -c128 -d30s http://localhost:8080 -s pipeline.lua --latency -- / 16
Running 30s test @ http://localhost:8080
  4 threads and 128 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    22.30ms   24.35ms 592.24ms   49.18%
    Req/Sec    10.70k     0.87k   11.95k    94.82%
  Latency Distribution
     50%   47.94ms
     75%    0.00us
     90%    0.00us
     99%    0.00us
  1274289 requests in 30.02s, 279.51MB read
Requests/sec:  42443.34
Transfer/sec:      9.31MB

```

Go Standard Http

To start the server
```
cd go-http
go run server.go -prefork
```

The testing result.

```
steve@joy:~/tool/wrk$ wrk -t4 -c128 -d30s http://localhost:8080 -s pipeline.lua --latency -- / 16
Running 30s test @ http://localhost:8080
  4 threads and 128 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    15.01ms   15.35ms 180.11ms   87.10%
    Req/Sec    42.80k     5.46k   62.49k    70.58%
  Latency Distribution
     50%   10.03ms
     75%   19.96ms
     90%   34.55ms
     99%   72.99ms
  5123194 requests in 30.08s, 630.28MB read
Requests/sec: 170313.02
Transfer/sec:     20.95MB

```

Go FastHttp

To start the server

```
cd go-fasthttp
go run server.go -prefork

```
The testing result.

```
steve@joy:~/tool/wrk$ wrk -t4 -c128 -d30s http://localhost:8080 -s pipeline.lua --latency -- / 16
Running 30s test @ http://localhost:8080
  4 threads and 128 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency    99.98ms  127.12ms 653.72ms   82.12%
    Req/Sec   351.24k    46.23k  525.74k    77.09%
  Latency Distribution
     50%   30.76ms
     75%  175.44ms
     90%  299.14ms
     99%  476.20ms
  41989168 requests in 30.06s, 4.93GB read
Requests/sec: 1396685.83
Transfer/sec:    167.83MB

```

After I post this online, one of spring developers recommended to test against Spring Boot with Reactor
which is Netty based without servlet container. I am very new to this and might miss something and everyone
is welcomed to submit pull request to enhance this project.

Here is the test result.

```
steve@joy:~/tool/wrk$ wrk -t4 -c128 -d30s http://localhost:3000 -s pipeline.lua --latency -- / 16
Running 30s test @ http://localhost:3000
  4 threads and 128 connections
  Thread Stats   Avg      Stdev     Max   +/- Stdev
    Latency     7.44ms   12.88ms 285.71ms   94.23%
    Req/Sec    61.44k    12.25k   88.29k    79.23%
  Latency Distribution
     50%    4.62ms
     75%    8.11ms
     90%   15.03ms
     99%   42.60ms
  7305649 requests in 30.03s, 536.48MB read
Requests/sec: 243240.17
Transfer/sec:     17.86MB

```
