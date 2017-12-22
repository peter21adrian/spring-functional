package org.springfunctional.processors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.function.Supplier;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = PostProcessBeforeInitializationTest.Config.class)
public class PostProcessBeforeInitializationTest {

    @Autowired
    Supplier<String> beanToChange;

    @Autowired
    Supplier<String> annotatedObject;

    @Autowired
    FieldAnnotated annotatedField;

    @Autowired
    MethodAnnotated annotatedMethod;

    @Test
    public void test() {
        Assert.assertEquals("CHANGED", beanToChange.get());
        Assert.assertEquals("CHANGED", annotatedObject.get());
        Assert.assertEquals("CHANGED", annotatedField.field);
        Assert.assertEquals("CHANGED", annotatedMethod.field);
    }

    @Configuration
    public static class Config {

        @Bean
        PostProcessBeforeInitialization postProcess() {
            return (bean, targetClass) -> {
                if(bean instanceof Supplier) {
                    return (Supplier)() -> "CHANGED";
                }
                return bean;
            };
        }

        @Bean
        PostProcessBeforeInitialization postProcessAnnotatedObject() {
            return PostProcessBeforeInitialization.classAnnotation(MyCustomAnnotation.class,
                    (bean, annotation) -> (Supplier) () -> "CHANGED");
        }

        @Bean
        PostProcessBeforeInitialization postProcessAnnotatedField() {
            return PostProcessBeforeInitialization.fieldAnnotation(MyCustomAnnotation.class,
                    (bean, field, annotation) -> {
                        ReflectionUtils.setField(field, bean, "CHANGED");
                    });
        }

        @Bean
        PostProcessBeforeInitialization postProcessAnnotatedMethod() {
            return PostProcessBeforeInitialization.methodAnnotation(MyCustomAnnotation.class,
                    (bean, method, annotation) -> {
                        ReflectionUtils.invokeMethod(method, bean, "CHANGED");
                    });
        }

        @Bean
        Supplier<String> beanToChange() {
            return () -> "NOT_CHANGED";
        }

        @Bean
        Supplier<String> annotatedObject() {
            return new ObjectAnnotated();
        }

        @Bean
        FieldAnnotated annotatedField() {
            return new FieldAnnotated();
        }

        @Bean
        MethodAnnotated annotatedMethod() {
            return new MethodAnnotated();
        }

    }

    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyCustomAnnotation {

    }

    @MyCustomAnnotation
    public static class ObjectAnnotated implements Supplier<String> {

        @Override
        public String get() {
            return "NOT_CHANGED";
        }
    }

    public static class FieldAnnotated {
        @MyCustomAnnotation
        public String field;
    }

    public static class MethodAnnotated {
        public String field;

        @MyCustomAnnotation
        public void setField(String field){
            this.field = field;
        }
    }

}
