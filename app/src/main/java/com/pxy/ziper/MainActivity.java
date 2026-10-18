
package com.pxy.ziper;

import android.app.*;
import android.os.*;
import android.content.*;
import android.net.Uri;
import android.view.*;
import android.widget.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.provider.DocumentsContract;

import java.util.*;

public class MainActivity extends Activity {

    private static final int PICK_FOLDER = 101;
    private LinearLayout root, toolbar, list;
    private Uri treeUri, currentUri;
    private TextView pathLabel;
    private final ArrayList<Uri> parents = new ArrayList<>();

    private int red = Color.rgb(255, 48, 79);
    private int bg = Color.rgb(12, 12, 16);
    private int panel = Color.rgb(25, 25, 33);

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(bg);
        getWindow().setNavigationBarColor(bg);
        getWindow().getDecorView().setSystemUiVisibility(0);
        showHome();
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(Color.WHITE);
        t.setTextSize(size);
        t.setPadding(14, 12, 14, 12);
        return t;
    }

    private Button button(String value, Runnable action) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextColor(Color.WHITE);
        b.setAllCaps(false);
        b.setBackground(shape(red, 18));
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private void setup(String title) {
        root = new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(16, 12, 16, 12);
        root.setBackgroundColor(bg);

        TextView brand = text("▤  PXY ZIPER", 23);
        brand.setTypeface(null, Typeface.BOLD);
        root.addView(brand);

        TextView sub = text(title, 12);
        sub.setTextColor(0xFFAAAAAF);
        root.addView(sub);

        toolbar = new LinearLayout(this);
        toolbar.setOrientation(1);
        root.addView(toolbar);

        ScrollView scroll = new ScrollView(this);
        list = new LinearLayout(this);
        list.setOrientation(1);
        scroll.addView(list);

        root.addView(scroll,
            new LinearLayout.LayoutParams(
                -1, 0, 1
            )
        );

        setContentView(root);
    }

    private void showHome() {
        setup("FILE MANAGER • ANDROID NATIVE");

        TextView welcome = text(
            "Kelola file dengan mudah.\n\n" +
            "Pilih folder penyimpanan untuk " +
            "mulai menjelajahi file.",
            16
        );
        welcome.setBackground(shape(panel, 24));
        root.addView(welcome);

        root.addView(button("BUKA PENYIMPANAN", () -> {
            Intent i = new Intent(
                Intent.ACTION_OPEN_DOCUMENT_TREE
            );
            i.addFlags(
                Intent.FLAG_GRANT_READ_URI_PERMISSION |
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION |
                Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
            );
            startActivityForResult(i, PICK_FOLDER);
        }));

        TextView info = text(
            "FITUR DASAR\n\n" +
            "• Jelajahi folder\n" +
            "• Lihat daftar file\n" +
            "• Buka subfolder\n" +
            "• Pilih folder melalui sistem Android\n\n" +
            "Logo PXY ZIPER",
            13
        );
        info.setTextColor(0xFFCCCCD4);
        root.addView(info);
    }

    @Override
    protected void onActivityResult(
        int requestCode, int resultCode, Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_FOLDER &&
            resultCode == RESULT_OK && data != null) {

            treeUri = data.getData();

            try {
                getContentResolver().takePersistableUriPermission(
                    treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION |
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                );
            } catch (Exception ignored) {}

            currentUri = DocumentsContract.buildDocumentUriUsingTree(
                treeUri,
                DocumentsContract.getTreeDocumentId(treeUri)
            );

            parents.clear();
            browse(currentUri);
        }
    }

    private void browse(Uri folder) {
        currentUri = folder;
        setup("PENYIMPANAN / FILE");

        root.addView(button("← KEMBALI", () -> {
            if (!parents.isEmpty()) {
                Uri previous = parents.remove(parents.size() - 1);
                browse(previous);
            } else {
                showHome();
            }
        }));

        TextView location = text(
            DocumentsContract.getDocumentId(folder), 11
        );
        location.setTextColor(0xFFFF8495);
        root.addView(location);

        root.addView(button("SESUARKAN / MUAT ULANG", () ->
            browse(currentUri)
        ));

        try {
            Uri childrenUri =
                DocumentsContract.buildChildDocumentsUriUsingTree(
                    treeUri,
                    DocumentsContract.getDocumentId(folder)
                );

            android.database.Cursor cursor =
                getContentResolver().query(
                    childrenUri,
                    new String[] {
                        DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                        DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                        DocumentsContract.Document.COLUMN_MIME_TYPE,
                        DocumentsContract.Document.COLUMN_SIZE
                    },
                    null, null,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                );

            if (cursor == null) {
                list.addView(text("Folder tidak dapat dibaca.", 14));
                return;
            }

            while (cursor.moveToNext()) {
                String id = cursor.getString(0);
                String name = cursor.getString(1);
                String mime = cursor.getString(2);
                long size = cursor.isNull(3) ? 0 : cursor.getLong(3);

                Uri itemUri =
                    DocumentsContract.buildDocumentUriUsingTree(
                        treeUri, id
                    );

                boolean isFolder =
                    DocumentsContract.Document.MIME_TYPE_DIR.equals(mime);

                LinearLayout row = new LinearLayout(this);
                row.setOrientation(0);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(5, 5, 5, 5);
                row.setBackground(shape(panel, 16));

                LinearLayout.LayoutParams rp =
                    new LinearLayout.LayoutParams(-1, -2);
                rp.setMargins(0, 4, 0, 4);

                TextView icon = text(isFolder ? "📁" : "📄", 22);
                row.addView(icon);

                LinearLayout info = new LinearLayout(this);
                info.setOrientation(1);

                TextView filename = text(name, 13);
                filename.setTypeface(null, Typeface.BOLD);
                info.addView(filename);

                TextView detail = text(
                    isFolder ? "Folder" : formatSize(size), 10
                );
                detail.setTextColor(0xFFAAAAAF);
                info.addView(detail);

                row.addView(info,
                    new LinearLayout.LayoutParams(0, -2, 1));

                row.setOnClickListener(v -> {
                    if (isFolder) {
                        parents.add(currentUri);
                        browse(itemUri);
                    } else {
                        openFile(itemUri, mime);
                    }
                });

                list.addView(row, rp);
            }

            cursor.close();

            if (list.getChildCount() == 0) {
                list.addView(text("Folder kosong.", 13));
            }

        } catch (Exception e) {
            list.addView(text(
                "Gagal membaca folder: " + e.getMessage(), 12
            ));
        }
    }

    private String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1048576)
            return String.format(Locale.US, "%.1f KB",
                bytes / 1024.0);
        return String.format(Locale.US, "%.2f MB",
            bytes / 1048576.0);
    }

    private void openFile(Uri uri, String mime) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(uri,
                mime == null ||
                mime.equals("application/octet-stream")
                    ? "*/*" : mime);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this,
                "Tidak ada aplikasi untuk membuka file ini.",
                Toast.LENGTH_SHORT).show();
        }
    }
} 
