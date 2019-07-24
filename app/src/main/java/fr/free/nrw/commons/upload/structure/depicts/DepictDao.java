package fr.free.nrw.commons.upload.structure.depicts;

import android.content.ContentProviderClient;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;

import dagger.Provides;

public class DepictDao {

    private final Provider<ContentProviderClient> clientProvider;

    @Inject
    public DepictDao(@Named("depicts") Provider<ContentProviderClient> clientProvider) {
        this.clientProvider = clientProvider;
    }

    public void save(Depiction depiction) {
        ContentProviderClient db = clientProvider.get();
        try {
            if (depiction.getContentUri() == null) {
                depiction.setContentUri(db.insert(DepictsContentProvider.BASE_URI, toContentValues(depiction)));
            } else {
                db.update(depiction.getContentUri(), toContentValues(depiction), null, null);
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            db.release();
        }
    }

    /**
     * Find persisted depicts in database, based on its name.
     *
     * @param name Depiction name
     * @return depiction from database, or null if not found
     */

    @Nullable
    Depiction find(String name) {
        Cursor cursor = null;
        ContentProviderClient db = clientProvider.get();
        try {
            cursor = db.query(
                    DepictsContentProvider.BASE_URI,
                    Table.ALL_FIELDS,
                    Table.COLUMN_NAME + "=?",
                    new String[]{name},
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                return fromCursor(cursor);
            }
            db.release();
        } catch (RemoteException e) {
            // This feels lazy, but to hell with checked exceptions. :)
            throw new RuntimeException(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return null;
    }

    /**
     * Retrieve recently-used depictions, ordered by descending date.
     *
     * @return a list containing recent depicts
     */
    @NonNull
    List<String> recentDepicts(int limit) {
        List<String> items = new ArrayList<>();
        Cursor cursor = null;
        ContentProviderClient db = clientProvider.get();
        try {
            cursor = db.query(
                    DepictsContentProvider.BASE_URI,
                    Table.ALL_FIELDS,
                    null,
                    new String[]{},
                    Table.COLUMN_LAST_USED + "DESC");
            while (cursor != null && cursor.moveToNext()
                    && cursor.getPosition() < limit) {
                items.add(fromCursor(cursor).getName());
            }
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            db.release();
        }
        return items;
    }

    @NonNull
    Depiction fromCursor(Cursor cursor) {
        // Hardcoding column positions!
        return new Depiction(
                DepictsContentProvider.uriForId(cursor.getInt(cursor.getColumnIndex(DepictDao.Table.COLUMN_ID))),
                cursor.getString(cursor.getColumnIndex(DepictDao.Table.COLUMN_NAME)),
                new Date(cursor.getLong(cursor.getColumnIndex(DepictDao.Table.COLUMN_LAST_USED))),
                cursor.getInt(cursor.getColumnIndex(DepictDao.Table.COLUMN_TIMES_USED))
        );
    }

    private ContentValues toContentValues(Depiction depiction) {
        ContentValues cv = new ContentValues();
        cv.put(DepictDao.Table.COLUMN_NAME, depiction.getName());
        cv.put(DepictDao.Table.COLUMN_LAST_USED, depiction.getLastUsed().getTime());
        cv.put(DepictDao.Table.COLUMN_TIMES_USED, depiction.getTimesUsed());
        return cv;
    }


    public static class Table {
        public static final String TABLE_NAME = "depicts";
        public static final String COLUMN_ID = "_id";
        static final String COLUMN_NAME = "name";
        static final String COLUMN_LAST_USED = "last_used";
        static final String COLUMN_TIMES_USED = "times_used";


        // NOTE! KEEP IN SAME ORDER AS THEY ARE DEFINED UP THERE. HELPS HARD CODE COLUMN INDICES.
        public static final String[] ALL_FIELDS = {
                COLUMN_ID,
                COLUMN_NAME,
                COLUMN_LAST_USED,
                COLUMN_TIMES_USED
        };

        static final String DROP_TABLE_STATEMENT = "DROP TABLE IF EXISTS " + TABLE_NAME;

        static final String CREATE_TABLE_STATEMENT = "CREATE TABLE " + TABLE_NAME + " ("
                + COLUMN_ID + " INTEGER PRIMARY KEY,"
                + COLUMN_NAME + " STRING,"
                + COLUMN_LAST_USED + " INTEGER,"
                + COLUMN_TIMES_USED + " INTEGER"
                + ");";

        public static void onCreate(SQLiteDatabase db) {
            db.execSQL(CREATE_TABLE_STATEMENT);
        }

        public static void onDelete(SQLiteDatabase db) {
            db.execSQL(DROP_TABLE_STATEMENT);
            onCreate(db);
        }

        public static void onUpdate(SQLiteDatabase db, int from, int to) {
            if (from == to) {
                return;
            }
            if (from < 4) {
                // doesn't exist yet
                from++;
                onUpdate(db, from, to);
                return;
            }
            if (from == 4) {
                // table added in version 5
                onCreate(db);
                from++;
                onUpdate(db, from, to);
                return;
            }
            if (from == 5) {
                from++;
                onUpdate(db, from, to);
                return;
            }
        }
    }
}