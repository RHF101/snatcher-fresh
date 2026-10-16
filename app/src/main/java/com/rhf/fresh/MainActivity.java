package com.rhf.fresh;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private WebView wv;
    private EditText urlInput;
    private TextView status;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Log.i("Fresh", "onCreate SDK=" + Build.VERSION.SDK_INT);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0a0a0f"));

        // ============ UI PANEL (top) ============
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundColor(Color.parseColor("#13131f"));
        panel.setPadding(dp(10), dp(10), dp(10), dp(8));

        // Title
        TextView title = new TextView(this);
        title.setText("Fresh V1.5 — WebView Test");
        title.setTextColor(Color.parseColor("#4ade80"));
        title.setTextSize(14);
        panel.addView(title);

        // URL row
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = dp(6);
        row.setLayoutParams(rlp);

        urlInput = new EditText(this);
        urlInput.setText("https://example.com");
        urlInput.setTextColor(Color.WHITE);
        urlInput.setTextSize(11);
        urlInput.setBackgroundColor(Color.parseColor("#0a0a0f"));
        urlInput.setPadding(dp(8), dp(8), dp(8), dp(8));
        urlInput.setSingleLine(true);
        row.addView(urlInput, new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button btnGo = new Button(this);
        btnGo.setText("GO");
        btnGo.setTextSize(11);
        btnGo.setTextColor(Color.parseColor("#0a0a0f"));
        btnGo.setBackgroundColor(Color.parseColor("#4ade80"));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        blp.leftMargin = dp(6);
        btnGo.setLayoutParams(blp);
        btnGo.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                String url = urlInput.getText().toString().trim();
                if (!url.isEmpty()) {
                    status.setText("Loading...");
                    wv.loadUrl(url);
                }
            }
        });
        row.addView(btnGo);
        panel.addView(row);

        // Status
        status = new TextView(this);
        status.setText("Ready. Tap GO.");
        status.setTextColor(Color.parseColor("#888"));
        status.setTextSize(10);
        status.setPadding(0, dp(4), 0, 0);
        panel.addView(status);

        root.addView(panel);

        // ============ WEBVIEW (bottom) ============
        wv = new WebView(this);
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowFileAccessFromFileURLs(true);
        s.setAllowUniversalAccessFromFileURLs(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setUserAgentString("Mozilla/5.0 (Linux; Android 13; SM-S911B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36");
        s.setBlockNetworkLoads(false);

        wv.setWebChromeClient(new WebChromeClient());
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                status.setText("Loaded: " + url.substring(0, Math.min(60, url.length())));
            }
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                status.setText("Error " + errorCode + ": " + description);
            }
        });
        wv.loadUrl("about:blank");

        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        wv.setLayoutParams(wlp);
        root.addView(wv);

        setContentView(root);
    }

    private int dp(int v) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
            getResources().getDisplayMetrics());
    }

    @Override
    public void onBackPressed() {
        if (wv != null && wv.canGoBack()) wv.goBack();
        else super.onBackPressed();
    }
}
