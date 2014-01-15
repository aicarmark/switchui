package javax.servlet;

import java.io.IOException;

public interface Servlet {
	void destroy();
	ServletConfig getServletConfig();
	String getServletInfo();
	void init(ServletConfig sc) throws ServletException;
	public void service( ServletRequest request, ServletResponse response ) throws ServletException, IOException;
}
