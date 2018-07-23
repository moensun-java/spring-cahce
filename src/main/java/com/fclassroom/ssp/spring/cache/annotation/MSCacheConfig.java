package com.fclassroom.ssp.spring.cache.annotation;

import java.lang.annotation.*;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/18
 * Time: 下午3:08
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MSCacheConfig {

    String[] cacheNames() default {};


    String keyGenerator() default "";

    String hashKeyGenerator() default "";

    String cacheManager() default "";

    String cacheResolver() default "";

}
