/*
 * Copyright 2013 Dmitry Monakhov.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package monakhv.android.samlib;



import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.widget.*;
import monakhv.android.samlib.data.GoogleDiskOperation;
import monakhv.android.samlib.dialogs.SingleChoiceSelectDialog;

import android.accounts.AccountManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView.OnItemClickListener;
import monakhv.android.samlib.service.MessageConstructor;
import monakhv.samlib.service.GuiUpdateObject;

import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import java.util.ArrayList;

/**
 *
 * @author monakhv
 */
public class ArchiveActivity extends MyBaseAbstractActivity {

    public static final String UPDATE_KEY = "UPDATE_LIST_PARAM";
    public static final int UPDATE_LIST = 22;
    private static final String DEBUG_TAG = "ArchiveActivity";
    private SingleChoiceSelectDialog dialog = null;
    private String selectedFile;
    private CheckBox cb;
    private Subscription mResultSubscription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.archive);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        final ActionBar actionBar=getSupportActionBar();
        if (actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
        }


        cb = (CheckBox) findViewById(R.id.cbGoogleAuto);
        cb.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Log.d(DEBUG_TAG, "set Google Auto to: " + isChecked);
            getSettingsHelper().setGoogleAuto(isChecked);
        });
        cb.setChecked(getSettingsHelper().isGoogleAuto());
        cb.setEnabled(getSettingsHelper().isGoogleAutoEnable());

    }

    private Dialog createImportAlert( DialogInterface.OnClickListener listener,String filename) {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        adb.setTitle(R.string.Attention);

        String msg = getString(R.string.alert_import);
        msg = msg.replaceAll("__", filename);

        adb.setMessage(msg);
        adb.setIcon(android.R.drawable.ic_dialog_alert);
        adb.setPositiveButton(R.string.Yes, listener);
        adb.setNegativeButton(R.string.No, listener);
        return adb.create();

    }
    private final DialogInterface.OnClickListener importDBListener = new OnClickListener() {
        public void onClick(DialogInterface dialog, int which) {
            switch (which) {
                case Dialog.BUTTON_POSITIVE:
                    _importDB(selectedFile);
                    break;
                case Dialog.BUTTON_NEGATIVE:
                    break;

            }

        }
    };

    @SuppressWarnings("UnusedParameters")
    public void exportDB(View v) {
        String file = getDataExportImport().exportDB();

        String text;
        if (file != null) {
            text = getString(R.string.res_export_db_good) + " " + file;
        } else {
            text = getString(R.string.res_export_db_bad);
        }
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(this, text, duration);
        toast.show();
    }

    @SuppressWarnings("UnusedParameters")
    public void importDB(View v) {
        final String[] files = getDataExportImport().getFilesToImportDB();
        OnItemClickListener listener = (parent, view, position, id) -> {
            selectedFile = files[position];
            Log.d(DEBUG_TAG, selectedFile);
            dialog.dismiss();
            Dialog alert = createImportAlert(importDBListener,selectedFile);
            alert.show();
            //_importDB(files[position]);
        };
        dialog = SingleChoiceSelectDialog.getInstance(files, listener,getText(R.string.dialog_title_file).toString());


        dialog.show(getSupportFragmentManager(), "importDBDlg");

    }

    private void _importDB(String fileName) {
        boolean res = getDataExportImport().importDB( fileName);

        String text;
        if (res) {
            text = getString(R.string.res_import_db_good);
        } else {
            text = getString(R.string.res_import_db_bad);

        }
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(this, text, duration);
        toast.show();

        if (!res) {
            return;
        }

        updateAndFinish();

    }
    private void updateAndFinish(){
        Intent intent = new Intent();
        intent.putExtra(UPDATE_KEY, UPDATE_LIST);
        setResult(RESULT_OK, intent);
        finish();

    }

    @SuppressWarnings("UnusedParameters")
    public void exportTxt(View v) {
        String file = getDataExportImport().exportAuthorList(getAuthorController());
        String text;
        if (file != null) {
            text = getString(R.string.res_export_txt_good) + " " + file;
        } else {
            text = getString(R.string.res_export_txt_bad);
        }
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(this, text, duration);
        toast.show();
    }

    @SuppressWarnings("UnusedParameters")
    public void importTxt(View v) {
        final String[] files = getDataExportImport().getFilesToImportTxt();
        OnItemClickListener listener = (parent, view, position, id) -> {
            selectedFile = files[position];
            Log.d(DEBUG_TAG, selectedFile);
            dialog.dismiss();
//                Dialog alert= createImportAlert(selectedFile);
//                alert.show();
            _importTxt(files[position]);
        };
        dialog =  SingleChoiceSelectDialog.getInstance(files, listener,getText(R.string.dialog_title_file).toString());


        dialog.show(getSupportFragmentManager(), "importTxtDlg");
       
    }

    private void _importTxt(String file) {
        
        ArrayList<String> urls =  getDataExportImport().importAuthorList(file);
        if (!urls.isEmpty()){
            getSamlibOperation().makeAuthorAdd(urls,null);
            progress = new ProgressDialog(this);
            progress.setMessage(getText(R.string.arc_import_text_title));
            progress.setCancelable(false);
            progress.setIndeterminate(true);
            progress.show();
        }


    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int sel = item.getItemId();
        if (sel == android.R.id.home ){
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
    @SuppressWarnings("UnusedParameters")
    public void exportGoogle(View v) {
        makeGoogleOperation(GoogleDiskOperation.OperationType.EXPORT);
    }
    @SuppressWarnings("UnusedParameters")
    public void importGoogle(View v) {
        DialogInterface.OnClickListener listener = (dialog1, which) -> {
            switch (which) {
                case Dialog.BUTTON_POSITIVE:
                    makeGoogleOperation(GoogleDiskOperation.OperationType.IMPORT);
                    break;
                case Dialog.BUTTON_NEGATIVE:
                    break;

            }
        };
        Dialog alert = createImportAlert(listener,getString(R.string.arc_google_file));
        alert.show();

    }
    private ProgressDialog progress;
    private GoogleReceiver receiver;
    private GoogleDiskOperation.OperationType operation;

    private  void makeGoogleOperation(GoogleDiskOperation.OperationType ot){
        operation = ot;
        progress = new ProgressDialog(this);
        progress.setMessage(getText(ot.getMessage()));
        progress.setCancelable(true);
        progress.setIndeterminate(true);
        progress.show();
        new GoogleDiskOperation(this,getSettingsHelper(),operation).execute();

    }

    @Override
    protected void onResume() {
        super.onResume();
        receiver = new GoogleReceiver();
        IntentFilter filter = new IntentFilter(GoogleReceiver.ACTION);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        registerReceiver(receiver, filter);

        mResultSubscription=getBus()
                .getObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.newThread())
                .filter(GuiUpdateObject::isResult)
                .subscribe(mSubscriber);
        addSubscription(mResultSubscription);

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        mResultSubscription.unsubscribe();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case GoogleDiskOperation.RESOLVE_CONNECTION_REQUEST_CODE:
                if (getSettingsHelper() == null){
                    Log.e(DEBUG_TAG,"settings is null!!");
                    return;
                }
                getSettingsHelper().setGoogleAccount(
                    data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME));
                progress.show();
                new GoogleDiskOperation(this,getSettingsHelper(),operation).execute();
                break;
        }
    }

    public class GoogleReceiver extends BroadcastReceiver {
        public static final String ACTION="GoogleReceiver_ACTION";
        public static final String EXTRA_RESULT="EXTRA_RESULT";
        public static final String EXTRA_OPERATION="EXTRA_OPERATION";
        public static final String EXTRA_ERROR="EXTRA_ERROR";

        @Override
        public void onReceive(Context context, Intent intent) {

            boolean res = intent.getBooleanExtra(EXTRA_RESULT,false);
            GoogleDiskOperation.OperationType ot=GoogleDiskOperation.OperationType.valueOf(
            intent.getStringExtra(EXTRA_OPERATION));

            progress.dismiss();

            if (res && ot == GoogleDiskOperation.OperationType.IMPORT){
                updateAndFinish();
                return;
            }
            if (res && ot == GoogleDiskOperation.OperationType.EXPORT){
                cb.setEnabled(getSettingsHelper().isGoogleAutoEnable());
                Toast.makeText(context, context.getString(R.string.res_export_google_good), Toast.LENGTH_LONG).show();
            }
            String error = intent.getStringExtra(EXTRA_ERROR);
            if (!res && error!= null){
                Toast.makeText(context,error,Toast.LENGTH_LONG).show();
            }

        }
    }
    Subscriber<GuiUpdateObject> mSubscriber = new Subscriber<GuiUpdateObject>() {

        @Override
        public void onCompleted() {
            Log.d(DEBUG_TAG,"onCompleted");
        }

        @Override
        public void onError(Throwable e) {
            Log.e(DEBUG_TAG,"onError",e);
        }

        @Override
        public void onNext(GuiUpdateObject guiUpdateObject) {
            AlertDialog.Builder builder = new AlertDialog.Builder(ArchiveActivity.this);
            MessageConstructor mc = new MessageConstructor(ArchiveActivity.this,getSettingsHelper());
            TextView tvMsg = new TextView(ArchiveActivity.this);
            tvMsg.setText(Html.fromHtml(mc.makeMessage(guiUpdateObject)));
            builder.setTitle(R.string.import_author_result)
                    .setView(tvMsg)
                    .setCancelable(false)
                    .setNegativeButton(R.string.Yes, (dialog1, which) -> {
                        dialog1.cancel();
                    });
            AlertDialog alert = builder.create();
            progress.dismiss();
            alert.show();
        }
    };


}
