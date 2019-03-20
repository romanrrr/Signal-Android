package org.thoughtcrime.securesms;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;

import com.appsgeyser.sdk.AppsgeyserSDK;

import org.thoughtcrime.securesms.logging.Log;

public class StartActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("appsgeyser", "start activity");
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        editor.putBoolean("takeoff", true).commit();
        Intent intent = new Intent(this, ConversationListActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AppsgeyserSDK.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        AppsgeyserSDK.onPause(this);
    }
}
