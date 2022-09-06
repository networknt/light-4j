package com.networknt.http;

public interface HttpService<I, O> {
    ResponseEntity<O> invoke(RequestEntity<I> requestEntity);
}
