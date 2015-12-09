package com.futurice.hereandnow.fragment;

import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import com.futurice.cascade.i.nonnull;
import com.futurice.hereandnow.Constants;
import com.futurice.hereandnow.R;
import com.futurice.hereandnow.adapter.TopicListAdapter;
import com.futurice.hereandnow.utils.ViewUtils;

// TODO common base class instead of deriving from TrendingFragment
public class HappeningNowFragment extends TrendingFragment {

    @NonNull
    @nonnull
    public static HappeningNowFragment newInstance() {
        return new HappeningNowFragment();
    }

    @Override
    @NonNull
    @nonnull
    protected View setupView(@NonNull @nonnull final LayoutInflater inflater, @NonNull @nonnull final ViewGroup container) {
        return inflater.inflate(R.layout.fragment_happening_now, container, false);
    }

    @Override
    protected void initViewsAndAdapters() {
        // Set ExpandableListView values
        final ExpandableListView elv = new ExpandableListView(getActivity());
        setExpandableListView(elv);
        elv.setOnGroupExpandListener(topicIndex -> {
            if (lastExpanded != -1 && topicIndex != lastExpanded) {
                elv.collapseGroup(lastExpanded);
            }
            lastExpanded = topicIndex;
        });
        setTopicListAdapter(new TopicListAdapter(getSourceTopicModel(),
                "HappeningNowExpandableListAdapter",
                ViewUtils.getColor(getActivity(), Constants.LISTVIEWMODE_HAPPENING_NOW),
                ViewUtils.getHighlightColor(getActivity(), Constants.LISTVIEWMODE_HAPPENING_NOW)));
    }
}
