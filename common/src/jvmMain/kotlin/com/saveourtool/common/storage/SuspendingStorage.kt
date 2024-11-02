package com.saveourtool.common.storage

/**
 * Base interface for Storage which implements [StorageCoroutines] and [StoragePreSignedUrl]
 *
 * @param K type of key
 */
interface SuspendingStorage<K : Any> : StorageCoroutines<K>, StoragePreSignedUrl<K>
