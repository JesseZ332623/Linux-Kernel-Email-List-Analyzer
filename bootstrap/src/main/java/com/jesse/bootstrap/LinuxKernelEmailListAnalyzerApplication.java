package com.jesse.bootstrap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 服务启动类。*/
@EnableRetry
@EnableScheduling
@ComponentScan(basePackages = { "com.jesse" })
@SpringBootApplication
public class LinuxKernalEmailListAnalyzerApplication
{
	public static void main(String[] args) {
		SpringApplication.run(LinuxKernalEmailListAnalyzerApplication.class, args);
	}
}