package com.futurice.hereandnow.adapter;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.*;
import android.widget.*;

import com.futurice.cascade.active.*;
import com.futurice.cascade.i.*;
import com.futurice.cascade.i.nonnull;
import com.futurice.cascade.i.nullable;
import com.futurice.hereandnow.Constants;
import com.futurice.hereandnow.R;
import com.futurice.hereandnow.card.*;
import com.futurice.hereandnow.singleton.ModelSingleton;
import com.futurice.hereandnow.utils.ViewUtils;

import java.util.*;

import static com.futurice.cascade.Async.*;

/**
 * Adapter for a list view displaying Topics and Cards.
 *
 * @author teemuk
 */
public class TopicListAdapter extends BaseExpandableListAdapter implements INamed, Filterable {
    // ListView needs to know how many different types of children (=Cards) there are.
    // This must be given statically, and each different child has to have a different type.
    private static final int CHILD_TYPE_COUNT = 5;

    private List<ITopic> sourceTopicModel;

    private List<ITopic> topicModel = new ArrayList<>();
    @Nullable
    @nullable
    private Comparator<ITopic> topicComparator;
    @Nullable
    @nullable
    private SimpleFilter topicFilter;

    @NonNull
    @nonnull
    private final ImmutableValue<String> origin = originAsync();
    @NonNull
    @nonnull
    private final String name;
    private final int color;
    private final int highlightColor;
    private int lastExpanded = 0;

    public TopicListAdapter(List<ITopic> sourceTopicModel, String name, int color, int highlightColor) {
        this.sourceTopicModel = sourceTopicModel;
        this.name = name;
        this.color = color;
        this.highlightColor = highlightColor;
    }

    @Override
    @NonNull
    @nonnull
    public View getGroupView(
            final int topicIndex,
            final boolean isExpanded,
            @Nullable @nullable final View convertView,
            @NonNull @nonnull final ViewGroup parentView) {
        final ITopic topic = this.getTopic(topicIndex);
        final View topicView;

        int topicHighlightColor = highlightColor;

       if (topic.getName() != null && topic.getName().equalsIgnoreCase(ModelSingleton.instance().myIdTag.get())) { // displaying user info for oneself
           topicHighlightColor = color;
        }

        if (convertView != null) {
            topic.updateView(convertView, isExpanded, color, topicHighlightColor);
            topicView = convertView;
        } else {
            topicView = topic.getView(parentView, isExpanded, color, topicHighlightColor);
        }

        return topicView;
    }

    @NonNull
    @nonnull
    public ITopic getTopic(final int topicIndex) {
        return topicModel.get(topicIndex);
    }

    @NonNull
    @nonnull
    public ICard getCard(final int topicIndex, final int cardIndexWithinTopic) {
        return getTopic(topicIndex).getCards().get(cardIndexWithinTopic);
    }

    /**
     * Sets a comparator for sorting the list contents.
     *
     * @param comparator
     */
    public void setSortFunction(@Nullable @nullable final Comparator<ITopic> comparator) {
        this.topicComparator = comparator;
    }

    /**
     * Adds a filter function, in addition to the default search box filtering.
     *
     * @param filter
     */
    public void setFilterFunction(@Nullable @nullable final SimpleFilter filter) {
        this.topicFilter = filter;
    }

    public void updateSorting() {
        if (this.topicComparator != null) {
            Collections.sort(this.topicModel, this.topicComparator);
            notifyDataSetChanged();
        }
    }

    // This Function used to inflate Card row views
    @Override
    @NonNull
    @nonnull
    public View getChildView(
            final int topicIndex,
            final int cardIndexWithinTopic,
            final boolean isLastChild,
            @Nullable @nullable final View convertView,
            @NonNull @nonnull ViewGroup parentView) {
        final ICard card = getCard(topicIndex, cardIndexWithinTopic);

        final View topicView;
        if (convertView != null) {
            vv(this, origin, "View is being recycled: " + convertView);
            card.updateView(convertView, color, highlightColor);
            topicView = convertView;
        } else {
            vv(this, origin, "New View is being created");
            topicView = card.getView(parentView, color, highlightColor);
        }

        return topicView;
    }

    @Override
    @NonNull
    @nonnull
    public Object getChild(final int groupPosition, final int childPosition) {
        return topicModel.get(groupPosition).getCards().get(childPosition);
    }

    //Call when child row clicked
    @Override
    public long getChildId(final int groupPosition, final int childPosition) {
        return topicModel.get(groupPosition).getCards().get(childPosition).getUid();
    }

    @Override
    public int getChildrenCount(final int groupPosition) {
        return topicModel.get(groupPosition).getCards().size();
    }

    @Override
    public Object getGroup(final int groupPosition) {
        ITopic topic = topicModel.get(groupPosition);
        dd(this, origin, "getGroup(" + groupPosition + ") will return " + topic.getName());

        return topic;
    }

    @Override
    public int getGroupCount() {
        return topicModel.size();
    }

    @Override
    public void onGroupExpanded(final int topicIndex) {
        super.onGroupExpanded(topicIndex);
        ((Topic) getTopic(lastExpanded)).collapsed();
        ((Topic) getTopic(topicIndex)).expanded();
        lastExpanded = topicIndex;
    }

    @Override
    public void onGroupCollapsed(final int topicIndex) {
        super.onGroupCollapsed(topicIndex);

        ((Topic) getTopic(topicIndex)).collapsed();
    }

    //Call when parent row clicked
    @Override
    public long getGroupId(final int groupPosition) {
        return topicModel.get(groupPosition).getUid();
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
    }

    @Override
    public boolean isEmpty() {
        return topicModel.isEmpty();
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean areAllItemsEnabled() {
        return true;
    }

    @Override
    @NonNull
    @nonnull
    public String getName() {
        return name;
    }

    @Override
    public int getChildTypeCount() {
        return CHILD_TYPE_COUNT;
    }

    @Override
    public int getChildType(final int groupPosition, final int childPosition) {
        return this.topicModel.get(groupPosition).getCards().get(childPosition).getType();
    }

    public interface SimpleFilter {
        /**
         * Returns true if the topic should pass the filter, false otherwise.
         *
         * @param topic
         * @return
         */
        boolean filter(@NonNull @nonnull ITopic topic);
    }

    @Override
    public Filter getFilter() {
        final Filter filter = new Filter() {
            @Override
            protected FilterResults performFiltering(@NonNull @nonnull final CharSequence constraint) {
                FilterResults results = new FilterResults();

                ArrayList<ITopic> filtered = new ArrayList<>();
                for (ITopic topic : sourceTopicModel) {
                    if (TopicListAdapter.this.filter(topic, constraint)) {
                        filtered.add(topic);
                    }
                }

                results.values = filtered;
                results.count = filtered.size();
                return results;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(
                    @NonNull @nonnull final CharSequence constraint,
                    @NonNull @nonnull final FilterResults results) {

                topicModel = (List<ITopic>) results.values;

                if (topicComparator != null) {
                    Collections.sort(topicModel, topicComparator);
                }

                notifyDataSetChanged();
            }
        };

        return filter;
    }

    public boolean filter(@NonNull @nonnull final ITopic topic, @Nullable @nullable final CharSequence constraint) {
        boolean result = true;

        if (constraint != null && constraint.length() > 0) {
            result = topic.matchesSearch(constraint.toString());
        }

        // Check if all of the cards in this topic are deleted
        result = result && topic.getCards().size() > 0;

        // Check if topic matches the search string
        if (result && this.topicFilter != null) {
            result = this.topicFilter.filter(topic);
        }

        return result;
    }

    public List<ITopic> getTopicModel() {
        return topicModel;
    }

    public void setTopicModel(List<ITopic> topicModel) {
        this.topicModel = topicModel;
    }

    public List<ITopic> getSourceTopicModel() {
        return sourceTopicModel;
    }

    public void setSourceTopicModel(List<ITopic> sourceTopicModel) {
        this.sourceTopicModel = sourceTopicModel;
    }
}
