package com.example.photogallery;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PhotoGalleryFragment extends Fragment {

    private static final String TAG = "PhotoGalleryFragment";

    private RecyclerView mPhotoRecyclerView;
    private List<GalleryItem> mItems = new ArrayList<>();
    private int mLastPage = 1;
    private ThumbnailDownloader<PhotoHolder> mThumbnailDownloader;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        new FetchItemsTask().execute();

        Handler responseHandler = new Handler();
        mThumbnailDownloader = new ThumbnailDownloader<>(responseHandler);
        mThumbnailDownloader.setThumbnailDownloadListener(
                new ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder>() {
                    @Override
                    public void onThumbnailDownloaded(PhotoHolder target, Bitmap thumbnail) {
                        Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
                        target.bindDrawable(drawable);
                    }
                }
        );
        mThumbnailDownloader.start();
        mThumbnailDownloader.getLooper();
        Log.i(TAG, "Background thread started");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        mPhotoRecyclerView = (RecyclerView) v.findViewById(R.id.photo_recycler_view);
        mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(), 3));

        mPhotoRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                PhotoAdapter adapter = (PhotoAdapter)recyclerView.getAdapter();
                int lastPosition = adapter.getLastPosition();
                GridLayoutManager layoutManager = (GridLayoutManager)recyclerView.getLayoutManager();
                int loadBufferPosition = 1;
                if(lastPosition >= adapter.getItemCount() - layoutManager.getSpanCount() - loadBufferPosition) {
                    new FetchItemsTask().execute(lastPosition + 1);
                   //Toast.makeText(getActivity(), "Page Number " + mLastPage, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });

        mPhotoRecyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void onGlobalLayout() {
                mPhotoRecyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                int numberColumns = mPhotoRecyclerView.getWidth()/360;
                mPhotoRecyclerView.setLayoutManager(new GridLayoutManager(getActivity(),numberColumns));
            }
        });

        setupAdapter();

        return v;
    }

    public void onDestroyView(){
        super.onDestroyView();
        mThumbnailDownloader.clearQueue();
    }

    public void onDestroy(){
        super.onDestroy();
        mThumbnailDownloader.quit();
        Log.i(TAG, "Background thread destroyed");
    }

    private void setupAdapter() {
        if (isAdded()) {
            mPhotoRecyclerView.setAdapter(new PhotoAdapter(mItems));
        }
    }

    private class PhotoHolder extends RecyclerView.ViewHolder {
        private ImageView mItemImageView;

        public PhotoHolder(View itemView) {
            super(itemView);

            mItemImageView = itemView.findViewById(R.id.fragment_photo_gallery_image);
        }

        public void bindDrawable(Drawable drawable) {
            mItemImageView.setImageDrawable(drawable);
        }
    }

    private class PhotoAdapter extends RecyclerView.Adapter<PhotoHolder> {

        private List<GalleryItem> mGalleryItems;
        private int lastPosition;

        public int getLastPosition() {
            return lastPosition;
        }

        public PhotoAdapter(List<GalleryItem> galleryItems) {
            mGalleryItems = galleryItems;
        }

        @Override
        public PhotoHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
          LayoutInflater inflater = LayoutInflater.from(getActivity());
          View view = inflater.inflate(R.layout.gallery_item, viewGroup, false);
          return new PhotoHolder(view);
        }

        @Override
        public void onBindViewHolder(PhotoHolder photoHolder, int position) {
            GalleryItem galleryItem = mGalleryItems.get(position);

            Bitmap bitmap = mThumbnailDownloader.getCachedImage(galleryItem.getUrl());
            if(bitmap == null){
                Drawable placeholder = getResources().getDrawable(R.drawable.icon);
                photoHolder.bindDrawable(placeholder);
                mThumbnailDownloader.queueThumbnail(photoHolder, galleryItem.getUrl());
            }else{
                Log.i(TAG, "Loaded image from cache");
                photoHolder.bindDrawable(new BitmapDrawable(getResources(), bitmap));
            }

            preLoadAdjacentImage(position);

            lastPosition = position;
            Log.i(TAG,"Last bound position is " + Integer.toString(lastPosition));
        }

        @Override
        public int getItemCount() {
            return mGalleryItems.size();
        }
    }

    private void preLoadAdjacentImage(int position) {
        final int imageBufferSize = 10;

        int startIndex = Math.max(position - imageBufferSize, 0);
        int endIndex = Math.min(position + imageBufferSize, mItems.size() - 1);

        for (int i = startIndex; i <= endIndex; i++) {
            if (i == position) continue;

            String url = mItems.get(i).getUrl();
            mThumbnailDownloader.preloadImage(url);
        }
    }

    private class FetchItemsTask extends AsyncTask<Integer,Void,List<GalleryItem>> {
        @Override
        protected List<GalleryItem> doInBackground(Integer... params) {
            return new FlickrFetchr().fetchItems(mLastPage);
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            if (mLastPage > 1) {
                mItems.addAll(items);
                mPhotoRecyclerView.getAdapter().notifyDataSetChanged();
            } else {
                mItems = items;
                setupAdapter();
            }
            mLastPage++;
        }
    }
}