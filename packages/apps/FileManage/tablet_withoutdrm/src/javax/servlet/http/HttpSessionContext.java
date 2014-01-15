package javax.servlet.http;

import java.util.Enumeration;

@Deprecated
public interface HttpSessionContext {
	@Deprecated
	Enumeration getIds();
	@Deprecated
	HttpSession getSession(String arg);
}
