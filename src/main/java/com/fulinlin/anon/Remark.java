package com.fulinlin.anon;

import java.lang.annotation.*;

@Target(ElementType.TYPE_USE)
@Retention(RetentionPolicy.SOURCE)
@Documented
@Inherited
public @interface Remark {
    String value() default "";
}
