package com.automatic.sample;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.databinding.ObservableBoolean;
import android.databinding.ObservableList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import com.automatic.android.sdk.Automatic;
import com.automatic.android.sdk.SdkError;
import com.automatic.android.sdk.ServiceBindingCallback;
import com.automatic.sample.databinding.ActivityServiceBindingBinding;
import java.util.concurrent.TimeUnit;

public class ServiceBindingActivity extends AppCompatActivity {

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private final ObservableBoolean mIsBound = new ObservableBoolean(false);
    private ArrayAdapter<String> mListAdapter;

    private ObservableList.OnListChangedCallback<ObservableList<String>> mCallback = new ObservableList.OnListChangedCallback<ObservableList<String>>() {
        @Override
        public void onChanged(ObservableList<String> sender) {
        }

        @Override
        public void onItemRangeChanged(ObservableList<String> sender, int positionStart, int itemCount) {
        }

        @Override
        public void onItemRangeInserted(ObservableList<String> sender, int positionStart, int itemCount) {
            mListAdapter.notifyDataSetChanged();
        }

        @Override
        public void onItemRangeMoved(ObservableList<String> sender, int fromPosition, int toPosition, int itemCount) {
        }

        @Override
        public void onItemRangeRemoved(ObservableList<String> sender, int positionStart, int itemCount) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityServiceBindingBinding binding = DataBindingUtil.setContentView(this, R.layout.activity_service_binding);

        binding.setIsBound(mIsBound);

        mListAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, AppClass.eventLog);
        binding.listView.setAdapter(mListAdapter);

        AppClass.eventLog.addOnListChangedCallback(mCallback);
        // start DriveStateService to log events
        startService(new Intent(this, DriveStateService.class));

        Automatic.get().bindService(new ServiceBindingCallback() {
            @Override
            public void onBindingResponse(boolean success, SdkError sdkError) {
                mIsBound.set(success);
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        AppClass.eventLog.removeOnListChangedCallback(mCallback);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_service_binding, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_bind) {
            Automatic.get().bindService();
            checkAndSetServiceBindingStatus();
            return true;
        } else if (id == R.id.action_unbind) {
            Automatic.get().unbindService();
            checkAndSetServiceBindingStatus();
            return true;
        } else if (id == R.id.action_query_drive_state) {
            try {
                Automatic.get().queryCurrentDriveState();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void checkAndSetServiceBindingStatus() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mIsBound.set(Automatic.get().isServiceAuthenticated());
            }
        }, TimeUnit.SECONDS.toMillis(5));
    }
}
