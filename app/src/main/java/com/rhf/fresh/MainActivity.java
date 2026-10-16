package com.rhf.fresh;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
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
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MainActivity extends Activity {
    private WebView wv;
    private File captureDir, zipDir;
    private final Set<String> urlSeen = new HashSet<>();
    private final Set<String> hashSeen = new HashSet<>();
    private int fileCount = 0;
    private long totalBytes = 0;
    private final Map<String, Integer> typeCount = new HashMap<>();
    private final List<FileEntry> fileList = new ArrayList<>();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private String currentUrl = "";
    private boolean sourceMode = false;
    private String activeFilter = "all";

    private EditText urlInput;
    private Button btnGo, btnClear, btnSource, btnPreview, btnSave;
    private TextView statText, typeText;
    private LinearLayout logList, fileListContainer;
    private ProgressBar progressBar;
    private TextView progressText;
    private ScrollView logScroll, fileScroll;
    private HorizontalScrollView filterBar;

    static class FileEntry {
        String url, path, type;
        int size;
        FileEntry(String url, String path, String type, int size) {
            this.url = url; this.path = path; this.type = type; this.size = size;
        }
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        captureDir = new File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS), "SnatcherV3/captures");
        zipDir = new File(Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS), "SnatcherV3");
        if (!captureDir.exists()) captureDir.mkdirs();
        if (!zipDir.exists()) zipDir.mkdirs();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0a0a0f"));

        root.addView(buildUI());

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

        wv.setWebChromeClient(new WebChromeClient());
        wv.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
                String url = req.getUrl().toString();
                if (url.startsWith("data:") || url.startsWith("blob:") ||
                    url.startsWith("about:") || url.startsWith("javascript:") ||
                    url.startsWith("file:")) return null;
                return intercept(url);
            }
            @Override
            public void onPageFinished(WebView view, String url) {
                if (!url.startsWith("data:") && !url.startsWith("about:")) {
                    currentUrl = url;
                }
            }
        });
        wv.loadUrl("about:blank");

        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        wv.setLayoutParams(wlp);
        root.addView(wv);

        setContentView(root);
    }

    private View buildUI() {
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setBackgroundColor(Color.parseColor("#0d0d1a"));
        p.setPadding(dp(8), dp(8), dp(8), dp(6));

        // Header
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("Fresh V3.1");
        title.setTextColor(Color.parseColor("#4ade80"));
        title.setTextSize(13);
        title.setTypeface(null, Typeface.BOLD);
        header.addView(title);

        statText = new TextView(this);
        statText.setTextColor(Color.parseColor("#60a5fa"));
        statText.setTextSize(10);
        statText.setTypeface(Typeface.MONOSPACE);
        statText.setText("  0f  0.00MB");
        header.addView(statText);
        p.addView(header);

        // URL row
        LinearLayout r1 = new LinearLayout(this);
        r1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = dp(4);
        r1.setLayoutParams(rlp);

        urlInput = new EditText(this);
        urlInput.setText("https://public.pg-demo.com/demo/?gi=65&lang=en");
        urlInput.setTextColor(Color.WHITE);
        urlInput.setTextSize(10);
        urlInput.setBackgroundColor(Color.parseColor("#0a0a0f"));
        urlInput.setPadding(dp(6), dp(6), dp(6), dp(6));
        urlInput.setSingleLine(true);
        r1.addView(urlInput, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        btnGo = new Button(this);
        btnGo.setText("GO");
        btnGo.setTextSize(10);
        btnGo.setTextColor(Color.parseColor("#0a0a0f"));
        btnGo.setBackgroundColor(Color.parseColor("#4ade80"));
        btnGo.setPadding(dp(8), dp(6), dp(8), dp(6));
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        blp.leftMargin = dp(4);
        btnGo.setLayoutParams(blp);
        btnGo.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openWeb(); }
        });
        r1.addView(btnGo);
        p.addView(r1);

        // SAVE + MODE + CLEAR
        LinearLayout r2 = new LinearLayout(this);
        r2.setOrientation(LinearLayout.HORIZONTAL);
        rlp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = dp(4);
        r2.setLayoutParams(rlp);

        btnSave = new Button(this);
        btnSave.setText("💾 SAVE ZIP");
        btnSave.setTextSize(10);
        btnSave.setTextColor(Color.parseColor("#0a0a0f"));
        btnSave.setBackgroundColor(Color.parseColor("#4ade80"));
        btnSave.setPadding(dp(6), dp(5), dp(6), dp(5));
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { saveZip(); }
        });
        r2.addView(btnSave, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.5f));

        btnPreview = new Button(this);
        btnPreview.setText("👁");
        btnPreview.setTextSize(10);
        btnPreview.setTextColor(Color.parseColor("#0a0a0f"));
        btnPreview.setBackgroundColor(Color.parseColor("#4ade80"));
        btnPreview.setPadding(dp(6), dp(5), dp(6), dp(5));
        LinearLayout.LayoutParams blp2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        blp2.leftMargin = dp(4);
        btnPreview.setLayoutParams(blp2);
        btnPreview.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setMode(false); }
        });
        r2.addView(btnPreview);

        btnSource = new Button(this);
        btnSource.setText("<>");
        btnSource.setTextSize(10);
        btnSource.setTextColor(Color.parseColor("#4ade80"));
        btnSource.setBackgroundColor(Color.parseColor("#1a1a2e"));
        btnSource.setPadding(dp(6), dp(5), dp(6), dp(5));
        LinearLayout.LayoutParams blp3 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        blp3.leftMargin = dp(4);
        btnSource.setLayoutParams(blp3);
        btnSource.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { setMode(true); }
        });
        r2.addView(btnSource);

        btnClear = new Button(this);
        btnClear.setText("🗑");
        btnClear.setTextSize(10);
        btnClear.setTextColor(Color.WHITE);
        btnClear.setBackgroundColor(Color.parseColor("#e74c3c"));
        btnClear.setPadding(dp(8), dp(5), dp(8), dp(5));
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        clp.leftMargin = dp(4);
        btnClear.setLayoutParams(clp);
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { clearCapture(); }
        });
        r2.addView(btnClear);
        p.addView(r2);

        // Progress
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(100);
        progressBar.setProgress(0);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(6));
        plp.topMargin = dp(4);
        progressBar.setLayoutParams(plp);
        p.addView(progressBar);

        progressText = new TextView(this);
        progressText.setTextColor(Color.parseColor("#888"));
        progressText.setTextSize(9);
        progressText.setText("Idle");
        p.addView(progressText);

        // Filter
        filterBar = new HorizontalScrollView(this);
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        flp.topMargin = dp(4);
        filterBar.setLayoutParams(flp);
        filterBar.setHorizontalScrollBarEnabled(false);

        LinearLayout filterRow = new LinearLayout(this);
        filterRow.setOrientation(LinearLayout.HORIZONTAL);
        String[] filters = {"all", "img", "js", "css", "json", "audio", "video", "font", "html", "xml", "wasm", "other"};
        for (String f : filters) {
            Button fb = new Button(this);
            fb.setText(f);
            fb.setTextSize(9);
            fb.setTextColor(Color.parseColor("#888"));
            fb.setBackgroundColor(Color.parseColor("#1a1a2e"));
            fb.setPadding(dp(8), dp(2), dp(8), dp(2));
            final String filterName = f;
            fb.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    activeFilter = filterName;
                    renderFileList();
                }
            });
            LinearLayout.LayoutParams fblp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            fblp.rightMargin = dp(3);
            fb.setLayoutParams(fblp);
            filterRow.addView(fb);
        }
        filterBar.addView(filterRow);
        p.addView(filterBar);

        // File list / Log toggle
        LinearLayout tabRow = new LinearLayout(this);
        tabRow.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tlp.topMargin = dp(4);
        tabRow.setLayoutParams(tlp);

        Button tabFiles = new Button(this);
        tabFiles.setText("📁 Files");
        tabFiles.setTextSize(9);
        tabFiles.setTextColor(Color.parseColor("#4ade80"));
        tabFiles.setBackgroundColor(Color.parseColor("#2a1f4a"));
        tabFiles.setPadding(dp(6), dp(3), dp(6), dp(3));
        tabFiles.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                fileScroll.setVisibility(View.VISIBLE);
                logScroll.setVisibility(View.GONE);
            }
        });
        tabRow.addView(tabFiles, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        Button tabLog = new Button(this);
        tabLog.setText("📜 Log");
        tabLog.setTextSize(9);
        tabLog.setTextColor(Color.parseColor("#4ade80"));
        tabLog.setBackgroundColor(Color.parseColor("#1a1a2e"));
        tabLog.setPadding(dp(6), dp(3), dp(6), dp(3));
        tabLog.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                fileScroll.setVisibility(View.GONE);
                logScroll.setVisibility(View.VISIBLE);
            }
        });
        LinearLayout.LayoutParams lgp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        lgp.leftMargin = dp(3);
        tabLog.setLayoutParams(lgp);
        tabRow.addView(tabLog);
        p.addView(tabRow);

        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(160));
        slp.topMargin = dp(4);

        fileScroll = new ScrollView(this);
        fileScroll.setLayoutParams(slp);
        fileScroll.setBackgroundColor(Color.parseColor("#0a0a0f"));
        fileListContainer = new LinearLayout(this);
        fileListContainer.setOrientation(LinearLayout.VERTICAL);
        fileListContainer.setPadding(dp(4), dp(4), dp(4), dp(4));
        fileScroll.addView(fileListContainer);
        p.addView(fileScroll);

        logScroll = new ScrollView(this);
        logScroll.setLayoutParams(slp);
        logScroll.setBackgroundColor(Color.parseColor("#0a0a0f"));
        logScroll.setVisibility(View.GONE);
        logList = new LinearLayout(this);
        logList.setOrientation(LinearLayout.VERTICAL);
        logList.setPadding(dp(4), dp(4), dp(4), dp(4));
        logScroll.addView(logList);
        p.addView(logScroll);

        typeText = new TextView(this);
        typeText.setTextColor(Color.parseColor("#60a5fa"));
        typeText.setTextSize(9);
        typeText.setTypeface(Typeface.MONOSPACE);
        typeText.setText("types: -");
        typeText.setPadding(0, dp(2), 0, 0);
        p.addView(typeText);

        return p;
    }

    private void setMode(boolean source) {
        sourceMode = source;
        if (source) {
            btnSource.setBackgroundColor(Color.parseColor("#4ade80"));
            btnSource.setTextColor(Color.parseColor("#0a0a0f"));
            btnPreview.setBackgroundColor(Color.parseColor("#1a1a2e"));
            btnPreview.setTextColor(Color.parseColor("#4ade80"));
            log("Mode: Source");
        } else {
            btnPreview.setBackgroundColor(Color.parseColor("#4ade80"));
            btnPreview.setTextColor(Color.parseColor("#0a0a0f"));
            btnSource.setBackgroundColor(Color.parseColor("#1a1a2e"));
            btnSource.setTextColor(Color.parseColor("#4ade80"));
            log("Mode: Preview");
        }
        if (!currentUrl.isEmpty()) wv.loadUrl(currentUrl);
    }

    private void openWeb() {
        String url = urlInput.getText().toString().trim();
        if (url.isEmpty()) return;
        urlSeen.clear();
        hashSeen.clear();
        fileCount = 0;
        totalBytes = 0;
        typeCount.clear();
        fileList.clear();
        fileListContainer.removeAllViews();
        logList.removeAllViews();
        btnGo.setEnabled(false);
        progressBar.setProgress(5);
        progressText.setText("Loading...");
        log("> " + url);
        currentUrl = url;
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

            if (code >= 200 && code < 300) saveFile(url, data);

            String ctype = conn.getContentType();
            if (ctype == null) ctype = "application/octet-stream";
            String cenc = conn.getContentEncoding();

            Map<String, String> headers = new HashMap<>();
            headers.put("Access-Control-Allow-Origin", "*");

            return new WebResourceResponse(ctype, cenc, code, "OK", headers, new ByteArrayInputStream(data));
        } catch (Exception e) {
            Log.e("FreshV3", "intercept fail: " + e.getMessage());
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

            int dot = path.lastIndexOf('.');
            String ext = "";
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

            String hash = sha1(data);
            if (!hashSeen.add(hash)) return;

            FileOutputStream fos = new FileOutputStream(out);
            fos.write(data);
            fos.close();

            fileCount++;
            totalBytes += data.length;
            Integer c = typeCount.get(type);
            typeCount.put(type, c == null ? 1 : c + 1);
            fileList.add(new FileEntry(url, pathSafe, type, data.length));

            ui.post(new Runnable() {
                @Override public void run() {
                    updateStats();
                    renderFileList();
                }
            });

            if (type.equals("html") || type.equals("js") || type.equals("css") || type.equals("json") || type.equals("xml")) {
                try {
                    String content = new String(data, "UTF-8");
                    Set<String> more = extract(url, content, type);
                    for (String m : more) {
                        if (!urlSeen.contains(m)) loadExtra(m, url);
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            Log.e("FreshV3", "save fail: " + e.getMessage());
        }
    }

    private void loadExtra(final String url, final String referer) {
        if (urlSeen.contains(url)) return;
        urlSeen.add(url);
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    URL u = new URL(url);
                    HttpURLConnection conn = (HttpURLConnection) u.openConnection();
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 11)");
                    conn.setRequestProperty("Referer", referer);
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(15000);
                    int code = conn.getResponseCode();
                    if (code >= 200 && code < 300) {
                        InputStream is = conn.getInputStream();
                        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
                        byte[] buf = new byte[8192];
                        int n;
                        while ((n = is.read(buf)) > 0) bos.write(buf, 0, n);
                        is.close();
                        byte[] data = bos.toByteArray();
                        if (data.length > 0 && data.length < 10 * 1024 * 1024) saveFile(url, data);
                    }
                    conn.disconnect();
                } catch (Exception ignored) {}
            }
        }).start();
    }

    private Set<String> extract(String base, String content, String type) {
        Set<String> found = new HashSet<>();
        try {
            Pattern p1 = Pattern.compile("https?://[^\\s\"'<>`\\)\\]}\\u002C]+", Pattern.CASE_INSENSITIVE);
            Matcher m = p1.matcher(content);
            while (m.find()) found.add(clean(m.group()));

            Pattern p2 = Pattern.compile("(?:src|href|data-src|poster|action)\\s*=\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
            m = p2.matcher(content);
            while (m.find()) {
                String r = resolve(base, m.group(1));
                if (r != null) found.add(r);
            }

            if (type.equals("css")) {
                Pattern p3 = Pattern.compile("url\\(\\s*[\"']?([^\"')]+)[\"']?\\s*\\)", Pattern.CASE_INSENSITIVE);
                m = p3.matcher(content);
                while (m.find()) {
                    String raw = m.group(1);
                    if (raw.startsWith("data:")) continue;
                    String r = resolve(base, raw);
                    if (r != null) found.add(r);
                }
            }

            if (type.equals("js")) {
                Pattern p4 = Pattern.compile("(?:import|require|fetch)\\s*\\(\\s*[\"']([^\"']+)[\"']", Pattern.CASE_INSENSITIVE);
                m = p4.matcher(content);
                while (m.find()) {
                    String r = resolve(base, m.group(1));
                    if (r != null) found.add(r);
                }
            }
        } catch (Exception ignored) {}
        return found;
    }

    private String resolve(String base, String raw) {
        try {
            if (raw == null || raw.isEmpty()) return null;
            raw = raw.trim();
            if (raw.startsWith("data:") || raw.startsWith("blob:") || raw.startsWith("#")) return null;
            java.net.URI baseUri = java.net.URI.create(base);
            java.net.URI r = baseUri.resolve(raw);
            String s = r.toString();
            if (!s.startsWith("http")) return null;
            return clean(s);
        } catch (Exception e) { return null; }
    }

    private String clean(String u) {
        if (u == null) return null;
        while (!u.isEmpty() && ".,;:!?)\\]}'\"".indexOf(u.charAt(u.length() - 1)) >= 0) {
            u = u.substring(0, u.length() - 1);
        }
        return u;
    }

    private String classify(String ext) {
        if (ext.equals("png") || ext.equals("jpg") || ext.equals("jpeg") || ext.equals("gif") ||
            ext.equals("webp") || ext.equals("svg") || ext.equals("ico") || ext.equals("bmp")) return "img";
        if (ext.equals("mp3") || ext.equals("ogg") || ext.equals("wav") || ext.equals("m4a") || ext.equals("aac")) return "audio";
        if (ext.equals("mp4") || ext.equals("webm") || ext.equals("mkv") || ext.equals("mov")) return "video";
        if (ext.equals("ttf") || ext.equals("otf") || ext.equals("woff") || ext.equals("woff2")) return "font";
        if (ext.equals("js") || ext.equals("mjs")) return "js";
        if (ext.equals("css")) return "css";
        if (ext.equals("json")) return "json";
        if (ext.equals("xml") || ext.equals("plist") || ext.equals("atlas")) return "xml";
        if (ext.equals("html") || ext.equals("htm")) return "html";
        if (ext.equals("wasm")) return "wasm";
        return "other";
    }

    private String safe(String s) {
        if (s == null) return "unknown";
        return s.replaceAll("[^a-zA-Z0-9._-]", "_").toLowerCase();
    }

    private String sha1(byte[] data) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
            byte[] h = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(data.length);
        }
    }

    private void updateStats() {
        statText.setText("  " + fileCount + "f  " + String.format("%.2f", totalBytes / 1024.0 / 1024.0) + "MB");
        int progress = Math.min(95, (int)(Math.log(fileCount + 1) * 25));
        progressBar.setProgress(progress);
        progressText.setText(fileCount + " files  " + (totalBytes / 1024) + " KB");

        StringBuilder sb = new StringBuilder("types: ");
        for (Map.Entry<String, Integer> e : typeCount.entrySet()) {
            sb.append(e.getKey()).append("=").append(e.getValue()).append(" ");
        }
        typeText.setText(sb.toString());
    }

    private void renderFileList() {
        fileListContainer.removeAllViews();
        List<FileEntry> filtered = new ArrayList<>();
        for (FileEntry e : fileList) {
            if (activeFilter.equals("all") || e.type.equals(activeFilter)) filtered.add(e);
        }
        Collections.sort(filtered, (a, b) -> Integer.compare(b.size, a.size));

        int count = 0;
        for (FileEntry e : filtered) {
            if (count++ > 200) break;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundColor(Color.parseColor("#13131f"));
            row.setPadding(dp(4), dp(3), dp(4), dp(3));

            TextView ext = new TextView(this);
            ext.setText(e.type.toUpperCase());
            ext.setTextColor(Color.parseColor("#4ade80"));
            ext.setTextSize(8);
            ext.setTypeface(null, Typeface.BOLD);
            ext.setPadding(dp(3), dp(1), dp(3), dp(1));
            ext.setBackgroundColor(Color.parseColor("#2a1f4a"));
            ext.setMinWidth(dp(36));
            ext.setGravity(Gravity.CENTER);
            row.addView(ext);

            TextView name = new TextView(this);
            String shortName = e.path.length() > 45 ? "..." + e.path.substring(e.path.length() - 42) : e.path;
            name.setText(" " + shortName);
            name.setTextColor(Color.parseColor("#cccccc"));
            name.setTextSize(9);
            name.setTypeface(Typeface.MONOSPACE);
            name.setSingleLine(true);
            row.addView(name, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

            TextView sz = new TextView(this);
            String sizeStr = e.size > 1024 * 1024 ? String.format("%.1fM", e.size / 1024.0 / 1024.0) : (e.size / 1024) + "K";
            sz.setText(sizeStr);
            sz.setTextColor(Color.parseColor("#4ade80"));
            sz.setTextSize(9);
            row.addView(sz);

            LinearLayout.LayoutParams rlp2 = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            rlp2.bottomMargin = dp(2);
            row.setLayoutParams(rlp2);
            fileListContainer.addView(row);
        }
    }

    private void addLog(final String msg) {
        ui.post(new Runnable() {
            @Override public void run() {
                if (logList == null) return;
                String time = new SimpleDateFormat("HH:mm:ss", Locale.US).format(new Date());
                TextView t = new TextView(MainActivity.this);
                t.setText("[" + time + "] " + msg);
                t.setTextColor(Color.parseColor("#cccccc"));
                t.setTextSize(9);
                t.setTypeface(Typeface.MONOSPACE);
                logList.addView(t, 0);
                while (logList.getChildCount() > 150) logList.removeViewAt(logList.getChildCount() - 1);
            }
        });
    }

    private void log(String msg) {
        Log.i("FreshV3", msg);
        addLog(msg);
    }

    private void saveZip() {
        if (fileCount == 0) {
            Toast.makeText(this, "Belum ada file ke-capture", Toast.LENGTH_SHORT).show();
            return;
        }
        log("Building ZIP...");
        progressBar.setProgress(100);
        try {
            String name = "snatch-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date()) + ".zip";
            File zipFile = new File(zipDir, name);
            ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile));
            File[] dirs = captureDir.listFiles();
            if (dirs != null) {
                for (File d : dirs) {
                    if (d.isDirectory()) addToZip(zos, d, "");
                }
            }
            zos.close();

            final String path = zipFile.getAbsolutePath();
            final long size = zipFile.length();
            log("💾 " + name + " (" + (size / 1024) + " KB)");
            Toast.makeText(this, "ZIP: " + name + "\nDi: Download/SnatcherV3/\nSize: " + (size / 1024) + " KB", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Log.e("FreshV3", "zip fail", e);
            Toast.makeText(this, "ZIP error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void addToZip(ZipOutputStream zos, File f, String base) throws Exception {
        if (f.isDirectory()) {
            zos.putNextEntry(new ZipEntry(base + f.getName() + "/"));
            zos.closeEntry();
            File[] kids = f.listFiles();
            if (kids != null) for (File c : kids) addToZip(zos, c, base + f.getName() + "/");
        } else {
            zos.putNextEntry(new ZipEntry(base + f.getName()));
            FileInputStream fis = new FileInputStream(f);
            byte[] buf = new byte[8192];
            int n;
            while ((n = fis.read(buf)) > 0) zos.write(buf, 0, n);
            fis.close();
            zos.closeEntry();
        }
    }

    private void clearCapture() {
        File[] fs = captureDir.listFiles();
        if (fs != null) for (File f : fs) {
            if (f.isDirectory()) deleteRec(f); else f.delete();
        }
        urlSeen.clear();
        hashSeen.clear();
        fileCount = 0;
        totalBytes = 0;
        typeCount.clear();
        fileList.clear();
        fileListContainer.removeAllViews();
        logList.removeAllViews();
        statText.setText("  0f  0.00MB");
        typeText.setText("types: -");
        progressBar.setProgress(0);
        progressText.setText("Idle");
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
