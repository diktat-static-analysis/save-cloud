package com.saveourtool.common.s3

import org.springframework.http.MediaType
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.core.client.config.SdkAdvancedAsyncClientOption
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest

import java.net.URI
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

import kotlin.time.Duration
import kotlin.time.toJavaDuration
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.reactor.asCoroutineDispatcher

/**
 * Default implementation of [S3Operations]
 *
 * @param properties S3 properties
 */
class DefaultS3Operations(
    properties: S3OperationsProperties,
) : S3Operations, AutoCloseable {
    private val bucketName = properties.bucketName
    private val credentialsProvider: AwsCredentialsProvider = properties.credentials.toAwsCredentialsProvider()
    private val executorName: String = "s3-operations-${properties.bucketName}"
    private val executorService = with(properties.async) {
        ThreadPoolExecutor(
            minPoolSize,
            maxPoolSize,
            ttl.toNanos(),
            TimeUnit.NANOSECONDS,
            LinkedBlockingQueue(queueSize),
            NamedDefaultThreadFactory(executorName),
        )
    }
    override val scheduler: Scheduler = Schedulers.fromExecutorService(executorService, executorName)
    override val coroutineDispatcher: CoroutineDispatcher = scheduler.asCoroutineDispatcher()
    private val s3Client: S3AsyncClient = with(properties) {
        S3AsyncClient.builder()
            .credentialsProvider(credentialsProvider)
            .httpClientBuilder(
                NettyNioAsyncHttpClient.builder()
                    .maxConcurrency(httpClient.maxConcurrency)
                    .connectionTimeout(httpClient.connectionTimeout)
                    .connectionAcquisitionTimeout(httpClient.connectionAcquisitionTimeout)
            )
            .asyncConfiguration { builder ->
                builder.advancedOption(SdkAdvancedAsyncClientOption.FUTURE_COMPLETION_EXECUTOR, executorService)
            }
            .region(stubRegion)
            .forcePathStyle(true)
            .endpointOverride(endpoint)
            .build()
            .also { createdClient ->
                if (createBucketIfNotExists) {
                    createdClient.createBucketIfNotExists(bucketName).join()
                }
            }
    }
    private val s3Presigner: S3Presigner = properties.createS3Presigner(S3OperationsProperties::endpoint)
    private val s3PresignerFromContainer: S3Presigner = properties.createS3Presigner(S3OperationsProperties::endpointFromContainer)

    override fun close() {
        s3Client.close()
        s3Presigner.close()
        s3PresignerFromContainer.close()
        executorService.shutdown()
    }

    override fun listObjectsV2(prefix: String, continuationToken: String?): CompletableFuture<ListObjectsV2Response> {
        val request = ListObjectsV2Request.builder()
            .bucket(bucketName)
            .prefix(prefix)
            .let { builder ->
                continuationToken?.let { builder.continuationToken(it) } ?: builder
            }
            .build()
        return s3Client.listObjectsV2(request)
    }

    private fun getObjectRequest(s3Key: String) = GetObjectRequest.builder()
        .bucket(bucketName)
        .key(s3Key)
        .build()

    override fun getObject(s3Key: String): CompletableFuture<GetObjectResponsePublisher?> =
            s3Client.getObject(getObjectRequest(s3Key), AsyncResponseTransformer.toPublisher())
                .handleNoSuchKeyException()

    override fun createMultipartUpload(s3Key: String): CompletableFuture<CreateMultipartUploadResponse> {
        val request = CreateMultipartUploadRequest.builder()
            .bucket(bucketName)
            .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
            .key(s3Key)
            .build()
        return s3Client.createMultipartUpload(request)
    }

    override fun uploadPart(createResponse: CreateMultipartUploadResponse, index: Long, contentPart: AsyncRequestBody): CompletableFuture<CompletedPart> {
        val nextPartRequest = UploadPartRequest.builder()
            .bucket(createResponse.bucket())
            .key(createResponse.key())
            .uploadId(createResponse.uploadId())
            .partNumber(index.toInt())
            .build()
        return s3Client.uploadPart(nextPartRequest, contentPart)
            .thenApply { partResponse ->
                CompletedPart.builder()
                    .eTag(partResponse.eTag())
                    .partNumber(index.toInt())
                    .build()
            }
    }

    override fun completeMultipartUpload(
        createResponse: CreateMultipartUploadResponse,
        completedParts: Collection<CompletedPart>
    ): CompletableFuture<CompleteMultipartUploadResponse> {
        val request = CompleteMultipartUploadRequest.builder()
            .bucket(createResponse.bucket())
            .key(createResponse.key())
            .uploadId(createResponse.uploadId())
            .multipartUpload { builder ->
                builder.parts(completedParts.sortedBy { it.partNumber() })
            }
            .build()
        return s3Client.completeMultipartUpload(request)
    }

    override fun abortMultipartUpload(
        createResponse: CreateMultipartUploadResponse,
    ): CompletableFuture<AbortMultipartUploadResponse> {
        val request = AbortMultipartUploadRequest.builder()
            .bucket(createResponse.bucket())
            .key(createResponse.key())
            .uploadId(createResponse.uploadId())
            .build()
        return s3Client.abortMultipartUpload(request)
    }

    private fun putObjectRequest(s3Key: String, contentLength: Long): PutObjectRequest = PutObjectRequest.builder()
        .bucket(bucketName)
        .contentType(MediaType.APPLICATION_OCTET_STREAM_VALUE)
        .key(s3Key)
        .contentLength(contentLength)
        .build()

    override fun putObject(s3Key: String, contentLength: Long, content: AsyncRequestBody): CompletableFuture<PutObjectResponse> =
            s3Client.putObject(putObjectRequest(s3Key, contentLength), content)

    override fun copyObject(sourceS3Key: String, targetS3Key: String): CompletableFuture<CopyObjectResponse?> {
        val request = CopyObjectRequest.builder()
            .sourceBucket(bucketName)
            .sourceKey(sourceS3Key)
            .destinationBucket(bucketName)
            .destinationKey(targetS3Key)
            .build()
        return s3Client.copyObject(request)
            .handleNoSuchKeyException()
    }

    override fun deleteObject(s3Key: String): CompletableFuture<DeleteObjectResponse?> {
        val request = DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(s3Key)
            .build()
        return s3Client.deleteObject(request)
            .handleNoSuchKeyException()
    }

    override fun headObject(s3Key: String): CompletableFuture<HeadObjectResponse?> {
        val request = HeadObjectRequest.builder()
            .bucket(bucketName)
            .key(s3Key)
            .build()
        return s3Client.headObject(request)
            .handleNoSuchKeyException()
    }

    override fun requestToDownloadObject(
        s3Key: String,
        duration: Duration,
        fromContainer: Boolean,
    ): PresignedGetObjectRequest = getS3Presigner(fromContainer).presignGetObject { builder ->
        builder
            .signatureDuration(duration.toJavaDuration())
            .getObjectRequest(getObjectRequest(s3Key))
            .build()
    }

    override fun requestToUploadObject(
        s3Key: String,
        contentLength: Long,
        duration: Duration,
        fromContainer: Boolean,
    ): PresignedPutObjectRequest = getS3Presigner(fromContainer).presignPutObject { builder ->
        builder
            .signatureDuration(duration.toJavaDuration())
            .putObjectRequest(putObjectRequest(s3Key, contentLength))
            .build()
    }

    private fun getS3Presigner(fromContainer: Boolean) = if (fromContainer) {
        s3PresignerFromContainer
    } else {
        s3Presigner
    }

    private fun S3OperationsProperties.createS3Presigner(
        endpointProvider: (S3OperationsProperties) -> URI,
    ): S3Presigner = S3Presigner.builder()
        .credentialsProvider(credentialsProvider)
        .region(stubRegion)
        .serviceConfiguration(S3Configuration.builder()
            .pathStyleAccessEnabled(true)
            .build())
        .endpointOverride(endpointProvider(this).lowercase())
        .build()

    companion object {
        // we don't use region when connect to S3
        private val stubRegion = Region.AWS_ISO_GLOBAL

        private fun <T : Any> CompletableFuture<T>.handleNoSuchKeyException(): CompletableFuture<T?> = exceptionally { ex ->
            when (ex.cause) {
                is NoSuchKeyException -> null
                else -> throw ex
            }
        }

        private fun URI.lowercase(): URI = URI(toString().lowercase())

        private fun S3AsyncClient.createBucketIfNotExists(bucketName: String): CompletableFuture<String> =
                HeadBucketRequest.builder()
                    .bucket(bucketName)
                    .build()
                    .let { headBucket(it) }
                    .thenApply { true }
                    .exceptionally { ex ->
                        when (ex.cause) {
                            is NoSuchBucketException -> false
                            else -> throw ex
                        }
                    }
                    .thenCompose { bucketExists ->
                        if (bucketExists) {
                            CompletableFuture.completedFuture("Bucket $bucketName already exists")
                        } else {
                            CreateBucketRequest.builder()
                                .bucket(bucketName)
                                .build()
                                .let { createBucket(it) }
                                .thenApply { _ ->
                                    "Created bucket $bucketName"
                                }
                        }
                    }

        private class NamedDefaultThreadFactory(private val namePrefix: String) : ThreadFactory {
            private val delegate: ThreadFactory = Executors.defaultThreadFactory()
            private val threadCount = AtomicInteger(0)

            override fun newThread(runnable: Runnable): Thread {
                val thread = delegate.newThread(runnable).apply {
                    name = namePrefix + "-" + threadCount.getAndIncrement()
                }
                return thread
            }
        }
    }
}
