---
date: 2017-02-06T21:33:54-05:00
title: Service
---

A light-weight and fast dependency injection framework without any third
party dependencies. It only support constructor inject and the injection is done
during server startup. All the object is saved into a map and the key is the
interface class. That can guarantee that only one instance of implementation
is available during runtime. 

Light 4J framework encourage developers to build microservices with Functional
Programming Style. One of the key principle is immutability so that the code can
be optimized to take advantage of multi-core CPUs. 

Unlike other IoC container, our service module only deals with singleton during
server startup with constructor injection. It give developer an opportunity to
choose from several implementations of an interface in service.yml

The following is an example of service.yml in the test folder for this module.

```
# singleton service factory configuration
singletons:
- com.networknt.service.A:
  - com.networknt.service.AImpl
- com.networknt.service.B:
  - com.networknt.service.BTestImpl
- com.networknt.service.C:
  - com.networknt.service.CImpl
- com.networknt.service.Processor:
  - com.networknt.service.ProcessorAImpl
  - com.networknt.service.ProcessorBImpl
  - com.networknt.service.ProcessorCImpl
- com.networknt.service.D1, com.networknt.service.D2:
  - com.networknt.service.DImpl
- com.networknt.service.E,com.networknt.service.F:
  - com.networknt.service.EF1Impl
  - com.networknt.service.EF2Impl
- com.networknt.service.G:
  - com.networknt.service.GImpl:
      name: Sky Walker
      age: 23
- com.networknt.service.J,com.networknt.service.K:
  - com.networknt.service.JK1Impl:
      jack: Jack1
      king: King1
  - com.networknt.service.JK2Impl:
      jack: Jack2
      king: King2
- com.networknt.service.L:
  - com.networknt.service.LImpl:
      protocol: https
      host: localhost
      port: 8080
      parameters:
        key1: value1
        key2: value2
- com.networknt.service.M:
  - com.networknt.service.MImpl:
    - java.lang.String: Steve
    - int: 2
    - int: 3

```

Here is the code that gets the singleton object from service map. 

```
A a = (A)SingletonServiceFactory.getBean(A.class);
```

