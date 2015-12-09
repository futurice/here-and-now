package com.futurice.hereandnow.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;

import com.futurice.cascade.i.nonnull;
import com.futurice.cascade.i.nullable;
import com.futurice.hereandnow.Constants;
import com.futurice.hereandnow.R;
import com.futurice.hereandnow.adapter.TopicListAdapter;
import com.futurice.hereandnow.card.ITopic;
import com.futurice.hereandnow.card.PeerProfileCard;
import com.futurice.hereandnow.card.Topic;
import com.futurice.hereandnow.i.ScampiDataChangeListener;
import com.futurice.hereandnow.utils.HereAndNowUtils;
import com.futurice.hereandnow.utils.ViewUtils;
import com.futurice.scampiclient.ScampiPeerDiscoveryService;
import com.futurice.scampiclient.items.Peer;

import java.util.ArrayList;
import java.util.List;

import static com.futurice.cascade.Async.assertNotNull;
import static com.futurice.cascade.Async.dd;

public class PeopleNearbyFragment extends BaseHereAndNowFragment {

    private int lastExpanded = -1;
    private boolean collapse = true;

    public static PeopleNearbyFragment newInstance() {
        PeopleNearbyFragment fragment = new PeopleNearbyFragment();
        Bundle b = new Bundle();
        fragment.setArguments(b);

        return fragment;
    }

    @NonNull
    @nonnull
    private ScampiDataChangeListener<Peer> peerReceivedListener = new ScampiDataChangeListener<Peer>() {
        @Override
        public void onItemAdded(@NonNull @nonnull final Peer peer) {
            gotPeerDiscovery(peer);
        }

        @Override
        public void onItemsUpdated(@NonNull @nonnull final long[] uids) {
            // Peers arent' updated
        }

        @Override
        public void onItemsRemoved(@NonNull @nonnull final long[] uidArray) {
            // Peer deletions are still based on separate topic timestamps
        }
    };

    @Override
    @NonNull
    @nonnull
    public View onCreateView(
            @NonNull @nonnull final LayoutInflater inflater,
            @Nullable @nullable final ViewGroup container,
            @Nullable @nullable final Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        final View rootView = inflater.inflate(R.layout.fragment_people_nearby, container, false);
        initViewsAndAdapters();

        final FrameLayout messageListFrameLayout = (FrameLayout) rootView.findViewById(R.id.here_and_now_message_list);
        messageListFrameLayout.addView(assertNotNull(getExpandableListView()));

        connectScampiServices();
        initListView();

        modelSingleton.notifyAllPeers(peerReceivedListener);
        return rootView;
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        disconnectScampiServices();
    }

    private void initViewsAndAdapters() {
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
                "PeopleNearbyExpandableListAdapter",
                ViewUtils.getColor(getActivity(), Constants.LISTVIEWMODE_PEOPLE_NEARBY),
                ViewUtils.getHighlightColor(getActivity(), Constants.LISTVIEWMODE_PEOPLE_NEARBY)));
    }

    /**
     * Receive a peer notification from the network
     *
     * @param peer
     */
    private void gotPeerDiscovery(@NonNull @nonnull final Peer peer) {
        dd(this, "Found peer: " + peer);

        // Filter out local user (based on aboutMe). TODO: anonymous?
        //Why based on aboutMe instead of Tag?
//        if (peer.aboutMe.equals(serviceSingleton.myAboutMe.get())) {
//            return;
//        }

        // Topic for the peer
        getActivity().runOnUiThread(() -> {
            Topic topic = null;
            PeerProfileCard profileCard = null;
            boolean addToModel = false;
            List<ITopic> removeMeTopics = new ArrayList<>();

            // Check if the peer already exists in the topics for "tribe" model
            // TODO: Set "freshness" for the entry so it doesn't get timed out
            for (ITopic existingTopic : getSourceTopicModel()) {
                if (existingTopic.getName().equals(peer.idTag)) {
                    ((Topic) existingTopic).setTimestamp(System.currentTimeMillis());
                    topic = (Topic) existingTopic;
                    // Assume there is a profile card
                    // TODO: Be smarter for production.
                    profileCard = (PeerProfileCard) topic.getCards().get(0);
                } else if (isTopicOutdated((Topic) existingTopic)) {
                    removeMeTopics.add(existingTopic);
                }
            }

            getSourceTopicModel().removeAll(removeMeTopics);

            // Create a new topic if necessary
            if (topic == null) {
                // New topic
                topic = new Topic(peer.idTag, this.getActivity());
                topic.setTimestamp(peer.timestamp);
                topic.setText(peer.tag);
                topic.setImageUri(HereAndNowUtils.getResourceUri(R.drawable.group_small_icon)); //TODO Different icons based on tag match- in tribe looks better

                // New profile card
                profileCard = new PeerProfileCard(peer.aboutMe + "-profile", this.getActivity());
                addToModel = true;
            }
            topic.setText(peer.tag);
            // Set the profile data
            profileCard.setPeerAboutMe(peer.aboutMe);
            profileCard.setPeerTag(peer.tag);
            profileCard.setPeerIdTag(peer.idTag);

            // Update view
            // XXX: Do these need to be done from the main thread? Doing for safety.
            // I'm told yes, everything UI from main thread or odd stuff happens
            final Topic topicF = topic;
            final boolean addToModelF = addToModel;
            final PeerProfileCard cardF = profileCard;

            // Update view model
            if (addToModelF) {
                topicF.addCard(cardF);
                getSourceTopicModel().add(topicF);
            }

            filterModel();

//            getExpandableListView().invalidate();
        });

        dd(this, "Tribe model has " + getSourceTopicModel().size() + " entries.");
    }

    private void connectScampiServices() {
        modelSingleton.addPeerListener(peerReceivedListener);
    }

    private void disconnectScampiServices() {
        modelSingleton.removePeerListener(peerReceivedListener);
    }

    public void collapseLast() {
        if (collapse) {
            getExpandableListView().collapseGroup(lastExpanded);
        } else {
            collapse = true;
        }
    }

    public boolean isTopicOutdated(Topic topic) {
        long timeSec = (System.currentTimeMillis() - topic.getTimestamp()) / 1000;
        return timeSec > ScampiPeerDiscoveryService.MESSAGE_LIFETIME_SECONDS;
    }
}
