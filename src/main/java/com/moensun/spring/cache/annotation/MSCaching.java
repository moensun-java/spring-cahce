package com.moensun.spring.cache.annotation;

import java.lang.annotation.*;

/**
 * Created by Bane.Shi.
 * Copyright FClassroom
 * User: Bane.Shi
 * Date: 2018/7/20
 * Time: 下午2:02
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface MSCaching {

    MSCacheable[] cacheable() default {};

    MSCachePut[] put() default {};

    MSCacheEvict[] evict() default {};

}
