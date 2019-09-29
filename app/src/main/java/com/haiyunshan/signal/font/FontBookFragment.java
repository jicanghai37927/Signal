package com.haiyunshan.signal.font;


import android.Manifest;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.haiyunshan.signal.R;
import com.haiyunshan.signal.font.dataset.FontEntry;
import com.haiyunshan.signal.font.dataset.FontManager;
import com.haiyunshan.signal.font.dataset.PreviewDataset;
import com.haiyunshan.signal.font.dataset.StorageFontManager;
import com.haiyunshan.signal.font.dataset.StorageFontScanner;
import com.haiyunshan.signal.utils.GsonUtils;
import com.haiyunshan.signal.utils.PackageUtils;
import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;

/**
 * 外部字体浏览
 *
 */
public class FontBookFragment extends Fragment implements View.OnClickListener, ViewPager.OnPageChangeListener, FontManager.InstallObserver {

    ViewPager mViewPager;
    PreviewAdapter mAdapter;

    Toolbar mToolbar;
    TextView mTitleView;
    TextView mSubtitleView;

    TextView mActionView;

    StorageFontManager mManager;
    StorageFontScanner mScanner;

    PreviewDataset mPreview;

    File mTargetFile;

    public static final FontBookFragment newInstance(Uri uri) {
        FontBookFragment f = new FontBookFragment();

        Bundle bundle = new Bundle();
        bundle.putString("uri", uri.toString());
        f.setArguments(bundle);

        return f;
    }

    public FontBookFragment() {
        this.mManager = new StorageFontManager();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_typeface_preview, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        {
            this.mViewPager = view.findViewById(R.id.view_pager);
            mViewPager.addOnPageChangeListener(this);
            mViewPager.setOffscreenPageLimit(1);
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
        }

        {
            view.findViewById(R.id.title_layout).setVisibility(View.VISIBLE);
            this.mTitleView = view.findViewById(R.id.tv_title);
            this.mSubtitleView = view.findViewById(R.id.tv_subtitle);
        }

        {
            this.mActionView = view.findViewById(R.id.tv_action);
            mActionView.setOnClickListener(this);
        }

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        this.mPreview = GsonUtils.readAssets("typeface_preview.json", PreviewDataset.class);

        File file = null;
        Uri uri = Uri.parse(getArguments().getString("uri"));
        try {
            file = new File(new URI(uri.toString()));
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        if (file == null) {

        } else {
            this.mTargetFile = file;
        }

        FontManager.instance().registerInstallObserver(this);

        this.requestScan();
    }

    @Override
    public void onDestroy() {
        if (mScanner != null) {
            mScanner.dispose();
            mScanner = null;
        }

        FontManager.instance().unregisterInstallObserver(this);

        super.onDestroy();
    }

    @Override
    public void onClick(View v) {
        if (v == mActionView) {
            int current = mViewPager.getCurrentItem();
            FontEntry entry = mAdapter.mList.get(current);

            FontManager.instance().install(entry);

            onPageSelected(current);
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        FontEntry entry = mAdapter.mList.get(position);
        String name = entry.getName();
        String uri = entry.getUri();

        if (TextUtils.isEmpty(name)) {
            if (!entry.isValid()) {
                name = getString(R.string.font_invalid);
            } else if (!entry.isSupport()) {
                name = getString(R.string.font_not_support);
            }
        }

        mTitleView.setText(name);
        mSubtitleView.setText(uri);

        if (!entry.isValid() || !entry.isSupport()) {
            mActionView.setText(R.string.font_action_cannot_install);
            mActionView.setEnabled(false);
        } else {
            int state = FontManager.instance().getState(entry);
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

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onChanged(FontManager manager, FontEntry entry) {
        int current = mViewPager.getCurrentItem();
        FontEntry e = mAdapter.mList.get(current);
        if (entry == e) {
            onPageSelected(current);
        }
    }

    void requestScan() {
        if (PackageUtils.canRead()) {
            beginScan();
            return;
        }

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
                                        requestScan();
                                    } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                                        getActivity().onBackPressed();
                                    }
                                }
                            };

                            DialogInterface.OnCancelListener cancelListener = new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    getActivity().onBackPressed();
                                }
                            };

                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setOnCancelListener(cancelListener);
                            builder.setMessage(R.string.font_preview_msg_1);
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

                                        getActivity().onBackPressed();
                                    } else if (which == DialogInterface.BUTTON_NEGATIVE) {
                                        getActivity().onBackPressed();
                                    }
                                }
                            };

                            DialogInterface.OnCancelListener cancelListener = new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    getActivity().onBackPressed();
                                }
                            };

                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setOnCancelListener(cancelListener);
                            builder.setMessage(R.string.font_preview_msg_2);
                            builder.setPositiveButton(R.string.btn_setting, listener);
                            builder.setNegativeButton(R.string.btn_cancel, listener);
                            AlertDialog dialog = builder.create();
                            dialog.setCanceledOnTouchOutside(false);
                            dialog.show();

                        }
                    }
                });
    }

    void beginScan() {

        File file = this.mTargetFile;
        File folder = file.getParentFile();

        this.mScanner = new StorageFontScanner(mManager, folder);
        mScanner.setFilter(0, false);

        Observable.create(mScanner)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<File>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(File file) {

                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                        mScanner = null;

                        buildViewPager();
                    }
                });
    }

    void buildViewPager() {

        mActionView.setVisibility(View.VISIBLE);

        String source = mTargetFile.getAbsolutePath();

        this.mAdapter = new PreviewAdapter(mManager.getDataset().getList());
        mViewPager.setAdapter(mAdapter);
        int pos = mAdapter.indexOf(source);
        if (pos > 0) {
            mViewPager.setCurrentItem(pos);
        }

        pos = mViewPager.getCurrentItem();
        if (pos >= 0) {
            onPageSelected(pos);
        }
    }

    /**
     *
     */
    private class PreviewAdapter extends PagerAdapter {

        List<FontEntry> mList;

        int mPrimaryItem;

        Collator mCollator;

        PreviewAdapter(List<FontEntry> list) {
            this.mList = list;

            this.mCollator = Collator.getInstance();
            Collections.sort(list, new Comparator<FontEntry>() {
                @Override
                public int compare(FontEntry o1, FontEntry o2) {
                    boolean empty1 = TextUtils.isEmpty(o1.getName());
                    boolean empty2 = TextUtils.isEmpty(o2.getName());
                    if (empty1 && empty2) {
                        return mCollator.compare(o1.getUri(), o2.getUri());
                    } else if (empty1 && !empty2) {
                        return 1;
                    } else if (!empty1 && empty2) {
                        return -1;
                    }

                    return mCollator.compare(o1.getName(), o2.getName());
                }
            });
        }

        int indexOf(String source) {
            for (int i = 0; i < mList.size(); i++) {
                FontEntry e = mList.get(i);
                if (e.getSource().equalsIgnoreCase(source)) {
                    return i;
                }
            }

            return -1;
        }
        @Override
        public int getCount() {
            return mList.size();
        }

        @Override
        public void startUpdate(@NonNull ViewGroup container) {
            super.startUpdate(container);
        }

        @NonNull
        @Override
        public Object instantiateItem(@NonNull ViewGroup container, int position) {

            int resource = R.layout.layout_typeface_preview;
            LayoutInflater inflater = getActivity().getLayoutInflater();
            View child = inflater.inflate(resource, container, false);
            container.addView(child);

            TextView view = (TextView)child;

            FontEntry entry = mList.get(position);
            PreviewDataset.PreviewEntry preview = mPreview.obtain(entry.getLanguage());

            StorageFontManager mgr = mManager;
            Typeface tf = mgr.getTypeface(entry);
            view.setTypeface(tf);

            if (!entry.isValid()) {
                view.setText(R.string.font_invalid_file);
            } else if (!entry.isSupport()) {
                view.setText(R.string.font_not_support_file);
            } else {
                view.setText(preview.mText);
            }

            return child;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            View child = (View)object;
            container.removeView(child);
        }

        @Override
        public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            this.mPrimaryItem = position;
        }

        @Override
        public void finishUpdate(@NonNull ViewGroup container) {
            super.finishUpdate(container);
        }

        @Override
        public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
            return (view == object);
        }
    }

}
