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

package tw.idv.gasolin.pycontw2012.io;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.res.Resources;

import tw.idv.gasolin.pycontw2012.R;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.Tracks;

public class LocalTracksHandler extends JSONArrayHandler {

    public LocalTracksHandler() {
        super(CoscupContract.CONTENT_AUTHORITY);
    }

    private static final int[] TRACK_COLORS = new int[] {
        R.color.track_color_0, R.color.track_color_1, R.color.track_color_2,
        R.color.track_color_3, R.color.track_color_4, R.color.track_color_5,
        R.color.track_color_6, R.color.track_color_7, R.color.track_color_8,
        R.color.track_color_9, R.color.track_color_10, R.color.track_color_11 };

    @Override
    public ArrayList<ContentProviderOperation> parse(JSONArray json,
        ContentResolver resolver, Resources res) throws JSONException,
        IOException {
        final ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
        batch.add(ContentProviderOperation.newDelete(Tracks.CONTENT_URI)
            .build());

        final int size = json.length();
        for ( int i = 0; i < size; i++ ) {
            String trackName = json.isNull(i) ? null : json.optString(i, null);
            if ( i == 0 && trackName == null ) {
                trackName = res.getString(R.string.general_sessions_title);
            }
            batch.add(parseTrack(i, trackName, res));
        }

        return batch;
    }

    private static ContentProviderOperation parseTrack(int id, String name,
        Resources res) throws IOException {
        final ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(Tracks.CONTENT_URI);

        builder.withValue(Tracks.TRACK_ID, Integer.toString(id));
        builder.withValue(Tracks.TRACK_NAME, name);
        final int color = TRACK_COLORS[id % TRACK_COLORS.length];
        builder.withValue(Tracks.TRACK_COLOR, res.getColor(color));
        builder.withValue(Tracks.TRACK_ABSTRACT, "");

        return builder.build();
    }

    interface Tags {
        String TRACK = "track";
        String NAME = "name";
        String COLOR = "color";
        String ABSTRACT = "abstract";
    }

}
