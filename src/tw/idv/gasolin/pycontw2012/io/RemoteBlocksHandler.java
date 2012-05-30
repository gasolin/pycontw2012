package tw.idv.gasolin.pycontw2012.io;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentProviderOperation.Builder;
import android.content.res.Resources;

import tw.idv.gasolin.pycontw2012.provider.CoscupProvider;
import tw.idv.gasolin.pycontw2012.provider.CoscupContract.Blocks;
import tw.idv.gasolin.pycontw2012.util.ParserUtils;

public class RemoteBlocksHandler extends JSONObjectHandler {

    public RemoteBlocksHandler(String authority) {
        super(authority);
    }

    @Override
    public ArrayList<ContentProviderOperation> parse(JSONObject json,
        ContentResolver resolver, Resources res) throws JSONException,
        IOException {

        final ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();
        batch.add(parseBlock(json));

        return batch;
    }

    private static ContentProviderOperation parseBlock(JSONObject json)
        throws JSONException, IOException {
        final Builder builder = ContentProviderOperation.newInsert(Blocks.CONTENT_URI);

        final boolean isBreak = json.getBoolean(Tags.SESSION_IS_BREAK);
        final long startTime = json.getLong(Tags.SESSION_TIME_START) * 1000;
        final long endTime = json.getLong(Tags.SESSION_TIME_END) * 1000;
        final String blockId = Blocks.generateBlockId(startTime, endTime);
        final int roomId = json.optInt(Tags.SESSION_ROOM,
            LocalRoomsHandler.INVALID_ROOM_ID);

        String blockType;
        String blockTitle = "";

        if ( isBreak ) {

            blockType = ParserUtils.BLOCK_TYPE_FOOD;
            blockTitle = json.getString(Tags.SESSION_TITLE);

        } else if ( roomId == 0 ) {

            blockType = ParserUtils.BLOCK_TYPE_SESSION;
            blockTitle = json.optString(Tags.SESSION_TITLE,
                ParserUtils.BLOCK_TITLE_BREAKOUT_SESSIONS);

        } else {

            blockType = ParserUtils.BLOCK_TYPE_SESSION;
            blockTitle = ParserUtils.BLOCK_TITLE_BREAKOUT_SESSIONS;

        }

        builder.withValue(Blocks.BLOCK_ID, blockId);
        builder.withValue(Blocks.BLOCK_TITLE, blockTitle);
        builder.withValue(Blocks.BLOCK_START, startTime);
        builder.withValue(Blocks.BLOCK_END, endTime);
        builder.withValue(Blocks.BLOCK_TYPE, blockType);
        builder.withValue(CoscupProvider.EXTRA_UNNOTIFY, Boolean.TRUE);

        return builder.build();
    }

    public static String parseBlockId(JSONObject json) throws JSONException {
        final long startTime = json.getLong(Tags.SESSION_TIME_START) * 1000;
        final long endTime = json.getLong(Tags.SESSION_TIME_END) * 1000;
        final String blockId = Blocks.generateBlockId(startTime, endTime);
        return blockId;
    }

    /** Tags coming from remote JSON. */
    private interface Tags {
        String SESSION_TIME_START = "from";
        String SESSION_TIME_END = "to";
        String SESSION_TRACK = "type";
        String SESSION_TITLE = "name";
        String SESSION_ABSTRACT = "abstract";
        String SESSION_SPEAKERS = "speaker";
        String SESSION_ROOM = "room";
        String SESSION_IS_BREAK = "isBreak";
    }

}
