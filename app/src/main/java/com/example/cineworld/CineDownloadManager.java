package com.example.cineworld;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.os.StatFs;

import java.io.File;

public class CineDownloadManager {

    private final Context context;
    private final DownloadManager manager;

    public CineDownloadManager(Context context) {
        this.context = context.getApplicationContext();
        manager = (DownloadManager)
                this.context.getSystemService(Context.DOWNLOAD_SERVICE);
    }

    public long getAvailableBytes() {
        File folder = getDownloadFolder();
        StatFs stat = new StatFs(folder.getAbsolutePath());
        return stat.getAvailableBytes();
    }

    public long getStorageLimitBytes() {
        return getAvailableBytes() * 80L / 100L;
    }

    public File getDownloadFolder() {
        File folder = context.getExternalFilesDir(
                Environment.DIRECTORY_MOVIES
        );

        if (folder == null) {
            folder = new File(context.getFilesDir(), "CineWorldMovies");
        }

        if (!folder.exists()) {
            folder.mkdirs();
        }

        return folder;
    }

    public long startDownload(String url, String fileName) {

        if (url == null || !url.startsWith("https://")
                && !url.startsWith("http://")) {
            return -1;
        }

        DownloadManager.Request request =
                new DownloadManager.Request(Uri.parse(url));

        request.setTitle(fileName);
        request.setDescription("Downloading in CineWorld");
        request.setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE
        );

        request.setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_MOVIES,
                fileName
        );

        return manager.enqueue(request);
    }

    public DownloadManager getManager() {
        return manager;
    }
}
