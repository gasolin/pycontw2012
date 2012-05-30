/*
 * Copyright 2011 Google Inc.
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

package tw.idv.gasolin.pycontw2012.ui;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import tw.idv.gasolin.pycontw2012.R;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract;
import tw.idv.gasolin.pycontw2012.util.AnalyticsUtils;
import tw.idv.gasolin.pycontw2012.util.NotifyingAsyncQueryHandler;

/**
 * A simple {@link ListFragment} that renders a list of tracks with available
 * sessions using a {@link TracksAdapter}.
 */
public class TracksFragment extends ListFragment implements
    NotifyingAsyncQueryHandler.AsyncQueryListener {

    private TracksAdapter mAdapter;
    private NotifyingAsyncQueryHandler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = BaseActivity.fragmentArgumentsToIntent(getArguments());
        final Uri tracksUri = intent.getData();

        mAdapter = new TracksAdapter(getActivity());
        setListAdapter(mAdapter);

        // Filter our tracks query to only include those with valid results
        String[] projection = TracksAdapter.TracksQuery.PROJECTION;
        String selection = null;
        // Only show tracks with at least one session
        projection = TracksAdapter.TracksQuery.PROJECTION_WITH_SESSIONS_COUNT;
        selection = CoscupContract.Tracks.SESSIONS_COUNT + ">0";
        AnalyticsUtils.getInstance(getActivity())
            .trackPageView("/Tracks");

        // Start background query to load tracks
        mHandler = new NotifyingAsyncQueryHandler(
            getActivity().getContentResolver(),
            this);
        mHandler.startQuery(tracksUri, projection, selection, null,
            CoscupContract.Tracks.DEFAULT_SORT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {

        ViewGroup root = (ViewGroup) inflater.inflate(
            R.layout.fragment_list_with_spinner, null);

        // For some reason, if we omit this, NoSaveStateFrameLayout thinks we
        // are
        // FILL_PARENT / WRAP_CONTENT, making the progress bar stick to the top
        // of the activity.
        root.setLayoutParams(new ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.FILL_PARENT,
            ViewGroup.LayoutParams.FILL_PARENT));
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }

    /** {@inheritDoc} */
    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        if ( getActivity() == null ) {
            return;
        }

        getActivity().startManagingCursor(cursor);
        mAdapter.setHasAllItem(true);
        mAdapter.changeCursor(cursor);
    }

    /** {@inheritDoc} */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        final Cursor cursor = (Cursor) mAdapter.getItem(position);
        final String trackId;

        if ( cursor != null ) {
            trackId = cursor.getString(TracksAdapter.TracksQuery.TRACK_ID);
        } else {
            trackId = CoscupContract.Tracks.ALL_TRACK_ID;
        }

        final Intent intent = new Intent(Intent.ACTION_VIEW);
        final Uri trackUri = CoscupContract.Tracks.buildTrackUri(trackId);
        intent.putExtra(SessionDetailFragment.EXTRA_TRACK, trackUri);

        if ( cursor == null ) {
            intent.setData(CoscupContract.Sessions.CONTENT_URI);
        } else {
            intent.setData(CoscupContract.Tracks.buildSessionsUri(trackId));
        }

        ( (BaseActivity) getActivity() ).openActivityOrFragment(intent);

        getListView().setItemChecked(position, true);
    }
}
