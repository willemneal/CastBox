/*
 * Copyright (C) 2013 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.sample.cast.refplayer.utils;

import com.google.android.gms.cast.MediaInfo;
import com.google.android.gms.cast.MediaQueueItem;
import com.google.android.libraries.cast.companionlibrary.cast.VideoCastManager;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.CastException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.NoConnectionException;
import com.google.android.libraries.cast.companionlibrary.cast.exceptions.TransientNetworkDisconnectionException;
import com.google.sample.cast.refplayer.CastApplication;
import com.google.sample.cast.refplayer.R;
import com.google.sample.cast.refplayer.queue.QueueDataProvider;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.IOException;

/**
 * A collection of utility methods, all static.
 */
public class Utils {

    private static final String TAG = "Utils";

    /*
     * Making sure public utility methods remain static
     */
    private Utils() {
    }

    @SuppressWarnings("deprecation")
    /**
     * Returns the screen/display size
     *
     */
    public static Point getDisplaySize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();
        return new Point(width, height);
    }

    /**
     * Returns {@code true} if and only if the screen orientation is portrait.
     */
    public static boolean isOrientationPortrait(Context context) {
        return context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_PORTRAIT;
    }

    /**
     * Shows an error dialog with a given text message.
     */
    public static void showErrorDialog(Context context, String errorString) {
        new AlertDialog.Builder(context).setTitle(R.string.error)
                .setMessage(errorString)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .create()
                .show();
    }

    /**
     * Shows an "Oops" error dialog with a text provided by a resource ID
     */
    public static void showOopsDialog(Context context, int resourceId) {
        new AlertDialog.Builder(context).setTitle(R.string.oops)
                .setMessage(context.getString(resourceId))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                })
                .setIcon(R.drawable.ic_action_alerts_and_states_warning)
                .create()
                .show();
    }

    /**
     * A utility method to handle a few types of exceptions that are commonly thrown by the cast
     * APIs in this library. It has special treatments for
     * {@link TransientNetworkDisconnectionException}, {@link NoConnectionException} and shows an
     * "Oops" dialog conveying certain messages to the user. The following resource IDs can be used
     * to control the messages that are shown:
     * <p>
     * <ul>
     * <li><code>R.string.connection_lost_retry</code></li>
     * <li><code>R.string.connection_lost</code></li>
     * <li><code>R.string.failed_to_perform_action</code></li>
     * </ul>
     */
    public static void handleException(Context context, Exception e) {
        int resourceId;
        if (e instanceof TransientNetworkDisconnectionException) {
            // temporary loss of connectivity
            resourceId = R.string.connection_lost_retry;

        } else if (e instanceof NoConnectionException) {
            // connection gone
            resourceId = R.string.connection_lost;
        } else if (e instanceof RuntimeException ||
                e instanceof IOException ||
                e instanceof CastException) {
            // something more serious happened
            resourceId = R.string.failed_to_perform_action;
        } else {
            // well, who knows!
            resourceId = R.string.failed_to_perform_action;
        }
        com.google.sample.cast.refplayer.utils.Utils.showOopsDialog(context, resourceId);
    }

    /**
     * Gets the version of app.
     */
    public static String getAppVersionName(Context context) {
        String versionString = null;
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(context.getPackageName(),
                    0 /* basic info */);
            versionString = info.versionName;
        } catch (Exception e) {
            // do nothing
        }
        return versionString;
    }

    /**
     * Shows a (long) toast.
     */
    public static void showToast(Context context, int resourceId) {
        Toast.makeText(context, context.getString(resourceId), Toast.LENGTH_LONG).show();
    }

    public static boolean hitTest(View v, int x, int y) {
        final int tx = (int) (ViewCompat.getTranslationX(v) + 0.5f);
        final int ty = (int) (ViewCompat.getTranslationY(v) + 0.5f);
        final int left = v.getLeft() + tx;
        final int right = v.getRight() + tx;
        final int top = v.getTop() + ty;
        final int bottom = v.getBottom() + ty;

        return (x >= left) && (x <= right) && (y >= top) && (y <= bottom);
    }

    /**
     * Show a popup to select whether the selected item should play immediately, be added to the
     * end of queue or be added to the queue right after the current item.
     */
    public static void showQueuePopup(final Context context, View view, final MediaInfo mediaInfo) {
        final VideoCastManager castManager = VideoCastManager.getInstance();
        final QueueDataProvider provider = QueueDataProvider.getInstance();
        if (!castManager.isConnected()) {
            Log.w(TAG, "showQueuePopup(): not connected to a cast device");
            return;
        }
        PopupMenu popup = new PopupMenu(context, view);
        popup.getMenuInflater().inflate(
                provider.isQueueDetached() || provider.getCount() == 0
                        ? R.menu.detached_popup_add_to_queue
                        : R.menu.popup_add_to_queue, popup.getMenu());
        PopupMenu.OnMenuItemClickListener clickListener = new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                QueueDataProvider provider = QueueDataProvider.getInstance();
                MediaQueueItem queueItem = new MediaQueueItem.Builder(mediaInfo).setAutoplay(
                        true).setPreloadTime(CastApplication.PRELOAD_TIME_S).build();
                MediaQueueItem[] newItemArray = new MediaQueueItem[]{queueItem};
                String toastMessage = null;
                try {
                    if (provider.isQueueDetached() && provider.getCount() > 0) {
                        switch (menuItem.getItemId()) {
                            case R.id.action_play_now:
                            case R.id.action_add_to_queue:
                                MediaQueueItem[] items = com.google.android.libraries.cast
                                        .companionlibrary.utils.Utils
                                        .rebuildQueueAndAppend(provider.getItems(), queueItem);
                                // temporary castManager.queueLoad(items, provider.getCount(),
                                // temporary        MediaStatus.REPEAT_MODE_REPEAT_OFF, null);
                                ((CastApplication) context.getApplicationContext())
                                        .loadQueue(items, provider.getCount());
                                break;
                            default:
                                return false;
                        }
                    } else {
                        if (provider.getCount() == 0) {
                            // temporary castManager.queueLoad(newItemArray, 0,
                            // temporary        MediaStatus.REPEAT_MODE_REPEAT_OFF, null);
                            ((CastApplication) context.getApplicationContext())
                                    .loadQueue(newItemArray, 0);
                        } else {
                            int currentId = provider.getCurrentItemId();
                            switch (menuItem.getItemId()) {
                                case R.id.action_play_now:
                                    castManager.queueInsertBeforeCurrentAndPlay(queueItem,
                                            currentId, null);
                                    break;
                                case R.id.action_play_next:
                                    int currentPosition = provider.getPositionByItemId(currentId);
                                    if (currentPosition == provider.getCount() - 1) {
                                        //we are adding to the end of queue
                                        castManager.queueAppendItem(queueItem, null);
                                    } else {
                                        int nextItemId = provider.getItem(currentPosition + 1)
                                                .getItemId();
                                        castManager.queueInsertItems(newItemArray, nextItemId,
                                                null);
                                    }
                                    toastMessage = context.getString(
                                            R.string.queue_item_added_to_play_next);
                                    break;
                                case R.id.action_add_to_queue:
                                    castManager.queueAppendItem(queueItem, null);
                                    toastMessage = context.getString(
                                            R.string.queue_item_added_to_queue);
                                    break;
                                default:
                                    return false;
                            }
                        }
                    }
                } catch (NoConnectionException |
                        TransientNetworkDisconnectionException e) {
                    Log.e(TAG, "Failed to add item to queue or play remotely", e);
                }
                if (toastMessage != null) {
                    Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show();
                }
                return true;
            }
        };
        popup.setOnMenuItemClickListener(clickListener);
        popup.show();
    }

}
