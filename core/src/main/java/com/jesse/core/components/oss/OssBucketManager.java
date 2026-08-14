package com.jesse.core.components.oss;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * OSS 服务桶管理器组件。
 * 一般情况下桶都是运维同志在维护，服务无权管理，本代码仅做示例。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OssBucketManager
{
    @Qualifier("minio-client")
    private final S3Client s3Client;

    /** 清空一个桶内的所有对象。*/
    private void cleanBucket(String bucketName)
    {
        // (1) 构造 “列出该桶的所有对象信息（包括版本）” 的请求
        ListObjectsV2Request listObjectsRequest
            = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build();

        ListObjectsV2Response listObjectsResponse;

        do {
            // (2) 列出该桶的所有对象信息（包括版本）
            listObjectsResponse
                = this.s3Client.listObjectsV2(listObjectsRequest);

            if (!listObjectsResponse.contents().isEmpty())
            {
                // (3) 列出本页所有对象的 标识符
                final List<ObjectIdentifier> objectsToDelete
                    = listObjectsResponse.contents().stream()
                        .map((object) ->
                            ObjectIdentifier.builder().key(object.key()).build())
                        .toList();

                // (4) 构造删除本页所有对象的请求
                final DeleteObjectsRequest deleteObjectsRequest
                    = DeleteObjectsRequest.builder()
                        .bucket(bucketName)
                        .delete(Delete.builder().objects(objectsToDelete).build())
                        .build();

                // (5) 执行删除
                this.s3Client.deleteObjects(deleteObjectsRequest);

                log.info(
                    "Delete {} objects in bucket {}.",
                    objectsToDelete.size(), bucketName
                );
            }

            // 翻页
            listObjectsRequest
                = ListObjectsV2Request.builder()
                    .bucket(bucketName)
                    .continuationToken(listObjectsResponse.nextContinuationToken())
                    .build();
        }
        // 直到这个桶被彻底删干净后再退出循环
        while (listObjectsResponse.isTruncated());
    }

    /** 创建一个新桶。*/
    public void createBucker(String bucketName)
    {
        try
        {
            final CreateBucketRequest request
                = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

            this.s3Client.createBucket(request);

            log.info("Create bucket: {} success.", bucketName);
        }
        catch (S3Exception s3Exception) {
            log.error("Create bucket failed.", s3Exception);
        }
    }

    /** 检查桶是否存在。*/
    public boolean bucketExists(String bucketName)
    {
        final HeadBucketRequest request
            = HeadBucketRequest.builder()
                .bucket(bucketName)
                .build();
        try
        {
            this.s3Client.headBucket(request);
            return true;
        }
        catch (S3Exception e) {
            return false;
        }
    }

    /** 列出所有桶。*/
    public List<String> listBuckets()
    {
        final ListBucketsResponse response
            = this.s3Client.listBuckets();

        return
        response.buckets().stream()
            .map(Bucket::name)
            .collect(Collectors.toList());
    }

    /** 删除一个桶。*/
    public void deleteBucket(String bucketName)
    {
        try
        {
            if (!this.bucketExists(bucketName))
            {
                log.warn("Bucket {} not exists.", bucketName);
                return;
            }

            this.cleanBucket(bucketName);

            final DeleteBucketRequest request
                = DeleteBucketRequest.builder()
                    .bucket(bucketName)
                    .build();

            this.s3Client.deleteBucket(request);

            log.info("Delete bucket {} complete.", bucketName);
        }
        catch (NoSuchBucketException exception)
        {
            log.error(
                "Delete bucket {} faield. Caused by [{}] {}",
                bucketName,
                exception.awsErrorDetails().errorCode(),
                exception.awsErrorDetails().errorMessage()
            );
        }
    }
}