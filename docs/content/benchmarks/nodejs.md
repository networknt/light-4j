---
date: 2016-11-18T14:41:29-05:00
title: Nodejs Pitfalls
---

When talking about microservices, a lot of Nodejs developers will say that 
Node is a better platform than Java and other languages to build microservices. 
The arguments are: 

* Nodejs is faster than Java at runtime.
* Nodejs development is more productive than Java.

These claims are proved to be false already as I've been working on both 
platforms in banking industry for the last couple of years. I have been
building REST API frameworks on both Java and Nodejs and the following are
my observations on Nodejs in enterprise computing.

## Production hell

As workers are single-threaded, any uncaught exception will crash the worker
and all the in-flight transactions will be lost and you don't know what is 
the state of each in-flight request. This is the biggest problem faced and
I have worked with several core Nodejs developers trying to mitigate the risk.

The end result is to capture it in the server.js and then wait for in-flight
to complete as many as possible in a unknown state of the server and then
shutdown the server. Not ideal but it reduce the risk to certain level. Due to
this reason, Nodejs API platform is only recommended for readonly API in the
bank. 

Node server crashes under load (cpu 90% or up). We found this during out loaded
test with Winston logger enabled and the server always dead without any response
after a while. The reason is these callbacks don't have enough cpu time to
complete and there are piling up in memory until you run out of memory, the server
won't shutdown in this situation but simply won't respond. The solution for
us is to monitor the cpu usage and starting more containers when it is in 
heavy load. 

## Callback Hell

This is know issue for Javascript. Although promise helps a lot, it is still a
big issue with Node platform.

 
## Code Maintenance

Nodejs is easy to get into and very fast to build small Hello World application;
however, building complex enterprise level application with thousands of line of
code is very hard. When I look at my own code wrote one year ago, I couldn't
reason about what that piece code does. I have to put some debug info and run it
to figure out how the code works. Callback Hell is one of the causes. For 
enterprise applications, easy to understand and maintain is very important and I 
feel sorry for the bank employees who have taken over my work.

## Debugging

Javascript as a platform doesn't have a good runtime debugging tool that you can inspect
variables during runtime easily. There are some tools but none of them works as 
good as other languages. I use a customized logger built by myself and put a lot
of logging statement in order to debug my code and I've seen so many js developers
just use console.log:)

## No Transaction Support

No transaction support on the platform makes it not enterprise ready. Let's say 
you are building a stock order API, once the server receives the request, it needs
to save the order in backend database and route the order to an exchange for execution.
what if the communication to Exchange is down? There is no way that the local
database update can be rolled back. 

## Lack of Connectivity to Backend System

In the above order API, the order has to be routed to an exchange through MQ Queue
but node didn't support it 2 years ago. Also, a lot of existing backend
systems and databases are not supported even today. I have worked with Strongloop
and IBM teams for three months to make IBM DB2 
drive(https://github.com/ibmdb/node-ibm_db) worked on production without
memory leak. 

## Insufficient of Module and Version Management

A lot of people praises npm and I agree that it is very good tool to manage modules.
However, only manage modules is no enough, it has to manage modules with versions.

Most Node developers will have this experience. You have an application running today
and tomorrow, you run npm install again and it stops working:) As one of the dependencies
got a new version and it is not backward compatible. Shrinkwrap helps a little bit but
it is very hard to update one or two immediate dependent modules as there is no way to
update sub dependencies. Another way is to check in node_modules into git and packaged it
into docker container. Now we always package node_modules into docker image.

## Windows Un-Friendly

while trying to check in node_modules folder into git on Windows platform, most cases
you will get an error as some files are buried too deep in the directory and Windows 
has limitation on path length. This issue has been partially fixed in later version
of Node as npm tries to flatten all the dependencies.

Some of the modules depending on C/C++ that cannot be compiled on Windows. It causes
issues for teams that use different platforms for development. 

Given Windows is not case sensitive on file names, application developed on Windows
usually cannot be executed on Linux the first time.

## Long Running process hogs CPU

This is not a problem of Nodejs but mistake of developers. I have seen too many this
kind of mistakes and I want to highlight it here. As node is using event loop
to dispatch tasks/callbacks, if any callback designed wrongly and doesn't give up CPU
for a period of time, the entire system will suffer. If you have to process thousands
of records loaded from database, process them in 100 blocks. There are so many articles
talking about this topic. 

## Public Module Quality

No doubt there is a very active community for Nodejs and there are a lot of modules published
on public npm repository. I myself got several modules published. 

On the other hand, there are so many modules are in bad shape as developers of these modules
often migrated from frontend without any enterprise level experience. Some of modules got 
10 line of the code but will depending on 8 other modules. Write a small express application
in nodejs and take a look at how many modules in node_modules folder. Is you application
using them all? I guess less than 5 percent of the code in node_modules are in the 
execution path and the rest of them are just wasting your hard drive space.
 
A legendary Node developer TJ mentioned the same reason in his farewell article
regarding to modules. Javascript sets the bar very low and it attracts a lot of low level 
developers. Remember Visual Basic was the most popular language on Microsoft platform?

## Stability of the Platform

For one bank I worked last year, they are still using Nodejs 0.10.39 as that was the only
production ready version. Both Strongloop and Joyent told us to stay on that version and as
I understand, other customers are on the same version. We were told to upgrade to 0.12.x
once it was prouduction ready and then Nodejs and IO.js were merged and Nodejs 4 was out. 
Before Nodejs 4 was production ready, they've moved to Nodejs 5 and now on Nodejs 6.

We are having big issue with Nodejs 0.10.39 as https module is not performing with API to 
API calls and the issue was resolved in 0.12.x. So our recommendation for Nodejs API 
framework added another condition upon only readonly API - The API must not call another
API in https. All APIs that calling another API with https must be implemented in Java 
framework.


## Talents abandoned the ship

As you might know, TJ who is the developer of express - most popular nodejs framework left
Nodejs to GO. Here is his [farewell](https://medium.com/@tjholowaychuk/farewell-node-js-4ba9e7f3e52b#.5brqa9has)
There are other heavy weight Nodejs developers left and that might make you think what 
is going on.


## Existing Customers are stuck or leaving

There are some early adopters of Nodejs and all of them are trapped with old version of Nodejs
and they are so afraid to upgrade to the new version as memory leak will be hard to 
resolve - refer to my production hell.

Other companies, just rewrite the application with other language. One example is 
[here](https://medium.com/@theflapjack103/the-way-of-the-gopher-6693db15ae1f#.jfcsl8hlg)

For the bank I've worked, they wrote some APIs in Nodejs in 2014 but most of them were rewritten
in Java in 2016.

## Memory Footprint is high on multiple core platform

For one worker, it uses less memory than Java, but on a multi-core system, you have to start
workers per cpu core in order to utilize the resource to its full potential. These workers
are independent and there is no shared memory, they allocate heap independently. If you start
four or eight workers, it uses more memory than Java which has only one instance multi-threaded
with shared heap memory.


## Much slower than New Java platforms

As for speed, it is faster than WebSphere/WebLogic/JBoss but not in the same level as other
new containerless Java frameworks and platforms. 
 
Here is a [benchmarks](https://github.com/networknt/microservices-framework-benchmark) 
that have both popular Java microservices frameworks and Nodejs/Express. The above performance
result only focus on raw throughput and latency. While more code is added, Nodejs will be
getting slower and slower. 

## Summary

I work on both Nodejs and Java so my opinion is not biased but to point out the facts on
Nodejs platform. No doubt you can build rock solid Nodejs application with a group of senior
developers but it is very hard to find that level of developers. I am not saying Java is 
better as I know there are a lot of issues with Java. I just hope these points will help 
you in choosing your next application platform. 



