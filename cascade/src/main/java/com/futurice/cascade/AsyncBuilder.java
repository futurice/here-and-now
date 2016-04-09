/*
This file is part of Reactive Cascade which is released under The MIT License.
See license.txt or http://reactivecascade.com for details.
This is open source for the common good. Please contribute improvements by pull request or contact paul.houghton@futurice.com
*/
package com.futurice.cascade;

import android.content.Context;
import android.os.Handler;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import com.futurice.cascade.active.ImmutableValue;
import com.futurice.cascade.i.CallOrigin;
import com.futurice.cascade.i.IThreadType;
import com.futurice.cascade.i.NotCallOrigin;
import com.futurice.cascade.i.nonnull;
import com.futurice.cascade.util.DefaultThreadType;
import com.futurice.cascade.util.DoubleQueue;
import com.futurice.cascade.util.TypedThread;
import com.futurice.cascade.util.UIExecutorService;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <code><pre>
 *     public static IThreadType mThreadType;
 * .. onCreate ..
 * if (mThreadType == null) {
 *    mThreadType = new ThreadTypeBuilder(this.getApplicationContext()).build();
 * } *
 * </pre></code>
 * <p>
 * The singled threaded class loader ensures all ALog variables are set only once,
 * but injectable for configuration split testability from values previously set here for best performance
 * <p>
 * The first ALog created because the default ALog split is accessible from that point forward by references
 * to {@link com.futurice.cascade.Async#WORKER}
 */
@CallOrigin
public class AsyncBuilder {
    public static final int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();
    public static final int NUMBER_OF_CONCURRENT_NET_READS = 4;
    static final String NOT_INITIALIZED = "Please init with new AsyncBuilder(this).build() in for example Activity.onCreate() _before_ the classloader touches Async.class";
    private final static AtomicInteger sThreadNumber = new AtomicInteger();
    public static volatile AsyncBuilder sAsyncBuilder = null;
    static Thread sSerialWorkerThread;
    public final Context mContext;
    private final String TAG = AsyncBuilder.class.getSimpleName();
    private final AtomicBoolean mWorkerPoolIncludesSerialWorkerThread = new AtomicBoolean(false);
    public Thread mUiThread;
    public ExecutorService mUiExecutorService;
    public boolean mDebug = false; //BuildConfig.DEBUG;
    public final boolean mStrictMode = mDebug;
    public boolean mFailFast = mDebug;
    public boolean mShowErrorStackTraces = mDebug;
    private IThreadType mWorkerThreadType;
    private IThreadType mSerialWorkerThreadType;
    private IThreadType mUiThreadType;
    private IThreadType mNetReadThreadType;
    private IThreadType mNetWriteThreadType;
    private IThreadType mFileThreadType;
    private BlockingQueue<Runnable> mWorkerQueue;
    private BlockingQueue<Runnable> mSerialWorkerQueue;
    private BlockingQueue<Runnable> mFileQueue;
    private BlockingQueue<Runnable> mNetReadQueue;
    private BlockingQueue<Runnable> mNetWriteQueue;
    private ExecutorService mWorkerExecutorService;
    private ExecutorService mSerialWorkerExecutorService;
    private ExecutorService mFileReadExecutorService;
    private ExecutorService mFileWriteExecutorService;
    private ExecutorService mNetReadExecutorService;
    private ExecutorService mNetWriteExecutorService;

    /**
     * Create a new <code>AsyncBuilder</code> that will run as long as the specified
     * {@link android.content.Context} will run.
     * <p>
     * Unless you have reason to do otherwise, you probably want to pass
     * {@link android.app.Activity#getApplicationContext()} to ensure that the asynchronous
     * actions last past the end of the current {@link android.app.Activity}. If you do so,
     * you can for effeciency only create the one instance for the entire application lifecycle
     * by for example setting a static variable the first time the <code>AsyncBuilder</code>
     * is used.
     *
     * @param context
     */
    public AsyncBuilder(@NonNull final Context context) {
        Context c = context;
        try {
            c = context.getApplicationContext();
        } catch (NullPointerException e) {
            // Needed for instrumentation setup with Android test runner
        }
        this.mContext = c;
    }

    /**
     * @return
     */
    public static boolean isInitialized() {
        return sAsyncBuilder != null;
    }

    /**
     * @return
     */
    @NonNull
    @nonnull
    @NotCallOrigin
    @VisibleForTesting
    IThreadType getWorkerThreadType() {
        if (mWorkerThreadType == null) {
            ImmutableValue<IThreadType> threadTypeImmutableValue = new ImmutableValue<>();
            setWorkerThreadType(new DefaultThreadType("WorkerThreadType",
                            getWorkerExecutorService(threadTypeImmutableValue),
                            getWorkerQueue()
                    )
            );
            threadTypeImmutableValue.set(mWorkerThreadType);
        }

        return mWorkerThreadType;
    }

    /**
     * @param workerThreadType
     * @return the builder, for chaining
     */
    @NonNull
    @nonnull
    public AsyncBuilder setWorkerThreadType(@NonNull final IThreadType workerThreadType) {
        Log.v(TAG, "setWorkerThreadType(" + workerThreadType + ")");
        this.mWorkerThreadType = workerThreadType;

        return this;
    }

    /**
     * @return
     */
    @NonNull
    @nonnull
    @NotCallOrigin
    @VisibleForTesting
    IThreadType getSerialWorkerThreadType() {
        if (mSerialWorkerThreadType == null) {
            ImmutableValue<IThreadType> threadTypeImmutableValue = new ImmutableValue<>();
            setSerialWorkerThreadType(new DefaultThreadType("SerialWorkerThreadType",
                            getSerialWorkerExecutorService(threadTypeImmutableValue),
                            getSerialWorkerQueue()
                    )
            );
            threadTypeImmutableValue.set(mSerialWorkerThreadType);
        }

        return mSerialWorkerThreadType;
    }

    /**
     * @param serialWorkerThreadType
     * @return the builder, for chaining
     */
    @NonNull
    @nonnull
    public AsyncBuilder setSerialWorkerThreadType(@NonNull final IThreadType serialWorkerThreadType) {
        Log.v(TAG, "setSerialWorkerThreadType(" + serialWorkerThreadType + ")");
        this.mSerialWorkerThreadType = serialWorkerThreadType;

        return this;
    }

    /**
     * @return
     */
    @NonNull
    @nonnull
    @VisibleForTesting
    IThreadType getUiThreadType() {
        if (mUiThreadType == null) {
            setUIThreadType(new DefaultThreadType("UIThreadType", getUiExecutorService(), null));
        }

        return mUiThreadType;
    }

    /**
     * @param uiThreadType
     * @return the builder, for chaining
     */
    @NonNull
    @nonnull
    public AsyncBuilder setUIThreadType(@NonNull @nonnull final IThreadType uiThreadType) {
        Log.v(TAG, "setUIThreadType(" + uiThreadType + ")");
        this.mUiThreadType = uiThreadType;
        return this;
    }

    /**
     * @return
     */
    @NonNull
    @nonnull
    @VisibleForTesting
    IThreadType getNetReadThreadType() {
        if (mNetReadThreadType == null) {
            final ImmutableValue<IThreadType> threadTypeImmutableValue = new ImmutableValue<>();
            setNetReadThreadType(new DefaultThreadType("NetReadThreadType",
                            getNetReadExecutorService(threadTypeImmutableValue),
                            getNetReadQueue()
                    )
            );
            threadTypeImmutableValue.set(mNetReadThreadType);
        }

        return mNetReadThreadType;
    }

    /**
     * @param netReadThreadType
     * @return the builder, for chaining
     */
    @NonNull
    @nonnull
    public AsyncBuilder setNetReadThreadType(@NonNull @nonnull final IThreadType netReadThreadType) {
        Log.v(TAG, "setNetReadThreadType(" + netReadThreadType + ")");
        this.mNetReadThreadType = netReadThreadType;
        return this;
    }

    /**
     * @return
     */
    @NonNull
    @nonnull
    @VisibleForTesting
    IThreadType getNetWriteThreadType() {
        if (mNetWriteThreadType == null) {
            final ImmutableValue<IThreadType> threadTypeImmutableValue = new ImmutableValue<>();
            setNetWriteThreadType(new DefaultThreadType("NetWriteThreadType",
                            getNetWriteExecutorService(threadTypeImmutableValue),
                            getNetWriteQueue()
                    )
            );
            threadTypeImmutableValue.set(mNetWriteThreadType);
        }

        return mNetWriteThreadType;
    }

    /**
     * @param netWriteThreadType
     * @return the builder, for chaining
     */
    @NonNull
    @nonnull
    public AsyncBuilder setNetWriteThreadType(@NonNull @nonnull final IThreadType netWriteThreadType) {
        Log.v(TAG, "setNetWriteThreadType(" + netWriteThreadType + ")");
        this.mNetWriteThreadType = netWriteThreadType;
        return this;
    }

    /**
     * @return
     */
    @NonNull
    @nonnull
    @VisibleForTesting
    IThreadType getFileThreadType() {
        if (mFileThreadType == null) {
            final ImmutableValue<IThreadType> threadTypeImmutableValue = new ImmutableValue<>();
            setFileThreadType(new DefaultThreadType("FileReadThreadType",
                            getFileExecutorService(threadTypeImmutableValue),
                            getFileQueue()
                    )
            );
            threadTypeImmutableValue.set(mFileThreadType);
        }

        return mFileThreadType;
    }

    /**
     * @param fileThreadType
     * @return the builder, for chaining
     */
    @NonNull
    @nonnull
    public AsyncBuilder setFileThreadType(@NonNull @nonnull final IThreadType fileThreadType) {
        Log.v(TAG, "setFileThreadType(" + fileThreadType + ")");
        this.mFileThreadType = fileThreadType;
        return this;
    }

//    @NonNull
//    public MirrorService getFileService() {
//        if (fileService == null) {
//            Log.v(TAG, "Creating default file service");
//            setFileService(new FileMirrorService("Default FileMirrorService",
//                    "FileMirrorService",
//                    false,
//                    mContext,
//                    Context.MODE_PRIVATE,
//                    getFileThreadType()));
//        }
//
//        return fileService;
//    }

//    @NonNull
//    public AsyncBuilder setFileService(@NonNull final MirrorService fileService) {
//        Log.v(TAG, "setFileService(" + fileService + ")");
//        this.fileService = fileService;
//        return this;
//    }

    @NonNull
    private Thread getWorkerThread(
            @NonNull @nonnull final IThreadType threadType,
            @NonNull @nonnull final Runnable runnable) {
        if (NUMBER_OF_CORES == 1 || mWorkerPoolIncludesSerialWorkerThread.getAndSet(true)) {
            return new TypedThread(threadType, runnable, "WorkerThread" + sThreadNumber.getAndIncrement());
        }
        return getSerialWorkerThread(threadType, runnable);
    }

    /**
     * @param threadTypeImmutableValue
     * @return the builder, for chaining
     */
    @NonNull
    @nonnull
    @VisibleForTesting
    ExecutorService getWorkerExecutorService(@NonNull @nonnull final ImmutableValue<IThreadType> threadTypeImmutableValue) {
        if (mWorkerExecutorService == null) {
            Log.v(TAG, "Creating default worker executor service");
            final BlockingQueue<Runnable> q = getWorkerQueue();
            final int numberOfThreads = q instanceof BlockingDeque ? NUMBER_OF_CORES : 1;

            setWorkerExecutorService(new ThreadPoolExecutor(
                    numberOfThreads,
                    numberOfThreads,
                    1000,
                    TimeUnit.MILLISECONDS,
                    q,
                    runnable ->
                            getWorkerThread(threadTypeImmutableValue.get(), runnable)
            ));
        }

        return mWorkerExecutorService;
    }

    @NonNull
    private synchronized Thread getSerialWorkerThread(
            @NonNull @nonnull final IThreadType threadType,
            @NonNull @nonnull final Runnable runnable) {
        if (sSerialWorkerThread == null) {
            sSerialWorkerThread = new TypedThread(threadType, runnable, "SerialWorkerThread" + sThreadNumber.getAndIncrement());
        }

        return sSerialWorkerThread;
    }

    /**
     * @param threadTypeImmutableValue
     * @return the builder, for chaining
     */
    @NonNull
    @nonnull
    @VisibleForTesting
    ExecutorService getSerialWorkerExecutorService(@NonNull @nonnull final ImmutableValue<IThreadType> threadTypeImmutableValue) {
        if (mSerialWorkerExecutorService == null) {
            Log.v(TAG, "Creating default serial worker executor service");

            setSerialWorkerExecutorService(new ThreadPoolExecutor(
                            1,
                            1,
                            1000,
                            TimeUnit.MILLISECONDS,
                            getSerialWorkerQueue(),
                            runnable -> getSerialWorkerThread(threadTypeImmutableValue.get(), runnable))
            );
        }

        return mWorkerExecutorService;
    }

    /**
     * @return the builder, for chaining
     */
    @NonNull
    @nonnull
    @VisibleForTesting
    BlockingQueue<Runnable> getWorkerQueue() {
        if (mWorkerQueue == null) {
            Log.d(TAG, "Creating default worker mQueue");
            setWorkerQueue(new LinkedBlockingDeque<>());
        }

        return mWorkerQueue;
    }

    /**
     * @param queue
     * @return the builder, for chaining
     */
    @NonNull
    @nonnull
    public AsyncBuilder setWorkerQueue(@NonNull @nonnull final BlockingQueue<Runnable> queue) {
        Log.d(TAG, "setWorkerQueue(" + queue + ")");
        mWorkerQueue = queue;
        return this;
    }

    /**
     * Call {@link #setWorkerQueue(java.util.concurrent.BlockingQueue)} before calling this method
     * if you wish to use something other than the default.
     *
     * @return the builder, for chaining
     */
    @NonNull
    @nonnull
    @VisibleForTesting
    BlockingQueue<Runnable> getSerialWorkerQueue() {
        if (mSerialWorkerQueue == null) {
            Log.d(TAG, "Creating default in-order worker mQueue");
            setSerialWorkerQueue(new DoubleQueue<>(getWorkerQueue()));
        }

        return mSerialWorkerQueue;
    }

    /**
     * @param queue
     * @return the builder, for chaining
     */
    @NonNull
    @nonnull
    public AsyncBuilder setSerialWorkerQueue(@NonNull @nonnull final BlockingQueue<Runnable> queue) {
        Log.d(TAG, "setSerialWorkerQueue(" + queue + ")");
        mSerialWorkerQueue = queue;
        return this;
    }

    /**
     * @return the builder, for chaining
     */
    @NonNull
    @nonnull
    @VisibleForTesting
    BlockingQueue<Runnable> getFileQueue() {
        if (mFileQueue == null) {
            Log.d(TAG, "Creating default file read mQueue");
            setFileQueue(new LinkedBlockingDeque<>());
        }

        return mFileQueue;
    }

    /**
     * @param queue
     * @return the builder, for chaining
     */
    @NonNull
    @nonnull
    public AsyncBuilder setFileQueue(@NonNull @nonnull final BlockingQueue<Runnable> queue) {
        Log.d(TAG, "setFileQueue(" + queue + ")");
        this.mFileQueue = queue;
        return this;
    }

    /**
     * @return the builder, for chaining
     */
    @NonNull
    @nonnull
    @VisibleForTesting
    BlockingQueue<Runnable> getNetReadQueue() {
        if (mNetReadQueue == null) {
            Log.d(TAG, "Creating default net read mQueue");
            setNetReadQueue(new LinkedBlockingDeque<>());
        }

        return mNetReadQueue;
    }

    /**
     * @param queue
     * @return the builder, for chaining
     */
    @NonNull
    @nonnull
    public AsyncBuilder setNetReadQueue(@NonNull @nonnull final BlockingQueue<Runnable> queue) {
        Log.d(TAG, "setNetReadQueue(" + queue + ")");
        this.mNetReadQueue = queue;
        return this;
    }

    /**
     * @return the builder, for chaining
     */
    @NonNull
    @nonnull
    @VisibleForTesting
    BlockingQueue<Runnable> getNetWriteQueue() {
        if (mNetWriteQueue == null) {
            Log.d(TAG, "Creating default worker net write mQueue");
            setNetWriteQueue(new LinkedBlockingDeque<>());
        }

        return mNetWriteQueue;
    }

    /**
     * @param queue
     * @return the builder, for chaining
     */
    @NonNull
    @nonnull
    public AsyncBuilder setNetWriteQueue(@NonNull @nonnull final BlockingQueue<Runnable> queue) {
        Log.d(TAG, "setNetWriteQueue(" + queue + ")");
        this.mNetWriteQueue = queue;
        return this;
    }

    /**
     * @param threadTypeImmutableValue
     * @return the builder, for chaining
     */
    @NonNull
    @nonnull
    @VisibleForTesting
    ExecutorService getFileExecutorService(
            @NonNull final ImmutableValue<IThreadType> threadTypeImmutableValue) {
        if (mFileReadExecutorService == null) {
            Log.d(TAG, "Creating default file read executor service");
            setFileReadExecutorService(new ThreadPoolExecutor(1, 1,
                            0L, TimeUnit.MILLISECONDS,
                            getFileQueue(),
                            runnable -> new TypedThread(threadTypeImmutableValue.get(), runnable, "FileThread" + sThreadNumber.getAndIncrement()))
            );
        }
        //TODO else Warn if mThreadType does match the previously created IThreadType parameter

        return mFileReadExecutorService;
    }

    /**
     * @param threadTypeImmutableValue
     * @return the builder, for chaining
     */
    @NonNull
    @nonnull
    @VisibleForTesting
    ExecutorService getNetReadExecutorService(
            @NonNull final ImmutableValue<IThreadType> threadTypeImmutableValue) {
        if (mNetReadExecutorService == null) {
            Log.d(TAG, "Creating default net read executor service");
            setNetReadExecutorService(new ThreadPoolExecutor(1, NUMBER_OF_CONCURRENT_NET_READS,
                            1000, TimeUnit.MILLISECONDS, getNetReadQueue(),
                            runnable -> new TypedThread(threadTypeImmutableValue.get(), runnable, "NetReadThread" + sThreadNumber.getAndIncrement()))
            );
        }

        return mNetReadExecutorService;
    }

    /**
     * @param threadTypeImmutableValue
     * @return the builder, for chaining
     */
    @NonNull
    @nonnull
    @VisibleForTesting
    ExecutorService getNetWriteExecutorService(
            @NonNull final ImmutableValue<IThreadType> threadTypeImmutableValue) {
        if (mNetWriteExecutorService == null) {
            Log.d(TAG, "Creating default net write executor service");
            setNetWriteExecutorService(Executors.newSingleThreadExecutor(
                            runnable -> new TypedThread(threadTypeImmutableValue.get(), runnable, "NetWriteThread" + sThreadNumber.getAndIncrement()))
            );
        }

        return mNetWriteExecutorService;
    }

    /**
     * @return the builder, for chaining
     */
    //TODO All ExecutorService-s should be a new DelayedExecutorService which supports executeDelayed(millis) behavior
    @NonNull
    @nonnull
    @VisibleForTesting
    ExecutorService getUiExecutorService() {
        if (mContext == null) {
            Exception e = new IllegalStateException(NOT_INITIALIZED);
            Log.e(TAG, NOT_INITIALIZED, e);
            System.exit(-1);
        }
        if (mUiExecutorService == null) {
            setUiExecutorService(new UIExecutorService(new Handler(mContext.getMainLooper())));
        }

        return mUiExecutorService;
    }

    /**
     * Note that if you override this, you may also want to override the associated default
     * {@link #setUI_Thread(Thread)}
     *
     * @param uiExecutorService
     * @return the builder, for chaining
     */
    @NonNull
    @nonnull
    public AsyncBuilder setUiExecutorService(@NonNull @nonnull final ExecutorService uiExecutorService) {
        Log.d(TAG, "setUiExecutorService(" + uiExecutorService + ")");
        this.mUiExecutorService = uiExecutorService;
        return this;
    }

    /**
     * Note that if you override this, you may also want to override the associated
     * {@link #setWorkerThreadType(com.futurice.cascade.i.IThreadType)} to for example match the <code>inOrderExecution</code> parameter
     *
     * @param executorService the service to be used for most callbacks split general purpose processing. The default implementation
     *                        is a threadpool sized based to match the number of CPU cores. Most operations on this executor
     *                        do not block for IO, those are relegated to specialized executors
     * @return the builder, for chaining
     */
    @NonNull
    @nonnull
    public AsyncBuilder setWorkerExecutorService(@NonNull @nonnull final ExecutorService executorService) {
        Log.v(TAG, "setWorkerExecutorService(" + executorService + ")");
        mWorkerExecutorService = executorService;
        return this;
    }

    /**
     * @param executorService
     * @return the builder, for chaining
     */
    @NonNull
    @nonnull
    public AsyncBuilder setSerialWorkerExecutorService(@NonNull @nonnull final ExecutorService executorService) {
        Log.v(TAG, "setSerialWorkerExecutorService(" + executorService + ")");
        mSerialWorkerExecutorService = executorService;
        return this;
    }

    /**
     * Indicate that all {@link Async#WORKER} tasks are single threaded on {@link Async#SERIAL_WORKER}.
     * <p>
     * This may be useful to temporarily add to your builder for testing if you wish to test if issues
     * are facing are caused or not caused by background task concurrency.
     *
     * @return the builder, for chaining
     */
    @NonNull
    @nonnull
    public AsyncBuilder singleThreadedWorkerExecutorService() {
        Log.v(TAG, "singleThreadedWorkerExecutorService()");
        final ImmutableValue<IThreadType> threadTypeImmutableValue = new ImmutableValue<>();
        this.mWorkerExecutorService = Executors.newSingleThreadScheduledExecutor(
                runnable -> new TypedThread(threadTypeImmutableValue.get(), runnable, "SingleThreadedWorker" + sThreadNumber.getAndIncrement())
        );
        threadTypeImmutableValue.set(getWorkerThreadType());
        return this;
    }

    /**
     * @param fileReadExecutorService
     * @return the builder, for chaining
     */
    @NonNull
    @nonnull
    public AsyncBuilder setFileReadExecutorService(@NonNull @nonnull final ExecutorService fileReadExecutorService) {
        Log.v(TAG, "setFileReadExecutorService(" + fileReadExecutorService + ")");
        this.mFileReadExecutorService = fileReadExecutorService;
        return this;
    }

    /**
     * @param executorService
     * @return the builder, for chaining
     */
    @NonNull
    @nonnull
    public AsyncBuilder setFileWriteExecutorService(@NonNull @nonnull final ExecutorService executorService) {
        Log.v(TAG, "setFileWriteExecutorService(" + mFileWriteExecutorService + ")");
        mFileWriteExecutorService = executorService;
        return this;
    }

    /**
     * @param netReadExecutorService
     * @return the builder, for chaining
     */
    @NonNull
    @nonnull
    public AsyncBuilder setNetReadExecutorService(@NonNull @nonnull final ExecutorService netReadExecutorService) {
        Log.v(TAG, "setNetReadExecutorService(" + netReadExecutorService + ")");
        this.mNetReadExecutorService = netReadExecutorService;
        return this;
    }

    /**
     * Retrieve or create the thread group that will hand net writes.
     *
     * @param netWriteExecutorService
     * @return the builder, for chaining
     */
    @NonNull
    @nonnull
    public AsyncBuilder setNetWriteExecutorService(@NonNull @nonnull final ExecutorService netWriteExecutorService) {
        Log.v(TAG, "setNetWriteExecutorService(" + netWriteExecutorService + ")");
        this.mNetWriteExecutorService = netWriteExecutorService;
        return this;
    }

    /**
     * Change the thread marked as the thread from which user interface calls are made
     * <p>
     * This is just a marker useful for application logic. It does not modify the UI implementation
     * in any way.
     *
     * @param uiThread
     * @return
     */
    @NonNull
    @nonnull
    public AsyncBuilder setUI_Thread(@NonNull @nonnull final Thread uiThread) {
        Log.v(TAG, "setUI_Thread(" + uiThread + ")");
        uiThread.setName("UIThread");
        this.mUiThread = uiThread;

        return this;
    }

    /**
     * Set this to false if you prefer the app to log exceptions during debugOrigin but continue running
     * <p>
     * This setting only affects debugOrigin builds. Production builds do not close on first Exception
     * <p>
     * Defaults is true
     * <p>
     *
     * @param failFast
     * @return
     */
    @NonNull
    @nonnull
    public AsyncBuilder setFailFast(final boolean failFast) {
        Log.v(TAG, "setFailFast(" + failFast + ")");
        this.mFailFast = failFast;
        return this;
    }

    /**
     * By default, error stack traces are shown in the debug output. When running system testing code which
     * intentionally throws errors, this may be better disabled.
     *
     * @param showErrorStackTraces
     * @return
     */
    @NonNull
    @nonnull
    public AsyncBuilder setShowErrorStackTraces(final boolean showErrorStackTraces) {
        Log.v(TAG, "setShowErrorStackTraces(" + showErrorStackTraces + ")");
        this.mShowErrorStackTraces = showErrorStackTraces;
        return this;
    }

//    public AsyncBuilder setSignalVisualizerUri(URI uri, List<BasicNameValuePair> extraHeaders) {
//        signalVisualizerClient = new SignalVisualizerClient.Builder()
//                .setUri(uri)
//                .setExtraHeaders(extraHeaders)
//                .build();
//        if (signalVisualizerClient == null) {
//            Log.v(TAG, "signalVisualizerClient set");
//        } else {
//            Log.v(TAG, "No signalVisualizerClient");
//        }
//
//        return this;
//    }
//
//    public SignalVisualizerClient getSignalVisualizerClient() {
//        return signalVisualizerClient;
//    }

    /**
     * Complete construction of the {@link AsyncBuilder}.
     * <p>
     * The first (and usually only unless doing testing) call to this method locks in the implementation
     * configurion defaults of the {@link Async} utility class and all the services it provides.
     *
     * @return the newly created Async class. This value can be ignored if you prefer to <code>static import com.futurice.Async.*</code>
     * for convenience.
     */
    @NonNull
    @nonnull
    @NotCallOrigin
    public Async build() {
        if (mUiThread == null) {
            Thread thread = Thread.currentThread();
            try {
                mContext.getMainLooper().getThread();
            } catch (NullPointerException e) {
                // Needed for Google instrumentation test runner
            }
            setUI_Thread(thread);
        }
        if (mStrictMode) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
        Log.v(TAG, "AsyncBuilder complete");

        sAsyncBuilder = this;
        return new Async(); //TODO Pass the builder as an argument to the constructor
    }
}
