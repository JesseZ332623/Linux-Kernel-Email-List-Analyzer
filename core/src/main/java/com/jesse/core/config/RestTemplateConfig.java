package com.jesse.core.config;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
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
                /*
                 * 2026.11.05 修改：
                 * maxConnTotal 和 MaxConnPerRoute 如果值一样的话，这个配置的意义不大。
                 * 因此连接池的的连接数可以调大，
                 * 每个路由的最大连接数和 RabbitMQ 的 max-concurrency 参数一致。
                 */
                .setMaxConnTotal(100)    // 连接池最大连接数
                .setMaxConnPerRoute(75) // 每个路由的最大连接数
                .setDefaultConnectionConfig(
                    ConnectionConfig.custom()
                        .setConnectTimeout(Timeout.ofSeconds(10))  // 建立连接超时（新 API）
                        /*
                         * 2026.11.05 修改：
                         * 所谓 socket timeout 指的是同一个 socket 连接上，InputStream.read() 调用的阻塞时间期限。
                         * 最初的 30s 太过激进，至少要提高到 120s+ 来覆盖 LLM 首字节等待。
                         *
                         * 另外，还需要新增 ValidateAfterInactivity 参数，用于连接复用前校验。
                         * 其目的是，在 “从连接池取出空闲了指定时间的连接复用” 的那一刻，先探一次死活，
                         * 避免把一条已被网关/NAT 静默回收的死连接交给业务代码，从而把 Connection reset 这类故障挡在业务层之外。
                         *
                         * 最后，需要设置 TimeToLive 来确定连接总寿命上限，防止超长连接。
                         */
                        .setSocketTimeout(Timeout.ofSeconds(120))
                        .setValidateAfterInactivity(TimeValue.ofSeconds(10))
                        .setTimeToLive(TimeValue.ofMinutes(30))
                        .build()
                )
                .build();

        // 2. 配置请求超时
        final RequestConfig requestConfig
            = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(10))  // 从连接池获取连接超时
                .setResponseTimeout(Timeout.ofSeconds(300))          // 响应超时
                /*
                 * 2026.11.05 修改：
                 * DeepSeek 这类 API 本就不该发生 3xx 重定向。
                 * 如果网络里有多一层网关/认证代理偷偷 302，HttpClient 会默默跟过去，
                 * 最后在 RedirectExec 里炸出一堆看不懂的异常信息。显式禁掉，让 3xx 直接以异常形式暴露。
                 */
                .setRedirectsEnabled(false)
                .build();

        // 3. 构建 HttpClient
        final CloseableHttpClient httpClient
            = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                /*
                 * 2026.11.05 修改：
                 * 连接池需要一个独立的守护线程，主动的周期性的驱逐空闲指定时间或者过期的连接。
                 * 否则死/空闲连接就会一直赖在池中，直到复用前校验或者吃满时被淘汰。
                 */
                .evictIdleConnections(TimeValue.ofMinutes(5))
                .build();

        return new
        RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
    }
}