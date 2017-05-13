---
date: 2017-02-06T21:34:02-05:00
title: Switcher
---

This module implement a switcher service interface and a local implementation. Switch 
is useful at system runtime to turn on or off some logic or service given certain
conditions. For example, the light-4j server won't stop handling requests but just
switching off during server shutdown process. The service registry will be notified
but in coming requests are still processed until all clients receives notification from 
service registry. 


