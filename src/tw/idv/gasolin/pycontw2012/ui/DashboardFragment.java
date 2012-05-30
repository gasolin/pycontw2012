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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import tw.idv.gasolin.pycontw2012.R;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract;
import tw.idv.gasolin.pycontw2012.ui.phone.ScheduleActivity;
import tw.idv.gasolin.pycontw2012.ui.phone.SponsorLevelsActivity;
import tw.idv.gasolin.pycontw2012.ui.tablet.ScheduleMultiPaneActivity;
import tw.idv.gasolin.pycontw2012.ui.tablet.SessionsMultiPaneActivity;
import tw.idv.gasolin.pycontw2012.ui.tablet.SponsorsMultiPaneActivity;
import tw.idv.gasolin.pycontw2012.util.AnalyticsUtils;
import tw.idv.gasolin.pycontw2012.util.UIUtils;

public class DashboardFragment extends Fragment {

    private View mScheduleButton;

    public void fireTrackerEvent(String label) {
        AnalyticsUtils.getInstance(getActivity())
            .trackEvent("Home Screen Dashboard", "Click", label, 0);
    }

    public void setScheduleButtonEnabled(boolean refreshing) {
        if ( mScheduleButton != null ) {
            mScheduleButton.setClickable(!refreshing);
            mScheduleButton.setEnabled(!refreshing);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_dashboard, container);

        // Attach event handlers
        mScheduleButton = root.findViewById(R.id.home_btn_schedule);
        mScheduleButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                fireTrackerEvent("Schedule");
                if ( UIUtils.isHoneycombTablet(getActivity()) ) {
                    startActivity(new Intent(
                        getActivity(),
                        ScheduleMultiPaneActivity.class));
                } else {
                    startActivity(new Intent(
                        getActivity(),
                        ScheduleActivity.class));
                }

            }

        });

        root.findViewById(R.id.home_btn_sessions)
            .setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    fireTrackerEvent("Sessions");
                    // Launch sessions list
                    if ( UIUtils.isHoneycombTablet(getActivity()) ) {
                        startActivity(new Intent(
                            getActivity(),
                            SessionsMultiPaneActivity.class));
                    } else {
                        final Intent intent = new Intent(
                            Intent.ACTION_VIEW,
                            CoscupContract.Tracks.CONTENT_URI);
                        intent.putExtra(Intent.EXTRA_TITLE,
                            getString(R.string.title_session_tracks));
                        startActivity(intent);
                    }

                }
            });

        root.findViewById(R.id.home_btn_starred)
            .setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    fireTrackerEvent("Starred");
                    // Launch list of sessions and vendors the user has starred
                    startActivity(new Intent(
                        getActivity(),
                        StarredActivity.class));
                }
            });

        root.findViewById(R.id.home_btn_vendors)
            .setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    fireTrackerEvent("Sponsors");
                    // Launch vendors list
                    if ( UIUtils.isHoneycombTablet(getActivity()) ) {
                        startActivity(new Intent(
                            getActivity(),
                            SponsorsMultiPaneActivity.class));
                    } else {
                        final Intent intent = new Intent(
                            getActivity(),
                            SponsorLevelsActivity.class);
                        intent.putExtra(Intent.EXTRA_TITLE,
                            getString(R.string.title_sponsor_levels));
                        startActivity(intent);
                    }
                }
            });

        root.findViewById(R.id.home_btn_announcements)
            .setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    // splicing in tag streamer
                    fireTrackerEvent("Bulletin");
                    Intent intent = new Intent(
                        getActivity(),
                        BulletinActivity.class);
                    startActivity(intent);
                }
            });

        /*
         * 
         * root.findViewById(R.id.home_btn_map) .setOnClickListener(new
         * View.OnClickListener() { public void onClick(View view) { // Launch
         * map of conference venue fireTrackerEvent("Map"); startActivity(new
         * Intent( getActivity(), UIUtils.getMapActivityClass(getActivity())));
         * } });
         */

        return root;
    }
}
