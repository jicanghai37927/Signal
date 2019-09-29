package com.haiyunshan.signal.font.dataset;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;

import com.haiyunshan.signal.dataset.FileStorage;
import com.haiyunshan.signal.utils.GsonUtils;
import com.haiyunshan.signal.utils.MD5Utils;
import com.haiyunshan.signal.utils.UUIDUtils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class FontManager {

    private static FontManager sInstance;

    public static final int STATE_NONE          = 0; // 未安装
    public static final int STATE_INSTALLED     = 1; // 已安装
    public static final int STATE_INSTALLING    = 2; // 安装中

    File mFile;
    FontDataset mDataset;

    Installer mInstaller;
    ArrayList<FontEntry> mInstallList;
    InstallObservable mObservable;

    public static FontManager instance() {
        if (sInstance == null) {
            File file = FileStorage.getFontDataset();
            sInstance = new FontManager(file);
        }

        return sInstance;
    }

    private FontManager(File file) {
        this.mFile = file;

        if (mFile != null) {
            this.mDataset = GsonUtils.read(mFile, FontDataset.class);
            if (mDataset == null) {
                mDataset = new FontDataset();
            }
        } else {
            this.mDataset = new FontDataset();
        }

        this.mInstallList = new ArrayList<>();
        this.mObservable = new InstallObservable();

    }

    public void put(String name, String uri, String source, String md5, int lang, long size) {
        FontEntry e = mDataset.obtainBySource(source);
        mDataset.remove(e);

        e = new FontEntry(UUIDUtils.next(), name, uri, source, md5, lang, size);
        mDataset.add(e);
    }

    public FontDataset getDataset() {
        return mDataset;
    }

    public void save() {
        if (mFile == null) {
            return;
        }

        if (mDataset != null) {
            GsonUtils.write(mDataset, mFile);
        }
    }

    public int getState(FontEntry entry) {
        if (mInstallList.contains(entry)) {
            return STATE_INSTALLING;
        }

        if (mDataset.obtainBySource(entry.getSource()) != null) {
            return STATE_INSTALLED;
        }

        return STATE_NONE;
    }

    public void install(FontEntry entry) {
        if (mInstallList.contains(entry)) {
            return;
        }

        boolean isEmpty = mInstallList.isEmpty();

        mInstallList.add(entry);
        if (isEmpty) {
            runInstall(entry);
        }
    }

    void runInstall(FontEntry entry) {

        this.mInstaller = new Installer(entry);
        Observable.create(mInstaller)
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<FontEntry>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(FontEntry entry) {
                        mInstallList.remove(entry);

                        mObservable.notifyChanged(entry);
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {
                        mInstaller = null;

                        if (!mInstallList.isEmpty()) {
                            runInstall(mInstallList.get(0));
                        }
                    }
                });
    }

    public void registerInstallObserver(@NonNull InstallObserver observer) {
        mObservable.registerObserver(observer);
    }

    public void unregisterInstallObserver(@NonNull InstallObserver observer) {
        mObservable.unregisterObserver(observer);
    }

    public interface InstallObserver {

        void onChanged(FontManager manager, FontEntry entry);

    }

    private class InstallObservable extends android.database.Observable<InstallObserver> {

        public void notifyChanged(FontEntry entry) {
            for (int i = mObservers.size() - 1; i >= 0; i--) {
                mObservers.get(i).onChanged(FontManager.this, entry);
            }
        }

    }

    private class Installer implements ObservableOnSubscribe<FontEntry>, Disposable {

        FontEntry mEntry;
        boolean mDisposed;

        Installer(FontEntry entry) {
            this.mEntry = entry;

            this.mDisposed = false;
        }

        @Override
        public void subscribe(ObservableEmitter<FontEntry> emitter) throws Exception {

            // 获取后缀名
            File file = new File(mEntry.getSource());
            String name = file.getName();
            int pos = name.lastIndexOf('.');
            if (pos < 0) {
                emitter.onNext(mEntry);
                emitter.onComplete();
                return;
            }
            String suffix = name.substring(pos).toLowerCase(); // 后缀统一为小写

            if (this.isDisposed()) {
                return;
            }

            // 计算MD5
            String md5 = mEntry.getMD5();
            if (TextUtils.isEmpty(md5)) {
                md5 = MD5Utils.getFileMD5(file);
            }

            if (TextUtils.isEmpty(md5)) {
                emitter.onNext(mEntry);
                emitter.onComplete();
                return;
            }

            mEntry.setMD5(md5);

            if (this.isDisposed()) {
                return;
            }

            // 拷贝文件
            String uri = md5 + suffix + ".font";
            File destFile = FileStorage.getFont(uri);
            if (!destFile.exists()) {
                String tmp = md5 + suffix + ".tmp";
                File tmpFile = FileStorage.getFont(tmp);

                // 拷贝到临时文件
                FileUtils.copyFile(file, tmpFile);

                // 重命名到目标文件
                if (destFile.exists()) {
                    tmpFile.delete();
                } else {
                    tmpFile.renameTo(destFile);
                }
            }

            // 添加数据
            FontManager mgr = FontManager.instance();
            mgr.put(mEntry.getName(), uri, mEntry.getSource(), md5, mEntry.getLanguage(), mEntry.getSize());
            mgr.save();

            if (this.isDisposed()) {
                return;
            }

            // 完成
            emitter.onNext(mEntry);
            emitter.onComplete();
        }

        @Override
        public void dispose() {
            this.mDisposed = true;
        }

        @Override
        public boolean isDisposed() {
            return this.mDisposed;
        }
    }
}
