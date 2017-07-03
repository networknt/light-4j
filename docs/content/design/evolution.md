---
date: 2017-06-30T13:53:11-04:00
title: Service Evolution
---

Change is happening all around us, new technologies, new methodologies, but how 
are these changes affecting the ways in which systems are architected and how do 
recent developments like patterns and refactoring cause us to think differently 
about architecture? 

When microservices were introduced, one of the benefit is you can make changes to
the services easily. Is this the case if there are so many consumers are depending
on the it although the consumers and services are loosely coupled? In reality, it
is not easy to make service changes once it is on production and the root cause is
due to designers/developers still follow the pattern of Java EE RMI approach to
build microservices and their consumers with POJO.

# Example of Evolving a Service

Let's say we have a restful service called party which can output all the profile
information about customers. Here is the output in JSON.

```
{
  "id": 12345,
  "operatorId": "67890",
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@nobody.com",
  "age": 27 
}
```

The party service is currently consumed by two applications: a web application and
a mobile native application. Both consumers validate the received response prior 
to processing them. The web application uses id, firstName, lastName and email fields; 
the mobile application only uses id and email fields. Neither uses the age field. 

One of the most common ways in which we might evolve a service is to add an additional
field to a contract on behalf of one or more consumers. Depending on how the provider 
and consumers have been implemented, even a simple change like this can have costly 
implications for the business and its partners.

In our example, after the party service has been in production for some time, a second 
mobile application considers using it, but asks that a type field be added to each party. 
Because of the way the consumers have been built, the change has significant and costly 
implications both for the provider and the existing consumers, the cost to each varying 
based on how we implement the change. There are at least two ways in which we can 
distribute the cost of change between the members of the service community. First, we 
could modify our original schema and require each consumer to update its copy of the 
schema in order correctly to validate search results; the cost of changing the system 
is here distributed between the provider - who, faced with a change request like this, 
will always have to make some kind of change - and the consumers, who have no interest 
in the updated functionality. Alternatively, we could choose to add a second operation 
and schema to the service provider on behalf of the new consumer, and maintain the 
original operation and schema on behalf of the existing consumers. The cost of change 
is now constrained to the provider, but at the expense of making the service more complex 
and more costly to maintain.


# Extension Points

Making schemas both backwards- and forwards-compatible is a well-understood design task, 
best expressed by the Must Ignore pattern of extensibility. The Must Ignore pattern 
recommends that schemas incorporate extensibility points, which allow extension elements 
to be added to types and additional attributes to each element. The pattern also 
recommends that schema languages define a processing model that specifies how consumers 
process extensions. The simplest model requires consumers to ignore elements that they do 
not recognize - hence the name of the pattern. The model may also require consumers to 
process elements that have a "Must Understand" flag, or abort if they cannot understand 
them.


The schema includes an optional extension object. The extension itself can contain one 
or more attributes. Now when we receive a change request to add a type to each party, 
we can publish a new schema with an additional type attribute that the provider inserts 
into the extension container. This allows the party service to return results that include 
party type, and consumers using the new schema to validate the entire document. Consumers 
using the old schema will not break, though they will not process the type. The new results 
documents look like this:

This is the updated example on party service:

```
{
  "id": 12345,
  "operatorId": "67890"
  "firstName": "John",
  "lastName": "Doe",
  "email": "john.doe@nobody.com",
  "age": 27,
  "extension": {
    "type": "retail"
  }
}
```

Note that the first version of the extensible schema is forwards-compatible with the 
second, and that the second is backwards-compatible with the first. This flexibility, 
however, comes at the expense of increased complexity. Extensible schemas allow us to 
make unforeseen changes to an schema language, but at the same time, they provide for 
requirements that may very well never arise and they obscure the expressive power that 
comes from a simple design, and frustrate the meaningful representation of business 
information by introducing meta-informational container attribute into the domain 
language.

We'll not discuss schema extensibility further here. Suffice to say, extension points 
allow us to make backwards- and forwards-compatible changes to schemas and documents 
without breaking service providers and consumers. Schema extensions do not help us 
manage the evolution of a system when we need to make what is a breaking change to a 
contract.

# Breaking Change

The party service depends on a security subsystem to populate operatorId for
consumers to access other system which uses this field to verify if access is allowed.
It was designed this way but never used and the security subsystem is too old to be
maintained. The service provider discuss with all consumers to evaluate if this field
can be removed. Given this field is not part of the extension points, this is a breaking
change although it is not been used by any of the consumers and all consumers confirm 
that they have to release a new version which is costly. The party service in this 
respect cannot evolve independently of its consumers. Provider and consumers must all 
jump at the same time.

Our service community is frustrated in its evolution because each consumer implements a 
form of "hidden" coupling that naively reflects the entirety of the provider contract 
in the consumer's internal logic. The consumers, through their use of JSON schema 
validation and JSON to POJO static language bindings derived from a schema, implicitly 
accept the whole of the provider contract, irrespective of their appetite for processing 
the component parts.

David Orchard provides some clues as to how we might have avoided this issue when he 
wrote to the Internet Protocol's Robustness Principle: "In general, an implementation 
must be conservative in its sending behaviour and liberal in its receiving behaviour". 
We can augment this principle in the context of service evolution by saying that message 
receivers should implement "just enough" validation: that is, they should only process 
data that contributes to the business functions they implement, and should only perform 
explicitly bounded or targeted validation of the data they receive - as opposed to the 
implicitly unbounded, "all-or-nothing" validation inherent in schema processing.

# Jsoniter Any

One way we can target or bound consumer-side validation and processing is to extract 
pattern expressions along the received message's document tree structure, perhaps using 
a zero copy JSON parser like [Jsoniter](https://github.com/json-iterator/java) instead of 
Jackson POJO binding. Using Jsoniter, each consumer of the party service can 
programmatically assert/process what it expects to find in the response.


Notice that this approach makes no assertions about attributes in the underlying document 
for which the consuming application has no appetite. In this way, the validation language 
explicitly targets a bounded set of required attributes. Changes to the underlying 
document's schema will not be picked up by the validation process unless they disturb 
the explicit expectations described in consumer side contract, even if those changes 
extend to deprecating or removing formerly mandatory attributes.

Here is a relatively lightweight solution to our contract and coupling problems, and one 
that doesn't require us to add obscure meta-informational elements to a document. So 
let's roll back time once again, and reinstate the simple schema described at the beginning
of the article. But this time, we'll also insist that consumers are liberal in their 
receiving behaviour, and only validate and process information that supports the business 
functions they implement. Now when the provider is asked to add a type to each party, the 
service can publish a revised schema without disturbing existing consumers. Similarly, 
on discovering that the operatorId field is not validated or processed by any of the 
consumers, the service can revise the service results schema - again without disturbing 
the rate of evolution of each of the consumers.


# Conclusion

Unlike monolithic applications, communications between client and service should be loosely
coupled with individual attributes in JSON instead of POJO which is popular in RMI. By
doing so, we can ensure that the service can be evolved easily without multiple versions
running in parallel. Given this, when we write [light-codegen](https://github.com/networknt/light-codegen), 
we deliberately removed POJO generator to encourage developer to not bind service response
to a POJO which is time consuming and tightly coupled your consumer to the service. 

Also, to give confidence for service provider to evolve, it is wise to ask all consumers
to provide test cases which described consumers expectations in their processing as part
of the regression test for the service. Once service changes happen, it only need to run
the entire regression test cases to ensure nothing is broken on the consumer side. This
consumer expectation is called [consume contract](https://networknt.github.io/light-4j/design/consumer-contract/) 
which is another topic that is out of scope of this article. 


