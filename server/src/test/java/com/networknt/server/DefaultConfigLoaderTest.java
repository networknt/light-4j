package com.networknt.server;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultConfigLoaderTest {

    public static DefaultConfigLoader configLoader;

    @BeforeClass
    public static void beforeClass() {
        configLoader = new DefaultConfigLoader();
    }

    @Test
    public void testProcessNestedMap() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("test1",
                "\n" +
                "  languages:\n" +
                "    - Ruby\n" +
                "    - Perl\n" +
                "    - Python \n" +
                "  websites:\n" +
                "    YAML: yaml.org\n" +
                "    Ruby: ruby-lang.org\n" +
                "    Python: python.org\n" +
                "    Perl: use.perl.org");
        map.put("test2",
                "\n" +
                "  - aaaa\n" +
                "  - bbbb\n" +
                "  -\n" +
                "    id: 1\n" +
                "    name: company1\n" +
                "    price: 200W\n" +
                "  -\n" +
                "    id: 2\n" +
                "    name: company2\n" +
                "    price: 500W\n" +
                "  -\n" +
                "    -aaaa\n" +
                "    -bbbb\n" +
                "  - abc: abc\n" +
                "    ccc: ccc\n" +
                "    ddd: ddd");
        Method processNestedMapMethod = DefaultConfigLoader.class.getDeclaredMethod("processNestedMap", Map.class);
        processNestedMapMethod.setAccessible(true);
        processNestedMapMethod.invoke(configLoader, map);

        Map<String, Object> result = (Map)map.get("test1");
        List<String> result1 = (List)result.get("languages");
        Assert.assertEquals("Ruby", result1.get(0));
        Map<String, String> result2 = (Map)result.get("websites");
        Assert.assertEquals( "yaml.org", result2.get("YAML"));
        System.out.println(map);

    }
}
