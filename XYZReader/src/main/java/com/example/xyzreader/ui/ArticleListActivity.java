package com.example.xyzreader.ui;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

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

import static com.example.xyzreader.ui.ArticleListFragment.EXTRA_ARTICLE_ID;


/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        ArticleListFragment.ArticleClickListener {

    private static final String TAG = ArticleListActivity.class.getSimpleName();
    private static final String SELECTED_POSITION = "selected_position";
    private final int DEFAULT_FRAGMENT_POSITION = 0;
    private boolean mIsTwoPane;
    private int mPosition;
    private ReaderViewModel mModel;
    private List<Book> mBooks;
    private FragmentManager mFragmentManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);
        mIsTwoPane = findViewById(R.id.details_fragment_container) != null;


        mFragmentManager = getSupportFragmentManager();
        if(!mIsTwoPane && savedInstanceState == null){
            addNewArticleListFragment();
        }else if(savedInstanceState == null){
            addNewArticleListFragment();
            addNewDetailsFragment();
        }else{
            //refreshFragments(savedInstanceState, fragmentManager);
        }

        setupViewModel();
    }

    private void setupViewModel() {
        mModel = ViewModelProviders.of(this).get(ReaderViewModel.class);

        mModel.getBooks().observe(this, new Observer<List<Book>>() {
            @Override
            public void onChanged(List<Book> books) {
                Log.d(TAG, "Updating books data set");
                mBooks = books;
                mModel.selectBook(mBooks.get(DEFAULT_FRAGMENT_POSITION));
            }
        });
    }

    private void addNewArticleListFragment() {
        Fragment fragment = new ArticleListFragment();
        mFragmentManager.beginTransaction()
                .add(R.id.list_fragment_container, fragment)
                .commit();
    }

    private void addNewDetailsFragment() {
        Fragment fragment = ArticleDetailFragment.newInstance(1);
        mFragmentManager.beginTransaction()
                .add(R.id.details_fragment_container, fragment)
                .commit();
    }

    private void replaceDetailFragment(int position){
        Fragment fragment = ArticleDetailFragment.newInstance(position);
        mFragmentManager.beginTransaction()
                .replace(R.id.details_fragment_container, fragment)
                .commit();
    }

    @Override
    public void onArticleClick(ArticleListFragment.ArticleViewHolder viewHolder) {

        if(!mIsTwoPane){
           openDetailActivityWithAnimation(viewHolder);
        }else {
            mModel.selectBook(mBooks.get(viewHolder.getAdapterPosition()));
            mPosition = viewHolder.getAdapterPosition();
            replaceDetailFragment(mPosition);
        }
    }

    private void openDetailActivityWithAnimation(ArticleListFragment.ArticleViewHolder viewHolder) {
        Intent intent = new Intent(this, ArticleDetailActivity.class);
        intent.putExtra(EXTRA_ARTICLE_ID, (long) viewHolder.getAdapterPosition());
        Bundle bundle = ActivityOptions
                .makeSceneTransitionAnimation(
                        this,
                        viewHolder.thumbnailView,
                        viewHolder.thumbnailView.getTransitionName())
                .toBundle();

        startActivity(intent, bundle);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(SELECTED_POSITION, mPosition);
        super.onSaveInstanceState(outState);
    }
}
