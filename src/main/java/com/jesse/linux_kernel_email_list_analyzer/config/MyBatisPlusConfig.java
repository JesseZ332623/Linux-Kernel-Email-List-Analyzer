package com.jesse.linux_kernel_email_list_analyzer.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/** Mybatis-Plus 配置类。*/
@Configuration
@MapperScan("com.jesse.linux_kernel_email_list_analyzer.repository")
public class MyBatisPlusConfig {}
