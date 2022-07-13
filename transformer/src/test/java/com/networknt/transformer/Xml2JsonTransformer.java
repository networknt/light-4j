package com.networknt.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.networknt.config.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Xml2JsonTransformer implements Transformer {
    static final Logger logger = LoggerFactory.getLogger(Xml2JsonTransformer.class);
    @Override
    public String transform(String input) {
        XmlMapper xmlMapper = new XmlMapper();
        String output = null;
        try {
            Flower poppy = xmlMapper.readValue(input, Flower.class);
            output = JsonMapper.toJson(poppy);
        } catch (JsonProcessingException e) {
            logger.error("Transform exception:", e);
        }
        return output;
    }

    @Override
    public String getName() {
        return "flowerXml2Json";
    }
}
