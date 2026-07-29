package com.tabibma.shared.storage;

import java.io.InputStream;

/**
 * Vendor-agnostic object storage port (docs/architecture-tabib-ma.md — shared between the
 * clinic module's credential documents and the prescription module's signed PDFs). The local
 * filesystem adapter is a dev/test stand-in; a real S3 adapter implements this same interface
 * without callers changing, same pattern as the mock TurnCredentialProvider decision.
 */
public interface ObjectStorageClient {

    /**
     * Stores the content and returns an opaque storage key that {@link #load} can retrieve later.
     * Implementations must not trust {@code filename} for path construction (path traversal).
     */
    String store(String keyPrefix, String filename, InputStream content, long contentLength, String contentType);

    InputStream load(String storageKey);
}
