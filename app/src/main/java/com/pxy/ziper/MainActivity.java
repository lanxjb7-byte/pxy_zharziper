
package com.pxy.ziper;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.database.Cursor;
import android.provider.DocumentsContract;
import android.view.View;
import android.widget.*;
import android.graphics.Color;

import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final int PICK_FOLDER = 101;

    private TextView storagePath, fileCount, emptyMessage;
    private Button btnChooseFolder;
    private ListView fileList;
    private ArrayAdapter<String> adapter;

    private Uri treeUri, currentUri;

    private final ArrayList<Uri> parents = new ArrayList<>();
    private final ArrayList<Uri> itemUris = new ArrayList<>();
    private final ArrayList<String> itemMimes = new ArrayList<>();
    private final ArrayList<Boolean> itemFolders = new ArrayList<>();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        getWindow().setStatusBarColor(Color.rgb(10, 10, 10));
        getWindow().setNavigationBarColor(Color.rgb(10, 10, 10));

        setContentView(R.layout.activity_main);

        storagePath = findViewById(R.id.storagePath);
        fileCount = findViewById(R.id.fileCount);
        emptyMessage = findViewById(R.id.emptyMessage);
        btnChooseFolder = findViewById(R.id.btnChooseFolder);
        fileList = findViewById(R.id.fileList);

        adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_list_item_1,
            new ArrayList<String>()
        );

        fileList.setAdapter(adapter);

        btnChooseFolder.setOnClickListener(v -> chooseFolder());

        fileList.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= itemUris.size()) return;

            Uri selected = itemUris.get(position);
            boolean folder = itemFolders.get(position);
            String mime = itemMimes.get(position);

            if (folder) {
                parents.add(currentUri);
                browse(selected);
            } else {
                openFile(selected, mime);
            }
        });

        showHome();
    }

    private void chooseFolder() {
        Intent intent = new Intent(
            Intent.ACTION_OPEN_DOCUMENT_TREE
        );

        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION |
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
            Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
        );

        startActivityForResult(intent, PICK_FOLDER);
    }

    private void showHome() {
        currentUri = null;
        parents.clear();
        treeUri = null;

        storagePath.setText("Pilih folder untuk mulai");
        fileCount.setText("0 ITEM");
        emptyMessage.setText("Belum ada folder yang dibuka");

        itemUris.clear();
        itemMimes.clear();
        itemFolders.clear();
        adapter.clear();

        btnChooseFolder.setText("BUKA PENYIMPANAN");
    }

    @Override
    protected void onActivityResult(
        int requestCode,
        int resultCode,
        Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FOLDER &&
            resultCode == RESULT_OK &&
            data != null) {

            treeUri = data.getData();

            try {
                getContentResolver().takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION |
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                );
            } catch (Exception ignored) {}

            currentUri = DocumentsContract
                .buildDocumentUriUsingTree(
                    treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri)
                );

            parents.clear();
            browse(currentUri);
        }
    }

    private void browse(Uri folder) {
        currentUri = folder;

        itemUris.clear();
        itemMimes.clear();
        itemFolders.clear();
        adapter.clear();

        storagePath.setText(
            DocumentsContract.getDocumentId(folder)
        );

        btnChooseFolder.setText("PILIH FOLDER LAIN");

        try {
            Uri childrenUri = DocumentsContract
                .buildChildDocumentsUriUsingTree(
                    treeUri,
                    DocumentsContract.getDocumentId(folder)
                );

            Cursor cursor = getContentResolver().query(
                childrenUri,
                new String[] {
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_SIZE
                },
                null,
                null,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            );

            if (cursor == null) {
                emptyMessage.setText("Folder tidak dapat dibaca");
                return;
            }

            while (cursor.moveToNext()) {
                String id = cursor.getString(0);
                String name = cursor.getString(1);
                String mime = cursor.getString(2);

                long size = cursor.isNull(3)
                    ? 0 : cursor.getLong(3);

                boolean isFolder =
                    DocumentsContract.Document.MIME_TYPE_DIR
                        .equals(mime);

                Uri itemUri = DocumentsContract
                    .buildDocumentUriUsingTree(treeUri, id);

                itemUris.add(itemUri);
                itemMimes.add(mime);
                itemFolders.add(isFolder);

                String icon = isFolder ? "[FOLDER] " : "[FILE] ";

                String detail = isFolder
                    ? "Folder"
                    : formatSize(size);

                adapter.add(
                    icon + name + "\n" + detail
                );
            }

            cursor.close();

            fileCount.setText(itemUris.size() + " ITEM");

            if (itemUris.isEmpty()) {
                emptyMessage.setText("Folder ini kosong");
                emptyMessage.setVisibility(View.VISIBLE);
            } else {
                emptyMessage.setVisibility(View.GONE);
            }

            adapter.notifyDataSetChanged();

        } catch (Exception e) {
            emptyMessage.setVisibility(View.VISIBLE);
            emptyMessage.setText("Gagal membaca folder");
            Toast.makeText(
                this,
                "Gagal membaca folder",
                Toast.LENGTH_SHORT
            ).show();
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";

        if (bytes < 1048576) {
            return String.format(
                Locale.US, "%.1f KB", bytes / 1024.0
            );
        }

        return String.format(
            Locale.US, "%.2f MB", bytes / 1048576.0
        );
    }

    private void openFile(Uri uri, String mime) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);

            intent.setDataAndType(
                uri,
                mime == null ? "*/*" : mime
            );

            intent.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            );

            startActivity(intent);

        } catch (Exception e) {
            Toast.makeText(
                this,
                "Tidak ada aplikasi untuk membuka file ini",
                Toast.LENGTH_SHORT
            ).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (!parents.isEmpty()) {
            Uri previous = parents.remove(parents.size() - 1);
            browse(previous);
        } else {
            showHome();
        }
    }
            }
