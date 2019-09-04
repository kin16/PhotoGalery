package com.example.photogallery;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.fragment.app.Fragment;

public class PhotoPageActivity extends SingleFragmentActivity {

    public static Intent newIntent(Context context, Uri uri){
        Intent intent = new Intent(context, PhotoPageActivity.class);
        intent.setData(uri);
        return intent;
    }

    @Override
    protected Fragment createFragment() {
        return PhotoPageFragment.newInstance(getIntent().getData());
    }

    public void onBackPressed(){
        PhotoPageFragment fragment = (PhotoPageFragment)
                getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if(fragment.getmWebView().canGoBack()){
            fragment.getmWebView().goBack();
        }else {
            super.onBackPressed();
        }
    }
}
