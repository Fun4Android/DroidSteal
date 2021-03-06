/*    	AuthHelper.java reads Authentication information and analyzes cookies for matching to a definition
    	Copyright (C) 2013-2014 Zach Brown <Zbob75x@gmail.com>
    	
    	This software was supported by the University of Trier 

	    This program is free software; you can redistribute it and/or modify
	    it under the terms of the GNU General Public License as published by
	    the Free Software Foundation; either version 3 of the License, or
	    (at your option) any later version.
	
	    This program is distributed in the hope that it will be useful,
	    but WITHOUT ANY WARRANTY; without even the implied warranty of
	    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	    GNU General Public License for more details.
	
	    You should have received a copy of the GNU General Public License along
	    with this program; if not, write to the Free Software Foundation, Inc.,
	    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA. */
package com.zbrown.droidsteal.auth;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import com.zbrown.droidsteal.R;
import com.zbrown.droidsteal.activities.ListenActivity;
import com.zbrown.droidsteal.helper.Constants;
import com.zbrown.droidsteal.helper.DBHelper;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AuthHelper {
    private static HashMap<String, AuthDefinition> authDefList = new HashMap<String, AuthDefinition>();
    private static AuthDefinition generic = null;
    private static String binaryPath = null;
    private static HashMap<String, Object> blacklist = null;
    private static Handler handler = null;

    public static void init(Context c, Handler handler) {
        AuthHelper.handler = handler;
        try {
            readConfig(c);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void readConfig(Context c) throws XmlPullParserException, IOException {
        blacklist = DBHelper.getBlacklist(c);
        XmlResourceParser xpp = c.getResources().getXml(R.xml.auth);

        xpp.next();
        int eventType = xpp.getEventType();

        String mobileurl = null;
        String name = null;
        String url = null;
        String domain = null;
        String idurl = null;
        String regexp = null;
        ArrayList<String> cookieNames = new ArrayList<String>();

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && xpp.getName().equals("auth")) {
                name = null;
                url = null;
                mobileurl = null;
                domain = null;
                idurl = null;
                regexp = null;
                cookieNames = new ArrayList<String>();
            }
            while (!(eventType == XmlPullParser.END_TAG && xpp.getName().equals("auth")) && eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (xpp.getName().equals("name")) {
                        xpp.next();
                        name = xpp.getText();
                    } else if (xpp.getName().equals("url")) {
                        xpp.next();
                        url = xpp.getText();
                    } else if (xpp.getName().equals("domain")) {
                        xpp.next();
                        domain = xpp.getText();
                    } else if (xpp.getName().equals("cookiename")) {
                        xpp.next();
                        cookieNames.add(xpp.getText());
                    } else if (xpp.getName().equals("mobileurl")) {
                        xpp.next();
                        mobileurl = xpp.getText();
                    } else if (xpp.getName().equals("idurl")) {
                        xpp.next();
                        idurl = xpp.getText();
                    } else if (xpp.getName().equals("regexp")) {
                        xpp.next();
                        regexp = xpp.getText();
                    }
                }
                eventType = xpp.next();
            }
            if (name != null && url != null && domain != null && cookieNames != null && !cookieNames.isEmpty()) {
                authDefList.put(name, new AuthDefinition(cookieNames, url, mobileurl, domain, name, idurl, regexp));
            }
            eventType = xpp.next();
        }
        if (ListenActivity.generic) {
            generic = new AuthDefinitionGeneric();
        }
    }

    private static List<Auth> match(String line) {
        List<Auth> lst = new ArrayList<Auth>();
        lst.clear();
        for (String key : authDefList.keySet()) {
            AuthDefinition ad = authDefList.get(key);
            Auth a = ad.getAuthFromCookieString(line);
            if (a != null) {
                if (Constants.DEBUG) {
                    Log.d(Constants.APPLICATION_TAG, "MATCH:" + a.getName());
                }
                if (blacklist.containsKey(a.getName())) {
                    continue;
                }
                lst.add(a);
            }
        }
        if (ListenActivity.generic && generic != null && lst.isEmpty()) {
            Auth a = generic.getAuthFromCookieString(line);
            if (a != null && a.getName() != null) {
                if (!blacklist.containsKey(a.getName())) {
                    lst.add(a);
                }
            }
        }
        return lst;
    }

    public static void process(String line) {
        List<Auth> lstAuth = match(line);
        if (lstAuth != null && !lstAuth.isEmpty()) {
            for (Auth a : lstAuth) {
                Message m = handler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putSerializable(Constants.BUNDLE_KEY_AUTH, a);
                bundle.putString(Constants.BUNDLE_KEY_TYPE, Constants.BUNDLE_TYPE_NEWAUTH);
                m.setData(bundle);
                handler.sendMessage(m);
            }
        }
    }

    public static void addToBlackList(Context c, String name) {
        blacklist.put(name, null);
        DBHelper.addBlacklistEntry(c, name);
    }

    public static void clearBlacklist() {
        blacklist.clear();
    }

}