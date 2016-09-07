/* 
 * This file is part of OppiaMobile - https://digital-campus.org/
 * 
 * OppiaMobile is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * OppiaMobile is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with OppiaMobile. If not, see <http://www.gnu.org/licenses/>.
 */

package org.digitalcampus.oppia.activity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

import org.digitalcampus.mobile.learning.R;
import org.digitalcampus.oppia.adapter.DownloadMediaListAdapter;
import org.digitalcampus.oppia.listener.DownloadMediaListener;
import org.digitalcampus.oppia.listener.ListInnerBtnOnClickListener;
import org.digitalcampus.oppia.model.Course;
import org.digitalcampus.oppia.model.CourseMetaPage;
import org.digitalcampus.oppia.model.Media;
import org.digitalcampus.oppia.service.DownloadBroadcastReceiver;
import org.digitalcampus.oppia.service.DownloadService;
import org.digitalcampus.oppia.utils.ConnectionUtils;
import org.digitalcampus.oppia.utils.UIUtils;

import com.androidplot.pie.PieRenderer;
import com.splunk.mint.Mint;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class DownloadMediaActivity extends AppActivity implements DownloadMediaListener {

	public static final String TAG = DownloadMediaActivity.class.getSimpleName();

    private SharedPreferences prefs;
    private ArrayList<Media> missingMedia;
	private DownloadMediaListAdapter dmla;
    private DownloadBroadcastReceiver receiver;
    Button downloadViaPCBtn;
    private Button downloadAll;
    private TextView emptyState;

    public enum DownloadMode {INDIVIDUALLY, DOWNLOAD_ALL, STOP_ALL};
	
	@SuppressWarnings("unchecked")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_download_media);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

		Bundle bundle = this.getIntent().getExtras();
		if (bundle != null) {
			missingMedia = (ArrayList<Media>) bundle.getSerializable(DownloadMediaActivity.TAG);
		}
        else{
            missingMedia = new ArrayList<Media>();
        }

		dmla = new DownloadMediaListAdapter(this, missingMedia);
        dmla.setOnClickListener(new DownloadMediaListener());

        ListView listView = (ListView) findViewById(R.id.missing_media_list);
		listView.setAdapter(dmla);

        downloadAll = (Button) this.findViewById(R.id.download_all);
        downloadAll.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                DownloadMode mode = downloadAll.getText().equals("Download All") ? DownloadMode.DOWNLOAD_ALL : DownloadMode.STOP_ALL;
                downloadAll.setText(downloadAll.getText().equals("Download All") ? "Stop All" : "Download All");
                for(int i = 0; i < missingMedia.size(); i++){

                    Media mediaToDownload = missingMedia.get(i);
                    downloadMedia(mediaToDownload, mode);
                }
            }
        });

		
		downloadViaPCBtn = (Button) this.findViewById(R.id.download_media_via_pc_btn);
		downloadViaPCBtn.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                downloadViaPC();
            }
        });

		Editor e = prefs.edit();
		e.putLong(PrefsActivity.PREF_LAST_MEDIA_SCAN, 0);
		e.commit();

        emptyState = (TextView) findViewById(R.id.empty_state);
	}
	
	@Override
	public void onResume(){
		super.onResume();
        if ((missingMedia != null) && missingMedia.size()>0) {
            //We already have loaded media (coming from orientationchange)
            dmla.sortByFilename();
            dmla.notifyDataSetChanged();
            emptyState.setVisibility(View.GONE);
            downloadViaPCBtn.setVisibility(View.VISIBLE);
        }else{
            emptyState.setVisibility(View.VISIBLE);
            downloadViaPCBtn.setVisibility(View.GONE);
        }
        receiver = new DownloadBroadcastReceiver();
        receiver.setMediaListener(this);
        IntentFilter broadcastFilter = new IntentFilter(DownloadService.BROADCAST_ACTION);
        broadcastFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        registerReceiver(receiver, broadcastFilter);

        invalidateOptionsMenu();
	}

    @Override
    public void onPause(){
        super.onPause();
        unregisterReceiver(receiver);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        ArrayList<Media> savedMissingMedia = (ArrayList<Media>) savedInstanceState.getSerializable(TAG);
        this.missingMedia.clear();
        this.missingMedia.addAll(savedMissingMedia);
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putSerializable(TAG, missingMedia);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.missing_media_sortby, menu);
        MenuItem sortBy = menu.findItem(R.id.sort_by);
        if(sortBy != null) {
            sortBy.setVisible(missingMedia.size() != 0);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int itemId = item.getItemId();
        switch(itemId){
            case R.id.menuSortCourseTitle:  dmla.sortByCourse(); return true;
            case R.id.menuSortMediaTitle: dmla.sortByFilename(); return true;
            case android.R.id.home: onBackPressed(); return true;
            default: return super.onOptionsItemSelected(item);
        }
    }

	private void downloadViaPC(){
		String filename = "oppia-media.html";
		String strData = "<html>";
		strData += "<head><title>"+this.getString(R.string.download_via_pc_title)+"</title></head>";
		strData += "<body>";
		strData += "<h3>"+this.getString(R.string.download_via_pc_title)+"</h3>";
		strData += "<p>"+this.getString(R.string.download_via_pc_intro)+"</p>";
		strData += "<ul>";
		for(Object o: missingMedia){
			Media m = (Media) o;
			strData += "<li><a href='"+m.getDownloadUrl()+"'>"+m.getFilename()+"</a></li>";
		}
		strData += "</ul>";
		strData += "</body></html>";
		strData += "<p>"+this.getString(R.string.download_via_pc_final,"/digitalcampus/media/")+"</p>";
		
		File file = new File(Environment.getExternalStorageDirectory(),filename);
		try {
			FileOutputStream f = new FileOutputStream(file);
			Writer out = new OutputStreamWriter(new FileOutputStream(file));
			out.write(strData);
			out.close();
			f.close();
			UIUtils.showAlert(this, R.string.info, this.getString(R.string.download_via_pc_message,filename));
		} catch (FileNotFoundException e) {
			Mint.logException(e);
			e.printStackTrace();
		} catch (IOException e) {
			Mint.logException(e);
			e.printStackTrace();
		}
	}

    // Override
    public void onDownloadProgress(String fileUrl, int progress) {
        Media mediaFile = findMedia(fileUrl);
        if (mediaFile != null){
            mediaFile.setProgress(progress);
            dmla.notifyDataSetChanged();
        }
    }

    // Override
    public void onDownloadFailed(String fileUrl, String message) {
        Media mediaFile = findMedia(fileUrl);
        if (mediaFile != null){
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            mediaFile.setDownloading(false);
            mediaFile.setProgress(0);
            dmla.notifyDataSetChanged();
        }
    }

    // Override
    public void onDownloadComplete(String fileUrl) {
        Media mediaFile = findMedia(fileUrl);
        if (mediaFile != null){
            Toast.makeText(this,  this.getString(R.string.download_complete), Toast.LENGTH_LONG).show();
            missingMedia.remove(mediaFile);
            dmla.notifyDataSetChanged();
            emptyState.setVisibility((missingMedia.size()==0) ? View.VISIBLE : View.GONE);
            downloadViaPCBtn.setVisibility((missingMedia.size()==0) ? View.GONE : View.VISIBLE);
            invalidateOptionsMenu();
        }
    }

    private Media findMedia(String fileUrl){
        if ( missingMedia.size()>0){
            for (Media mediaFile : missingMedia){
                if (mediaFile.getDownloadUrl().equals(fileUrl)){
                    return mediaFile;
                }
            }
        }
        return null;
    }

    private void downloadMedia(Media mediaToDownload, DownloadMode mode){
        if(!ConnectionUtils.isOnWifi(DownloadMediaActivity.this) && !DownloadMediaActivity.this.prefs.getBoolean(PrefsActivity.PREF_BACKGROUND_DATA_CONNECT, false)){
            UIUtils.showAlert(DownloadMediaActivity.this, R.string.warning, R.string.warning_wifi_required);
            return;
        }

        if(!mediaToDownload.isDownloading()){
            if(mode.equals(DownloadMode.DOWNLOAD_ALL) ||
                    mode.equals(DownloadMode.INDIVIDUALLY)) {
                startDownload(mediaToDownload);
            }
        }else{
            if(mode.equals(DownloadMode.STOP_ALL) ||
                mode.equals(DownloadMode.INDIVIDUALLY)) {
                 stopDownload(mediaToDownload);
            }
        }


    }

    private void startDownload(Media mediaToDownload){
        Intent mServiceIntent = new Intent(DownloadMediaActivity.this, DownloadService.class);
        mServiceIntent.putExtra(DownloadService.SERVICE_ACTION, DownloadService.ACTION_DOWNLOAD);
        mServiceIntent.putExtra(DownloadService.SERVICE_URL, mediaToDownload.getDownloadUrl());
        mServiceIntent.putExtra(DownloadService.SERVICE_DIGEST, mediaToDownload.getDigest());
        mServiceIntent.putExtra(DownloadService.SERVICE_FILENAME, mediaToDownload.getFilename());
        DownloadMediaActivity.this.startService(mServiceIntent);

        mediaToDownload.setDownloading(true);
        mediaToDownload.setProgress(0);
        dmla.notifyDataSetChanged();
    }
    private void stopDownload(Media mediaToDownload){
        Intent mServiceIntent = new Intent(DownloadMediaActivity.this, DownloadService.class);
        mServiceIntent.putExtra(DownloadService.SERVICE_ACTION, DownloadService.ACTION_CANCEL);
        mServiceIntent.putExtra(DownloadService.SERVICE_URL, mediaToDownload.getDownloadUrl());
        DownloadMediaActivity.this.startService(mServiceIntent);

        mediaToDownload.setDownloading(false);
        mediaToDownload.setProgress(0);
        dmla.notifyDataSetChanged();
    }
    private class DownloadMediaListener implements ListInnerBtnOnClickListener {
    	
    	public final String TAG = DownloadMediaListener.class.getSimpleName();
    	
        //@Override
        public void onClick(int position) {

            Log.d(TAG, "Clicked " + position);
            Media mediaToDownload = missingMedia.get(position);

        	downloadMedia(mediaToDownload, DownloadMode.INDIVIDUALLY);

        }


    }

}
