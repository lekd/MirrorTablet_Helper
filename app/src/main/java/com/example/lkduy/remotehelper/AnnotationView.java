package com.example.lkduy.remotehelper;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lkduy on 4/8/2017.
 */
public class AnnotationView extends View {
    int curId = 0;
    private Paint paint = new Paint();
    List<PathWithTime> existingPaths = new ArrayList<PathWithTime>();
    PathWithTime onGoingPath = new PathWithTime();
    private   IPointingEventTriggered pointingEventHandler = null;
    public  void setPointingEventHandler(IPointingEventTriggered eventhandler){
        pointingEventHandler = eventhandler;
    }
    public AnnotationView(Context ctx){
        super(ctx);
        this.setBackgroundColor(Color.TRANSPARENT);
        initializePaint();
        schedulingToRefresh();
    }
    public AnnotationView(Context context, AttributeSet attrs) {
        super(context);
        // TODO Auto-generated constructor stub
        this.setBackgroundColor(Color.TRANSPARENT);
        initializePaint();
        schedulingToRefresh();
    }

    void schedulingToRefresh(){
        final android.os.Handler handler = new android.os.Handler(Looper.getMainLooper());
        Runnable runnableRefresh = new Runnable() {
            @Override
            public void run() {
                invalidate();
                handler.postDelayed(this,100);
            }
        };
        handler.postDelayed(runnableRefresh,100);

    }
    void initializePaint(){
        paint.setAntiAlias(true);
        paint.setStrokeWidth(10f);
        paint.setColor(Color.CYAN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeJoin(Paint.Join.ROUND);
    }
    @Override
    protected void onDraw(Canvas canvas){
        long curTimeStamp = System.currentTimeMillis();
        List<PathWithTime> unvalidPaths = new ArrayList<PathWithTime>();
        for(int i=0; i< existingPaths.size(); i++){
            PathWithTime curPath = existingPaths.get(i);
            long elapsedTime = curTimeStamp - curPath.get_timeStamp();
            if(elapsedTime > 2000){
                unvalidPaths.add(curPath);
                continue;
            }
            int opacity = (int)((1.0 - (elapsedTime - 1000)*1.0/1000)*255);
            opacity = opacity>255 ? 255:opacity;
            paint.setColor(Color.argb(opacity,0,255,255));
            canvas.drawPath(curPath.getPath(),paint);
        }
        paint.setColor(Color.CYAN);
        if(onGoingPath != null) {
            canvas.drawPath(onGoingPath.getPath(), paint);
            for (int i = 0; i < unvalidPaths.size(); i++) {
                existingPaths.remove(unvalidPaths.get(i));
            }
        }
    }
    @Override
    public boolean onTouchEvent(MotionEvent event){
        float eventX = event.getX();
        float eventY = event.getY();
        Point p = new Point((int)eventX, (int)eventY);
        switch(event.getAction()){
            case MotionEvent.ACTION_DOWN:
                onGoingPath = new PathWithTime();
                onGoingPath.set_id(curId++);
                onGoingPath.getPath().moveTo(eventX,eventY);
                onGoingPath.getPath().lineTo(eventX,eventY);

                break;
            case MotionEvent.ACTION_MOVE:
                onGoingPath.getPath().lineTo(eventX,eventY);
                break;
            case MotionEvent.ACTION_UP:
                onGoingPath.set_timeStamp(System.currentTimeMillis());
                existingPaths.add(onGoingPath);
                break;
        }
        //invalidate();
        if(pointingEventHandler != null){
            PointF relativePos = getRelativeLocation(eventX,eventY);
            pointingEventHandler.HandlePointingEvent(onGoingPath.get_id(),relativePos.x, relativePos.y,event.getAction());
        }
        if(event.getAction() == MotionEvent.ACTION_UP){
            onGoingPath = null;
        }
        return true;
    }
    PointF getRelativeLocation(float X, float Y){
        int viewW = this.getWidth();
        int viewH = this.getHeight();
        return  new PointF(X/viewW, Y/viewH);
    }
}
interface IPointingEventTriggered{
    void HandlePointingEvent(int pathID, float x, float y, int eventType);
}
