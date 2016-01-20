package com.company.wishlist.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.util.Patterns;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.concurrent.ExecutionException;

/**
 * Created by v.odahovskiy on 10.01.2016.
 */
public class Utilities {

    public static String encodeUrlForFirebase(String urlToEncode) {
        String urlEncoded = null;
        try {
            urlEncoded = URLEncoder.encode(urlToEncode, "UTF-8");
            urlEncoded = urlEncoded.replaceAll("\\.", "%2E");
        } catch (Exception e) {
            Log.v("encodeUrlForFirebase", "Catched encoding exception: " + e.getMessage());
        }
        return urlEncoded;
    }


    public static boolean isUrlValid(String urlToValidate) {
        return Patterns.WEB_URL.matcher(urlToValidate).matches();
    }

    public static boolean isBlank(String s) {
        return s == null || s.isEmpty();
    }

    public static boolean isExpired(long expirationDate) {
        return expirationDate <= System.currentTimeMillis() / 1000;
    }

    public static String encodeThumbnail(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP);
    }

    public static Bitmap decodeThumbnail(String thumbData) {
        byte[] bytes = Base64.decode(thumbData, Base64.NO_WRAP);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    public static Bitmap getBitmapFromURL(final String src) {

        class LoadImageFromUrl extends AsyncTask<String,Void,Bitmap>{

            @Override
            protected Bitmap doInBackground(String... params) {
                URL url = null;
                try {
                    url = new URL(params[0]);

                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setDoInput(true);
                    connection.connect();

                    InputStream input = connection.getInputStream();

                    return BitmapFactory.decodeStream(input);
                } catch (IOException e) {
                    return null;
                }

            }
        }
        try {
            return new LoadImageFromUrl().execute(src).get();
        } catch (InterruptedException | ExecutionException e) {
            return null;
        }
    }
}
