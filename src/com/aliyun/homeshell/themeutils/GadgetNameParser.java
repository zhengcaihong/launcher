package com.aliyun.homeshell.themeutils;

import java.util.HashMap;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class GadgetNameParser extends DefaultHandler {
    private Map<String, String> mGadgetName;
    private String mLabel;
    private String mName;

    public Map<String, String> getGadgetName() {
        return mGadgetName;
    }

    @Override
    public void characters(char[] ch, int start, int length)
            throws SAXException {
        // TODO Auto-generated method stub
        if (mLabel != null) {
            mName = new String(ch, start, length);
        }
    }

    @Override
    public void startDocument() throws SAXException {
        // TODO Auto-generated method stub
        mGadgetName = new HashMap<String, String>();
    }

    @Override
    public void startElement(String uri, String localName, String qName,
            Attributes attributes) throws SAXException {
        // TODO Auto-generated method stub
        if ("item".equals(qName)) {
            mLabel = attributes.getValue(0);
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        // TODO Auto-generated method stub
        if (mLabel != null) {
            mGadgetName.put(mLabel, mName);
            mLabel = null;
        }
    }

}
