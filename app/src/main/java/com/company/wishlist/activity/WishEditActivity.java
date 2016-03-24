package com.company.wishlist.activity;

import android.animation.Animator;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.company.wishlist.R;
import com.company.wishlist.activity.abstracts.DebugActivity;
import com.company.wishlist.bean.EditWishBean;
import com.company.wishlist.fragment.WishListFragment;
import com.company.wishlist.model.Reservation;
import com.company.wishlist.model.Wish;
import com.company.wishlist.model.WishList;
import com.company.wishlist.util.AuthUtils;
import com.company.wishlist.util.CloudinaryUtil;
import com.company.wishlist.util.ConnectionUtil;
import com.company.wishlist.util.DateUtil;
import com.company.wishlist.util.DialogUtil;
import com.company.wishlist.util.social.share.ShareStrategy;
import com.company.wishlist.view.BottomSheetShareDialog;
import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.Length;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by vladstarikov on 15.01.16.
 */
public class WishEditActivity extends DebugActivity implements Validator.ValidationListener {

    private static final int RESULT_LOAD_IMAGE = 1;
    private static final int RESULT_IMAGE_SELECT = 2;
    private static final String DATE_DIALOG = "DATE_PICKER";
    public static final String ACTION_READ = "com.company.wishlist.ACTION_READ";
    public static final String ACTION_EDIT = "com.company.wishlist.ACTION_EDIT";
    public static final String ACTION_CREATE = "com.company.wishlist.ACTION_CREATE";
    public static final String ACTION_TAKE_FROM_TOP = "com.company.wishlist.ACTION_TAKE_FROM_TOP";

    @Bind(R.id.image_view)
    ImageView imageView;

    @Bind(R.id.edit_text_title)
    @NotEmpty
    @Length(min = 1)
    EditText editTextTitle;

    @Bind(R.id.edit_text_comment)
    @NotEmpty
    @Length(min = 2)
    EditText editTextComment;

    @Bind(R.id.coordinator_layout)
    CoordinatorLayout coordinatorLayout;

    @Bind(R.id.collapsing_toolbar)
    CollapsingToolbarLayout collapsingToolbarLayout;

    @Bind(R.id.fab)
    FloatingActionButton fab;

    private EditWishBean editWishBean;//TODO: test if edit wish bean contains correct data after screen rotate
    private Validator validator;
    private CalendarDatePickerDialogFragment reservedDateDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wish_edit);
        ButterKnife.bind(this);

        //Setup ActionBar
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setTitle(getIntent().getAction().equals(ACTION_CREATE) ? "New wish" : "Edit wish");

        //Setup validator
        validator = new Validator(this);
        validator.setValidationListener(this);

        //Init reserve date picker
        reservedDateDialog = new CalendarDatePickerDialogFragment();
        reservedDateDialog.setFirstDayOfWeek(Calendar.MONDAY);
        reservedDateDialog.setRetainInstance(true);
        reservedDateDialog.setDateRange(DateUtil.getToday(), null);

        //Get wish and wishlist
        Wish wish = (Wish) getIntent().getSerializableExtra(Wish.class.getSimpleName());
        WishList wishList = (WishList) getIntent().getSerializableExtra(WishList.class.getSimpleName());
        editWishBean = new EditWishBean(wish);

        //Init fields and image
        editTextTitle.setText(editWishBean.getTitle());
        editTextComment.setText(editWishBean.getComment());
        if (editWishBean.getPicture() != null) {//TODO: load optimized image
            Glide.with(this)
                    .load(CloudinaryUtil.getInstance().url().generate(editWishBean.getPicture()))
                    .crossFade()
                    .into(imageView);
        } else {
            imageView.setImageResource(R.drawable.wish_header);
        }

        //Init bean
        switch (getIntent().getAction()) {
            case ACTION_CREATE:
                editWishBean.setWishListId(wishList.getId());
                break;
            case ACTION_READ:
                fab.setVisibility(View.GONE);
                ActionMode.Callback callback = new ActionMode.Callback() {//TODO: temporally will be like this, ask a question on next lesson
                    @Override
                    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                        menu.removeItem(android.R.id.cut);
                        menu.removeItem(android.R.id.paste);
                        return true;
                    }

                    @Override
                    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                        return false;
                    }

                    @Override
                    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                        return false;
                    }

                    @Override
                    public void onDestroyActionMode(ActionMode mode) {

                    }

                };
                editTextTitle.setInputType(InputType.TYPE_NULL);
                editTextTitle.setTextIsSelectable(true);
                editTextTitle.setCustomSelectionActionModeCallback(callback);
                editTextComment.setInputType(InputType.TYPE_NULL);
                editTextComment.setTextIsSelectable(true);
                editTextComment.setMinLines(8);
                editTextComment.setCustomSelectionActionModeCallback(callback);
                break;
            case ACTION_TAKE_FROM_TOP:
                editWishBean.setId(null);
                editWishBean.setWishListId(wishList.getId());
                break;
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_wish_edit, menu);
        menu.findItem(R.id.action_delete).setVisible(getIntent().getAction().equals(ACTION_EDIT));
        if (editWishBean.isReserved() && !editWishBean.getReservation().getByUser().equals(AuthUtils.getCurrentUser().getId())) {
            menu.findItem(R.id.action_reserve).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return false;
            case R.id.action_done:
                validator.validate();
                return false;
            case R.id.action_reserve:
                reserveWish();
                return false;
            case R.id.action_delete:
                deleteWish();
                return false;
            case R.id.action_share:
                final String message = getString(R.string.message_default_tweet_wish, editWishBean.getTitle());
                BottomSheetDialog bottomSheetDialog = new BottomSheetShareDialog(this);
                bottomSheetDialog.show();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        discardChanges();
    }

    @Deprecated
    public void circleRevealAnimation() {
        final View rootLayout = findViewById(R.id.coordinator_layout);
        ViewTreeObserver viewTreeObserver = rootLayout.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        DisplayMetrics displayMetrics = new DisplayMetrics();
                        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
                        float finalRadius = Math.max(displayMetrics.widthPixels, displayMetrics.heightPixels);

                        Animator circularReveal = ViewAnimationUtils.createCircularReveal(rootLayout, 645, 870, 0, finalRadius);
                        circularReveal.setDuration(1000);
                        circularReveal.start();
                    }
                    rootLayout.setVisibility(View.VISIBLE);
                    rootLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            });
        }
    }

    /*vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv
     * Database changes logic section
     */

    private void reserveWish() {
        if (!editWishBean.isReserved()) {
            reservedDateDialog.setOnDateSetListener(new CalendarDatePickerDialogFragment.OnDateSetListener() {
                @Override
                public void onDateSet(CalendarDatePickerDialogFragment dialog, int year, int monthOfYear, int dayOfMonth) {
                    editWishBean.setReservation(new Reservation(AuthUtils.getCurrentUser().getId(), dialog.getSelectedDay().getDateInMillis()));
                    Toast.makeText(getApplicationContext(), "wish " + editWishBean.getTitle() + " reserved", Toast.LENGTH_SHORT).show();
                }
            });
            reservedDateDialog.show(getSupportFragmentManager(), DATE_DIALOG);
        } else {
            DialogUtil.alertShow(getString(R.string.app_name), getString(R.string.message_unreserve), this, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    editWishBean.setReservation(null);
                    Toast.makeText(getApplicationContext(), "wish " + editWishBean.getTitle() + " unreserved", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void deleteWish() {
        DialogUtil.alertShow(getString(R.string.app_name), getString(R.string.message_remove_wish_dialog), this, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                editWishBean.softRemove();
                Toast.makeText(getApplicationContext(), "wish " + editWishBean.getTitle() + " deleted", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        //TODO: need to show SnackBar on main activity, like after swipe to remove, may be start this activity for result and return something like "removed" flag and than show SnackBar?
    }

    private void commitChanges() {
        if (!getIntent().getAction().equals(ACTION_READ)) {
            if (editWishBean.isPictureChanged() && editWishBean.getOriginalWish().getPicture() != null) {
                CloudinaryUtil.destroy(editWishBean.getOriginalWish().getPicture());//destroy old image on cloud
            }
            editWishBean.setComment(editTextComment.getText().toString().trim());
            editWishBean.setTitle(editTextTitle.getText().toString().trim());
        }
        if (getIntent().getAction().equals(ACTION_CREATE) || getIntent().getAction().equals(ACTION_TAKE_FROM_TOP)) {
            editWishBean.push();
        } else  {
            Wish.getFirebaseRef().child(editWishBean.getId()).updateChildren(editWishBean.toMap());
        }
        Toast.makeText(this, editWishBean.getTitle(), Toast.LENGTH_SHORT).show();
        finish();
    }

    private void share() {
//        if (TwitterUtils.isConnected() && twitterChekbox.isChecked()) {
//            if (ConnectionUtil.isConnected()) {
//                SocialShareUtils.ref().share(String.format("I have a new wish, it is %s!", editWishBean.getTitle()), social, new ShareStrategy.SharingCallback() {
//                    @Override
//                    public void success() {
//                        Toast.makeText(getApplicationContext(), "Tweet successful published",
//                                Toast.LENGTH_SHORT).show();
//                    }
//
//                    @Override
//                    public void failure(Throwable e) {
//                        Toast.makeText(getApplicationContext(), "Problem with share sending",
//                                Toast.LENGTH_SHORT).show();
//                    }
//                });
//            } else {
//                Toast.makeText(this, "Check internet connection for sharing", Toast.LENGTH_SHORT).show();
//            }
//        }
    }

    private void discardChanges() {
        if (editWishBean.isPictureChanged()) {
            CloudinaryUtil.destroy(editWishBean.getPicture());//destroy new image on cloud
        }
    }

    /*
     * Database changes logic section end
     *^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^*/

    @OnClick({R.id.fab})
    public void showImagesDialog(View view) {
        if (ConnectionUtil.isConnected()) {
            switch (view.getId()) {
                case R.id.fab:
                    String query = editTextTitle.getText().toString().trim();
                    if (query.isEmpty()) {
                        Snackbar.make(coordinatorLayout, "Wish title should be not empty!", Snackbar.LENGTH_SHORT).show();
                    } else {
                        Intent intent = new Intent(this, ImageSearchActivity.class);
                        intent.putExtra(ImageSearchActivity.QUERY, editTextTitle.getText().toString());
                        startActivityForResult(intent, RESULT_IMAGE_SELECT);
                    }
                    break;
                //TODO: move loading image from storage to ImageSearchActivity
//                case R.id.gallery_image_btn:
//                    startActivityForResult(Intent.createChooser(new Intent(Intent.ACTION_GET_CONTENT)
//                            .setType("image/*"), getString(R.string.choose_image)), RESULT_LOAD_IMAGE);
//                    break;
            }
        } else {
            Snackbar.make(coordinatorLayout, getResources().getString(R.string.no_connection), Snackbar.LENGTH_LONG).show();
        }
    }

    /**
     * called when user choose image from device
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK && null != data) {
            if (ConnectionUtil.isConnected()) {
                CloudinaryUtil.IOnDoneListener listener = new CloudinaryUtil.IOnDoneListener() {
                    @Override
                    public void onDone(final Map<String, Object> imgInfo) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (editWishBean.isPictureChanged() && editWishBean.getPicture() != null) {
                                    CloudinaryUtil.destroy(editWishBean.getPicture());//if user pick image second time destroy old
                                }
                                editWishBean.setPicture((String) imgInfo.get("public_id"));
                                Glide.with(getApplicationContext()) //TODO: load optimized image
                                        .load(CloudinaryUtil.getInstance().url().generate(editWishBean.getPicture()))
                                        .crossFade()
                                        .into(imageView);
                            }
                        });
                    }
                };
                try {
                    if (requestCode == RESULT_LOAD_IMAGE) {
                        Uri selectedImageUri = data.getData();
                        InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                        CloudinaryUtil.upload(inputStream, listener);
                    } else if (requestCode == RESULT_IMAGE_SELECT) {
                        final String url = data.getStringExtra(ImageSearchActivity.RESULT_DATA).trim();
                        if (url.isEmpty()) throw new IOException("Something went wrong...");
                        CloudinaryUtil.upload(url, listener);
                    }
                } catch (IOException e) {
                    Snackbar.make(findViewById(R.id.coordinator_layout), e.getMessage(), Snackbar.LENGTH_SHORT);
                }
            } else {
                Snackbar.make(coordinatorLayout, getResources().getString(R.string.no_connection), Snackbar.LENGTH_LONG).show();
            }
        }
    }

    /*vvvvvvvvvvvvvvvvvvv
     * Validation section
     */

    @Override
    public void onValidationSucceeded() {
        commitChanges();
    }

    @Override
    public void onValidationFailed(List<ValidationError> errors) {
        for (ValidationError error : errors) {
            View view = error.getView();
            String message = error.getCollatedErrorMessage(this);
            if (view instanceof EditText) {
                ((EditText) view).setError(message);
            } else {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        }
    }
    /*
     * Validation section end
     *^^^^^^^^^^^^^^^^^^^^^^^*/
}
