package mg.itu.prom16.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MyNumeric {
    double min() default Double.MIN_VALUE;
    double max() default Double.MAX_VALUE;
    String message() default "Invalid numeric value";
}

