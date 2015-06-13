package com.phicdy.filfeed.db;
  
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.phicdy.filfeed.rss.Article;
import com.phicdy.filfeed.rss.Feed;

public class DatabaseHelper extends SQLiteOpenHelper{
  
	public static final String DATABASE_NAME = "rss_manage";
	private static final int DATABASE_VERSION = 1;
	
    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //onCreate() is called when database is created
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createFeedsTableSQL =
                "create table " + Feed.TABLE_NAME + "(" +
                Feed.ID + " integer primary key autoincrement,"+
                Feed.TITLE + " text,"+
                Feed.URL + " text,"+
                Feed.FORMAT + " text," +
                Feed.SITE_URL + " text," +
                Feed.ICON_PATH + " text," +
                Feed.UNREAD_ARTICLE + " integer)";
        String createArticlesTableSQL =
                "create table articles(_id integer primary key autoincrement,"+
                "title text,"+
                "url text,"+
                "status text default "+ Article.UNREAD+","+
                "point text,"+
                "date text,"+
                "feedId integer,"+
                "foreign key(feedId) references feeds(_id))";
        String createFiltersTableSQL =
                "create table filters(_id integer primary key autoincrement,"+
                "feedId integer,"+
                "keyword text,"+
                "url text," +
                "title text,"+
                "foreign key(feedId) references feeds(_id))";
        String createPrioritiesTableSQL =
                "create table priorities(_id integer primary key autoincrement,"+
                "priorFeedId integer,"+
                "posteriorFeedId integer,"+
                "foreign key(priorFeedId) references feeds(_id),"+
                "foreign key(posteriorFeedId) references feeds(_id))";
                  
        db.execSQL(createFeedsTableSQL);
        db.execSQL(createArticlesTableSQL);
        db.execSQL(createFiltersTableSQL);
        db.execSQL(createPrioritiesTableSQL);
    }
      
    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            // Enable foreign key constraints
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }
  
    //onUpgrade() is called when database version changes
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
  
}