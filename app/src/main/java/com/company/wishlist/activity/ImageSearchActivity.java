package com.company.wishlist.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.company.wishlist.R;
import com.company.wishlist.activity.abstracts.InternetActivity;
import com.company.wishlist.adapter.ImageSearchAdapter;
import com.company.wishlist.view.GridAutofitLayoutManager;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ImageSearchActivity extends InternetActivity implements ImageSearchAdapter.IOnPictureSelectedListener {

    public static final String QUERY = "com.company.wishlist.activity.QUERY";
    public static final String RESULT_DATA = "com.company.wishlist.activity.RESULT_DATA";

    @Bind(R.id.recyclerView)
    RecyclerView recyclerView;

    ImageSearchAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_grid);
        ButterKnife.bind(this);

        //Setup ActionBar
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);

        String query = getIntent().getStringExtra(QUERY);

        actionBar.setTitle(String.format("Results for query:%s", query));

        RecyclerView.LayoutManager layoutManager = new GridAutofitLayoutManager(this, (int) getResources().getDimension(R.dimen.image_size_large_large));
        adapter = new ImageSearchAdapter(this, this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(layoutManager);
        adapter.getPictures(query);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return false;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPictureSelected(String url) {
        Intent intent = new Intent().putExtra(RESULT_DATA, url);
        setResult(RESULT_OK, intent);
        finish();
    }
}

