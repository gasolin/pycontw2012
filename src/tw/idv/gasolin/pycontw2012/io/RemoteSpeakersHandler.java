package tw.idv.gasolin.pycontw2012.io;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentProviderOperation.Builder;
import android.content.res.Resources;
import android.text.TextUtils;

import tw.idv.gasolin.pycontw2012.provider.CoscupProvider;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.Speakers;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.SyncColumns;
import tw.idv.gasolin.pycontw2012.util.ParserUtils;

public class RemoteSpeakersHandler extends JSONObjectHandler {

    public RemoteSpeakersHandler(String authority) {
        super(authority);
    }

    @Override
    public ArrayList<ContentProviderOperation> parse(JSONObject json,
        ContentResolver resolver, Resources res) throws JSONException,
        IOException {

        final ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
        final ContentProviderOperation operation = parseSpeaker(json);

        if ( operation != null ) {
            batch.add(operation);
        }

        return batch;
    }

    private static ContentProviderOperation parseSpeaker(JSONObject json)
        throws JSONException, IOException {

        if ( json.has(Tags.SESSION_SPEAKERS) ) {

            final Builder builder = ContentProviderOperation.newInsert(Speakers.CONTENT_URI);
            final String speakerName = json.getString(Tags.SESSION_SPEAKERS);
            final String speakerId = ParserUtils.sanitizeId(speakerName, true);
            final String company = json.optString(Tags.SESSION_SPEAKER_COMPANY);
            final String speakerAbstract = json.optString(Tags.SESSION_SPEAKER_ABSTRACT);

            builder.withValue(SyncColumns.UPDATED, 0);
            builder.withValue(Speakers.SPEAKER_ID, speakerId);
            builder.withValue(Speakers.SPEAKER_NAME, speakerName);
            // builder.withValue(Speakers.SPEAKER_IMAGE_URL, "");
            if ( !TextUtils.isEmpty(company) ) {
                builder.withValue(Speakers.SPEAKER_COMPANY, company);
            }
            if ( !TextUtils.isEmpty(speakerAbstract) ) {
                builder.withValue(Speakers.SPEAKER_ABSTRACT, speakerAbstract);
            }
            // builder.withValue(Speakers.SPEAKER_URL, "");
            builder.withValue(CoscupProvider.EXTRA_UNNOTIFY, Boolean.TRUE);

            return builder.build();

        }

        return null;
    }

    public static String parseSpeakerId(JSONObject json) throws JSONException {
        final String speakerName = json.optString(Tags.SESSION_SPEAKERS);
        final String speakerId = ParserUtils.sanitizeId(speakerName, true);
        return speakerId;
    }

    /** Tags coming from remote JSON. */
    private interface Tags {
        String SESSION_SPEAKERS = "speaker";
        String SESSION_SPEAKER_COMPANY = "speakerTitle";
        String SESSION_SPEAKER_ABSTRACT = "bio";
    }

}
