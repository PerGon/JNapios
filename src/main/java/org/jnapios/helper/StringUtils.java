package org.jnapios.helper;

import java.util.ArrayList;
import java.util.List;

public class StringUtils {

    //BEGIN - Methods retrieved from StringUtils of Apache - commons-lang.jar.
    //No code modification made - just copied the methods

    //Would not include the the commons-lang of apache, just to use the StringUtils
    //I want to keep the jar file as smallest as possible!

    public static String trim(String str) {
        return str == null ? null : str.trim();
    }

    public static boolean isEmpty(CharSequence cs) {
        return cs == null || cs.length() == 0;
    }

    public static List<String> substringsBetween(String str, String open, String close) {
        if (str == null || isEmpty(open) || isEmpty(close)) {
            return null;
        }
        int strLen = str.length();
        if (strLen == 0) {
            return new ArrayList<>();
        }
        int closeLen = close.length();
        int openLen = open.length();
        List<String> list = new ArrayList<String>();
        int pos = 0;
        while (pos < strLen - closeLen) {
            int start = str.indexOf(open, pos);
            if (start < 0) {
                break;
            }
            start += openLen;
            int end = str.indexOf(close, start);
            if (end < 0) {
                break;
            }
            list.add(str.substring(start, end));
            pos = end + closeLen;
        }
        if (list.isEmpty()) {
            return null;
        }
        return list;
    }
    //END - Methods retrieved from StringUtils of Apache.
}
