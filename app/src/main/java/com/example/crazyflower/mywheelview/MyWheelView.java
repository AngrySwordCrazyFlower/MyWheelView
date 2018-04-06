package com.example.crazyflower.mywheelview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by CrazyFlower on 2018/4/2.
 */

public class MyWheelView extends View {

    private static final String TAG = "MyWheelView";

    private List<String> data;

    private float lastY = 0;
    private float scrollY = 0;

    private int viewWidth = 0;
    private int viewHeight = 0;
    private int itemHeight = 0;

    private boolean isTouch = false;

    private int currentItemIndex = 0;

    Paint linePaint;
    Paint textPaint;

    private Handler handler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            invalidate();
        }
    };

    private Timer timer = new Timer();

    public MyWheelView(Context context) {
        this(context, null);
    }

    public MyWheelView(Context context, AttributeSet attrs) {
        this(context, attrs, 1);
    }

    public MyWheelView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initDataAndPaint();
    }

    private void initDataAndPaint() {
        Log.d(TAG, "initData: ");
        data = new ArrayList<>();
        data.add("1");
        data.add("2");
        data.add("3");
        data.add("4");
        data.add("这个5555比较长");
        data.add("6");
        data.add("7");
        data.add("8");
        currentItemIndex = 0;

        linePaint = new Paint();
        textPaint = new Paint();
        linePaint.setColor(0xffcccccc);
        linePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        linePaint.setStrokeWidth(4);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int width = 0;
        int height = 0;

        switch (widthMode) {
            case MeasureSpec.EXACTLY:
                width = MeasureSpec.getSize(widthMeasureSpec);
                break;
            case MeasureSpec.AT_MOST:
                width = 100;
                break;
            case MeasureSpec.UNSPECIFIED:
                width = 100;
                break;
        }

        switch (heightMode) {
            case MeasureSpec.EXACTLY:
                height = MeasureSpec.getSize(heightMeasureSpec);
                break;
            case MeasureSpec.AT_MOST:
                height = 100;
                break;
            case MeasureSpec.UNSPECIFIED:
                height = 100;
                break;
        }

        viewWidth = width;
        viewHeight = height;
        itemHeight = height / 5;

        setMeasuredDimension(width, height);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (null == data) {
            return;
        }

        String text = "";
        float topY = 0;

        for (int i = 0, length = data.size(); i < length; i++) {
            topY = (i - currentItemIndex + 2) * itemHeight + scrollY;
            text = data.get(i);
            setTextPaint(topY - viewHeight * 2 / 5);
            canvas.drawText(text, getMidText(textPaint, text, viewWidth), getBaseLine(textPaint, topY, itemHeight), textPaint);
        }

        for (int count = 2; count < 4; count++) {
            canvas.drawLine(0, itemHeight * count, viewWidth, itemHeight * count, linePaint);
        }

    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isTouch = true;
                lastY = event.getY();
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                scrollY = scrollY + event.getY() - lastY;
                lastY = event.getY();
                confirmCurrentItem(1);
                break;
            case MotionEvent.ACTION_UP:
                scrollY = scrollY + event.getY() - lastY;
                lastY = 0;
                isTouch = false;
                confirmCurrentItem(2);
                break;
        }
        return true;
    }


    /**
     * @return 该字串在width下 中间对齐中间的x
     */
    private float getMidText(Paint paint, String text, float width) {
        float fontWidth = paint.measureText(text);
        return (width - fontWidth) / 2;
    }

    /**
     * @return 该字串在itemheight下 中间对齐中间的y 该函数copy by https://blog.csdn.net/junzia/article/details/50979382
     */
    private float getBaseLine(Paint paint, float top, float height) {
        Paint.FontMetricsInt fontMetrics = paint.getFontMetricsInt();
        return  (2*top+height - fontMetrics.bottom - fontMetrics.top) / 2;
    }

    private void confirmCurrentItem(int level) {
        timer.cancel();
        float topY = 0;
        float min = 0x10000000;
        int index = 0;
        for (int i = 0, length = data.size(); i < length; i++) {
            topY = (i - currentItemIndex + 2) * itemHeight + scrollY;
            if (Math.abs((topY + itemHeight / 2) - viewHeight / 2) < Math.abs(min)) {
                min = (topY + itemHeight / 2) - viewHeight / 2;
                index = i;
            }
        }
        scrollY -=itemHeight * (currentItemIndex - index);
        currentItemIndex = index;
        if (level == 1) {
            invalidate();
            return;
        }

        final float finalMin = min;

        TimerTask task = new TimerTask() {
            int times = 0;
            float reduceDistance = finalMin / 5;

            @Override
            public void run() {
                times++;
                if (times == 6)  {
                    scrollY = 0;
                    super.cancel();
                    return;
                }
                scrollY -= reduceDistance;
                Log.d(TAG, "run: " + times);
                handler.sendEmptyMessage(0);
            }
        };
        timer = new Timer();
        timer.schedule(task, 0, 60);
    }

    private void setTextPaint(float distance) {
        distance = Math.abs(distance);
        int color = (int) (255 - (distance * 62.4 / viewHeight));
        textPaint.setColor(color * 0x1000000);
        float textSize = (float) (0.6 * itemHeight - distance / 10);
        textPaint.setTextSize(textSize);
    }

    public int getCurrentIndex() {
        return currentItemIndex;
    }

    public String getCurrentText() {
        return data.get(currentItemIndex);
    }

    public void addData(String text) {
        if ( null == data)
            data = new ArrayList<>();
        data.add(text);
        invalidate();
    }

    public void setData(List<String> data) {
        this.data = data;
        invalidate();
    }
}
