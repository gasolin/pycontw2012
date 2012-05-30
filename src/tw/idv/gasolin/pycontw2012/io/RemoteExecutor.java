package tw.idv.gasolin.pycontw2012.io;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.json.JSONTokener;

import android.content.ContentResolver;
import android.content.res.Resources;

import tw.idv.gasolin.pycontw2012.util.ParserUtils;

public class RemoteExecutor {
    private final HttpClient mHttpClient;
    private final ContentResolver mResolver;
    private final Resources mRes;

    public RemoteExecutor(HttpClient httpClient, ContentResolver resolver,
        Resources res) {
        mHttpClient = httpClient;
        mResolver = resolver;
        mRes = res;
    }

    public void executeGet(String url, JSONHandler handler)
        throws HandlerException {
        final HttpUriRequest request = new HttpGet(url);
        execute(request, handler);
    }

    public void execute(HttpUriRequest request, JSONHandler handler)
        throws HandlerException {
        try {
            final HttpResponse resp = mHttpClient.execute(request);
            final int status = resp.getStatusLine()
                .getStatusCode();
            if ( status != HttpStatus.SC_OK ) {
                throw new HandlerException("Unexpected server response "
                    + resp.getStatusLine() + " for " + request.getRequestLine());
            }

            final InputStream input = resp.getEntity()
                .getContent();
            try {
                final JSONTokener tokener = ParserUtils.newJsonTokener(input);
                handler.parseAndApply(tokener, mResolver, mRes);
            } catch ( IOException e ) {
                throw new HandlerException("Malformed response for "
                    + request.getRequestLine(), e);
            }
        } catch ( HandlerException e ) {
            throw e;
        } catch ( IOException e ) {
            throw new HandlerException("Problem reading remote response for "
                + request.getRequestLine(), e);
        }
    }
}
