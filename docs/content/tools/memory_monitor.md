---
date: 2016-10-27T08:44:57-04:00
title: Memory Monitoring on Mac
---

# Introduction

While you are testing performance of your microservices, you might be interested in
monitoring your server memory usage. The memory footprint is very important as we
are talking about microservices - we might need to deploy hundreds or even thousands
of microservices to compose a big application. In today's cloud environment, you can
create vms or containers based on cpu, memory and hard drive usage and usually memory
is the big constraint and more costly than cpu and hard drive. 

# Monitoring Memory Usage for Java

It is very hard to get the accurate memory usage for Java process. 


# On Mac

On Mac, I usually start my performance test with [wrk]() and run it for 1 minute and
monitor the memory with Activity Monitor. The memory tab will give you a snapshot of
memory usage on each process. The number is not the real memory usage but the max
memory usage and you can see it only increases to certain point and stays there.


# On Linux


# On Windows

I am not using Windows so I have limited experience on how to monitor Java memory usage
on this platform. However, the generic JDK tools should work as other platforms. 

If anyone has better ideas, please let me know or send me a pull request. 


# Example





