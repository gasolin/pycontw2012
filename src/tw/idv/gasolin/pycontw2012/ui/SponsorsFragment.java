package tw.idv.gasolin.pycontw2012.ui;

import java.util.Locale;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import tw.idv.gasolin.pycontw2012.R;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract;
import tw.idv.gasolin.pycontw2012.util.NotifyingAsyncQueryHandler;
import tw.idv.gasolin.pycontw2012.util.UIUtils;

public class SponsorsFragment extends ListFragment implements
    NotifyingAsyncQueryHandler.AsyncQueryListener {

    private static final String STATE_CHECKED_POSITION = "checkedPosition";

    private Cursor mCursor;
    private CursorAdapter mAdapter;
    private int mCheckedPosition = -1;
    private boolean mHasSetEmptyText = false;

    private NotifyingAsyncQueryHandler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new NotifyingAsyncQueryHandler(
            getActivity().getContentResolver(),
            this);
        reloadFromArguments(getArguments());
    }

    public void reloadFromArguments(Bundle arguments) {
        // Teardown from previous arguments
        if ( mCursor != null ) {
            getActivity().stopManagingCursor(mCursor);
            mCursor = null;
        }

        mCheckedPosition = -1;
        setListAdapter(null);

        mHandler.cancelOperation(SponsorsQuery._TOKEN);

        // Load new arguments
        final Intent intent = BaseActivity.fragmentArgumentsToIntent(arguments);
        final Uri sponsorsUri = intent.getData();
        final int sponsorQueryToken;

        if ( sponsorsUri == null ) {
            return;
        }

        String[] projection;
        mAdapter = new SponsorsAdapter(getActivity());
        projection = SponsorsQuery.PROJECTION;
        sponsorQueryToken = SponsorsQuery._TOKEN;

        setListAdapter(mAdapter);

        // Start background query to load vendors
        mHandler.startQuery(sponsorQueryToken, null, sponsorsUri, projection,
            null, null, CoscupContract.Sponsors.DEFAULT_SORT);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        if ( savedInstanceState != null ) {
            mCheckedPosition = savedInstanceState.getInt(
                STATE_CHECKED_POSITION, -1);
        }

        if ( !mHasSetEmptyText ) {
            // Could be a bug, but calling this twice makes it become visible
            // when it shouldn't
            // be visible.
            setEmptyText(getString(R.string.empty_sponsors));
            mHasSetEmptyText = true;
        }
    }

    @Override
    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        if ( getActivity() == null ) {
            return;
        }

        if ( token == SponsorsQuery._TOKEN ) {
            onSponsorsQueryComplete(cursor);
        } else {
            cursor.close();
        }
    }

    private void onSponsorsQueryComplete(Cursor cursor) {
        // TODO(romannurik): stopManagingCursor on detach (throughout app)
        mCursor = cursor;
        getActivity().startManagingCursor(mCursor);
        mAdapter.changeCursor(mCursor);
        if ( mCheckedPosition >= 0 && getView() != null ) {
            getListView().setItemChecked(mCheckedPosition, true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().getContentResolver()
            .registerContentObserver(CoscupContract.Sponsors.CONTENT_URI, true,
                mSponsorChangesObserver);
        if ( mCursor != null ) {
            mCursor.requery();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().getContentResolver()
            .unregisterContentObserver(mSponsorChangesObserver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CHECKED_POSITION, mCheckedPosition);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Launch viewer for specific vendor.
        final Cursor cursor = (Cursor) mAdapter.getItem(position);
        final String sponsorId = cursor.getString(SponsorsQuery.SPONSOR_ID);
        final Uri sponsorUri = CoscupContract.Sponsors.buildSponsorUri(sponsorId);
        ( (BaseActivity) getActivity() ).openActivityOrFragment(new Intent(
            Intent.ACTION_VIEW,
            sponsorUri));

        getListView().setItemChecked(position, true);
        mCheckedPosition = position;
    }

    public void clearCheckedPosition() {
        if ( mCheckedPosition >= 0 ) {
            getListView().setItemChecked(mCheckedPosition, false);
            mCheckedPosition = -1;
        }
    }

    private class SponsorsAdapter extends CursorAdapter {
        public SponsorsAdapter(Context context) {
            super(context, null);
        }

        /** {@inheritDoc} */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return getActivity().getLayoutInflater()
                .inflate(R.layout.list_item_sponsor_oneline, parent, false);
        }

        /** {@inheritDoc} */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final Locale locale = context.getResources()
                .getConfiguration().locale;
            final String name = UIUtils.getBestLocaleString(locale,
                cursor.getString(SponsorsQuery.SPONSOR_NAME),
                cursor.getString(SponsorsQuery.SPONSOR_NAME_ZH_TW),
                cursor.getString(SponsorsQuery.SPONSOR_NAME_ZH_CN));
            ( (TextView) view.findViewById(R.id.sponsor_name) ).setText(name);

            view.findViewById(R.id.star_button)
                .setVisibility(View.INVISIBLE);
        }
    }

    private ContentObserver mSponsorChangesObserver = new ContentObserver(
        new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if ( mCursor != null ) {
                mCursor.requery();
            }
        }
    };

    private interface SponsorsQuery {
        int _TOKEN = 0x1;

        String[] PROJECTION = { BaseColumns._ID,
            CoscupContract.Sponsors.SPONSOR_ID,
            CoscupContract.Sponsors.SPONSOR_LEVEL,
            CoscupContract.Sponsors.SPONSOR_NAME,
            CoscupContract.Sponsors.SPONSOR_NAME_ZH_TW,
            CoscupContract.Sponsors.SPONSOR_NAME_ZH_CN, };

        int _ID = 0;
        int SPONSOR_ID = 1;
        int SPONSOR_LEVEL = 2;
        int SPONSOR_NAME = 3;
        int SPONSOR_NAME_ZH_TW = 4;
        int SPONSOR_NAME_ZH_CN = 5;
    }

}
