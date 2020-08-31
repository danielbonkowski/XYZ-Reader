package com.example.xyzreader.ui;

import android.app.ActivityOptions;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.example.xyzreader.R;


/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        ArticleListFragment.ArticleClickListener {
    private boolean mIsTwoPane;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);
        mIsTwoPane = findViewById(R.id.details_fragment_container) != null;

        FragmentManager fragmentManager = getSupportFragmentManager();
        if(!mIsTwoPane && savedInstanceState == null){
            addNewArticleListFragment(fragmentManager);
        }else if(savedInstanceState == null){
            addNewArticleListFragment(fragmentManager);
            addNewDetailsFragment(fragmentManager);
        }else{
            //refreshFragments(savedInstanceState, fragmentManager);
        }


    }

    private void addNewArticleListFragment(FragmentManager fragmentManager) {
        Fragment fragment = new ArticleListFragment();
        fragmentManager.beginTransaction()
                .add(R.id.list_fragment_container, fragment)
                .commit();
    }

    private void addNewDetailsFragment(FragmentManager fragmentManager) {
       /* Intent intent = new Intent(getActivity().getApplicationContext(), ArticleDetailActivity.class);
        intent.putExtra(EXTRA_ARTICLE_ID, (long) vh.getAdapterPosition());
        Bundle bundle = ActivityOptions
                .makeSceneTransitionAnimation(
                        getActivity(),
                        vh.thumbnailView,
                        vh.thumbnailView.getTransitionName())
                .toBundle();

        startActivity(intent, bundle);*/


            Fragment fragment = new ArticleDetailFragment();
            fragmentManager.beginTransaction()
                    .add(R.id.list_fragment_container, fragment)
                    .commit();
    }

    @Override
    public void onArticleClick() {
        
    }
}
