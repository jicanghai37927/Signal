package com.haiyunshan.signal.html;

import java.io.File;

public class BaseDiv {

    HtmlPage mPage;

    BaseDiv(HtmlPage page) {
        this.mPage = page;
    }

    File getFile() {
        return mPage.mFile;
    }
}
