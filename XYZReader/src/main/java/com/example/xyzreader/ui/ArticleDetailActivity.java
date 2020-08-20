package com.example.xyzreader.ui;

import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.loader.app.LoaderManager;
import androidx.viewpager.widget.ViewPager;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor>,
ArticleDetailFragment.SwipeListener{

    private Cursor mCursor;
    private long mSelectedFragmentId;

    private final String TAG = ArticleDetailActivity.class.getSimpleName();
    private long mSelectedItemId;
    private int mSelectedItemUpButtonFloor = Integer.MAX_VALUE;
    private int mTopInset;

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

        getSupportLoaderManager().initLoader(1, null, this);

        mUpButtonContainer = findViewById(R.id.up_container);

        mUpButton = findViewById(R.id.action_up);
        mUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSupportNavigateUp();
            }
        });

        mFragmentContainer = findViewById(R.id.fragment_container);


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
                mSelectedFragmentId = getIntent().getLongExtra(ArticleListActivity.EXTRA_ARTICLE_ID, 2);
                mSelectedItemId = mSelectedFragmentId;
            }
        }
    }

    @Override
    public androidx.loader.content.Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(@NonNull androidx.loader.content.Loader<Cursor> loader, Cursor cursor) {
        mCursor = cursor;

        // Select the start ID
        if (mSelectedFragmentId >= 0) {
            FragmentManager fragmentManager = getSupportFragmentManager();
            mCursor.moveToPosition((int) mSelectedFragmentId);
            Fragment fragment = ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID));
            fragmentManager.beginTransaction()
                    .add(R.id.fragment_container , fragment)
                    .commit();

        }
    }

    @Override
    public void onLoaderReset(@NonNull androidx.loader.content.Loader<Cursor> loader) {
        mCursor = null;
    }



    public void onUpButtonFloorChanged(long itemId, ArticleDetailFragment fragment) {
        if (itemId == mSelectedItemId) {
            mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
            updateUpButtonPosition();
        }
    }

    private void updateUpButtonPosition() {
        int upButtonNormalBottom = mTopInset + mUpButton.getHeight();
        mUpButton.setTranslationY(Math.min(mSelectedItemUpButtonFloor - upButtonNormalBottom, 10));
    }

    @Override
    public void swipeRight() {
        Log.d(TAG, "Swipe left");
        if(mSelectedFragmentId > 0){
            --mSelectedFragmentId;
            FragmentManager fragmentManager = getSupportFragmentManager();
            mCursor.moveToPosition((int) mSelectedFragmentId);
            ArticleDetailFragment fragment = ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID));
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container , fragment)
                    .commit();
        }
    }

    @Override
    public void swipeLeft() {
        Log.d(TAG, "Swipe left");
        if(mCursor.getCount() - 1 > mSelectedFragmentId){
            ++mSelectedFragmentId;
            FragmentManager fragmentManager = getSupportFragmentManager();
            mCursor.moveToPosition((int) mSelectedFragmentId);
            ArticleDetailFragment fragment = ArticleDetailFragment.newInstance(mCursor.getLong(ArticleLoader.Query._ID));
            mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
            onUpButtonFloorChanged(mSelectedItemId, fragment);
            updateUpButtonPosition();
            fragmentManager.beginTransaction()
                    .replace(R.id.fragment_container , fragment)
                    .commit();
        }
    }
}
