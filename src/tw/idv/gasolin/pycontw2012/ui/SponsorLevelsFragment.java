package tw.idv.gasolin.pycontw2012.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import tw.idv.gasolin.pycontw2012.R;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract;
import tw.idv.gasolin.pycontw2012.util.AnalyticsUtils;

public class SponsorLevelsFragment extends ListFragment {

    private SponsorLevelsAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new SponsorLevelsAdapter(getActivity());
        setListAdapter(mAdapter);

        AnalyticsUtils.getInstance(getActivity())
            .trackPageView("/Sponsors");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
        Bundle savedInstanceState) {

        ViewGroup root = (ViewGroup) inflater.inflate(
            R.layout.fragment_list_with_spinner, null);

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
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        final int sponsorLevel = position;

        final Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(CoscupContract.Sponsors.buildSponsorsDirUri(sponsorLevel));

        ( (BaseActivity) getActivity() ).openActivityOrFragment(intent);

        getListView().setItemChecked(position, true);
    }

    public static class SponsorLevelsAdapter extends ArrayAdapter<String> {

        private Activity mActivity;

        public SponsorLevelsAdapter(Activity activity) {
            super(activity, R.layout.list_item_track, android.R.id.text1,
                activity.getResources()
                    .getStringArray(R.array.sponsor_level_names));
            mActivity = activity;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = super.getView(position, convertView, parent);

            if ( view.getTag() == null ) {
                ImageView iconView = (ImageView) view.findViewById(android.R.id.icon1);
                iconView.setVisibility(View.GONE);
                view.setTag(iconView);
            }

            return view;
        }

    }

}
