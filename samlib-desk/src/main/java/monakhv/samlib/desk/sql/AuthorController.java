package monakhv.samlib.desk.sql;

import monakhv.samlib.db.AbstractController;
import monakhv.samlib.db.SQLController;
import monakhv.samlib.db.entity.Author;
import monakhv.samlib.log.Log;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
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
 * 2/13/15.
 */
public class AuthorController implements AbstractController<Author> {
    private static final String DEBUG_TAG="AuthorController";
    private static final String TABLE=SQLController.TABLE_AUTHOR;
    private SQLController sql;
    public AuthorController(SQLController sql){
        this.sql=sql;

    }
    @Override
    public int update(Author author) {
        return 0;
    }

    @Override
    public long insert(Author author) {
        return 0;
    }

    @Override
    public int delete(Author author) {
        return 0;
    }


    @Override
    public List<Author> getAll() {
        return getAll(null,null);
    }

    public List<Author> getAll(String selection, String order ) {

        String statement= "SELECT * from "+TABLE;

        if (selection != null){
            statement +=" where "+selection;
        }
        if (order != null){
            statement += " ORDER BY "+order;
        }
        ResultSet rs;

        try {
            rs = sql.query(statement);
        } catch (SQLException e) {
            Log.e(DEBUG_TAG,"select error: "+statement,e);
            return null;
        }
        List<Author> res = new ArrayList<>();
        try {
            while(rs.next()){
                res.add(resultSetToAuthor(rs));
            }
        } catch (SQLException e) {
            Log.e(DEBUG_TAG, "rs next error: " + statement,e);
            res = null;
        }


        try {
            rs.close();
        } catch (SQLException e) {
            Log.e(DEBUG_TAG, "rs close error: " + statement,e);
        }


        return res;
    }

    @Override
    public Author getById(long id) {
        return null;
    }

    private static Author resultSetToAuthor(ResultSet rs){
        Author a = new Author();

        try {
            a.setId(rs.getInt(SQLController.COL_ID));
            a.setName(rs.getString(SQLController.COL_NAME));
            a.setUrl(rs.getString(SQLController.COL_URL));
            a.setUpdateDate(rs.getLong(SQLController.COL_mtime));
            a.setIsNew(rs.getInt(SQLController.COL_isnew)==1);
        } catch (SQLException e) {
            Log.e(DEBUG_TAG,"Author create error: ",e);
        }
        return a;
    }

}