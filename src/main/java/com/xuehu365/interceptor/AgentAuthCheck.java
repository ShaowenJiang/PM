package com.xuehu365.interceptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AgentAuthCheck {  
  
    /** 
     * 是否需要用户鉴权 
     */  
    boolean authRequired() default false;  
}  