package com.example.xyzreader.ui;

import android.content.Intent;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ImageView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

public class ArticleDetailActivity extends AppCompatActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {
    private Cursor mCursor;
    private long mItemId;
    private ImageView mPhoto;
    private FloatingActionButton mShareFab;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_detail);

        mPhoto = (ImageView) findViewById(R.id.photo);
        mShareFab = (FloatingActionButton) findViewById(R.id.share_fab);

        mShareFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(Intent.createChooser(ShareCompat.IntentBuilder
                        .from(ArticleDetailActivity.this)
                        .setType("text/plain")
                        .setText("Some sample text")
                        .getIntent(), getString(R.string.action_share)));
            }
        });

        if (savedInstanceState == null) {
            if (getIntent() != null && getIntent().getData() != null) {
                mItemId = ItemsContract.Items.getItemId(getIntent().getData());
            }
        }

        getSupportLoaderManager().initLoader(0, null, this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            postponeEnterTransition();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newInstanceForItemId(this, mItemId);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mCursor = cursor;

        mCursor.moveToFirst();

        // Load the thumbnail first, as it should be cached because it is displayed in the list
        // activity. This prevents a frustrating delay by the common element transition to load
        // the full sized image. This is done later (and almost unnoticeable).
        Picasso.with(this).load(mCursor.getString(ArticleLoader.Query.THUMB_URL)).into(mPhoto, new Callback() {
            @Override
            public void onSuccess() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    startPostponedEnterTransition();
                }

                Picasso.with(ArticleDetailActivity.this)
                        .load(mCursor.getString(ArticleLoader.Query.PHOTO_URL))
                        .placeholder(mPhoto.getDrawable())
                        .into(mPhoto);
            }

            @Override
            public void onError() {
            }
        });

        ((CollapsingToolbarLayout) findViewById(R.id.toolbar_layout))
                .setTitle(mCursor.getString(ArticleLoader.Query.TITLE));

        Fragment detailFragment = getSupportFragmentManager().findFragmentByTag("detail");
        if (detailFragment == null) {
            detailFragment = ArticleDetailFragment.newInstance(mItemId);

            getSupportFragmentManager()
                    .beginTransaction()
                    .add(R.id.detail_fragment_container, detailFragment, "detail")
                    .commitAllowingStateLoss();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mCursor = null;
    }
}
