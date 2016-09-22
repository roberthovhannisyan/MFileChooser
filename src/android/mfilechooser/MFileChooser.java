package net.devneko.plugins;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.apache.cordova.file.FileUtils;
import org.json.JSONArray;
import org.json.JSONException;

import com.orleonsoft.android.simplefilechooser.Constants;
import com.orleonsoft.android.simplefilechooser.ui.FileChooserActivity;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.BaseColumns;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

public class MFileChooser extends CordovaPlugin implements ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "MFileChooser";
    private static final String ACTION_OPEN = "open";
    private static final int PICK_FILE_REQUEST = 1;
    CallbackContext callback;
    ArrayList<String> exts;
    Intent lastIntent = null;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        exts = new ArrayList<String>();

        int count = args.length();

        for (int i = 0; i < count; i++) {
            exts.add(args.getString(i).toLowerCase());
        }

        if (action.equals(ACTION_OPEN)) {
            chooseFile(callbackContext, exts);
            return true;
        }

        return false;
    }

    public void chooseFile(CallbackContext callbackContext, ArrayList<String> ext) {

        // type and title should be configurable
        Context context = this.cordova.getActivity().getApplicationContext();

        Intent intent = null;

        if (Build.VERSION.SDK_INT >= 19) {
            intent = new Intent(Intent.ACTION_OPEN_DOCUMENT, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            intent.putExtra("return-data", true);
        } else {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setClass(context, FileChooserActivity.class);
        }


        if (ext.size() > 0) {
            intent.putStringArrayListExtra(Constants.KEY_FILTER_FILES_EXTENSIONS, ext);
        }
        cordova.startActivityForResult(this, intent, PICK_FILE_REQUEST);

        PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
        pluginResult.setKeepCallback(true);
        callback = callbackContext;
        callbackContext.sendPluginResult(pluginResult);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == PICK_FILE_REQUEST && callback != null) {

            if (resultCode == Activity.RESULT_OK) {
                this.lastIntent = data;
                this.processLastIntent();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                // TODO NO_RESULT or error callback?
                PluginResult pluginResult = new PluginResult(PluginResult.Status.NO_RESULT);
                callback.sendPluginResult(pluginResult);

            } else {

                callback.error(resultCode);
            }
        }
    }

    private void processLastIntent() {
        if ( lastIntent == null ) {
            return;
        }
        if (Build.VERSION.SDK_INT >= 19) {
            ClipData clipData = lastIntent.getClipData();
            if (clipData != null && clipData.getItemCount() > 0) {
                processClipData(clipData);
            } else {
                try {
                    Uri originalUri = transformUri(lastIntent);
                    Uri imageUri = getImageUri(originalUri);
                    if (imageUri == null) {
                        callback.error("not support");
                    } else {
                        callback.success(imageUri.toString());
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    callback.error("not support");
                }
            }
        } else {
            String uri = lastIntent.getStringExtra(Constants.KEY_FILE_SELECTED);
            if (uri != null) {
                Log.w(TAG, uri.toString());
                callback.success(uri.toString());
            } else {
                callback.error("File uri was null");

            }
        }
    }

    private void processClipData(ClipData clipData) {
        for (int i = 0; i < clipData.getItemCount(); i++) {
            ClipData.Item item = clipData.getItemAt(i);
            Uri itemUri = getImageUri(item.getUri());
            if (itemUri == null) {
                callback.error("not support");
            } else {
                callback.success(itemUri.toString());
            }
        }
    }

    private Uri transformUri(Intent data) throws IOException {
        //return if (Build.VERSION.SDK_INT >= 19 ) {  // Android4.4 KITKAT Later
        //    val originalUri = data.getData()
        //    val takeFlags = data.getFlags() and (Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        //    getContentResolver().takePersistableUriPermission(originalUri, takeFlags)
        //    originalUri
        //} else {
        //    data.getData()
        //}
        // ES File Explorer
        Bundle bundle = data.getExtras();
        if (bundle != null && bundle.keySet().contains("data")) {
            Object obj = bundle.get("data");
            if (obj instanceof Bitmap) {
                File tmpFile = File.createTempFile("bmp", ".png");
                FileOutputStream ostr = new FileOutputStream(tmpFile);
                Bitmap bmp = (Bitmap) obj;
                bmp.compress(Bitmap.CompressFormat.PNG, 100, ostr);
                return Uri.fromFile(tmpFile);
            } else {
                return data.getData();
            }
        }

        return data.getData();
    }

    private Uri getImageUri(Uri originalUri) {
        String scheme = originalUri.getScheme();
        String host = originalUri.getHost();
        if (host.equals("com.google.android.apps.photos.contentprovider")) {
            String filePath = getImageUrlWithAuthority(this.cordova.getActivity(), originalUri);
            String selectedImagePath = getFileUri(Uri.parse(filePath));
            if ( selectedImagePath == null ) {
                return null;
            }
            return Uri.parse(selectedImagePath);
        } else if (Build.VERSION.SDK_INT >= 19 && originalUri.getHost().equals("com.android.providers.downloads.documents")) {
            String id = DocumentsContract.getDocumentId(originalUri);
            Uri docUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));
            String[] imageColumns = new String[]{MediaStore.MediaColumns.DATA};
            String imageOrderBy = null;

            Cursor imageCursor = this.cordova.getActivity().getContentResolver().query(docUri, imageColumns, null, null, imageOrderBy);

            String selectedImagePath = "";
            if (imageCursor.moveToFirst()) {
                selectedImagePath = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.MediaColumns.DATA));
            }
            Log.e("path", selectedImagePath);

            return Uri.parse("file://" + selectedImagePath);
        } else if (scheme.equals("file")) {
            return originalUri;
        } else {
            String selectedImagePath = getFileUri(originalUri);
            if (selectedImagePath != null) {
                return Uri.parse("file://" + selectedImagePath);
            }

            Activity activity = this.cordova.getActivity();
            String type = activity.getContentResolver().getType(originalUri);
            Cursor cursor = activity.getContentResolver().query(originalUri, null, null, null, null);
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            cursor.moveToFirst();
            String name = cursor.getString(nameIndex);
            InputStream istr = null;
            try {
                istr = activity.getContentResolver().openInputStream(originalUri);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                while (istr.read(buffer, 0, buffer.length) > 0) {
                    baos.write(buffer, 0, buffer.length);
                }
                File tmpFile = File.createTempFile(name, "");
                FileOutputStream outstr = new FileOutputStream(tmpFile);
                outstr.write(baos.toByteArray());
                outstr.flush();
                outstr.close();
                return Uri.fromFile(tmpFile);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    private String getFileUri(Uri originalUri) {
        String id = null;
        try {
            String lastpath = originalUri.getLastPathSegment();
            if (lastpath.indexOf(":") == -1) {
                id = lastpath;
            } else {
                id = lastpath.split(":")[1];
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        try {
            String[] imageColumns = new String[]{MediaStore.MediaColumns.DATA};
            String imageOrderBy = null;

            Uri uri = getUri();
            boolean hasPermission = true;
            Activity activity = this.cordova.getActivity();
            if (Build.VERSION.SDK_INT >= 23) {
                if (!this.cordova.hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    activity.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                    hasPermission = false;
                }
            }
            if ( hasPermission ) {
                Cursor imageCursor = activity.getContentResolver().query(uri, imageColumns, BaseColumns._ID + "=" + id, null, imageOrderBy);
                String selectedImagePath = null;
                if (imageCursor.moveToFirst()) {
                    selectedImagePath = imageCursor.getString(imageCursor.getColumnIndex(MediaStore.MediaColumns.DATA));
                }
                return selectedImagePath;
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    private Uri getUri() {
        String state = Environment.getExternalStorageState();
        if (!state.toLowerCase().equals(Environment.MEDIA_MOUNTED.toLowerCase())) {
            return MediaStore.Images.Media.INTERNAL_CONTENT_URI;
        } else {
            return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        }
    }

    public String getImageUrlWithAuthority(Context context, Uri uri) {
        InputStream istream = null;
        if (uri.getAuthority() != null) {
            try {
                istream = context.getContentResolver().openInputStream(uri);
                Bitmap bmp = BitmapFactory.decodeStream(istream);
                return writeToTempImageAndGetPathUri(context, bmp).toString();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } finally {
                try {
                    istream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public Uri writeToTempImageAndGetPathUri(Context inContext, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (permissions.length != 1 || grantResults.length != 1 || !Manifest.permission.READ_EXTERNAL_STORAGE.equals(permissions[0])) {
            throw new RuntimeException("Unexpected permission results " + Arrays.toString(permissions) + ", " + Arrays.toString(grantResults));
        }
        int result = grantResults[0];
        String action = null;
        switch (result) {
            case PackageManager.PERMISSION_DENIED:
                break;
            case PackageManager.PERMISSION_GRANTED:
                this.processLastIntent();
                break;
            default:
                throw new RuntimeException("Unexpected permission result int " + result);
        }
    }
}