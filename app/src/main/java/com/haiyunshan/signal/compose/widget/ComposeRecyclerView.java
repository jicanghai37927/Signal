package com.haiyunshan.signal.compose.widget;

import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 *
 */
public class ComposeRecyclerView extends RecyclerView {

    View mTargetChild;
    Rect mTempRect = new Rect();

    OnNestedScrollListener mOnNestedScrollListener;

    public ComposeRecyclerView(Context context) {
        this(context, null);
    }

    public ComposeRecyclerView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ComposeRecyclerView(Context context, @Nullable AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setOnNestedScrollListener(OnNestedScrollListener listener) {
        this.mOnNestedScrollListener = listener;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        boolean result = super.dispatchTouchEvent(ev);

        int action = ev.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                this.mTargetChild = findTargetChild(ev);
                break;
            }
            case MotionEvent.ACTION_MOVE: {
                if (mTargetChild != null) {

                    int state = this.getScrollState();
                    if (state != SCROLL_STATE_IDLE) {
                        MotionEvent event = MotionEvent.obtain(ev);
                        event.offsetLocation(mTempRect.left, -mTempRect.top); // 转换到Child坐标
                        event.setAction(MotionEvent.ACTION_CANCEL);

                        mTargetChild.dispatchTouchEvent(event);
                        mTargetChild = null;

                        event.recycle();
                    }
                }

                break;
            }
        }

        if (mTargetChild != null) {
            MotionEvent event = MotionEvent.obtain(ev);
            event.offsetLocation(mTempRect.left, -mTempRect.top); // 转换到Child坐标

            mTargetChild.dispatchTouchEvent(event);

            event.recycle();
        }

        return result;
    }

    @Override
    public boolean dispatchNestedFling(float velocityX, float velocityY, boolean consumed) {
        if (mOnNestedScrollListener != null) {
            mOnNestedScrollListener.onNestedFling(this, velocityX, velocityY, consumed);
        }

        return super.dispatchNestedFling(velocityX, velocityY, consumed);
    }

    @Override
    public boolean dispatchNestedPreFling(float velocityX, float velocityY) {
        if (mOnNestedScrollListener != null) {
            mOnNestedScrollListener.onNestedPreFling(this, velocityX, velocityY);
        }

        return super.dispatchNestedPreFling(velocityX, velocityY);
    }

    View findTargetChild(MotionEvent ev) {

        int count = this.getChildCount();
        if (count != 0) {
            View view = this.getChildAt(count - 1);
            mTempRect.set(0, 0, view.getWidth(), view.getHeight());

            offsetDescendantRectToMyCoords(view, mTempRect);

            float y = ev.getY();
            int bottom = mTempRect.bottom;
            if (y > bottom) {
                return view;
            }
        }

        return null;
    }

    /**
     *
     */
    public interface OnNestedScrollListener {

        boolean onNestedPreFling(@NonNull View target, float velocityX, float velocityY);

        boolean onNestedFling(@NonNull View target, float velocityX, float velocityY, boolean consumed);

    }
}
