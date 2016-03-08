package com.company.wishlist.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import com.company.wishlist.R;
import com.company.wishlist.activity.abstracts.DebugActivity;
import com.company.wishlist.adapter.ImageSearchAdapter;
import com.company.wishlist.view.GridAutofitLayoutManager;

import java.util.ArrayList;

import butterknife.Bind;
import butterknife.ButterKnife;

public class ImageSearchActivity extends DebugActivity implements ImageSearchAdapter.IOnPictureSelectedListener {

    public static final String QUERY = "com.company.wishlist.activity.QUERY";
    public static final String RESULT_DATA = "com.company.wishlist.activity.RESULT_DATA";

    ImageSearchAdapter adapter;

    @Bind(R.id.recyclerView)
    RecyclerView recyclerView;

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
        if (savedInstanceState != null && savedInstanceState.getStringArrayList("Items") != null) {
            adapter = new ImageSearchAdapter(this, this, savedInstanceState.getStringArrayList("Items"));
        } else {
            adapter = new ImageSearchAdapter(this, this, query);
        }
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(layoutManager);
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
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList("Items", (ArrayList<String>) adapter.getItems());
    }

    @Override
    public void onPictureSelected(String url) {
        Intent intent = new Intent().putExtra(RESULT_DATA, url);
        setResult(RESULT_OK, intent);
        finish();
    }
}

