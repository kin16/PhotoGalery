package com.example.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.util.LruCache;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ThumbnailDownloader<T>  extends HandlerThread {
    private Handler mRequestHandler;
    private Handler mResponseHandler;
    private ConcurrentMap<T,String> mRequestMap = new ConcurrentHashMap<>();
    private static final int MESSAGE_DOWNLOAD = 0;
    private static final int MESSAGE_PRELOAD = 1;
    public static String TAG = "ThumbnailDownloader";
    private LruCache<String, Bitmap> mLruCache;
    private ThumbnailDownloadListener<T> mThumbnailDownloadListener;

    public interface ThumbnailDownloadListener<T>{
        void onThumbnailDownloaded(T target, Bitmap thumbnail);
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> mThumbnailDownloadListener) {
        this.mThumbnailDownloadListener = mThumbnailDownloadListener;
    }

    public ThumbnailDownloader(Handler responseHandler) {
        super(TAG);
        mResponseHandler = responseHandler;
        mLruCache = new LruCache<>(20480);
    }

    @Override
    protected void onLooperPrepared() {
        mRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_DOWNLOAD:
                        T target = (T) msg.obj;
                        Log.i(TAG, "Got a request for URL: " + mRequestMap.get(target));
                        handleRequest(target);
                        break;
                    case MESSAGE_PRELOAD:
                        String url = (String) msg.obj;
                        download(url);
                        break;
                }
            }
        };
    }

    public void queueThumbnail(T target, String url){
        Log.i(TAG, "Got a url" + url);

        if(url == null){
            mRequestMap.remove(target);
        }else {
            mRequestMap.put((T) target, url);
            mRequestHandler.obtainMessage(MESSAGE_DOWNLOAD,target)
                    .sendToTarget();
        }
    }
    public void handleRequest(final T target){
       final Bitmap bitmap;
       final String url = mRequestMap.get(target);
       if(url ==  null){
           return;
       }

       bitmap = download(url);
       Log.i(TAG, "Bitmap created");

       mResponseHandler.post(new Runnable() {
           @Override
           public void run() {
               if (mRequestMap.get(target) != url) {
                   return;
               }
               mRequestMap.remove(target);
               mThumbnailDownloadListener.onThumbnailDownloaded(target, bitmap);
           }
       });
    }

    private Bitmap download(String url){
        Bitmap bitmap;

        bitmap = mLruCache.get(url);
        if (bitmap != null){
            return bitmap;
        }

        try {
            byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(url);
            bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            mLruCache.put(url, bitmap);
            Log.i(TAG, "Download and cached image: " + url);
            return bitmap;
        } catch (IOException ex) {
            Log.e(TAG, "Error downloading image.", ex);
            return null;
        }
    }
    public void clearQueue(){
        mRequestHandler.removeMessages(MESSAGE_DOWNLOAD);
    }

    public void preloadImage(String url){
        mRequestHandler.obtainMessage(MESSAGE_PRELOAD, url).sendToTarget();
    }

    public void clearCache() {
        mLruCache.evictAll();
    }

    public Bitmap getCachedImage(String url) {
        return mLruCache.get(url);
    }
}
