package com.jesse.linux_kernel_email_list_analyzer.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 方法执行耗时计时器注解实现。
 *
 * <ul>
 *    <li>@Target(ElementType.METHOD) 表示本注解只能应用在方法上。</li>
 *    <li>
 *        {@literal @Retention(RetentionPolicy.RUNTIME)}
 *        表示注解在运行时仍然保留，且可以通过反射读取。
 *        <p>RetentionPolicy 枚举还有下面几个成员：</p>
 *        <ol>
 *            <li>SOURCE 源码级别保留，编译时丢弃。</li>
 *            <li>
 *                编译时保留在 .class 文件中，
 *                但 JVM 加载时丢弃（默认值，Lombok 插件的注解用的就是这个）。
 *            </li>
 *        </ol>
 *    </li>
 * </ui>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TimeMonitor
{
    /** 计时器名称，会输出到日志中（默认是 类名 + 方法名）*/
    String value() default "";

    /** 是否记录方法参数（默认不记录，避免敏感信息泄露）。*/
    boolean logArgs() default false;

    /** 是否记录返回结果（默认不记录，避免大对象序列化）。*/
    boolean logResult() default false;

    /** 超时阈值，超过此值会输出 WARN 级别日志。*/
    long warnThreshold() default 3000;

    /** 超时阈值时间单位（默认为毫秒）。*/
    TimeUnit timeunit() default TimeUnit.MILLISECONDS;
}