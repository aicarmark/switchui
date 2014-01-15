package javax.servlet;

import java.io.IOException;
import java.io.InputStream;

public abstract class ServletInputStream extends InputStream {
    protected ServletInputStream() {

    }

    public int readLine(byte[] b, int off, int len) throws IOException {
	boolean found = false;
	int br = 0;
	int total = 0;

	while (!found && br != -1 && total < len) {
	    br = read();
	    if (br != -1) {
		b[off + total] = (byte) br;
		total++;
		if (br == '\n')
		    found = true;
	    }
	}
	if (br != -1)
	    return total;
	else
	    return br;
    }
}
