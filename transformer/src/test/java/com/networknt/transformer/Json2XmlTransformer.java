package com.networknt.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.networknt.config.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Json2XmlTransformer implements Transformer {
    static final Logger logger = LoggerFactory.getLogger(Json2XmlTransformer.class);
    @Override
    public String transform(String input) {
        Flower poppy = JsonMapper.fromJson(input, Flower.class);
        XmlMapper xmlMapper = new XmlMapper();
        String output = null;
        try {
            output = xmlMapper.writeValueAsString(poppy);
        } catch (JsonProcessingException e) {
            logger.error("Transform exception:", e);
        }
        return output;
    }

    @Override
    public String getName() {
        return "flowerJson2Xml";
    }
}
