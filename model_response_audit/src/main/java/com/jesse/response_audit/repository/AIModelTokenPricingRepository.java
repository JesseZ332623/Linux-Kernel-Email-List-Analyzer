package com.jesse.response_audit.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jesse.core.entity.AIModelTokenPricingEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Optional;

/** 大模型 Token 定价表仓储类。*/
public interface AIModelTokenPricingRepository
    extends BaseMapper<AIModelTokenPricingEntity>
{
    /** 按模型名称查询模型定价。*/
    @Select("SELECT * FROM ai_model_token_pricing WHERE model_name = #{modelName}")
    Optional<AIModelTokenPricingEntity>
    getPricingByModelName(@Param("modelName") String modelName);

    /** 提供多个模型名称，查询它们的定价。*/
    @Select("""
        <script>
            SELECT * FROM ai_model_token_pricing
            WHERE model_name IN
            <foreach collection='modelNames' item='modelName' open='(' separator=',' close=')'>
                #{modelName}
            </foreach>
        </script>
    """)
    List<AIModelTokenPricingEntity>
    getPricingByModelNameList(@Param("modelNames") List<String> modelNames);
}