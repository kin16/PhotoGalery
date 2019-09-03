package com.example.photogallery;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

public class VisibleFragment extends Fragment {
    private static String TAG = "VisibleFragment";

    @Override
    public void onStart(){
        super.onStart();
        IntentFilter intentFilter = new IntentFilter(PollService.ACTION_SHOW_NOTIFICATION);
        getActivity().registerReceiver(broadcastReceiver, intentFilter,PollService.PERM_PRIVATE, null);
    }

    @Override
    public void onStop(){
        super.onStop();
        getActivity().unregisterReceiver(broadcastReceiver);
    }

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(getActivity(), "Got a broadcast: " + intent.getAction(),
                    Toast.LENGTH_LONG).show();

            Log.i(TAG, "cancelling notification");
            setResultCode(Activity.RESULT_CANCELED);
        }
    };
}
