---
date: 2016-10-23T13:20:22-04:00
title: Utility
---

This module contains some useful classes that shared by multiple modules within
the framework.

# Constants

Contains all the constants shared by all modules.

# ModuleRegistry

When the plugin modules are loaded, it will register itself to this module along
with configuration. When /server/info is called, the endpoint will return all
plugged in modules and their configurations.

# Util

Some useful utility method like uuid generator etc.

