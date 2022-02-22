package com.networknt.exception;

import com.networknt.status.Status;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ExceptionProcessorTest {

    static DefaultExceptionProcessor defaultExceptionProcessor;
    static List<Class<? extends Throwable>> exceptions;

    @BeforeClass
    public static void setUp() throws  Exception{
        defaultExceptionProcessor = new DefaultExceptionProcessor();
        exceptions = new ArrayList<>();
        processAnnotationMethods("com.networknt.exception.DefaultExceptionProcessor");
    }

    @Test
    public void testApiException()  {
        Status status = new Status("ERR11618");
        ApiException apiException = new ApiException(status);
        Status result = defaultExceptionProcessor.exception(apiException);
        Assert.assertEquals("ERR11618", result.getCode());
    }

    @Test
    public void testFrameworkException()  {
        Status status = new Status("ERR13001");
        FrameworkException fException = new FrameworkException(status);
        Status result = defaultExceptionProcessor.exception(fException);
        Assert.assertEquals("ERR13001", result.getCode());
    }

    @Test
    public void testApiExceptionProcess()  {
        Status status = new Status("ERR11618");
        ApiException apiException = new ApiException(status);
        Assert.assertTrue(exceptions.contains(apiException.getClass()));
        Assert.assertFalse(exceptions.contains(new NullPointerException("null value").getClass()));
    }

    private static void processAnnotationMethods(String processorName) throws  Exception{
        Class processor = Class.forName(processorName);
        for (final Method method : processor.getDeclaredMethods()) {
            if (method.isAnnotationPresent(ExceptionIndicator.class)) {
                ExceptionIndicator annotInstance = method.getAnnotation(ExceptionIndicator.class);
                exceptions.addAll(Arrays.stream(annotInstance.value()).collect(Collectors.toList()));
            }
        }
    }
}
