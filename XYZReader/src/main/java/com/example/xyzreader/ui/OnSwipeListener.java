package com.example.xyzreader.ui;

import android.content.Context;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

public class OnSwipeListener implements View.OnTouchListener {

    private final GestureDetector gestureDetector;

    public OnSwipeListener(Context context) {
        this.gestureDetector = new GestureDetector(context, new GestureListener());
    }

    public void leftSwipe(){

    }

    public void rightSwipe(){

    }

    public void upSwipe() {
    }

    public void downSwipe() {
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        if(gestureDetector == null){
            return false;
        }
        return gestureDetector.onTouchEvent(motionEvent);
    }

    public class GestureListener extends GestureDetector.SimpleOnGestureListener {

        private static final int TRIGGER_AFTER_SWIPED_DISTANCE = 100;
        private static final int TRIGGER_AFTER_SWIPED_SPEED = 100;
        private final String TAG = GestureListener.class.getSimpleName();


        @Override
        public boolean onDown(MotionEvent motionEvent) {
            return true;
        }

        @Override
        public boolean onFling(MotionEvent motionEvent1, MotionEvent motionEvent2, float speedX, float speedY) {
            float x1;
            float y1;
            float x2;
            float y2;

            if(motionEvent1 == null && motionEvent2 == null){
                return false;
            }

            if(motionEvent1 == null){
                x1 = motionEvent2.getX();
                y1 = motionEvent2.getY();
            }else {
                x1 = motionEvent1.getX();
                y1 = motionEvent1.getY();
            }

            if(motionEvent2 == null){
                x2 = motionEvent1.getX();
                y2 = motionEvent1.getY();
            }else {
                x2 = motionEvent2.getX();
                y2 = motionEvent2.getY();
            }

            float distanceX = x2 - x1;
            float distanceY = y2 - y1;
            Log.d(TAG, "Swipe fling");
            if(Math.abs(distanceX) > Math.abs(distanceY) && Math.abs(distanceX) >TRIGGER_AFTER_SWIPED_DISTANCE
                    && Math.abs(speedX) > TRIGGER_AFTER_SWIPED_SPEED){
                if(distanceX > 0){
                    rightSwipe();
                }else {
                    leftSwipe();
                }
                return true;
            }else if (Math.abs(distanceY) > Math.abs(distanceX)){
                if(distanceY > 0){
                    downSwipe();
                }else {
                    upSwipe();
                }
                return true;
            }
            return false;
        }
    }

}
