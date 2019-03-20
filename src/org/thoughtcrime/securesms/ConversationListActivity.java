/*
 * Copyright (C) 2014-2017 Open Whisper Systems
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

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.ActionMenuView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.TooltipCompat;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.appsgeyser.sdk.AppsgeyserSDK;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import org.thoughtcrime.securesms.color.MaterialColor;
import org.thoughtcrime.securesms.components.RatingManager;
import org.thoughtcrime.securesms.components.SearchToolbar;
import org.thoughtcrime.securesms.config.Config;
import org.thoughtcrime.securesms.contacts.avatars.ContactColors;
import org.thoughtcrime.securesms.contacts.avatars.GeneratedContactPhoto;
import org.thoughtcrime.securesms.contacts.avatars.ProfileContactPhoto;
import org.thoughtcrime.securesms.conversation.ConversationActivity;
import org.thoughtcrime.securesms.database.Address;
import org.thoughtcrime.securesms.database.DatabaseFactory;
import org.thoughtcrime.securesms.database.MessagingDatabase.MarkedMessageInfo;
import org.thoughtcrime.securesms.database.ThreadDatabase;
import org.thoughtcrime.securesms.lock.RegistrationLockDialog;
import org.thoughtcrime.securesms.logging.Log;
import org.thoughtcrime.securesms.mms.GlideApp;
import org.thoughtcrime.securesms.notifications.MarkReadReceiver;
import org.thoughtcrime.securesms.notifications.MessageNotifier;
import org.thoughtcrime.securesms.permissions.Permissions;
import org.thoughtcrime.securesms.recipients.Recipient;
import org.thoughtcrime.securesms.search.SearchFragment;
import org.thoughtcrime.securesms.service.KeyCachingService;
import org.thoughtcrime.securesms.util.DynamicLanguage;
import org.thoughtcrime.securesms.util.DynamicNoActionBarTheme;
import org.thoughtcrime.securesms.util.DynamicTheme;
import org.thoughtcrime.securesms.util.TextSecurePreferences;
import org.thoughtcrime.securesms.util.UiUtils;
import org.thoughtcrime.securesms.util.concurrent.SimpleTask;
import org.whispersystems.libsignal.util.guava.Optional;

import java.util.ArrayList;
import java.util.List;

public class ConversationListActivity extends PassphraseRequiredActionBarActivity
        implements ConversationListFragment.ConversationSelectedListener {
    @SuppressWarnings("unused")
    private static final String TAG = ConversationListActivity.class.getSimpleName();

    private final DynamicTheme dynamicTheme = new DynamicNoActionBarTheme();
    private final DynamicLanguage dynamicLanguage = new DynamicLanguage();

    private ConversationListFragment conversationListFragment;
    private SearchFragment searchFragment;
    private SearchToolbar searchToolbar;
    private ViewGroup fragmentContainer;
    private DrawerLayout mDrawerLayout;
    private NavigationView navigationView;
    private View searchAction;


    @Override
    protected void onPreCreate() {
        dynamicTheme.onCreate(this);
        dynamicLanguage.onCreate(this);
    }

    @Override
    protected void onCreate(Bundle icicle, boolean ready) {
        setContentView(R.layout.conversation_list_activity);
        Toolbar toolbar = findViewById(R.id.toolbar);
        Log.w("ADS", "INIT");

        setSupportActionBar(toolbar);
        ActionBar actionbar = getSupportActionBar();
        actionbar.setDisplayHomeAsUpEnabled(true);
        Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_menu);
        drawable.setColorFilter(UiUtils.themeAttributeToColor(R.attr.toolbar_icon_color, this, R.color.white), PorterDuff.Mode.SRC_ATOP);
        actionbar.setHomeAsUpIndicator(drawable);

        setTitle(R.string.app_name);

        searchToolbar = findViewById(R.id.search_toolbar);
        fragmentContainer = findViewById(R.id.fragment_container);
        conversationListFragment = initFragment(R.id.fragment_container, new ConversationListFragment(), dynamicLanguage.getCurrentLocale());

        initializeSearchListener();

        RatingManager.showRatingDialogIfNecessary(this);
        RegistrationLockDialog.showReminderIfNecessary(this);

        mDrawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(
                menuItem -> {
                    mDrawerLayout.closeDrawers();
                    switch (menuItem.getItemId()) {
                        case R.id.menu_new_group:
                            createGroup();
                            return true;
                        case R.id.menu_settings:
                            handleDisplaySettings();
                            return true;
                        case R.id.menu_clear_passphrase:
                            handleClearPassphrase();
                            return true;
                        case R.id.menu_mark_all_read:
                            handleMarkAllRead();
                            return true;
                        case R.id.menu_invite:
                            handleInvite();
                            return true;
                        case R.id.menu_help:
                            handleHelp();
                            return true;
                        case R.id.menu_contacts:
                            startActivity(new Intent(this, NewConversationActivity.class));
                            return true;
                        case R.id.saved_messages:
                            onContactSelected(TextSecurePreferences.getLocalNumber(this));
                            return true;
                        case R.id.menu_about:
                            AppsgeyserSDK.showAboutDialog(this);
                            return true;
                    }
                    return true;
                });

        AppsgeyserSDK.isAboutDialogEnabled(this, new AppsgeyserSDK.OnAboutDialogEnableListener() {
            @Override
            public void onDialogEnableReceived(boolean enabled) {
                navigationView.getMenu().findItem(R.id.menu_about).setVisible(enabled);
            }
        });
    }

    @Override
    public void initializeAppsgeyser() {
        if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("takeoff", true)){
            Log.d("appsgeyser", "takeoff conversation list");

            AppsgeyserSDK.takeOff(this,
                    getString(R.string.widgetID),
                    getString(R.string.app_metrica_on_start_event),
                    getString(R.string.template_version));
            PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("takeoff", false).commit();
            showFullscreen(Config.INSTANCE.getADS_PLACEMENT_TAG_FS_MAIN());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        dynamicTheme.onResume(this);
        dynamicLanguage.onResume(this);
        AppsgeyserSDK.getFastTrackAdsController().setBannerViewContainer((ViewGroup) findViewById(R.id.adView), Config.INSTANCE.getADS_PLACEMENT_TAG_SB_CONVERSATION_LIST());

        SimpleTask.run(getLifecycle(), () -> {
            return Recipient.from(this, Address.fromSerialized(TextSecurePreferences.getLocalNumber(this)), false);
        }, this::initializeProfileIcon);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuInflater inflater = this.getMenuInflater();
        menu.clear();

        inflater.inflate(R.menu.text_secure_normal, menu);

        menu.findItem(R.id.menu_clear_passphrase).setVisible(!TextSecurePreferences.isPasswordDisabled(this));

        super.onPrepareOptionsMenu(menu);

        Toolbar toolbar = findViewById(R.id.toolbar);

        searchAction = toolbar.getChildAt(2);
        return true;
    }

    private void initializeSearchListener() {

        searchToolbar.setListener(new SearchToolbar.SearchListener() {
            @Override
            public void onSearchTextChange(String text) {
                String trimmed = text.trim();

                if (trimmed.length() > 0) {
                    if (searchFragment == null) {
                        searchFragment = SearchFragment.newInstance(dynamicLanguage.getCurrentLocale());
                        getSupportFragmentManager().beginTransaction()
                                .add(R.id.fragment_container, searchFragment, null)
                                .commit();
                    }
                    searchFragment.updateSearchQuery(trimmed);
                } else if (searchFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .remove(searchFragment)
                            .commit();
                    searchFragment = null;
                }
            }

            @Override
            public void onSearchClosed() {
                if (searchFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .remove(searchFragment)
                            .commit();
                    searchFragment = null;
                }
            }
        });
    }

    private void initializeProfileIcon(@NonNull Recipient recipient) {
        ImageView icon = navigationView.getHeaderView(0).findViewById(R.id.toolbar_icon);
        String name = Optional.fromNullable(recipient.getName()).or(Optional.fromNullable(TextSecurePreferences.getProfileName(this))).or("");
        MaterialColor fallbackColor = recipient.getColor();

        if (fallbackColor == ContactColors.UNKNOWN_COLOR && !TextUtils.isEmpty(name)) {
            fallbackColor = ContactColors.generateFor(name);
        }

        Drawable fallback = new GeneratedContactPhoto(name, R.drawable.outline_account_circle_24).asDrawable(this, fallbackColor.toAvatarColor(this));

        GlideApp.with(this)
                .load(new ProfileContactPhoto(recipient.getAddress(), String.valueOf(TextSecurePreferences.getProfileAvatarId(this))))
                .error(fallback)
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(icon);

        icon.setOnClickListener(v -> handleDisplaySettings());

        TextView userName = navigationView.getHeaderView(0).findViewById(R.id.name);
        userName.setText(name);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        super.onOptionsItemSelected(item);

        switch (item.getItemId()) {
            case R.id.menu_new_group:
                createGroup();
                return true;
            case R.id.menu_settings:
                handleDisplaySettings();
                return true;
            case R.id.menu_clear_passphrase:
                handleClearPassphrase();
                return true;
            case R.id.menu_mark_all_read:
                handleMarkAllRead();
                return true;
            case R.id.menu_invite:
                handleInvite();
                return true;
            case R.id.menu_help:
                handleHelp();
                return true;
            case android.R.id.home:
                mDrawerLayout.openDrawer(GravityCompat.START);
                return true;
            case R.id.search_action:
                Permissions.with(this)
                        .request(Manifest.permission.READ_CONTACTS, Manifest.permission.WRITE_CONTACTS)
                        .ifNecessary()
                        .onAllGranted(() -> searchToolbar.display(searchAction.getX() + (searchAction.getWidth() / 2),
                                searchAction.getY() + (searchAction.getHeight() / 2)))
                        .withPermanentDenialDialog(getString(R.string.ConversationListActivity_signal_needs_contacts_permission_in_order_to_search_your_contacts_but_it_has_been_permanently_denied))
                        .execute();
                return true;
        }

        return false;
    }

    @Override
    public void onCreateConversation(long threadId, Recipient recipient, int distributionType, long lastSeen) {
        openConversation(threadId, recipient, distributionType, lastSeen, -1);
    }

    public void openConversation(long threadId, Recipient recipient, int distributionType, long lastSeen, int startingPosition) {
        searchToolbar.clearFocus();

        Intent intent = new Intent(this, ConversationActivity.class);
        intent.putExtra(ConversationActivity.ADDRESS_EXTRA, recipient.getAddress());
        intent.putExtra(ConversationActivity.THREAD_ID_EXTRA, threadId);
        intent.putExtra(ConversationActivity.DISTRIBUTION_TYPE_EXTRA, distributionType);
        intent.putExtra(ConversationActivity.TIMING_EXTRA, System.currentTimeMillis());
        intent.putExtra(ConversationActivity.LAST_SEEN_EXTRA, lastSeen);
        intent.putExtra(ConversationActivity.STARTING_POSITION_EXTRA, startingPosition);

        startActivity(intent);
        overridePendingTransition(R.anim.slide_from_right, R.anim.fade_scale_out);
    }

    public void onContactSelected(String number) {
        Recipient recipient = Recipient.from(this, Address.fromExternal(this, number), true);

        Intent intent = new Intent(this, ConversationActivity.class);
        intent.putExtra(ConversationActivity.ADDRESS_EXTRA, recipient.getAddress());
        intent.putExtra(ConversationActivity.TEXT_EXTRA, getIntent().getStringExtra(ConversationActivity.TEXT_EXTRA));
        intent.setDataAndType(getIntent().getData(), getIntent().getType());

        long existingThread = DatabaseFactory.getThreadDatabase(this).getThreadIdIfExistsFor(recipient);

        intent.putExtra(ConversationActivity.THREAD_ID_EXTRA, existingThread);
        intent.putExtra(ConversationActivity.DISTRIBUTION_TYPE_EXTRA, ThreadDatabase.DistributionTypes.DEFAULT);
        startActivity(intent);
    }

    @Override
    public void onSwitchToArchive() {
        Intent intent = new Intent(this, ConversationListArchiveActivity.class);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        if (searchToolbar.isVisible()) searchToolbar.collapse();
        else super.onBackPressed();
    }

    private void createGroup() {
        Intent intent = new Intent(this, GroupCreateActivity.class);
        startActivity(intent);
    }

    private void handleDisplaySettings() {
        Intent preferencesIntent = new Intent(this, ApplicationPreferencesActivity.class);
        startActivity(preferencesIntent);
    }

    private void handleClearPassphrase() {
        Intent intent = new Intent(this, KeyCachingService.class);
        intent.setAction(KeyCachingService.CLEAR_KEY_ACTION);
        startService(intent);
    }

    @SuppressLint("StaticFieldLeak")
    private void handleMarkAllRead() {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Context context = ConversationListActivity.this;
                List<MarkedMessageInfo> messageIds = DatabaseFactory.getThreadDatabase(context).setAllThreadsRead();

                MessageNotifier.updateNotification(context);
                MarkReadReceiver.process(context, messageIds);

                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    private void handleInvite() {
        startActivity(new Intent(this, InviteActivity.class));
    }

    private void handleHelp() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://support.whispersystems.org")));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.ConversationListActivity_there_is_no_browser_installed_on_your_device, Toast.LENGTH_LONG).show();
        }
    }
}
