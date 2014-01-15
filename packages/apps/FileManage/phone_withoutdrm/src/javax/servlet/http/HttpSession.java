package javax.servlet.http;

import java.util.Enumeration;

import javax.servlet.ServletContext;

public interface HttpSession {
    Object getAttribute(String attr);

    Enumeration getAttributeNames();

    long getCreationTime();

    String getId();

    long getLastAccessedTime();

    int getMaxInactiveInterval();

    ServletContext getServletContext();

    @Deprecated
    HttpSessionContext getSessionContext();

    @Deprecated
    Object getValue(String arg);

    @Deprecated
    String[] getValueNames();

    void invalidate();

    boolean isNew();

    @Deprecated
    void putValue(String arg0, Object arg1);

    void removeAttribute(String attr);

    @Deprecated
    void removeValue(String val);

    void setAttribute(String arg0, Object arg1);

    void setMaxInactiveInterval(int val);
}
