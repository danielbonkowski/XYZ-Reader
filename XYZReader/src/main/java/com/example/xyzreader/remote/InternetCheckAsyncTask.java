package com.example.xyzreader.remote;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;

public class InternetCheckAsyncTask extends AsyncTask<Context, Void, Boolean> {

    private ShowConnectionError mShowConnectionError;

    public InternetCheckAsyncTask(ShowConnectionError listener){
        mShowConnectionError = listener;
    }

    public interface ShowConnectionError{
        public void showError();
    }

    @Override
    protected Boolean doInBackground(Context... contexts) {
        Context context = contexts[0];
        ConnectivityManager connectivityManager
                = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnectedOrConnecting();
    }

    @Override
    protected void onPostExecute(Boolean isConnected) {
        if(!isConnected){
            mShowConnectionError.showError();
        }
    }
}
