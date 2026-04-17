package com.timers.widget;

import android.content.Context;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;

public final class DriveUploader {
    private static final String APP_NAME = "TimerWidget";
    private static final String BACKUP_FOLDER_NAME = "timers_backup";

    private DriveUploader() {
    }

    public static void uploadBackupJson(Context context, String accountEmail) throws Exception {
        String json = TimerData.exportBackupJson(context);
        Drive drive = buildDriveService(context, accountEmail);
        String folderId = ensureFolderExists(drive);

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String fileName = "timers_backup_" + timestamp + ".json";

        File metadata = new File();
        metadata.setName(fileName);
        metadata.setParents(Collections.singletonList(folderId));
        metadata.setMimeType("application/json");

        ByteArrayContent content = new ByteArrayContent(
                "application/json",
                json.getBytes(StandardCharsets.UTF_8)
        );

        drive.files().create(metadata, content).setFields("id,name").execute();
    }

    public static String downloadLatestBackupJson(Context context, String accountEmail) throws Exception {
        Drive drive = buildDriveService(context, accountEmail);
        String folderId = findFolderId(drive);
        if (folderId == null) {
            return null;
        }

        String query = "'" + folderId + "' in parents and trashed=false and mimeType='application/json'";
        FileList files = drive.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setOrderBy("createdTime desc")
                .setFields("files(id,name,createdTime)")
                .setPageSize(1)
                .execute();

        if (files.getFiles() == null || files.getFiles().isEmpty()) {
            return null;
        }

        String fileId = files.getFiles().get(0).getId();
        try (InputStream in = drive.files().get(fileId).executeMediaAsInputStream();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static Drive buildDriveService(Context context, String accountEmail) {
        GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                context,
                Collections.singleton(DriveScopes.DRIVE_FILE)
        );
        credential.setSelectedAccountName(accountEmail);

        HttpRequestInitializer requestInitializer = request -> {
            credential.initialize(request);
            request.setConnectTimeout(30_000);
            request.setReadTimeout(30_000);
        };

        return new Drive.Builder(
                AndroidHttp.newCompatibleTransport(),
                GsonFactory.getDefaultInstance(),
                requestInitializer
        )
                .setApplicationName(APP_NAME)
                .build();
    }

    private static String ensureFolderExists(Drive drive) throws Exception {
        String existingFolderId = findFolderId(drive);
        if (existingFolderId != null) {
            return existingFolderId;
        }

        File folderMetadata = new File();
        folderMetadata.setName(BACKUP_FOLDER_NAME);
        folderMetadata.setMimeType("application/vnd.google-apps.folder");

        File folder = drive.files().create(folderMetadata).setFields("id").execute();
        return folder.getId();
    }

    private static String findFolderId(Drive drive) throws Exception {
        String query = "mimeType='application/vnd.google-apps.folder' and trashed=false and name='"
                + BACKUP_FOLDER_NAME + "'";

        FileList list = drive.files().list()
                .setQ(query)
                .setSpaces("drive")
                .setFields("files(id,name)")
                .setPageSize(1)
                .execute();

        if (list.getFiles() != null && !list.getFiles().isEmpty()) {
            return list.getFiles().get(0).getId();
        }

        return null;
    }
}
