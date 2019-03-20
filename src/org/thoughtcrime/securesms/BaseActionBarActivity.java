package org.thoughtcrime.securesms;

import android.annotation.TargetApi;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

import com.appsgeyser.sdk.AppsgeyserSDK;
import com.appsgeyser.sdk.ads.fastTrack.adapters.FastTrackBaseAdapter;
import com.appsgeyser.sdk.configuration.Constants;

import org.thoughtcrime.securesms.config.Config;
import org.thoughtcrime.securesms.logging.Log;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.thoughtcrime.securesms.util.UiUtils;

import java.lang.reflect.Field;


public abstract class BaseActionBarActivity extends AppCompatActivity {
  private static final String TAG = BaseActionBarActivity.class.getSimpleName();

  private static final long FULLSCREEN_BANNER_TIMEOUT = 4 * 60 * 1000;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    if (BaseActivity.isMenuWorkaroundRequired()) {
      forceOverflowMenu();
    }
    super.onCreate(savedInstanceState);
    if(!isFinishing()) {
      initializeAppsgeyser();
    }
    UiUtils.setThemedStatusBar(this);
  }

  public void initializeAppsgeyser(){
    if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("takeoff", true)){
      Log.d("appsgeyser", "takeoff");
      AppsgeyserSDK.takeOff(this,
              getString(R.string.widgetID),
              getString(R.string.app_metrica_on_start_event),
              getString(R.string.template_version));
      PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("takeoff", false).commit();
    }
  }

  @Override
  protected void onResume() {
    super.onResume();
    initializeScreenshotSecurity();
    AppsgeyserSDK.onResume(this);
  }

  @Override
  protected void onPause() {
    super.onPause();
    AppsgeyserSDK.onPause(this);
  }

  public void showFullscreen(String bannerTag){
    long lastBannerTime = PreferenceManager.getDefaultSharedPreferences(this).getLong("lastBannerTime", 0L);
    if(System.currentTimeMillis() - lastBannerTime > FULLSCREEN_BANNER_TIMEOUT) {
        AppsgeyserSDK.getFastTrackAdsController()
                .showFullscreen(Constants.BannerLoadTags.ON_START, this, bannerTag);
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit().putLong("lastBannerTime", System.currentTimeMillis()).apply();
    }

  }

  @Override
  public boolean onKeyDown(int keyCode, KeyEvent event) {
    return (keyCode == KeyEvent.KEYCODE_MENU && BaseActivity.isMenuWorkaroundRequired()) || super.onKeyDown(keyCode, event);
  }

  @Override
  public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
    if (keyCode == KeyEvent.KEYCODE_MENU && BaseActivity.isMenuWorkaroundRequired()) {
      openOptionsMenu();
      return true;
    }
    return super.onKeyUp(keyCode, event);
  }

  private void initializeScreenshotSecurity() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH &&
            TextSecurePreferences.isScreenSecurityEnabled(this))
    {
      getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    } else {
      getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }
  }

  /**
   * Modified from: http://stackoverflow.com/a/13098824
   */
  private void forceOverflowMenu() {
    try {
      ViewConfiguration config       = ViewConfiguration.get(this);
      Field             menuKeyField = ViewConfiguration.class.getDeclaredField("sHasPermanentMenuKey");
      if(menuKeyField != null) {
        menuKeyField.setAccessible(true);
        menuKeyField.setBoolean(config, false);
      }
    } catch (IllegalAccessException e) {
      Log.w(TAG, "Failed to force overflow menu.");
    } catch (NoSuchFieldException e) {
      Log.w(TAG, "Failed to force overflow menu.");
    }
  }

  protected void startActivitySceneTransition(Intent intent, View sharedView, String transitionName) {
    Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(this, sharedView, transitionName)
                                         .toBundle();
    ActivityCompat.startActivity(this, intent, bundle);
  }

  @TargetApi(VERSION_CODES.LOLLIPOP)
  protected void setStatusBarColor(int color) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      getWindow().setStatusBarColor(color);
    }
  }

}
