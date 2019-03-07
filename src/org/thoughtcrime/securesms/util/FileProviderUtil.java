package org.thoughtcrime.securesms.util;


import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.FileProvider;

import java.io.File;

public class FileProviderUtil {

  public static String AUTHORITY(Context context) {
    return context.getPackageName()+".securesms.fileprovider";
  }

  public static Uri getUriFor(@NonNull Context context, @NonNull File file) {
    if (Build.VERSION.SDK_INT >= 24) return FileProvider.getUriForFile(context, AUTHORITY(context), file);
    else                             return Uri.fromFile(file);
  }

  public static boolean isAuthority(Context context, @NonNull Uri uri) {
    return AUTHORITY(context).equals(uri.getAuthority());
  }

  public static boolean delete(@NonNull Context context, @NonNull Uri uri) {
    if (AUTHORITY(context).equals(uri.getAuthority())) {
      return context.getContentResolver().delete(uri, null, null) > 0;
    }
    return new File(uri.getPath()).delete();
  }
}
