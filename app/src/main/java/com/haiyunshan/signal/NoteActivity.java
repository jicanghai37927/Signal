package com.haiyunshan.signal;

import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.FrameLayout;

import com.haiyunshan.signal.note.NoteFragment;

public class NoteActivity extends AppCompatActivity {

    FrameLayout mRootLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        this.mRootLayout = findViewById(R.id.root_layout);

        FragmentManager fm = this.getSupportFragmentManager();
        FragmentTransaction t = fm.beginTransaction();

        NoteFragment f = NoteFragment.newInstance(null);

        t.replace(mRootLayout.getId(), f);
        t.commit();
    }

}
