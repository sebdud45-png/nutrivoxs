package com.nutrivox.app;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class LocalDatabase extends SQLiteOpenHelper {
    public LocalDatabase(Context context) {
        super(context, "nutrivox.db", null, 1);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE history(id INTEGER PRIMARY KEY AUTOINCREMENT, title TEXT NOT NULL, details TEXT NOT NULL, created_at INTEGER NOT NULL)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS history");
        onCreate(db);
    }

    public void add(String title, String details) {
        getWritableDatabase().execSQL(
                "INSERT INTO history(title,details,created_at) VALUES(?,?,?)",
                new Object[]{title, details, System.currentTimeMillis()}
        );
    }

    public List<String> history() {
        ArrayList<String> rows = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT title, details FROM history ORDER BY created_at DESC LIMIT 100", null);
        while (c.moveToNext()) rows.add(c.getString(0) + "\n" + c.getString(1));
        c.close();
        return rows;
    }
}
