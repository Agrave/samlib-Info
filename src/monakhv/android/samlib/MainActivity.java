package monakhv.android.samlib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import monakhv.android.samlib.data.SettingsHelper;
import monakhv.android.samlib.search.SearchAuthorActivity;
import monakhv.android.samlib.search.SearchAuthorsListFragment;
import monakhv.android.samlib.service.CleanNotificationData;
import monakhv.android.samlib.sql.entity.SamLibConfig;
import monakhv.android.samlib.tasks.AddAuthor;


/*
 * Copyright 2014  Dmitry Monakhov
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
 *
 * 12/5/14.
 */
public class MainActivity extends ActionBarActivity implements AuthorFragment.Callbacks {

    private static final String DEBUG_TAG = "TestActivity";
    public static final int ARCHIVE_ACTIVITY = 1;
    public static final int SEARCH_ACTIVITY  = 2;
    public static final int PREFS_ACTIVITY  = 3;
    public static final  String CLEAN_NOTIFICATION = "CLEAN_NOTIFICATION";
    private UpdateActivityReceiver updateReceiver;
    private AuthorFragment authorFragment;
    private SettingsHelper settingsHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        settingsHelper = new SettingsHelper(this);
        setTheme(settingsHelper.getTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_layout);
        authorFragment = (AuthorFragment) getSupportFragmentManager().findFragmentById(R.id.authorFragment);
        authorFragment.setHasOptionsMenu(true);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent == null){
            return;
        }
        Bundle bundle= intent.getExtras();
        if (bundle != null){
            String clean = bundle.getString(CLEAN_NOTIFICATION);
            if (clean != null) {
                CleanNotificationData.start(this);
            }
        }
    }
    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter updateFilter = new IntentFilter(UpdateActivityReceiver.ACTION_RESP);


        updateFilter.addCategory(Intent.CATEGORY_DEFAULT);


        updateReceiver = new UpdateActivityReceiver();

        //getActionBarHelper().setRefreshActionItemState(refreshStatus);
        registerReceiver(updateReceiver, updateFilter);

    }
    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(updateReceiver);

        //Stop refresh status
        authorFragment.onRefreshComplete();
        //getActionBarHelper().setRefreshActionItemState(refreshStatus);
    }

    /**
     * Return from ArchiveActivity or SearchActivity
     *
     * @param requestCode request code
     * @param resultCode result code
     * @param data Intent data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            Log.d(DEBUG_TAG, "Wrong result code from onActivityResult");
            return;
        }
        if (requestCode == ARCHIVE_ACTIVITY) {

            int res = data.getIntExtra(ArchiveActivity.UPDATE_KEY, -1);
            if (res == ArchiveActivity.UPDATE_LIST) {
                Log.d(DEBUG_TAG, "Reconstruct List View");
                authorFragment.refresh(null, null);

            }
        }
        if (requestCode == SEARCH_ACTIVITY) {
            AddAuthor aa = new AddAuthor(getApplicationContext());
            aa.execute(data.getStringExtra(SearchAuthorsListFragment.AUTHOR_URL));
        }
        if (requestCode == PREFS_ACTIVITY){
            finish();
        }
    }

    @Override
    public void onAuthorSelected(long id) {
        Log.d(DEBUG_TAG, "go to Books");
        Intent intent = new Intent(this,BooksActivity.class);
        intent.putExtra(BookFragment.AUTHOR_ID,id);

        startActivity(intent);
        

    }

    @Override
    public void selectBookSortOrder() {

    }

    @Override
    public void onTitleChange(String lTitle) {
        Log.d(DEBUG_TAG, "set title: "+lTitle);

        getSupportActionBar().setTitle(lTitle);
        if (authorFragment.getSelection() == null){
            getSupportActionBar().setDisplayOptions(0, ActionBar.DISPLAY_HOME_AS_UP);
        }
        else {
            getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP, ActionBar.DISPLAY_HOME_AS_UP);
        }

    }
    /**
     * Add new Author to SQL Store
     *
     * @param view View
     */
    @SuppressWarnings("UnusedParameters")
    public void addAuthor(View view) {

        addAuthorFromText();

    }

    @Override
    public void addAuthorFromText(){
        EditText editText = (EditText) findViewById(R.id.addUrlText);

        if (editText == null){
            return;
        }
        if (editText.getText() == null){
            return;
        }
        String text = editText.getText().toString();
        editText.setText("");


        View v = findViewById(R.id.add_author_panel);
        v.setVisibility(View.GONE);

        String url = SamLibConfig.getParsedUrl(text);
        if (url != null){
            AddAuthor aa = new AddAuthor(this.getApplicationContext());
            aa.execute(url);
        }
        else {
            if (TextUtils.isEmpty(text)) {
                return;
            }
            Intent prefsIntent = new Intent(getApplicationContext(),
                    SearchAuthorActivity.class);
            prefsIntent.putExtra(SearchAuthorActivity.EXTRA_PATTERN, text);
            prefsIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);

            startActivityForResult(prefsIntent, SEARCH_ACTIVITY);
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if ((keyCode == KeyEvent.KEYCODE_BACK)) { //Back key pressed

            if (authorFragment.getSelection() != null) {
                authorFragment.refresh(null, null);
                onTitleChange(getString(R.string.app_name));
            } else {
                finish();
            }

            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
    /**
     * Receive updates from Update Service
     */
    public class UpdateActivityReceiver extends BroadcastReceiver {

        public static final String ACTION_RESP = "monakhv.android.samlib.action.UPDATED";
        public static final String TOAST_STRING = "TOAST_STRING";
        public static final String ACTION = "ACTION";
        public static final String ACTION_TOAST = "TOAST";
        public static final String ACTION_PROGRESS = "PROGRESS";

        @Override
        public void onReceive(Context context, Intent intent) {

            String action = intent.getStringExtra(ACTION);
            if (action != null) {
                if (action.equalsIgnoreCase(ACTION_TOAST)) {
                    int duration = Toast.LENGTH_SHORT;
                    Toast toast = Toast.makeText(context, intent.getCharSequenceExtra(TOAST_STRING), duration);
                    toast.show();

                    authorFragment.onRefreshComplete();
                }//
                if (action.equalsIgnoreCase(ACTION_PROGRESS)) {
                    authorFragment.updateProgress(intent.getStringExtra(TOAST_STRING));
                }
            }


        }
    }
}
