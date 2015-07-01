package monakhv.samlib.desk.service;

import monakhv.samlib.db.SQLController;
import monakhv.samlib.db.entity.Author;
import monakhv.samlib.db.entity.Book;
import monakhv.samlib.desk.data.Settings;
import monakhv.samlib.desk.sql.AuthorController;
import monakhv.samlib.exception.SamlibParseException;
import monakhv.samlib.http.HttpClientController;
import monakhv.samlib.log.Log;

import java.io.IOException;

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
 * 30.06.15.
 */
public class Update {
    private static final String DEBUG_TAG = "Update";

    private Settings settings;

    public Update(Settings settings) {
        this.settings = settings;
    }

    public void run(List<Author> list) {
        SQLController sql;
        try {
            sql = SQLController.getInstance(settings.getDataDirectoryPath());
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "SQL Error", e);
            return;
        }
        AuthorController ctl = new AuthorController(sql);
        HttpClientController http = HttpClientController.getInstance(settings);

        Author ess = ctl.getById(118);

        Log.i(DEBUG_TAG, "Author: " + ess.getName() + " - " + ess.getBooks().size());
        Author newEss = ctl.getEmptyObject();
        try {
            newEss = http.getAuthorByURL(ess.getUrl(), newEss);
        } catch (IOException ex) {
            Log.e(DEBUG_TAG, "Connection Error: " + ess.getUrl(), ex);

            return;

        } catch (SamlibParseException ex) {
            Log.e(DEBUG_TAG, "Error parsing url: " + ess.getUrl() + " skip update author ", ex);


        }
        Log.i(DEBUG_TAG, "Author: " + newEss.getName() + " - " + newEss.getBooks().size());


        if (ess.update(newEss)) {//we have update for the author

            Log.i(DEBUG_TAG, "We need update author: " + ess.getName() + " - " + ess.getId() + " : " + ess.getBooks().size());
            ctl.update(ess);
        } else {
            Log.e(DEBUG_TAG, "Constant Author");
        }


//        for (Book b : ess.getBooks()) {
//
//            Log.i(DEBUG_TAG, "url: " + b.getUri());
//            Log.i(DEBUG_TAG, "size: " + b.getSize());
//            Log.i(DEBUG_TAG, "upd: " + b.getUpdateDate());
//            Log.i(DEBUG_TAG,"desc: "+b.getDescription());
//
//        }
//
//        for (Book b : newEss.getBooks()) {
//
//            Log.i(DEBUG_TAG, "url: " + b.getUri());
//            Log.i(DEBUG_TAG, "size: " + b.getSize());
//            Log.i(DEBUG_TAG, "upd: " + b.getUpdateDate());
//            Log.i(DEBUG_TAG,"desc: "+b.getDescription());
//        }


    }

    public void runw(List<Author> list) {
        SQLController sql;
        try {
            sql = SQLController.getInstance(settings.getDataDirectoryPath());
        } catch (Exception e) {
            Log.e(DEBUG_TAG, "SQL Error", e);
            return;
        }
        AuthorController ctl = new AuthorController(sql);
        HttpClientController http = HttpClientController.getInstance(settings);


        for (Author a : list) {

            String url = a.getUrl();
            Author newA = ctl.getEmptyObject();


            try {
                newA = http.getAuthorByURL(url, newA);
            } catch (IOException ex) {
                Log.e(DEBUG_TAG, "Connection Error: " + url, ex);

                return;

            } catch (SamlibParseException ex) {
                Log.e(DEBUG_TAG, "Error parsing url: " + url + " skip update author ", ex);

                //++skippedAuthors;
                newA = a;
            }
            if (a.update(newA)) {//we have update for the author

                Log.i(DEBUG_TAG, "We need update author: " + a.getName() + " - " + a.getId() + " : " + a.getBooks().size());
                //ctl.update(a);
            } else {
                Log.e(DEBUG_TAG, "Constant Author");
            }


        }

    }
}
