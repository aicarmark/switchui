package com.motorola.mmsp.render.gl;

public class EglMakeCurrentException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2810415260065754569L;
	
	/**
     * Constructs a new {@code EglMakeCurrentException} that includes the current stack
     * trace.
     */
    public EglMakeCurrentException() {
    }

    /**
     * Constructs a new {@code EglMakeCurrentException} with the current stack trace
     * and the specified detail message.
     *
     * @param detailMessage
     *            the detail message for this exception.
     */
    public EglMakeCurrentException(String detailMessage) {
        super(detailMessage);
    }

   /**
     * Constructs a new {@code EglMakeCurrentException} with the current stack trace,
     * the specified detail message and the specified cause.
     *
     * @param detailMessage
     *            the detail message for this exception.
     * @param throwable
     *            the cause of this exception.
     */
    public EglMakeCurrentException(String detailMessage, Throwable throwable) {
        super(detailMessage, throwable);
    }

    /**
     * Constructs a new {@code EglMakeCurrentException} with the current stack trace
     * and the specified cause.
     *
     * @param throwable
     *            the cause of this exception.
     */
    public EglMakeCurrentException(Throwable throwable) {
        super(throwable);
    }

}
