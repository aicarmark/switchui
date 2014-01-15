package javax.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Map;

public interface ServletRequest {
	Object getAttribute(String name);
	Enumeration getAttributeNames();
	String getCharacterEncoding();
	int getContentLength();
	String getContentType();
	ServletInputStream getInputStream() throws IOException;
	String getLocalAddr();
	Locale getLocale();
	Enumeration getLocales();
	String getLocalName();
	int getLocalPort();
	String getParameter(String key);
	Map getParameterMap();
	Enumeration getParameterNames();
	String[] getParameterValues(String key);
	String getProtocol();
	BufferedReader getReader() throws IOException;
	String getRealPath(String arg );
	String getRemoteAddr();
	String getRemoteHost();
	int getRemotePort();
	RequestDispatcher getRequestDispatcher(String arg);
	String getScheme();
	String getServerName();
	int getServerPort();
	boolean isSecure();
	void removeAttribute(String attr );
	void setAttribute(String attr, Object obj);
	void setCharacterEncoding(String encoding) throws UnsupportedEncodingException;
}
