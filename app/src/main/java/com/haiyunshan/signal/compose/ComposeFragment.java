package com.haiyunshan.signal.compose;

import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.haiyunshan.signal.ComposeActivity;
import com.haiyunshan.signal.FtpServerActivity;
import com.haiyunshan.signal.R;
import com.haiyunshan.signal.browse.ChapterDialogFragment;
import com.haiyunshan.signal.compose.adapter.BaseHolder;
import com.haiyunshan.signal.compose.adapter.DocumentAdapter;
import com.haiyunshan.signal.compose.document.BaseItem;
import com.haiyunshan.signal.compose.document.Document;
import com.haiyunshan.signal.compose.document.ParagraphItem;
import com.haiyunshan.signal.compose.document.PictureItem;
import com.haiyunshan.signal.compose.export.ExportDialogFragment;
import com.haiyunshan.signal.compose.export.ExportFactory;
import com.haiyunshan.signal.compose.export.ExportHelper;
import com.haiyunshan.signal.compose.note.Note;
import com.haiyunshan.signal.compose.widget.ComposeRecyclerView;
import com.haiyunshan.signal.note.dataset.NoteEntry;
import com.haiyunshan.signal.note.dataset.NoteManager;
import com.haiyunshan.signal.utils.FileHelper;
import com.haiyunshan.signal.utils.SoftInputUtils;
import com.haiyunshan.signal.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

import static android.app.Activity.RESULT_OK;

/**
 *
 */
public class ComposeFragment extends Fragment implements Toolbar.OnMenuItemClickListener, ComposeRecyclerView.OnNestedScrollListener {

    static final int REQUEST_PHOTO  = 1001;
    static final int REQUEST_CAMERA = 1002;

    ComposeRecyclerView mRecyclerView;
    DocumentAdapter mAdapter;

    Toolbar mToolbar;

    Document mDocument;
    Handler mHandler;

    Uri mPictureUri;

    public static final ComposeFragment newInstance(Bundle args) {
        ComposeFragment f = new ComposeFragment();

        if (args != null) {
            f.setArguments(args);
        }

        return f;
    }

    public ComposeFragment() {
        this.mHandler = new Handler();

        this.mPictureUri = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_compose, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.mRecyclerView = view.findViewById(R.id.recycler_list_view);

        this.mToolbar = view.findViewById(R.id.toolbar);
        mToolbar.inflateMenu(R.menu.menu_compose);
        mToolbar.setOnMenuItemClickListener(this);
        mToolbar.setNavigationIcon(R.drawable.ic_material_arrow_left_dark);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SoftInputUtils.hide(getActivity());

                getActivity().onBackPressed();
            }
        });
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        String action = ComposeActivity.ACTION_NOTE;

        {
            Bundle args = this.getArguments();
            String id = (args == null)? "signal": args.getString("id", "signal");
            Note note = Note.create(id);
            this.mDocument = new Document(note);

            if (args != null) {
                action = args.getString("action", action);
            }
        }

        {
            mRecyclerView.setOnNestedScrollListener(this);

            LinearLayoutManager layout = new LinearLayoutManager(getActivity());
            mRecyclerView.setLayoutManager(layout);

            this.mAdapter = new DocumentAdapter(this);
            mRecyclerView.setAdapter(mAdapter);
        }

        if (action.equalsIgnoreCase(ComposeActivity.ACTION_CAMERA)) {
            this.takePhoto();
        } else if (action.equalsIgnoreCase(ComposeActivity.ACTION_PHOTO)) {
            this.selectPhoto();
        } else {
            if (mDocument.isEmpty()) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        SoftInputUtils.show(getActivity(), getActivity().getCurrentFocus());
                    }
                }, 200);
            }
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CAMERA) {
            if (resultCode == RESULT_OK) {
                if (mPictureUri != null) {
                    String path = Utils.getRealPathFromURI(getActivity(), mPictureUri);
                    if (!TextUtils.isEmpty(path)) {
                        this.insertPictures(new String[] { path });
                    }

                    mPictureUri = null;
                }
            } else {
                if (mPictureUri != null) {
                    Utils.deleteImageUri(getActivity(), mPictureUri);

                    mPictureUri = null;
                }
            }

        } else if (requestCode == REQUEST_PHOTO) {
            if (resultCode == RESULT_OK) {
                if (data != null) {

                    ArrayList<Uri> list = new ArrayList<>();

                    {
                        ClipData clipData = data.getClipData();
                        if (clipData != null) {
                            for (int i = 0; i < clipData.getItemCount(); i++) {
                                ClipData.Item item = clipData.getItemAt(i);
                                Uri uri = item.getUri();
                                list.add(uri);
                            }
                        }

                        if (list.isEmpty()) {
                            Uri uri = data.getData();
                            if (uri != null) {
                                list.add(uri);
                            }
                        }
                    }

                    {
                        String[] array = new String[list.size()];
                        int index = 0;

                        for (Uri uri : list) {
                            String uriString = uri.toString();
                            String path;

                            if (uriString.contains("content")) {
                                path = Utils.getRealPathFromURI(getActivity(), uri);
                            } else {
                                path = uriString.replace("file://", "");
                            }

                            path = (path == null) ? uriString : path;
                            array[index++] = path;
                        }

                        this.insertPictures(array);
                    }
                }
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onPause() {

        {
            this.saveDocument();
            this.updateEntry();
        }

        super.onPause();
    }

    @Override
    public void onStop() {

        {
            mDocument.save();
        }

        super.onStop();
    }

    void updateEntry() {
        NoteEntry entry;

        {
            String id = mDocument.getId();
            NoteManager mgr = NoteManager.instance();
            entry = mgr.obtain(id);
            if (entry == null) {
                entry = mgr.put(id);
            }
        }

        {
            entry.setCreated(mDocument.getCreated());
            entry.setModified(mDocument.getModified());
        }

        {
            String title = null;
            String subtitle = null;

            String[] array = mDocument.getTitle();
            title = array[0];
            subtitle = array[1];

            entry.setTitle(title);
            entry.setSubtitle(subtitle);
        }

        NoteManager.instance().save();
    }

    public Document getDocument() {
        return this.mDocument;
    }

    public RecyclerView getRecyclerView() {
        return this.mRecyclerView;
    }

    public DocumentAdapter getAdapter() {
        return this.mAdapter;
    }

    @Override
    public boolean onNestedPreFling(@NonNull View target, float velocityX, float velocityY) {
        if (velocityY < 0 && Math.abs(velocityY) >= 3700) {
            long duration = 5 * 60 * 1000; // XX分钟

            long time = mDocument.getSavedTime();
            long current = System.currentTimeMillis();
            long ellapse = current - time;
            if (ellapse > duration) {
                this.saveNote();
            }
        }

        return false;
    }

    @Override
    public boolean onNestedFling(@NonNull View target, float velocityX, float velocityY, boolean consumed) {
        if (velocityY < 0 && Math.abs(velocityY) >= 3700) {
            SoftInputUtils.hide(getActivity());
        }

        return false;
    }

    void saveDocument() {
        int count = mRecyclerView.getChildCount();
        for (int i = 0; i < count; i++) {
            View child = mRecyclerView.getChildAt(i);
            RecyclerView.ViewHolder h = mRecyclerView.getChildViewHolder(child);
            BaseHolder holder = (BaseHolder)h;
            holder.onSave();
        }
    }

    void saveNote() {
        this.saveDocument();

        mDocument.save();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        int itemId = item.getItemId();
        switch (itemId) {
            case R.id.menu_take_photo: {
                this.takePhoto();
                break;
            }
            case R.id.menu_picture: {
                this.selectPhoto();
                break;
            }
            case R.id.menu_export: {
                this.showExportDialog();

//                this.export();
                break;
            }
        }

        return true;
    }

    void showExportDialog() {

        FragmentManager fm = this.getChildFragmentManager();
        ExportDialogFragment f = new ExportDialogFragment();
        f.show(fm, "export");

    }

    public void export(int type) {

        ExportHelper helper = ExportFactory.create(getActivity(), mDocument, type);

        ExportTask task = new ExportTask(helper);
        Observable.create(task)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<ExportHelper>() {
                    @Override
                    public void accept(ExportHelper helper) {
                        onExportComplete(helper);
                    }
                });

    }

    void onExportComplete(final ExportHelper helper) {
        File file = helper.getTarget();

        String[] array = FileHelper.getPrettyPath(getActivity(), file);
        StringBuilder sb = new StringBuilder(128);
        sb.append(getString(R.string.export_msg_prefix));
        for (String str : array) {
            sb.append(str);
        }

        Snackbar bar = Snackbar.make(mRecyclerView, sb, Snackbar.LENGTH_LONG);
        bar.setAction(R.string.btn_ftp, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startFtp(helper);
            }
        });
        bar.setActionTextColor(getResources().getColor(R.color.primary_color));

        bar.show();
    }

    void startFtp(ExportHelper helper) {
        String homeDir = helper.getTarget().getParentFile().getAbsolutePath();

        FtpServerActivity.start(this, homeDir);
    }

    public boolean selectPhoto() {
        SoftInputUtils.hide(getActivity());

        Intent intent = new Intent(Intent.ACTION_PICK, null);
        intent.setDataAndType(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                "image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

        try {
            this.startActivityForResult(intent, REQUEST_PHOTO);

            return true;
        } catch (Exception e) {

        }

        return false;
    }

    void insertPictures(String[] array) {
        View focus = this.getActivity().getCurrentFocus();
        if (focus == null) {
            this.addPictures(array);
            return;
        }

        RecyclerView.ViewHolder h = mRecyclerView.findContainingViewHolder(focus);
        if (h == null) {
            this.addPictures(array);
            return;
        }

        BaseHolder holder = (BaseHolder)h;
        holder.insertPicture(array);

        // 保存一次
        this.saveNote();
    }

    void addPictures(String[] array) {

        ArrayList<BaseItem> list = new ArrayList<>(array.length * 2 + 1);

        // 创建新对象
        {
            int length = array.length;
            for (int i = 0; i < length; i++) {
                String path = array[i];

                {
                    File file = new File(path);

                    PictureItem p = PictureItem.create(mDocument, file);
                    list.add(p);
                }

                {
                    ParagraphItem p = ParagraphItem.create(mDocument, "");
                    list.add(p);
                }
            }

        }


        // 更新Document
        {
            for (BaseItem item : list) {
                mDocument.add(item);
            }
        }

        // 更新Adapter
        {
            int position = mAdapter.getItemCount();

            for (BaseItem p : list) {
                mAdapter.add(p);
            }

            int count = list.size();

            mAdapter.notifyItemChanged(position);
            mAdapter.notifyItemRangeInserted(position, count);

            mRecyclerView.scrollToPosition(position);
        }

        // 保存一次
        this.saveNote();
    }

    void takePhoto() {
        SoftInputUtils.hide(getActivity());

        Uri imageUri = Utils.createImageUri(getActivity());

        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);//如果不设置EXTRA_OUTPUT getData()  获取的是bitmap数据  是压缩后的

        this.startActivityForResult(intent, REQUEST_CAMERA);

        this.mPictureUri = imageUri;
    }

    private class ExportTask implements ObservableOnSubscribe<ExportHelper> {

        ExportHelper mHelper;

        ExportTask(ExportHelper helper) {
            this.mHelper = helper;
        }

        @Override
        public void subscribe(ObservableEmitter<ExportHelper> emitter) throws Exception {
            mHelper.export();

            emitter.onNext(mHelper);
            emitter.onComplete();
        }
    }
}
