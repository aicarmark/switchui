package com.motorola.mmsp.render.gl;

public class EglBadSurfaceException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2810415260065754569L;
	
	/**
     * Constructs a new {@code EglBadSurfaceException} that includes the current stack
     * trace.
     */
    public EglBadSurfaceException() {
    }

    /**
     * Constructs a new {@code RuntimeException} with the current stack trace
     * and the specified detail message.
     *
     * @param detailMessage
     *            the detail message for this exception.
     */
    public EglBadSurfaceException(String detailMessage) {
        super(detailMessage);
    }

   /**
     * Constructs a new {@code EglBadSurfaceException} with the current stack trace,
     * the specified detail message and the specified cause.
     *
     * @param detailMessage
     *            the detail message for this exception.
     * @param throwable
     *            the cause of this exception.
     */
    public EglBadSurfaceException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    /**
     * Constructs a new {@code EglBadSurfaceException} with the current stack trace
     * and the specified cause.
     *
     * @param throwable
     *            the cause of this exception.
     */
    public EglBadSurfaceException(Throwable throwable) {
        super(throwable);
    }

}
