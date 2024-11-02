package com.saveourtool.common.storage

/**
 * Base interface for Storage which implements [StorageProjectReactor] and [StoragePreSignedUrl]
 *
 * @param K type of key
 */
interface ReactiveStorage<K : Any> : StorageProjectReactor<K>, StoragePreSignedUrl<K>
