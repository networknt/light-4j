Move the CacheExplorerHandler to this module to ensure that cache-manager doesn't have Undertow as a dependency. So the cache-manager can be used in the light-aws-lambda.
