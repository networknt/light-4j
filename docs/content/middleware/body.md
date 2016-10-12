---
date: 2016-10-12T18:57:17-04:00
title: Body
---

# Introduction

Body is an HttpHandler to parse the body according to the content type int the 
request header and attach the parsed result into the exchange so that subsequent 
handlers will use it directly. 

The current implementation is to convert the input stream to String object so 
that it can be used in validator handler in the handler chain.
