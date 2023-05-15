package com.ginxdroid.flamebrowseranddownloader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.ginxdroid.flamebrowseranddownloader.models.BookmarkItem;
import com.ginxdroid.flamebrowseranddownloader.models.HistoryItem;
import com.ginxdroid.flamebrowseranddownloader.models.HomePageItem;
import com.ginxdroid.flamebrowseranddownloader.models.QuickLinkModel;
import com.ginxdroid.flamebrowseranddownloader.models.ThemeModel;
import com.ginxdroid.flamebrowseranddownloader.models.UserPreferences;

import java.util.ArrayList;

public class DatabaseHandler extends SQLiteOpenHelper {
    private static DatabaseHandler instance = null;
    private static SQLiteDatabase readableDB, writableDB;

    private final String userPreferencesTBL="userPreferencesTBL";

    //UserPreferences Table columns
    private final String UP_KEY_ID="upId";
    private final String CURRENT_THEME_ID="uCurrentThemeID";
    private final String IS_DARK_WEB_UI="uIsDarkWebUI";
    private final String DARK_THEME = "tHDarkTheme";
    private final String HOME_PAGE_URL = "uHomePageURL";


    private final String differentThemesTBL="differentThemesTBL";

    //Themes table columns
    private final String D_TH_ID = "dTHId";
    private final String D_THEME_NAME = "dTHName";
    private final String D_TH_BG_COLOR = "dTHBGColor";
    private final String D_TH_ACCENT_COLOR = "dTHAccentColor";
    private final String D_TH_TYPE = "dTHType";
    private final String D_TH_ACCENT_TEXT_COLOR = "dTHAccentTC";
    private final String D_TH_TEXT_COLOR = "dTHTextColor";
    private final String D_TH_SECONDARY_TEXT_COLOR = "dTHSecondaryTextColor";


    private final String quickLinksTBL="quickLinksTBL";
    //Quick links table columns
    private final String QL_KEY_ID = "qLId";
    private final String QL_ITEM_FAVICON_PATH = "qLItemFaviconPath";
    private final String QL_ITEM_TITLE = "qLItemTitle";
    private final String QL_ITEM_URL = "qLItemURL";
    private final String QL_VISIBLE_POSITION = "qLVisiblePosition";

    private final String homePagesTBL="homePagesTBL";
    //Home page table columns

    private final String HP_KEY_ID = "hPId";
    private final String HP_ITEM_FAVICON_PATH = "hPItemFaviconPath";
    private final String HP_ITEM_TITLE = "hPItemTitle";
    private final String HP_ITEM_URL = "hPItemURL";

    private final String historyTBL="historyTBL";
    //History table columns

    private final String H_KEY_ID = "hId";
    private final String H_ITEM_FAVICON_PATH = "hItemFaviconPath";
    private final String H_ITEM_TITLE = "hItemTitle";
    private final String H_ITEM_URL = "hItemURL";
    private final String H_ITEM_DATE = "hItemDate";
    private final String H_ITEM_TYPE = "hItemType";

    private final String bookmarksTbl="bookmarksTBL";
    //Bookmark table columns
    private final String B_KEY_ID = "bId";
    private final String B_ITEM_FAVICON_PATH = "bItemFaviconPath";
    private final String B_ITEM_TITLE = "bItemTitle";
    private final String B_ITEM_URL = "bItemURL";



    public DatabaseHandler(Context context) {
        super(context, "flameDatabase", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        createTablesIfNotExists(sqLiteDatabase);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {

    }


    private void createQuickLinkTBL(SQLiteDatabase db)
    {
        String CREATE_QUICK_LINKS_TABLE = "CREATE TABLE IF NOT EXISTS "+quickLinksTBL+"("
                +QL_KEY_ID+" INTEGER PRIMARY KEY,"+QL_ITEM_FAVICON_PATH+" TEXT,"+
                QL_ITEM_TITLE+" TEXT,"+QL_ITEM_URL+" TEXT,"+QL_VISIBLE_POSITION+" INTEGER);";

        db.execSQL(CREATE_QUICK_LINKS_TABLE);
    }



    public void addQuickLinkItem(QuickLinkModel quickLinkModel)
    {
        ContentValues values = new ContentValues();
        values.put(QL_ITEM_TITLE,quickLinkModel.getQlTitle());
        values.put(QL_ITEM_FAVICON_PATH,quickLinkModel.getQlFaviconPath());
        values.put(QL_ITEM_URL,quickLinkModel.getQlURL());
        values.put(QL_VISIBLE_POSITION,quickLinkModel.getQlVisiblePosition());

        //insert our model
        writableDB.insert(quickLinksTBL,null,values);
    }


    public boolean checkNotContainsFaviconInQuickLinks(String faviconPath)
    {
        Cursor cursor = readableDB.query(quickLinksTBL, new String[]{QL_KEY_ID},QL_ITEM_FAVICON_PATH + "=?",
                new String[]{faviconPath},null,null,null,null);

        if(cursor.moveToFirst())
        {
            cursor.close();
            return false;
        } else {
            cursor.close();
            return true;
        }
    }

    public boolean checkNotContainsFaviconInHomePages(String faviconPath)
    {
        Cursor cursor = readableDB.query(homePagesTBL, new String[]{HP_KEY_ID},HP_ITEM_FAVICON_PATH + "=?",
                new String[]{faviconPath},null,null,null,null);

        if(cursor.moveToFirst())
        {
            cursor.close();
            return false;
        } else {
            cursor.close();
            return true;
        }
    }

    public boolean checkNotContainsFaviconInHistory(String faviconPath)
    {
        Cursor cursor = readableDB.query(historyTBL, new String[]{H_KEY_ID},H_ITEM_FAVICON_PATH + "=?",
                new String[]{faviconPath},null,null,null,null);

        if(cursor.moveToFirst())
        {
            cursor.close();
            return false;
        } else {
            cursor.close();
            return true;
        }
    }

    public boolean checkNotContainsFaviconInBookmarks(String faviconPath)
    {
        Cursor cursor = readableDB.query(bookmarksTbl, new String[]{B_KEY_ID},B_ITEM_FAVICON_PATH + "=?",
                new String[]{faviconPath},null,null,null,null);

        if(cursor.moveToFirst())
        {
            cursor.close();
            return false;
        } else {
            cursor.close();
            return true;
        }
    }



    public static synchronized DatabaseHandler getInstance(Context context)
    {
        if (instance == null)
        {
            instance = new DatabaseHandler(context.getApplicationContext());
            readableDB = instance.getReadableDatabase();
            writableDB = instance.getWritableDatabase();
        }

        return instance;
    }

    private void createTablesIfNotExists(SQLiteDatabase db)
    {
        createUserPreferencesTbl(db);
        createDifferentThemesTbl(db);
        createQuickLinkTBL(db);
        createHomePagesTbl(db);
        createHistoryTbl(db);
        createBookmarksTbl(db);
    }



    //CRUD: CREATE, READ, UPDATE and DELETE operations

    private void createBookmarksTbl(SQLiteDatabase db)
    {
        String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS "+ bookmarksTbl +"("+B_KEY_ID+" INTEGER PRIMARY KEY,"+
                B_ITEM_FAVICON_PATH+" TEXT,"+B_ITEM_TITLE+" TEXT,"+B_ITEM_URL+" TEXT);";
        db.execSQL(CREATE_TABLE);
    }

    private void createHistoryTbl(SQLiteDatabase db)
    {
        String CREATE_HISTORY_TABLE = "CREATE TABLE IF NOT EXISTS "+ historyTBL +"("+H_KEY_ID+" INTEGER PRIMARY KEY,"+
                H_ITEM_FAVICON_PATH+" TEXT,"+H_ITEM_TITLE+" TEXT,"+H_ITEM_URL+" TEXT,"+
                H_ITEM_TYPE+" INTEGER,"+H_ITEM_DATE+" TEXT);";
        db.execSQL(CREATE_HISTORY_TABLE);
    }


    private void createUserPreferencesTbl(SQLiteDatabase db)
    {
        String CREATE_USER_PREFERENCES_TABLE = "CREATE TABLE IF NOT EXISTS "+ userPreferencesTBL +"("+UP_KEY_ID+" INTEGER PRIMARY KEY,"+
                CURRENT_THEME_ID+" INTEGER,"+IS_DARK_WEB_UI+" INTEGER,"+DARK_THEME+" INTEGER,"+HOME_PAGE_URL+" TEXT);";

        db.execSQL(CREATE_USER_PREFERENCES_TABLE);
    }

    private void createHomePagesTbl(SQLiteDatabase db)
    {
        String CREATE_HP_TABLE = "CREATE TABLE IF NOT EXISTS "+ homePagesTBL +"("+HP_KEY_ID+" INTEGER PRIMARY KEY,"+
                HP_ITEM_FAVICON_PATH+" TEXT,"+HP_ITEM_TITLE+" TEXT,"+HP_ITEM_URL+" TEXT);";

        db.execSQL(CREATE_HP_TABLE);
    }

    public boolean checkContainsBookmarkItem(String urlString)
    {
        Cursor cursor = readableDB.query(bookmarksTbl, new String[]{B_KEY_ID},B_ITEM_URL + "=?",new String[]{urlString},
                null,null,null,null);

        if(cursor.moveToFirst())
        {
            cursor.close();
            return true;
        } else {
            cursor.close();
            return false;
        }
    }

    public boolean checkNotContainsQuickLinks(String urlString)
    {
        Cursor cursor = readableDB.query(quickLinksTBL, new String[]{QL_KEY_ID},QL_ITEM_URL + "=?",new String[]{urlString},
                null,null,null,null);

        if(cursor.moveToFirst())
        {
            cursor.close();
            return false;
        } else {
            cursor.close();
            return true;
        }
    }

    public boolean checkNotContainsHomePages(String urlString)
    {
        Cursor cursor = readableDB.query(homePagesTBL, new String[]{HP_KEY_ID},HP_ITEM_URL + "=?",new String[]{urlString},
                null,null,null,null);

        if(cursor.moveToFirst())
        {
            cursor.close();
            return false;
        } else {
            cursor.close();
            return true;
        }
    }

    public void updateQLName(String qlName, int qlKeyId)
    {
        ContentValues values = new ContentValues();
        values.put(QL_ITEM_TITLE, qlName);

        writableDB.update(quickLinksTBL, values, QL_KEY_ID + "=?",new String[]{String.valueOf(qlKeyId)});
    }

    public void updateHomePageURL(String urlString)
    {
        ContentValues values = new ContentValues();
        values.put(HOME_PAGE_URL, urlString);

        writableDB.update(userPreferencesTBL, values, UP_KEY_ID + "=?",new String[]{String.valueOf(1)});
    }

    public void addHomePageItem(HomePageItem homePageItem)
    {
        ContentValues values = new ContentValues();
        values.put(HP_ITEM_TITLE, homePageItem.getHpTitle());
        values.put(HP_ITEM_URL, homePageItem.getHpURL());
        values.put(HP_ITEM_FAVICON_PATH, homePageItem.getHpFaviconPath());

        writableDB.insert(homePagesTBL,null,values);
    }

    public void addBookMarkItem(BookmarkItem bookmarkItem)
    {
        ContentValues values = new ContentValues();
        values.put(B_ITEM_TITLE, bookmarkItem.getBTitle());
        values.put(B_ITEM_FAVICON_PATH, bookmarkItem.getBFaviconPath());
        values.put(B_ITEM_URL, bookmarkItem.getBURL());

        writableDB.insert(bookmarksTbl,null,values);
    }

    public ArrayList<Integer> getAllHomePageItemsIDs()
    {
        ArrayList<Integer> resultList = new ArrayList<>();

        Cursor cursor = readableDB.query(homePagesTBL,new String[]{HP_KEY_ID},null,
                null,null,null,null);

        while (cursor.moveToNext())
        {
            resultList.add(cursor.getInt(cursor.getColumnIndexOrThrow(HP_KEY_ID)));
        }

        cursor.close();

        return resultList;
    }

    public ArrayList<Integer> getAllBookmarkItemsIDs()
    {
        ArrayList<Integer> resultList = new ArrayList<>();

        Cursor cursor = readableDB.query(bookmarksTbl,new String[]{B_KEY_ID},null,
                null,null,null,null);

        while (cursor.moveToNext())
        {
            resultList.add(cursor.getInt(cursor.getColumnIndexOrThrow(B_KEY_ID)));
        }

        cursor.close();

        return resultList;
    }

    public ArrayList<Integer> getAllHomePageItemsIDsWithTitle(String title)
    {
        ArrayList<Integer> resultList = new ArrayList<>();

        Cursor cursor = readableDB.query(homePagesTBL,new String[]{HP_KEY_ID},HP_ITEM_TITLE+" LIKE ?",
                new String[]{"%"+title+"%"},null,null,HP_KEY_ID + " DESC");

        while (cursor.moveToNext())
        {
            resultList.add(cursor.getInt(cursor.getColumnIndexOrThrow(HP_KEY_ID)));
        }

        cursor.close();

        return resultList;
    }

    public ArrayList<Integer> getAllBookmarkIDsWithTitle(String title)
    {
        ArrayList<Integer> resultList = new ArrayList<>();

        Cursor cursor = readableDB.query(bookmarksTbl,new String[]{B_KEY_ID},B_ITEM_TITLE+" LIKE ?",
                new String[]{"%"+title+"%"},null,null,B_KEY_ID + " DESC");

        while (cursor.moveToNext())
        {
            resultList.add(cursor.getInt(cursor.getColumnIndexOrThrow(B_KEY_ID)));
        }

        cursor.close();

        return resultList;
    }

    public BookmarkItem getBookmarkItem(int id)
    {
        Cursor cursor = readableDB.query(bookmarksTbl,new String[]{B_KEY_ID,B_ITEM_FAVICON_PATH,B_ITEM_TITLE,B_ITEM_URL},B_KEY_ID+"=?",
                new String[]{String.valueOf(id)},null,null,null);

        BookmarkItem bookmarkItem = new BookmarkItem();

        cursor.moveToFirst();
        bookmarkItem.setBKeyId(cursor.getInt(cursor.getColumnIndexOrThrow(B_KEY_ID)));
        bookmarkItem.setBTitle(cursor.getString(cursor.getColumnIndexOrThrow(B_ITEM_TITLE)));
        bookmarkItem.setBURL(cursor.getString(cursor.getColumnIndexOrThrow(B_ITEM_URL)));
        bookmarkItem.setBFaviconPath(cursor.getString(cursor.getColumnIndexOrThrow(B_ITEM_FAVICON_PATH)));

        cursor.close();
        return bookmarkItem;
    }

    public int getBookmarkItemId(String urlString)
    {
        Cursor cursor = readableDB.query(bookmarksTbl,new String[]{B_KEY_ID},B_ITEM_URL+"=?",
                new String[]{urlString},null,null,null);

        cursor.moveToFirst();
        int result = cursor.getInt(cursor.getColumnIndexOrThrow(B_KEY_ID));
        cursor.close();
        return result;
    }

    public void deleteBookmarkItem(int id)
    {
        writableDB.delete(bookmarksTbl,B_KEY_ID + "=?",new String[]{String.valueOf(id)});
    }

    public HomePageItem getHomePageItem(int id)
    {
        Cursor cursor = readableDB.query(homePagesTBL,new String[]{HP_ITEM_TITLE,HP_ITEM_URL,HP_ITEM_FAVICON_PATH},HP_KEY_ID+"=?",
                new String[]{String.valueOf(id)},null,null,null);

        HomePageItem homePageItem = new HomePageItem();

        cursor.moveToFirst();
        homePageItem.setHpFaviconPath(cursor.getString(cursor.getColumnIndexOrThrow(HP_ITEM_FAVICON_PATH)));
        homePageItem.setHpTitle(cursor.getString(cursor.getColumnIndexOrThrow(HP_ITEM_TITLE)));
        homePageItem.setHpURL(cursor.getString(cursor.getColumnIndexOrThrow(HP_ITEM_URL)));

        cursor.close();
        return homePageItem;
    }

    public void deleteHomePageItem(int id)
    {
        writableDB.delete(homePagesTBL,HP_KEY_ID + "=?",new String[]{String.valueOf(id)});
    }

    public int getUserPreferencesCount()
    {
        Cursor cursor = readableDB.query(userPreferencesTBL, new String[]{UP_KEY_ID},null,null,null,null,
                null,null);

        int count = cursor.getCount();
        cursor.close();

        return count;
    }

    private void createDifferentThemesTbl(SQLiteDatabase db)
    {
        String CREATE_THEMES_TABLE = "CREATE TABLE IF NOT EXISTS "+differentThemesTBL+"("+D_TH_ID+" INTEGER PRIMARY KEY AUTOINCREMENT,"
                +D_THEME_NAME+" TEXT,"+D_TH_BG_COLOR+" TEXT,"+D_TH_ACCENT_COLOR+" TEXT,"+D_TH_TYPE+" INTEGER,"+D_TH_ACCENT_TEXT_COLOR+" TEXT,"
                +D_TH_TEXT_COLOR+" TEXT,"+D_TH_SECONDARY_TEXT_COLOR+" TEXT);";

        db.execSQL(CREATE_THEMES_TABLE);

        insertDifferentThemes(db);
    }

    private void insertDifferentThemes(SQLiteDatabase db)
    {
        db.delete(differentThemesTBL, null, null);

        insertThemeModel(db,1,"Default Light","#FFFFFF","#E60023",
                0,"#FFFFFF","#de000000","#99000000");

        insertThemeModel(db,2,"Default Dark","#121212","#7cabf7",
                1,"#121212","#deFFFFFF","#99FFFFFF");

        insertThemeModel(db,3,"Light-Blue","#FFFFFF","#1a73e8",
                0,"#FFFFFF","#de000000","#99000000");

        insertThemeModel(db,4,"Light-Orange","#FFFFFF","#ff9118",
                0,"#FFFFFF","#de000000","#99000000");

        insertThemeModel(db,5,"Dark-Yellow","#121212","#ffeb3b",
                1,"#121212","#deFFFFFF","#99FFFFFF");

        insertThemeModel(db,6,"Dark-Light Blue","#121212","#03a9f4",
                1,"#121212","#deFFFFFF","#99FFFFFF");

        insertThemeModel(db,7,"Dark-Pink","#121212","#FF4081",
                1,"#121212","#deFFFFFF","#99FFFFFF");

        insertThemeModel(db,8,"Dark-Orange","#121212","#ff9118",
                1,"#121212","#deFFFFFF","#99FFFFFF");

        insertThemeModel(db,9,"Light-Pink","#FFFFFF","#FF4081",
                0,"#FFFFFF","#de000000","#99000000");

        insertThemeModel(db,10,"Light-Light Blue","#FFFFFF","#03a9f4",
                0,"#FFFFFF","#de000000","#99000000");

        insertThemeModel(db,11,"Light-Tropical Rain Forest","#FFFFFF","#01875f",
                0,"#FFFFFF","#de000000","#99000000");

        insertThemeModel(db,12,"Light-Purple","#FFFFFF","#FF6200EE",
                0,"#FFFFFF","#de000000","#99000000");

        insertThemeModel(db,13,"Dark-Purple","#121212","#FFBB86FC",
                1,"#121212","#deFFFFFF","#99FFFFFF");
    }

    private void insertThemeModel(SQLiteDatabase db, int keyID, String name, String bgColor, String accentColor, int type,
                                  String accentTextColor, String textColor, String secondaryTextColor)
    {
        ContentValues values = new ContentValues();
        values.put(D_TH_ID,keyID);
        values.put(D_THEME_NAME,name);
        values.put(D_TH_BG_COLOR,bgColor);
        values.put(D_TH_ACCENT_COLOR,accentColor);
        values.put(D_TH_TYPE,type);
        values.put(D_TH_ACCENT_TEXT_COLOR,accentTextColor);
        values.put(D_TH_TEXT_COLOR,textColor);
        values.put(D_TH_SECONDARY_TEXT_COLOR,secondaryTextColor);

        //Insert row
        db.insert(differentThemesTBL, null, values);
    }

    public void addUserPreferences(UserPreferences userPreferences)
    {
        //truncate existing user preferences
        writableDB.delete(userPreferencesTBL,null,null);

        ContentValues values = new ContentValues();
        values.put(UP_KEY_ID, userPreferences.getUpKeyId());
        values.put(CURRENT_THEME_ID, userPreferences.getCurrentThemeID());
        values.put(IS_DARK_WEB_UI, userPreferences.getIsDarkWebUI());
        values.put(DARK_THEME, userPreferences.getDarkTheme());
        values.put(HOME_PAGE_URL, userPreferences.getHomePageURL());

        writableDB.insert(userPreferencesTBL,null,values);

    }
    
    public String getHomePageURL()
    {
        Cursor cursor = readableDB.query(userPreferencesTBL,new String[]{HOME_PAGE_URL},UP_KEY_ID+"=?",
                new String[]{String.valueOf(1)},null,null,null,null);

        cursor.moveToFirst();
        String homePageURL = cursor.getString(cursor.getColumnIndexOrThrow(HOME_PAGE_URL));
        cursor.close();

        return homePageURL;
    }


    public ThemeModel getThemeModel(int themeKeyId)
    {
        ThemeModel themeModel = new ThemeModel();

        Cursor cursor = readableDB.query(differentThemesTBL,new String[]{D_TH_ACCENT_COLOR, D_TH_ACCENT_TEXT_COLOR},D_TH_ID+"=?",
                new String[]{String.valueOf(themeKeyId)}, null, null, null);

        cursor.moveToFirst();

        themeModel.setThemeAccentColor(cursor.getString(cursor.getColumnIndexOrThrow(D_TH_ACCENT_COLOR)));
        themeModel.setThemeAccentTextColor(cursor.getString(cursor.getColumnIndexOrThrow(D_TH_ACCENT_TEXT_COLOR)));

        cursor.close();

        return themeModel;
    }

    public int getCurrentThemeId()
    {
        Cursor cursor = readableDB.query(userPreferencesTBL, new String[]{CURRENT_THEME_ID},UP_KEY_ID+"=?",
                new String[]{String.valueOf(1)}, null, null, null);

        if(cursor.moveToFirst())
        {
            int currentThemeId = cursor.getInt((cursor.getColumnIndexOrThrow(CURRENT_THEME_ID)));
            cursor.close();
            return currentThemeId;
        } else {
            cursor.close();
            return 1;
        }
    }

    public int getCurrentThemeType()
    {
        try {
            Cursor cursor = readableDB.query(userPreferencesTBL, new String[]{DARK_THEME},UP_KEY_ID+"=?",
                    new String[]{String.valueOf(1)},null,null,null,null);
            cursor.moveToFirst();
            int result = cursor.getInt(cursor.getColumnIndexOrThrow(DARK_THEME));
            cursor.close();
            return result;
        }catch (Exception e)
        {
            return 2;
        }
    }

    //change theme type
    public void changeTheme(int themeType)
    {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DARK_THEME,themeType);

        //update userPreferences
        writableDB.update(userPreferencesTBL,contentValues,UP_KEY_ID+"=?",new String[]{String.valueOf(1)});
    }




    public void updateCurrentThemeID(int currentThemeID)
    {
        ContentValues values = new ContentValues();
        values.put(CURRENT_THEME_ID, currentThemeID);

        //update row
        writableDB.update(userPreferencesTBL, values, UP_KEY_ID + "=?",
                new String[]{String.valueOf(1)});

    }

    public int getDarkWebUI()
    {
        Cursor cursor = readableDB.query(userPreferencesTBL, new String[]{IS_DARK_WEB_UI}
                , UP_KEY_ID + "=?",
                new String[]{String.valueOf(1)}, null, null, null
        );

        cursor.moveToFirst();
        int clearStatus = cursor.getInt(cursor.getColumnIndexOrThrow(IS_DARK_WEB_UI));

        cursor.close();

        return clearStatus;

    }

    public void updateIsDarkWebUI(int value)
    {
        ContentValues values = new ContentValues();
        values.put(IS_DARK_WEB_UI, value);

        //update row
        writableDB.update(userPreferencesTBL, values, UP_KEY_ID + "=?",
                new String[]{String.valueOf(1)});
    }

    //Get isDarkTheme or not
    public boolean isDarkTheme()
    {
        try {
            Cursor cursor = readableDB.query(userPreferencesTBL,new String[] {CURRENT_THEME_ID},UP_KEY_ID+"=?",
                        new String[]{String.valueOf(1)},null,null,null,null);

            cursor.moveToFirst();
            int currentThemeId = cursor.getInt(cursor.getColumnIndexOrThrow(CURRENT_THEME_ID));
            cursor.close();

            return isDarkThemeModel(currentThemeId);

        }catch (Exception e)
        {
            return false;
        }
    }



    private boolean isDarkThemeModel(int currentThemeId)
    {
        try {
            Cursor cursor = readableDB.query(differentThemesTBL,new String[]{D_TH_TYPE}, D_TH_ID+"=?",
                    new String[]{String.valueOf(currentThemeId)},null,null,null,null);

            cursor.moveToFirst();
            int type = cursor.getInt(cursor.getColumnIndexOrThrow(D_TH_TYPE));
            cursor.close();

            return type == 1;
        }catch (Exception e)
        {
            return false;
        }
    }

    public QuickLinkModel getQuickLinkModel(int id)
    {
        Cursor cursor = readableDB.query(quickLinksTBL,new String[]{QL_KEY_ID,QL_ITEM_FAVICON_PATH,QL_ITEM_TITLE,QL_ITEM_URL},
                QL_KEY_ID+"=?",new String[]{String.valueOf(id)},null,null,null,null);
        QuickLinkModel quickLinkModel = new QuickLinkModel();
        cursor.moveToFirst();
        quickLinkModel.setQlKeyId(cursor.getInt(cursor.getColumnIndexOrThrow(QL_KEY_ID)));
        quickLinkModel.setQlTitle(cursor.getString(cursor.getColumnIndexOrThrow(QL_ITEM_TITLE)));
        quickLinkModel.setQlFaviconPath(cursor.getString(cursor.getColumnIndexOrThrow(QL_ITEM_FAVICON_PATH)));
        quickLinkModel.setQlURL(cursor.getString(cursor.getColumnIndexOrThrow(QL_ITEM_URL)));

        cursor.close();

        return quickLinkModel;
    }

    public ArrayList<Integer> getAllQuickLinkItemsIDs() {
        ArrayList<Integer> quickLinksList = new ArrayList<>();

        Cursor cursor = readableDB.query(quickLinksTBL,new String[]{QL_KEY_ID},
                null,null,null,null,QL_VISIBLE_POSITION + " ASC",null);

        while (cursor.moveToNext())
        {
            //add ql item to list
            quickLinksList.add(cursor.getInt(cursor.getColumnIndexOrThrow(QL_KEY_ID)));
        }

        cursor.close();

        return quickLinksList;
    }

    public void deleteQuickLinkItem(int id) {
        writableDB.delete(quickLinksTBL,QL_KEY_ID + "=?",new String[]{String.valueOf(id)});
    }

    public void addHistoryItem(HistoryItem historyItem)
    {
        ContentValues contentValues = new ContentValues();
        contentValues.put(H_ITEM_FAVICON_PATH,historyItem.getHiFaviconPath());
        contentValues.put(H_ITEM_TITLE,historyItem.getHiTitle());
        contentValues.put(H_ITEM_URL,historyItem.getHiURL());
        contentValues.put(H_ITEM_DATE,historyItem.getHiDate());
        contentValues.put(H_ITEM_TYPE,historyItem.getHiType());
        writableDB.insert(historyTBL,null,contentValues);
    }

    public HistoryItem getHistoryItem(int id)
    {
        Cursor cursor = readableDB.query(historyTBL, new String[]{H_KEY_ID,H_ITEM_FAVICON_PATH,H_ITEM_TITLE,H_ITEM_URL,
        H_ITEM_DATE,H_ITEM_TYPE},H_KEY_ID + "=?",new String[]{String.valueOf(id)},null,null,null,null);

        HistoryItem historyItem = new HistoryItem();
        cursor.moveToFirst();
        historyItem.setHiKeyId(cursor.getInt((cursor.getColumnIndexOrThrow(H_KEY_ID))));
        historyItem.setHiFaviconPath(cursor.getString(cursor.getColumnIndexOrThrow(H_ITEM_FAVICON_PATH)));
        historyItem.setHiTitle(cursor.getString(cursor.getColumnIndexOrThrow(H_ITEM_TITLE)));
        historyItem.setHiURL(cursor.getString(cursor.getColumnIndexOrThrow(H_ITEM_URL)));
        historyItem.setHiDate(cursor.getString(cursor.getColumnIndexOrThrow(H_ITEM_DATE)));
        historyItem.setHiType(cursor.getInt(cursor.getColumnIndexOrThrow(H_ITEM_TYPE)));
        cursor.close();
        return historyItem;
    }

    public int getHistoryItemType(int id)
    {
        Cursor cursor = readableDB.query(historyTBL, new String[]{H_ITEM_TYPE},H_KEY_ID + "=?",new String[]{String.valueOf(id)},
                null,null,null,null);
        int type;
        cursor.moveToFirst();
        type = cursor.getInt(cursor.getColumnIndexOrThrow(H_ITEM_TYPE));
        cursor.close();
        return type;
    }

    public ArrayList<Integer> getAllHistoryItemsIDs()
    {
        ArrayList<Integer> historyItemsIDsList = new ArrayList<>();
        ArrayList<String> uniqueHistoryItemsDates = getUniqueHistoryItemsDates();

        for(int i = 0; i < uniqueHistoryItemsDates.size(); i++)
        {
            String date = uniqueHistoryItemsDates.get(i);
            int id = getUniqueIDHistoryItemsDates(date);

            if(id != -1)
            {
                historyItemsIDsList.add(id);
                historyItemsIDsList.addAll(getHistoryItemsByDate(date));
            } else {
                deleteHistoryItemWithDate(date);
            }
        }

        return historyItemsIDsList;
    }

    public boolean checkNotContainsFaviconInHistoryMore(String faviconPath,int keyID)
    {

        Cursor cursor = readableDB.query(historyTBL, new String[]{H_KEY_ID
                }
                , H_ITEM_FAVICON_PATH + "=? AND "+H_KEY_ID+" !=?",
                new String[]{String.valueOf(faviconPath),String.valueOf(keyID)}, null, null, null, null
        );


        if(cursor.moveToFirst())
        {
            cursor.close();
            return false;
        }
        else
        {
            cursor.close();
            return true;
        }
    }

    public ArrayList<Integer> getAllHistoryItemsIDsWithTitle(String title)
    {
        ArrayList<Integer> historyItemsIDsList = new ArrayList<>();
        ArrayList<String> uniqueHistoryItemsDates = getUniqueHistoryItemsDates();
        for(int i = 0; i < uniqueHistoryItemsDates.size(); i++)
        {
            ArrayList<Integer> prefetch = getHistoryItemsByDateWithTitle(uniqueHistoryItemsDates.get(i),title);
            if(prefetch.size() > 0)
            {
                historyItemsIDsList.add(getUniqueIDHistoryItemsDateWithTitle(uniqueHistoryItemsDates.get(i)));
                historyItemsIDsList.addAll(prefetch);
            }
        }

        return historyItemsIDsList;
    }

    private ArrayList<Integer> getHistoryItemsByDate(String historyItemDate)
    {
        ArrayList<Integer> historyItemsByDateAL = new ArrayList<>();
        Cursor cursor = readableDB.query(historyTBL,new String[]{H_KEY_ID},H_ITEM_DATE + "=? AND "+H_ITEM_TYPE+"!=?",
                new String[]{historyItemDate,"0"},null,null,H_KEY_ID + " DESC",null);
        while (cursor.moveToNext())
        {
            historyItemsByDateAL.add(cursor.getInt(cursor.getColumnIndexOrThrow(H_KEY_ID)));
        }
        cursor.close();
        return historyItemsByDateAL;
    }

    public int getHistoryItemsByDateSize(String historyItemDate)
    {
        Cursor cursor = readableDB.query(historyTBL,new String[]{H_KEY_ID},H_ITEM_DATE + "=? AND "+H_ITEM_TYPE+"!=?",
                new String[]{historyItemDate,"0"},null,null,H_KEY_ID + " DESC",null);
        int count = cursor.getCount();
        cursor.close();
        return count;
    }

    public int getHistoryItemIdWithDate(String historyItemDate)
    {
        Cursor cursor = readableDB.query(historyTBL,new String[]{H_KEY_ID},H_ITEM_DATE + "=? AND "+H_ITEM_TYPE+"==?",
                new String[]{historyItemDate,"0"},null,null,null,null);
        cursor.moveToFirst();
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(H_KEY_ID));
        cursor.close();
        return id;
    }

    private ArrayList<Integer> getHistoryItemsByDateWithTitle(String historyItemDate, String itemTitle)
    {
        ArrayList<Integer> historyItemsByDateAL = new ArrayList<>();
        Cursor cursor = readableDB.query(historyTBL,new String[]{H_KEY_ID},H_ITEM_DATE + "=? AND "+H_ITEM_TYPE+"!=? AND "+H_ITEM_TITLE+" LIKE ?",
                new String[]{historyItemDate,"0","%"+itemTitle+"%"},null,null,H_KEY_ID +" DESC",null);

        while (cursor.moveToNext())
        {
            historyItemsByDateAL.add(cursor.getInt(cursor.getColumnIndexOrThrow(H_KEY_ID)));
        }
        cursor.close();
        return historyItemsByDateAL;
    }

    private ArrayList<String> getUniqueHistoryItemsDates()
    {
        ArrayList<String> uniqueHistoryItemsDates = new ArrayList<>();
        Cursor cursor = readableDB.query(true,historyTBL,new String[]{H_ITEM_DATE},null,
                null,null,null,H_KEY_ID +" DESC",null);
        while (cursor.moveToNext())
        {
            uniqueHistoryItemsDates.add(cursor.getString(cursor.getColumnIndexOrThrow(H_ITEM_DATE)));
        }
        cursor.close();
        return uniqueHistoryItemsDates;
    }

    private int getUniqueIDHistoryItemsDates(String historyItemDate)
    {
        Cursor cursor = readableDB.query(historyTBL,new String[]{H_KEY_ID},H_ITEM_DATE + "=? AND "+H_ITEM_TYPE+"=?",
                new String[]{historyItemDate,"0"},null,null,null,null);
        boolean result = cursor.moveToFirst();
        if(result)
        {
            return cursor.getInt(cursor.getColumnIndexOrThrow(H_KEY_ID));
        }
        cursor.close();

        return -1;
    }

    private int getUniqueIDHistoryItemsDateWithTitle(String historyItemDate)
    {
        Cursor cursor = readableDB.query(historyTBL,new String[]{H_KEY_ID},H_ITEM_DATE + "=? AND "+H_ITEM_TYPE+"=?",
                new String[]{historyItemDate,"0"},null,null,null,null);
        cursor.moveToFirst();
        int uniqueKeyId = cursor.getInt(cursor.getColumnIndexOrThrow(H_KEY_ID));
        cursor.close();

        return uniqueKeyId;
    }

    public boolean checkNotContainsHistoryItem(String historyItemURL, String historyItemDate)
    {
        Cursor cursor = readableDB.query(historyTBL, new String[]{H_KEY_ID},H_ITEM_URL + "=? AND "+H_ITEM_DATE+"=?",
                new String[]{historyItemURL,historyItemDate},null,null,null,null);

        if(cursor.moveToFirst())
        {
            cursor.close();
            return false;
        } else {
            cursor.close();
            return true;
        }
    }

    public boolean checkNotContainsHistoryItemDate(String historyItemDate)
    {
        Cursor cursor = readableDB.query(historyTBL, new String[]{H_KEY_ID},H_ITEM_DATE + "=?",
                new String[]{historyItemDate},null,null,null,null);
        if(cursor.moveToFirst())
        {
            cursor.close();
            return false;
        } else {
            cursor.close();
            return true;
        }

    }

    public void deleteHistoryItem(int id)
    {
        writableDB.delete(historyTBL,H_KEY_ID + "=?",new String[]{String.valueOf(id)});
    }

    private void deleteHistoryItemWithDate(String date)
    {


        writableDB.delete(historyTBL, H_ITEM_DATE + "=?",
                new String[]{date});
    }

}


