package com.futurice.hereandnow.fragment;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ExpandableListView;

import com.futurice.cascade.active.ImmutableValue;
import com.futurice.cascade.i.nonnull;
import com.futurice.cascade.i.nullable;
import com.futurice.hereandnow.activity.HereAndNowActivity;
import com.futurice.hereandnow.singleton.ModelSingleton;
import com.futurice.hereandnow.singleton.ServiceSingleton;
import com.futurice.hereandnow.adapter.TopicListAdapter;
import com.futurice.hereandnow.card.ITopic;

import java.util.ArrayList;
import java.util.List;

import static com.futurice.cascade.Async.assertNotNull;
import static com.futurice.cascade.Async.dd;
import static com.futurice.cascade.Async.originAsync;

/**
 * Base Fragment
 * <p>
 * Created by pper on 16/04/15.
 */
public class BaseHereAndNowFragment extends Fragment implements TextWatcher {

    @Nullable
    @nullable
    private ExpandableListView expandableListView;
    private final List<ITopic> sourceTopicModel = new ArrayList<>();

    protected static final ServiceSingleton serviceSingleton = ServiceSingleton.instance();
    @NonNull
    @nonnull
    protected static final ModelSingleton modelSingleton = ModelSingleton.instance();
    @Nullable
    @nullable
    private TopicListAdapter topicListAdapter; // View Model, sorted and filtered from the Model
    @Nullable
    @nullable
    private CharSequence searchString;
    @NonNull
    @nonnull
    final ImmutableValue<String> origin = originAsync();

    protected void initListView() {
        dd(origin, "Init HereAndNowFragment list");
        final ExpandableListView lv = assertNotNull(expandableListView);

        lv.setAdapter(topicListAdapter);
        lv.setGroupIndicator(null);
        lv.setDividerHeight(0);
        registerForContextMenu(lv);
    }

    @Nullable
    @nullable
    public TopicListAdapter getTopicListAdapter() {
        return topicListAdapter;
    }

    public void setTopicListAdapter(@NonNull @nonnull final TopicListAdapter topicListAdapter) {
        this.topicListAdapter = topicListAdapter;
    }

    @Override
    public void setUserVisibleHint(final boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        filterModel();
    }

    @Nullable
    @nullable
    public ExpandableListView getExpandableListView() {
        return expandableListView;
    }

    public void setExpandableListView(@NonNull @nonnull ExpandableListView expandableListView) {
        this.expandableListView = expandableListView;
    }

    protected List<ITopic> getSourceTopicModel() {
        return sourceTopicModel;
    }

    protected void initTopicsAndCards(
            @NonNull @nonnull final List<ITopic> preBuiltTopics,
            @NonNull @nonnull final List<ITopic> topicModel,
            @NonNull @nonnull final TopicListAdapter adapter) {
        topicModel.addAll(preBuiltTopics);
        adapter.notifyDataSetChanged();
    }

    protected void filterModel() {
        if (topicListAdapter != null) {
            topicListAdapter.getFilter().filter(searchString);
        }
    }

    @Override
    public void onTextChanged(
            @NonNull @nonnull final CharSequence sequence,
            final int start,
            final int before,
            final int count) {
        searchString = sequence;
        filterModel();
    }

    @Override
    public void beforeTextChanged(
            @NonNull @nonnull final CharSequence s,
            final int start,
            final int count,
            final int after) {
    }

    @Override
    public void afterTextChanged(@NonNull @nonnull final Editable s) {
    }

    @Override
    public void onAttach(@NonNull @nonnull final Activity activity) {
        super.onAttach(activity);
        ((HereAndNowActivity) activity).onFragmentAttached(this);
    }
}
