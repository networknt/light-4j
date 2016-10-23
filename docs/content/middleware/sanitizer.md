---
date: 2016-10-23T10:35:04-04:00
title: Sanitizer
---

# Introduction

This is a middleware that addresses cross site scripting concerns. It encodes the header
and body according to the configuration. As body encoding depends on
[Body](https://networknt.github.io/light-java/middleware/body/) middleware, it has to be
plugged into the request/response chain after Body.

