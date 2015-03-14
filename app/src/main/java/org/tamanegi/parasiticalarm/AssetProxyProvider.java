package org.tamanegi.parasiticalarm;

import java.io.FileNotFoundException;
import java.io.IOException;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

// content://org.tamanegi.parasiticalarm.assetproxy/<PACKAGE-NAME>/<ASSET-PATH>
public class AssetProxyProvider extends ContentProvider
{
    public static final String AUTHORITY =
        "org.tamanegi.parasiticalarm.assetproxy";

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
        String assetPath = path[2];

        PackageManager pm = getContext().getPackageManager();
        AssetManager am;
        try {
            am = pm.getResourcesForApplication(packageName).getAssets();
        }
        catch(PackageManager.NameNotFoundException e) {
            FileNotFoundException fnf =
                new FileNotFoundException("package not found");
            fnf.initCause(e);
            throw fnf;
        }

        try {
            return am.openFd(assetPath);
        }
        catch(IOException e) {
            FileNotFoundException fnf =
                new FileNotFoundException("openFd failed");
            fnf.initCause(e);
            throw fnf;
        }
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
