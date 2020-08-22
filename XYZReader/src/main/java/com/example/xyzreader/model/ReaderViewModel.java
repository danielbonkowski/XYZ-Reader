package com.example.xyzreader.model;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class ReaderViewModel extends AndroidViewModel {

    private static final String TAG = ReaderViewModel.class.getSimpleName();

    private LiveData<List<Book>> books;

    public ReaderViewModel(@NonNull Application application) {
        super(application);
        AppDatabase database = AppDatabase.getInstance(this.getApplication());
        Log.d(TAG, "Actively retrieving the books from the database");
        books = database.bookDao().loadAllBooks();
    }

    public LiveData<List<Book>> getBooks(){
        return books;
    }
}
