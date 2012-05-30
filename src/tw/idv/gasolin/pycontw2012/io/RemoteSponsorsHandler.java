package tw.idv.gasolin.pycontw2012.io;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentProviderOperation.Builder;
import android.content.res.Resources;

import tw.idv.gasolin.pycontw2012.provider.CoscupContract;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.SponsorLevels;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.Sponsors;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.SyncColumns;

public class RemoteSponsorsHandler extends JSONObjectHandler {

    public RemoteSponsorsHandler() {
        super(CoscupContract.CONTENT_AUTHORITY);
    }

    @Override
    public ArrayList<ContentProviderOperation> parse(JSONObject json,
        ContentResolver resolver, Resources res) throws JSONException,
        IOException {
        final long now = System.currentTimeMillis();
        final ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();

        final JSONArray levels = json.names();
        final int numLevels = levels == null ? 0 : levels.length();
        for ( int i = 0; i < numLevels; i++ ) {
            final String level = levels.getString(i);

            JSONArray jsons = json.getJSONArray(level);
            final int size = jsons.length();

            for ( int j = 0; j < size; j++ ) {
                JSONObject obj = jsons.getJSONObject(j);
                batch.add(parseSponsor(level, now, obj));
            }

        }

        // Delete outdated sponsors
        batch.add(ContentProviderOperation.newDelete(Sponsors.CONTENT_URI)
            .withSelection(SyncColumns.UPDATED + "!=?",
                new String[] { Long.toString(now) })
            .build());

        return batch;
    }

    private static ContentProviderOperation parseSponsor(String level,
        long syncTime, JSONObject json) throws JSONException, IOException {
        final Builder builder = ContentProviderOperation.newInsert(Sponsors.CONTENT_URI);

        final int levelIndex = SponsorLevels.MAP_LEVELS.indexOfValue(level);
        final int intLevel = SponsorLevels.MAP_LEVELS.keyAt(levelIndex);
        JSONObject names = json.getJSONObject(Tags.SPONSOR_NAME);
        final String name = names.optString(Tags.SPONSOR_LANGUAGE_EN);
        final String name_zh_tw = names.optString(Tags.SPONSOR_LANGUAGE_ZH_TW);
        final String name_zh_cn = names.optString(Tags.SPONSOR_LANGUAGE_ZH_CN);
        final String logoUrl = json.getString(Tags.SPONSOR_LOGO_URL);
        final String url = json.getString(Tags.SPONSOR_URL);
        JSONObject descriptions = json.getJSONObject(Tags.SPONSOR_DESCRIPTION);
        final String desc = descriptions.optString(Tags.SPONSOR_LANGUAGE_EN);
        final String desc_zh_tw = descriptions.optString(Tags.SPONSOR_LANGUAGE_ZH_TW);
        final String desc_zh_cn = descriptions.optString(Tags.SPONSOR_LANGUAGE_ZH_CN);

        builder.withValue(SyncColumns.UPDATED, syncTime);
        builder.withValue(Sponsors.SPONSOR_ID, Sponsors.generateSponsorId(name));
        builder.withValue(Sponsors.SPONSOR_LEVEL, intLevel);
        builder.withValue(Sponsors.SPONSOR_NAME, name);
        builder.withValue(Sponsors.SPONSOR_NAME_ZH_TW, name_zh_tw);
        builder.withValue(Sponsors.SPONSOR_NAME_ZH_CN, name_zh_cn);
        builder.withValue(Sponsors.SPONSOR_LOGO_URL, logoUrl);
        builder.withValue(Sponsors.SPONSOR_URL, url);
        builder.withValue(Sponsors.SPONSOR_DESC, desc);
        builder.withValue(Sponsors.SPONSOR_DESC_ZH_TW, desc_zh_tw);
        builder.withValue(Sponsors.SPONSOR_DESC_ZH_CN, desc_zh_cn);

        return builder.build();
    }

    /** Tags coming from remote JSON. */
    private interface Tags {
        String SPONSOR_NAME = "name";
        String SPONSOR_LOGO_URL = "logoUrl";
        String SPONSOR_URL = "url";
        String SPONSOR_DESCRIPTION = "desc";
        String SPONSOR_LANGUAGE_EN = "en";
        String SPONSOR_LANGUAGE_ZH_TW = "zh-tw";
        String SPONSOR_LANGUAGE_ZH_CN = "zh-cn";
    }

}
