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

package tw.idv.gasolin.pycontw2012.ui.tablet;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.ViewGroup;

import tw.idv.gasolin.pycontw2012.R;
import tw.idv.gasolin.pycontw2012.ui.BaseMultiPaneActivity;
import tw.idv.gasolin.pycontw2012.ui.SponsorDetailFragment;
import tw.idv.gasolin.pycontw2012.ui.SponsorsFragment;
import tw.idv.gasolin.pycontw2012.ui.phone.SponsorDetailActivity;
import tw.idv.gasolin.pycontw2012.ui.phone.SponsorsActivity;

/**
 * A multi-pane activity, consisting of a {@link SponsorLevelsDropdownFragment},
 * a {@link SponsorsFragment}, and {@link SponsorDetailFragment}. This activity
 * is very similar in function to {@link SessionsMultiPaneActivity}.
 * 
 * This activity requires API level 11 or greater because
 * {@link SponsorLevelsDropdownFragment} requires API level 11.
 */
public class SponsorsMultiPaneActivity extends BaseMultiPaneActivity {

    private SponsorLevelsDropdownFragment mSponsorLevelsDropdownFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sponsors);

        Intent intent = new Intent();

        final FragmentManager fm = getSupportFragmentManager();
        mSponsorLevelsDropdownFragment = (SponsorLevelsDropdownFragment) fm.findFragmentById(R.id.fragment_sponsor_levels_dropdown);
        mSponsorLevelsDropdownFragment.reloadFromArguments(intentToFragmentArguments(intent));
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getActivityHelper().setupSubActivity();

        ViewGroup detailContainer = (ViewGroup) findViewById(R.id.fragment_container_sponsor_detail);
        if ( detailContainer != null && detailContainer.getChildCount() > 0 ) {
            findViewById(R.id.fragment_container_sponsor_detail).setBackgroundColor(
                0xffffffff);
        }
    }

    @Override
    public FragmentReplaceInfo onSubstituteFragmentForActivityLaunch(
        String activityClassName) {
        if ( SponsorsActivity.class.getName()
            .equals(activityClassName) ) {
            return new FragmentReplaceInfo(SponsorsFragment.class, "vendors",
                R.id.fragment_container_sponsors);
        } else if ( SponsorDetailActivity.class.getName()
            .equals(activityClassName) ) {
            findViewById(R.id.fragment_container_sponsor_detail).setBackgroundColor(
                0xffffffff);
            return new FragmentReplaceInfo(SponsorDetailFragment.class,
                "vendor_detail", R.id.fragment_container_sponsor_detail);
        }
        return null;
    }
}
