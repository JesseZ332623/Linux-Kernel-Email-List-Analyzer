package com.jesse.core.config;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/** Spring RestTemplate 配置类。*/
@Configuration
public class RestTemplateConfig
{
    @Bean
    public RestTemplate restTemplate()
    {
        // 1. 配置连接管理器（包括连接超时）
        final PoolingHttpClientConnectionManager connectionManager
            = PoolingHttpClientConnectionManagerBuilder.create()
                .setMaxConnTotal(50)    // 连接池最大连接数
                .setMaxConnPerRoute(50) // 每个路由的最大连接数
                .setDefaultConnectionConfig(
                    ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofSeconds(10))  // 建立连接超时（新 API）
                        .setSocketTimeout(Timeout.ofSeconds(30))   // 数据传输超时
                        .build()
                )
                .build();

        // 2. 配置请求超时
        final RequestConfig requestConfig
            = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(10))  // 从连接池获取连接超时
                .setResponseTimeout(Timeout.ofSeconds(300))          // 响应超时
                .build();

        // 3. 构建 HttpClient
        final CloseableHttpClient httpClient
            = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .build();

        return new
        RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
    }
}