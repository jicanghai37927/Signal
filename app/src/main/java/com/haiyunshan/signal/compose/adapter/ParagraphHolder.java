package com.haiyunshan.signal.compose.adapter;

import android.text.Editable;
import android.text.SpannableString;
import android.text.TextWatcher;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.haiyunshan.signal.R;
import com.haiyunshan.signal.compose.ComposeFragment;
import com.haiyunshan.signal.compose.document.BaseItem;
import com.haiyunshan.signal.compose.document.ParagraphItem;
import com.haiyunshan.signal.compose.document.PictureItem;
import com.haiyunshan.signal.compose.helper.ParagraphSpanHelper;
import com.haiyunshan.signal.compose.widget.ParagraphEditText;
import com.haiyunshan.signal.style.HighlightSpan;
import com.haiyunshan.signal.style.LineSpanRender;

import java.io.File;
import java.util.ArrayList;

public class ParagraphHolder extends BaseHolder<ParagraphItem> implements TextWatcher {

    ParagraphEditText mEdit;

    ParagraphSpanHelper mSpanHelper;

    public static final ParagraphHolder create(ComposeFragment parent, ViewGroup container) {
        LayoutInflater inflater = parent.getLayoutInflater();
        int resource = R.layout.layout_compose_paragraph_item;
        View view = inflater.inflate(resource, container, false);

        ParagraphHolder holder = new ParagraphHolder(parent, view);
        return holder;
    }

    public ParagraphHolder(ComposeFragment parent, View itemView) {
        super(parent, itemView);

        this.mEdit = itemView.findViewById(R.id.edit_paragraph);
        mEdit.addRender(new LineSpanRender(mEdit, HighlightSpan.class));

        this.mSpanHelper = new ParagraphSpanHelper(this, mEdit);
    }

    @Override
    public void onBind(int position, ParagraphItem item) {
        super.onBind(position, item);

        mEdit.removeTextChangedListener(this);

        {
            CharSequence text = item.getText();
            mEdit.setText(text);

            mEdit.prepareCursorControllers();

            mEdit.setCustomSelectionActionModeCallback(mCustomSelectionActionModeCallback);
        }

        mEdit.addTextChangedListener(this);

    }

    @Override
    public void onViewAttachedToWindow() {
        super.onViewAttachedToWindow();

    }

    @Override
    public void onViewDetachedFromWindow() {
        super.onViewDetachedFromWindow();
    }

    @Override
    public void onSave() {
        super.onSave();

        Editable text = mEdit.getText();
        mItem.setText(text);
    }

    @Override
    public boolean insertPicture(String[] array) {
        int start = this.getSelectionStart();
        int end = this.getSelectionEnd();
        if (start < 0 || end < 0) {
            return false;
        }

        if (start != end) {
            return false;
        }

        boolean changed = false;

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

                // 每张图片后，追加一个段落
                if ((i + 1) != length) {
                    ParagraphItem p = ParagraphItem.create(mDocument,"");
                    list.add(p);
                }
            }

            boolean shouldSplit = true;

            {
                int pos = start;
                if (pos == mEdit.length()) {
                    int index = mDocument.indexOf(mItem);
                    if (index + 1 < mDocument.size()) {
                        BaseItem item = mDocument.get(index + 1);
                        shouldSplit = (!(item instanceof ParagraphItem));
                    }
                }
            }

            if (shouldSplit) {

                changed = true;

                int pos = start;
                Editable text = mEdit.getText();
                CharSequence s1 = text.subSequence(0, pos);
                CharSequence s2 = text.subSequence(pos, text.length());

                mItem.setText(s1);
                mItem.setSelection(pos, pos);

                ParagraphItem item2 = ParagraphItem.create(mDocument, s2);
                item2.setText(s2);

                list.add(item2);
            }
        }

        // 更新Document
        {
            int position = mDocument.indexOf(this.mItem);
            int index = position + 1;
            for (BaseItem item : list) {
                mDocument.add(index, item);

                index++;
            }
        }

        // 更新Adapter
        {
            int position = mAdapter.indexOf(mItem);
            int index = position + 1;

            for (BaseItem p : list) {
                mAdapter.add(index, p);

                index++;
            }

            int count = list.size();

            if (changed) {
                mAdapter.notifyItemChanged(position);
            }

            mAdapter.notifyItemRangeInserted(position + 1, count);
        }

        {
            int pos = mAdapter.indexOf(list.get(0));
            if (pos >= 0) {
                mRecyclerView.scrollToPosition(pos);
            }
        }

        return true;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {

    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {

    }

    @Override
    public void afterTextChanged(Editable s) {
        this.setModified(System.currentTimeMillis());
    }


    public int getSelectionStart() {
        int start = mEdit.getSelectionStart();
        int end = mEdit.getSelectionEnd();

        int pos = Math.min(start, end);
        return pos;
    }

    public int getSelectionEnd() {
        int start = mEdit.getSelectionStart();
        int end = mEdit.getSelectionEnd();

        int pos = Math.max(start, end);
        return pos;
    }

    public boolean isSelectedAll() {
        int start = this.getSelectionStart();
        int end = this.getSelectionEnd();

        return (start == 0 && end == mEdit.length());
    }

    ActionMode.Callback mCustomSelectionActionModeCallback = new ActionMode.Callback() {

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            menu.clear();

            {
                SpannableString ss = new SpannableString(mContext.getString(R.string.btn_highlight));
                menu.add(Menu.NONE, R.id.highlight, Menu.NONE, ss);
            }

            {
                menu.add(Menu.NONE, android.R.id.copy, Menu.NONE, android.R.string.copy);
            }

            if (!isSelectedAll()) {
                menu.add(Menu.NONE, android.R.id.selectAll, Menu.NONE, android.R.string.selectAll);
            }

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {

            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            boolean result = false;

            int itemId = item.getItemId();
            switch (itemId) {
                case R.id.highlight: {
                    mSpanHelper.toggleHighlight();

                    result = true;
                    break;
                }

                case android.R.id.copy: {
                    break;
                }
                case android.R.id.selectAll: {
                    break;
                }
            }

            return result;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {

        }
    };
}
