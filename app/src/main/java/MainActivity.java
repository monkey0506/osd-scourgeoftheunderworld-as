package $AGS_GAME_PACKAGE$;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import com.bigbluecup.android.AgsEngine;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.storage.OnObbStateChangeListener;
import android.os.storage.StorageManager;
import android.util.Log;


public class MainActivity extends Activity {
    static final int OBB_FILE_VERSION = App.getContext().getResources().
            getInteger(R.integer.obbFileVersion);
    static final long OBB_FILE_SIZE = Long.parseLong(App.getContext().getResources().
            getString(R.string.obbFileSize)); // file size in bytes of expansion file
    private static final String OBB_KEY; // key used when generating expansion file
    private static final String PACKAGE_NAME = App.getContext().getPackageName();
    private static final String GAME_FILE_NAME = App.getContext().getResources()
            .getString(R.string.game_file_name);
    private static final String OBB_FILE_NAME = "main." + OBB_FILE_VERSION + "." + PACKAGE_NAME +
            ".obb";
    private static final boolean OBB_EMBEDDED = getObbEmbedded();
    private static final String OBB_FILE_EXTERNAL_PATH =
            Environment.getExternalStorageDirectory() + "/Android/obb/" + PACKAGE_NAME + "/" +
                    OBB_FILE_NAME;
    private static final int DOWNLOAD_REQUEST = 1;
    private OnObbStateChangeListener expansionListener;

    static
    {
        String obbKey = App.getContext().getResources().getString(R.string.obbKey);
        OBB_KEY = obbKey.matches("@null") ? null : obbKey;
    }

    private static boolean getObbEmbedded() {
        try {
            return Arrays.asList(App.getContext().getResources().getAssets().list(""))
                    .contains(OBB_FILE_NAME);
        }
        catch (IOException e) {
            Log.d("INIT", "IOException occurred during initialization: " + e.getMessage());
            return false;
        }
    }

    // once the expansion file is mounted, this starts the activity that launches the game
    private void startGame(String fileName) {
        Intent intent = new Intent(this, AgsEngine.class);
        Bundle b = new Bundle();
        b.putString("filename", fileName); // full path to game data
        b.putString("directory", getApplicationInfo().dataDir); // writable location (saves, etc.)
        b.putBoolean("loadLastSave", false); // TODO: auto-load last save?
        intent.putExtras(b);
        startActivity(intent);
        finish(); // TODO: do something other than just exit the app when the game exits?
    }

    // checks if the expansion file exists (must have matching size in bytes)
    // TODO: add a CRC check on expansion file to check for corruption
    private boolean obbFileExists() {
        File obbFile = new File(OBB_FILE_EXTERNAL_PATH);
        return ((obbFile.isFile()) && (obbFile.length() == OBB_FILE_SIZE));
    }

    // ensures that the folders where the expansion file is copied/downloaded exist
    // also creates an empty file for the expansion file to be copied over to help
    // ensure it is created without exceptions being thrown
    private void ensureObbExternalPathExists() {
        File obbFile = new File(OBB_FILE_EXTERNAL_PATH);
        obbFile.getParentFile().mkdirs();
        try {
            obbFile.createNewFile();
        }
        catch (IOException e) {
        }
    }

    // copies the embedded expansion file to external storage
    // while not ideal, this is necessary for the expansion file to be mounted
    // you most likely want to upload the expansion file separately and use
    // the downloader interface instead
    private void copyEmbeddedObbToExternalStorage() {
        if ((!OBB_EMBEDDED) || (obbFileExists())) {
            return;
        }
        final int BUFFER_SIZE = 102400; // update as needed, your mileage may vary
        InputStream is = null;
        OutputStream os = null;
        try {
            is = getResources().getAssets().open(OBB_FILE_NAME);
            ensureObbExternalPathExists();
            os = new FileOutputStream(OBB_FILE_EXTERNAL_PATH);
            byte[] buffer = new byte[BUFFER_SIZE];
            for (int len; (len = is.read(buffer)) != -1; ) {
                os.write(buffer, 0, len);
            }
        }
        catch (FileNotFoundException e) {
            Log.d("OBB_COPY", "File not found exception occurred copying expansion file: " + e.getMessage());
            finish();
        }
        catch (IOException e) {
            Log.d("OBB_COPY", "IOException occurred copying expansion file: " + e.getMessage());
            finish();
        }
        finally {
            try {
                if (is != null) {
                    is.close(); // FFS, Java, I'm CLOSING a stream... why can this throw an exception???
                }
            }
            catch (IOException e) {
                Log.d("OBB_COPY", "Exception occurred closing input file: " + e.getMessage());
            }
            try {
                if (os != null) {
                    os.close();
                }
            }
            catch (IOException e) {
                Log.d("OBB_COPY", "Exception occurred closing output file: " + e.getMessage());
            }
        }
    }

    // Requests to download the expansion file from Google Play
    private void downloadExpansionFile() {
        if (OBB_EMBEDDED) {
            return;
        }
        if (obbFileExists()) {
            mountExpansionAndStartGame(OBB_KEY);
            return;
        }
        Intent intent = new Intent(this, ExpansionDownloaderActivity.class);
        startActivityForResult(intent, DOWNLOAD_REQUEST);
    }

    // helper to copy or download the expansion file
    // the expansion file will be mounted when finished, and the game will start
    private void copyOrDownloadExpansionFile() {
        if (OBB_EMBEDDED) {
            copyEmbeddedObbToExternalStorage();
            mountExpansionAndStartGame(OBB_KEY);
        }
        else {
            downloadExpansionFile();
            // expansion mounted when download finishes (async)
        }
    }

    // mounts the expansion file and starts the game (async)
    private void mountExpansionAndStartGame(String key) {
        final StorageManager storageManager =
                (StorageManager)getApplicationContext().getSystemService(Context.STORAGE_SERVICE);
        String filePath = OBB_FILE_EXTERNAL_PATH;
        final File mainFile = new File(filePath);
        if (!storageManager.isObbMounted(mainFile.getAbsolutePath())) {
            if (mainFile.exists()) {
                if (storageManager.mountObb(mainFile.getAbsolutePath(), key, expansionListener)) {
                    Log.d("STORAGE_MNT", "SUCCESSFULLY QUEUED");
                } else {
                    Log.d("STORAGE_MNT", "FAILED");
                }
            } else {
                Log.d("STORAGE", "Expansion file " + filePath + " not found!");
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setContentView(R.layout.activity_main);
        expansionListener = new OnObbStateChangeListener() {
            @Override
            public void onObbStateChange(String path, int state) {
                super.onObbStateChange(path, state);
                final StorageManager storageManager =
                        (StorageManager) getApplicationContext()
                                .getSystemService(Context.STORAGE_SERVICE);
                Log.d("PATH = ", path);
                Log.d("STATE = ", state + "");
                if (state == OnObbStateChangeListener.MOUNTED) {
                    String mountedPath = storageManager.getMountedObbPath(path);
                    Log.d("STORAGE", "-->MOUNTED");
                    startGame(new File(mountedPath, GAME_FILE_NAME).getPath());
                } else {
                    Log.d("##", "Path: " + path + "; state: " + state);
                }
            }
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        copyOrDownloadExpansionFile();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Check which request we're responding to
        if (requestCode == DOWNLOAD_REQUEST) {
            if (resultCode == RESULT_OK) {
                // Download was successful
                mountExpansionAndStartGame(OBB_KEY);
            }
            else {
                // The download failed! Try again... we can't do anything it!
                downloadExpansionFile();
            }
        }
    }
}
