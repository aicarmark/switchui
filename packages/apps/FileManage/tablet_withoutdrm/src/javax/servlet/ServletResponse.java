package javax.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;

public interface ServletResponse {
	void flushBuffer() throws IOException;
	int getBufferSize();
	String getCharacterEncoding();
	String getContentType();
	Locale getLocale();
	ServletOutputStream getOutputStream() throws IOException;
	PrintWriter getWriter() throws IOException;
	boolean isCommitted();
	void reset();
	void resetBuffer();
	void setBufferSize( int siz );
	void setCharacterEncoding( String encoding );
	void setContentLength( int len );
	void setContentType(String type);
	void setLocale(Locale loc);
}
