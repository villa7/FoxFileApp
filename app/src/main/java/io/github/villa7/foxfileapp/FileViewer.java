package io.github.villa7.foxfileapp;

import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.NotificationCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

//import com.goebl.david.Webb;
import com.loopj.android.http.BinaryHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import org.apache.http.Header;

import java.io.File;
import java.io.FileOutputStream;

public class FileViewer extends Activity {

    //private Webb webb;
    private String phpsessid;
    private String user;
    private String fileHash;
    private String fileName;
    private String type;
    private ProgressBar progress;
    private String[] allowedTypes = {".*"};
    private Context context = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_viewer);

        //webb = Webb.create();

        progress = (ProgressBar) findViewById(R.id.load_progress);
        progress.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.primary), android.graphics.PorterDuff.Mode.SRC_IN);

        Intent intent = getIntent();
        phpsessid = intent.getStringExtra("phpsessid");
        user = intent.getStringExtra("username");
        fileHash = intent.getStringExtra("filehash");
        fileName = intent.getStringExtra("filename");
        type = intent.getStringExtra("filetype");
        F.nl("user:\t\t" + user + "\nsessid:\t" + phpsessid + "\nhash:\t" + fileHash + "\ntype:\t" + type);

        setTitle(fileName);
        getPreview(getQuery(), fileHash);

    }
    private String getQuery() {
        switch(type) {
            case "text":
            case "code":
                return "read_file";
            case "image":
            case "audio":
            case "video":
                return "preview";
            case "zip":
                return "zip";
            case "pdf":
                return "preview";
            default:
                F.nl("file query type not supported yet");
                return "";
        }
    }
    private void getPreview(String... params) {
        showSpinner();
        switch (type) {
            case "text":
            case "code":
                getText(params);
                break;
            case "image":
                getImagePreview(params);
                //toast("Is image");
                break;
            case "audio":
                toast("Is audio");
                break;
            case "video":
                toast("Is video");
                break;
            case "zip":
                toast("Is archive");
                break;
            case "pdf":
                toast("Is PDF");
                break;
            default:
                F.nl("file type not supported yet");
                toast("File type not supported yet");
        }
    }
    public void getText(String... params) {

        Object[] bla = Params.getParams(params);
        String page = (String) bla[0];
        F.nl("Page: " + page);
        F.nl("params:");
        F.pa(params);
        RequestParams param = (RequestParams) bla[1];

        FoxFileClient.post(page, param, new TextHttpResponseHandler() { /*JsonHttpResponseHandler*/
            @Override
            public void onSuccess(int statusCode, Header[] headers, String res) {
                F.nl("Result: " + res);
                TextView textPreview = (TextView) findViewById(R.id.text_preview);
                textPreview.setText(res);
                textPreview.setVisibility(View.VISIBLE);
                hideSpinner();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String res, Throwable error) {
                F.nl("failed");
            }
        });
    }
    public void getImagePreview(String... params) {

        Object[] bla = Params.getParams(params);
        String page = (String) bla[0];
        F.nl("Page: " + page);
        F.nl("params:");
        F.pa(params);
        RequestParams param = (RequestParams) bla[1];

        FoxFileClient.get(page, param, new BinaryHttpResponseHandler(allowedTypes) { /*JsonHttpResponseHandler*/
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] res) {
                F.nl("Result: " + new String(res));
                F.nl("res is: " + res.getClass().getName());
                ImageView imagePreview = (ImageView) findViewById(R.id.image_preview);
                Bitmap bmp = BitmapFactory.decodeByteArray(res, 0, res.length);
                imagePreview.setImageBitmap(bmp);
                imagePreview.setVisibility(View.VISIBLE);
                hideSpinner();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] res, Throwable error) {
                hideSpinner();
                F.nl("failed");
                toast("Failed to connect to server");
                error.printStackTrace();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_file_viewer, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.action_download) {
            download(findViewById(id));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    public void download(View v) {
        F.nl("Download " + fileName + " (" + fileHash + ")");
        Object[] bla = Params.getParams("download", fileHash, fileName);
        String page = (String) bla[0];
        RequestParams param = (RequestParams) bla[1];
        F.nl("Page: " + page);
        F.nl("params:");
        //F.pa(params);

        final NotificationManager notManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        final NotificationCompat.Builder notBuilder = new NotificationCompat.Builder(context);
        notBuilder.setContentTitle("FoxFile")
                .setContentText("Downloading " + fileName)
                .setSmallIcon(R.mipmap.ic_launcher); //change to actual icon sometime
        //final int notId;
        final int notId = (int) Math.random() * 100;

        final String directory_downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath();
        final Dialog clickMenu = new Dialog(context);
        clickMenu.requestWindowFeature(Window.FEATURE_NO_TITLE);
        clickMenu.getWindow().getAttributes().width = WindowManager.LayoutParams.MATCH_PARENT;

        clickMenu.setContentView(R.layout.modal_progress);
        final ProgressBar modal_progress = (ProgressBar) clickMenu.findViewById(R.id.modal_progressbar);
        modal_progress.getIndeterminateDrawable().setColorFilter(getResources().getColor(R.color.primary), PorterDuff.Mode.SRC_IN);
        modal_progress.getProgressDrawable().setColorFilter(getResources().getColor(R.color.primary), PorterDuff.Mode.SRC_IN);

        FoxFileClient.get(page, param, new BinaryHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] res) {

                String tgt = (directory_downloads + "/" + fileName);
                File tgtFile = new File(tgt);
                try {
                    FileOutputStream outputStream = new FileOutputStream(tgtFile, true);
                    outputStream.write(res);
                    outputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                intent.setData(Uri.fromFile(tgtFile));
                sendBroadcast(intent);

                notBuilder.setContentText("Download complete")
                        .setProgress(0, 0, false);
                notManager.notify(notId, notBuilder.build());

                clickMenu.dismiss();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] res, Throwable error) {
                //hideSpinner();
                clickMenu.dismiss();
                notBuilder.setContentText("Download Failed")
                        .setProgress(0, 0, false);
                notManager.notify(notId, notBuilder.build());
                toast("Failed to connect to server");
                F.nl("failed");
            }
            @Override
            public void onProgress(int bytesWritten, int totalSize) {
                if (modal_progress.getMax() != totalSize) modal_progress.setMax(totalSize);
                F.nl("Download progress: " + bytesWritten + " / " + totalSize + " (" + bytesWritten / totalSize * 100 + "%)");
                modal_progress.setProgress(bytesWritten);
                notBuilder.setProgress(totalSize, bytesWritten, false);
                notManager.notify(notId, notBuilder.build());
            }
        });
    }
    public void toast(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_SHORT).show();
    }
    public void showSpinner() {
        F.nl("Showing spinner");
        //setProgressBarIndeterminateVisibility(true);
        progress.setVisibility(View.VISIBLE);
    }
    public void hideSpinner() {
        F.nl("Hiding spinner");
        //setProgressBarIndeterminateVisibility(false);
        progress.setVisibility(View.GONE);
    }
}
