package com.haiyunshan.signal.font;


import android.Manifest;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.annotations.SerializedName;
import com.haiyunshan.signal.R;
import com.haiyunshan.signal.font.dataset.FontDataset;
import com.haiyunshan.signal.font.dataset.FontEntry;
import com.haiyunshan.signal.font.dataset.FontManager;
import com.haiyunshan.signal.font.dataset.PreviewDataset;
import com.haiyunshan.signal.font.dataset.StorageFontManager;
import com.haiyunshan.signal.utils.GsonUtils;
import com.haiyunshan.signal.utils.PackageUtils;
import com.tbruyelle.rxpermissions2.Permission;
import com.tbruyelle.rxpermissions2.RxPermissions;

import java.util.ArrayList;

import io.reactivex.functions.Consumer;

/**
 * A simple {@link Fragment} subclass.
 */
public class TypefacePreviewFragment extends Fragment implements View.OnClickListener, ViewPager.OnPageChangeListener, FontManager.InstallObserver {

    ViewPager mViewPager;
    PreviewAdapter mAdapter;

    Toolbar mToolbar;

    TextView mActionView;

    PreviewDataset mPreview;

    public static TypefacePreviewFragment newInstance(Bundle args) {

        TypefacePreviewFragment f = new TypefacePreviewFragment();
        if (args != null) {
            f.setArguments(args);
        }

        return f;
    }

    public TypefacePreviewFragment() {

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
            this.mActionView = view.findViewById(R.id.tv_action);
            mActionView.setOnClickListener(this);
        }

    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        this.mPreview = GsonUtils.readAssets("typeface_preview.json", PreviewDataset.class);

        FontManager.instance().registerInstallObserver(this);

        this.requestScan();
    }

    @Override
    public void onDestroy() {
        FontManager.instance().unregisterInstallObserver(this);

        super.onDestroy();
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        FontEntry entry = mAdapter.mList.get(position);
        mToolbar.setTitle(entry.getName());

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

    @Override
    public void onClick(View v) {
        if (v == mActionView) {
            int current = mViewPager.getCurrentItem();
            FontEntry entry = mAdapter.mList.get(current);

            FontManager.instance().install(entry);

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

        mActionView.setVisibility(View.VISIBLE);

        Bundle args = this.getArguments();
        String source = args.getString("source");
        ArrayList<String> list = args.getStringArrayList("list");

        this.mAdapter = new PreviewAdapter(list);
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

    private class PreviewAdapter extends PagerAdapter {

        ArrayList<FontEntry> mList;

        FontEntry mPrimaryItem;

        PreviewAdapter(ArrayList<String> list) {
            this.mList = new ArrayList<>(list.size());

            StorageFontManager mgr = StorageFontManager.instance();
            FontDataset ds = mgr.getDataset();
            for (String source : list) {
                FontEntry e = ds.obtainBySource(source);
                if (e != null) {
                    mList.add(e);
                }
            }
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

            StorageFontManager mgr = StorageFontManager.instance();
            Typeface tf = mgr.getTypeface(entry);
            view.setTypeface(tf);
            view.setText(preview.mText);

            return child;
        }

        @Override
        public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            View child = (View)object;
            container.removeView(child);
        }

        @Override
        public void setPrimaryItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
            this.mPrimaryItem = mList.get(position);
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
