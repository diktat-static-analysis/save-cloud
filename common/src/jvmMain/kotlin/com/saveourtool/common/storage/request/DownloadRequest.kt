package com.saveourtool.common.storage.request

import java.net.URL

/**
 * @property key key to download
 * @property url pre signed url to download [key]
 * @property urlFromContainer pre signed url to download [key]
 */
data class DownloadRequest<K : Any>(
    val key: K,
    val url: URL,
    val urlFromContainer: URL,
)
