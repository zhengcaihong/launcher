package com.aliyun.homeshell.utils;

import java.io.IOException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.util.AttributeSet;

public class ParseApkPackage {
    public static String parsePackageName(XmlPullParser parser, AttributeSet attrs)
                               throws IOException, XmlPullParserException {
        int type;

        while ((type = parser.next()) != XmlPullParser.START_TAG
                && type != XmlPullParser.END_DOCUMENT){}

        if (type != XmlPullParser.START_TAG) {
            return null;
        }
        
        if (!parser.getName().equals("manifest")) {
            return null;
        }

        String pkgName = attrs.getAttributeValue(null, "package");
        if (pkgName == null || pkgName.length() == 0) {
            return null;
        }

        String nameError = validateName(pkgName, true);
        if (nameError != null && !"android".equals(pkgName)) {
            return null;
        }

        return pkgName.intern();
    }

    private static String validateName(String name, boolean requiresSeparator) {
        boolean hasSep = false;
        boolean front = true;

        final int N = name.length();
        for (int i = 0; i < N; i++) {
            final char c = name.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
                front = false;
                continue;
            }
            if (!front) {
                if ((c >= '0' && c <= '9') || c == '_') {
                    continue;
                }
            }
            if (c == '.') {
                hasSep = true;
                front = true;
                continue;
            }

            return "bad character '" + c + "'";
        }
        return hasSep || !requiresSeparator
                ? null : "must have at least one '.' separator";
    }
}
