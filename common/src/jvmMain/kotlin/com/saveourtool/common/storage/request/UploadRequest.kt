package com.saveourtool.common.storage.request

import java.net.URL

/**
 * @property key key to upload
 * @property url pre signed url to upload [key]
 * @property headers required headers to upload [key] using [url]
 * @property urlFromContainer pre signed url to upload [key]
 * @property headersFromContainer required headers to upload [key] using [urlFromContainer]
 */
data class UploadRequest<K : Any>(
    val key: K,
    val url: URL,
    val headers: Map<String, List<String>>,
    val urlFromContainer: URL,
    val headersFromContainer: Map<String, List<String>>,
)
