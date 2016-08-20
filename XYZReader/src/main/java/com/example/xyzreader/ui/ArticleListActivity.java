package com.example.xyzreader.ui;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.xyzreader.R;
import com.example.xyzreader.data.ArticleLoader;
import com.example.xyzreader.data.ItemsContract;
import com.example.xyzreader.data.UpdaterService;
import com.squareup.picasso.Picasso;

/**
 * An activity representing a list of Articles. This activity has different presentations for
 * handset and tablet-size devices. On handsets, the activity presents a list of items, which when
 * touched, lead to a {@link ArticleDetailActivity} representing item details. On tablets, the
 * activity presents a grid of items as cards.
 */
public class ArticleListActivity extends AppCompatActivity implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private RecyclerView mRecyclerView;
    private TextView mTabletTitleTextView;
    private ImageView mTabletImageView;
    private View mTabletDetailContainerView;
    private FloatingActionButton mTabletShareFab;
    private boolean tabletMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article_list);

        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);

        if (findViewById(R.id.tablet_detail_container) != null) {
            // We are on tablet layout
            tabletMode = true;
            mTabletTitleTextView = (TextView) findViewById(R.id.tablet_title);
            mTabletImageView = (ImageView) findViewById(R.id.tablet_image);
            mTabletDetailContainerView = findViewById(R.id.tablet_detail_container);
            mTabletShareFab = (FloatingActionButton) findViewById(R.id.share_fab);
        }

        getSupportLoaderManager().initLoader(0, null, this);

        if (savedInstanceState == null) {
            refresh();
        }
    }

    private void refresh() {
        startService(new Intent(this, UpdaterService.class));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return ArticleLoader.newAllArticlesInstance(this);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        Adapter adapter = new Adapter(cursor);
        adapter.setHasStableIds(true);
        mRecyclerView.setAdapter(adapter);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mRecyclerView.setAdapter(null);
    }

    private class Adapter extends RecyclerView.Adapter<ViewHolder> {
        private Cursor mCursor;

        public Adapter(Cursor cursor) {
            mCursor = cursor;
        }

        @Override
        public long getItemId(int position) {
            mCursor.moveToPosition(position);
            return mCursor.getLong(ArticleLoader.Query._ID);
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.list_item_article, parent, false);
            final ViewHolder vh = new ViewHolder(view);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!tabletMode) {
                        ActivityOptionsCompat options =
                                ActivityOptionsCompat.makeSceneTransitionAnimation(
                                        ArticleListActivity.this, vh.thumbnailView,
                                        getString(R.string.image_transition));
                        startActivity(new Intent(Intent.ACTION_VIEW,
                                        ItemsContract.Items.buildItemUri(getItemId(vh.getAdapterPosition()))),
                                options.toBundle());
                    } else {
                        ArticleDetailFragment fragment = ArticleDetailFragment.newInstance(getItemId(vh.getAdapterPosition()));
                        getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.tablet_detail_container, fragment)
                                .commit();

                        mCursor.moveToPosition(vh.getAdapterPosition());
                        String title = mCursor.getString(ArticleLoader.Query.TITLE);
                        String imageUrl = mCursor.getString(ArticleLoader.Query.PHOTO_URL);
                        Picasso.with(getApplicationContext()).load(imageUrl).into(mTabletImageView);
                        mTabletTitleTextView.setText(title);
                        mTabletShareFab.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                startActivity(Intent.createChooser(ShareCompat.IntentBuilder
                                        .from(ArticleListActivity.this)
                                        .setType("text/plain")
                                        .setText("Some sample text")
                                        .getIntent(), getString(R.string.action_share)));
                            }
                        });
                    }
                }
            });
            return vh;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            mCursor.moveToPosition(position);
            holder.titleView.setText(mCursor.getString(ArticleLoader.Query.TITLE));
            holder.subtitleView.setText(
                    DateUtils.getRelativeTimeSpanString(
                            mCursor.getLong(ArticleLoader.Query.PUBLISHED_DATE),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString()
                            + " by "
                            + mCursor.getString(ArticleLoader.Query.AUTHOR));
            Picasso.with(getApplicationContext())
                    .load(mCursor.getString(ArticleLoader.Query.THUMB_URL))
                    .into(holder.thumbnailView);
        }

        @Override
        public int getItemCount() {
            return mCursor.getCount();
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ImageView thumbnailView;
        public TextView titleView;
        public TextView subtitleView;

        public ViewHolder(View view) {
            super(view);
            thumbnailView = (ImageView) view.findViewById(R.id.thumbnail);
            titleView = (TextView) view.findViewById(R.id.article_title);
            subtitleView = (TextView) view.findViewById(R.id.article_subtitle);
        }
    }
}
