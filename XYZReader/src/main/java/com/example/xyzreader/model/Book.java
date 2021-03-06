package com.example.xyzreader.model;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.io.Serializable;

@Entity(tableName = "book")
public class Book implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String title;
    private String author;
    private String body;
    @ColumnInfo(name = "thumbnail_url")
    private String thumbnailUrl;
    @ColumnInfo(name = "photo_url")
    private String photoUrl;
    private float aspectRatio;
    @ColumnInfo(name = "published_date")
    private String publishedDate;


    @Ignore
    public Book(String title, String author, String body, String thumbnailUrl, String photoUrl, float aspectRatio, String publishedDate) {
        this.title = title;
        this.author = author;
        this.body = body;
        this.thumbnailUrl = thumbnailUrl;
        this.photoUrl = photoUrl;
        this.aspectRatio = aspectRatio;
        this.publishedDate = publishedDate;
    }

    public Book(int id, String title, String author, String body, String thumbnailUrl, String photoUrl, float aspectRatio, String publishedDate) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.body = body;
        this.thumbnailUrl = thumbnailUrl;
        this.photoUrl = photoUrl;
        this.aspectRatio = aspectRatio;
        this.publishedDate = publishedDate;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getAuthor() {
        return author;
    }

    public String getBody() {
        return body;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public float getAspectRatio() {
        return aspectRatio;
    }

    public String getPublishedDate() {
        return publishedDate;
    }

}
