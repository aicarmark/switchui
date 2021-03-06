package javax.servlet;

import java.io.IOException;

public interface RequestDispatcher {
    void forward(ServletRequest request, ServletResponse response)
	    throws ServletException, IOException;

    void include(ServletRequest request, ServletResponse response)
	    throws ServletException, IOException;
}
