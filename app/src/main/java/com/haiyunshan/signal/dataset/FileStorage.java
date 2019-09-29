package com.haiyunshan.signal.dataset;

import android.content.Context;
import android.os.Environment;

import com.haiyunshan.signal.R;
import com.haiyunshan.signal.SignalApp;

import java.io.File;

public class FileStorage {

    private static final String STORAGE_NAME    = "Signal";
    private static final String STORAGE_FILES   = "files";

    private static final String NOTE_FOLDER     = "note";
    private static final String NOTE_DATASET    = "note_ds.json";

    private static final String FONT_FOLDER     = "font";
    private static final String FONT_DATASET    = "font_ds.json";

    private static final String SCAN_FOLDER      = "scan";
    private static final String SCAN_TYPEFACE    = "typeface_ds.json";

    /**
     * 导出文档类型目录-URI
     *
     * @param uri
     * @return
     */
    public static final File getDocumentExportFolder(String uri) {
        File file = getDocumentExportRoot();

        file = new File(file, uri);

        file.mkdirs();

        return file;
    }

    /**
     * 导出文档根目录-文档Document
     *
     * @return
     */
    public static final File getDocumentExportRoot() {
        Context context = SignalApp.instance();

        File file = getExportRoot();

        String name = context.getString(R.string.export_document);
        file = new File(file, name);

        file.mkdirs();

        return file;
    }

    /**
     * 导出数据根目录-信号Signal
     *
     * @return
     */
    public static final File getExportRoot() {
        Context context = SignalApp.instance();

        File file = Environment.getExternalStorageDirectory();

        String name = context.getString(R.string.export_folder);
        file = new File(file, name);

        file.mkdirs();

        return file;
    }

    public static final File getScanTypefaceDataset() {
        File dir = getStorageDir();
        dir = new File(dir, SCAN_FOLDER);
        dir.mkdirs();

        File file = new File(dir, SCAN_TYPEFACE);
        return file;
    }

    public static final File getFont(String uri) {
        File dir = getStorageDir();
        dir = new File(dir, FONT_FOLDER);
        dir = new File(dir, STORAGE_FILES);
        dir.mkdirs();

        dir = new File(dir, uri);

        return dir;
    }

    /**
     *
     * @return
     */
    public static final File getFontDataset() {
        File dir = getStorageDir();
        dir = new File(dir, FONT_FOLDER);
        dir.mkdirs();

        File file = new File(dir, FONT_DATASET);
        return file;
    }

    public static final File getNotePicture(String noteId, String uri) {
        File file = getNote(noteId);
        file = new File(file, "pictures");
        file.mkdirs();

        file = new File(file, uri + ".picture");
        return file;
    }

    /**
     *
     * @param id
     * @return
     */
    public static final File getNote(String id) {
        File dir = getStorageDir();
        dir = new File(dir, NOTE_FOLDER);
        dir = new File(dir, STORAGE_FILES);
        dir = new File(dir, id + ".note");

        return dir;
    }

    /**
     *
     * @return
     */
    public static final File getNoteDataset() {
        File dir = getStorageDir();
        dir = new File(dir, NOTE_FOLDER);
        dir.mkdirs();

        File file = new File(dir, NOTE_DATASET);
        return file;
    }

    /**
     *
     * @return
     */
    public static final File getStorageDir() {
        SignalApp context = SignalApp.instance();
        File dir = context.getExternalFilesDir(null);
        dir = new File(dir, STORAGE_NAME);
        dir.mkdirs();

        return dir;
    }
}
