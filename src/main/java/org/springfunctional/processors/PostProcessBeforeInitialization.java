package org.springfunctional.processors;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.BiFunction;

/**
 * Extends the BeanPostProcessor and converts it to a functional interface
 */
@FunctionalInterface
public interface PostProcessBeforeInitialization extends BeanPostProcessor {

    /**
     * Implement in order to change beans
     * @param bean the bean object
     * @param targetClass the original class - unwrapped from proxy
     * @return the changed object, the same object or null if spring should not create this bean
     */
    Object doForBean(Object bean, Class<?> targetClass);

    /**
     * Implementing before initialization
     * @param bean the bean instance
     * @param beanName the bean name
     * @return the bean instance to pass to the context
     */
    @Nullable
    default Object postProcessBeforeInitialization(Object bean, String beanName){
        Class<?> targetClass = AopUtils.getTargetClass(bean);
        return doForBean(bean, targetClass);
    }

    /**
     * Option to chain multiple processors
     * @param next the next processor to be executed
     * @return another processor containing both
     */
    default PostProcessBeforeInitialization andThen(PostProcessBeforeInitialization next){
        return (bean, targetClass) -> {
            bean = this.doForBean(bean, targetClass);
            return bean != null ?
                next.doForBean(bean, targetClass) : null;
        };
    }

    /**
     * Static helper method to process class annotations
     * @param annotationType the type for the annotation
     * @param biFunction the function used to process the annotation. Will receive the bean instance and the annotation instance.
     *                   The return of this function will be used to pass to the spring context
     * @param <T> the annotation type
     * @return a fully functional BeanPostProcessor
     */
    static <T extends Annotation> PostProcessBeforeInitialization classAnnotation(Class<T> annotationType, BiFunction<Object, T, Object> biFunction){
        return (bean, targetClass) -> {
            T annotation = AnnotationUtils.findAnnotation(targetClass, annotationType);
            if(annotation != null) {
                return biFunction.apply(bean, annotation);
            }
            return bean;
        };
    }

    /**
     * Static helper method to process field annotations
     * @param annotationType the type for the annotation
     * @param triConsumer A consumer taking the bean instance, the field, and the annotation instance
     * @param <T> the annotation type
     * @return a fully functional BeanPostProcessor
     */
    static <T extends Annotation> PostProcessBeforeInitialization fieldAnnotation(Class<T> annotationType, TriConsumer<Object, Field, T> triConsumer){
        return (bean, targetClass) -> {
            ReflectionUtils.doWithFields(targetClass, field -> {
                T annotation = AnnotationUtils.findAnnotation(field, annotationType);
                if (annotation != null){
                    triConsumer.accept(bean, field, annotation);
                }
            });
            return bean;
        };
    }

    /**
     * Static helper method to process method annotations
     * @param annotationType the type for the annotation
     * @param triConsumer a consumer taking the bean instance, the method, and the annotation instance
     * @param <T> the annotation type
     * @return
     */
    static <T extends Annotation> PostProcessBeforeInitialization methodAnnotation(Class<T> annotationType, TriConsumer<Object, Method, T> triConsumer){
        return (bean, targetClass) -> {
            ReflectionUtils.doWithMethods(targetClass, method -> {
                T annotation = AnnotationUtils.findAnnotation(method, annotationType);
                if (annotation != null){
                    triConsumer.accept(bean, method, annotation);
                }
            });
            return bean;
        };
    }

    /**
     * Static helper method to process method annotations, and allows bean proxying
     * @param annotationType the type for the annotation
     * @param triFunction  a function taking the bean instance, the method, and the annotation instance.
     *                     The return of this function will be used to pass to the spring context
     * @param <T> the annotation type
     * @return
     */
    static <T extends Annotation> PostProcessBeforeInitialization methodProxyAnnotation(Class<T> annotationType, TriFunction<Object, Method, T, Object> triFunction){
        return (bean, targetClass) -> {
            Method[] methods = ReflectionUtils.getAllDeclaredMethods(targetClass);
            for(Method method : methods){
                T annotation = AnnotationUtils.findAnnotation(method, annotationType);
                if(annotation != null) {
                    bean = triFunction.apply(bean, method, annotation);
                }
            }
            return bean;
        };
    }

    /**
     * Utility consumer, accepting 3 variables
     * @param <T>
     * @param <U>
     * @param <V>
     */
    @FunctionalInterface
    interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }

    /**
     * Utility function, accepting 3 variables and returning a value
     * @param <T>
     * @param <U>
     * @param <V>
     * @param <K>
     */
    @FunctionalInterface
    interface TriFunction<T, U, V, K> {
        K apply(T t, U u, V v);
    }
}
