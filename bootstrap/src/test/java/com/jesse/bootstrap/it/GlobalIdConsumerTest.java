package com.jesse.bootstrap.it;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jesse.core.components.global_id.impl.GlobalIdConsumerImpl;
import com.jesse.core.properties.IdConsumerProperties;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** {@link GlobalIdConsumerImpl} 外部端点调用集成测试（容器化 HTTP 桩）。*/
@Testcontainers(disabledWithoutDocker = true)
class GlobalIdConsumerTest
{
    @Container
    static final MockServerContainer MOCK_SERVER =
        new MockServerContainer(DockerImageName.parse("mockserver/mockserver:5.15.0"));

    private static String baseUrl;

    @BeforeAll
    static void stubEndpoints()
    {
        baseUrl = "http://" + MOCK_SERVER.getHost() + ":" + MOCK_SERVER.getServerPort();

        final MockServerClient client
            = new MockServerClient(MOCK_SERVER.getHost(), MOCK_SERVER.getServerPort());

        client.when(HttpRequest.request().withPath("/api/id_allocator/next"))
            .respond(HttpResponse.response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"data\": 12345}"));

        client.when(HttpRequest.request().withPath("/api/id_allocator/next-batch"))
            .respond(HttpResponse.response()
                .withStatusCode(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"data\": [1, 2, 3]}"));
    }

    private GlobalIdConsumerImpl newConsumer()
    {
        final IdConsumerProperties.DirectoryUrls urls = new IdConsumerProperties.DirectoryUrls();
        urls.setNext(baseUrl + "/api/id_allocator/next");
        urls.setNextBatch(baseUrl + "/api/id_allocator/next-batch");

        final IdConsumerProperties properties = new IdConsumerProperties();
        properties.setDirectoryUrls(urls);

        return new GlobalIdConsumerImpl(new RestTemplate(), properties, new ObjectMapper());
    }

    @Test
    @DisplayName("nextId() 解析响应中的 data 字段")
    void nextIdParsesDataField()
    {
        assertThat(this.newConsumer().nextId()).isEqualTo(12345L);
    }

    @Test
    @DisplayName("nextIds() 解析响应中的 data 数组")
    void nextIdsParsesDataArray()
    {
        assertThat(this.newConsumer().nextIds(3)).containsExactly(1L, 2L, 3L);
    }

    @Test
    @DisplayName("nextIds() 批大小非法时抛 IllegalArgumentException")
    void nextIdsRejectsNonPositiveSize()
    {
        assertThatThrownBy(() -> this.newConsumer().nextIds(0))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
