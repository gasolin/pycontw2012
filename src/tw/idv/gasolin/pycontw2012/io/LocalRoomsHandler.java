package tw.idv.gasolin.pycontw2012.io;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.res.Resources;

import tw.idv.gasolin.pycontw2012.provider.CoscupContract;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.Rooms;

public class LocalRoomsHandler extends JSONArrayHandler {

    public static final int INVALID_ROOM_ID = -1;

    public LocalRoomsHandler() {
        super(CoscupContract.CONTENT_AUTHORITY);
    }

    @Override
    public ArrayList<ContentProviderOperation> parse(JSONArray json,
        ContentResolver resolver, Resources res) throws JSONException,
        IOException {
        final ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();

        final int size = json.length();
        for ( int i = 0; i < size; i++ ) {
            JSONObject obj = json.getJSONObject(i);
            parseRoom(i, obj, batch, resolver);
        }

        return batch;
    }

    private static void parseRoom(int id, JSONObject json,
        ArrayList<ContentProviderOperation> batch, ContentResolver resolver)
        throws JSONException, IOException {
        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(Rooms.CONTENT_URI);
        
        //fix the problem of GAE can not show the classroom.
        if(!"".equals(json.optString(Tags.ID))){
        	builder.withValue(Rooms.ROOM_ID, json.optString(Tags.ID));
        } else {
        	builder.withValue(Rooms.ROOM_ID, Integer.toString(id));
        }
        
        builder.withValue(Rooms.ROOM_FLOOR, Integer.toString(3));
        builder.withValue(Rooms.ROOM_NAME, json.getString(Tags.EN));
        builder.withValue(Rooms.ROOM_NAME_ZH_TW, json.getString(Tags.ZH_TW));
        builder.withValue(Rooms.ROOM_NAME_ZH_CN, json.getString(Tags.ZH_CN));
        
        batch.add(builder.build());
    }

    private interface Tags {
    	String ID = "id";
        String EN = "en";
        String ZH_CN = "zh-cn";
        String ZH_TW = "zh-tw";
    }

}
