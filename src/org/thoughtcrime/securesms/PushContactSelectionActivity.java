/*
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.thoughtcrime.securesms;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;

import org.thoughtcrime.securesms.util.UiUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity container for selecting a list of contacts.
 *
 * @author Moxie Marlinspike
 *
 */
public class PushContactSelectionActivity extends ContactSelectionActivity {

  @SuppressWarnings("unused")
  private final static String TAG = PushContactSelectionActivity.class.getSimpleName();

  @Override
  protected void onCreate(Bundle icicle, boolean ready) {
    getIntent().putExtra(ContactSelectionListFragment.MULTI_SELECT, true);
    super.onCreate(icicle, ready);

    Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_check_white_24dp);
    drawable.setColorFilter(UiUtils.themeAttributeToColor(R.attr.toolbar_icon_color, this, R.color.white), PorterDuff.Mode.SRC_ATOP);
    getToolbar().setNavigationIcon(drawable);

    getToolbar().setNavigationOnClickListener(v -> {
      Intent resultIntent = getIntent();
      List<String> selectedContacts = contactsFragment.getSelectedContacts();

      if (selectedContacts != null) {
        resultIntent.putStringArrayListExtra("contacts", new ArrayList<>(selectedContacts));
      }

      setResult(RESULT_OK, resultIntent);
      finish();
    });
  }
}
