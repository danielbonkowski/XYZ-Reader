package com.example.xyzreader.ui;

import android.os.Build;
import android.os.Bundle;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowInsets;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.example.xyzreader.R;
import com.example.xyzreader.model.Book;
import com.example.xyzreader.model.ReaderViewModel;

import java.util.List;
import java.util.Objects;

import static com.example.xyzreader.ui.ArticleListFragment.EXTRA_ARTICLE_ID;

/**
 * An activity representing a single Article detail screen, letting you swipe between articles.
 */
public class ArticleDetailActivity extends AppCompatActivity
        implements
ArticleDetailFragment.SwipeListener{

    private final String TAG = ArticleDetailActivity.class.getSimpleName();
    private final String STATE_SELECTED_ITEM_ID = "selected_item_id";


    private long mSelectedFragmentId;
    private ReaderViewModel mModel;
    private List<Book> mBooks;

    private int mSelectedItemUpButtonFloor = Integer.MAX_VALUE;
    private int mTopInset;

    private View mUpButtonContainer;
    private View mUpButton;

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

        mUpButtonContainer = findViewById(R.id.up_container);

        mUpButton = findViewById(R.id.action_up);
        mUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSupportNavigateUp();
            }
        });


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
            addDetailsFragment();

        }else{
            mSelectedFragmentId = savedInstanceState.getLong(STATE_SELECTED_ITEM_ID);
        }


        setupViewModel();
    }

    private void addDetailsFragment() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment fragment = ArticleDetailFragment.newInstance(mSelectedFragmentId);
        fragmentManager.beginTransaction()
                .add(R.id.details_fragment_container, fragment)
                .commit();
    }

    private void setupViewModel() {
        mModel = ViewModelProviders.of(this).get(ReaderViewModel.class);

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

    public void onUpButtonFloorChanged(ArticleDetailFragment fragment) {
        mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
        updateUpButtonPosition();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finishAfterTransition();
        return true;
    }

    private void updateUpButtonPosition() {
        int upButtonNormalBottom = mTopInset + mUpButton.getHeight();
        mUpButton.setTranslationY(Math.min(mSelectedItemUpButtonFloor - upButtonNormalBottom, 20));
    }

    @Override
    public void swipeRight() {
        Log.d(TAG, "Swipe right");
        if(mSelectedFragmentId > 0){
            --mSelectedFragmentId;
            replaceFragmentWithAnimation(Gravity.LEFT, R.anim.fragment_enter_right, R.anim.fragment_exit_right,
                    R.anim.fragment_enter_left, R.anim.fragment_exit_left);
        }
    }

    @Override
    public void swipeLeft() {
        Log.d(TAG, "Swipe left");
        if(mBooks.size() - 1 > mSelectedFragmentId){
            ++mSelectedFragmentId;
            replaceFragmentWithAnimation(Gravity.RIGHT, R.anim.fragment_enter_left, R.anim.fragment_exit_left,
                    R.anim.fragment_enter_right, R.anim.fragment_exit_right);
        }
    }


    private void replaceFragmentWithAnimation(int left, int p, int p2, int p3, int p4) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        mModel.selectBook(Objects.requireNonNull(mModel.getBooks().getValue()).get((int) mSelectedFragmentId));
        ArticleDetailFragment fragment = ArticleDetailFragment.newInstance(mSelectedFragmentId);
        mSelectedItemUpButtonFloor = fragment.getUpButtonFloor();
        updateUpButtonPosition();

        Slide exitTransition = new Slide();
        exitTransition.setSlideEdge(left);
        exitTransition.setDuration(300);
        getWindow().setExitTransition(exitTransition);

        fragmentManager.beginTransaction()
                .setCustomAnimations(p,
                        p2,
                        p3,
                        p4)
                .replace(R.id.details_fragment_container, fragment)
                .commit();
    }
}
