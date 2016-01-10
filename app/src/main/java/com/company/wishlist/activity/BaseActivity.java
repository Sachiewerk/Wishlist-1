package com.company.wishlist.activity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;

import com.company.wishlist.FirebaseFragment;
import com.company.wishlist.R;
import com.company.wishlist.model.User;
import com.company.wishlist.util.IntentUtil;
import com.company.wishlist.util.Utilities;
import com.firebase.client.AuthData;
import com.firebase.client.Firebase;
import com.firebase.client.FirebaseError;


public abstract class BaseActivity extends DebugActivity  implements FirebaseFragment.Callbacks{

    private IntentUtil intentUtil;
    private FirebaseFragment mFirebaseFragment;
    private Firebase mFirebase;
    private User mUser;
    private ProgressDialog mAuthProgressDialog;
    private String mAuthToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        intentUtil = new IntentUtil(this);

        setupFirebase();

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey(LoginActivity.AUTH_TOKEN_EXTRA)) {
            mAuthToken = extras.getString(LoginActivity.AUTH_TOKEN_EXTRA);
            processFirebaseLogin();
        } else {
            if (!mFirebaseFragment.isAuthenticated() || isTokenExpired()) {
                mFirebase.unauth();
                processFacebookLogin();
                return;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(!Utilities.isConnected(getApplicationContext())){
            onMissingConnection();
        }
    }

    private void processFacebookLogin() {
        intentUtil.showLoginActivity();
        finish();
    }

    private void processFirebaseLogin() {
        mAuthProgressDialog = new ProgressDialog(this);
        mAuthProgressDialog.setTitle(getString(R.string.app_name));
        mAuthProgressDialog.setMessage("Reconnection");
        mAuthProgressDialog.setCancelable(false);
        mAuthProgressDialog.show();
        if (!mFirebaseFragment.isAuthenticated()) {
            mFirebaseFragment.authenticate("facebook", mAuthToken);
        } else {
            mAuthProgressDialog.hide();
        }
    }

    private void setupFirebase() {
        mFirebaseFragment = Utilities.setupFirebase(getSupportFragmentManager());
        mFirebase = mFirebaseFragment.getFirebase();
        mUser = mFirebaseFragment.getUser();
    }

    private boolean isTokenExpired() {
        return (mFirebaseFragment.getAuthdata() == null
                || Utilities.isExpired(mFirebaseFragment.getAuthdata().getExpires()));
    }

    @Override
    public void onResume() {
        super.onResume();
        if(!Utilities.isConnected(getApplicationContext())){
            onMissingConnection();
        }
    }

    /**
     * FirebaseFragment.Callbacks implementation
     */
    @Override
    public void onAuthenticated(AuthData authData) {
        invalidateOptionsMenu();
        mAuthProgressDialog.hide();
    }

    @Override
    public void onAuthenticationError(FirebaseError error) {
        invalidateOptionsMenu();
        mAuthProgressDialog.hide();
        showErrorDialog(error.getMessage());
    }

    @Override
    public void onMissingConnection() {
        showErrorDialog("No internet connection");
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.app_name))
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    /**
     * Getters and setters
     */
    protected boolean isAuthenticated() {
        return mFirebaseFragment.isAuthenticated();
    }

    public Firebase getFirebase() {
        return mFirebase;
    }

    public User getUser() {
        return mUser;
    }

    public FirebaseFragment getFirebaseFragment() {
        return mFirebaseFragment;
    }

    public void logout() {
        if (mFirebaseFragment.isAuthenticated()) {
            mFirebase.unauth();
            processFacebookLogout();
        }
    }

    private void processFacebookLogout() {
        Intent logoutIntent = new Intent(getApplicationContext(), LoginActivity.class);
        logoutIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        logoutIntent.putExtra(LoginActivity.INTENT_SIGNOUT, true);
        startActivity(logoutIntent);
        finish();
    }
}
