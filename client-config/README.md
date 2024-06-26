## client-config

The client-config module is a component shared by both the client module in light-4j and light-aws-lambda for both the Undertow-based client and JDK 11 http-client. It is moved from the client module with the new features to support reloading.

It is based on the same client.yml and supports Undertow and JDK  client syntaxes. To ensure that this module is backward-compatible, we will follow the Undertow client syntax if there are any discrepancies.
