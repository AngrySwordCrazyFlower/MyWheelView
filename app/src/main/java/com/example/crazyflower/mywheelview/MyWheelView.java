package com.example.crazyflower.mywheelview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.lang.ref.WeakReference;
import java.util.List;


/**
 * Created by CrazyFlower on 2018/4/2.
 */

public class MyWheelView extends View {

    private static final String TAG = "MyWheelView";

    private static final String RESILIENCE_DISTANCE_OF_ONCE = "resilience_distance_of_once";
    private static final String RESILIENCE_LEFT_TIMES = "left_times";

    private static final int RESILIENCE_TIMES = 5;
    private static final int RESILIENCE_TIME_INTERVAL = 50;

    private List<String> data;
    private int selectedItemIndex = 0;

    private float lastY;
    private float scrollY;

    private int viewWidth;
    private int viewHeight;
    private float itemHeight;
    private int itemNumber;
    private int halfItemNumber;
    private static final float maxScaleTextSizeToItemHeight = 0.9f;
    private static final float minScaleTextSizeToItemHeight = 0.72f;
    private float maxTextSize;
    private float minTextSize;

    private IWheelViewSelectedListener wheelViewSelectedListener;

    Paint selectedLinePaint;
    Paint selectedBackgroundPaint;
    Paint normalTextPaint;
    Paint selectedTextPaint;

    private Handler handler;

    public MyWheelView(Context context) {
        this(context, null);
    }

    public MyWheelView(Context context, AttributeSet attrs) {
        this(context, attrs, 1);
    }

    public MyWheelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MyWheelView);
        initAttributesData(typedArray);
        initDefaultData();
    }

    private void initAttributesData(TypedArray typedArray) {
        Log.d(TAG, "initDataAndPaint: ");

        itemNumber = typedArray.getInt(R.styleable.MyWheelView_item_number, 5);
        halfItemNumber = itemNumber / 2;

        selectedLinePaint = new Paint();
        selectedBackgroundPaint = new Paint();
        normalTextPaint = new Paint();
        selectedTextPaint = new Paint();

        selectedLinePaint.setColor(typedArray.getColor(R.styleable.MyWheelView_selected_line_color, Color.rgb(0, 0, 0)));
        selectedBackgroundPaint.setColor(typedArray.getColor(R.styleable.MyWheelView_selected_background_color, Color.rgb(255, 255, 255)));
        normalTextPaint.setColor(typedArray.getColor(R.styleable.MyWheelView_normal_text_color, Color.rgb(0, 0, 0)));
        selectedTextPaint.setColor(typedArray.getColor(R.styleable.MyWheelView_selected_text_color, Color.rgb(0, 255, 204)));

        selectedLinePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        selectedLinePaint.setStrokeWidth(4);
        selectedBackgroundPaint.setStyle(Paint.Style.FILL);
    }

    /*
     * 初始化宽高有关的数据
     */
    private void initWHData() {
        viewWidth = getMeasuredWidth();
        viewHeight = getMeasuredHeight();
        itemHeight = ((float) viewHeight) / itemNumber;
        maxTextSize = maxScaleTextSizeToItemHeight * itemHeight;
        minTextSize = minScaleTextSizeToItemHeight * itemHeight;
    }

    private void initDefaultData() {
        //默认选中为0，实际上setData的时候也会初始化selectedItemIndex
        selectedItemIndex = 0;

        handler = new MyWheelViewHandler(this);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        initWHData();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Log.d(TAG, "onDraw: " + selectedItemIndex);

        //如果没有数据或者数据量为0，不绘制
        if (null == data || 0 == data.size()) {
            return;
        }

        drawSelectedRectangle(canvas);

        /*
         * draw the text
         * think about the effect, draw the selected, and (halfItemNumber + 1) items above it,
         * and (halfItemNumber + 1) items below it.
         */
        String text;
        Paint paint;
        float midY;
        for (int i = Math.max(0, selectedItemIndex - (halfItemNumber + 1)),
             max = Math.min(data.size() - 1, selectedItemIndex + (halfItemNumber + 1));
             i <= max; i++) {
            text = data.get(i);

            midY = itemHeight * (halfItemNumber - (selectedItemIndex - i)) + itemHeight / 2 - scrollY;
            if (i == selectedItemIndex)
                paint = selectedTextPaint;
            else
                paint = normalTextPaint;

            setTextPaint(paint, midY);

            canvas.drawText(text, (viewWidth - getTextWidth(paint, text)) / 2,
                    midY + getTextBaselineToCenter(paint), paint);
        }
    }

    //绘制选中item的背景和线条
    private void drawSelectedRectangle(Canvas canvas) {
        canvas.drawLine(0, itemHeight * halfItemNumber, viewWidth, itemHeight * halfItemNumber, selectedLinePaint);
        canvas.drawLine(0, itemHeight * (halfItemNumber + 1), viewWidth, itemHeight * (halfItemNumber + 1), selectedLinePaint);
        canvas.drawRect(0, itemHeight * halfItemNumber, viewWidth, itemHeight * (halfItemNumber + 1), selectedBackgroundPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Log.d(TAG, "onTouchEvent: " + event.getAction() + " " + event.getY());
        Message message;
        Bundle bundle;
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                handler.removeMessages(MyWheelViewHandler.RESILIENCE);
                lastY = event.getY();
                return true;
            case MotionEvent.ACTION_MOVE:
                scrollY -= event.getY() - lastY;
                lastY = event.getY();
                confirmSelectedItem();
                return true;
            case MotionEvent.ACTION_UP:
                message = handler.obtainMessage();
                message.what = MyWheelViewHandler.RESILIENCE;
                bundle = new Bundle();
                bundle.putFloat(RESILIENCE_DISTANCE_OF_ONCE, scrollY / RESILIENCE_TIMES);
                bundle.putInt(RESILIENCE_LEFT_TIMES, RESILIENCE_TIMES);
                message.setData(bundle);
                message.sendToTarget();
                return true;
        }
        return false;
    }

    /**
     * @return 该字串在width下 字串中间对齐控件中间时候的 drawText用的x
     */
    private float getTextWidth(Paint paint, String text) {
        return paint.measureText(text);
    }

    /**
     * @return 该字串在itemHeight下 字串中间对齐控件中间的baseLine的y
     */
    private float getTextBaselineToCenter(Paint paint) {
        Paint.FontMetricsInt fontMetrics = paint.getFontMetricsInt();
        return ((float) (- fontMetrics.bottom - fontMetrics.top)) / 2;
    }

    private void confirmSelectedItem() {
        //计算移动了几个item的height了, < 0说明向上， >0说明向下
        int changedItemNumber = Math.round(scrollY / itemHeight);

        int lastItem = getSelectedItemIndex();
        //计算这次的【合法的】的index
        int tempSelectedItem = getSelectedItemIndex() + changedItemNumber;
        if (tempSelectedItem < 0)
            tempSelectedItem = 0;
        if (tempSelectedItem >= data.size())
            tempSelectedItem = data.size() - 1;
        this.selectedItemIndex = tempSelectedItem;
        //减去相应的scrollY值（为了可以上滑和下滑超出）
        scrollY -= itemHeight * (selectedItemIndex - lastItem);
        invalidate();
        if (lastItem != tempSelectedItem)
            noticeListener();
    }

    private void setTextPaint(Paint paint, float midY) {
        paint.setTextSize(maxTextSize - (maxTextSize - minTextSize) * Math.abs(viewHeight / 2 - midY) / (viewHeight / 2) );
    }

    public int getSelectedItemIndex() {
        return selectedItemIndex;
    }


    /*
     * 这个是专门给外部类调用设置用的，类内不应该调用。 也是因为类内不用主动
     */
    public void setDataWithSelectedItemIndex(List<String> data, int selectedItemIndex) {
        this.data = data;
        setSelectedItemIndex(selectedItemIndex);
    }

    /*
     * 这个是专门给外部类调用设置用的，类内不应该调用。 也是因为类内不用主动
     */
    public void setSelectedItemIndex(int selectedItemIndex) {
        //外部自己负责处理这个index是否合法
        this.selectedItemIndex = selectedItemIndex;
        //既然外部设置index，就不要这个偏移量了
        this.scrollY = 0;
        invalidate();
        noticeListener();
    }

    private void resilienceToCenter(float distance) {
        scrollY -= distance;
        invalidate();
    }

    private void noticeListener() {
        if (null != wheelViewSelectedListener)
            wheelViewSelectedListener.wheelViewSelectedChanged(this, data, selectedItemIndex);
    }

    public void setWheelViewSelectedListener(IWheelViewSelectedListener wheelViewSelectedListener) {
        this.wheelViewSelectedListener = wheelViewSelectedListener;
    }

    private static class MyWheelViewHandler extends Handler {

        static final int RESILIENCE = 1;

        private WeakReference<MyWheelView> myWheelViewWeakReference;

        private MyWheelViewHandler(MyWheelView myWheelView) {
            myWheelViewWeakReference = new WeakReference<>(myWheelView);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            MyWheelView myWheelView;
            Message message;
            Bundle bundle;
            int leftTimes;
            switch (msg.what) {
                case RESILIENCE:
                    bundle = msg.getData();
                    leftTimes = bundle.getInt(RESILIENCE_LEFT_TIMES, 0);
                    if (leftTimes > 0) {

                        myWheelView = myWheelViewWeakReference.get();
                        if (null != myWheelView) {

                            myWheelView.resilienceToCenter(bundle.getFloat(RESILIENCE_DISTANCE_OF_ONCE, 0));

                            if (leftTimes > 1) {
                                bundle.putInt(RESILIENCE_LEFT_TIMES, leftTimes - 1);
                                message = new Message();
                                message.what = RESILIENCE;
                                message.setData(bundle);
                                this.sendMessageDelayed(message, RESILIENCE_TIME_INTERVAL);
                            }
                        }
                    }
                    break;
            }
        }
    }

}
