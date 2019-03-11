package org.thoughtcrime.securesms.util;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.TypedValue;
import android.view.View;
import android.view.Window;

import org.thoughtcrime.securesms.R;

import static org.thoughtcrime.securesms.util.DynamicTheme.DARK;
import static org.thoughtcrime.securesms.util.DynamicTheme.LIGHT;

public class UiUtils {
    public static int themeAttributeToColor(int themeAttributeId,
                                            Context context,
                                            int fallbackColorId) {
        TypedValue outValue = new TypedValue();
        Resources.Theme theme = context.getTheme();
        boolean wasResolved =
                theme.resolveAttribute(
                        themeAttributeId, outValue, true);
        if (wasResolved) {
            return ContextCompat.getColor(
                    context, outValue.resourceId);
        } else {
            return ContextCompat.getColor(
                    context, fallbackColorId);
        }
    }

    public static void setLightStatusBar(Activity activity){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            /*int flags = activity.getWindow().getDecorView().getSystemUiVisibility(); // get current flag
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;   // add LIGHT_STATUS_BAR to flag
            activity.getWindow().getDecorView().setSystemUiVisibility(flags);*/
            activity.getWindow().setStatusBarColor(UiUtils.themeAttributeToColor(R.attr.mainStatusBarColor, activity, R.color.status_bar_light_color)); // optional
        }
    }

    public static void setDarkStatusBar(Activity activity){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Window window = activity.getWindow();

            int flags = activity.getWindow().getDecorView().getSystemUiVisibility(); // get current flag
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;   // add LIGHT_STATUS_BAR to flag
            activity.getWindow().getDecorView().setSystemUiVisibility(flags);

            window.setStatusBarColor(UiUtils.themeAttributeToColor(R.attr.mainStatusBarColor, activity, R.color.black));
        }
    }

    public static void setThemedStatusBar(Activity activity) {
        String theme = TextSecurePreferences.getTheme(activity);

        if (theme.equals(DARK)){
            setDarkStatusBar(activity);
        }else {
            setLightStatusBar(activity);
        }
    }

    public static boolean isDarkTheme(Context context){
        String theme = TextSecurePreferences.getTheme(context);

        return theme.equals(DARK);
    }
}
