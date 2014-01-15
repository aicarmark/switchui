package javax.servlet;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Set;

public interface ServletContext {
    Object getAttribute(String attr);

    Enumeration getAttributeNames();

    ServletContext getContext(String ctx);

    String getInitParameter(String par);

    Enumeration getInitParameterNames();

    int getMajorVersion();

    String getMimeType(String arg);

    int getMinorVersion();

    RequestDispatcher getNamedDispatcher(String nam);

    String getRealPath(String arg);

    RequestDispatcher getRequestDispatcher(String arg);

    URL getResource(String arg) throws MalformedURLException;

    InputStream getResourceAsStream(String arg);

    Set getResourcePaths(String arg);

    String getServerInfo();

    Servlet getServlet(String arg) throws ServletException;

    String getServletContextName();

    Enumeration getServletNames();

    Enumeration getServlets();

    void log(Exception arg0, String arg1);

    void log(String arg0);

    void log(String arg0, Throwable arg1);

    void removeAttribute(String attr);

    void setAttribute(String attr, Object obj);
}
