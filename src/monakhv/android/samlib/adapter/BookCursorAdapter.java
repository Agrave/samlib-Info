package monakhv.android.samlib.adapter;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;


import monakhv.android.samlib.R;
import monakhv.android.samlib.data.SettingsHelper;
import monakhv.android.samlib.sql.SQLController;
import monakhv.android.samlib.sql.entity.SamLibConfig;

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
 * 12/4/14.
 */
public class BookCursorAdapter extends RecyclerCursorAdapter<BookCursorAdapter.ViewHolder> {

    private int author_id;
    private Context context;
    private SettingsHelper settingsHelper;
    public BookCursorAdapter(Cursor cursor, Context context) {
        super(cursor);
        this.context = context;
        settingsHelper = new SettingsHelper(context);
    }

    @Override
    public void onBindViewHolderCursor(ViewHolder holder, Cursor cursor) {
        int idx_form = cursor.getColumnIndex(SQLController.COL_BOOK_FORM);
        int idx_mtime = cursor.getColumnIndex(SQLController.COL_BOOK_MTIME);
        int idx_date = cursor.getColumnIndex(SQLController.COL_BOOK_DATE);
        int idx_desc = cursor.getColumnIndex(SQLController.COL_BOOK_DESCRIPTION);
        int idx_size = cursor.getColumnIndex(SQLController.COL_BOOK_SIZE);
        int idx_title = cursor.getColumnIndex(SQLController.COL_BOOK_TITLE);
        int idx_isNew = cursor.getColumnIndex(SQLController.COL_BOOK_ISNEW);
        int idx_group_id = cursor.getColumnIndex(SQLController.COL_BOOK_GROUP_ID);
        int idx_author = cursor.getColumnIndex(SQLController.COL_BOOK_AUTHOR);
        long book_id = cursor.getLong(cursor.getColumnIndex(SQLController.COL_ID));

        holder.bookTitle.setText(Html.fromHtml(cursor.getString(idx_title)));

        try {
            holder.bookDesc.setText(Html.fromHtml(cursor.getString(idx_desc)));
        } catch (Exception ex) {//This is because of old book scheme where Description could be null
            holder.bookDesc.setText("");
        }


        holder.bookAuthorName.setText(cursor.getString(idx_author));
        if (author_id !=  SamLibConfig.SELECTED_BOOK_ID){
            holder.bookAuthorName.setVisibility(View.GONE);

        }
        else {
            holder.bookAuthorName.setVisibility(View.VISIBLE);
        }

        holder.bookSize.setText(cursor.getString(idx_size)+"K");
        holder.bookForm.setText(cursor.getString(idx_form));

        if (cursor.getInt(idx_isNew) == 1) {
            holder.bookIcon.setImageResource(R.drawable.open);

        } else {
            holder.bookIcon.setImageResource(R.drawable.closed);

        }
        holder.bookIcon.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {

            }
        });

        if(cursor.getInt(idx_group_id)==1){
            holder.starIcon.setImageResource(settingsHelper.getSelectedIcon());
            holder.starIcon.setVisibility(View.VISIBLE);
        }
        else {
           holder.starIcon.setImageResource(R.drawable.rating_not_important);
            holder.starIcon.setVisibility(View.GONE);
        }




    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int i) {
        return null;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        // {R.id.bookTitle, R.id.bookUpdate, R.id.bookDesc, R.id.Bookicon,R.id.Staricon,R.id.bookAuthorName,R.id.bookForm};
        public TextView bookTitle,bookSize,bookDesc,bookAuthorName,bookForm;
        public ImageView bookIcon,starIcon;

        public ViewHolder(View itemView) {
            super(itemView);
            bookTitle = (TextView) itemView.findViewById(R.id.bookTitle);
            bookSize= (TextView) itemView.findViewById(R.id.bookUpdate);
            bookDesc= (TextView) itemView.findViewById(R.id.bookDesc);
            bookAuthorName= (TextView) itemView.findViewById(R.id.bookAuthorName);
            bookForm = (TextView) itemView.findViewById(R.id.bookForm);

            bookIcon= (ImageView) itemView.findViewById(R.id.Bookicon);
            starIcon= (ImageView) itemView.findViewById(R.id.Staricon);
        }
    }
}