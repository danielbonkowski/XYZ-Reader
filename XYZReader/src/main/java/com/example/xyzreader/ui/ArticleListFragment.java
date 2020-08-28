package com.example.xyzreader.ui;

import android.app.ActivityOptions;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.loader.content.AsyncTaskLoader;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.UpdaterService;
import com.example.xyzreader.model.AppDatabase;
import com.example.xyzreader.model.Book;
import com.example.xyzreader.model.ReaderViewModel;
import com.example.xyzreader.remote.InternetCheckAsyncTask;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.snackbar.Snackbar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

public class ArticleListFragment extends Fragment
implements InternetCheckAsyncTask.ShowConnectionError{

    private static final String STATE_CHECK_CONNECTION = "check_internet_connection";
    View mRootView;
    private static final String TAG = ArticleListActivity.class.toString();
    public static final String EXTRA_ARTICLE_ID = "extra_article_id";

    private CollapsingToolbarLayout mToolbar;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mRecyclerView;
    private Adapter mAdapter;
    private ReaderViewModel mModel;
    private boolean mIsRefreshing = false;
    private int mAnimatedViewPosition = -1;
    private AppDatabase mDb;
    private ImageView mSharedImageView;
    private boolean mConnectionError = false;
    private CoordinatorLayout mCoordinatorLayout;
    private int mLoadedImagesCounter = 0;
    private boolean mCheckInternetConnection = true;
    private StaggeredGridLayoutManager mLayoutManager;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);

    public ArticleListFragment() {
    }



    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_article_list, container, false);


        mToolbar = (CollapsingToolbarLayout) mRootView.findViewById(R.id.collapsingToolbar);
        mDb = AppDatabase.getInstance(getActivity().getApplicationContext());

        if(savedInstanceState != null){
           mCheckInternetConnection = savedInstanceState.getBoolean(STATE_CHECK_CONNECTION);
        }

        if(mCheckInternetConnection){
            new InternetCheckAsyncTask(this).execute(this.getContext());
        }

        mCoordinatorLayout = mRootView.findViewById(R.id.coordinator_layout);

        final View toolbarContainerView = mRootView.findViewById(R.id.appBar);
        //final LoaderManager.LoaderCallbacks context = this;


        mSwipeRefreshLayout = (SwipeRefreshLayout) mRootView.findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setProgressViewOffset(false, 50, 50);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.theme_accent);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                refreshRecyclerView();
            }
        });




        mRecyclerView = (RecyclerView) mRootView.findViewById(R.id.books_recycler_view);
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
        mLayoutManager = new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(mLayoutManager);

        setupViewModel();

        return mRootView;
    }

    private void refreshRecyclerView() {

        mRecyclerView.setAdapter(null);
        mRecyclerView.setLayoutManager(null);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mAdapter.notifyDataSetChanged();
        mIsRefreshing = false;
        updateRefreshingUI();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    private void setupViewModel() {
        mModel = ViewModelProviders.of(getActivity()).get(ReaderViewModel.class);
        mModel.getBooks().observe(getActivity(), new Observer<List<Book>>() {
            @Override
            public void onChanged(List<Book> books) {
                Log.d(TAG, "Updating list of books from LiveData in ViewModel");
                mAdapter.setBooks(books);
                mAdapter.notifyDataSetChanged();
            }
        });
    }

    private void refresh() {
        getActivity().startService(new Intent(getActivity(), UpdaterService.class));
    }

    @Override
    public void onStart() {
        super.onStart();
        getActivity().registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    public void onStop() {
        super.onStop();
        getActivity().unregisterReceiver(mRefreshingReceiver);
    }

    private BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                mConnectionError =intent.getBooleanExtra(UpdaterService.EXTRA_SHOW_SNACKBAR, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
        if(mConnectionError){
            showConnectionError();
        }
    }

    private void showConnectionError() {
        mConnectionError = false;
        Snackbar.make(mCoordinatorLayout,
                "Cannot update the articles list. Check your internet connection",
                Snackbar.LENGTH_LONG)
                .setAction("CLOSE", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCheckInternetConnection = false;
            }
        }).show();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean(STATE_CHECK_CONNECTION, mCheckInternetConnection);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void showError() {
        mConnectionError = true;
        updateRefreshingUI();
    }

    public class Adapter extends RecyclerView.Adapter<ArticleListFragment.ViewHolder> {
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
        public ArticleListFragment.ViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ArticleListFragment.ViewHolder vh = new ArticleListFragment.ViewHolder(view);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    openDetailActivityWithAnimation(vh);
                }
            });
            return vh;
        }


        private void openDetailActivityWithAnimation(ViewHolder vh) {
            Intent intent = new Intent(getActivity().getApplicationContext(), ArticleDetailActivity.class);
            intent.putExtra(EXTRA_ARTICLE_ID, (long) vh.getAdapterPosition());
            Bundle bundle = ActivityOptions
                    .makeSceneTransitionAnimation(
                            getActivity(),
                            vh.thumbnailView,
                            vh.thumbnailView.getTransitionName())
                    .toBundle();

            startActivity(intent, bundle);
        }


        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
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


            ImageLoader imageLoader = ImageLoaderHelper.getInstance(getActivity()).getImageLoader();

            holder.thumbnailView.setImageUrl(
                    book.getThumbnailUrl(),
                    imageLoader);
            holder.thumbnailView.setAspectRatio(book.getAspectRatio());

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
