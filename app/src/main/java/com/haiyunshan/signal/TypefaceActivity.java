package com.haiyunshan.signal;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.FrameLayout;

import com.haiyunshan.signal.font.StorageTypefaceFragment;

public class TypefaceActivity extends AppCompatActivity {

    FrameLayout mRootLayout;

    public static void start(Activity context) {
        Intent intent = new Intent(context, TypefaceActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_typeface);

        this.mRootLayout = findViewById(R.id.root_layout);

        FragmentManager fm = this.getSupportFragmentManager();
        FragmentTransaction t = fm.beginTransaction();

        StorageTypefaceFragment f = StorageTypefaceFragment.newInstance(null);

        t.replace(mRootLayout.getId(), f);
        t.commit();
    }
}
