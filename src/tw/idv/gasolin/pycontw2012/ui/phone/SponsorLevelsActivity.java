package tw.idv.gasolin.pycontw2012.ui.phone;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import tw.idv.gasolin.pycontw2012.ui.BaseSinglePaneActivity;
import tw.idv.gasolin.pycontw2012.ui.SponsorLevelsFragment;

public class SponsorLevelsActivity extends BaseSinglePaneActivity {

    @Override
    protected Fragment onCreatePane() {
        return new SponsorLevelsFragment();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getActivityHelper().setupSubActivity();
    }

}
