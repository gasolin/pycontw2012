package tw.idv.gasolin.pycontw2012.ui.tablet;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListPopupWindow;
import android.widget.PopupWindow;
import android.widget.TextView;

import tw.idv.gasolin.pycontw2012.R;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract;
import tw.idv.gasolin.pycontw2012.ui.BaseActivity;
import tw.idv.gasolin.pycontw2012.ui.SponsorLevelsFragment.SponsorLevelsAdapter;

public class SponsorLevelsDropdownFragment extends Fragment implements
    AdapterView.OnItemClickListener, PopupWindow.OnDismissListener {

    private boolean mAutoloadTarget = true;
    private SponsorLevelsAdapter mAdapter;

    private ListPopupWindow mListPopupWindow;
    private ViewGroup mRootView;
    private TextView mTitle;
    private TextView mAbstract;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new SponsorLevelsAdapter(getActivity());

        if ( savedInstanceState != null ) {
            // Prevent auto-load behavior on orientation change.
            mAutoloadTarget = false;
        }

        reloadFromArguments(getArguments());
    }

    public void reloadFromArguments(Bundle arguments) {
        // Teardown from previous arguments
        if ( mListPopupWindow != null ) {
            mListPopupWindow.setAdapter(null);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {
        mRootView = (ViewGroup) inflater.inflate(
            R.layout.fragment_tracks_dropdown, null);
        mTitle = (TextView) mRootView.findViewById(R.id.track_title);
        mAbstract = (TextView) mRootView.findViewById(R.id.track_abstract);

        mRootView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                mListPopupWindow = new ListPopupWindow(getActivity());
                mListPopupWindow.setAdapter(mAdapter);
                mListPopupWindow.setModal(true);
                mListPopupWindow.setContentWidth(400);
                mListPopupWindow.setAnchorView(mRootView);
                mListPopupWindow.setOnItemClickListener(SponsorLevelsDropdownFragment.this);
                mListPopupWindow.show();
                mListPopupWindow.setOnDismissListener(SponsorLevelsDropdownFragment.this);
            }
        });
        return mRootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    /** {@inheritDoc} */
    public void onItemClick(AdapterView<?> parent, View view, int position,
        long id) {
        loadSponsorLevel(position, true);

        if ( mListPopupWindow != null ) {
            mListPopupWindow.dismiss();
        }
    }

    public void loadSponsorLevel(int level, boolean loadTargetFragment) {
        mTitle.setText(getResources().getStringArray(
            R.array.sponsor_level_names)[level]);
        mAbstract.setText("");

        final Resources res = getResources();

        mTitle.setTextColor(res.getColor(R.color.body_text_1));
        mAbstract.setTextColor(res.getColor(R.color.body_text_2));
        mRootView.findViewById(R.id.track_dropdown_arrow)
            .setBackgroundResource(R.drawable.track_dropdown_arrow_dark);

        if ( loadTargetFragment ) {
            final Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(CoscupContract.Sponsors.buildSponsorsDirUri(level));

            ( (BaseActivity) getActivity() ).openActivityOrFragment(intent);
        }
    }

    public void onDismiss() {
        mListPopupWindow = null;
    }

}
