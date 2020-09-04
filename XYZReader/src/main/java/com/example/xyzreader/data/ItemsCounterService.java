package com.example.xyzreader.data;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

import java.util.Objects;

public class ItemsCounterService extends IntentService {

    private static final String TAG = "ItemsCounterService";

    public static final String BROADCAST_ACTION_ITEMS_COUNTER
            = "com.example.xyzreader.intent.action.ITEMS_COUNTER";
    public static final String EXTRA_SETTING_UP_COUNTER
            = "com.example.xyzreader.intent.extra.SETTING_UP_COUNTER";

    public ItemsCounterService(){
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

            Bundle bundle = Objects.requireNonNull(intent).getExtras();
            String bookBody = Objects.requireNonNull(bundle).getString(EXTRA_SETTING_UP_COUNTER);
            String[] bookBodyArray = Objects.requireNonNull(bookBody).split("\n\n");

            sendStickyBroadcast(new Intent(BROADCAST_ACTION_ITEMS_COUNTER).putExtra(EXTRA_SETTING_UP_COUNTER, bookBodyArray));

    }
}
