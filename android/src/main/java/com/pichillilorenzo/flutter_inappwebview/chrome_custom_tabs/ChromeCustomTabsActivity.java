package com.pichillilorenzo.flutter_inappwebview.chrome_custom_tabs;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsService;
import androidx.browser.customtabs.CustomTabsSession;

import com.pichillilorenzo.flutter_inappwebview.R;
import com.pichillilorenzo.flutter_inappwebview.headless_in_app_webview.HeadlessInAppWebViewManager;
import com.pichillilorenzo.flutter_inappwebview.types.CustomTabsActionButton;
import com.pichillilorenzo.flutter_inappwebview.types.CustomTabsMenuItem;
import com.pichillilorenzo.flutter_inappwebview.types.Disposable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.flutter.plugin.common.MethodChannel;

public class ChromeCustomTabsActivity extends Activity implements Disposable {
  protected static final String LOG_TAG = "CustomTabsActivity";
  public static final String METHOD_CHANNEL_NAME_PREFIX = "com.pichillilorenzo/flutter_chromesafaribrowser_";
  
  public String id;
  @Nullable
  public CustomTabsIntent.Builder builder;
  public ChromeCustomTabsSettings customSettings = new ChromeCustomTabsSettings();
  public CustomTabActivityHelper customTabActivityHelper = new CustomTabActivityHelper();
  @Nullable
  public CustomTabsSession customTabsSession;
  protected final int CHROME_CUSTOM_TAB_REQUEST_CODE = 100;
  protected boolean onChromeSafariBrowserOpened = false;
  protected boolean onChromeSafariBrowserCompletedInitialLoad = false;
  @Nullable
  public ChromeSafariBrowserManager manager;
  public String initialUrl;
  public List<CustomTabsMenuItem> menuItems = new ArrayList<>();
  @Nullable
  public CustomTabsActionButton actionButton;
  @Nullable
  public ChromeCustomTabsChannelDelegate channelDelegate;

  @CallSuper
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.chrome_custom_tabs_layout);

    Bundle b = getIntent().getExtras();
    if (b == null) return;
    
    id = b.getString("id");

    String managerId = b.getString("managerId");
    manager = ChromeSafariBrowserManager.shared.get(managerId);
    if (manager == null || manager.plugin == null|| manager.plugin.messenger == null) return;

    ChromeSafariBrowserManager.browsers.put(id, this);

    MethodChannel channel = new MethodChannel(manager.plugin.messenger, METHOD_CHANNEL_NAME_PREFIX + id);
    channelDelegate = new ChromeCustomTabsChannelDelegate(this, channel);

    initialUrl = b.getString("url");

    customSettings = new ChromeCustomTabsSettings();
    customSettings.parse((HashMap<String, Object>) b.getSerializable("settings"));
    actionButton = CustomTabsActionButton.fromMap((Map<String, Object>) b.getSerializable("actionButton"));
    List<Map<String, Object>> menuItemList = (List<Map<String, Object>>) b.getSerializable("menuItemList");
    for (Map<String, Object> menuItem : menuItemList) {
      menuItems.add(CustomTabsMenuItem.fromMap(menuItem));
    }

    final ChromeCustomTabsActivity chromeCustomTabsActivity = this;

    customTabActivityHelper.setConnectionCallback(new CustomTabActivityHelper.ConnectionCallback() {
      @Override
      public void onCustomTabsConnected() {
        customTabsConnected();
      }

      @Override
      public void onCustomTabsDisconnected() {
        chromeCustomTabsActivity.close();
        dispose();
      }
    });

    customTabActivityHelper.setCustomTabsCallback(new CustomTabsCallback() {
      @Override
      public void onNavigationEvent(int navigationEvent, Bundle extras) {
        if (navigationEvent == TAB_SHOWN && !onChromeSafariBrowserOpened) {
          onChromeSafariBrowserOpened = true;
          if (channelDelegate != null) {
            channelDelegate.onChromeSafariBrowserOpened();
          }
        }

        if (navigationEvent == NAVIGATION_FINISHED && !onChromeSafariBrowserCompletedInitialLoad) {
          onChromeSafariBrowserCompletedInitialLoad = true;
          if (channelDelegate != null) {
            channelDelegate.onChromeSafariBrowserCompletedInitialLoad();
          }
        }
      }

      @Override
      public void extraCallback(String callbackName, Bundle args) {

      }

      @Override
      public void onMessageChannelReady(Bundle extras) {

      }

      @Override
      public void onPostMessage(String message, Bundle extras) {

      }

      @Override
      public void onRelationshipValidationResult(@CustomTabsService.Relation int relation, Uri requestedOrigin,
                                                 boolean result, Bundle extras) {

      }
    });
  }

  public void customTabsConnected() {
    customTabsSession = customTabActivityHelper.getSession();
    Uri uri = Uri.parse(initialUrl);
    customTabActivityHelper.mayLaunchUrl(uri, null, null);

    builder = new CustomTabsIntent.Builder(customTabsSession);
    prepareCustomTabs();

    CustomTabsIntent customTabsIntent = builder.build();
    prepareCustomTabsIntent(customTabsIntent);

    CustomTabActivityHelper.openCustomTab(this, customTabsIntent, uri, CHROME_CUSTOM_TAB_REQUEST_CODE);
  }

  private void prepareCustomTabs() {
    if (customSettings.addDefaultShareMenuItem != null) {
      builder.setShareState(customSettings.addDefaultShareMenuItem ?
              CustomTabsIntent.SHARE_STATE_ON : CustomTabsIntent.SHARE_STATE_OFF);
    } else {
      builder.setShareState(customSettings.shareState);
    }

    if (customSettings.toolbarBackgroundColor != null && !customSettings.toolbarBackgroundColor.isEmpty()) {
      CustomTabColorSchemeParams.Builder defaultColorSchemeBuilder = new CustomTabColorSchemeParams.Builder();
      builder.setDefaultColorSchemeParams(defaultColorSchemeBuilder
              .setToolbarColor(Color.parseColor(customSettings.toolbarBackgroundColor))
              .build());
    }

    builder.setShowTitle(customSettings.showTitle);
    builder.setUrlBarHidingEnabled(customSettings.enableUrlBarHiding);
    builder.setInstantAppsEnabled(customSettings.instantAppsEnabled);

    for (CustomTabsMenuItem menuItem : menuItems) {
      builder.addMenuItem(menuItem.getLabel(), 
              createPendingIntent(menuItem.getId()));
    }

    if (actionButton != null) {
      byte[] data = actionButton.getIcon();
      BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
      bitmapOptions.inMutable = true;
      Bitmap bmp = BitmapFactory.decodeByteArray(
              data, 0, data.length, bitmapOptions
      );
      builder.setActionButton(bmp, actionButton.getDescription(),
              createPendingIntent(actionButton.getId()),
              actionButton.isShouldTint());
    }
  }

  private void prepareCustomTabsIntent(CustomTabsIntent customTabsIntent) {
    if (customSettings.packageName != null)
      customTabsIntent.intent.setPackage(customSettings.packageName);
    else
      customTabsIntent.intent.setPackage(CustomTabsHelper.getPackageNameToUse(this));

    if (customSettings.keepAliveEnabled)
      CustomTabsHelper.addKeepAliveExtra(this, customTabsIntent.intent);
  }

  @Override
  protected void onStart() {
    super.onStart();
    customTabActivityHelper.bindCustomTabsService(this);
  }

  @Override
  protected void onStop() {
    super.onStop();
    customTabActivityHelper.unbindCustomTabsService(this);
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == CHROME_CUSTOM_TAB_REQUEST_CODE) {
      close();
      dispose();
    }
  }

  private PendingIntent createPendingIntent(int actionSourceId) {
    Intent actionIntent = new Intent(this, ActionBroadcastReceiver.class);

    Bundle extras = new Bundle();
    extras.putInt(ActionBroadcastReceiver.KEY_ACTION_ID, actionSourceId);
    extras.putString(ActionBroadcastReceiver.KEY_ACTION_VIEW_ID, id);
    actionIntent.putExtras(extras);

    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      return PendingIntent.getBroadcast(
              this, actionSourceId, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
    } else {
      return PendingIntent.getBroadcast(
              this, actionSourceId, actionIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }
  }

  @Override
  public void dispose() {
    onStop();
    onDestroy();
    if (channelDelegate != null) {
      channelDelegate.dispose();
      channelDelegate = null;
    }
    if (ChromeSafariBrowserManager.browsers.containsKey(id)) {
      ChromeSafariBrowserManager.browsers.put(id, null);
    }
    manager = null;
  }

  public void close() {
    onStop();
    onDestroy();
    customTabsSession = null;
    finish();
    if (channelDelegate != null) {
      channelDelegate.onChromeSafariBrowserClosed();
    }
  }
}
