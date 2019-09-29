package com.haiyunshan.signal.font.dataset;

import com.haiyunshan.signal.dataset.BaseDataset;

public class FontDataset extends BaseDataset<FontEntry> {

    public FontEntry obtainBySource(String source) {
        for (FontEntry item : mList) {
            if (item.mSource.equalsIgnoreCase(source)) {
                return item;
            }
        }

        return null;
    }
}
