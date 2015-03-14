package org.tamanegi.parasiticalarm;

import java.io.FileNotFoundException;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

// content://org.tamanegi.parasiticalarm.resourceproxy/<PACKAGE-NAME>/<RESOURCE-PATH>
// note: v2.1 and older ImageView does not accept "android.resource://..." Uri
public class ResourceProxyProvider extends ContentProvider
{
    public static final String AUTHORITY =
        "org.tamanegi.parasiticalarm.resourceproxy";

    @Override
    public boolean onCreate()
    {
        return true;
    }

    @Override
    public String getType(Uri uri)
    {
        return "application/octet-stream";
    }

    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode)
        throws FileNotFoundException
    {
        String[] path = uri.getPath().split("/", 3);
        if(path.length != 3) {
            throw new FileNotFoundException(
                "insufficient path: " + uri.getPath());
        }

        String packageName = path[1];
        String resPath = path[2];
        Uri resUri = new Uri.Builder()
            .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
            .authority(packageName)
            .appendEncodedPath(resPath)
            .build();

        return getContext().getContentResolver()
            .openAssetFileDescriptor(resUri, mode);
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode)
        throws FileNotFoundException
    {
        return openAssetFile(uri, mode).getParcelFileDescriptor();
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder)
    {
        throw new UnsupportedOperationException(
            "query operation is not supported");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs)
    {
        throw new UnsupportedOperationException(
            "delete operation is not supported");
    }

    @Override
    public Uri insert(Uri uri, ContentValues values)
    {
        throw new UnsupportedOperationException(
            "insert operation is not supported");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
            String[] selectionArgs)
    {
        throw new UnsupportedOperationException(
            "update operation is not supported");
    }
}
