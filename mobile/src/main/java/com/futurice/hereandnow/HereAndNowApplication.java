package com.futurice.hereandnow;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.futurice.cascade.Async;
import com.futurice.cascade.i.nonnull;
import com.futurice.cascade.i.nullable;
import com.futurice.hereandnow.activity.HereAndNowActivity;
import com.futurice.hereandnow.singleton.ModelSingleton;
import com.futurice.hereandnow.singleton.ServiceSingleton;
import com.spacetimenetworks.scampiandroidlib.ScampiService;
import com.spacetimenetworks.scampiandroidlib.ServiceConfig;
import com.squareup.leakcanary.LeakCanary;

import java.util.concurrent.TimeUnit;

/**
 * Main application
 */
public class HereAndNowApplication extends Application implements ScampiService.StateChangeCallback {

    public static String TAG = HereAndNowApplication.class.getCanonicalName();
    private static Context context;
    /**
     * Reference to the service.
     */
    @Nullable
    @nullable
    ScampiService service;

    public static Context getStaticContext() {
        return HereAndNowApplication.context;
    }

    public static void stopServiceDelayed(@NonNull @nonnull final Context context) {
        final HereAndNowApplication app = ((HereAndNowApplication) (context.getApplicationContext()));
        if (app.service != null) {
            app.service.stop(Constants.DELAY_IN_MINUTES, TimeUnit.MINUTES);
        }
    }

    public static void stopServiceNow(@NonNull @nonnull final Context context) {
        final HereAndNowApplication app = ((HereAndNowApplication) (context.getApplicationContext()));
        if (app.service != null) {
            app.service.stop();
            Async.exitWithErrorCode(TAG, "User shutdown", null);
            new Thread(() -> {
                try {
                    Thread.sleep(300);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Interrupted app exit sleep: ", e);
                }
                Async.exitWithErrorCode(TAG, "User shutdown", null);
            });
        }
    }

    public static void startServiceIf(@NonNull @nonnull final Context context) {
        final HereAndNowApplication app = ((HereAndNowApplication) (context.getApplicationContext()));

        if (app.service != null) {
            app.service.start();
        } else {
            app.initConnection();
        }
    }

    public static String getApplicationName() {
        return context.getString(context.getApplicationInfo().labelRes);
    }

    @Override
    public final void onCreate() {
        super.onCreate();
        LeakCanary.install(this);

        ServiceSingleton.create(getApplicationContext());
        ModelSingleton.create(getApplicationContext());
        HereAndNowApplication.context = getApplicationContext();
    }

    @Override
    public void stateChanged(@NonNull @nonnull final ScampiService.RouterState routerState) {
        Log.d(TAG, "stateChanged");
    }

    private void initConnection() {
        // Setup the service binding
        super.startService(new Intent(this, ScampiService.class));
        // Connection used to get a callback once the service is connected.
        this.bindService(new Intent(this, ScampiService.class),
                this.getServiceConnection(), Context.BIND_AUTO_CREATE);
    }

    private void startRouter() {
        // Build a configuration for the Service. The configuration
        // defines all user visible things such as notification tray icon
        // and texts, as well as things like the home directory to store the
        // router files and the configuration to use for the router. There
        // are defaults for all the settings.
        final ServiceConfig scampiServiceConfig =
                ServiceConfig.builder(HereAndNowApplication.this)
                        .logToStdout()    // Causes the router to log to stdout
                        .debugLogLevel()  // Sets log level to debug for the router
//Use only if works only with a local server        .configFileAsset("star.conf")
                        .notifyIntent(new Intent(this, HereAndNowActivity.class))
                        .notifyContentText(HereAndNowApplication.getApplicationName())
                        .notifyIcon(R.drawable.ic_launcher_transparent)
                        .build();

        // startScan() either starts up the router if it's not running, or does
        // nothing otherwise.
        if (service != null) {
            this.service.start(scampiServiceConfig);
        } else {
            Log.i(TAG, "Can not startScan SCAMPI service- not connected");
        }
    }

    @NonNull
    private ServiceConnection getServiceConnection() {
        return new ServiceConnection() {

            @Override
            public void onServiceConnected(@NonNull @nonnull final ComponentName className, @NonNull @nonnull final IBinder service) {
                Log.d(TAG, "onServiceConnected");

                // Get reference to the IScampiService
                final ScampiService scampiService = ((ScampiService.ScampiBinder) service).getService();
                HereAndNowApplication.this.service = scampiService;

                // Add callbacks to the service
                scampiService.addStateChangeCallback(HereAndNowApplication.this);
                startRouter();

            }

            @Override
            public void onServiceDisconnected(@NonNull @nonnull final ComponentName arg0) {
                Log.d(TAG, "onServiceDisconnected");
                removeCallback();
            }
        };
    }

    public void removeCallback() {
        if (HereAndNowApplication.this.service != null) {
            HereAndNowApplication.this.service.removeStateChangeCallback(HereAndNowApplication.this);
            HereAndNowApplication.this.service = null;
        }
    }
}
