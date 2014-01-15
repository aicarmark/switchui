package javax.servlet;

import java.util.Enumeration;

public interface ServletConfig {
	String getInitParameter(String par);
	Enumeration getInitParameterNames();
	ServletContext getServletContext();
	String getServletName();
}
