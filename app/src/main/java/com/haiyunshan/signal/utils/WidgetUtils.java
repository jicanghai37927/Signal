package com.haiyunshan.signal.utils;

import android.os.Build;
import android.text.Layout;
import android.widget.TextView;

import java.lang.reflect.Method;

public class WidgetUtils {

    public static void prepareCursorControllers(TextView view) {

        boolean result = nullLayoutsByReflect(view);
        if (!result) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                view.setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE);
            }
        }

    }

    public static boolean nullLayoutsByReflect(TextView view) {
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
}
