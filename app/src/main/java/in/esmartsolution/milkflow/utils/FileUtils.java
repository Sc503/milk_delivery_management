package in.esmartsolution.milkflow.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import in.esmartsolution.milkflow.models.FileModel;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileUtils {

    public static FileModel getFileModelFromUri(Context context, Uri uri) {
        if (uri == null) return null;

        String name = null;
        long size = 0;
        String mimeType = context.getContentResolver().getType(uri);
        String extension = "";

        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (nameIndex != -1) {
                        name = cursor.getString(nameIndex);
                    }
                    if (sizeIndex != -1) {
                        size = cursor.getLong(sizeIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (name == null) {
            name = uri.getLastPathSegment();
        }
        if (name == null) {
            name = "unknown_file";
        }

        int lastDot = name.lastIndexOf('.');
        if (lastDot != -1) {
            extension = name.substring(lastDot + 1).toLowerCase();
        } else if (mimeType != null) {
            extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType);
            if (extension == null) extension = "";
            name = name + "." + extension;
        }

        return new FileModel(name, uri, size, extension, mimeType);
    }

    public static File getSaveDirectory() {
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File appDir = new File(downloadsDir, "WiFiDirectShare");
        if (!appDir.exists()) {
            boolean created = appDir.mkdirs();
        }
        return appDir;
    }

    public static File getUniqueFile(File dir, String filename) {
        File file = new File(dir, filename);
        if (!file.exists()) {
            return file;
        }

        String name = filename;
        String ext = "";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot != -1) {
            name = filename.substring(0, lastDot);
            ext = filename.substring(lastDot);
        }

        int count = 1;
        while (file.exists()) {
            file = new File(dir, name + "_" + count + ext);
            count++;
        }
        return file;
    }

    public static void copyStream(InputStream is, OutputStream os) throws IOException {
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            os.write(buffer, 0, bytesRead);
        }
        os.flush();
    }
}
