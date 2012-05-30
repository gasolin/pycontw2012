package tw.idv.gasolin.pycontw2012.io;

import java.io.IOException;
import java.io.InputStream;

import org.json.JSONTokener;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;

import tw.idv.gasolin.pycontw2012.util.ParserUtils;

public class LocalExecutor {

    private Resources mRes;
    private ContentResolver mResolver;

    public LocalExecutor(Resources res, ContentResolver resolver) {
        mRes = res;
        mResolver = resolver;
    }

    /*
     * JSON
     */

    public void execute(Context context, String assetName, JSONHandler handler)
        throws HandlerException {
        try {
            final InputStream input = context.getAssets()
                .open(assetName);
            execute(input, handler);
        } catch ( HandlerException e ) {
            throw e;
        } catch ( IOException e ) {
            throw new HandlerException("Problem parsing local asset: "
                + assetName, e);
        }
    }

    public void execute(int resId, JSONHandler handler) throws HandlerException {
        try {
            final InputStream input = mRes.openRawResource(resId);
            execute(input, handler);
        } catch ( HandlerException e ) {
            throw e;
        } catch ( IOException e ) {
            throw new HandlerException("Problem parsing local resource: "
                + resId, e);
        }
    }

    public void execute(InputStream inputStream, JSONHandler handler)
        throws HandlerException, IOException {
        final JSONTokener tokener = ParserUtils.newJsonTokener(inputStream);
        handler.parseAndApply(tokener, mResolver, mRes);
    }

    /*
     * XML
     */

    public void execute(Context context, String assetName, XmlHandler handler)
        throws HandlerException {
        try {
            final InputStream input = context.getAssets()
                .open(assetName);
            final XmlPullParser parser = ParserUtils.newPullParser(input);
            handler.parseAndApply(parser, mResolver);
        } catch ( HandlerException e ) {
            throw e;
        } catch ( XmlPullParserException e ) {
            throw new HandlerException("Problem parsing local asset: "
                + assetName, e);
        } catch ( IOException e ) {
            throw new HandlerException("Problem parsing local asset: "
                + assetName, e);
        }
    }

    public void execute(int resId, XmlHandler handler) throws HandlerException {
        final XmlResourceParser parser = mRes.getXml(resId);
        try {
            handler.parseAndApply(parser, mResolver);
        } finally {
            parser.close();
        }
    }

}
