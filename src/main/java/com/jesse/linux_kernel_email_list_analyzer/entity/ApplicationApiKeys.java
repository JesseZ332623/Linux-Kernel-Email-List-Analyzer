package com.jesse.linux_kernel_email_list_analyzer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 第三方应用访问 API Keys 表实体类。*/
@Data
@NoArgsConstructor
@TableName("application_api_keys")
public class ApplicationApiKeys
{
    @TableId(type = IdType.INPUT)
    private Long id;

    private String applicationName;

    private String apiKey;
}