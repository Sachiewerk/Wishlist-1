package com.company.wishlist.view;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by vladstarikov on 02.03.16.
 */
public class CustomViewPager extends ViewPager {

    private boolean swiping = true;

    public CustomViewPager(Context context) {
        super(context);
    }

    public CustomViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return this.swiping && super.onTouchEvent(event);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return this.swiping && super.onInterceptTouchEvent(event);
    }

    public void setSwiping(boolean enabled) {
        this.swiping = enabled;
    }
}
