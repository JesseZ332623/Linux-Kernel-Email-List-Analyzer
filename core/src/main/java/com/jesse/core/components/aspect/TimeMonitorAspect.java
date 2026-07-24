package com.jesse.core.components.aspect;

import com.jesse.core.annotation.TimeMonitor;
import com.jesse.core.pojo.PlainTextEmail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** {@link TimeMonitor} 计时器切面实现类。*/
@Slf4j
@Aspect
@Component
public class TimeMonitorAspect
{
    /** 按反射出来的方法实例拼接默认的计时器名。*/
    private static String
    concatDefaultMonitorName(Method method) {
        return method.getDeclaringClass().getSimpleName() + "." + method.getName() + "()";
    }

    /** 记录方法入参。*/
    private void logArgs(
        final String monitorName,
        final ProceedingJoinPoint joinPoint,
        final Method method
    )
    {
        final Object[] args             = joinPoint.getArgs();     // 反射参数值
        final Parameter[] parameters    = method.getParameters();  // 反射参数名
        final StringBuilder argsBuilder = new StringBuilder();

        for (int index = 0; index < parameters.length; ++index)
        {
            final String name  = parameters[index].getName();
            final Object value = args[index];

            switch (value)
            {
                case null ->
                    argsBuilder.append(name).append("=null");

                case PlainTextEmail email ->
                    argsBuilder.append(name)
                        .append("={messageId=")
                        .append(email.getMessageId())
                        .append(", subject=")
                        .append(email.getSubject())
                        .append("}");

                // 如果参数值类型为任意 List 时
                case List<?> list ->
                    argsBuilder.append(name)
                        .append("=List(size=")
                        .append(list.size())
                        .append(")");

                default -> {
                    // 如果是 Raw-Array
                    if (value.getClass().isArray())
                    {
                        argsBuilder.append(name)
                            .append("=Array(size=")
                            .append(Array.getLength(value))
                            .append(")");
                    }
                    else
                    {
                        // 最后按 toString() 兜底
                        final String str
                            = (value.toString().length() > 150)
                                ? value.toString().substring(0, 150) + "..."
                                : value.toString();

                        argsBuilder.append(name).append("=").append(str);
                    }
                }
            }

            if (index != parameters.length - 1) {
                argsBuilder.append(", ");
            }
        }

        log.info(
            "Method [{}] called with arguments: [{}]",
            monitorName, argsBuilder
        );
    }

    /** 统计一个方法的执行时间。*/
    @Around("@annotation(timeMonitor)")
    public Object
    monitor(ProceedingJoinPoint joinPoint, TimeMonitor timeMonitor) throws Throwable
    {
        // 获取被注解的方法签名
        final MethodSignature signature
            = (MethodSignature) joinPoint.getSignature();

        // 反射出方法实例
        final Method method = signature.getMethod();

        // 获取计时器名
        final String monitorName
            = (timeMonitor.value().isEmpty())
                ? concatDefaultMonitorName(method)
                : timeMonitor.value();

        // (可选) 记录方法入参
        if (timeMonitor.logArgs()) {
            this.logArgs(monitorName, joinPoint, method);
        }

        final StopWatch stopWatch = new StopWatch(monitorName);
        stopWatch.start();

        try
        {
            // 执行目标方法
            final Object result = joinPoint.proceed();

            // (可选) 记录方法的返回结果
            if (timeMonitor.logResult() && Objects.nonNull(result)) {
                log.info("Method [{}] returned: [{}]", monitorName, result);
            }

            return result;
        }
        finally
        {
            // 停止计时
            stopWatch.stop();

            // 按照注解给出的时间单位转化计时器的统计时间
            final long duration
                = stopWatch.getTime(timeMonitor.timeunit());

            // 超时阈值
            final long warnThreshold = timeMonitor.warnThreshold();

            final String unitString
                = timeMonitor.timeunit().name().toLowerCase(Locale.ROOT);

            if (duration > warnThreshold)
            {
                log.warn(
                    "Task {} took [{}] {}, exceeds threshold of [{}] {}.",
                    monitorName, duration, unitString, warnThreshold, unitString
                );
            }
            else
            {
                log.info(
                    "Task {} took [{}] {}.",
                    monitorName, duration, unitString
                );
            }
        }
    }
}