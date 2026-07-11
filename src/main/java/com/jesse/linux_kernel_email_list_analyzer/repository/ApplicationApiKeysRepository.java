package com.jesse.linux_kernel_email_list_analyzer.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jesse.linux_kernel_email_list_analyzer.entity.ApplicationApiKeys;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 第三方应用访问 API Keys 表仓库类。*/
@Mapper
public interface ApplicationApiKeysRepository
    extends BaseMapper<ApplicationApiKeys>
{
    /** 通过第三方 APP 名查询对应的 API Key。*/
    @Select("""
        SELECT
            api_key
        FROM
            application_api_keys
        WHERE
            application_name = #{appName}
    """)
    String findByAppName(@Param("appName") String appName);
}