package com.company.wishlist.activity.abstracts;

import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;

import com.company.wishlist.R;
import com.company.wishlist.activity.LoginActivity;
import com.company.wishlist.util.FirebaseUtil;
import com.company.wishlist.util.social.FacebookUtil;
import com.firebase.client.AuthData;
import com.firebase.client.FirebaseError;

/**
 * Created by v.odahovskiy on 12.01.2016.
 */
public abstract class FirebaseActivity extends AuthActivity implements FirebaseUtil.IFirebaseConnection {

    public static final String RELOAD_DATA = "RELOAD_DATA";

    private FirebaseUtil firebaseUtil;
    private String authToken;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseUtil = new FirebaseUtil(this);
        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey(LoginActivity.AUTH_TOKEN_EXTRA)) {
            authToken = extras.getString(LoginActivity.AUTH_TOKEN_EXTRA);
            processFirebaseLogin();
        } else {
            if (firebaseUtil.isDisconnected()) {
                firebaseUtil.unauth();
                FacebookUtil.processFacebookLogin(getApplicationContext());
                finish();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (null != this.getIntent().getExtras()) {
            boolean reloadData = this.getIntent().getExtras().getBoolean(RELOAD_DATA, false);
            if (reloadData) {
                firebaseUtil.refreshAuth();
            }
        }
    }

    private void processFirebaseLogin() {
        if (isConnected()) {
            if (!firebaseUtil.isAuthenticated()) {
                firebaseUtil.auth("facebook", authToken);
            }
        }
    }

    public void logOut() {
        if (firebaseUtil.isAuthenticated()) {
            firebaseUtil.unauth();
            FacebookUtil.processFacebookLogout(getApplicationContext());
            finish();
        }
    }

    /**
     * FirebaseFragment.Callbacks implementation
     */
    @Override
    public void onAuthenticated(AuthData authData) {
        invalidateOptionsMenu();
    }

    @Override
    public void onAuthenticationError(FirebaseError error) {
        invalidateOptionsMenu();
        showErrorDialog(error.getMessage());
    }

    @Override
    public void onMissingConnection() {
        invalidateOptionsMenu();
        showErrorDialog("Missing connection");
    }

    private void showErrorDialog(String message) {
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.app_name))
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    public FirebaseUtil getFirebaseUtil() {
        return firebaseUtil;
    }

}
