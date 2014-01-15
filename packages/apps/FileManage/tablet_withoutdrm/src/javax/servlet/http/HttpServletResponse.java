package javax.servlet.http;

import java.io.IOException;

import javax.servlet.ServletResponse;

public interface HttpServletResponse extends ServletResponse {
	static int SC_ACCEPTED = 202;
	static int SC_BAD_GATEWAY = 502;
	static int SC_BAD_REQUEST = 400;
	static int SC_CONFLICT = 409;
	static int SC_CONTINUE = 100;
	static int SC_CREATED = 201;
	static int SC_EXPECTATION_FAILED = 417;
	static int SC_FORBIDDEN = 403;
	static int SC_GATEWAY_TIMEOUT = 504;
	static int SC_GONE = 410;
	static int SC_HTTP_VERSION_NOT_SUPPORTED = 505;
	static int SC_INTERNAL_SERVER_ERROR = 500;
	static int SC_LENGTH_REQUIRED = 411;
	static int SC_METHOD_NOT_ALLOWED = 405;
	static int SC_MOVED_PERMANENTLY = 301;
	static int SC_MOVED_TEMPORARILY = 302;
	static int SC_MULTIPLE_CHOICES = 300;
	static int SC_NO_CONTENT = 204;
	static int SC_NON_AUTHORITATIVE_INFORMATION = 203;
	static int SC_NOT_ACCEPTABLE = 406;
	static int SC_NOT_FOUND = 404;
	static int SC_NOT_IMPLEMENTED = 501;
	static int SC_NOT_MODIFIED = 304;
	static int SC_OK = 200;
	static int SC_PARTIAL_CONTENT = 206;
	static int SC_PAYMENT_REQUIRED = 402;
	static int SC_PRECONDITION_FAILED = 412;
	static int SC_PROXY_AUTHENTICATION_REQUIRED = 407;
	static int SC_REQUEST_ENTITY_TOO_LARGE = 413;
	static int SC_REQUEST_TIMEOUT = 408;
	static int SC_REQUEST_URI_TOO_LONG = 414;
	static int SC_REQUESTED_RANGE_NOT_SATISFIABLE = 416;
	static int SC_RESET_CONTENT = 205;
	static int SC_SEE_OTHER = 303;
	static int SC_SERVICE_UNAVAILABLE = 503;
	static int SC_SWITCHING_PROTOCOLS = 101;
	static int SC_UNAUTHORIZED = 401;
	static int SC_UNSUPPORTED_MEDIA_TYPE = 415;
	static int SC_USE_PROXY = 305;
	
	void addCookie(Cookie cookie);
	void addDateHeader( String name, long date);
	void addHeader(String name, String value);
	void addIntHeader(String name, int value);
	boolean containsHeader(String name);
	@Deprecated
	String encodeRedirectUrl(String url);
	String encodeRedirectURL(String url);
	@Deprecated
	String encodeUrl(String url);
	void sendError(int sc) throws IOException;
	void sendError(int sc, String msg) throws IOException;
	void sendRedirect(String location) throws IOException;
	void setDateHeader(String name, long date);
	void setHeader(String name, String value);
	void setIntHeader(String name, int value);
	void setStatus(int sc);
	@Deprecated
	void setStatus(int sc, String sm);
}
