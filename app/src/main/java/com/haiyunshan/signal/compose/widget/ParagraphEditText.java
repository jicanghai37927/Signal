package com.haiyunshan.signal.compose.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.support.v7.widget.AppCompatEditText;
import android.text.Layout;
import android.text.Spanned;
import android.util.AttributeSet;
import android.widget.TextView;

import com.haiyunshan.signal.R;
import com.haiyunshan.signal.style.LineSpanRender;

import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 *
 */
public class ParagraphEditText extends AppCompatEditText {

    ArrayList<LineSpanRender> mLineSpanRenders;

    public ParagraphEditText(Context context) {
        this(context, null);
    }

    public ParagraphEditText(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.editTextStyle);
    }

    public ParagraphEditText(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTextContextMenuItem(int id) {

        // 粘贴为纯文本
        if (id == android.R.id.paste) {
            id = android.R.id.pasteAsPlainText;

            // 6.0之前没有pasteAsPlainText
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                id = android.R.id.paste;
            }
        }

        return super.onTextContextMenuItem(id);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (this.getLayout() == null) {
            assumeLayoutByReflect();
        }

        if (getText() instanceof Spanned) {
            if (mLineSpanRenders != null) {
                for (LineSpanRender r : mLineSpanRenders) {
                    r.draw(canvas);
                }
            }
        }

        super.onDraw(canvas);
    }

    public void addRender(LineSpanRender r) {
        if (mLineSpanRenders == null) {
            mLineSpanRenders = new ArrayList<>();
        }

        mLineSpanRenders.add(r);
    }

    public void prepareCursorControllers() {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) { // Android 6.0之前，不需要
//            return;
//        }

        boolean result = this.nullLayoutsByReflect();
        if (!result) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                this.setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE);
            }
        }
    }

    public boolean nullLayoutsByReflect() {
        TextView view = this;

        try {
            Method textCanBeSelected = TextView.class.getDeclaredMethod("nullLayouts");
            textCanBeSelected.setAccessible(true);
            textCanBeSelected.invoke(view);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean assumeLayoutByReflect() {
        TextView view = this;

        try {
            Method textCanBeSelected = TextView.class.getDeclaredMethod("assumeLayout");
            textCanBeSelected.setAccessible(true);
            textCanBeSelected.invoke(view);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }
}
