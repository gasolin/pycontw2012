package tw.idv.gasolin.pycontw2012.io;

import java.io.IOException;
import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.OperationApplicationException;
import android.content.res.Resources;
import android.os.RemoteException;

public abstract class JSONObjectHandler extends JSONHandler {

    public JSONObjectHandler(String authority) {
        super(authority);
    }

    public void parseAndApply(JSONObject json, ContentResolver resolver,
        Resources res) throws HandlerException {
        try {

            final ArrayList<ContentProviderOperation> batch = parse(json,
                resolver, res);
            resolver.applyBatch(mAuthority, batch);

        } catch ( HandlerException e ) {
            throw e;
        } catch ( JSONException e ) {
            throw new HandlerException("Problem parsing JSON response", e);
        } catch ( IOException e ) {
            throw new HandlerException("Problem reading response", e);
        } catch ( RemoteException e ) {
            // Failed binder transactions aren't recoverable
            throw new RuntimeException("Problem applying batch operation", e);
        } catch ( OperationApplicationException e ) {
            // Failures like constraint violation aren't recoverable
            // TODO: write unit tests to exercise full provider
            // TODO: consider catching version checking asserts here, and then
            // wrapping around to retry parsing again.
            throw new RuntimeException("Problem applying batch operation", e);
        }
    }

    @Override
    public ArrayList<ContentProviderOperation> parse(JSONTokener tokener,
        ContentResolver resolver, Resources res) throws JSONException,
        IOException {
        return parse(new JSONObject(tokener), resolver, res);
    }

    public abstract ArrayList<ContentProviderOperation> parse(JSONObject json,
        ContentResolver resolver, Resources res) throws JSONException,
        IOException;

}
