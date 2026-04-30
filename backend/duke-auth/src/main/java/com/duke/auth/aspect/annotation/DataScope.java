package com.duke.auth.aspect.annotation;

import java.lang.annotation.*;

/**
 * 数据权限注解，标注在 Service 方法上
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DataScope {
    /** 部门表别名 */
    String deptAlias() default "d";
    /** 用户表别名 */
    String userAlias() default "u";
}
