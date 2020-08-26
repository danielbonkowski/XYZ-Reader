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
            float distanceX = motionEvent2.getX() - motionEvent1.getX();
            float distanceY = motionEvent2.getY() - motionEvent1.getY();
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
