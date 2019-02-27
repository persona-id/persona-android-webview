package com.google.chrome.android.webrtcsample;

import android.annotation.TargetApi;
import android.app.Activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.PermissionRequest;
import android.webkit.WebViewClient;

import java.util.HashMap;


public class MainActivity extends Activity {

    private static final String TAG = MainActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final WebView personaWebView = (WebView) findViewById(R.id.fragment_main_webview);

        setUpWebViewDefaults(personaWebView);

        // Initialize Persona
        HashMap<String, String> personaOptions = new HashMap<>();
        personaOptions.put("is-webview", "true");

        personaOptions.put("blueprint-id", "blu_M1ivtd7uaiZQBESJvR4tN8Fu");
        personaOptions.put("redirect-uri", "https://personademo.com");
        personaOptions.put("baseUrl", "https://withpersona.com:3000/verify");
        final Uri personaUrl = generatePersonaUrl(personaOptions);

        personaWebView.loadUrl(personaUrl.toString());

        personaWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                Uri parsedUri = Uri.parse(url);
                if (parsedUri.getScheme().equals("persona")) {
                    String action = parsedUri.getHost();
                    HashMap<String, String> linkData = parsePersonaUriData(parsedUri);

                    if (action.equals("start")) {
                        // User start an inquiry
                        Log.d("Inquiry Id: ", linkData.get("inquiryId"));
                        Log.d("Subject: ", linkData.get("subject"));
                    } else if (action.equals("success")) {
                        // User succeeded verification

                        // Reload Persona in the Webview
                        // You will likely want to transition the view at this point.
                        personaWebView.loadUrl(personaUrl.toString());
                    }
                    // Override URL loading
                    return true;
                } else if (parsedUri.getScheme().equals("https") ||
                        parsedUri.getScheme().equals("http")) {
                    // Open in browser - this is most likely external help links
                    view.getContext().startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    // Override URL loading
                    return true;
                } else {
                    // Unknown case - do not override URL loading
                    return false;
                }
            }
        });

        personaWebView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                Log.d(TAG, "onPermissionRequest");
                runOnUiThread(new Runnable() {
                    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
                    @Override
                    public void run() {
                        if(request.getOrigin().toString().equals("https://apprtc-m.appspot.com/")) {
                            request.grant(request.getResources());
                        } else {
                            request.deny();
                        }
                    }
                });
            }

        });
    }

    private void setUpWebViewDefaults(WebView webView) {
        WebSettings settings = webView.getSettings();

        // Enable Javascript
        settings.setJavaScriptEnabled(true);

        // Use WideViewport and Zoom out if there is no viewport defined
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);

        // Enable pinch to zoom without the zoom buttons
        settings.setBuiltInZoomControls(true);

        // Allow use of Local Storage
        settings.setDomStorageEnabled(true);

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
            // Hide the zoom controls for HONEYCOMB+
            settings.setDisplayZoomControls(false);
        }

        // Enable remote debugging via chrome://inspect
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        // AppRTC requires third party cookies to work
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptThirdPartyCookies(webView, true);
    }

    // Generate a Persona initialization URL based on a set of configuration options
    public Uri generatePersonaUrl(HashMap<String,String> personaOptions) {
        Uri.Builder builder = Uri.parse(personaOptions.get("baseUrl"))
                .buildUpon()
                .appendQueryParameter("isWebview", "true")
                .appendQueryParameter("isMobile", "true");
        for (String key : personaOptions.keySet()) {
            if (!key.equals("baseUrl")) {
                builder.appendQueryParameter(key, personaOptions.get(key));
            }
        }
        return builder.build();
    }

    // Parse a Persona redirect URL querystring into a HashMap for easy manipulation and access
    public HashMap<String,String> parsePersonaUriData(Uri personaUri) {
        HashMap<String,String> personaData = new HashMap<String,String>();
        for(String key : personaUri.getQueryParameterNames()) {
            personaData.put(key, personaUri.getQueryParameter(key));
        }
        return personaData;
    }
}
