package monakhv.android.samlib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Toast;


import com.mikepenz.iconics.typeface.FontAwesome;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SectionDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import monakhv.android.samlib.data.SettingsHelper;
import monakhv.android.samlib.search.SearchAuthorActivity;
import monakhv.android.samlib.search.SearchAuthorsListFragment;
import monakhv.android.samlib.service.AuthorEditorServiceIntent;
import monakhv.android.samlib.service.CleanNotificationData;
import monakhv.android.samlib.sql.AuthorProvider;
import monakhv.samlib.db.SQLController;
import monakhv.samlib.db.entity.SamLibConfig;

import java.util.ArrayList;


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

    private static final String DEBUG_TAG = "MainActivity";
    //    private static final String STATE_SELECTION = "STATE_SELECTION";
//    private static final String STATE_AUTHOR_POS = "STATE_AUTHOR_ID";
    public static final int ARCHIVE_ACTIVITY = 1;
    public static final int SEARCH_ACTIVITY = 2;
    public static final int PREFS_ACTIVITY = 3;
    public static final String CLEAN_NOTIFICATION = "CLEAN_NOTIFICATION";
    private UpdateActivityReceiver updateReceiver;
    private AuthorFragment authorFragment;
    private BookFragment bookFragment;
    private SettingsHelper settingsHelper;
    private DownloadReceiver downloadReceiver;
    private AuthorEditReceiver authorReceiver;
    private boolean twoPain;


    private Drawer.Result drResult;
    private int menu_add_search = 1;
    private int menu_settings = 3;
    private int menu_data = 5;
    private int menu_sort_author = 7;
    private int menu_sort_books = 9;
    private int tagsShift = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        settingsHelper = new SettingsHelper(this);
        setTheme(settingsHelper.getTheme());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
        Bundle bundle = getIntent().getExtras();

        String clean = null;
        if (bundle != null) {
            clean = bundle.getString(CLEAN_NOTIFICATION);
        }
        if (clean != null) {
            CleanNotificationData.start(this);
            bundle = null;
        }

        authorFragment = (AuthorFragment) getSupportFragmentManager().findFragmentById(R.id.authorFragment);
        authorFragment.setHasOptionsMenu(true);


//        if (bundle != null) {
//            Log.i(DEBUG_TAG, "Restore state");
//            onRestoreInstanceState(bundle);
//        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        twoPain = findViewById(R.id.two_pain) != null;
        if (twoPain) {
            Log.i(DEBUG_TAG, "onCreate: two pane");
        } else {
            Log.i(DEBUG_TAG, "onCreate: one pane");
        }


        ArrayList<IDrawerItem> items = new ArrayList<>();

        items.add(new PrimaryDrawerItem().withName(R.string.menu_search).withIcon(FontAwesome.Icon.faw_search_plus).withIdentifier(menu_add_search) );
        items.add(new PrimaryDrawerItem().withName(R.string.menu_settings).withIcon(FontAwesome.Icon.faw_cog).withIdentifier(menu_settings));
        items.add(new PrimaryDrawerItem().withName(R.string.menu_archive).withIcon(FontAwesome.Icon.faw_archive).withIdentifier(menu_data));


        items.add(new SectionDrawerItem().withName(R.string.dialog_title_sort_author));
        items.add(new SecondaryDrawerItem().withName(R.string.sort_author_name).withTag(AuthorFragment.SortOrder.AuthorName)
        .withIdentifier(menu_sort_author));
        items.add(new SecondaryDrawerItem().withName(R.string.sort_update_date).withTag(AuthorFragment.SortOrder.DateUpdate)
        .withIdentifier(menu_sort_author));

        if (twoPain) {
            items.add(new SectionDrawerItem().withName(R.string.dialog_title_sort_book));
            items.add(new SecondaryDrawerItem().withName(R.string.sort_book_date).withTag(BookFragment.SortOrder.BookDate)
                    .withIdentifier(menu_sort_books));
            items.add(new SecondaryDrawerItem().withName(R.string.sort_book_mtime).withTag(BookFragment.SortOrder.DateUpdate)
                    .withIdentifier(menu_sort_books));
            items.add(new SecondaryDrawerItem().withName(R.string.sort_book_title).withTag(BookFragment.SortOrder.BookName)
                    .withIdentifier(menu_sort_books));
            items.add(new SecondaryDrawerItem().withName(R.string.sort_book_size).withTag(BookFragment.SortOrder.BookSize)
                    .withIdentifier(menu_sort_books));

        }


        //Begin author group
        items.add(new SectionDrawerItem().withName(R.string.menu_tags));
        items.add(new SecondaryDrawerItem().withName(R.string.filter_all).withIdentifier(SamLibConfig.TAG_AUTHOR_ALL + tagsShift)
        .withTag(getString(R.string.filter_all)));
        items.add(new SecondaryDrawerItem().withName(R.string.filter_new).withIdentifier(SamLibConfig.TAG_AUTHOR_NEW + tagsShift)
        .withTag(getString(R.string.filter_new)));

        Cursor tags = getContentResolver().query(AuthorProvider.TAG_URI, null, null, null, SQLController.COL_TAG_NAME);

        while (tags.moveToNext()) {
            items.add(new SecondaryDrawerItem().withName(tags.getString(tags.getColumnIndex(SQLController.COL_TAG_NAME)))
                    .withIdentifier( tagsShift + tags.getInt(tags.getColumnIndex(SQLController.COL_ID)))
                    .withTag(tags.getString(tags.getColumnIndex(SQLController.COL_TAG_NAME))));
        }
        //end author group

        drResult = new Drawer()
                .withActivity(this)
                .withToolbar(toolbar)
                .withActionBarDrawerToggle(true)
                .withActionBarDrawerToggleAnimated(true)
                .withHeader(R.layout.drawer_header)
                .addDrawerItems(items.toArray(new IDrawerItem[1]) )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l, IDrawerItem iDrawerItem) {
                        int ident =iDrawerItem.getIdentifier();
                        Log.i(DEBUG_TAG,"Identifier: "+ident);
                        if (ident > 90){//tag selection section
                            int tag_id=ident - tagsShift;
                            authorFragment.selectTag(tag_id, (String) iDrawerItem.getTag());
                        }
                        if (ident == menu_settings){
                            Log.d(DEBUG_TAG, "go to Settings");
                            Intent prefsIntent = new Intent(getApplicationContext(),
                                    SamlibPreferencesActivity.class);
                            //prefsIntent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                            startActivityForResult(prefsIntent, MainActivity.PREFS_ACTIVITY);
                        }
                        if (ident == menu_data){
                            Log.d(DEBUG_TAG, "go to Archive");
                            Intent prefsIntent = new Intent(getApplicationContext(),
                                    ArchiveActivity.class);
                            startActivityForResult(prefsIntent, MainActivity.ARCHIVE_ACTIVITY);
                        }
                        if (ident == menu_add_search){
                            authorFragment.searchOrAdd();
                        }
                        if (ident == menu_sort_author){
                            AuthorFragment.SortOrder so =  (AuthorFragment.SortOrder) iDrawerItem.getTag();
                            authorFragment.setSortOrder(so);
                        }
                        if (ident == menu_sort_books){
                            BookFragment.SortOrder so = (BookFragment.SortOrder) iDrawerItem.getTag();
                            bookFragment.setSortOrder(so);
                        }

                    }
                }).build();
        tags.close();


    }
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent == null) {
            return;
        }
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
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
        IntentFilter authorFilter = new IntentFilter(AuthorEditorServiceIntent.RECEIVER_FILTER);


        updateFilter.addCategory(Intent.CATEGORY_DEFAULT);
        authorFilter.addCategory(Intent.CATEGORY_DEFAULT);


        updateReceiver = new UpdateActivityReceiver();
        authorReceiver = new AuthorEditReceiver();

        //getActionBarHelper().setRefreshActionItemState(refreshStatus);
        registerReceiver(updateReceiver, updateFilter);
        registerReceiver(authorReceiver, authorFilter);

        if (twoPain) {
            bookFragment = (BookFragment) getSupportFragmentManager().findFragmentById(R.id.listBooksFragment);
            if (bookFragment == null) {
                Log.e(DEBUG_TAG, "Fragment is NULL for two pane layout!!");
            }
            downloadReceiver = new DownloadReceiver(bookFragment);
            IntentFilter filter = new IntentFilter(DownloadReceiver.ACTION_RESP);
            filter.addCategory(Intent.CATEGORY_DEFAULT);
            registerReceiver(downloadReceiver, filter);
        }
        getSupportActionBar().setTitle(R.string.app_name);
        authorFragment.refresh(null, null);
        drResult.setSelectionByIdentifier(SamLibConfig.TAG_AUTHOR_ALL + tagsShift);


    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(updateReceiver);
        unregisterReceiver(authorReceiver);
        if (twoPain) {
            unregisterReceiver(downloadReceiver);
        }

        //Stop refresh status
        authorFragment.onRefreshComplete();
        //getActionBarHelper().setRefreshActionItemState(refreshStatus);
    }

//    @Override
//    public void onSaveInstanceState(Bundle bundle) {
//        super.onSaveInstanceState(bundle);
//        bundle.putString(STATE_SELECTION, authorFragment.getSelection());
//        bundle.putInt(STATE_AUTHOR_POS, authorFragment.getSelectedAuthorPosition());
//
//    }
//    @Override
//    public void onRestoreInstanceState(Bundle bundle) {
//        super.onRestoreInstanceState(bundle);
//        if (bundle == null) {
//            return;
//        }
//        Log.i(DEBUG_TAG,"onRestoreInstanceState");
//
//
//        authorFragment.refresh(bundle.getString(STATE_SELECTION), null);
//        authorFragment.restoreSelection(bundle.getInt(STATE_AUTHOR_POS));
//        if (bookFragment != null){
//            bookFragment.setAuthorId(authorFragment.getSelectedAuthorId());
//        }
//
//    }

    /**
     * Return from ArchiveActivity or SearchActivity
     *
     * @param requestCode request code
     * @param resultCode  result code
     * @param data        Intent data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            Log.d(DEBUG_TAG, "Wrong result code from onActivityResult");
            authorFragment.refresh(null, null);
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
            Log.v(DEBUG_TAG, "Start add Author");

            AuthorEditorServiceIntent.addAuthor(getApplicationContext(), data.getStringExtra(SearchAuthorsListFragment.AUTHOR_URL));
        }
        if (requestCode == PREFS_ACTIVITY) {
            finish();
        }
    }

    @Override
    public void onAuthorSelected(long id) {
        Log.d(DEBUG_TAG, "onAuthorSelected: go to Books");
        if (twoPain) {
            Log.i(DEBUG_TAG, "Two fragments Layout - set author_id: " + id);
            bookFragment.setAuthorId(id);
        } else {
            Log.i(DEBUG_TAG, "One fragment Layout - set author_id: " + id);
            Intent intent = new Intent(this, BooksActivity.class);
            intent.putExtra(BookFragment.AUTHOR_ID, id);

            startActivity(intent);
        }


    }

    @Override
    public void selectBookSortOrder() {
        bookFragment.selectSortOrder();
    }

    @Override
    public void onTitleChange(String lTitle) {
        Log.d(DEBUG_TAG, "set title: " + lTitle);

        getSupportActionBar().setTitle(lTitle);
//        if (authorFragment.getSelection() == null){
//            getSupportActionBar().setDisplayOptions(0, ActionBar.DISPLAY_HOME_AS_UP);
//        }
//        else {
//            getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_HOME_AS_UP, ActionBar.DISPLAY_HOME_AS_UP);
//        }

    }

    @Override
    public void cleanBookSelection() {
        if (twoPain) {
            bookFragment.setAuthorId(0);//empty selection
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


    public void addAuthorFromText() {
        EditText editText = (EditText) findViewById(R.id.addUrlText);

        if (editText == null) {
            return;
        }
        if (editText.getText() == null) {
            return;
        }
        String text = editText.getText().toString();
        editText.setText("");


        View v = findViewById(R.id.add_author_panel);
        v.setVisibility(View.GONE);

        String url = SamLibConfig.getParsedUrl(text);
        if (url != null) {//add  Author by URL
            AuthorEditorServiceIntent.addAuthor(getApplicationContext(), url);

        } else {
            if (TextUtils.isEmpty(text)) {
                return;
            }
            //Start Search activity to make search and add selected Authors to Data Base
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

    public class AuthorEditReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            int duration = Toast.LENGTH_SHORT;
            CharSequence msg = intent.getCharSequenceExtra(AuthorEditorServiceIntent.RESULT_MESSAGE);
            Toast toast = Toast.makeText(context, msg, duration);

            if (intent.getStringExtra(AuthorEditorServiceIntent.EXTRA_ACTION_TYPE).equals(AuthorEditorServiceIntent.ACTION_ADD)) {
                Log.d(DEBUG_TAG, "onReceive: author add");
                long id = intent.getLongExtra(AuthorEditorServiceIntent.RESULT_AUTHOR_ID, 0);

                authorFragment.selectAuthor(id);
                toast.show();
                onAuthorSelected(id);

            }
            if (intent.getStringExtra(AuthorEditorServiceIntent.EXTRA_ACTION_TYPE).equals(AuthorEditorServiceIntent.ACTION_DELETE)) {
                Log.d(DEBUG_TAG, "onReceive: author del");
                toast.show();
            }

        }
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
