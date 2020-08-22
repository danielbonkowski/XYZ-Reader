package com.example.xyzreader.ui;

import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.loader.app.LoaderManager;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.model.Book;
import com.example.xyzreader.model.ReaderViewModel;

import java.util.List;

import static com.example.xyzreader.ui.ArticleListFragment.EXTRA_ARTICLE_ID;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements
ArticleDetailFragment.SwipeListener{

    private final String STATE_SELECTED_ITEM_ID = "selected_item_id";


    private long mSelectedFragmentId;
    private ReaderViewModel mModel;
    private Book mBook;
    private List<Book> mBooks;

    private final String TAG = ArticleDetailActivity.class.getSimpleName();
    private int mSelectedItemUpButtonFloor = Integer.MAX_VALUE;
    private int mTopInset;
    private final long DEFAULT_BOOK_ID = 1;

    private View mUpButtonContainer;
    private View mUpButton;
    private FrameLayout mFragmentContainer;

    @Override
    public void onAttachFragment(@NonNull Fragment fragment) {
        if (fragment instanceof ArticleDetailFragment) {
            ArticleDetailFragment articleDetailFragment = (ArticleDetailFragment) fragment;
            articleDetailFragment.setSwipeListener(this);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                            View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        }
        setContentView(R.layout.activity_article_detail);

        Intent intent = getIntent();
        long bookId = intent.getLongExtra(EXTRA_ARTICLE_ID, DEFAULT_BOOK_ID);




        mUpButtonContainer = findViewById(R.id.up_container);

        mUpButton = findViewById(R.id.action_up);
        mUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSupportNavigateUp();
            }
        });

        mFragmentContainer = findViewById(R.id.details_fragment_container);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mUpButtonContainer.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View view, WindowInsets windowInsets) {
                    view.onApplyWindowInsets(windowInsets);
                    mTopInset = windowInsets.getSystemWindowInsetTop();
                    mUpButtonContainer.setTranslationY(mTopInset);
                    updateUpButtonPosition();
                    return windowInsets;
                }
            });
        }

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getExtras() != null) {
                mSelectedFragmentId = getIntent().getLongExtra(EXTRA_ARTICLE_ID, 2);
            }
        }else{
            mSelectedFragmentId = savedInstanceState.getLong(STATE_SELECTED_ITEM_ID);
        }



        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = ArticleDetailFragment.newInstance(mSelectedFragmentId);
        fragmentManager.beginTransaction()
                .add(R.id.details_fragment_container, fragment)
                .commit();

        setupViewModel();
    }

    private void setupViewModel() {
        mModel = ViewModelProviders.of(this).get(ReaderViewModel.class);
        mModel.getSelectedBook().observe(this, new Observer<Book>() {
            @Override
            public void onChanged(Book book) {
                Log.d(TAG, "Updating selected book");
                mBook = book;
            }
        });

        mModel.getBooks().observe(this, new Observer<List<Book>>() {
            @Override
            public void onChanged(List<Book> books) {
                Log.d(TAG, "Updating books data set");

                mBooks = books;
                mModel.selectBook(books.get((int)mSelectedFragmentId));
            }
        });
    }


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putLong(STATE_SELECTED_ITEM_ID, mSelectedFragmentId);

        super.onSaveInstanceState(outState);
    }

    public void onUpButtonFloorChanged(long itemId, ArticleDetailFragment fragment) {
        mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
        updateUpButtonPosition();
    }

    private void updateUpButtonPosition() {
        int upButtonNormalBottom = mTopInset + mUpButton.getHeight();
        mUpButton.setTranslationY(Math.min(mSelectedItemUpButtonFloor - upButtonNormalBottom, 20));
    }

    @Override
    public void swipeRight() {
        Log.d(TAG, "Swipe left");
        if(mSelectedFragmentId > 0){
            --mSelectedFragmentId;
            FragmentManager fragmentManager = getSupportFragmentManager();
            mModel.selectBook(mModel.getBooks().getValue().get((int)mSelectedFragmentId));
            ArticleDetailFragment fragment = ArticleDetailFragment.newInstance(mBook.getId());
            mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
            fragmentManager.beginTransaction()
                    .replace(R.id.details_fragment_container, fragment)
                    .commit();
        }
    }

    @Override
    public void swipeLeft() {
        Log.d(TAG, "Swipe left");
        if(mBooks.size() - 1 > mSelectedFragmentId){
            ++mSelectedFragmentId;
            FragmentManager fragmentManager = getSupportFragmentManager();
            mModel.selectBook(mModel.getBooks().getValue().get((int) mSelectedFragmentId) );
            ArticleDetailFragment fragment = ArticleDetailFragment.newInstance(mBook.getId());
            mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
            updateUpButtonPosition();
            fragmentManager.beginTransaction()
                    .replace(R.id.details_fragment_container, fragment)
                    .commit();
        }
    }
}
