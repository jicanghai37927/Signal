package com.haiyunshan.signal;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.FrameLayout;

import com.haiyunshan.signal.compose.ComposeFragment;

public class ComposeActivity extends AppCompatActivity {

    public static final String ACTION_NOTE      = "note";
    public static final String ACTION_CAMERA    = "camera";
    public static final String ACTION_PHOTO     = "photo";

    FrameLayout mRootLayout;

    public static final void startForResult(Fragment fragment, int requestCode, String id, String action) {
        Activity context = fragment.getActivity();
        Intent intent = new Intent(context, ComposeActivity.class);
        intent.putExtra("id", id);
        intent.putExtra("action", action);

        fragment.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compose);

        this.mRootLayout = findViewById(R.id.root_layout);

        FragmentManager fm = this.getSupportFragmentManager();
        FragmentTransaction t = fm.beginTransaction();

        ComposeFragment f = ComposeFragment.newInstance(getIntent().getExtras());

        t.replace(mRootLayout.getId(), f);
        t.commit();
    }

    @Override
    public void onBackPressed() {
        Intent intent = this.getIntent();
        String id = (intent == null)? null: intent.getStringExtra("id");
        if (TextUtils.isEmpty(id)) {
            this.setResult(RESULT_CANCELED, null);
        } else {
            intent = new Intent();
            intent.putExtra("id", id);
            this.setResult(RESULT_OK, intent);
        }

        super.onBackPressed();
    }
}
