package com.seatmap.common.exception;

public class SeatmapException extends Exception {
    private final String errorCode;
    private final int httpStatus;

    public SeatmapException(String errorCode, String message) {
        this(errorCode, message, 400);
    }

    public SeatmapException(String errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public SeatmapException(String errorCode, String message, Throwable cause) {
        this(errorCode, message, 400, cause);
    }

    public SeatmapException(String errorCode, String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    // Common exception factory methods
    public static SeatmapException badRequest(String message) {
        return new SeatmapException("INVALID_REQUEST", message, 400);
    }

    public static SeatmapException unauthorized(String message) {
        return new SeatmapException("UNAUTHORIZED", message, 401);
    }

    public static SeatmapException forbidden(String message) {
        return new SeatmapException("FORBIDDEN", message, 403);
    }

    public static SeatmapException notFound(String message) {
        return new SeatmapException("NOT_FOUND", message, 404);
    }

    public static SeatmapException conflict(String message) {
        return new SeatmapException("CONFLICT", message, 409);
    }

    public static SeatmapException guestLimitReached() {
        return new SeatmapException("GUEST_LIMIT_REACHED", 
            "You've reached your guest limit of 2 seat map views. Please sign up to continue.", 403);
    }

    public static SeatmapException internalError(String message) {
        return new SeatmapException("INTERNAL_ERROR", message, 500);
    }

    public static SeatmapException externalApiError(String message) {
        return new SeatmapException("EXTERNAL_API_ERROR", message, 502);
    }
}