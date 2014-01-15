package javax.servlet.http;

public class Cookie {
    public Cookie(String name, String value) {
	mName = name;
	mValue = value;
    }

    public String getComment() {
	return mComment;
    }

    public String getDomain() {
	return mDomain;
    }

    public int getMaxAge() {
	return mMaxAge;
    }

    public String getName() {
	return mName;
    }

    public String getPath() {
	return mPath;
    }

    public boolean getSecure() {
	return mSecure;
    }

    public String getValue() {
	return mValue;
    }

    public int getVersion() {
	return mVersion;
    }

    public void setComment(String purpose) {
	mComment = purpose;
    }

    public void setDomain(String pattern) {
	mDomain = pattern;
    }

    public void setMaxAge(int expiry) {
	mMaxAge = expiry;
    }

    public void setPath(String uri) {
	mPath = uri;
    }

    public void setSecure(boolean s) {
	mSecure = s;
    }

    public void setValue(String val) {
	mValue = val;
    }

    public void setVersion(int v) {
	mVersion = v;
    }

    private String mComment;
    private String mDomain;
    private int mMaxAge;
    private String mPath;
    private boolean mSecure;
    private String mValue;
    private int mVersion;
    private String mName;
}
