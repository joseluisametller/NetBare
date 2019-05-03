/*  NetBare - An android network capture and injection library.
 *  Copyright (C) 2018-2019 Megatron King
 *  Copyright (C) 2018-2019 GuoShi
 *
 *  NetBare is free software: you can redistribute it and/or modify it under the terms
 *  of the GNU General Public License as published by the Free Software Found-
 *  ation, either version 3 of the License, or (at your option) any later version.
 *
 *  NetBare is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 *  PURPOSE. See the GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with NetBare.
 *  If not, see <http://www.gnu.org/licenses/>.
 */
package com.github.megatronking.netbare;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.VpnService;
import android.os.Binder;
import android.os.IBinder;
import android.os.Parcel;
import android.support.annotation.NonNull;

import com.github.megatronking.netbare.ssl.SSLEngineFactory;

/**
 * Base class for NetBare services.
 * <p>
 * NetBare service is an implement of {@link VpnService}, it establishes a vpn connection to
 * route incoming and outgoing net packets. The NetBare service are forced to display a notification
 * due to intercepting packets raises huge security concerns.
 * </p>
 * <P>
 * The NetBare service is managed by {@link NetBare}, and you can use {@link NetBareListener} to
 * observe the state.
 * </P>
 *
 * @author Megatron King
 * @since 2018-10-08 21:09
 */
public /*abstract*/ class NetBareService extends VpnService {

    /**
     * Start capturing target app's net packets.
     */
    public static final String ACTION_START =
            "com.github.megatronking.netbare.action.Start";

    /**
     * Stop capturing target app's net packets.
     */
    public static final String ACTION_STOP =
            "com.github.megatronking.netbare.action.Stop";

    /**
     * The identifier for this notification as per
     * {@link NotificationManager#notify(int, Notification)}; must not be 0.
     *
     * @return The identifier
     */
//    protected abstract int notificationId();

    /**
     * A {@link Notification} object describing what to show the user. Must not be null.
     *
     * @return The Notification to be displayed.
     */
//    @NonNull
//    protected abstract Notification createNotification();

    private NetBareThread mNetBareThread;
    private final IBinder binder = new LocalBinder2();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder2 extends Binder {
        public NetBareService getService() {
            // Return this instance of LocalService so clients can call public methods
            return NetBareService.this;
        }

        @Override
        protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
            if (code == IBinder.LAST_CALL_TRANSACTION) {
                onRevoke();
                return true;
            }
            return false;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            return START_NOT_STICKY;
        }
        String action = intent.getAction();
        if (ACTION_START.equals(action)) {
            startNetBare();
//            startForeground(notificationId(), createNotification());
        } else if (ACTION_STOP.equals(action)) {
            stopNetBare();
//            stopForeground(true);
            stopSelf();
        } else {
            stopSelf();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopNetBare();
//        stopForeground(true);
    }

    public void startNetBare() {
        // Terminate previous service.
        stopNetBare();

        NetBareConfig config = NetBare.get().getConfig();
        if (config == null) {
            throw new IllegalArgumentException("Must start NetBareService with a " +
                    "NetBareConfig");
        }

        NetBareLog.i("Start NetBare service!");
        SSLEngineFactory.updateProviders(config.keyManagerProvider, config.trustManagerProvider);
        mNetBareThread = new NetBareThread(this, config);
        mNetBareThread.start();
    }

    public void stopNetBare() {
        if (mNetBareThread == null) {
            return;
        }
        NetBareLog.i("Stop NetBare service!");
        mNetBareThread.interrupt();
        mNetBareThread = null;
    }

    public boolean isNetBareActive() {
        boolean isActive = false;

        if (mNetBareThread != null) {
            isActive = mNetBareThread.isRunning();
        }

        NetBareLog.i("MITM isNetBareActive: " + isActive);
        return isActive;
    }

}
