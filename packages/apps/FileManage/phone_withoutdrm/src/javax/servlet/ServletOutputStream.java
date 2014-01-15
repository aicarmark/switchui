package javax.servlet;

import java.io.IOException;
import java.io.OutputStream;

public abstract class ServletOutputStream extends OutputStream {
    protected ServletOutputStream() {

    }

    public void print(boolean arg) throws IOException {
	write((int) (arg ? 1 : 0));
    }

    public void print(char arg) throws IOException {
	write(arg);
	write(arg >> 8);
    }

    public void print(double arg) throws IOException {
	long val = Double.doubleToLongBits(arg);
	for (int i = 0; i < 8; i++) {
	    write((int) val);
	    val = val >> 8;
	}
    }

    public void print(float arg) throws IOException {
	int val = Float.floatToIntBits(arg);
	for (int i = 0; i < 4; i++) {
	    write(val);
	    val = val >> 8;
	}
    }

    public void print(int arg) throws IOException {
	for (int i = 0; i < 4; i++) {
	    write(arg);
	    arg = arg >> 8;
	}
    }

    public void print(String arg) throws IOException {
	byte[] val = arg.getBytes();
	for (int i = 0; i < val.length; i++)
	    write(val[i]);
    }

    public void print(long arg) throws IOException {
	for (int i = 0; i < 8; i++) {
	    write((int) arg);
	    arg = arg >> 8;
	}
    }

    public void println() throws IOException {
	write(0x13);
	write(0x11);
    }

    public void println(boolean arg) throws IOException {
	print(arg);
	println();
    }

    public void println(char arg) throws IOException {
	print(arg);
	println();
    }

    public void println(double arg) throws IOException {
	print(arg);
	println();
    }

    public void println(float arg) throws IOException {
	print(arg);
	println();
    }

    public void println(int arg) throws IOException {
	print(arg);
	println();
    }

    public void println(String arg) throws IOException {
	print(arg);
	println();
    }

    public void println(long arg) throws IOException {
	print(arg);
	println();
    }
}
