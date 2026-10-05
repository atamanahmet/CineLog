package com.atamanahmet.cinelog.client.tmdb;

/**
 * TMDB call failed. upstreamStatus is the HTTP status, or -1 when there was no HTTP response.
 */
public class TmdbClientException extends RuntimeException {

    private final String mediaType;
    private final String requestContext;
    private final int upstreamStatus;

    public TmdbClientException(String mediaType, String requestContext, String message, Throwable cause) {
        this(mediaType, requestContext, message, cause, -1);
    }

    public TmdbClientException(String mediaType, String requestContext, String message, Throwable cause,
            int upstreamStatus) {
        super(message, cause);
        this.mediaType = mediaType;
        this.requestContext = requestContext;
        this.upstreamStatus = upstreamStatus;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getRequestContext() {
        return requestContext;
    }

    public int getUpstreamStatus() {
        return upstreamStatus;
    }
}
