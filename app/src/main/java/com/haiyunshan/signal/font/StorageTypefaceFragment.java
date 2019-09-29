package com.haiyunshan.signal.font;


import android.Manifest;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.util.SortedList;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.util.SortedListAdapterCallback;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.haiyunshan.signal.R;
import com.haiyunshan.signal.TypefacePreviewActivity;
import com.haiyunshan.signal.divider.LeadingMarginDividerItemDecoration;
import com.haiyunshan.signal.font.dataset.FontDataset;
import com.haiyunshan.signal.font.dataset.FontEntry;
import com.haiyunshan.signal.font.dataset.FontManager;
import com.haiyunshan.signal.font.dataset.StorageFontManager;
import com.haiyunshan.signal.font.dataset.StorageFontScanner;
import com.haiyunshan.signal.utils.PackageUtils;
import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;
import java.text.Collator;
import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * A simple {@link Fragment} subclass.
 */
public class StorageTypefaceFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, FontManager.InstallObserver {

    static final int REQUEST_PREVIEW = 1001;

    static final int STEP_AUTO = 1;
    static final int STEP_USER = 2;

    RecyclerView mRecyclerView;
    SortedList<FontEntry> mSortedList;
    TypefaceAdapter mAdapter;

    Toolbar mToolbar;

    SwipeRefreshLayout mSwipeLayout;

    TextView mNumView;
    View mSearchLayout;
    TextView mSearchDirView;

    StorageFontScanner mScanner;

    public static final StorageTypefaceFragment newInstance(Bundle args) {
        StorageTypefaceFragment f = new StorageTypefaceFragment();

        if (args != null) {
            f.setArguments(args);
        }

        return f;
    }

    public StorageTypefaceFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_storage_typeface, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        {
            this.mRecyclerView = view.findViewById(R.id.recycler_list_view);
            LinearLayoutManager layout = new LinearLayoutManager(getActivity());
            mRecyclerView.setLayoutManager(layout);

            int margin = getResources().getDimensionPixelSize(R.dimen.list_margin_horizontal);
            LeadingMarginDividerItemDecoration decor = new LeadingMarginDividerItemDecoration(getActivity());
            decor.setDrawable(getResources().getDrawable(R.drawable.shape_list_divider, null));
            decor.setMargin(margin);

            mRecyclerView.addItemDecoration(decor);
        }

        {
            this.mToolbar = view.findViewById(R.id.toolbar);
            mToolbar.setNavigationIcon(R.drawable.ic_material_arrow_left_dark);
            mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().onBackPressed();
                }
            });

            this.mSwipeLayout = view.findViewById(R.id.swipe_refresh_layout);
            mSwipeLayout.setOnRefreshListener(this);
        }

        {
            this.mNumView = view.findViewById(R.id.tv_num);
            this.mSearchLayout = view.findViewById(R.id.search_layout);
            this.mSearchDirView = view.findViewById(R.id.tv_search_dir);
        }
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        {
            StorageFontManager.instance().validate();
            FontDataset ds = StorageFontManager.instance().getDataset();

            this.mAdapter = new TypefaceAdapter();
            TypefaceCallback callback = new TypefaceCallback(mAdapter);
            this.mSortedList = new SortedList<>(FontEntry.class, callback);

            mSortedList.addAll(ds.getList());

            mRecyclerView.setAdapter(mAdapter);
        }

        {
            int size = mSortedList.size();
            mNumView.setText(getString(R.string.font_num_fmt, size));

            this.mSearchLayout.setVisibility(View.INVISIBLE);
            this.mNumView.setVisibility(View.VISIBLE);
        }

        FontManager.instance().registerInstallObserver(this);

        // 申请权限
        this.requestScan(STEP_AUTO);
    }

    @Override
    public void onDestroy() {
        this.stopScan();

        FontManager.instance().unregisterInstallObserver(this);

        super.onDestroy();
    }

    private void requestScan(final int step) {
        if (step == STEP_AUTO) {

            if (PackageUtils.canRead()) {
                if (mSortedList.size() == 0) {
                    beginScan();
                }
            }

        } else {

            RxPermissions rxPermission = new RxPermissions(getActivity());

            rxPermission
                    .requestEach(Manifest.permission.READ_EXTERNAL_STORAGE)
                    .subscribe(new Consumer<Permission>() {

                        @Override
                        public void accept(Permission permission) throws Exception {
                            if (permission.granted) { // 用户已经同意该权限


                                beginScan();

                            } else if (permission.shouldShowRequestPermissionRationale) { // 用户拒绝了该权限，没有选中『不再询问』（Never ask again）,那么下次再次启动时，还会提示请求权限的对话框


                                DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (which == DialogInterface.BUTTON_POSITIVE) {
                                            requestScan(step);
                                        } else if (which == DialogInterface.BUTTON_NEGATIVE) {

                                        }
                                    }
                                };

                                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                builder.setMessage(R.string.font_scan_msg_1);
                                builder.setPositiveButton(R.string.btn_continue, listener);
                                builder.setNegativeButton(R.string.btn_cancel, listener);
                                AlertDialog dialog = builder.create();
                                dialog.setCanceledOnTouchOutside(false);
                                dialog.show();



                            } else { // 用户拒绝了该权限，并且选中『不再询问』

                                DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (which == DialogInterface.BUTTON_POSITIVE) {
                                            PackageUtils.showDetailsSettings(getActivity(), getActivity().getPackageName());
                                        } else if (which == DialogInterface.BUTTON_NEGATIVE) {

                                        }
                                    }
                                };

                                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                                builder.setMessage(R.string.font_scan_msg_2);
                                builder.setPositiveButton(R.string.btn_setting, listener);
                                builder.setNegativeButton(R.string.btn_cancel, listener);
                                AlertDialog dialog = builder.create();
                                dialog.setCanceledOnTouchOutside(false);
                                dialog.show();

                            }
                        }
                    });
        }
    }

    void beginScan() {

        int size = mSortedList.size();
        mNumView.setText(getString(R.string.font_num_fmt, size));

        this.mSearchLayout.setVisibility(View.VISIBLE);
        this.mNumView.setVisibility(View.INVISIBLE);

        StorageFontManager mgr = StorageFontManager.instance();
        this.mScanner = new StorageFontScanner(mgr);

        Observable.create(mScanner)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<File>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(File file) {
                        if (file.isDirectory()) {
                            mSearchDirView.setText(file.getAbsolutePath());
                        } else {
                            FontDataset ds = StorageFontManager.instance().getDataset();
                            FontEntry fontEntry = ds.obtainBySource(file.getAbsolutePath());
                            if (fontEntry != null) {

                                int index = mSortedList.indexOf(fontEntry);
                                if (index < 0) {
                                    mSortedList.add(fontEntry);
                                }

                                int size = mSortedList.size();
                                mNumView.setText(getString(R.string.font_num_fmt, size));
                            }
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                        mScanner = null;

                        mSearchLayout.setVisibility(View.INVISIBLE);
                        mNumView.setVisibility(View.VISIBLE);
                    }
                });
    }

    void stopScan() {
        if (mScanner != null) {
            mScanner.dispose();
            mScanner = null;
        }
    }

    @Override
    public void onRefresh() {
        if (mScanner == null) {
            this.stopScan();

            StorageFontManager.instance().validate();
            FontDataset ds = StorageFontManager.instance().getDataset();

            mSortedList.clear();
            mSortedList.addAll(ds.getList());

            this.requestScan(STEP_USER);
        }

        mSwipeLayout.post(new Runnable() {
            @Override
            public void run() {
                mSwipeLayout.setRefreshing(false);
            }
        });
    }

    void preview(FontEntry entry) {
        String source = entry.getSource();
        ArrayList<String> list = getList();

        TypefacePreviewActivity.startForResult(this, REQUEST_PREVIEW, source, list);
    }

    ArrayList<String> getList() {
        ArrayList<String> list = new ArrayList<>();

        int size = mSortedList.size();
        for (int i = 0; i < size; i++) {
            FontEntry entry = mSortedList.get(i);
            list.add(entry.getSource());
        }

        return list;
    }

    @Override
    public void onChanged(FontManager manager, FontEntry entry) {
        int pos = mSortedList.indexOf(entry);
        if (pos >= 0) {
            mAdapter.notifyItemChanged(pos);
        }
    }

    private class TypefaceCallback extends SortedListAdapterCallback<FontEntry> {

        Collator mCollator;

        /**
         *
         */
        public TypefaceCallback(RecyclerView.Adapter adapter) {
            super(adapter);

            this.mCollator = Collator.getInstance();
        }

        @Override
        public int compare(FontEntry o1, FontEntry o2) {
            String name1 = o1.getName();
            String name2 = o2.getName();

            return mCollator.compare(name1, name2);
        }

        @Override
        public boolean areContentsTheSame(FontEntry oldItem, FontEntry newItem) {
            boolean b1 = oldItem.getName().equals(newItem.getName());
            boolean b2 = oldItem.getSource().equals(newItem.getSource());

            return (b1 && b2);
        }

        @Override
        public boolean areItemsTheSame(FontEntry item1, FontEntry item2) {
            boolean b2 = item1.getSource().equals(item2.getSource());

            return b2;
        }
    }

    private class TypefaceAdapter extends RecyclerView.Adapter<TypefaceHolder> {

        @NonNull
        @Override
        public TypefaceHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            int resource = R.layout.layout_typeface_item;
            View view = getLayoutInflater().inflate(resource, parent, false);

            TypefaceHolder holder = new TypefaceHolder(view);
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull TypefaceHolder holder, int position) {
            FontEntry entry = mSortedList.get(position);
            holder.bind(position, entry);
        }

        @Override
        public int getItemCount() {
            return mSortedList.size();
        }
    }

    private class TypefaceHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView mNameView;
        TextView mActionView;

        FontEntry mEntry;

        public TypefaceHolder(View itemView) {
            super(itemView);

            this.mNameView = itemView.findViewById(R.id.tv_name);
            this.mActionView = itemView.findViewById(R.id.tv_action);
            mActionView.setOnClickListener(this);

            itemView.setOnClickListener(this);
        }

        void bind(int position, FontEntry entry) {
            this.mEntry = entry;

            Typeface tf = StorageFontManager.instance().getTypeface(entry);
            if (tf == null) {
                tf = Typeface.DEFAULT;
            }

            mNameView.setTypeface(tf);
            mNameView.setText(entry.getName());

            int state = FontManager.instance().getState(mEntry);
            if (state == FontManager.STATE_NONE) {
                mActionView.setText(R.string.font_action_none);
                mActionView.setEnabled(true);
            } else if (state == FontManager.STATE_INSTALLING) {
                mActionView.setText(R.string.font_action_installing);
                mActionView.setEnabled(false);
            } else {
                mActionView.setText(R.string.font_action_installed);
                mActionView.setEnabled(false);
            }
        }

        @Override
        public void onClick(View v) {
            if (v == itemView) {

                preview(mEntry);

            } else if (v == mActionView) {

                mActionView.setText(R.string.font_action_installing);
                mActionView.setEnabled(false);

                FontManager mgr = FontManager.instance();
                mgr.install(mEntry);
            }
        }
    }


}
