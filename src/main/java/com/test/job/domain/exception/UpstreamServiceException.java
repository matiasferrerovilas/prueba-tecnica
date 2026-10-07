package com.test.job.domain.exception;

public class UpstreamServiceException extends RuntimeException {

	public UpstreamServiceException(String message, Throwable cause) {
		super(message, cause);
	}
}
