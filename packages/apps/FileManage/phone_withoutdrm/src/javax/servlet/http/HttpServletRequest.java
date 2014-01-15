package javax.servlet.http;

import java.security.Principal;
import java.util.Enumeration;

import javax.servlet.ServletRequest;

public interface HttpServletRequest extends ServletRequest {
    String getAuthType();

    String getContextPath();

    Cookie[] getCookies();

    long getDateHeader(String arg);

    String getHeader(String h);

    Enumeration getHeaderNames();

    Enumeration getHeaders(String h);

    int getIntHeader(String h);

    String getMethod();

    String getPathInfo();

    String getPathTranslated();

    String getQueryString();

    String getRemoteUser();

    String getRequestedSessionId();

    String getRequestURI();

    StringBuffer getRequestURL();

    String getServletPath();

    HttpSession getSession();

    HttpSession getSession(boolean arg);

    Principal getUserPrincipal();

    boolean isRequestedSessionIdFromCookie();

    boolean isRequestedSessionIdFromUrl();

    boolean isRequestedSessionIdFromURL();

    boolean isRequestedSessionIdValid();

    boolean isUserInRole(String role);
}
