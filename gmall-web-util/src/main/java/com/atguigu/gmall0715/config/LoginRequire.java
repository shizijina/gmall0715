package com.atguigu.gmall0715.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)//放在方法上面
@Retention(RetentionPolicy.RUNTIME)//在jvm里也可以存活
public @interface LoginRequire {
    // 是否需要登录标识
    boolean autoRedirect() default true;
}
