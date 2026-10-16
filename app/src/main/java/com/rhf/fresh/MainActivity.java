package com.rhf.fresh;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {
    private WebView wv;
    private File captureDir;
    private final Set<String> urlSeen = new HashSet<>();
    private int fileCount = 0;
    private long totalBytes = 0;
    private final Map<String, Integer> typeCount = new HashMap<>();
    private final Handler ui = new Handler(Looper.getMainLooper());

    private EditText urlInput;
    private Button btnGo, btnClear;
    private TextView statText, typeText;
    private LinearLayout logList;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Log.i("FreshV2", "onCreate start");

        // Folder capture di Download/SnatcherV2
        captureDir = new File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS), "SnatcherV2/captures");
        if (!captureDir.exists()) {
            boolean ok = captureDir.mkdirs();
            Log.i("FreshV2", "capture dir: " + ok + " @ " + captureDir.getAbsolutePath());
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0a0a0f"));

        root.addView(buildUI());

        try {
            wv = new WebView(this);
            WebSettings s = wv.getSettings();
            s.setJavaScriptEnabled(true);
            s.setDomStorageEnabled(true);
            s.setAllowFileAccess(true);
            s.setAllowFileAccessFromFileURLs(true);
            s.setAllowUniversalAccessFromFileURLs(true);
            s.setMediaPlaybackRequiresUserGesture(false);
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
            s.setUserAgentString("Mozilla/5.0 (Linux; Android 11; Infinix X689) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36");
            s.setBlockNetworkLoads(false);
            s.setCacheMode(WebSettings.LOAD_DEFAULT);

            wv.setWebChromeClient(new WebChromeClient());
            wv.setWebViewClient(new WebViewClient() {
                @Override
                public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
                    String url = req.getUrl().toString();
                    if (url.startsWith("data:") || url.startsWith("blob:") ||
                        url.startsWith("about:") || url.startsWith("javascript:") ||
                        url.startsWith("file:")) {
                        return null;
                    }
                    return intercept(url);
                }
            });
            wv.loadUrl("about:blank");

            LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
            wv.setLayoutParams(wlp);
            root.addView(wv);
            log("WebView OK");
        } catch (Exception e) {
            Log.e("FreshV2", "WebView init fail", e);
            log("WV ERROR: " + e.getMessage());
        }

        setContentView(root);
    }

    private View buildUI() {
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setBackgroundColor(Color.parseColor("#13131f"));
        p.setPadding(dp(10), dp(10), dp(10), dp(8));

        // Title
        TextView title = new TextView(this);
        title.setText("Fresh V2 — Interceptor");
        title.setTextColor(Color.parseColor("#4ade80"));
        title.setTextSize(13);
        p.addView(title);

        // URL + GO
        LinearLayout r1 = new LinearLayout(this);
        r1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = dp(6);
        r1.setLayoutParams(rlp);

        urlInput = new EditText(this);
        urlInput.setText("https://public.pg-demo.com/demo/?gi=65&lang=en");
        urlInput.setTextColor(Color.WHITE);
        urlInput.setTextSize(11);
        urlInput.setBackgroundColor(Color.parseColor("#0a0a0f"));
        urlInput.setPadding(dp(8), dp(8), dp(8), dp(8));
        urlInput.setSingleLine(true);
        r1.addView(urlInput, new LinearLayout.LayoutParams(0,
            LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        btnGo = new Button(this);
        btnGo.setText("GO");
        btnGo.setTextSize(11);
        btnGo.setTextColor(Color.parseColor("#0a0a0f"));
        btnGo.setBackgroundColor(Color.parseColor("#4ade80"));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        blp.leftMargin = dp(6);
        btnGo.setLayoutParams(blp);
        btnGo.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openWeb(); }
        });
        r1.addView(btnGo);
        p.addView(r1);

        // Stats
        statText = new TextView(this);
        statText.setTextColor(Color.parseColor("#4ade80"));
        statText.setTextSize(11);
        statText.setTypeface(Typeface.MONOSPACE);
        statText.setText("0 files  0.00 MB");
        LinearLayout.LayoutParams lp2 = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp2.topMargin = dp(6);
        statText.setLayoutParams(lp2);
        p.addView(statText);

        typeText = new TextView(this);
        typeText.setTextColor(Color.parseColor("#60a5fa"));
        typeText.setTextSize(10);
        typeText.setTypeface(Typeface.MONOSPACE);
        typeText.setText("types: -");
        p.addView(typeText);

        // Clear button
        btnClear = new Button(this);
        btnClear.setText("CLEAR");
        btnClear.setTextSize(10);
        btnClear.setTextColor(Color.WHITE);
        btnClear.setBackgroundColor(Color.parseColor("#e74c3c"));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.topMargin = dp(6);
        btnClear.setLayoutParams(clp);
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { clearCapture(); }
        });
        p.addView(btnClear);

        // Log
        ScrollView sv = new ScrollView(this);
        LinearLayout.LayoutParams svlp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(140));
        svlp.topMargin = dp(6);
        sv.setLayoutParams(svlp);
        sv.setBackgroundColor(Color.parseColor("#0a0a0f"));

        logList = new LinearLayout(this);
        logList.setOrientation(LinearLayout.VERTICAL);
        logList.setPadding(dp(6), dp(6), dp(6), dp(6));
        sv.addView(logList);
        p.addView(sv);

        return p;
    }

    private void openWeb() {
        String url = urlInput.getText().toString().trim();
        if (url.isEmpty()) return;
        urlSeen.clear();
        fileCount = 0;
        totalBytes = 0;
        typeCount.clear();
        btnGo.setEnabled(false);
        log("> " + url);
        wv.loadUrl(url);
    }

    private WebResourceResponse intercept(String url) {
        HttpURLConnection conn = null;
        try {
            URL u = new URL(url);
            conn = (HttpURLConnection) u.openConnection();
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 11; Infinix X689) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36");
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Accept-Language", "en-US,en;q=0.9");

            String c = CookieManager.getInstance().getCookie(url);
            if (c != null && !c.isEmpty()) conn.setRequestProperty("Cookie", c);

            conn.setConnectTimeout(15000);
            conn.setReadTimeout(20000);
            conn.setInstanceFollowRedirects(true);

            int code = conn.getResponseCode();
            InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();
            if (is == null) return null;

            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
            is.close();
            byte[] data = bos.toByteArray();

            saveFile(url, data);

            String ctype = conn.getContentType();
            if (ctype == null) ctype = "application/octet-stream";
            String cenc = conn.getContentEncoding();

            Map<String, String> headers = new HashMap<>();
            headers.put("Access-Control-Allow-Origin", "*");

            return new WebResourceResponse(ctype, cenc, code, "OK", headers, new ByteArrayInputStream(data));
        } catch (Exception e) {
            Log.e("FreshV2", "intercept fail: " + e.getMessage());
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private void saveFile(String url, byte[] data) {
        try {
            URL u = new URL(url);
            String host = safe(u.getHost());
            String path = u.getPath();
            if (path == null || path.isEmpty() || path.endsWith("/")) path = "/index.html";
            String ext = "";
            int dot = path.lastIndexOf('.');
            if (dot >= 0) {
                ext = path.substring(dot + 1).toLowerCase();
                if (ext.length() > 6) ext = ext.substring(0, 6);
            }
            if (ext.isEmpty()) ext = "html";

            String type = classify(ext);
            String pathSafe = path.replaceAll("[?].*$", "");
            pathSafe = pathSafe.replaceAll("[^a-zA-Z0-9._/-]", "_");
            if (pathSafe.length() > 180) pathSafe = pathSafe.substring(pathSafe.length() - 180);
            if (pathSafe.startsWith("/")) pathSafe = pathSafe.substring(1);

            File out = new File(captureDir, host + "/" + type + "/" + pathSafe);
            File parent = out.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();

            FileOutputStream fos = new FileOutputStream(out);
            fos.write(data);
            fos.close();

            fileCount++;
            totalBytes += data.length;
            Integer c = typeCount.get(type);
            typeCount.put(type, c == null ? 1 : c + 1);

            final int fc = fileCount;
            final long tb = totalBytes;
            final String typeF = type;
            final String pathF = pathSafe;
            final int sz = data.length;

            ui.post(new Runnable() {
                @Override public void run() {
                    statText.setText(fc + " files  " + String.format("%.2f", tb / 1024.0 / 1024.0) + " MB");
                    StringBuilder sb = new StringBuilder("types: ");
                    for (Map.Entry<String, Integer> e : typeCount.entrySet()) {
                        sb.append(e.getKey()).append("=").append(e.getValue()).append(" ");
                    }
                    typeText.setText(sb.toString());
                    addLog("[" + fc + "] " + typeF + "/" + pathF + " (" + (sz / 1024) + "K)");
                }
            });
        } catch (Exception e) {
            Log.e("FreshV2", "save fail: " + e.getMessage());
        }
    }

    private String classify(String ext) {
        if (ext.equals("png") || ext.equals("jpg") || ext.equals("jpeg") || ext.equals("gif") ||
            ext.equals("webp") || ext.equals("svg") || ext.equals("ico")) return "img";
        if (ext.equals("mp3") || ext.equals("ogg") || ext.equals("wav") || ext.equals("m4a")) return "audio";
        if (ext.equals("mp4") || ext.equals("webm")) return "video";
        if (ext.equals("ttf") || ext.equals("otf") || ext.equals("woff") || ext.equals("woff2")) return "font";
        if (ext.equals("js") || ext.equals("mjs")) return "js";
        if (ext.equals("css")) return "css";
        if (ext.equals("json")) return "json";
        if (ext.equals("html") || ext.equals("htm")) return "html";
        if (ext.equals("wasm")) return "wasm";
        return "other";
    }

    private String safe(String s) {
        if (s == null) return "unknown";
        return s.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
    }

    private void addLog(final String msg) {
        ui.post(new Runnable() {
            @Override public void run() {
                if (logList == null) return;
                TextView t = new TextView(MainActivity.this);
                t.setText(msg);
                t.setTextColor(Color.parseColor("#cccccc"));
                t.setTextSize(9);
                t.setTypeface(Typeface.MONOSPACE);
                logList.addView(t, 0);
                while (logList.getChildCount() > 150) logList.removeViewAt(logList.getChildCount() - 1);
            }
        });
    }

    private void log(String msg) {
        Log.i("FreshV2", msg);
        addLog(msg);
    }

    private void clearCapture() {
        File[] fs = captureDir.listFiles();
        if (fs != null) for (File f : fs) {
            if (f.isDirectory()) deleteRec(f); else f.delete();
        }
        urlSeen.clear();
        fileCount = 0;
        totalBytes = 0;
        typeCount.clear();
        statText.setText("0 files  0.00 MB");
        typeText.setText("types: -");
        logList.removeAllViews();
        btnGo.setEnabled(true);
        log("Cleared");
    }

    private void deleteRec(File f) {
        File[] kids = f.listFiles();
        if (kids != null) for (File c : kids) {
            if (c.isDirectory()) deleteRec(c); else c.delete();
        }
        f.delete();
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
