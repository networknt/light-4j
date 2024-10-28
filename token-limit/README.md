This is a handler that is used to rate limit the OAuth 2.0 token endpoint to ensure all client are caching the token. This is the way to identfy any bad citizens that are getting a new token per request.

This handler can be used on the oauth-kafka server or dedicate light-gateway for OAuth token and key services.

For lower environments, we want to return an error message if token is not cached by the client. However, on production, we need to allow the request to complete with a warning in the log. So this should be configurable.
