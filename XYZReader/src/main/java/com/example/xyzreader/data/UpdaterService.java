package com.example.xyzreader.data;

import android.app.IntentService;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.text.format.Time;
import android.util.Log;

import com.example.xyzreader.model.AppDatabase;
import com.example.xyzreader.model.AppExecutors;
import com.example.xyzreader.model.Book;
import com.example.xyzreader.remote.RemoteEndpointUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class UpdaterService extends IntentService {
    private static final String TAG = "UpdaterService";

    public static final String BROADCAST_ACTION_STATE_CHANGE
            = "com.example.xyzreader.intent.action.STATE_CHANGE";
    public static final String EXTRA_REFRESHING
            = "com.example.xyzreader.intent.extra.REFRESHING";
    public static final String EXTRA_SHOW_SNACKBAR =
            "com.example.xyzreader.intent.extra.EXTRA_SHOW_SNACKBAR";



    AppDatabase mDb;
    boolean mConnectionError = false;

    public UpdaterService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Time time = new Time();

        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null || !ni.isConnected()) {
            Log.w(TAG, "Not online, not refreshing.");
            return;
        }
        mDb = AppDatabase.getInstance(getApplicationContext());

        sendStickyBroadcast(new Intent(BROADCAST_ACTION_STATE_CHANGE).putExtra(EXTRA_REFRESHING, true));

        // Don't even inspect the intent, we only do one thing, and that's fetch content.


        try {
            JSONArray array = RemoteEndpointUtil.fetchJsonArray();
            if (array == null) {
                throw new JSONException("Invalid parsed item array" );
            }

            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.getJSONObject(i);

                String fullText = object.getString("body");
                fullText = fullText.replaceAll("\r\n\r\n", "\n\n");
                fullText = fullText.replaceAll("\r\n", "");
                fullText = fullText.replaceAll(" {2}", " ");
                fullText = fullText.trim();



                final Book book = new Book(Integer.parseInt(object.getString("id" )), object.getString("title" ),
                        object.getString("author"),fullText, object.getString("thumb" ),
                        object.getString("photo" ), Float.parseFloat(object.getString("aspect_ratio" )),
                        object.getString("published_date" ));

                AppExecutors.getInstance().diskIO().execute(new Runnable() {
                    @Override
                    public void run() {
                        mDb.bookDao().insertBook(book);
                    }
                });
            }

        } catch (JSONException e) {
            Log.e(TAG, "Error updating content.", e);
            mConnectionError = true;
        }finally {
            sendStickyBroadcast(new Intent(BROADCAST_ACTION_STATE_CHANGE)
                    .putExtra(EXTRA_REFRESHING, false)
                    .putExtra(EXTRA_SHOW_SNACKBAR, mConnectionError));
        }
    }
}