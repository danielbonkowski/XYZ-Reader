package com.example.xyzreader.model;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface BookDao {

    @Query("SELECT * FROM book ORDER BY published_date DESC")
    List<Book> loadAllBooks();

    @Insert
    void insertBook(Book book);

    @Update(onConflict = OnConflictStrategy.REPLACE)
    void updateBook(Book book);

    @Delete
    void deleteBook(Book book);
}
