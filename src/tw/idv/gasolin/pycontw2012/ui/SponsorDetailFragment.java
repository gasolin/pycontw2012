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

import java.util.Locale;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import tw.idv.gasolin.pycontw2012.R;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract;
import tw.idv.gasolin.pycontw2012.util.ActivityHelper;
import tw.idv.gasolin.pycontw2012.util.AnalyticsUtils;
import tw.idv.gasolin.pycontw2012.util.BitmapUtils;
import tw.idv.gasolin.pycontw2012.util.FractionalTouchDelegate;
import tw.idv.gasolin.pycontw2012.util.NotifyingAsyncQueryHandler;
import tw.idv.gasolin.pycontw2012.util.UIUtils;

public class SponsorDetailFragment extends Fragment implements
    NotifyingAsyncQueryHandler.AsyncQueryListener,
    CompoundButton.OnCheckedChangeListener {
    private static final String TAG = SponsorDetailFragment.class.getSimpleName();

    private Uri mSponsorUri;

    private String mSponsorId;

    private ViewGroup mRootView;
    private TextView mName;
    private CompoundButton mStarred;

    private ImageView mLogo;
    private TextView mUrl;
    private TextView mDesc;
    private TextView mProductDesc;

    private String mNameString;

    private NotifyingAsyncQueryHandler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = BaseActivity.fragmentArgumentsToIntent(getArguments());
        mSponsorUri = intent.getData();
        if ( mSponsorUri == null ) {
            return;
        }

        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if ( mSponsorUri == null ) {
            return;
        }

        // Start background query to load vendor details
        mHandler = new NotifyingAsyncQueryHandler(
            getActivity().getContentResolver(),
            this);
        mHandler.startQuery(mSponsorUri, SponsorsQuery.PROJECTION);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {

        mRootView = (ViewGroup) inflater.inflate(
            R.layout.fragment_sponsor_detail, null);

        mName = (TextView) mRootView.findViewById(R.id.sponsor_name);
        mStarred = (CompoundButton) mRootView.findViewById(R.id.star_button);

        mStarred.setFocusable(true);
        mStarred.setClickable(true);

        // Larger target triggers star toggle
        final View starParent = mRootView.findViewById(R.id.header_sponsor);
        FractionalTouchDelegate.setupDelegate(starParent, mStarred, new RectF(
            0.6f,
            0f,
            1f,
            0.8f));

        mLogo = (ImageView) mRootView.findViewById(R.id.sponsor_logo);
        mUrl = (TextView) mRootView.findViewById(R.id.sponsor_url);
        mDesc = (TextView) mRootView.findViewById(R.id.sponsor_desc);
        mProductDesc = (TextView) mRootView.findViewById(R.id.sponsor_product_desc);

        return mRootView;
    }

    private View buildIndicator(int textRes) {
        final TextView indicator = (TextView) getActivity().getLayoutInflater()
            .inflate(R.layout.tab_indicator,
                (ViewGroup) mRootView.findViewById(android.R.id.tabs), false);
        indicator.setText(textRes);
        return indicator;
    }

    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        if ( getActivity() == null ) {
            return;
        }

        try {
            if ( !cursor.moveToFirst() ) {
                return;
            }

            final Locale locale = getResources().getConfiguration().locale;
            mNameString = UIUtils.getBestLocaleString(locale,
                cursor.getString(SponsorsQuery.SPONSOR_NAME),
                cursor.getString(SponsorsQuery.SPONSOR_NAME_ZH_TW),
                cursor.getString(SponsorsQuery.SPONSOR_NAME_ZH_CN));
            mName.setText(mNameString);

            // Unregister around setting checked state to avoid triggering
            // listener since change isn't user generated.
            mStarred.setOnCheckedChangeListener(null);
            mStarred.setChecked(false);
            mStarred.setOnCheckedChangeListener(this);

            // Start background fetch to load vendor logo
            final String logoUrl = cursor.getString(SponsorsQuery.SPONSOR_LOGO_URL);

            if ( !TextUtils.isEmpty(logoUrl) ) {
                BitmapUtils.fetchImage(getActivity(), logoUrl, null, null,
                    new BitmapUtils.OnFetchCompleteListener() {
                        public void onFetchComplete(Object cookie, Bitmap result) {
                            if ( result == null ) {
                                mLogo.setVisibility(View.GONE);
                            } else {
                                mLogo.setVisibility(View.VISIBLE);
                                mLogo.setImageBitmap(result);
                            }
                        }
                    });
            }

            mUrl.setText(cursor.getString(SponsorsQuery.SPONSOR_URL));
            final String desc = UIUtils.getBestLocaleString(locale,
                cursor.getString(SponsorsQuery.SPONSOR_DESC),
                cursor.getString(SponsorsQuery.SPONSOR_DESC_ZH_TW),
                cursor.getString(SponsorsQuery.SPONSOR_DESC_ZH_CN));
            UIUtils.setTextMaybeHtml(mDesc, desc);

            mSponsorId = cursor.getString(SponsorsQuery.SPONSOR_ID);

            // Assign track details when found
            // TODO: handle vendors not attached to track
            ActivityHelper activityHelper = ( (BaseActivity) getActivity() ).getActivityHelper();
            final int level = cursor.getInt(SponsorsQuery.SPONSOR_LEVEL);
            final String levelTitle = getResources().getStringArray(
                R.array.sponsor_level_names)[level];
            activityHelper.setActionBarTitle(levelTitle);
            // activityHelper.setActionBarColor();

            AnalyticsUtils.getInstance(getActivity())
                .trackPageView("/Sponsors/" + levelTitle + "/" + mNameString);

        } finally {
            cursor.close();
        }
    }

    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    }

    private interface SponsorsQuery {
        String[] PROJECTION = { CoscupContract.Sponsors.SPONSOR_ID,
            CoscupContract.Sponsors.SPONSOR_LEVEL,
            CoscupContract.Sponsors.SPONSOR_URL,
            CoscupContract.Sponsors.SPONSOR_LOGO_URL,
            CoscupContract.Sponsors.SPONSOR_NAME,
            CoscupContract.Sponsors.SPONSOR_NAME_ZH_TW,
            CoscupContract.Sponsors.SPONSOR_NAME_ZH_CN,
            CoscupContract.Sponsors.SPONSOR_DESC,
            CoscupContract.Sponsors.SPONSOR_DESC_ZH_TW,
            CoscupContract.Sponsors.SPONSOR_DESC_ZH_CN, };

        int SPONSOR_ID = 0;
        int SPONSOR_LEVEL = 1;
        int SPONSOR_URL = 2;
        int SPONSOR_LOGO_URL = 3;
        int SPONSOR_NAME = 4;
        int SPONSOR_NAME_ZH_TW = 5;
        int SPONSOR_NAME_ZH_CN = 6;
        int SPONSOR_DESC = 7;
        int SPONSOR_DESC_ZH_TW = 8;
        int SPONSOR_DESC_ZH_CN = 9;
    }
}
