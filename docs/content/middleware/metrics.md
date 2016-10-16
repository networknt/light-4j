---
date: 2016-10-15T20:42:32-04:00
title: Metrics
---

## Introduction

Metrics handler collects the API runtime information and report to Influxdb periodically 
(5 minutes to 15 minutes based on the volume of the API). A Grafana instance is hooked to Influxdb
to output the metrics on dashboard from two different perspectives:
 
* Client oriented - client centric info to show how many APIs to call and each API runtime info.

* API oriented - API centric info to show how many clients is calling this API.

## Configuration



## Customization

The default implementation is based on InfluxDB and Grafana which are the most popular combination
for docker containers. However, the database and dashboard can be replaced easily with another
reporter implementation. To replace it, change the MetricsHandler to use another reporter instead of
InfluxDB.

