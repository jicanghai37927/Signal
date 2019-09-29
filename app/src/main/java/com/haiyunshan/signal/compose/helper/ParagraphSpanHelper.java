package com.haiyunshan.signal.compose.helper;

import android.text.Editable;

import com.haiyunshan.signal.compose.adapter.ParagraphHolder;
import com.haiyunshan.signal.compose.span.HighlightItem;
import com.haiyunshan.signal.compose.span.Spans;
import com.haiyunshan.signal.compose.widget.ParagraphEditText;


public class ParagraphSpanHelper {

    ParagraphHolder mHolder;
    ParagraphEditText mEdit;

    public ParagraphSpanHelper(ParagraphHolder holder, ParagraphEditText editText) {
        this.mHolder = holder;
        this.mEdit = editText;
    }

    public boolean toggleHighlight() {
        Editable text = mEdit.getText();

        HighlightItem item = Spans.HIGHLIGHT_ITEM;
        boolean result = item.match(text);
        if (result) {
            item.clear(text);
        } else {
            item.set(text);
        }

        return !result;
    }
}
