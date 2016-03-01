package com.company.wishlist.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.company.wishlist.R;
import com.company.wishlist.activity.TopWishActivity;
import com.company.wishlist.activity.WishEditActivity;
import com.company.wishlist.adapter.FriendListAdapter;
import com.company.wishlist.adapter.WishItemTouchHelper;
import com.company.wishlist.adapter.WishListAdapter;
import com.company.wishlist.events.FriendSelectedEvent;
import com.company.wishlist.model.WishList;
import com.company.wishlist.util.FirebaseUtil;
import com.firebase.client.DataSnapshot;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.github.clans.fab.FloatingActionMenu;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by vladstarikov on 07.01.16.
 */
public class WishListFragment extends DebugFragment {

    public static final int MY_WISH_LIST_MODE = 0;
    public static final int WISH_LIST_MODE = 1;
    public static final int GIFT_LIST_MODE = 2;
    public static final String WISH_LIST_ID = "WISH_LIST_ID";
    public static final String MODE = "mode";

    private WishListAdapter adapter;
    private String wishListId;
    private int mode;

    @Bind(R.id.fab) FloatingActionMenu mFab;

    public static WishListFragment newInstance(int mode, String friendId) {
        WishListFragment fragment = new WishListFragment();
        Bundle args = new Bundle();
        args.putInt(MODE, mode);
        args.putString(FriendListAdapter.FRIEND_ID, friendId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        EventBus.getDefault().unregister(this);
        super.onPause();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wish_list, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);//TODO: may be not bind if mFab is not visible?

        Bundle bundle = getArguments();
        this.mode = bundle.getInt(MODE);
        String friendId = bundle.getString(FriendListAdapter.FRIEND_ID);

        adapter = new WishListAdapter(getContext(), mode == MY_WISH_LIST_MODE ? GIFT_LIST_MODE : mode);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        new ItemTouchHelper(new WishItemTouchHelper(recyclerView)).attachToRecyclerView(recyclerView);

        onFriendSelectedEvent(new FriendSelectedEvent(friendId));

        if (mode == WISH_LIST_MODE) mFab.setVisibility(View.GONE);
        else {
            mFab.setClickable(false);
            mFab.setOnMenuButtonClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mFab.isOpened()) {
                        mFab.setClickable(false);
                        mFab.close(true);
                    } else {
                        mFab.setClickable(true);
                        mFab.open(true);
                    }
                }
            });
        }
    }

    @OnClick({R.id.fab, R.id.fab_add, R.id.fab_choose})
    public void onClick(View v) {
        mFab.toggle(true);
        mFab.setClickable(false);
        switch (v.getId()) {
            case R.id.fab_add:
                startActivity(new Intent(getContext(), WishEditActivity.class)
                        .setAction(WishEditActivity.ACTION_CREATE)
                        .putExtra(WISH_LIST_ID, wishListId));
                break;
            case R.id.fab_choose:
                startActivity(new Intent(getContext(), TopWishActivity.class)
                        .setAction(WishEditActivity.ACTION_CREATE)
                        .putExtra(WISH_LIST_ID, wishListId));
                break;
        }
    }

    @Subscribe
    public void onFriendSelectedEvent(FriendSelectedEvent event) {
        final Fragment fragment = this;
        final String friendId = event.getFriendId();
        switch (mode) {
            case WISH_LIST_MODE:
                adapter.onFriendSelected(friendId); return;
            case MY_WISH_LIST_MODE:
                if (!event.getFriendId().equals(FirebaseUtil.getCurrentUser().getId())) return;
            case GIFT_LIST_MODE:
                WishList.getFirebaseRef()
                        .orderByChild("forUser")
                        .equalTo(friendId)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Log.d(LOG_TAG, getId() + " - " + fragment.hashCode() + ".onDataChange(" + dataSnapshot + ")");
                                for (DataSnapshot wishListDS : dataSnapshot.getChildren()) {
                                    if (wishListDS.getValue(WishList.class).getOwner().equals(FirebaseUtil.getCurrentUser().getId())) {
                                        wishListId = wishListDS.getKey();
                                        adapter.onFriendSelected(friendId);
                                        return;
                                    }
                                }
                                WishList wishList = new WishList(FirebaseUtil.getCurrentUser().getId(), friendId);
                                wishListId = wishList.push();
                                adapter.onFriendSelected(friendId);
                            }

                            @Override
                            public void onCancelled(FirebaseError firebaseError) {
                                Log.e(LOG_TAG, firebaseError.toString());
                            }
                        });
        }
    }

}
