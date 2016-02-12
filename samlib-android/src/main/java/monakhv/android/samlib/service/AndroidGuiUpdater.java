package monakhv.android.samlib.service;

import android.content.Context;
import android.content.Intent;
import monakhv.android.samlib.DownloadReceiver;
import monakhv.android.samlib.R;
import monakhv.android.samlib.data.SettingsHelper;
import monakhv.samlib.data.AbstractSettings;
import monakhv.samlib.db.AuthorController;
import monakhv.samlib.db.TagController;
import monakhv.samlib.db.entity.*;
import monakhv.samlib.log.Log;
import monakhv.samlib.service.GuiUpdate;
import monakhv.samlib.service.GuiUpdateObject;
import monakhv.samlib.service.SamlibService;

import javax.inject.Inject;
import java.util.List;

/*
 * Copyright 2015  Dmitry Monakhov
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
 * 09.07.15.
 */
public class AndroidGuiUpdater implements GuiUpdate {
    public enum CALLER_TYPE {
        CALLER_IS_ACTIVITY,
        CALLER_IS_RECEIVER,
        CALLER_IS_UNDEF
    }
    private static final String DEBUG_TAG="AndroidGuiUpdater";
    public static final String ACTION_RESP = "monakhv.android.samlib.action.UPDATED";
    public static final String TOAST_STRING = "TOAST_STRING";
    public static final String ACTION = "ACTION";
    public static final String ACTION_TOAST = "TOAST";

    public static final String EXTRA_PARCEL="monakhv.android.samlib.action.EXTRA_PARCEL";


    public static final String ACTION_REFRESH = "ACTION_REFRESH";
    public static final String ACTION_REFRESH_OBJECT = "ACTION_REFRESH_OBJECT";

   // public static final int     ACTION_REFRESH_AUTHORS = 10;
   // public static final int     ACTION_REFRESH_BOTH     = 20;//authors & books
  public static final int     ACTION_REFRESH_TAGS        = 30;



    public static final String CALLER_TYPE_EXTRA="monakhv.android.samlib.action.CALLER_TYPE_EXTRA";
    public static final String RESULT_AUTHOR_ID="RESULT_AUTHOR_ID";



    private final Context mContext;
    private final CALLER_TYPE mCallerType;
    private final SettingsHelper mSettingsHelper;
    private ProgressNotification mProgressNotification;

    @Inject
    public AndroidGuiUpdater(SettingsHelper settingsHelper,UpdateObject updateObject, AuthorController ctl) {
        this.mSettingsHelper=settingsHelper;
        this.mContext = settingsHelper.getContext();
        this.mCallerType = updateObject.getCALLER_type();

        if (ctl != null){
            init(updateObject,ctl);
        }

    }

    private void init( UpdateObject updateObject, AuthorController ctl){

        String notificationTitle;
        if (updateObject.getObjectType() == SamlibService.UpdateObjectSelector.Author) {//Check update for the only Author

            //int id = intent.getIntExtra(SELECT_ID, 0);//author_id
            Author author = ctl.getById(updateObject.getObjectId());
            if (author != null) {

                notificationTitle = mContext.getString(R.string.notification_title_author) + " " + author.getName();
                android.util.Log.i(DEBUG_TAG, "Check single Author: " + author.getName());
            } else {
                android.util.Log.e(DEBUG_TAG, "Can not find Author: " + updateObject.getObjectId());
                return;
            }
        } else {//Check update for authors by TAG

            notificationTitle = mContext.getString(R.string.notification_title_TAG);
            if (updateObject.getObjectId() == SamLibConfig.TAG_AUTHOR_ALL) {
                notificationTitle += " " + mContext.getString(R.string.filter_all);
            } else if (updateObject.getObjectId() == SamLibConfig.TAG_AUTHOR_NEW) {
                notificationTitle += " " + mContext.getString(R.string.filter_new);
            } else {
                TagController tagCtl =ctl.getTagController();
                Tag tag = tagCtl.getById(updateObject.getObjectId());
                if (tag != null) {
                    notificationTitle += " " + tag.getName();
                }

            }
            android.util.Log.i(DEBUG_TAG, "selection index: " + updateObject.getObjectId());
        }
        if (updateObject.callerIsActivity()){
            mProgressNotification = new ProgressNotification(mSettingsHelper, notificationTitle);
        }
    }
    public void makeUpdateUpdate(Author a,GuiUpdateObject guiUpdateObject){
        if (mProgressNotification != null){
            mProgressNotification.update(a);
        }
        sendBroadcast(new AndroidGuiUpdateObject(guiUpdateObject));
    }

    private void sendBroadcast(AndroidGuiUpdateObject guiUpdateObject){
        Intent broadcastIntent = new Intent();
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.setAction(ACTION_RESP);

        broadcastIntent.putExtra(EXTRA_PARCEL,guiUpdateObject);
        mContext.sendBroadcast(broadcastIntent);
    }

    @Override
    public void makeUpdateTagList() {
        Intent broadcastIntent = new Intent();
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.setAction(ACTION_RESP);
        broadcastIntent.putExtra(ACTION, ACTION_REFRESH);
        broadcastIntent.putExtra(ACTION_REFRESH_OBJECT,ACTION_REFRESH_TAGS);

        mContext.sendBroadcast(broadcastIntent);

    }

    @Override
    public void finishBookLoad(  boolean b, AbstractSettings.FileType ft, long book_id) {
        Log.d(DEBUG_TAG, "finish result: " + b);
        Log.d(DEBUG_TAG, "file type:  " + ft.toString());
        if (mCallerType == CALLER_TYPE.CALLER_IS_RECEIVER){
            return;
        }
        CharSequence msg;
        if (b) {
            msg = mContext.getText(R.string.download_book_success);
        } else {
            msg = mContext.getText(R.string.download_book_error);
        }
        Intent broadcastIntent = new Intent();
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.setAction(DownloadReceiver.ACTION_RESP);
        broadcastIntent.putExtra(DownloadReceiver.MESG, msg);
        broadcastIntent.putExtra(DownloadReceiver.RESULT, b);
        broadcastIntent.putExtra(DownloadReceiver.FILE_TYPE, ft.toString());
        broadcastIntent.putExtra(DownloadReceiver.BOOK_ID, book_id);

        mContext.sendBroadcast(broadcastIntent);

    }



    @Override
    public void sendAuthorUpdateProgress(int total, int iCurrent, String name) {
        if (mCallerType == CALLER_TYPE.CALLER_IS_RECEIVER) {//Call as a regular service
            return;//we do not send update for regular service
        }
        mProgressNotification.updateProgress(total,iCurrent,name);
    }

    @Override
    public void finishUpdate(boolean result, List<Author> updatedAuthors) {
        Log.d(DEBUG_TAG, "Finish intent.");


        if (mSettingsHelper.isGoogleAuto() && result && !updatedAuthors.isEmpty()) {
            GoogleAutoService.startService(mContext);
        }

        if (mCallerType == CALLER_TYPE.CALLER_IS_ACTIVITY) {//Call from activity
            mProgressNotification.cancel();

            CharSequence text;

            if (result) {//Good Call
                if (updatedAuthors.isEmpty()) {
                    text = mContext.getText(R.string.toast_update_good_empty);
                } else {
                    text = mContext.getText(R.string.toast_update_good_good);
                }

            } else {//Error call
                text = mContext.getText(R.string.toast_update_error);
            }
            Intent broadcastIntent = new Intent();
            broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
            broadcastIntent.setAction(ACTION_RESP);
            broadcastIntent.putExtra(ACTION, ACTION_TOAST);
            broadcastIntent.putExtra(TOAST_STRING, text);
            mContext.sendBroadcast(broadcastIntent);
        }

        if (mCallerType == CALLER_TYPE.CALLER_IS_RECEIVER) {//Call as a regular service


            if (result && updatedAuthors.isEmpty() && !mSettingsHelper.getDebugFlag()) {
                return;//no errors and no updates - no notification
            }

            if (!result && mSettingsHelper.getIgnoreErrorFlag()) {
                return;//error and we ignore them
            }

            NotificationData notifyData = NotificationData.getInstance(mContext);
            if (result) {//we have updates

                if (updatedAuthors.isEmpty()) {//DEBUG CASE
                    notifyData.notifyUpdateDebug(mSettingsHelper);

                } else {

                    notifyData.notifyUpdate(mSettingsHelper, updatedAuthors);
                }

            } else {//connection Error
                notifyData.notifyUpdateError(mSettingsHelper);

            }
        }

    }



}
