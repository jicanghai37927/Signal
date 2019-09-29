package com.haiyunshan.signal.test;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.haiyunshan.signal.R;

public class TestDialogFragmentActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_dialog_fragment);

        findViewById(R.id.btn_test_dialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testDialog();
            }
        });
    }

    void testDialog() {

    }
}
