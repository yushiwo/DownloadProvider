package com.mozillaonline.downloadprovider;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.format.Formatter;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.mozillaonline.providers.DownloadManager;
import com.mozillaonline.providers.DownloadManager.Request;
import com.mozillaonline.providers.downloads.DownloadService;
import com.mozillaonline.providers.downloads.Downloads;
import com.mozillaonline.providers.downloads.ui.DownloadList;

public class DownloadProviderActivity extends Activity implements
		OnClickListener {
	@SuppressWarnings("unused")
	private static final String TAG = DownloadProviderActivity.class.getName();

	private BroadcastReceiver mReceiver;

	EditText mUrlInputEditText;
	Button mStartDownloadButton;
	DownloadManager mDownloadManager;
	Button mShowDownloadListButton;
	
	Button mSingleDownloadBtn;
	ProgressBar mProgressBar;
	private DownloadChangeObserver downloadObserver;  

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		mDownloadManager = new DownloadManager(getContentResolver(), getPackageName());
		buildComponents();
		startDownloadService();

		mReceiver = new BroadcastReceiver() {

			@Override
			public void onReceive(Context context, Intent intent) {
				showDownloadList();
			}
		};

		registerReceiver(mReceiver, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
		downloadObserver = new DownloadChangeObserver(null);      
        getContentResolver().registerContentObserver(Downloads.CONTENT_URI, true, downloadObserver); 
	}
	
	
	
    class DownloadChangeObserver extends ContentObserver {  
    	  
        public DownloadChangeObserver(Handler handler) {  
            super(handler);  
        }  
  
        @Override  
        public void onChange(boolean selfChange) {  
              queryDownloadStatus(downloadId);     
        }  
  
  
    } 

	@Override
	protected void onDestroy() {
		unregisterReceiver(mReceiver);
		super.onDestroy();
	}

	private void buildComponents() {
		mUrlInputEditText = (EditText) findViewById(R.id.url_input_edittext);
		mStartDownloadButton = (Button) findViewById(R.id.start_download_button);
		mShowDownloadListButton = (Button) findViewById(R.id.show_download_list_button);
		mSingleDownloadBtn = (Button) findViewById(R.id.download_single);
		mProgressBar = (ProgressBar) findViewById(R.id.download_progress);

		mStartDownloadButton.setOnClickListener(this);
		mShowDownloadListButton.setOnClickListener(this);
		mSingleDownloadBtn.setOnClickListener(this);

		mUrlInputEditText
				.setText("http://v.cctv.com/flash/mp4video28/TMS/2013/05/06/265114d5f2e641278098503f1676d017_h264418000nero_aac32-1.mp4");
		
		
	}

	private void startDownloadService() {
		Intent intent = new Intent();
		intent.setClass(this, DownloadService.class);
		startService(intent);
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		switch (id) {
		case R.id.start_download_button:
			startDownload();
			break;
		case R.id.show_download_list_button:
			showDownloadList();
			break;
		case R.id.download_single:
			startDownload();
			break;
		default:
			break;
		}
	}

	private void showDownloadList() {
		Intent intent = new Intent();
		intent.setClass(this, DownloadList.class);
		startActivity(intent);
	}

	private void startDownload() {
		String url = mUrlInputEditText.getText().toString();
		Uri srcUri = Uri.parse(url);
		DownloadManager.Request request = new Request(srcUri);
		request.setDestinationInExternalPublicDir(
				Environment.DIRECTORY_DOWNLOADS, "/");
		request.setDescription("Just for test");
		request.setTitle("天线宝宝");
//		request.setShowRunningNotification(false);
		downloadId = mDownloadManager.enqueue(request);
	}
	
	long downloadId;
	private void queryDownloadStatus(long lastDownloadId) {     
        DownloadManager.Query query = new DownloadManager.Query();     
        query.setFilterById(lastDownloadId);     
        Cursor c = mDownloadManager.query(query);     
        if(c!=null&&c.moveToFirst()) {     
            int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));     
              
            int reasonIdx = c.getColumnIndex(DownloadManager.COLUMN_REASON);    
            int titleIdx = c.getColumnIndex(DownloadManager.COLUMN_TITLE);    
            int fileSizeIdx =     
              c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);        
            int bytesDLIdx =     
              c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);    
            String title = c.getString(titleIdx);    
            int fileSize = c.getInt(fileSizeIdx);    
            int bytesDL = c.getInt(bytesDLIdx);    
              
            // Translate the pause reason to friendly text.    
            int reason = c.getInt(reasonIdx);    
            StringBuilder sb = new StringBuilder();    
            sb.append(title).append("\n");   
            sb.append("Downloaded ").append(bytesDL).append(" / " ).append(fileSize);    
            Log.d("tag", fileSizeIdx + ":" + bytesDLIdx);    
            int progress = getProgressValue(fileSize, bytesDL);
            Log.d("tag", "progress = " + progress);  
            mProgressBar.setProgress(progress);
              
            // Display the status     
            Log.d("tag", sb.toString());    
            switch(status) {     
            case DownloadManager.STATUS_PAUSED:     
                Log.v("tag", "STATUS_PAUSED");    
            case DownloadManager.STATUS_PENDING:     
                Log.v("tag", "STATUS_PENDING");    
            case DownloadManager.STATUS_RUNNING:     
                //正在下载，不做任何事情    
                Log.v("tag", "STATUS_RUNNING");    
         
             
                break;     
            case DownloadManager.STATUS_SUCCESSFUL:     
                //完成    
                Log.v("tag", "下载完成");    
//              dowanloadmanager.remove(lastDownloadId);     
                break;     
            case DownloadManager.STATUS_FAILED:     
                //清除已下载的内容，重新下载    
                Log.v("tag", "STATUS_FAILED");    
                mDownloadManager.remove(lastDownloadId);     
                break;     
            }     
        }    
    } 
	
	/**
	 * 获取下载进度百分比
	 * @param totalBytes
	 * @param currentBytes
	 * @return
	 */
	public int getProgressValue(long totalBytes, long currentBytes) {
		if (totalBytes == -1) {
			return 0;
		}
		return (int) (currentBytes * 100 / totalBytes);
	}

}