package javax.servlet;

public class ServletException extends Exception {
	ServletException() {
		super();
	}
	
	public ServletException( String err ) {
		super(err);
	}
	
	public ServletException( String err, Throwable t ) {
		super(err);
		mRootCause = t;
	}
	
	public ServletException( Throwable t ) {
		super();
		mRootCause = t;
	}
	
	public Throwable getRootCause() {
		return mRootCause;
	}
	
	private Throwable mRootCause;
}
