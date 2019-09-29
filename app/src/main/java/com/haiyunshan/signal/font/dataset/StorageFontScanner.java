package com.haiyunshan.signal.font.dataset;

import android.content.Context;
import android.text.TextUtils;

import com.haiyunshan.signal.SignalApp;
import com.haiyunshan.signal.utils.StorageManagerHack;

import org.apache.fontbox.ttf.NameRecord;
import org.apache.fontbox.ttf.NamingTable;
import org.apache.fontbox.ttf.OTFParser;
import org.apache.fontbox.ttf.OpenTypeFont;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.disposables.Disposable;

public class StorageFontScanner implements ObservableOnSubscribe<File>, Disposable {

    static final int[][] NAMING_ARRAY = new int[][] {
            new int[] {16,                                  NameRecord.PLATFORM_WINDOWS, NameRecord.ENCODING_WINDOWS_UNICODE_BMP, 2052},
            new int[] {NameRecord.NAME_FONT_FAMILY_NAME,    NameRecord.PLATFORM_WINDOWS, NameRecord.ENCODING_WINDOWS_UNICODE_BMP, 2052},
            new int[] {NameRecord.NAME_FULL_FONT_NAME,      NameRecord.PLATFORM_WINDOWS, NameRecord.ENCODING_WINDOWS_UNICODE_BMP, 2052},

            new int[] {16,                                  NameRecord.PLATFORM_WINDOWS, NameRecord.ENCODING_WINDOWS_UNICODE_BMP, 1033},
            new int[] {NameRecord.NAME_FONT_FAMILY_NAME,    NameRecord.PLATFORM_WINDOWS, NameRecord.ENCODING_WINDOWS_UNICODE_BMP, 1033},
            new int[] {NameRecord.NAME_FULL_FONT_NAME,      NameRecord.PLATFORM_WINDOWS, NameRecord.ENCODING_WINDOWS_UNICODE_BMP, 1033},

            new int[] {NameRecord.NAME_FONT_FAMILY_NAME,    NameRecord.PLATFORM_MACINTOSH, NameRecord.ENCODING_MACINTOSH_ROMAN, NameRecord.LANGUGAE_MACINTOSH_ENGLISH}

    };

    ArrayList<File> mList;
    boolean mDisposed;

    OTFParser mParser;
    StorageFontManager mManager;

    long mTimestamp;
    boolean mRecursive;

    long mFilterSize = 2 * 1024;
    boolean mFilterName = true;

    public StorageFontScanner(StorageFontManager manager, File... folders) {
        this.mManager = manager;
        this.mRecursive = false;

        this.mList = new ArrayList<>();
        for (File f : folders) {
            mList.add(f);
        }
    }

    public StorageFontScanner(StorageFontManager manager) {
        this.mManager = manager;
        this.mRecursive = true;

        this.mList = new ArrayList<>();

        boolean stackFromEnd = true;
        Context context = SignalApp.instance();
        List<String> list = StorageManagerHack.getMountedVolume(context);

        if (!list.isEmpty()) {
            int i = (stackFromEnd) ? (list.size() - 1) : 0;
            while (true) {
                String path = list.get(i);
                File file = new File(path);
                mList.add(file);

                if (stackFromEnd) {
                    --i;
                    if (i < 0) {
                        break;
                    }
                } else {
                    ++i;
                    if (i >= list.size()) {
                        break;
                    }
                }
            }
        }

        this.mDisposed = false;
    }

    public void setFilter(long minSize, boolean emptyName) {
        this.mFilterSize = minSize;
        this.mFilterName = emptyName;
    }

    public void dispose() {
        this.mDisposed = true;
    }

    public boolean isDisposed() {
        return this.mDisposed;
    }

    boolean isTypeface(File file) {

        while (true) {

            if (!file.canRead() || !file.exists()) {
                return false;
            }

            if (file.length() < mFilterSize) {
                return false;
            }

            String name = file.getName();
            if (name.length() >= 2 && name.charAt(0) == '.' && name.charAt(1) == '_') { // MAC系统._文件
                return false;
            }

            int pos = name.lastIndexOf('.');
            if (pos < 0) {
                return false;
            }

            String suffix = name.substring(pos).toLowerCase();
            if (!(suffix.endsWith(".ttf") || suffix.endsWith(".otf"))) {
                return false;
            }

            return true;
        }


    }

    boolean accept(String name, File file) {
        boolean chinese = containChinese(name);
        if (chinese) {
            long size = file.length();
            return size > 1024 * 1024;
        }

        return true;
    }

    boolean containChinese(String name) {
        int length = name.length();
        for (int i = 0; i < length; i++) {
            char c = name.charAt(i);

            boolean v = (c >= 0x4e00 && c <= 0x9fa5);
            if (v) {
                return true;
            }
        }

        return false;
    }

    FontEntry createEntry(File file) {

        String name = null;

        int nameId;
        int platformId;
        int encodingId;
        int languageId = -1;

        if (mParser == null) {
            this.mParser = new OTFParser(false, true);
        }
        OTFParser parser = this.mParser;

        if (file.length() > 0) {

            try {

                OpenTypeFont font = parser.parse(file.getAbsolutePath());
                NamingTable table = font.getNaming();

                int[][] array = NAMING_ARRAY;
                for (int[] s : array) {
                    nameId = s[0];
                    platformId = s[1];
                    encodingId = s[2];
                    languageId = s[3];

                    name = table.getName(nameId, platformId, encodingId, languageId);
                    if (!TextUtils.isEmpty(name)) {
                        break;
                    }
                }

                font.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        FontEntry entry = null;
        if (mFilterName) {
            if (!TextUtils.isEmpty(name)) {
                if (accept(name, file)) {
                    entry = new FontEntry(name, file.getName(), file.getAbsolutePath(), languageId, file.length());
                }
            }
        } else {
            name = (name == null)? "": name;
            entry = new FontEntry(name, file.getName(), file.getAbsolutePath(), languageId, file.length());
        }

        return entry;
    }

    void iterate(ObservableEmitter<File> emitter, File file) {

        if (this.mDisposed) {
            return;
        }

        if (file.isDirectory()) {
            {
                long time = System.currentTimeMillis();
                long ellapse = time - mTimestamp;
                if (ellapse > 100) { // 控制消息次数
                    emitter.onNext(file);

                    mTimestamp = time;
                }
            }

            File[] files = file.listFiles();
            for (File f : files) {
                iterate(emitter, f);
            }

        } else {
            this.accept(emitter, file);
        }
    }

    void accept(ObservableEmitter<File> emitter, File file) {
        if (!isTypeface(file)) {
            return;
        }

        FontEntry entry = null;

        if (mManager != null) {
            FontDataset ds = mManager.getDataset();
            entry = ds.obtainBySource(file.getAbsolutePath());
        }

        if (entry != null) {

        } else {

            entry = this.createEntry(file);

            // 同步到数据库中
            if (entry != null && mManager != null) {
                FontDataset ds = mManager.getDataset();
                ds.add(entry);
                mManager.save();
            }
        }

        if (entry != null) {
            emitter.onNext(file);
        }
    }

    @Override
    public void subscribe(ObservableEmitter<File> emitter) throws Exception {
        this.mTimestamp = 0;

        if (mRecursive) {

            for (File file : mList) {
                iterate(emitter, file);
            }

        } else {

            for (File file : mList) {
                if (isDisposed()) {
                    break;
                }

                if (file.isDirectory()) {
                    File[] array = file.listFiles();
                    for (File f : array) {

                        if (isDisposed()) {
                            break;
                        }

                        if (f.isDirectory()) {
                            continue;
                        }

                        accept(emitter, f);

                    }

                } else {
                    accept(emitter, file);
                }
            }

        }

        // 记录最后一次时间
        if (!isDisposed()) {
            if (mManager != null) {
                FontDataset ds = mManager.getDataset();
                ds.setModified(System.currentTimeMillis());
            }
        }

        // 保存数据
        if (mManager != null) {
            mManager.save();
        }

        // 加载第一个字体
        if (mManager != null) {
            FontDataset ds = mManager.getDataset();
            if (!ds.isEmpty()) {
                mManager.getTypeface(ds.getList().get(0));
            }
        }

        //
        if (!isDisposed()) {
            emitter.onComplete();
        }

    }
}