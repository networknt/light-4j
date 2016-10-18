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

Here is an example of configuration.

```
{
  "description": "Metrics handler configuration",
  "enabled": true,
  "influxdbHost": "localhost",
  "influxdbPort": 8086,
  "influxdbName": "metrics",
  "influxdbUser": "admin",
  "influxdbPass": "admin",
  "reportInMinutes": 5
}
```

## InfluxDB and Grafana

Here is the docker-compose.yml

```
influxdb:
  image: influxdb:latest
  container_name: influxdb
  ports:
    - "8083:8083"
    - "8086:8086"

grafana:
  image: grafana/grafana:latest
  container_name: grafana
  ports:
    - "3000:3000"
  links:
    - influxdb
```

## Metrics Collected

![measurements](/images/measurements.png)

As you can see, there two perspectives in collecting metrics info. The measurements started with clientId are client
centric info and the measurements started with API name are API centric info.

Currently, we are collecting 5 metrics each.


![client metrics](/images/client_metrics.png)

This is request count for client f7d42348-c647-4efb-a52d-4c5787421e72

The first column is timestamp and value column is the value for this time series. Other columns are tags and they
are endpoint, hostname/container id, ipAddress and version.


![api metrics](/images/api_metrics.png)

This is request count for API swagger_petstore.

The first column is timestamp and value column is the value for this time series. Other columns are tags and they
are endpoint, hostname/container id, ipAddress and version.


## Customization

The default implementation is based on InfluxDB and Grafana which are the most popular combination
for docker containers. However, the database and dashboard can be replaced easily with another
reporter implementation. To replace it, change the MetricsHandler to use another reporter instead of
InfluxDB.

