package com.jesse.core.components.oss;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.InputStream;

/** OSS 服务对象管理器组件。*/
@Slf4j
@Component
@RequiredArgsConstructor
public class OssStorageManager
{
    @Qualifier("minio-client")
    private final S3Client s3Client;

    /**
     * 往指定的桶上传文本对象（适合小的文本文件）。
     *
     * @param bucketName    存储桶名称
     * @param key           对象唯一标识符（在这里可以是一个唯一的路径）
     * @param contentType   对象数据类型
     * @param content       文本内容
     *
     * @throws S3Exception 上传过程中出现任何错误，都会封装成本异常抛出
     */
    public void uploadObjectByString(
        final String bucketName,
        final String key,
        final String contentType,
        final String content
    )
    {
        final PutObjectRequest request
            = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .build();

        this.s3Client.putObject(
            request,
            RequestBody.fromString(content)
        );

        log.info(
            "Upload string object {} to {} success.",
            key, bucketName
        );
    }

    /**
     * 往指定的桶流式上传对象（适合大文件、来自远程的文件）。
     *
     * @param bucketName    存储桶名称
     * @param key           对象唯一标识符（在这里可以是一个唯一的路径）
     * @param contentType   对象数据类型
     * @param inputStream   文件输入流
     * @param contentLength 输入流长度
     *
     * @throws S3Exception 上传过程中出现任何错误，都会封装成本异常抛出
     */
    public void uploadObjectByInputStream(
        final String      bucketName,
        final String      key,
        final String      contentType,
        final InputStream inputStream,
        final long        contentLength
    ) throws S3Exception
    {
        // (1) 构造上传文件请求体
        final PutObjectRequest uploadFileRequest
            = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(contentType)
                .contentLength(contentLength)
                .build();

        // (2) 执行上传操作
        this.s3Client.putObject(
            uploadFileRequest,
            RequestBody.fromInputStream(inputStream, contentLength)
        );

        log.info("Upload object {} to {} success.", key, bucketName);
    }
}