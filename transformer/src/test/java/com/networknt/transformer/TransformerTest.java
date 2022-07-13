package com.networknt.transformer;

import org.junit.Assert;
import org.junit.Test;

public class TransformerTest {
    @Test
    public void testNotificationTransform() {
        String source = "{\"data\":null,\"notifications\":[{\"code\":\"ERR00610000\",\"message\":\"Exception in getting service:Unable to create user info\",\"timestamp\":1655739885937,\"metadata\":null,\"description\":\"Internal Server Error\"}]}";
        String target = "{\"status\":\"NOT FOUND\",\"message\":\"Exception in getting service:Unable to create user info\",\"timestamp\":{\"epochSecond\":1655739885,\"nano\":937000000}}";
        Transformer transformer = Transformer.lookupTransformer("notification");
        String s = transformer.transform(source);
        System.out.println("s = " + s);
        Assert.assertEquals(s, target);
    }

    @Test
    public void testJson2XmlTransform() {
        String source = "{\"name\":\"Poppy\",\"color\":\"RED\",\"petals\":9}";
        String target = "<Flower><name>Poppy</name><color>RED</color><petals>9</petals></Flower>";
        Transformer transformer = Transformer.lookupTransformer("flowerJson2Xml");
        String s = transformer.transform(source);
        System.out.println("s = " + s);
        Assert.assertEquals(s, target);
    }

    @Test
    public void testXml2JsonTransform() {
        String source = "<Flower><name>Poppy</name><color>RED</color><petals>9</petals></Flower>";
        String target = "{\"name\":\"Poppy\",\"color\":\"RED\",\"petals\":9}";
        Transformer transformer = Transformer.lookupTransformer("flowerXml2Json");
        String s = transformer.transform(source);
        System.out.println("s = " + s);
        Assert.assertEquals(s, target);
    }
}
