---
date: 2017-06-19T21:49:05-04:00
title: Idempotency
---

# Networks are unreliable. 

The networks connecting our clients and servers are, on average, more reliable than 
consumer-level last miles like cellular or home ISPs, but given enough information 
moving across the wire, they’re still going to fail given enough time. Outages, 
routing problems, and other intermittent failures may be statistically unusual on 
the whole, but still bound to be happening all the time at some ambient background rate.

While in microservices architecture, the number of network connections grow
exponentially and the risk of network issues will be much higher than monolithic
applications. 

To overcome this sort of inherently unreliable environment, it’s important to design 
APIs and clients that will be robust in the event of failure, and will predictably 
bring a complex integration to a consistent state despite failures. Remember that a
service might be a client when calling another service.

# Planning for failure
  
Consider a call between any two nodes. There are a variety of failures that can occur:

- The initial connection could fail as the client tries to connect to a server.

- The call could fail midway while the server is fulfilling the operation, leaving 
the work in limbo.

- The call could succeed, but the connection break before the server can tell its 
client about it.

Any one of these leaves the client that made the request in an uncertain situation. 
In some cases, the failure is definitive enough that the client knows with good certainty 
that it’s safe to simply retry it. For example, a total failure to even establish a 
connection to the server. In many others though, the success of the operation is ambiguous 
from the perspective of the client, and it doesn’t know whether retrying the operation is 
safe. A connection terminating midway through message exchange is an example of this case.

This problem is a classic staple of distributed systems, and the definition is broad when 
talking about a “distributed system” in this sense: as few as two computers connecting 
via a network that are passing each other messages.
 
With microservices are getting popular, the partial failure is more complicated as it can
happen deep in the chain of services. It is not a simple retry from any client and the retry
has to be initialized from the original client. This requires all the services in the chain
must be idempotent.


# Use of idempotency

The easiest way to address inconsistencies in distributed state caused by failures is to 
implement server endpoints so that they’re idempotent, which means that they can be called 
any number of times while guaranteeing that side effects only occur once.

When a client sees any kind of error, it can ensure the convergence of its own state with 
the server’s by retrying, and can continue to retry until it verifiably succeeds. This 
fully addresses the problem of an ambiguous failure because the client knows that it can 
safely handle any failure using one simple technique.

As an example, consider the API call for a hypothetical DNS provider that enables us to add 
subdomains via an HTTP request:


All the information needed to create a record is included in the call, and it’s perfectly 
safe for a client to invoke it any number of times. If the server receives a call that it 
realizes is a duplicate because the domain already exists, it simply ignores the request and 
responds with a successful status code.

According to HTTP semantics, the PUT and DELETE verbs are idempotent, and the PUT verb in 
particular signifies that a target resource should be created or replaced entirely with the 
contents of a request’s payload (in modern RESTful service, a partial modification would be 
represented by a PATCH).

We have used request/response communication style as an example; however, messaging based
microservices should follow the same approach in case duplicate messages are delivery in the
pipeline.

# Guarantee exactly once

While the inherently idempotent HTTP semantics around PUT and DELETE are a good fit for many 
API calls, what if we have an operation that needs to be invoked exactly once and no more? 
An example might be if we were designing an API endpoint to charge a customer money; accidentally 
calling it twice would lead to the customer being double-charged, which is very bad.

This is where idempotency keys come into play. When performing a request, a client generates a 
unique ID to identify just that operation and sends it up to the server along with the normal 
payload. The server receives the ID and correlates it with the state of the request on its end. 
If the client notices a failure, it retries the request with the same ID, and from there it’s 
up to the server to figure out what to do with it.

If we consider our sample network failure cases from above:

- On retrying a connection failure, on the second request the server will see the ID for the 
first time, and process it normally.

- On a failure midway through an operation, the server picks up the work and carries it through. 
The exact behavior is heavily dependent on implementation, but if the previous operation was 
successfully rolled back by way of an ACID database, it’ll be safe to retry it. Otherwise, state 
is recovered and the call is continued.

- On a response failure (i.e. the operation executed successfully, but the client couldn’t get 
the result), the server simply replies with a cached result of the successful operation.

In light-4j, we have a component traceability that is a middleware handler to move traceabilityId
from request header to response header. Also, the client module which is used to call services
has the ability to propagate traceabilityId to the next request in the service call stack. This
header can be used as idempotency key on mutating services(i.e. anything under POST in our case).

If the above one request fails due to a network connection error, you can safely retry it 
with the same idempotency key, and the services must be designed to work with the idempotency key
/traceabilityId to decide what the business logic with the key.

# Being a good distributed citizen

Safely handling failure is hugely important, but beyond that, it’s also recommended that it be 
handled in a considerate way. When a client sees that a network operation has failed, there’s 
a good chance that it’s due to an intermittent failure that will be gone by the next retry. 
However, there’s also a chance that it’s a more serious problem that’s going to be more tenacious; 
for example, if the server is in the middle of an incident that’s causing hard downtime. Not only 
will retries of the operation not go through, but they may contribute to further degradation.

It’s usually recommended that clients follow something akin to an exponential backoff algorithm 
as they see errors. The client blocks for a brief initial wait time on the first failure, but 
as the operation continues to fail, it waits proportionally to 2n, where n is the number of 
failures that have occurred. By backing off exponentially, we can ensure that clients aren’t 
hammering on a downed server and contributing to the problem.

Exponential backoff has a long and interesting history in computer networking.

Furthermore, it’s also a good idea to mix in an element of randomness. If a problem with a 
server causes a large number of clients to fail at close to the same time, then even with 
back off, their retry schedules could be aligned closely enough that the retries will hammer 
the troubled server. This is known as the thundering herd problem.

We can address thundering herd by adding some amount of random “jitter” to each client’s wait 
time. This will space out requests across all clients, and give the server some breathing room 
to recover.

The client module in [light-4j](https://github.com/networknt/light-4j) will be enhanced to retry 
on failure automatically with an idempotency key using increasing backoff times and jitter. Given
not all client need to retry on failure, we need to make sure it is configurable in client.yml so
that retry can only be initiated from the original client.


# Design robust APIs

Considering the possibility of failure in a distributed system and how to handle it is of 
paramount importance in building APIs that are both robust and predictable. Retry logic on 
clients and idempotency on servers are techniques that are useful in achieving this goal and 
relatively simple to implement in any technology stack.

Here are a few core principles to follow while designing your clients and APIs:

- Make sure that failures are handled consistently. Have clients retry operations against 
remote services. Not doing so could leave data in an inconsistent state that will lead to 
problems down the road.

- Make sure that failures are handled safely. Use idempotency and idempotency keys to allow 
clients to pass a unique value and retry requests as needed.

- Make sure that failures are handled responsibly. Use techniques like exponential backoff 
and random jitter. Be considerate of servers that may be stuck in a degraded state.

