package com.example.xyzreader.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.xyzreader.R;
import com.example.xyzreader.data.UpdaterService;
import com.example.xyzreader.model.AppDatabase;
import com.example.xyzreader.model.Book;
import com.example.xyzreader.model.ReaderViewModel;
import com.google.android.material.appbar.CollapsingToolbarLayout;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity /*implements
        androidx.loader.app.LoaderManager.LoaderCallbacks<Cursor>*/ {

    private static final String TAG = ArticleListActivity.class.toString();
    public static final String EXTRA_ARTICLE_ID = "extra_article_id";
    private CollapsingToolbarLayout mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private Adapter mAdapter;
    private boolean mIsRefreshing = false;
    private int mAnimatedViewPosition = -1;
    private AppDatabase mDb;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        mToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsingToolbar);
        mDb = AppDatabase.getInstance(getApplicationContext());


        final View toolbarContainerView = findViewById(R.id.appBar);
        //final LoaderManager.LoaderCallbacks context = this;


        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setProgressViewOffset(false, 50, 50);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.theme_accent);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //getSupportLoaderManager().restartLoader(0, null, context);

                mAdapter.notifyDataSetChanged();
                mIsRefreshing = false;
                updateRefreshingUI();
            }
        });




        mRecyclerView = (RecyclerView) findViewById(R.id.books_recycler_view);
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int adapterPosition =
                        (recyclerView == null || recyclerView.getChildCount() == 0) ? 0 : recyclerView.getChildAt(0).getTop();
                mSwipeRefreshLayout.setEnabled(adapterPosition >= 0);
            }
        });


        if (savedInstanceState == null) {
            refresh();
        }

        mAdapter = new Adapter(null);
        mAdapter.setHasStableIds(true);
        mRecyclerView.setAdapter(mAdapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        StaggeredGridLayoutManager sglm =
                new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(sglm);

        setupViewModel();

        //getSupportLoaderManager().initLoader(0, null, this);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void setupViewModel() {
        ReaderViewModel viewModel = ViewModelProviders.of(this).get(ReaderViewModel.class);
        viewModel.getBooks().observe(this, new Observer<List<Book>>() {
            @Override
            public void onChanged(List<Book> books) {
                Log.d(TAG, "Updating list of books from LiveData in ViewModel");
                mAdapter.setBooks(books);
            }
        });
    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mRefreshingReceiver);
    }



    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
    }



    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private List<Book> mBooks;
        private int mPosition;

        public Adapter(List<Book> books) {
            mBooks = books;
        }

        public void setBooks(List<Book> books){
            mBooks = books;
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public long getItemId(int position) {
            mPosition = position;
            return  mBooks.get(position).getId();
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(getApplicationContext(), ArticleDetailActivity.class);
                    intent.putExtra(EXTRA_ARTICLE_ID, (long) vh.getAdapterPosition());
                    startActivity(intent);
                }
            });
            return vh;
        }

        private Date parsePublishedDate() {
            try {
                String date = mBooks.get(mPosition).getPublishedDate();
                return dateFormat.parse(date);
            } catch (ParseException ex) {
                Log.e(TAG, ex.getMessage());
                Log.i(TAG, "passing today's date");
                return new Date();
            }
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Book book = mBooks.get(position);
            holder.titleView.setText(book.getTitle());
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {

                holder.subtitleView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + "<br/>" + " by "
                                + book.getAuthor()));
            } else {
                holder.subtitleView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate)
                        + "<br/>" + " by "
                        + book.getAuthor()));
            }
            holder.thumbnailView.setImageUrl(
                    book.getThumbnailUrl(),
                    ImageLoaderHelper.getInstance(ArticleListActivity.this).getImageLoader());
            holder.thumbnailView.setAspectRatio(book.getAspectRatio());

        }


        @Override
        public int getItemCount() {
            if(mBooks == null){
                return 0;
            }
            return mBooks.size();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public DynamicHeightNetworkImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = (DynamicHeightNetworkImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
        }
    }
}
