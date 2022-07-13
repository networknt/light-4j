package com.networknt.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.service.SingletonServiceFactory;

/**
 * All POJO transformer classes must implement this interface so that it can be loaded via
 * service.yml configuration from the rule engine to do the request/response body transformation
 * between JSON to JSON, JSON to XML and XML to JSON. The idea is to transform the source to
 * a JAVA POJO and then manipulate/or copy it into another object to serialize it into another
 * format.
 *
 * @author Steve Hu
 */
public interface Transformer {
    /**
     * Transform between the source and target. The source and target can be JSON and XML in
     * String format. An implementation of this interface would know what is the source and
     * what is the target.
     *
     * @param input String input for the source
     * @return String output after the transformation
     */
    String transform(String input);

    /**
     * return the name of the transformer that is used to identify the right transformer for the
     * rules associated with the endpoint. For a particular request path or endpoint, different
     * transformer implementation should be used on the gateway or sidecar or application.
     * @return String transformer name
     */
    String getName();

    /**
     * A static method to look up a Transformer from service.yml based on the name.
     *
     * @param name of the transformer
     * @return An instance of Transformer that matches the name
     */
    static Transformer lookupTransformer(String name) {
        Transformer[] transformers = SingletonServiceFactory.getBeans(Transformer.class);
        Transformer transformer = null;
        for(Transformer t: transformers) {
            if(t.getName().equals(name)) {
                transformer = t;
                break;
            }
        }
        return transformer;
    }
}
