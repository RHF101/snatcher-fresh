package com.rhf.fresh;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0a0a0f"));
        root.setPadding(60, 60, 60, 60);

        TextView title = new TextView(this);
        title.setText("FRESH WORKS");
        title.setTextColor(Color.parseColor("#4ade80"));
        title.setTextSize(36);
        root.addView(title);

        TextView info = new TextView(this);
        info.setText("\n\nAndroid: " + Build.VERSION.RELEASE +
                     "\nSDK: " + Build.VERSION.SDK_INT +
                     "\nDevice: " + Build.MODEL +
                     "\nBrand: " + Build.BRAND);
        info.setTextColor(Color.WHITE);
        info.setTextSize(18);
        root.addView(info);

        setContentView(root);
        Log.i("Fresh", "onCreate OK");
    }
}
