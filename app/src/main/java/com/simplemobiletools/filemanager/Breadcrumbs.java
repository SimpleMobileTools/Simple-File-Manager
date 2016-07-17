package com.simplemobiletools.filemanager;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class Breadcrumbs extends LinearLayout implements View.OnClickListener {
    private LayoutInflater mInflater;
    private int mDeviceWidth;
    private BreadcrumbsListener mListener;

    public Breadcrumbs(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        final Point deviceDisplay = new Point();
        display.getSize(deviceDisplay);
        mDeviceWidth = deviceDisplay.x;
    }

    public void setListener(BreadcrumbsListener listener) {
        mListener = listener;
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int paddingTop = getPaddingTop();
        final int paddingLeft = getPaddingLeft();
        final int paddingRight = getPaddingRight();
        final int childRight = getMeasuredWidth() - paddingRight;
        final int childBottom = getMeasuredHeight() - getPaddingBottom();
        final int childHeight = childBottom - paddingTop;

        final int usableWidth = mDeviceWidth - paddingLeft - paddingRight;
        int maxHeight = 0;
        int curWidth;
        int curHeight;
        int curLeft = paddingLeft;
        int curTop = paddingTop;

        final int cnt = getChildCount();
        for (int i = 0; i < cnt; i++) {
            final View child = getChildAt(i);

            child.measure(MeasureSpec.makeMeasureSpec(usableWidth, MeasureSpec.AT_MOST),
                    MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.AT_MOST));
            curWidth = child.getMeasuredWidth();
            curHeight = child.getMeasuredHeight();

            if (curLeft + curWidth >= childRight) {
                curLeft = paddingLeft;
                curTop += maxHeight;
                maxHeight = 0;
            }

            child.layout(curLeft, curTop, curLeft + curWidth, curTop + curHeight);
            if (maxHeight < curHeight)
                maxHeight = curHeight;

            curLeft += curWidth;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int usableWidth = mDeviceWidth - getPaddingLeft() - getPaddingRight();
        int width = 0;
        int rowHeight = 0;
        int lines = 1;

        final int cnt = getChildCount();
        for (int i = 0; i < cnt; i++) {
            final View child = getChildAt(i);
            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            width += child.getMeasuredWidth();
            rowHeight = child.getMeasuredHeight();

            if (width / usableWidth > 0) {
                lines++;
                width = child.getMeasuredWidth();
            }
        }

        final int parentWidth = MeasureSpec.getSize(widthMeasureSpec);
        final int calculatedHeight = getPaddingTop() + getPaddingBottom() + (rowHeight * lines);
        setMeasuredDimension(parentWidth, calculatedHeight);
    }

    public void setInitialBreadcrumb() {
        addBreadcrumb(getResources().getString(R.string.initial_breadcrumb));
    }

    public void addBreadcrumb(String text) {
        final View view = mInflater.inflate(R.layout.breadcrumb_item, null, false);
        view.setTag(getChildCount());
        final TextView textView = (TextView) view.findViewById(R.id.breadcrumb_text);
        textView.setText(text);
        addView(view);
        view.setOnClickListener(this);
    }

    public void removeBreadcrumb() {
        removeView(getChildAt(getChildCount() - 1));
    }

    @Override
    public void onClick(View v) {
        if (mListener != null) {
            mListener.breadcrumbClicked((Integer) v.getTag());
        }
    }

    public interface BreadcrumbsListener {
        void breadcrumbClicked(int id);
    }
}
