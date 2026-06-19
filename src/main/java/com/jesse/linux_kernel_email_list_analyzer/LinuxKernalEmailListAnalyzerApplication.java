package com.jesse.linux_kernel_email_list_analyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 服务启动类。*/
@EnableRetry
@EnableScheduling
@SpringBootApplication
public class LinuxKernalEmailListAnalyzerApplication
{
	public static void main(String[] args) {
		SpringApplication.run(LinuxKernalEmailListAnalyzerApplication.class, args);
	}
}
