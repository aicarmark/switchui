package javax.servlet.http;

import java.io.IOException;
import java.io.Serializable;
import java.text.MessageFormat;

import javax.servlet.GenericServlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

public abstract class HttpServlet extends GenericServlet {

    public HttpServlet() {

    }

    protected void doDelete(HttpServletRequest req, HttpServletResponse resp)
	    throws ServletException, IOException {
	resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
	    throws ServletException, IOException {
	resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    protected void doHead(HttpServletRequest req, HttpServletResponse resp)
	    throws ServletException, IOException {
	resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    protected void doOptions(HttpServletRequest req, HttpServletResponse resp)
	    throws ServletException, IOException {
	resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
	    throws ServletException, IOException {
	resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
	    throws ServletException, IOException {
	resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    protected void doTrace(HttpServletRequest req, HttpServletResponse resp)
	    throws ServletException, IOException {
	resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    }

    protected long getLastModified(HttpServletRequest req) {
	return -1;
    }

    protected void service(HttpServletRequest req, HttpServletResponse resp)
	    throws ServletException, IOException {
	String method = req.getMethod();

	if (method.equals(__GET)) {
	    long lastModified = getLastModified(req);
	    if (lastModified == -1) {
		doGet(req, resp);
	    } else {
		long ifModifiedSince = req.getDateHeader(__IFMODSINCE);
		if (ifModifiedSince == -1) {
		    doGet(req, resp);
		} else {
		    if (lastModified >= 0)
			resp.setDateHeader(__LASTMOD, lastModified);
		    long now = System.currentTimeMillis();
		    if (now < ifModifiedSince || ifModifiedSince < lastModified) {
			doGet(req, resp);
		    } else {
			resp.sendError(HttpServletResponse.SC_NOT_MODIFIED);
		    }
		}
	    }

	} else if (method.equals(__HEAD)) {
	    long lastModified = getLastModified(req);
	    if (lastModified >= 0)
		resp.setDateHeader(__LASTMOD, lastModified);
	    doHead(req, resp);
	} else if (method.equals(__POST)) {
	    doPost(req, resp);
	} else if (method.equals(__PUT)) {
	    doPut(req, resp);
	} else if (method.equals(__DELETE)) {
	    doDelete(req, resp);
	} else if (method.equals(__OPTIONS)) {
	    doOptions(req, resp);
	} else if (method.equals(__TRACE)) {
	    doTrace(req, resp);
	} else {
	    String errMsg = "Method {0} is not defined in RFC 2068 and is not supported by the Servlet API";
	    Object[] errArgs = new Object[1];
	    errArgs[0] = method;
	    errMsg = MessageFormat.format(errMsg, errArgs);
	    resp.sendError(HttpServletResponse.SC_NOT_IMPLEMENTED, errMsg);
	}
    }

    public void service(ServletRequest req, ServletResponse resp)
	    throws ServletException, IOException {
	service((HttpServletRequest) req, (HttpServletResponse) resp);
    }

    private static final String __DELETE = "DELETE";
    private static final String __HEAD = "HEAD";
    private static final String __GET = "GET";
    private static final String __OPTIONS = "OPTIONS";
    private static final String __POST = "POST";
    private static final String __PUT = "PUT";
    private static final String __TRACE = "TRACE";

    private static final String __IFMODSINCE = "If-Modified-Since";
    private static final String __LASTMOD = "Last-Modified";

}
