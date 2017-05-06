---
date: 2016-10-22T20:22:34-04:00
title: Database
---

# Introduction

This example can be found at [https://github.com/networknt/light-example-4j/tree/master/database](https://github.com/networknt/light-example-4j/tree/master/database)

There are three parts in this projects:

* An API implemented on top of light-4j framework.
* Mysql database as part of the docker compose with init script.
* Postgres database as part of the docker compose with init script.

The project shows:

* How to use [HikariCP](https://github.com/brettwooldridge/HikariCP) for JDBC connection pool.
* How to initialize database connection pool and plug it into light-4j startup hooks.
* How to dockerize light-4j application.
* How to compose databases with light-4j application so that you can start all servers together.
* How to performance test API application with wrk.
* Postgres performs better than mysql on my i5 desktop.

