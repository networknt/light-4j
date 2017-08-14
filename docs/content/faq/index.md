---
date: 2017-08-12T10:14:30-04:00
title: FAQ
---

## How to compress the response with gzip

For some of the API endpoints, the response body are big and it might be necessary
to compress it before return it to the client. Undertow has a built-in EncodingHandler
that can be utilize for this.

Here is an example with customized path provider.

```
public` HttpHandler getHandler() {

    return new EncodingHandler(new ContentEncodingRepository()
            .addEncodingHandler("gzip",
                    new GzipEncodingProvider(), 50,
                    Predicates.parse("max-content-size[150]")))
            .setNext(
                    Handlers.routing()

                            .add(Methods.GET, "/v2/fronts/{front_id}", new FrontGetHandler(Config.getInstance().getMapper()))

                            .add(Methods.GET, "/v2/articles/{article_id}", new ArticleGetHandler(Config.getInstance().getMapper()))

                            .add(Methods.GET,"/v2/medias/{media_id}", new MediaGetHandler(Config.getInstance().getMapper()))

                            .add(Methods.GET,"/v2/menus/{site_id}", new MenuGetHandler(Config.getInstance().getMapper()))

                            .add(Methods.GET,"/v2/sections/{section_id}", new SectionGetHandler(Config.getInstance().getMapper()))

            );
}
```

For more information about the implementation, please refer to https://github.com/networknt/light-4j/issues/88

Thanks @samek to confirm it works and provide the example.

## How to serve Single Page Application(React/Angular etc.) from your API server instance

During development, we normally use Nodejs to serve the single page application as it is easy
to package and test/debug. However, on production, it makes sense that the API server serves
the single page application by itself. In this way, we don't need to enable [CORS handler](https://networknt.github.io/light-4j/middleware/cors/)
and same another instance of the server.

To learn how to use Nodejs for Single Page Application development, please take a look at
https://github.com/networknt/light-codegen/tree/master/codegen-web

Here I will give you an example to show you how to package your application and deploy it
with the API built on top of light-*-4j frameworks.

There are two options depending on if you are using Docker or not.

### Without Docker

You can serve the SPA with command line to start the server. The only thing that you need
to make sure is to have the webserver.json config file point to the exact local filesystem
directory. The default config point to the relative folder which is not going to work.

For more info, please take a look at the [webserver example](https://github.com/networknt/light-example-4j/tree/master/webserver)
and its readme.

### With Docker

It is much easier to deploy SPA with containerized API as you can just put your SPA into
a folder in your host machine and then map into the Docker with a volume. The API server
can find the static files and serve them. Also, it allow you to change the UI easier without
touching the API.

Take a look at the [Dockerfile](https://github.com/networknt/light-example-4j/blob/master/webserver/Dockerfile)
in webserver example and pay attention on the volume that maps a host folder to /public in
the container.

