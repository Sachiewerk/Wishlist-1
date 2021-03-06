package com.company.wishlist.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.company.wishlist.R;
import com.company.wishlist.activity.TopWishActivity;
import com.company.wishlist.activity.WishEditActivity;
import com.company.wishlist.adapter.WishListAdapter;
import com.company.wishlist.events.FriendSelectedEvent;
import com.company.wishlist.model.User;
import com.company.wishlist.model.WishList;
import com.company.wishlist.util.AuthUtils;
import com.firebase.client.DataSnapshot;
import com.firebase.client.FirebaseError;
import com.firebase.client.ValueEventListener;
import com.github.clans.fab.FloatingActionMenu;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by vladstarikov on 07.01.16.
 */
public class WishListFragment extends Fragment {

    private static final String LOG_TAG = WishListFragment.class.getSimpleName();

    public static final int MY_WISH_LIST_MODE = 0;
    public static final int WISH_LIST_MODE = 1;
    public static final int GIFT_LIST_MODE = 2;
    public static final String MODE = "mode";

    private WishListAdapter mAdapter;
    private WishList mWishList;
    private int mMode;

    @BindView(R.id.fab) FloatingActionMenu mFab;

    public static WishListFragment newInstance(int mode, User user) {
        WishListFragment fragment = new WishListFragment();
        Bundle args = new Bundle();
        args.putInt(MODE, mode);
        args.putSerializable(User.class.getSimpleName(), user);
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
        ButterKnife.bind(this, view);

        this.mMode = getArguments().getInt(MODE);

        //Init RecyclerView
        mAdapter = new WishListAdapter(getContext(), getView(), mMode == MY_WISH_LIST_MODE ? GIFT_LIST_MODE : mMode);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(mAdapter);

        //Init selected friend
        User user = (User) getArguments().getSerializable(User.class.getSimpleName());
        onFriendSelectedEvent(new FriendSelectedEvent(user));

        //Init FAB menu
        if (mMode == WISH_LIST_MODE) mFab.setVisibility(View.GONE);
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
                        .putExtra(WishList.class.getSimpleName(), mWishList));
                break;
            case R.id.fab_choose:
                startActivity(new Intent(getContext(), TopWishActivity.class)
                        .putExtra(WishList.class.getSimpleName(), mWishList));
                break;
        }
    }

    @Subscribe
    public void onFriendSelectedEvent(FriendSelectedEvent event) {
        final String friendId = event.getFriend().getId();
        switch (mMode) {
            case WISH_LIST_MODE:
                mAdapter.onFriendSelected(friendId); return;
            case MY_WISH_LIST_MODE:
                if (!event.getFriend().getId().equals(AuthUtils.getCurrentUser().getId())) return;//TODO: equals check hash, but not ID
            case GIFT_LIST_MODE:
                WishList.getFirebaseRef()
                        .orderByChild("forUser")
                        .equalTo(friendId)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Log.d(LOG_TAG, getId()  + ".onDataChange(" + dataSnapshot + ")");
                                for (DataSnapshot wishListDS : dataSnapshot.getChildren()) {
                                    if (wishListDS.getValue(WishList.class).getOwner().equals(AuthUtils.getCurrentUser().getId())) {
                                        mWishList = wishListDS.getValue(WishList.class);
                                        mWishList.setId(wishListDS.getKey());
                                        mAdapter.onFriendSelected(friendId);
                                        return;
                                    }
                                }
                                mWishList = new WishList(AuthUtils.getCurrentUser().getId(), friendId);
                                mWishList.push();//TODO: check if new user
                                mAdapter.onFriendSelected(friendId);
                            }

                            @Override
                            public void onCancelled(FirebaseError firebaseError) {
                                Log.e(LOG_TAG, firebaseError.toString());
                            }
                        });
        }
    }

}
