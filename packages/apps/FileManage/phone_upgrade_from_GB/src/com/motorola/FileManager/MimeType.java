package com.motorola.FileManager;

import java.util.HashMap;

import android.webkit.MimeTypeMap;

public class MimeType {
	public static final int MIME_NONE = 0;
	public static final int MIME_TEXT = 1;
	public static final int MIME_IMAGE = 2;
	public static final int MIME_VIDEO = 3;
	public static final int MIME_AUDIO = 4;
	public static final int MIME_APK = 5;
	public static final int MIME_HTML = 6;
	public static final int MIME_PDF = 7;
	public static final int MIME_DOC = 8;
	public static final int MIME_PPT = 9;
	public static final int MIME_EXCEL = 10;
	public static final int MIME_ZIP = 11;
        //Begin CQG478 liyun, IKDOMINO-2266, add vcf type 2011-9-5
        public static final int MIME_VCF = 12;
        //End


    static final String TYPE_APPLICATION = "application"; //$NON-NLS-1$
    static final String TYPE_TEXT = "text"; //$NON-NLS-1$
    static final String TYPE_IMAGE = "image"; //$NON-NLS-1$
    static final String TYPE_AUDIO = "audio"; //$NON-NLS-1$
    static final String TYPE_VIDEO = "video"; //$NON-NLS-1$
    static final String TYPE_MULTIPART = "multipart"; //$NON-NLS-1$
    static final String TYPE_MESSAGE = "message"; //$NON-NLS-1$

    static final String SUBTYPE_ANY = "*"; //$NON-NLS-1$
    static final String SUBTYPE_PLAIN = "plain"; //$NON-NLS-1$
    static final String SUBTYPE_HTML = "html"; //$NON-NLS-1$
    static final String SUBTYPE_XML = "xml"; //$NON-NLS-1$
    static final String SUBTYPE_OCTET_STREAM = "octet-stream"; //$NON-NLS-1$
    static final String SUBTYPE_MIXED = "mixed"; //$NON-NLS-1$
    static final String SUBTYPE_ALTERNATIVE = "alternative"; //$NON-NLS-1$
    static final String SUBTYPE_DIGEST = "digest"; //$NON-NLS-1$
    static final String SUBTYPE_PARALLEL = "parallel"; //$NON-NLS-1$
    static final String SUBTYPE_RFC822 = "rfc822"; //$NON-NLS-1$

    static final String PARAM_CHARSET = "charset"; //$NON-NLS-1$
    static final String PARAM_BOUNDARY = "boundary"; //$NON-NLS-1$
    static final String PARAM_FORMAT = "Format"; //$NON-NLS-1$
    static final String PARAM_DEL_SP = "DelSp"; //$NON-NLS-1$

    static final String MIME_TYPE_DEFAULT = TYPE_APPLICATION + "/" + SUBTYPE_OCTET_STREAM; //$NON-NLS-1$

    private static HashMap<String, String> sMimeTypeMap = new HashMap<String, String>();
    
    static {
        sMimeTypeMap.put("323",         "text/h323"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("3dml",        "text/vnd.in3d.3dml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("3g2",         "video/3gpp2"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("3gp",         "video/3gpp"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("3gpp",         "video/3gpp"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("3gpp2",         "video/3gpp2"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("aac",         "audio/aac"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("ai",          "application/postscript"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("aif",         "audio/x-aiff"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("aifc",        "audio/x-aiff"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("aiff",        "audio/x-aiff"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("amr",         "audio/amr"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("asc",         "text/plain"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("asf",         "video/x-ms-asf"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("asm",         "text/x-asm"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("asx",         "video/x-ms-asf"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("atom",        "application/atom+xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("atomcat",     "application/atomcat+xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("atomsvc",     "application/atomsvc+xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("au",          "audio/basic"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("avi",         "video/3gpp"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("awb",         "audio/amr-wb"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("bdm",         "application/vnd.syncml.dm+wbxml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("bib",         "text/x-bibtex"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("bin",         "application/octet-stream"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("bmp",         "image/bmp"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("boo",         "text/x-boo"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("boz",         "application/x-bzip2"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("btif",        "image/prs.btif"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("bz",          "application/x-bzip"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("bz2",         "application/x-bzip2"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("c",           "text/x-csrc"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("c++",         "text/x-c++src"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("cc",          "text/x-c"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("cer",         "application/pkix-cert"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("cgm",         "image/cgm"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("class",       "application/octet-stream"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("clp",         "application/x-msclip"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("cls",         "text/x-tex"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("cmx",         "image/x-cmx"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("conf",        "text/plain"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("cpp",         "text/x-c++src"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("crl",         "application/pkix-crl"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("crt",         "application/x-x509-ca-cert"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("csh",         "text/x-csh"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("css",         "text/css"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("csv",         "text/comma-separated-values"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("cxx",         "text/x-c++src"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("d",           "text/x-dsrc"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("def",         "text/plain"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("der",         "application/x-x509-ca-cert"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("dif",         "video/x-dv"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("diff",        "text/plain"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("djv",         "image/vnd.djvu"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("djvu",        "image/vnd.djvu"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("dmg",         "application/octet-stream"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("dms",         "application/octet-stream"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("doc",         "application/msword"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("docx",        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("dot",         "application/msword"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("dotx",         "application/vnd.openxmlformats-officedocument.wordprocessingml.template"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("dsc",         "text/prs.lines.tag"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("dtd",         "application/xml-dtd"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("dts",         "audio/vnd.dts"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("dtshd",       "audio/vnd.dts.hd"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("dv",          "video/x-dv"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("dvi",         "application/x-dvi"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("dwg",         "image/vnd.dwg"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("dxf",         "image/vnd.dxf"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("ecelp4800",   "audio/vnd.nuera.ecelp4800"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("ecelp7470",   "audio/vnd.nuera.ecelp7470"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("ecelp9600",   "audio/vnd.nuera.ecelp9600"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("ecma",        "application/ecmascript"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("eml",         "message/rfc822"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("eps",         "application/postscript"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("etx",         "text/x-setext"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("excel",       "application/vnd.ms-excel"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("f",           "text/x-fortran"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("f77",         "text/x-fortran"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("f90",         "text/x-fortran"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("fli",         "video/x-fli"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("flx",         "text/vnd.fmi.flexstor"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("fly",         "text/vnd.fly"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("for",         "text/x-fortran"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("fpx",         "image/vnd.fpx"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("fst",         "image/vnd.fst"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("fvt",         "video/vnd.fvt"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("g3",          "image/g3fax"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("gcd",         "text/x-pcs-gcd"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("gif",         "image/gif"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("gtar",        "application/x-gtar"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("gz",          "application/x-gzip"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("h",           "text/x-chdr"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("h++",         "text/x-c++hdr"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("h261",        "video/h261"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("h263",        "video/h263"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("h264",        "video/h264"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("hh",          "text/x-c++hdr"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("hpp",         "text/x-c++hdr"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("hs",          "text/x-haskell"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("htc",         "text/x-component"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("htm",         "text/html"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("html",        "text/html"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("hxx",         "text/x-c++hdr"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("icc",         "application/vnd.iccprofile"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("icm",         "application/vnd.iccprofile"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("ico",         "image/x-icon"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("ics",         "text/calendar"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("icz",         "text/calendar"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("ief",         "image/ief"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("ifb",         "text/calendar"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("jad",         "text/vnd.sun.j2me.app-descriptor"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("java",        "text/x-java"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("imy",         "audio/imelody"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("jfif",        "image/pipeg"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("jng",         "image/x-jng"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("jp2",         "image/jp2"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("jpe",         "image/jpeg"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("jpeg",        "image/jpeg"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("jpg",         "image/jpeg"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("jpgm",        "video/jpm"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("jpgv",        "video/jpeg"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("jpm",         "video/jpm"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("js",          "text/javascript"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("json",        "text/json"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("kar",         "audio/midi"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("kml",         "application/vnd.google-earth.kml+xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("kmz",         "application/vnd.google-earth.kmz"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("lha",         "application/octet-stream"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("lhs",         "text/x-literate-haskell"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("ltx",         "text/x-tex"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("lvp",         "audio/vnd.lucent.voice"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("lzh",         "application/octet-stream"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("m1v",         "video/mpeg"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("m3u",         "audio/x-mpegurl"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("m4a",         "audio/mp4"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("m4u",         "video/vnd.mpegurl"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("m4v",         "video/mp4"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("mac",         "image/x-macpaint"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("man",         "text/troff"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("mdb",         "application/x-msaccess"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("mid",         "audio/midi"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("midi",        "audio/midi"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("mime",        "message/rfc822"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("mj2",         "video/mj2"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("mjp2",        "video/mj2"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("mml",         "text/mathml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("mmr",         "image/vnd.fujixerox.edmics-mmr"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("mng",         "video/x-mng"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("mny",         "application/x-msmoney"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("moc",         "text/x-moc"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("mov",         "video/quicktime"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("movie",       "video/x-sgi-movie"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("mp2",         "audio/mpeg"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("mp2a",        "audio/mpeg"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("mp3",         "audio/mpeg"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("mp4",         "video/mp4"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("mp4a",        "audio/mp4"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("mp4v",        "video/mp4"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("mpe",         "video/mpeg"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("mpeg",        "video/mpeg"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("mpg",         "video/mpeg"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("mpg4",        "video/mp4"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("mpga",        "audio/mpeg"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("mpp",         "application/vnd.ms-project"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("mpt",         "application/vnd.ms-project"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("mvb",         "application/x-msmediaview"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("mxu",         "video/vnd.mpegurl"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("npx",         "image/vnd.net-fpx"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("oda",         "application/oda"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("odc",         "application/vnd.oasis.opendocument.chart"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("odf",         "application/vnd.oasis.opendocument.formula"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("odg",         "application/vnd.oasis.opendocument.graphics"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("odi",         "application/vnd.oasis.opendocument.image"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("odp",         "application/vnd.oasis.opendocument.presentation"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("ods",         "application/vnd.oasis.opendocument.spreadsheet"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("odt",         "application/vnd.oasis.opendocument.text"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("oga",         "audio/ogg"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("ogg",         "audio/ogg"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("ogv",         "video/ogg"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("ogx",         "application/ogg"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("ota",         "audio/midi"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("otc",         "application/vnd.oasis.opendocument.chart-template"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("otf",         "application/vnd.oasis.opendocument.formula-template"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("otg",         "application/vnd.oasis.opendocument.graphics-template"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("oth",         "application/vnd.oasis.opendocument.text-web"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("oti",         "application/vnd.oasis.opendocument.image-template"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("otm",         "application/vnd.oasis.opendocument.text-master"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("otp",         "application/vnd.oasis.opendocument.presentation-template"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("ots",         "application/vnd.oasis.opendocument.spreadsheet-template"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("ott",         "application/vnd.oasis.opendocument.text-template"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("oxt",         "application/vnd.openofficeorg.extension"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("p",           "text/x-pascal"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("pas",         "text/x-pascal"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("pbm",         "image/x-portable-bitmap"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("pct",         "image/x-pict"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("pcx",         "image/x-pcx"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("pdf",         "application/pdf"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("pgm",         "image/x-portable-graymap"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("pgp",         "application/pgp-encrypted"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("phps",        "text/text"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("pic",         "image/x-pict"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("pict",        "image/pict"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("pls",         "audio/x-scpls"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("png",         "image/png"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("pnm",         "image/x-portable-anymap"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("pnt",         "image/x-macpaint"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("pntg",        "image/x-macpaint"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("pot",         "text/plain"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("ppm",         "image/x-portable-pixmap"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("pps",         "application/vnd.ms-powerpoint"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("ppt",         "application/vnd.ms-powerpoint"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("pot",         "application/vnd.ms-powerpoint"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("pptx",        "application/vnd.openxmlformats-officedocument.presentationml.presentation"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("ppsx",         "application/vnd.openxmlformats-officedocument.presentationml.slideshow"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("potx",         "application/vnd.openxmlformats-officedocument.presentationml.template"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("ps",          "application/postscript"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("psd",         "image/vnd.adobe.photoshop"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("pya",         "audio/vnd.ms-playready.media.pya"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("pyv",         "video/vnd.ms-playready.media.pyv"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("qt",          "video/quicktime"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("qti",         "image/x-quicktime"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("qtif",        "image/x-quicktime"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("ra",          "audio/x-pn-realaudio"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("ram",         "audio/x-pn-realaudio"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("rar",         "application/x-rar-compressed"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("ras",         "image/x-cmu-raster"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("rdf",         "application/rdf+xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("rgb",         "image/x-rgb"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("rlc",         "image/vnd.fujixerox.edmics-rlc"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("rmi",         "audio/midi"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("rmp",         "audio/x-pn-realaudio-plugin"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("roff",        "text/troff"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("rpm",         "audio/x-pn-realaudio-plugin"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("rsd",         "application/rsd+xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("rss",         "application/rss+xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("rtf",         "text/rtf"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("rtttl",       "audio/midi"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("rtx",         "text/richtext"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("s",           "text/x-asm"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("sbml",        "application/sbml+xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("sgm",         "text/sgml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("sgml",        "text/sgml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("sh",          "application/x-sh"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("shar",        "application/x-shar"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("shf",         "application/shf+xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("sig",         "application/pgp-signature"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("sit",         "application/x-stuffit"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("sitx",        "application/x-stuffitx"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("smf",         "audio/sp-midi"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("smi",         "application/smil+xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("smil",        "application/smil+xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("snd",         "audio/basic"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("spot",        "text/vnd.in3d.spot"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("spx",         "audio/ogg"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("ssml",        "application/ssml+xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("sty",         "text/x-tex"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("svg",         "image/svg+xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("svgz",        "image/svg+xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("swf",         "application/x-shockwave-flash"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("t",           "text/troff"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("tar",         "application/x-tar"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("tcl",         "text/x-tcl"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("tex",         "text/x-tex"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("text",        "text/plain"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("tga",         "image/targa"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("tgz",         "application/x-gzip"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("tif",         "image/tiff"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("tiff",        "image/tiff"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("tmo",         "application/vnd.tmobile-livetv"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("torrent",     "application/x-bittorrent"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("tr",          "text/troff"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("ts",          "text/texmacs"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("tsv",         "text/tab-separated-values"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("txt",         "text/plain"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("uls",         "text/iuls"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("uri",         "text/uri-list"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("uris",        "text/uri-list"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("urls",        "text/uri-list"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("uu",          "text/x-uuencode"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("vcf",         "text/x-vcard"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("vcs",         "text/x-vcalendar"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("viv",         "video/vnd.vivo"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("vxml",        "application/voicexml+xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("wav",         "audio/x-wav"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("wax",         "audio/x-ms-wax"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("wbmp",        "image/vnd.wap.wbmp"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("wm",          "video/x-ms-wm"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("wma",         "audio/x-ms-wma"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("wmd",         "application/x-ms-wmd"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("wmf",         "application/x-msmetafile"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("wml",         "text/vnd.wap.wml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("wmlc",        "application/vnd.wap.wmlc"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("wmls",        "text/vnd.wap.wmlscript"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("wmlsc",       "application/vnd.wap.wmlscriptc"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("wmv",         "video/x-ms-wmv"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("wmx",         "video/x-ms-wmx"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("wmz",         "application/x-ms-wmz"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("wpd",         "application/vnd.wordperfect"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("wpl",         "application/vnd.ms-wpl"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("wps",         "application/vnd.ms-works"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("wri",         "application/x-mswrite"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("wsdl",        "application/wsdl+xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("wspolicy",    "application/wspolicy+xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("wvx",         "video/x-ms-wvx"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("xbm",         "image/x-xbitmap"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("xht",         "application/xhtml+xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("xhtml",       "application/xhtml+xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("xif",         "image/vnd.xiff"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("xla",         "application/vnd.ms-excel"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("xlc",         "application/vnd.ms-excel"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("xlm",         "application/vnd.ms-excel"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("xls",         "application/vnd.ms-excel"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("xlsx",        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("xlt",         "application/vnd.ms-excel"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("xltx",         "application/vnd.openxmlformats-officedocument.spreadsheetml.template"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("xmf",         "audio/midi"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("xml",         "application/xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("xop",         "application/xop+xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("xpm",         "image/x-xpixmap"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("xsl",         "application/xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("xslt",        "application/xslt+xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("xsm",         "application/vnd.syncml+xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("xspf",        "application/xspf+xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("xul",         "application/vnd.mozilla.xul+xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("xvm",         "application/xv+xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("xvml",        "application/xv+xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("xwd",         "image/x-xwindowdump"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("zip",         "application/zip"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("zmm",         "application/vnd.handheld-entertainment+xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("mkv",         "video/x-matroska"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("lmx",         "application/vnd.nokia.landmarkcollection+xml"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("rm",          "video/rm"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("rmvb",        "video/rmvb"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("ra",          "audio/ra"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("flv",         "video/flv"); //$NON-NLS-1$ //$NON-NLS-2$
        //Begin CQG478 liyun, IKDOMINO-3047, add qcp type 2011-10-12
        sMimeTypeMap.put("qcp",         "audio/vnd.qcelp"); //$NON-NLS-1$ //$NON-NLS-2$
        //End
        sMimeTypeMap.put("flac",         "audio/flac"); //$NON-NLS-1$ //$NON-NLS-2$
        sMimeTypeMap.put("webp",         "image/jpeg"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    public static String getMimeType(String suffix) {
    	String s = suffix == null ? null : suffix.toLowerCase();
        String result = suffix == null ? null : sMimeTypeMap.get(s);
        if (result == null) {
            result = MimeTypeMap.getSingleton().getMimeTypeFromExtension(s);
        }
        return result == null ? MIME_TYPE_DEFAULT : result;
    }
     
 	public static int getMimeId(String mimeType) {
		if(mimeType == null)
			return MIME_NONE;
		
		if(mimeType.indexOf("text") == 0) {
			if(mimeType.equals("text/htm") || mimeType.equals("text/html"))
				return MIME_HTML;
                        //Begin CQG478 liyun, IKDOMINO-2266, add vcf type 2011-9-5
                        else if(mimeType.equals("text/x-vcard"))
				return MIME_VCF;
			//End
			else
				return MIME_TEXT;
		}
		
		if(mimeType.indexOf("image") == 0)
			return MIME_IMAGE;

		if(mimeType.indexOf("video") == 0)
			return MIME_VIDEO;
		
		if(mimeType.indexOf("audio") == 0)
			return MIME_AUDIO;
		
		if(mimeType.indexOf("application") == 0) {
			if(mimeType.equals("application/vnd.android.package-archive"))
				return MIME_APK;
			
			if(mimeType.equals("application/pdf"))
				return MIME_PDF;
			
			if(mimeType.equals("application/msword") 
			   || mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
			   || mimeType.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.template"))
				return MIME_DOC;
				
			if(mimeType.equals("application/vnd.ms-powerpoint")
			   || mimeType.equals("application/vnd.openxmlformats-officedocument.presentationml.presentation")
			   || mimeType.equals("application/vnd.openxmlformats-officedocument.presentationml.template")
			   || mimeType.equals("application/vnd.openxmlformats-officedocument.presentationml.slideshow"))
				return MIME_PPT;
			
			if(mimeType.equals("application/vnd.ms-excel") 
			   || mimeType.equals( "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
		       || mimeType.equals("application/vnd.openxmlformats-officedocument.spreadsheetml.template"))
				return MIME_EXCEL;

			if(mimeType.equals("application/zip"))
				return MIME_ZIP;
                     
                        if(mimeType.equals("application/vnd.rn-realmedia-vbr") 
			   ||mimeType.equals("application/vnd.rn-realmedia")) {				
				return MIME_VIDEO;
			}
		}
		
		return MIME_NONE;
	} 
}
