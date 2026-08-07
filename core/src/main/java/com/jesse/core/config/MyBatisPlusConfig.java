package com.jesse.core.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Mybatis-Plus 配置类。*/
@Configuration
@MapperScan("com.jesse.**.repository")
public class MyBatisPlusConfig
{
    /** Mybatis-Plus 拦截器配置。*/
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor()
    {
        final MybatisPlusInterceptor interceptor
            = new MybatisPlusInterceptor();

        // 添加数据库乐观锁拦截器
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        // 添加分页插件，并指定数据库类型为 MySQL
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));

        return interceptor;
    }
}