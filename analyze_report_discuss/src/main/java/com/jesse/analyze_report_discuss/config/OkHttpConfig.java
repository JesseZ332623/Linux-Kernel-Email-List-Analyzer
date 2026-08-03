package com.jesse.analyze_report_discuss.config;

import lombok.RequiredArgsConstructor;
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/** OK HTTP 客户端配置类。*/
@Configuration
@RequiredArgsConstructor
public class OkHttpConfig
{
    /** OK HTTP 客户端专用的响应回调虚拟线程执行器。*/
    @Bean(
        name = "ok-http-client-dispatcher-executor",
        destroyMethod = "shutdown"
    )
    public ExecutorService okHttpClientDispatcherExecutor()
    {
        final ThreadFactory threadFactory
            = Thread.ofVirtual()
                .name("ok-http-client-dispatcher-", 0)
                .factory();

        return
        Executors.newThreadPerTaskExecutor(threadFactory);
    }

    /** 配置 HTTP 客户端，专用与处理 SSE 协议的响应数据。*/
    @Bean
    public OkHttpClient okHttpClient()
    {
        return new
        OkHttpClient.Builder()
            /*
             * 客户端自己的 dispatcher 是基于平台线程池调度的，
             * 这里切换成虚拟线程池。
             */
            .dispatcher(new Dispatcher(this.okHttpClientDispatcherExecutor()))
            /*
             * HTTP/2 有多路复用策略，
             * 同一个 TCP 连接上可以同时存在多个 Stream 互不干扰，
             * 避免默认的 HTTP 1.1 并行的 SSE 流或者频繁的创建 TCP 连接。
             */
            .protocols(List.of(Protocol.HTTP_2, Protocol.HTTP_1_1))
            .connectTimeout(10L, TimeUnit.SECONDS)
            /*
             * 如果要读取基于 SSE 协议的响应数据，
             * 则必须关闭读取超时限制。
             */
            .readTimeout(0L, TimeUnit.MILLISECONDS)
            /*
             * 发送请求体所耗的时间，随提示词上下文的膨胀而上涨，
             * 这里设为 1 分钟（默认是 10 秒）。
             */
            .writeTimeout(1L, TimeUnit.MINUTES)
            .build();
    }
}