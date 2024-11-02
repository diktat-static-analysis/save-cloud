package com.saveourtool.common.s3

import reactor.core.scheduler.Scheduler
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.ResponsePublisher
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest

import java.util.concurrent.CompletableFuture

import kotlin.time.Duration
import kotlinx.coroutines.CoroutineDispatcher

typealias GetObjectResponsePublisher = ResponsePublisher<GetObjectResponse>

/**
 * Base interface to operate with S3 in single bucket (like JdbcOperations)
 */
interface S3Operations {
    /**
     * a shared [Scheduler] based on [java.util.concurrent.ExecutorService]
     */
    val scheduler: Scheduler

    /**
     * a shared [CoroutineDispatcher] based on [java.util.concurrent.ExecutorService]
     */
    val coroutineDispatcher: CoroutineDispatcher

    /**
     * @param prefix
     * @param continuationToken a token from previous request
     * @return [CompletableFuture] with response
     */
    fun listObjectsV2(prefix: String, continuationToken: String? = null): CompletableFuture<ListObjectsV2Response>

    /**
     * @param s3Key
     * @return [CompletableFuture] with response as [ResponsePublisher] or null if object is not found
     */
    fun getObject(s3Key: String): CompletableFuture<GetObjectResponsePublisher?>

    /**
     * @param s3Key
     * @return [CompletableFuture] with response
     */
    fun createMultipartUpload(s3Key: String): CompletableFuture<CreateMultipartUploadResponse>

    /**
     * @param createResponse response on creating multipart upload
     * @param index
     * @param contentPart
     * @return [CompletableFuture] with [CompletedPart]
     */
    fun uploadPart(
        createResponse: CreateMultipartUploadResponse,
        index: Long,
        contentPart: AsyncRequestBody,
    ): CompletableFuture<CompletedPart>

    /**
     * @param createResponse response on creating multipart upload
     * @param completedParts
     * @return [CompletableFuture] with response
     */
    fun completeMultipartUpload(
        createResponse: CreateMultipartUploadResponse,
        completedParts: Collection<CompletedPart>,
    ): CompletableFuture<CompleteMultipartUploadResponse>

    /**
     * @param createResponse response on creating multipart upload
     * @return [CompletableFuture] with response
     */
    fun abortMultipartUpload(
        createResponse: CreateMultipartUploadResponse,
    ): CompletableFuture<AbortMultipartUploadResponse>

    /**
     * @param s3Key
     * @param contentLength
     * @param content
     * @return [CompletableFuture] with response
     */
    fun putObject(s3Key: String, contentLength: Long, content: AsyncRequestBody): CompletableFuture<PutObjectResponse>

    /**
     * @param sourceS3Key
     * @param targetS3Key
     * @return [CompletableFuture] with response
     */
    fun copyObject(sourceS3Key: String, targetS3Key: String): CompletableFuture<CopyObjectResponse?>

    /**
     * @param s3Key
     * @return [CompletableFuture] with response
     */
    fun deleteObject(s3Key: String): CompletableFuture<DeleteObjectResponse?>

    /**
     * @param s3Key
     * @return [CompletableFuture] with response
     */
    fun headObject(s3Key: String): CompletableFuture<HeadObjectResponse?>

    /**
     * @param s3Key
     * @param duration duration when url is valid
     * @param fromContainer true if a pre-signed request will be performed from container
     * @return a pre-signed request to download an object
     */
    fun requestToDownloadObject(s3Key: String, duration: Duration, fromContainer: Boolean = false): PresignedGetObjectRequest

    /**
     * @param s3Key
     * @param contentLength
     * @param duration duration when url is valid
     * @param fromContainer true if a pre-signed request will be performed from container
     * @return a pre-signed request to upload an object with known content length
     */
    fun requestToUploadObject(
        s3Key: String,
        contentLength: Long,
        duration: Duration,
        fromContainer: Boolean = false,
    ): PresignedPutObjectRequest
}
