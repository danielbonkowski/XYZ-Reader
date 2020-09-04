package com.example.xyzreader.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.xyzreader.R;
import com.example.xyzreader.data.UpdaterService;
import com.example.xyzreader.model.Book;
import com.example.xyzreader.model.ReaderViewModel;
import com.example.xyzreader.remote.InternetCheckAsyncTask;
import com.google.android.material.snackbar.Snackbar;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;

public class ArticleListFragment extends Fragment
implements InternetCheckAsyncTask.ShowConnectionError{

    private static final String STATE_CHECK_CONNECTION = "check_internet_connection";
    View mRootView;
    private static final String TAG = ArticleListActivity.class.toString();
    public static final String EXTRA_ARTICLE_ID = "extra_article_id";

    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RecyclerView mArticlesRecyclerView;
    private ArticlesAdapter mArticlesAdapter;
    private ReaderViewModel mModel;
    private boolean mIsRefreshing = false;
    private boolean mConnectionError = false;
    private CoordinatorLayout mCoordinatorLayout;
    private boolean mCheckInternetConnection = true;
    private RecyclerView.LayoutManager mLayoutManager;
    private boolean mIsCard;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private final SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private final GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);
    private ArticleClickListener mArticleClickListener;

    public ArticleListFragment() {
    }

    public interface ArticleClickListener{
        void onArticleClick(ArticleViewHolder viewHolder);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        mArticleClickListener = (ArticleClickListener) context;
        super.onAttach(context);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_article_list, container, false);
        mIsCard = getResources().getBoolean(R.bool.detail_is_card);

        if (savedInstanceState == null) {
            refreshArticles();
        }else{
            mCheckInternetConnection = savedInstanceState.getBoolean(STATE_CHECK_CONNECTION);
        }

        checkInternetConnection();

        setupCoordinatorLayout();

        setupSwipeRefreshLayout();

        setupArticlesRecyclerView();

        setupViewModel();

        return mRootView;
    }

    private void checkInternetConnection() {
        if(mCheckInternetConnection){
            new InternetCheckAsyncTask(this).execute(this.getContext());
        }
    }

    private void setupCoordinatorLayout() {
        mCoordinatorLayout = mRootView.findViewById(R.id.coordinator_layout);
    }

    private void setupSwipeRefreshLayout() {

        mSwipeRefreshLayout = mRootView.findViewById(R.id.swipe_refresh_layout);
        mSwipeRefreshLayout.setProgressViewOffset(false, 50, 50);
        mSwipeRefreshLayout.setColorSchemeResources(R.color.theme_accent);
        mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshRecyclerView();
            }
        });
    }

    private void refreshRecyclerView() {

        mArticlesRecyclerView.setAdapter(null);
        mArticlesRecyclerView.setLayoutManager(null);
        mArticlesRecyclerView.setAdapter(mArticlesAdapter);
        mArticlesRecyclerView.setLayoutManager(mLayoutManager);
        mArticlesAdapter.notifyDataSetChanged();
        mIsRefreshing = false;
        updateRefreshingUI();
    }

    private void setupArticlesRecyclerView() {

        mArticlesRecyclerView = mRootView.findViewById(R.id.books_recycler_view);
        mArticlesAdapter = new ArticlesAdapter(null);
        mArticlesAdapter.setHasStableIds(true);
        mArticlesRecyclerView.setAdapter(mArticlesAdapter);
        int columnCount = getResources().getInteger(R.integer.list_column_count);
        if(mIsCard){
            mLayoutManager = new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false);
        }else {
            mLayoutManager = new StaggeredGridLayoutManager(columnCount, StaggeredGridLayoutManager.VERTICAL);
        }

        mArticlesRecyclerView.setLayoutManager(mLayoutManager);
        mArticlesRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                int adapterPosition =
                        (recyclerView == null || recyclerView.getChildCount() == 0) ? 0 : recyclerView.getChildAt(0).getTop();
                mSwipeRefreshLayout.setEnabled(adapterPosition >= 0);
            }
        });
    }


    private void setupViewModel() {
        mModel = ViewModelProviders.of(Objects.requireNonNull(getActivity())).get(ReaderViewModel.class);
        mModel.getBooks().observe(getActivity(), new Observer<List<Book>>() {
            @Override
            public void onChanged(List<Book> books) {
                Log.d(TAG, "Updating list of books from LiveData in ViewModel");
                mArticlesAdapter.setBooks(books);
                mArticlesAdapter.notifyDataSetChanged();
            }
        });
    }

    private void refreshArticles() {
        Objects.requireNonNull(getActivity()).startService(new Intent(getActivity(), UpdaterService.class));
    }

    @Override
    public void onStart() {
        super.onStart();
        Objects.requireNonNull(getActivity()).registerReceiver(mRefreshingReceiver,
                new IntentFilter(UpdaterService.BROADCAST_ACTION_STATE_CHANGE));
    }

    @Override
    public void onStop() {
        super.onStop();
        Objects.requireNonNull(getActivity()).unregisterReceiver(mRefreshingReceiver);
    }

    private final BroadcastReceiver mRefreshingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (UpdaterService.BROADCAST_ACTION_STATE_CHANGE.equals(intent.getAction())) {
                mIsRefreshing = intent.getBooleanExtra(UpdaterService.EXTRA_REFRESHING, false);
                mConnectionError = intent.getBooleanExtra(UpdaterService.EXTRA_SHOW_SNACKBAR, false);
                updateRefreshingUI();
            }
        }
    };

    private void updateRefreshingUI() {
        mSwipeRefreshLayout.setRefreshing(mIsRefreshing);
        if(mConnectionError){ showConnectionError(); }
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
    public void showError() {
        mConnectionError = true;
        updateRefreshingUI();
    }



    public class ArticlesAdapter extends RecyclerView.Adapter<ArticleViewHolder> {
        private List<Book> mBooks;
        private int mPosition;

        public ArticlesAdapter(List<Book> books) {
            mBooks = books;
        }

        public void setBooks(List<Book> books){
            mBooks = books;
            mArticlesAdapter.notifyDataSetChanged();
        }

        @Override
        public long getItemId(int position) {
            mPosition = position;
            return  mBooks.get(position).getId();
        }

        @Override
        public ArticleViewHolder onCreateViewHolder(final ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ArticleViewHolder vh = new ArticleViewHolder(view);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mArticleClickListener.onArticleClick(vh);
                }
            });
            return vh;
        }


        @Override
        public void onBindViewHolder(@NonNull ArticleViewHolder holder, int position) {
            Book book = mBooks.get(position);

            bindTitle(holder, book);
            bindSubtitle(holder, book);
            bindThumbnail(holder, book);
        }


        private void bindTitle(@NonNull ArticleViewHolder holder, Book book) {
            holder.titleView.setText(book.getTitle());
        }


        private void bindSubtitle(@NonNull ArticleViewHolder holder, Book book) {
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
        }


        private void bindThumbnail(@NonNull ArticleViewHolder holder, Book book) {
            holder.thumbnailView.setImageUrl(
                    book.getThumbnailUrl(),
                    ImageLoaderHelper.getInstance(getActivity()).getImageLoader());
            holder.thumbnailView.setAspectRatio(book.getAspectRatio());
        }


        private Date parsePublishedDate() {
            try {
                String date = mBooks.get(mPosition).getPublishedDate();
                return dateFormat.parse(date);
            } catch (ParseException ex) {
                Log.e(TAG, Objects.requireNonNull(ex.getMessage()));
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

    public static class ArticleViewHolder extends RecyclerView.ViewHolder {
        public final DynamicHeightNetworkImageView thumbnailView;
        public final TextView titleView;
        public final TextView subtitleView;

        public ArticleViewHolder(View view) {
            super(view);
            thumbnailView = view.findViewById(R.id.thumbnail);
            titleView = view.findViewById(R.id.article_title);
            subtitleView = view.findViewById(R.id.article_subtitle);
        }
    }
}
