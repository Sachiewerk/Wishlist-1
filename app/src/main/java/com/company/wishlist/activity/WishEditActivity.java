package com.company.wishlist.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.codetroopers.betterpickers.calendardatepicker.CalendarDatePickerDialogFragment;
import com.company.wishlist.R;
import com.company.wishlist.activity.abstracts.InternetActivity;
import com.company.wishlist.bean.EditWishBean;
import com.company.wishlist.fragment.WishListFragment;
import com.company.wishlist.model.Wish;
import com.company.wishlist.util.CropCircleTransformation;
import com.company.wishlist.util.DialogUtil;
import com.company.wishlist.util.FirebaseUtil;
import com.company.wishlist.util.LocalStorage;
import com.company.wishlist.util.Utilities;
import com.mobsandgeeks.saripaar.ValidationError;
import com.mobsandgeeks.saripaar.Validator;
import com.mobsandgeeks.saripaar.annotation.Length;
import com.mobsandgeeks.saripaar.annotation.NotEmpty;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by vladstarikov on 15.01.16.
 */
public class WishEditActivity extends InternetActivity implements Validator.ValidationListener {

    private static int RESULT_LOAD_IMAGE = 1;
    private static int RESULT_IMAGE_SELECT = 2;
    private static String DATE_DIALOG = "DATE_PICKER";
    public static String ACTION_EDIT = "com.company.wishlist.ACTION_EDIT";
    public static String ACTION_CREATE = "com.company.wishlist.ACTION_CREATE";
    public static String ACTION_TAKE_FROM_TOP = "com.company.wishlist.ACTION_TAKE_FROM_TOP";

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

    @Bind(R.id.insta_images_btn)
    ImageButton instaImgBtn;

    @Bind(R.id.insta_layout)
    LinearLayout instaLayout;


    private EditWishBean editWishBean;
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
        reservedDateDialog.setThemeDark(true);

        initWishEdit();
        initView();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_wish_edit, menu);
        if (getIntent().getAction().equals(ACTION_CREATE)) {
            menu.findItem(R.id.action_delete).setVisible(false);
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
                LocalStorage.getInstance().setWish(null);
                finish();
                return false;
            case R.id.action_reserve:
                reserveWish();
                return false;
            case R.id.action_delete:
                deleteWish();
                return false;
        }
        return super.onOptionsItemSelected(item);
    }

    /*TODO: remove singleton use firebase cache
      - just put wish list or wish id into extra when calling this activity
      - if it not creating new wish run DB query that pull Wish from Firebase cache,
        and will be update views if wish changed, or close this activity if we delete wish.
        At this moment we have several bugs without query listener.
      - try to remove editWishBean
      //TODO: alternative way
      - we can add onComplete listeners to all edit queries
     */
    public void initWishEdit() {
        if (getIntent().getAction().equals(ACTION_CREATE)) {
            editWishBean = new EditWishBean(new Wish());
            editWishBean.setWishListId(getIntent().getStringExtra(WishListFragment.WISH_LIST_ID));
        } else if (getIntent().getAction().equals(ACTION_EDIT)) {
            editWishBean = new EditWishBean(LocalStorage.getInstance().getWish());
        } else if (getIntent().getAction().equals(ACTION_TAKE_FROM_TOP)) {
            editWishBean = new EditWishBean(LocalStorage.getInstance().getWish());
            editWishBean.setId(null);
            editWishBean.setWishListId(getIntent().getStringExtra(WishListFragment.WISH_LIST_ID));
        }
    }

    private void initView() {
        editTextTitle.setText(editWishBean.getTitle());
        editTextComment.setText(editWishBean.getComment());
        if (!Utilities.isBlank(editWishBean.getPicture())) {
            //Glide.with(this).load(Utilities.decodeThumbnail(editWishBean.getPicture())).into(imageView);
            imageView.setImageBitmap(Utilities.decodeThumbnail(editWishBean.getPicture()));//TODO: CropCircleTransformation
        } else {
            imageView.setImageResource(R.drawable.gift_icon);
        }
    }

    /**
     * Change logic
     */
    private void reserveWish() {
        if (!editWishBean.isReserved()) {
            reservedDateDialog.setOnDateSetListener(new CalendarDatePickerDialogFragment.OnDateSetListener() {
                @Override
                public void onDateSet(CalendarDatePickerDialogFragment dialog, int year, int monthOfYear, int dayOfMonth) {
                    editWishBean.reserve(FirebaseUtil.getCurrentUser().getId(), dialog.getSelectedDay().getDateInMillis());
                    Toast.makeText(getApplicationContext(), "wish " + editWishBean.getTitle() + " reserved", Toast.LENGTH_SHORT).show();
                }
            });
            reservedDateDialog.show(getSupportFragmentManager(), DATE_DIALOG);
        } else {
            DialogUtil.alertShow(getString(R.string.app_name), getString(R.string.unreserve), this, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    editWishBean.unreserve();
                    Toast.makeText(getApplicationContext(), "wish " + editWishBean.getTitle() + " unreserved", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void deleteWish() {
        DialogUtil.alertShow(getString(R.string.app_name), getString(R.string.remove_wish_dialog_text), this, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                editWishBean.softRemove();
                Toast.makeText(getApplicationContext(), "wish " + editWishBean.getTitle() + " deleted", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
        //TODO: need to show SnackBar on main activity, like after swipe to remove, may be start this activity for result and return something like "removed" flag and than show SnackBar?
    }

    private void commitChanges() {
        editWishBean.setComment(editTextComment.getText().toString());
        editWishBean.setTitle(editTextTitle.getText().toString());
        if (getIntent().getAction().equals(ACTION_CREATE) || getIntent().getAction().equals(ACTION_TAKE_FROM_TOP)) {
            editWishBean.push();
        } else if (getIntent().getAction().equals(ACTION_EDIT)) {
            Wish.getFirebaseRef().child(editWishBean.getId()).updateChildren(editWishBean.toMap());
        }
        Toast.makeText(this, editWishBean.getTitle(), Toast.LENGTH_SHORT).show();
    }

    @OnClick(R.id.insta_images_btn)
    public void showInstaImagesDialog(View view) {
        //String[] tags = editTextTitle.getText().toString().split("\\s+");
        final Context app = this;

        String query = editTextTitle.getText().toString().trim();

        String message = "Wish title should be not empty!";
        if (StringUtils.isEmpty(query)) {
            showSnake(message);
        } else {
            Intent intent = new Intent(this, ImageGridActivity.class);
            intent.putExtra(ImageGridActivity.QUERY, editTextTitle.getText().toString());
            startActivityForResult(intent, RESULT_IMAGE_SELECT);
        }
    }

    private void showSnake(String message) {
        Snackbar.make(findViewById(R.id.coordinator_layout_wish_edit), message, Snackbar.LENGTH_SHORT).show();
    }

    @OnClick(R.id.galery_image_btn)
    public void chooseWishImage(ImageView view) {
        startActivityForResult(
                Intent.createChooser(
                        new Intent(Intent.ACTION_GET_CONTENT)
                                .setType("image/*"), getString(R.string.choose_image)),
                RESULT_LOAD_IMAGE);
    }

    /**
     * called when user choose image from device
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            try {
                Uri selectedImageUri = data.getData();
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getApplicationContext().getContentResolver(), selectedImageUri);
                editWishBean.setPicture(Utilities.encodeThumbnail(bitmap));
                Glide.with(this)
                        .load(selectedImageUri)
                        .bitmapTransform(new CropCircleTransformation(Glide.get(this).getBitmapPool()))
                        .into(imageView);
            } catch (IOException e) {
                Snackbar.make(findViewById(R.id.coordinator_layout_wish_edit), e.getMessage(), Snackbar.LENGTH_SHORT);
            }
        } else if (requestCode == RESULT_IMAGE_SELECT && resultCode == RESULT_OK && null != data) {
            String url = data.getStringExtra(ImageGridActivity.RESULT_DATA).trim();
            if (StringUtils.isEmpty(url)) {
                showSnake("Something went wrong..");
            } else {
                Glide.with(getApplicationContext())
                        .load(url)
                        .bitmapTransform(new CropCircleTransformation(Glide.get(getApplicationContext()).getBitmapPool()))
                        .into(imageView);
                editWishBean.setPicture(Utilities.encodeThumbnail(Utilities.getBitmapFromURL(url)));
            }
        }
    }

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

}
