package com.example.xyzreader.ui;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.GregorianCalendar;


import android.os.Build;
import android.os.Bundle;


import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.Html;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.app.ShareCompat;
import androidx.core.text.PrecomputedTextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.example.xyzreader.R;
import com.example.xyzreader.data.ItemsCounterService;
import com.example.xyzreader.model.AppExecutors;
import com.example.xyzreader.model.Book;
import com.example.xyzreader.model.ReaderViewModel;

/**
 * A fragment representing a single Article detail screen. This fragment is
 * either contained in a {@link ArticleListActivity} in two-pane mode (on
 * tablets) or a {@link ArticleDetailActivity} on handsets.
 */
public class ArticleDetailFragment extends Fragment {
    private static final String TAG = "ArticleDetailFragment";

    public static final String ARG_ITEM_ID = "item_id";
    private static final float PARALLAX_FACTOR = 1.25f;
    public static final String CURRENT_ITEMS_NR = "current_items_nr";
    public static final String MAX_ITEMS_NR = "max_items_nr";
    public static final String SELECTED_ITEM_NR = "selected_item_nr";

    private long mItemId;
    private View mRootView;
    private int mMutedColor = 0xFF333333;
    private ObservableScrollView mScrollView;
    private DrawInsetsFrameLayout mDrawInsetsFrameLayout;
    private ColorDrawable mStatusBarColorDrawable;

    private int mTopInset;
    private View mPhotoContainerView;
    private ImageView mPhotoView;
    private int mScrollY;
    private boolean mIsCard = false;
    private int mStatusBarFullOpacityBottom;
    private ProgressBar mTextProgressBar;
    private RecyclerView mTextRecyclerView;
    private String mBodyText = "";
    private int mMaxNrOfItemsInRecyclerView;
    private int mCurrentNrOfItemsInRecyclerView = 10;
    private String[] mBodyTextArray;
    private DrawInsetsFrameLayout mContainerFrameLayout;

    private TextView mTitleView;
    private TextView mBylineView;
    private RecyclerView mRecyclerView;
    private ReaderViewModel mModel;
    private Book mBook;
    private Handler mHandler;
    private int mRecyclerViewPosition;
    private LinearLayoutManager mLinearLayoutManager;

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2,1,1);
    private SwipeListener mSwipeListener;
    Context mContext;

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public ArticleDetailFragment() {
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;

    }

    public interface SwipeListener{
        public void swipeRight();
        public void swipeLeft();
    }

    public void setSwipeListener(SwipeListener swipeListener){
        mSwipeListener = swipeListener;
    }

    public static ArticleDetailFragment newInstance(long itemId) {
        Bundle arguments = new Bundle();
        arguments.putLong(ARG_ITEM_ID, itemId);
        ArticleDetailFragment fragment = new ArticleDetailFragment();
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments().containsKey(ARG_ITEM_ID)) {
            mItemId = getArguments().getLong(ARG_ITEM_ID);
        }

        mHandler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(@NonNull Message msg) {
                mBodyTextArray = (String[]) msg.obj;
                mMaxNrOfItemsInRecyclerView = mBodyTextArray.length;
            }
        };

        mIsCard = getResources().getBoolean(R.bool.detail_is_card);
        mStatusBarFullOpacityBottom = getResources().getDimensionPixelSize(
                R.dimen.detail_card_top_margin);
        setHasOptionsMenu(true);

    }



    public ArticleDetailActivity getActivityCast() {
        return (ArticleDetailActivity) getActivity();
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        mRootView = inflater.inflate(R.layout.fragment_article_detail, container, false);
        FrameLayout frameLayout = mRootView.findViewById(R.id.scrollview);
        frameLayout.setOnTouchListener(new OnSwipeListener(getActivity()){
            @Override
            public void leftSwipe() {
                Log.d(TAG, "Swipe left fragment");
                mSwipeListener.swipeLeft();
            }

            @Override
            public void rightSwipe() {
                Log.d(TAG, "Swipe right fragment");
                mSwipeListener.swipeRight();
            }

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                Log.d(TAG, "Touch fragment");
                return super.onTouch(view, motionEvent);
            }
        });
        mDrawInsetsFrameLayout = (DrawInsetsFrameLayout)
                mRootView.findViewById(R.id.draw_insets_frame_layout);
        mDrawInsetsFrameLayout.setOnInsetsCallback(new DrawInsetsFrameLayout.OnInsetsCallback() {
            @Override
            public void onInsetsChanged(Rect insets) {
                mTopInset = insets.top;
            }
        });

        mScrollView = (ObservableScrollView) mRootView.findViewById(R.id.scrollview);
        mScrollView.setCallbacks(new ObservableScrollView.Callbacks() {
            @Override
            public void onScrollChanged() {
                mScrollY = mScrollView.getScrollY();
                getActivityCast().onUpButtonFloorChanged(mItemId, ArticleDetailFragment.this);
                mPhotoContainerView.setTranslationY((int) (mScrollY - mScrollY / PARALLAX_FACTOR));
                updateStatusBar();

                View view = (View) mScrollView.getChildAt(0);
                int diff = view.getBottom() - (mScrollView.getHeight() + mScrollY);



                if (diff == 0){
                    if(mCurrentNrOfItemsInRecyclerView == mMaxNrOfItemsInRecyclerView){
                        return;
                    }else if(mCurrentNrOfItemsInRecyclerView + 10 > mMaxNrOfItemsInRecyclerView){
                        mCurrentNrOfItemsInRecyclerView = mMaxNrOfItemsInRecyclerView;
                    }else {
                        mCurrentNrOfItemsInRecyclerView += 10;
                    }


                    mRecyclerView.getAdapter().notifyItemInserted(mCurrentNrOfItemsInRecyclerView);

                    //mRecyclerView.smoothScrollToPosition(mScrollY);

                }

            }
        });

        mPhotoView = (ImageView) mRootView.findViewById(R.id.photo);
        mPhotoContainerView = mRootView.findViewById(R.id.photo_container);

        mStatusBarColorDrawable = new ColorDrawable(0);

        mRootView.findViewById(R.id.share_fab).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder.from(getActivity())
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        mTextProgressBar = (ProgressBar) mRootView.findViewById(R.id.text_progress_bar);


        mTitleView = (TextView) mRootView.findViewById(R.id.article_title);
        mBylineView = (TextView) mRootView.findViewById(R.id.article_byline);
        mBylineView.setMovementMethod(new LinkMovementMethod());
        mRecyclerView = (RecyclerView) mRootView.findViewById(R.id.article_body_recycler_view);
        TextAdapter textAdapter = new TextAdapter();
        mRecyclerView.setAdapter(textAdapter);
        mLinearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(mLinearLayoutManager);




        if(savedInstanceState != null){
            mMaxNrOfItemsInRecyclerView = savedInstanceState.getInt(MAX_ITEMS_NR);
            mCurrentNrOfItemsInRecyclerView = savedInstanceState.getInt(CURRENT_ITEMS_NR);
            mRecyclerViewPosition = savedInstanceState.getInt(SELECTED_ITEM_NR);
        }



        bindViews();
        updateStatusBar();

        setupViewModel();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                    View view = mRecyclerView.getChildAt(mRecyclerViewPosition);
                    float y = 0;
                    if(view != null){
                        y = view.getY();
                    }
                    mScrollView.smoothScrollTo(0, (int) y + 100);


            }
        }, 200);

        return mRootView;
    }

    @Override
    public void onDestroy() {
        Slide exitTransition = new Slide();
        exitTransition.setSlideEdge(Gravity.RIGHT);
        exitTransition.setDuration(300);
        getActivity().getWindow().setExitTransition(exitTransition);
        super.onDestroy();
    }

    private void setupViewModel() {
        mModel = ViewModelProviders.of(getActivity()).get(ReaderViewModel.class);

        mModel.getSelectedBook().observe(getActivity(), new Observer<Book>() {
            @Override
            public void onChanged(Book book) {
                mModel.getSelectedBook().removeObserver(this);
                Log.d(TAG, "Updating fragment book object");
                mBook = book;

                AppExecutors appExecutors = AppExecutors.getInstance();
                appExecutors.diskIO().execute(new Runnable() {
                    @Override
                    public void run() {

                        String bookBody = mBook.getBody();
                        mBodyTextArray = bookBody.split("\n\n");
                        mMaxNrOfItemsInRecyclerView = mBodyTextArray.length;

                        mHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                bindViews();
                                //mRecyclerView.getAdapter().notifyDataSetChanged();
                            }
                        });
                    }
                });
                if(mBodyTextArray != null){
                    mModel.setSelectedBookBodyArray(mBodyTextArray);
                }


            }
        });

        mModel.getSelectedBookBodyArray().observe(getActivity(), new Observer<String[]>() {
            @Override
            public void onChanged(String[] strings) {
                mModel.getSelectedBookBodyArray().removeObserver(this);

                bindViews();
                mRecyclerView.getAdapter().notifyDataSetChanged();
            }
        });
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mModel.getSelectedBook().removeObservers(this);
        mModel.getSelectedBookBodyArray().removeObservers(this);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putInt(CURRENT_ITEMS_NR, mCurrentNrOfItemsInRecyclerView);
        outState.putInt(MAX_ITEMS_NR, mMaxNrOfItemsInRecyclerView);
        //LinearLayoutManager linearLayoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
       //View focusedChild = (View) linearLayoutManager.getChildAt(5);
       //mRecyclerViewPosition = mRecyclerView.getChildAdapterPosition(focusedChild);
        outState.putInt(SELECTED_ITEM_NR, mRecyclerViewPosition);
        super.onSaveInstanceState(outState);

    }

    private void updateStatusBar() {
        int color = 0;
        if (mPhotoView != null && mTopInset != 0 && mScrollY > 0) {
            float f = progress(mScrollY,
                    mStatusBarFullOpacityBottom - mTopInset * 3,
                    mStatusBarFullOpacityBottom - mTopInset);
            color = Color.argb((int) (255 * f),
                    (int) (Color.red(mMutedColor) * 0.9),
                    (int) (Color.green(mMutedColor) * 0.9),
                    (int) (Color.blue(mMutedColor) * 0.9));
        }
        mStatusBarColorDrawable.setColor(color);
        mDrawInsetsFrameLayout.setInsetBackground(mStatusBarColorDrawable);
    }

    static float progress(float v, float min, float max) {
        return constrain((v - min) / (max - min), 0, 1);
    }

    static float constrain(float val, float min, float max) {
        if (val < min) {
            return min;
        } else if (val > max) {
            return max;
        } else {
            return val;
        }
    }

    private Date parsePublishedDate() {
        try {
            String date = mBook.getPublishedDate();
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void bindViews() {
        if (mRootView == null) {
            return;
        }

        //bodyView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "Rosario-Regular.ttf"));

        if (mBook != null) {
            //mRootView.setAlpha(0);
            mRootView.setVisibility(View.VISIBLE);
            //mRootView.animate().alpha(1);
            mTitleView.setText(mBook.getTitle());
            Date publishedDate = parsePublishedDate();
            if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                mBylineView.setText(Html.fromHtml(
                        DateUtils.getRelativeTimeSpanString(
                                publishedDate.getTime(),
                                System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                                DateUtils.FORMAT_ABBREV_ALL).toString()
                                + " by <font color='#ffffff'>"
                                + mBook.getAuthor()
                                + "</font>"));

            } else {
                // If date is before 1902, just show the string
                mBylineView.setText(Html.fromHtml(
                        outputFormat.format(publishedDate) + " by <font color='#ffffff'>"
                        + mBook.getAuthor()
                                + "</font>"));

            }

            //mTextProgressBar.setVisibility(View.VISIBLE);


            ImageLoaderHelper.getInstance(getActivity()).getImageLoader()
                    .get(mBook.getPhotoUrl(), new ImageLoader.ImageListener() {
                        @Override
                        public void onResponse(ImageLoader.ImageContainer imageContainer, boolean b) {
                            Bitmap bitmap = imageContainer.getBitmap();
                            if (bitmap != null) {
                                Palette p = Palette.generate(bitmap, 12);
                                mMutedColor = p.getDarkMutedColor(0xFF333333);
                                mPhotoView.setImageBitmap(imageContainer.getBitmap());
                                mRootView.findViewById(R.id.meta_bar)
                                        .setBackgroundColor(mMutedColor);
                                updateStatusBar();
                            }
                        }

                        @Override
                        public void onErrorResponse(VolleyError volleyError) {

                        }
                    });

        } else {
            mRootView.setVisibility(View.GONE);
            mTitleView.setText("N/A");
            mBylineView.setText("N/A" );
            //bodyView.setText("N/A");
        }
    }


    public int getUpButtonFloor() {
        if (mPhotoContainerView == null || mPhotoView.getHeight() == 0) {
            return Integer.MAX_VALUE;
        }

        // account for parallax
        return mIsCard
                ? (int) mPhotoContainerView.getTranslationY() + mPhotoView.getHeight() - mScrollY
                : mPhotoView.getHeight() - mScrollY;
    }

    private class TextAdapter extends RecyclerView.Adapter<TextAdapter.TextAdapterViewHolder>{

        public void updateAdapter(){
            this.notifyDataSetChanged();
        }

        @Override
        public TextAdapter.TextAdapterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getActivity().getLayoutInflater().inflate(R.layout.list_item_text, parent, false);
            TextAdapterViewHolder textAdapterViewHolder = new TextAdapterViewHolder(view);
            return textAdapterViewHolder;
        }

        @Override
        public void onBindViewHolder(final TextAdapterViewHolder holder, final int position) {

            if(mBodyTextArray != null){
                holder.appCompatTextView.setTextFuture(
                        PrecomputedTextCompat.getTextFuture(
                                mBodyTextArray[position].trim() + "\n",
                                holder.appCompatTextView.getTextMetricsParamsCompat(),
                                null
                        )
                );
                holder.appCompatTextView.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent) {
                        mRecyclerViewPosition = position;
                        return true;
                    }
                });
            }

        }

        @Override
        public int getItemCount() {
            if(mBook == null){
                return 0;
            }
            return mCurrentNrOfItemsInRecyclerView;
        }

        public class TextAdapterViewHolder extends RecyclerView.ViewHolder {

            AppCompatTextView appCompatTextView;

            public TextAdapterViewHolder(View itemView) {
                super(itemView);
                appCompatTextView = (AppCompatTextView) itemView.findViewById(R.id.list_item_text);
            }
        }
    }
}
