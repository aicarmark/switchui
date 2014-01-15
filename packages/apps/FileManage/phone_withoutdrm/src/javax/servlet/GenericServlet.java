package javax.servlet;

import java.io.IOException;
import java.io.Serializable;
import java.util.Enumeration;

public abstract class GenericServlet implements Servlet, ServletConfig {

    public void destroy() {
    }

    public ServletConfig getServletConfig() {
	return mServletConfig;
    }

    public String getServletInfo() {
	return null;
    }

    public void init(ServletConfig sc) throws ServletException {
	mServletConfig = sc;
	init();
    }

    public void init() throws ServletException {

    }

    public abstract void service(ServletRequest request,
	    ServletResponse response) throws ServletException, IOException;

    public String getInitParameter(String par) {
	return mServletConfig.getInitParameter(par);
    }

    public Enumeration getInitParameterNames() {
	return mServletConfig.getInitParameterNames();
    }

    public ServletContext getServletContext() {
	return mServletConfig.getServletContext();
    }

    public String getServletName() {
	// TODO Auto-generated method stub
	return null;
    }

    void log(String msg) {
	mServletConfig.getServletContext().log(
		getClass().getName() + ": " + msg);
    }

    void log(String msg, Throwable t) {
	mServletConfig.getServletContext().log(
		getClass().getName() + ": " + msg, t);
    }

    private ServletConfig mServletConfig;
}
