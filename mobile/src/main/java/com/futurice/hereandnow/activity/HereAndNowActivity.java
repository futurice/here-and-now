package com.futurice.hereandnow.activity;

import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.SearchView;
import android.text.TextWatcher;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;

import com.futurice.cascade.i.CallOrigin;
import com.futurice.cascade.i.IActionOne;
import com.futurice.cascade.i.nonnull;
import com.futurice.cascade.i.nullable;
import com.futurice.cascade.reactive.ReactiveValue;
import com.futurice.hereandnow.Constants;
import com.futurice.hereandnow.HereAndNowApplication;
import com.futurice.hereandnow.R;
import com.futurice.hereandnow.fragment.BaseHereAndNowFragment;
import com.futurice.hereandnow.fragment.HappeningNowFragment;
import com.futurice.hereandnow.fragment.PeopleNearbyFragment;
import com.futurice.hereandnow.fragment.TrendingFragment;
import com.futurice.hereandnow.singleton.ModelSingleton;
import com.futurice.hereandnow.singleton.ServiceSingleton;
import com.futurice.hereandnow.utils.HereAndNowUtils;
import com.futurice.hereandnow.utils.ViewUtils;
import com.futurice.hereandnow.view.SlidingTabLayout;
import com.futurice.scampiclient.items.Peer;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import fi.tkk.netlab.dtn.scampi.applib.SCAMPIMessage;

import static com.futurice.cascade.Async.dd;
import static com.futurice.cascade.Async.ee;
import static com.futurice.cascade.Async.ii;

@CallOrigin
public class HereAndNowActivity extends BaseActivity {
    public static final int PLAY_VIDEO_INTENT_RESULT = 12345;
    public static final int PUBLISH_MEDIA_INTENT_RESULT = 12346;
    public static final int SETTINGS_INTENT_RESULT = 12348;
    private static final int REQUEST_VIDEO_CAPTURE = 1;
    private static final ServiceSingleton serviceSingleton = ServiceSingleton.instance();
//    private static final ModelSingleton modelSingleton = ModelSingleton.instance();
//    private static PersistentValue<Integer> currentListViewMode;
//    private static IReactiveTarget<Integer> viewModeSubscription; //TODO This prevents weakref from collecting quickly. Fix the subscription and then remove this
    private static IActionOne<Uri> actionAfterSelectVideo;
//    private static IActionOne<Uri> actionAfterCaptureVideo;
    ReactiveValue<String> chatReactiveValue; // A View Model object, we will bind the ReactiveTextView to this and similar objects
    private ViewPager mViewPager;
    private int mCurrentTabPosition = 0;
    private SearchView mSearchView;
    private EditText mSearchTextView;
    private final SparseArray<TextWatcher> mSearchWatchers = new SparseArray<>();
    private final List<CharSequence> mSearchQuery = Arrays.asList("", "", "");

    private static int getFragmentIndex(@NonNull final BaseHereAndNowFragment fragment) {
        if (fragment instanceof PeopleNearbyFragment) {
            return Constants.LISTVIEWMODE_PEOPLE_NEARBY;
        } else if (fragment instanceof HappeningNowFragment) {
            return Constants.LISTVIEWMODE_HAPPENING_NOW;
        } else {
            return Constants.LISTVIEWMODE_TRENDING;
        }
    }

    private void connectScampiServices() {
        serviceSingleton.scampiHandler().scheduleReconnect();
        serviceSingleton.peerDiscoveryService().startAdvertisingLocalUser();

        //TODO After connection attempt, re-publish all previously unpublished messages with appropriate time-to-live adjustments
    }

    private void disconnectScampiServices() {
        ServiceSingleton.instance().scampiHandler().cancelReconnect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        HereAndNowApplication.startServiceIf(this);
        connectScampiServices();
    }

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        initReactiveValues();
        setContentView(R.layout.activity_here_and_now);

        mViewPager = (ViewPager) findViewById(R.id.view_pager);
        mViewPager.setAdapter(new HereAndNowFragmentPagerAdapter(this.getSupportFragmentManager()));
        mViewPager.setCurrentItem(1); // Start with the HappeningNow fragment

        SlidingTabLayout mSlidingTabLayout = (SlidingTabLayout) findViewById(R.id.sliding_tabs);
        mSlidingTabLayout.setDistributeEvenly(true);
        mSlidingTabLayout.setCustomTabView(R.layout.tab_custom_layout, 0);
        mSlidingTabLayout.setViewPager(mViewPager);
        mSlidingTabLayout.setCustomTabColorizer(new SlidingTabLayout.TabColorizer() {
            @Override
            public int getIndicatorColor(int position) {
                return ViewUtils.getColor(getApplicationContext(), position);
            }

            @Override
            public int getDividerColor(int position) {
                return 0;
            }
        });

        mSlidingTabLayout.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageSelected(final int position) {
                if (mSearchTextView == null) {
                    return;
                }

                mSearchTextView.removeTextChangedListener(mSearchWatchers.get(mCurrentTabPosition));
                mSearchTextView.addTextChangedListener(mSearchWatchers.get(position));

                mSearchQuery.set(mCurrentTabPosition, mSearchView.getQuery().toString());
                mSearchView.setQuery(mSearchQuery.get(position), false);

                switch (position) {
                    case Constants.LISTVIEWMODE_TRENDING:
                    default:
                        mSearchView.setQueryHint(getString(R.string.search_hint_trending));
                        break;
                    case Constants.LISTVIEWMODE_HAPPENING_NOW:
                        mSearchView.setQueryHint(getString(R.string.search_hint_happening_now));
                        break;
                    case Constants.LISTVIEWMODE_PEOPLE_NEARBY:
                        mSearchView.setQueryHint(getString(R.string.search_hint_people));
                        collapseLastPeopleNearby();
                        break;
                }

                mCurrentTabPosition = position;
            }

            @Override
            public void onPageScrolled(final int position, final float positionOffset, final int positionOffsetPixels) {
            }

            @Override
            public void onPageScrollStateChanged(final int state) {
            }
        });

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    public void collapseLastPeopleNearby() {
        PeopleNearbyFragment peopleFragment = (PeopleNearbyFragment) getSupportFragmentManager().findFragmentByTag(HereAndNowUtils.getFragmentTag(mViewPager.getId(), Constants.LISTVIEWMODE_PEOPLE_NEARBY));
        if (peopleFragment != null) {
            peopleFragment.collapseLast();
        }

    }

    @Override
    protected void onNewIntent(@NonNull final Intent intent) {
        super.onNewIntent(intent);

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            // TODO filter according to the query
            String query = intent.getStringExtra(SearchManager.QUERY);
        }
    }

    private void initReactiveValues() {
        // There are all per app run- not initialized from flash memory like the persistent values
        if (chatReactiveValue == null) {
            chatReactiveValue = new ReactiveValue<>("ChatValue", ""); // Bindings to this View Model will fire on the UI thread by default, but they can specify something else if they prefer
//            mostRecentSelectedPictureUri = new ReactiveValue<>("MostRecentSelectedPictureLocation", Uri.parse(""));
//            mostRecentScampiIncomingPictureUri = new ReactiveValue<>(UI, "MostRecentReceivedPictureLocation");
//            mostRecentSelectedVideoUri = new ReactiveValue<>("MostRecentSelectedVideoLocation", Uri.parse(""));
//            mostRecentScampiIncomingVideoUri = new ReactiveValue<>("MostRecentScampiIncomingVideoLocation", Uri.parse(""));
        }
    }

    @Override
    protected void onPause() {
        // disconnectScampiServices();
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();

        // We should be OK without the unsubscribeSource, but it doesn't hurt and may help we we work out the kinks
//        chatReactiveValue.unsubscribeSource((ReactiveTextView) findViewById(R.id.chat_text_view));
    }

    @Override
    protected void onDestroy() {
        HereAndNowApplication.stopServiceDelayed(this);
        super.onDestroy();

        //TODO Persist unpublished messages to the bundle if they have not yet expired while waiting to publish
//        List<SCAMPIMessage> unpublishedSCAMPIMessages = serviceSingleton.scampiHandler().stopServiceScan(SCAMPI_HANDLER_STOP_TIMEOUT);
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull @nonnull final Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.options_menu, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        mSearchView = (SearchView) MenuItemCompat.getActionView(searchItem);
        mSearchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));

        // Set cursor color to match the text color
        mSearchTextView = (EditText) mSearchView.findViewById(android.support.v7.appcompat.R.id.search_src_text);
        try {
            Field mCursorDrawableRes = TextView.class.getDeclaredField("mCursorDrawableRes");
            mCursorDrawableRes.setAccessible(true);
            mCursorDrawableRes.set(mSearchTextView, 0);
        } catch (Exception e) {
        }

        // Init the search field watcher if it's available
        if (mSearchWatchers.get(0) != null) {
            mSearchTextView.addTextChangedListener(mSearchWatchers.get(0));
        }

        super.onCreateOptionsMenu(menu);
        return true;
    }

    public void fireSelectContentIntent(
            @NonNull @nonnull final IActionOne<Uri> action,
            @NonNull @nonnull final String mediaType,
            final int result) {
        HereAndNowActivity.actionAfterSelectVideo = action;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType(mediaType);
        startActivityForResult(Intent.createChooser(intent, getResources().getString(R.string.gen_select_media)), result);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull @nonnull final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivityForResult(new Intent(this, SettingsActivity.class), SETTINGS_INTENT_RESULT);
            return true;
        }

        if (id == R.id.action_close_service) {
            List<SCAMPIMessage> unpublishedSCAMPIMessages = serviceSingleton.scampiHandler().stop(1000);
            disconnectScampiServices();
            HereAndNowApplication.stopServiceNow(this);
            HereAndNowApplication app = (HereAndNowApplication) getApplication();
            app.removeCallback();
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(
            final int requestCode,
            final int resultCode,
            @NonNull @nonnull final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PLAY_VIDEO_INTENT_RESULT) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    actionAfterSelectVideo.call(data.getData());
                } catch (Exception e) {
                    ee(this, "Problem executing action after content selection", e);
                }
            } else {
                ii(this, "Problem with externally selected video play intent result: " + resultCode);
            }
        }
        if (requestCode == PUBLISH_MEDIA_INTENT_RESULT) {
            if (resultCode == Activity.RESULT_OK) {
                try {
                    actionAfterSelectVideo.call(data.getData());
                } catch (Exception e) {
                    ee(this, "Problem executing action after content selection", e);
                }
            } else {
                ii(this, "Problem with externally selected video publish intent result: " + resultCode);
            }
        }
        if (requestCode == SETTINGS_INTENT_RESULT) {
            dd(this, "Copying over local reactive persistent settings with the latest values from the SettingsActivity");
            String preexistingIdTag = PreferenceManager.getDefaultSharedPreferences(this).getString(SettingsActivity.ID_TAG_KEY, UUID.randomUUID().toString());
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
            editor.putString(SettingsActivity.ID_TAG_KEY, preexistingIdTag);
            editor.apply();
            ModelSingleton.instance().myTag.set(data.getStringExtra(SettingsActivity.TAG_KEY));
            ModelSingleton.instance().myIdTag.set(preexistingIdTag);
            ModelSingleton.instance().myAboutMe.set(data.getStringExtra(SettingsActivity.ABOUT_KEY));

            // XXX: Not sure if the reactive stuff works, so I'll just push
            // the changes old fashioned way.
            // TODO: Should check if there actually was a change to optimize away useless calls.
            final Peer localPeer = new Peer(ModelSingleton.instance().myTag.get(),
                    ModelSingleton.instance().myIdTag.get(),
                    ModelSingleton.instance().myAboutMe.get(),
                    ModelSingleton.instance().myLikes.get(),
                    ModelSingleton.instance().deletedCards.get(),
                    ModelSingleton.instance().flaggedCards.get(),
                    ModelSingleton.instance().myComments.get(),
                    System.currentTimeMillis());

            serviceSingleton.peerDiscoveryService().updateLocalUser(localPeer);
            serviceSingleton.peerDiscoveryService().refreshLocalAdvert();
        }
    }

    private void dispatchTakeVideoIntent() {
        final Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }

    //===============================================================================//

    public void onFragmentAttached(@NonNull @nonnull final BaseHereAndNowFragment fragment) {
        final int position = getFragmentIndex(fragment);
        boolean firstWatcher = position == 0 && mSearchWatchers.get(0) == null;
        mSearchWatchers.put(position, fragment);

        if (firstWatcher && mSearchTextView != null) {
            mSearchTextView.addTextChangedListener(fragment);
        }
    }

    public class HereAndNowFragmentPagerAdapter extends FragmentPagerAdapter {

        public HereAndNowFragmentPagerAdapter(@NonNull final FragmentManager fm) {
            super(fm);
        }

        @Override
        @Nullable
        @nullable
        public Fragment getItem(final int position) {
            BaseHereAndNowFragment fragment;
            switch (position) {
                case Constants.LISTVIEWMODE_TRENDING:
                    fragment = TrendingFragment.newInstance();
                    break;
                case Constants.LISTVIEWMODE_HAPPENING_NOW:
                    fragment = HappeningNowFragment.newInstance();
                    break;
                case Constants.LISTVIEWMODE_PEOPLE_NEARBY:
                    fragment = PeopleNearbyFragment.newInstance();
                    break;
                default:
                    fragment = null;
            }

            return fragment;
        }

        @Override
        public int getCount() {
            return Constants.NUMBER_OF_LIST_VIEW_MODES;
        }

        @Override
        @NonNull
        @nonnull
        public CharSequence getPageTitle(final int position) {
            final CharSequence title;
            switch (position) {
                case Constants.LISTVIEWMODE_TRENDING:
                    title = getResources().getString(R.string.tab_trending_title);
                    break;
                case Constants.LISTVIEWMODE_HAPPENING_NOW:
                    title = getResources().getString(R.string.tab_now_title);
                    break;
                case Constants.LISTVIEWMODE_PEOPLE_NEARBY:
                    title = getResources().getString(R.string.tab_people_nearby_title);
                    break;
                default:
                    title = ""; // TODO throw instead
            }
            return title;
        }
    }
}
