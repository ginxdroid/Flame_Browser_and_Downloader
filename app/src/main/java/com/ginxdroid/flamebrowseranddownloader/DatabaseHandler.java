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
import com.ginxdroid.flamebrowseranddownloader.models.SearchEngineItem;
import com.ginxdroid.flamebrowseranddownloader.models.SearchItem;
import com.ginxdroid.flamebrowseranddownloader.models.SiteSettingsModel;
import com.ginxdroid.flamebrowseranddownloader.models.ThemeModel;
import com.ginxdroid.flamebrowseranddownloader.models.UserPreferences;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.DownloadTask;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialBindDownloadTask;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialCompletedTask;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialEight;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialFour;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialOne;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialSix;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialSixteen;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialThirtyTwo;
import com.ginxdroid.flamebrowseranddownloader.models.tasks.PartialTwo;

import java.util.ArrayList;

public class DatabaseHandler extends SQLiteOpenHelper {
    private static DatabaseHandler instance = null;
    private static SQLiteDatabase readableDB, writableDB;

    private final String downloadTasksTBL = "downloadTasksTBL";
    //downloadTasksTBL columns
    private final String KEY_ID = "dId";
    private final String FILE_NAME = "dFileName";
    private final String URL = "dURL";
    private final String DIR_PATH = "dDirPath";
    private final String TOTAL_BYTES = "dTotalBytes";
    private final String DOWNLOADED_BYTES = "dDownloadedBytes";
    private final String MIME_TYPE = "dMimeType";
    private final String CURRENT_STATUS = "dCurrentStatus";
    private final String CURRENT_PROGRESS = "dCurrentProgress";
    private final String DOWNLOAD_SPEED = "dDownloadSpeed";
    private final String TIME_LEFT = "dTimeLeft";
    private final String PAUSE_RESUME_SUPPORTED = "dPauseResumeSupported";
    private final String IS_PAUSE_RESUME_SUPPORTED = "dIsPauseResumeSupported";
    private final String CHUNK_MODE = "dChunkMode";
    private final String USER_AGENT_STRING = "dUserAgentString";
    private final String PAGE_URL = "dPageURL";
    private final String WHICH_ERROR = "dWhichError";
    private final String SEGMENTS_FOR_DOWNLOAD_TASK = "dSegmentsForDownloadTask";

    private final String TPB1 = "dTPB1";
    private final String TPB2 = "dTPB2";
    private final String TPB3 = "dTPB3";
    private final String TPB4 = "dTPB4";
    private final String TPB5 = "dTPB5";
    private final String TPB6 = "dTPB6";
    private final String TPB7 = "dTPB7";
    private final String TPB8 = "dTPB8";
    private final String TPB9 = "dTPB9";
    private final String TPB10 = "dTPB10";
    private final String TPB11 = "dTPB11";
    private final String TPB12 = "dTPB12";
    private final String TPB13 = "dTPB13";
    private final String TPB14 = "dTPB14";
    private final String TPB15 = "dTPB15";
    private final String TPB16 = "dTPB16";
    private final String TPB17 = "dTPB17";
    private final String TPB18 = "dTPB18";
    private final String TPB19 = "dTPB19";
    private final String TPB20 = "dTPB20";
    private final String TPB21 = "dTPB21";
    private final String TPB22 = "dTPB22";
    private final String TPB23 = "dTPB23";
    private final String TPB24 = "dTPB24";
    private final String TPB25 = "dTPB25";
    private final String TPB26 = "dTPB26";
    private final String TPB27 = "dTPB27";
    private final String TPB28 = "dTPB28";
    private final String TPB29 = "dTPB29";
    private final String TPB30 = "dTPB30";
    private final String TPB31 = "dTPB31";
    private final String TPB32 = "dTPB32";

    private final String TSS1 = "dTSS1";
    private final String TSS2 = "dTSS2";
    private final String TSS3 = "dTSS3";
    private final String TSS4 = "dTSS4";
    private final String TSS5 = "dTSS5";
    private final String TSS6 = "dTSS6";
    private final String TSS7 = "dTSS7";
    private final String TSS8 = "dTSS8";
    private final String TSS9 = "dTSS9";
    private final String TSS10 = "dTSS10";
    private final String TSS11 = "dTSS11";
    private final String TSS12 = "dTSS12";
    private final String TSS13 = "dTSS13";
    private final String TSS14 = "dTSS14";
    private final String TSS15 = "dTSS15";
    private final String TSS16 = "dTSS16";
    private final String TSS17 = "dTSS17";
    private final String TSS18 = "dTSS18";
    private final String TSS19 = "dTSS19";
    private final String TSS20 = "dTSS20";
    private final String TSS21 = "dTSS21";
    private final String TSS22 = "dTSS22";
    private final String TSS23 = "dTSS23";
    private final String TSS24 = "dTSS24";
    private final String TSS25 = "dTSS25";
    private final String TSS26 = "dTSS26";
    private final String TSS27 = "dTSS27";
    private final String TSS28 = "dTSS28";
    private final String TSS29 = "dTSS29";
    private final String TSS30 = "dTSS30";
    private final String TSS31 = "dTSS31";
    private final String TSS32 = "dTSS32";

    private final String completedTasksTBL = "completedTasksTBL";
    //completedTasksTBL columns
    private final String CDT_KEY_ID = "cDtKeyId";

    private final String recentSitesTBL = "recentSitesTBL";
    //Recent sites columns

    private final String RCT_SITE_URL = "rCTSiteURL";

    private final String siteSettingsTBL = "siteSettingsTBL";
    //Site settings table columns

    private final String SS_ID = "sId";
    private final String SS_JAVA_SCRIPT = "sJavaScript";
    private final String SS_LOCATION = "sLocation";
    private final String SS_COOKIES = "sCookies";
    private final String SS_SAVE_SITES_IN_HISTORY = "sSaveSitesInHistory";
    private final String SS_SAVE_SEARCH_HISTORY = "sSaveSearchHistory";
    private final String SS_IS_CHANGED = "sIsChanged";

    private final String userPreferencesTBL="userPreferencesTBL";

    //UserPreferences Table columns
    private final String UP_KEY_ID="upId";
    private final String CURRENT_THEME_ID="uCurrentThemeID";
    private final String IS_DARK_WEB_UI="uIsDarkWebUI";
    private final String DARK_THEME = "tHDarkTheme";
    private final String HOME_PAGE_URL = "uHomePageURL";
    private final String SEARCH_ENGINE_URL = "uSearchEngineURL";
    private final String IS_SAVE_RECENT_TABS = "uIsSaveRecentTabs";
    private final String BROWSER_TUTORIAL_INFO = "uBrowserTutorialInfo";

    private final String DOWNLOAD_PATH = "uDownloadPath";
    private final String AUTO_RESUME_STATUS = "uAutoResumeStatus";
    private final String SIMULTANEOUS_TASKS = "uSimultaneousTasks";
    private final String DEFAULT_SEGMENTS = "uDefaultSegments";
    private final String DIRECT_DOWNLOAD = "uDirectDownload";
    private final String SHOW_OPTIMIZATION = "uShowOptimization";


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

    private final String searchItemTBL = "searchItemTBL";
    //search table columns
    private final String S_KEY_ID = "sId";
    private final String S_ITEM_TITLE = "sItemTitle";



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

    private final String searchEnginesTBL="searchEnginesTBL";
    //Search engine table columns
    private final String SE_KEY_ID = "sEKetId";
    private final String SE_ITEM_TITLE = "sEItemTitle";
    private final String SE_ITEM_URL = "sEItemURL";
    private final String SE_IS_DEFAULT = "sEIsDefault";
    private final String SE_ITEM_IS_CURRENT = "sEItemIsCurrent";




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

    public void deleteSearchHistory()
    {
        writableDB.delete(searchItemTBL,null,null);
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

    public boolean checkNotContainsSearchItem(String keyWord)
    {
        Cursor cursor = readableDB.query(searchItemTBL, new String[]{S_KEY_ID},S_ITEM_TITLE + "=?",
                new String[]{keyWord},null,null,null,null);

        if(cursor.moveToFirst())
        {
            cursor.close();
            return false;
        } else {
            cursor.close();
            return true;
        }
    }

    public void addSearchItem(SearchItem searchItem)
    {
        ContentValues values = new ContentValues();
        values.put(S_ITEM_TITLE,searchItem.getSeItemTitle());

        writableDB.insert(searchItemTBL,null,values);
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
        createSearchEnginesTbl(db);
        createSearchItemsTbl(db);
        createSiteSettingsTbl(db);
        createRecentSitesTbl(db);
        createDownloadTasksTbl(db);
        createCompletedDownloadTasksTbl(db);
    }



    //CRUD: CREATE, READ, UPDATE and DELETE operations

    private void createDownloadTasksTbl(SQLiteDatabase db)
    {
        String CREATE_DOWNLOAD_TASKS_TABLE = "CREATE TABLE IF NOT EXISTS "+downloadTasksTBL+"("+KEY_ID+" INTEGER PRIMARY KEY AUTOINCREMENT,"+
                FILE_NAME + " TEXT," + URL + " TEXT," + DIR_PATH + " TEXT," + TOTAL_BYTES + " LONG,"+DOWNLOADED_BYTES + " LONG," +
                CURRENT_STATUS + " INTEGER," + CURRENT_PROGRESS + " INTEGER," + DOWNLOAD_SPEED + " TEXT,"+TIME_LEFT + " TEXT," +
                PAUSE_RESUME_SUPPORTED + " TEXT," + IS_PAUSE_RESUME_SUPPORTED + " INTEGER,"+CHUNK_MODE + " INTEGER,"+ USER_AGENT_STRING + " TEXT,"+
                PAGE_URL + " TEXT," + WHICH_ERROR + " TEXT," + SEGMENTS_FOR_DOWNLOAD_TASK + " INTEGER," +
                TPB1 + " Integer," + TPB2 + " Integer," + TPB3 + " Integer," + TPB4 + " Integer," + TPB5 + " Integer," + TPB6 + " Integer," +
                TPB7 + " Integer," + TPB8 + " Integer," + TPB9 + " Integer," + TPB10 + " Integer," + TPB11 + " Integer," + TPB12 + " Integer," +
                TPB13 + " Integer," + TPB14 + " Integer," + TPB15 + " Integer," + TPB16 + " Integer," + TPB17 + " Integer," + TPB18 + " Integer," +
                TPB19 + " Integer," + TPB20 + " Integer," + TPB21 + " Integer," + TPB22 + " Integer," + TPB23 + " Integer," + TPB24 + " Integer," + TPB25 + " Integer," +
                TPB26 + " Integer," + TPB27 + " Integer," + TPB28 + " Integer," + TPB29 + " Integer," + TPB30 + " Integer," + TPB31 + " Integer," + TPB32 + " Integer," +
                TSS1 + " LONG," + TSS2 + " LONG," + TSS3 + " LONG," + TSS4 + " LONG," + TSS5 + " LONG," + TSS6 + " LONG," + TSS7 + " LONG," + TSS8 + " LONG," +
                TSS9 + " LONG," + TSS10 + " LONG," + TSS11 + " LONG," + TSS12 + " LONG," + TSS13 + " LONG," + TSS14 + " LONG," + TSS15 + " LONG," + TSS16 + " LONG," +
                TSS17 + " LONG," + TSS18 + " LONG," + TSS19 + " LONG," + TSS20 + " LONG," + TSS21 + " LONG," + TSS22 + " LONG," + TSS23 + " LONG," + TSS24 + " LONG," +
                TSS25 + " LONG," + TSS26 + " LONG," + TSS27 + " LONG," + TSS28 + " LONG," + TSS29 + " LONG," + TSS30 + " LONG," + TSS31 + " LONG," + TSS32 + " LONG," +
                MIME_TYPE + " TEXT);";
        db.execSQL(CREATE_DOWNLOAD_TASKS_TABLE);
    }

    private void createCompletedDownloadTasksTbl(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + completedTasksTBL + "(" + CDT_KEY_ID + " INTEGER);";
        db.execSQL(CREATE_TABLE);
    }

    private void createRecentSitesTbl(SQLiteDatabase db) {
        String RCT_KEY_ID = "rCTKeyId";
        String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + recentSitesTBL + "(" + RCT_KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                RCT_SITE_URL + " Text);";
        db.execSQL(CREATE_TABLE);
    }

    private void createSiteSettingsTbl(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + siteSettingsTBL + "(" + SS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                SS_JAVA_SCRIPT + " INTEGER,"+SS_COOKIES+" INTEGER,"+SS_LOCATION+" INTEGER,"+SS_SAVE_SITES_IN_HISTORY+" INTEGER,"+
                SS_SAVE_SEARCH_HISTORY+" INTEGER,"+SS_IS_CHANGED+" INTEGER);";
        db.execSQL(CREATE_TABLE);
    }

    private void createSearchItemsTbl(SQLiteDatabase db) {
        String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " + searchItemTBL + "(" + S_KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                S_ITEM_TITLE + " TEXT);";
        db.execSQL(CREATE_TABLE);
    }

    private void createSearchEnginesTbl(SQLiteDatabase db)
    {
        String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS "+ searchEnginesTBL +"("+SE_KEY_ID+" INTEGER PRIMARY KEY AUTOINCREMENT,"+
                SE_ITEM_TITLE+" TEXT,"+SE_ITEM_URL+" TEXT,"+SE_IS_DEFAULT+" INTEGER,"+SE_ITEM_IS_CURRENT+" INTEGER);";
        db.execSQL(CREATE_TABLE);

        {
            SearchEngineItem searchEngineItem = new SearchEngineItem();
            searchEngineItem.setSEItemTitle("Google");
            searchEngineItem.setSEItemURL("https://www.google.com/search?q=");
            searchEngineItem.setSEIsDefault(1);
            searchEngineItem.setSEItemIsCurrent(1);

            addSearchEngineItemsWhenCreation(db,searchEngineItem);
        }

        {
            SearchEngineItem searchEngineItem = new SearchEngineItem();
            searchEngineItem.setSEItemTitle("DuckDuckGo");
            searchEngineItem.setSEItemURL("https://duckduckgo.com/?q=");
            searchEngineItem.setSEIsDefault(1);
            searchEngineItem.setSEItemIsCurrent(0);

            addSearchEngineItemsWhenCreation(db,searchEngineItem);
        }

        {
            SearchEngineItem searchEngineItem = new SearchEngineItem();
            searchEngineItem.setSEItemTitle("Bing");
            searchEngineItem.setSEItemURL("https://www.bing.com/search?q=");
            searchEngineItem.setSEIsDefault(1);
            searchEngineItem.setSEItemIsCurrent(0);

            addSearchEngineItemsWhenCreation(db,searchEngineItem);
        }

    }

    private void addSearchEngineItemsWhenCreation(SQLiteDatabase db, SearchEngineItem searchEngineItem)
    {
        ContentValues contentValues = new ContentValues();
        contentValues.put(SE_ITEM_TITLE,searchEngineItem.getSEItemTitle());
        contentValues.put(SE_ITEM_URL,searchEngineItem.getSEItemURL());
        contentValues.put(SE_IS_DEFAULT,searchEngineItem.getSEIsDefault());
        contentValues.put(SE_ITEM_IS_CURRENT,searchEngineItem.getSEItemIsCurrent());

        db.insert(searchEnginesTBL,null,contentValues);
    }

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
                CURRENT_THEME_ID+" INTEGER,"+IS_DARK_WEB_UI+" INTEGER,"+DARK_THEME+" INTEGER,"+HOME_PAGE_URL+" TEXT,"+SEARCH_ENGINE_URL+
                " TEXT,"+IS_SAVE_RECENT_TABS+" INTEGER,"+BROWSER_TUTORIAL_INFO+" INTEGER,"+DOWNLOAD_PATH+" TEXT,"+
                AUTO_RESUME_STATUS + " INTEGER," + SIMULTANEOUS_TASKS + " INTEGER," + DEFAULT_SEGMENTS + " INTEGER," +
                DIRECT_DOWNLOAD + " INTEGER," + SHOW_OPTIMIZATION +" INTEGER);";

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
        values.put(SEARCH_ENGINE_URL, userPreferences.getSearchEngineURL());
        values.put(BROWSER_TUTORIAL_INFO, userPreferences.getBrowserTutorialInfo());
        values.put(IS_SAVE_RECENT_TABS,userPreferences.getIsSaveRecentTabs());
        values.put(DOWNLOAD_PATH,userPreferences.getDownloadPath());
        values.put(AUTO_RESUME_STATUS,userPreferences.getAutoResumeStatus());
        values.put(SIMULTANEOUS_TASKS,userPreferences.getSimultaneousTasks());
        values.put(DEFAULT_SEGMENTS,userPreferences.getDefaultSegments());
        values.put(DIRECT_DOWNLOAD,userPreferences.getDirectDownload());
        values.put(SHOW_OPTIMIZATION,userPreferences.getShowOptimization());

        writableDB.insert(userPreferencesTBL,null,values);

    }

    public PartialCompletedTask getBindDownloadTaskComplete(int id)
    {
        Cursor cursor = readableDB.query(downloadTasksTBL,new String[]{KEY_ID,FILE_NAME,TIME_LEFT,DOWNLOAD_SPEED
                },KEY_ID + "=?",
                new String[]{String.valueOf(id)},null,null,null,null);
        PartialCompletedTask partialCompletedTask = new PartialCompletedTask();

        cursor.moveToFirst();
        partialCompletedTask.setFileName(cursor.getString(cursor.getColumnIndexOrThrow(FILE_NAME)));
        partialCompletedTask.setTimeLeft(cursor.getString(cursor.getColumnIndexOrThrow(TIME_LEFT)));
        partialCompletedTask.setDownloadSpeed(cursor.getString(cursor.getColumnIndexOrThrow(DOWNLOAD_SPEED)));
        cursor.close();
        return partialCompletedTask;
    }

    public ArrayList<Integer> getAllDownloadTaskIDs()
    {
        ArrayList<Integer> downloadTaskIDsList = new ArrayList<>();
        Cursor cursor = readableDB.query(downloadTasksTBL,new String[] {KEY_ID},CURRENT_STATUS + " IN (?,?,?,?,?,?,?)",
                new String[]{"1","2","3","4","5","6","0"},null,null,KEY_ID + " DESC",null);

        while (cursor.moveToNext())
        {
            downloadTaskIDsList.add(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)));
        }

        cursor.close();
        return downloadTaskIDsList;
    }

    public ArrayList<Integer> getCompletedDownloadTaskIDs()
    {
        //for e.g. if we want download task with ids in order 2,11,8 then we can do following to get results in reverse order
        ArrayList<Integer> downloadTaskIDsList = new ArrayList<>();
        Cursor cursor = readableDB.query(completedTasksTBL,new String[] {CDT_KEY_ID},null,
                null,null,null,null,null);

        if(cursor.moveToLast())
        {
            do {
                downloadTaskIDsList.add(cursor.getInt(cursor.getColumnIndexOrThrow(CDT_KEY_ID)));
            } while (cursor.moveToPrevious());
        }

        cursor.close();
        return downloadTaskIDsList;
    }

    public PartialOne getSingleThreadedDT(int id)
    {
        Cursor cursor = readableDB.query(downloadTasksTBL,new String[]{CHUNK_MODE,CURRENT_STATUS,CURRENT_PROGRESS},KEY_ID + "=?",
                new String[]{String.valueOf(id)},null,null,null,null);
        PartialOne partialOne = new PartialOne();

        cursor.moveToFirst();
        partialOne.setChunkMode(cursor.getInt(cursor.getColumnIndexOrThrow(CHUNK_MODE)));
        partialOne.setCurrentStatus(cursor.getInt(cursor.getColumnIndexOrThrow(CURRENT_STATUS)));
        partialOne.setCurrentProgress(cursor.getInt(cursor.getColumnIndexOrThrow(CURRENT_PROGRESS)));
        cursor.close();
        return partialOne;
    }

    public PartialTwo getTwoThreadedDT(int id)
    {
        Cursor cursor = readableDB.query(downloadTasksTBL,new String[]{CURRENT_STATUS,TPB1,TPB2},KEY_ID + "=?",
                new String[]{String.valueOf(id)},null,null,null,null);
        PartialTwo partialTwo = new PartialTwo();

        cursor.moveToFirst();
        partialTwo.setCurrentStatus(cursor.getInt(cursor.getColumnIndexOrThrow(CURRENT_STATUS)));
        partialTwo.setTPB1(cursor.getInt(cursor.getColumnIndexOrThrow(TPB1)));
        partialTwo.setTPB2(cursor.getInt(cursor.getColumnIndexOrThrow(TPB2)));
        cursor.close();
        return partialTwo;
    }

    public PartialFour getFourThreadedDT(int id)
    {
        Cursor cursor = readableDB.query(downloadTasksTBL,new String[]{CURRENT_STATUS,TPB1,TPB2,TPB3,TPB4},KEY_ID + "=?",
                new String[]{String.valueOf(id)},null,null,null,null);
        PartialFour partialFour = new PartialFour();

        cursor.moveToFirst();
        partialFour.setCurrentStatus(cursor.getInt(cursor.getColumnIndexOrThrow(CURRENT_STATUS)));
        partialFour.setTPB1(cursor.getInt(cursor.getColumnIndexOrThrow(TPB1)));
        partialFour.setTPB2(cursor.getInt(cursor.getColumnIndexOrThrow(TPB2)));
        partialFour.setTPB3(cursor.getInt(cursor.getColumnIndexOrThrow(TPB3)));
        partialFour.setTPB4(cursor.getInt(cursor.getColumnIndexOrThrow(TPB4)));
        cursor.close();
        return partialFour;
    }

    public PartialSix getSixThreadedDT(int id)
    {
        Cursor cursor = readableDB.query(downloadTasksTBL,new String[]{CURRENT_STATUS,TPB1,TPB2,TPB3,TPB4,TPB5,TPB6},KEY_ID + "=?",
                new String[]{String.valueOf(id)},null,null,null,null);
        PartialSix partialSix = new PartialSix();

        cursor.moveToFirst();
        partialSix.setCurrentStatus(cursor.getInt(cursor.getColumnIndexOrThrow(CURRENT_STATUS)));
        partialSix.setTPB1(cursor.getInt(cursor.getColumnIndexOrThrow(TPB1)));
        partialSix.setTPB2(cursor.getInt(cursor.getColumnIndexOrThrow(TPB2)));
        partialSix.setTPB3(cursor.getInt(cursor.getColumnIndexOrThrow(TPB3)));
        partialSix.setTPB4(cursor.getInt(cursor.getColumnIndexOrThrow(TPB4)));
        partialSix.setTPB5(cursor.getInt(cursor.getColumnIndexOrThrow(TPB5)));
        partialSix.setTPB6(cursor.getInt(cursor.getColumnIndexOrThrow(TPB6)));
        cursor.close();
        return partialSix;
    }

    public PartialEight getEightThreadedDT(int id)
    {
        Cursor cursor = readableDB.query(downloadTasksTBL,new String[]{CURRENT_STATUS,TPB1,TPB2,TPB3,TPB4,TPB5,TPB6,TPB7,TPB8},KEY_ID + "=?",
                new String[]{String.valueOf(id)},null,null,null,null);
        PartialEight partialEight = new PartialEight();

        cursor.moveToFirst();
        partialEight.setCurrentStatus(cursor.getInt(cursor.getColumnIndexOrThrow(CURRENT_STATUS)));
        partialEight.setTPB1(cursor.getInt(cursor.getColumnIndexOrThrow(TPB1)));
        partialEight.setTPB2(cursor.getInt(cursor.getColumnIndexOrThrow(TPB2)));
        partialEight.setTPB3(cursor.getInt(cursor.getColumnIndexOrThrow(TPB3)));
        partialEight.setTPB4(cursor.getInt(cursor.getColumnIndexOrThrow(TPB4)));
        partialEight.setTPB5(cursor.getInt(cursor.getColumnIndexOrThrow(TPB5)));
        partialEight.setTPB6(cursor.getInt(cursor.getColumnIndexOrThrow(TPB6)));
        partialEight.setTPB7(cursor.getInt(cursor.getColumnIndexOrThrow(TPB7)));
        partialEight.setTPB8(cursor.getInt(cursor.getColumnIndexOrThrow(TPB8)));
        cursor.close();
        return partialEight;
    }

    public PartialSixteen getSixteenThreadedDT(int id)
    {
        Cursor cursor = readableDB.query(downloadTasksTBL,new String[]{CURRENT_STATUS,TPB1,TPB2,TPB3,TPB4,TPB5,TPB6,TPB7,TPB8,
                TPB9,TPB10,TPB11,TPB12,TPB13,TPB14,TPB15,TPB16},KEY_ID + "=?",
                new String[]{String.valueOf(id)},null,null,null,null);
        PartialSixteen partialSixteen = new PartialSixteen();

        cursor.moveToFirst();
        partialSixteen.setCurrentStatus(cursor.getInt(cursor.getColumnIndexOrThrow(CURRENT_STATUS)));
        partialSixteen.setTPB1(cursor.getInt(cursor.getColumnIndexOrThrow(TPB1)));
        partialSixteen.setTPB2(cursor.getInt(cursor.getColumnIndexOrThrow(TPB2)));
        partialSixteen.setTPB3(cursor.getInt(cursor.getColumnIndexOrThrow(TPB3)));
        partialSixteen.setTPB4(cursor.getInt(cursor.getColumnIndexOrThrow(TPB4)));
        partialSixteen.setTPB5(cursor.getInt(cursor.getColumnIndexOrThrow(TPB5)));
        partialSixteen.setTPB6(cursor.getInt(cursor.getColumnIndexOrThrow(TPB6)));
        partialSixteen.setTPB7(cursor.getInt(cursor.getColumnIndexOrThrow(TPB7)));
        partialSixteen.setTPB8(cursor.getInt(cursor.getColumnIndexOrThrow(TPB8)));
        partialSixteen.setTPB9(cursor.getInt(cursor.getColumnIndexOrThrow(TPB9)));
        partialSixteen.setTPB10(cursor.getInt(cursor.getColumnIndexOrThrow(TPB10)));
        partialSixteen.setTPB11(cursor.getInt(cursor.getColumnIndexOrThrow(TPB11)));
        partialSixteen.setTPB12(cursor.getInt(cursor.getColumnIndexOrThrow(TPB12)));
        partialSixteen.setTPB13(cursor.getInt(cursor.getColumnIndexOrThrow(TPB13)));
        partialSixteen.setTPB14(cursor.getInt(cursor.getColumnIndexOrThrow(TPB14)));
        partialSixteen.setTPB15(cursor.getInt(cursor.getColumnIndexOrThrow(TPB15)));
        partialSixteen.setTPB16(cursor.getInt(cursor.getColumnIndexOrThrow(TPB16)));
        cursor.close();
        return partialSixteen;
    }

    public PartialThirtyTwo getThirtyTwoThreadedDT(int id)
    {
        Cursor cursor = readableDB.query(downloadTasksTBL,new String[]{CURRENT_STATUS,TPB1,TPB2,TPB3,TPB4,TPB5,TPB6,TPB7,TPB8,
                        TPB9,TPB10,TPB11,TPB12,TPB13,TPB14,TPB15,TPB16,
                TPB17,TPB18,TPB19,TPB20,TPB21,TPB22,TPB23,TPB24,TPB25,TPB26,TPB27,TPB28,TPB29,TPB30,TPB31,TPB32},KEY_ID + "=?",
                new String[]{String.valueOf(id)},null,null,null,null);
        PartialThirtyTwo partialThirtyTwo = new PartialThirtyTwo();

        cursor.moveToFirst();
        partialThirtyTwo.setCurrentStatus(cursor.getInt(cursor.getColumnIndexOrThrow(CURRENT_STATUS)));
        partialThirtyTwo.setTPB1(cursor.getInt(cursor.getColumnIndexOrThrow(TPB1)));
        partialThirtyTwo.setTPB2(cursor.getInt(cursor.getColumnIndexOrThrow(TPB2)));
        partialThirtyTwo.setTPB3(cursor.getInt(cursor.getColumnIndexOrThrow(TPB3)));
        partialThirtyTwo.setTPB4(cursor.getInt(cursor.getColumnIndexOrThrow(TPB4)));
        partialThirtyTwo.setTPB5(cursor.getInt(cursor.getColumnIndexOrThrow(TPB5)));
        partialThirtyTwo.setTPB6(cursor.getInt(cursor.getColumnIndexOrThrow(TPB6)));
        partialThirtyTwo.setTPB7(cursor.getInt(cursor.getColumnIndexOrThrow(TPB7)));
        partialThirtyTwo.setTPB8(cursor.getInt(cursor.getColumnIndexOrThrow(TPB8)));
        partialThirtyTwo.setTPB9(cursor.getInt(cursor.getColumnIndexOrThrow(TPB9)));
        partialThirtyTwo.setTPB10(cursor.getInt(cursor.getColumnIndexOrThrow(TPB10)));
        partialThirtyTwo.setTPB11(cursor.getInt(cursor.getColumnIndexOrThrow(TPB11)));
        partialThirtyTwo.setTPB12(cursor.getInt(cursor.getColumnIndexOrThrow(TPB12)));
        partialThirtyTwo.setTPB13(cursor.getInt(cursor.getColumnIndexOrThrow(TPB13)));
        partialThirtyTwo.setTPB14(cursor.getInt(cursor.getColumnIndexOrThrow(TPB14)));
        partialThirtyTwo.setTPB15(cursor.getInt(cursor.getColumnIndexOrThrow(TPB15)));
        partialThirtyTwo.setTPB16(cursor.getInt(cursor.getColumnIndexOrThrow(TPB16)));
        partialThirtyTwo.setTPB17(cursor.getInt(cursor.getColumnIndexOrThrow(TPB17)));
        partialThirtyTwo.setTPB18(cursor.getInt(cursor.getColumnIndexOrThrow(TPB18)));
        partialThirtyTwo.setTPB19(cursor.getInt(cursor.getColumnIndexOrThrow(TPB19)));
        partialThirtyTwo.setTPB20(cursor.getInt(cursor.getColumnIndexOrThrow(TPB20)));
        partialThirtyTwo.setTPB21(cursor.getInt(cursor.getColumnIndexOrThrow(TPB21)));
        partialThirtyTwo.setTPB22(cursor.getInt(cursor.getColumnIndexOrThrow(TPB22)));
        partialThirtyTwo.setTPB23(cursor.getInt(cursor.getColumnIndexOrThrow(TPB23)));
        partialThirtyTwo.setTPB24(cursor.getInt(cursor.getColumnIndexOrThrow(TPB24)));
        partialThirtyTwo.setTPB25(cursor.getInt(cursor.getColumnIndexOrThrow(TPB25)));
        partialThirtyTwo.setTPB26(cursor.getInt(cursor.getColumnIndexOrThrow(TPB26)));
        partialThirtyTwo.setTPB27(cursor.getInt(cursor.getColumnIndexOrThrow(TPB27)));
        partialThirtyTwo.setTPB28(cursor.getInt(cursor.getColumnIndexOrThrow(TPB28)));
        partialThirtyTwo.setTPB29(cursor.getInt(cursor.getColumnIndexOrThrow(TPB29)));
        partialThirtyTwo.setTPB30(cursor.getInt(cursor.getColumnIndexOrThrow(TPB30)));
        partialThirtyTwo.setTPB31(cursor.getInt(cursor.getColumnIndexOrThrow(TPB31)));
        partialThirtyTwo.setTPB32(cursor.getInt(cursor.getColumnIndexOrThrow(TPB32)));
        cursor.close();
        return partialThirtyTwo;
    }

    public PartialBindDownloadTask getBindDownloadTask(int id)
    {
        Cursor cursor = readableDB.query(downloadTasksTBL,new String[]{KEY_ID,FILE_NAME,TIME_LEFT,DOWNLOAD_SPEED,PAUSE_RESUME_SUPPORTED,CURRENT_STATUS
                        },KEY_ID + "=?",
                new String[]{String.valueOf(id)},null,null,null,null);
        PartialBindDownloadTask partialBindDownloadTask = new PartialBindDownloadTask();

        cursor.moveToFirst();
        partialBindDownloadTask.setKeyId(cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)));
        partialBindDownloadTask.setFileName(cursor.getString(cursor.getColumnIndexOrThrow(FILE_NAME)));
        partialBindDownloadTask.setTimeLeft(cursor.getString(cursor.getColumnIndexOrThrow(TIME_LEFT)));
        partialBindDownloadTask.setDownloadSpeed(cursor.getString(cursor.getColumnIndexOrThrow(DOWNLOAD_SPEED)));
        partialBindDownloadTask.setPauseResumeSupported(cursor.getString(cursor.getColumnIndexOrThrow(PAUSE_RESUME_SUPPORTED)));
        partialBindDownloadTask.setCurrentStatus(cursor.getInt(cursor.getColumnIndexOrThrow(CURRENT_STATUS)));
        cursor.close();
        return partialBindDownloadTask;
    }


    public void updateAutoResumeStatus(int value)
    {
        ContentValues contentValues = new ContentValues();
        contentValues.put(AUTO_RESUME_STATUS,value);

        writableDB.update(userPreferencesTBL,contentValues,UP_KEY_ID + "=?",new String[]{String.valueOf(1)});
    }

    public void updateSimultaneousTasks(int tasks)
    {
        ContentValues contentValues = new ContentValues();
        contentValues.put(SIMULTANEOUS_TASKS,tasks);

        writableDB.update(userPreferencesTBL,contentValues,UP_KEY_ID + "=?",new String[]{String.valueOf(1)});
    }

    public void updateDefaultSegments(int segments)
    {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DEFAULT_SEGMENTS,segments);

        writableDB.update(userPreferencesTBL,contentValues,UP_KEY_ID + "=?",new String[]{String.valueOf(1)});
    }

    public void updateDownloadAddress(String downloadAddress)
    {
        ContentValues contentValues = new ContentValues();
        contentValues.put(DOWNLOAD_PATH,downloadAddress);

        writableDB.update(userPreferencesTBL,contentValues,UP_KEY_ID + "=?",new String[]{String.valueOf(1)});
    }

    public ArrayList<String> getAllDownloadTaskNames()
    {

        ArrayList<String> result = new ArrayList<>();
        Cursor cursor = readableDB.query(downloadTasksTBL,new String[]{FILE_NAME},null,null,null,null,null,null);

        while (cursor.moveToNext())
        {
            result.add(cursor.getString(cursor.getColumnIndexOrThrow(FILE_NAME)));
        }

        cursor.close();

        return result;
    }



    public UserPreferences getDTSettingsUP()
    {
        UserPreferences userPreferences = new UserPreferences();

        Cursor cursor = readableDB.query(userPreferencesTBL, new String[]{UP_KEY_ID,DOWNLOAD_PATH,
                        AUTO_RESUME_STATUS,SIMULTANEOUS_TASKS,DEFAULT_SEGMENTS,DIRECT_DOWNLOAD}
                , UP_KEY_ID + "=?",
                new String[]{String.valueOf(1)}, null, null, null
        );

        cursor.moveToFirst();
        userPreferences.setUpKeyId(cursor.getInt(cursor.getColumnIndexOrThrow(UP_KEY_ID)));
        userPreferences.setDownloadPath(cursor.getString(cursor.getColumnIndexOrThrow(DOWNLOAD_PATH)));
        userPreferences.setAutoResumeStatus(cursor.getInt(cursor.getColumnIndexOrThrow(AUTO_RESUME_STATUS)));
        userPreferences.setSimultaneousTasks(cursor.getInt(cursor.getColumnIndexOrThrow(SIMULTANEOUS_TASKS)));
        userPreferences.setDefaultSegments(cursor.getInt(cursor.getColumnIndexOrThrow(DEFAULT_SEGMENTS)));
        userPreferences.setDirectDownload(cursor.getInt(cursor.getColumnIndexOrThrow(DIRECT_DOWNLOAD)));

        cursor.close();

        return userPreferences;
    }

    public UserPreferences getHalfUserPreferences()
    {
        UserPreferences userPreferences = new UserPreferences();

        Cursor cursor = readableDB.query(userPreferencesTBL, new String[]{UP_KEY_ID,DOWNLOAD_PATH, AUTO_RESUME_STATUS}
                , UP_KEY_ID + "=?",
                new String[]{String.valueOf(1)}, null, null, null
        );


        cursor.moveToFirst();
        userPreferences.setUpKeyId(cursor.getInt(cursor.getColumnIndexOrThrow(UP_KEY_ID)));
        userPreferences.setDownloadPath(cursor.getString(cursor.getColumnIndexOrThrow(DOWNLOAD_PATH)));
        userPreferences.setAutoResumeStatus(cursor.getInt(cursor.getColumnIndexOrThrow(AUTO_RESUME_STATUS)));

        cursor.close();

        return userPreferences;


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

    public int getDefaultSegments()
    {
        Cursor cursor = readableDB.query(userPreferencesTBL,new String[]{DEFAULT_SEGMENTS},
                UP_KEY_ID + "=?",new String[]{String.valueOf(1)},null,null,null,null);
        cursor.moveToFirst();
        int result = cursor.getInt(cursor.getColumnIndexOrThrow(DEFAULT_SEGMENTS));
        cursor.close();
        return result;

    }

    //Get all downloadTasks
    public int getRecentTaskID()
    {
        Cursor cursor = readableDB.query(downloadTasksTBL, new String[]{KEY_ID
                }
                , CURRENT_STATUS + " IN (?,?,?,?,?,?,?)",
                new String[]{"1","2","3","4","5","6","0"}, null, null, KEY_ID
                        + " DESC", String.valueOf(1)
        );

        cursor.moveToFirst();
        final int recentTaskID = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID));
        cursor.close();

        return recentTaskID;

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

    public ArrayList<Integer> getAllSearchEngineItemIDs()
    {
        ArrayList<Integer> resultList = new ArrayList<>();

        Cursor cursor = readableDB.query(searchEnginesTBL,new String[]{SE_KEY_ID},null,
                null,null,null,null);

        while (cursor.moveToNext())
        {
            resultList.add(cursor.getInt(cursor.getColumnIndexOrThrow(SE_KEY_ID)));
        }

        cursor.close();

        return resultList;
    }

    public SearchEngineItem getCurrentSearchEngineItem()
    {
        Cursor cursor = readableDB.query(searchEnginesTBL,new String[]{SE_IS_DEFAULT,SE_ITEM_TITLE,SE_ITEM_URL,SE_ITEM_IS_CURRENT},SE_ITEM_IS_CURRENT+"=?",
                new String[]{String.valueOf(1)},null,null,null);

        SearchEngineItem searchEngineItem = new SearchEngineItem();

        cursor.moveToFirst();
        searchEngineItem.setSEIsDefault(cursor.getInt(cursor.getColumnIndexOrThrow(SE_IS_DEFAULT)));
        searchEngineItem.setSEItemTitle(cursor.getString(cursor.getColumnIndexOrThrow(SE_ITEM_TITLE)));
        searchEngineItem.setSEItemURL(cursor.getString(cursor.getColumnIndexOrThrow(SE_ITEM_URL)));
        searchEngineItem.setSEItemIsCurrent(cursor.getInt(cursor.getColumnIndexOrThrow(SE_ITEM_IS_CURRENT)));

        cursor.close();
        return searchEngineItem;
    }

    public SearchEngineItem getSearchEngineItem(int id)
    {
        Cursor cursor = readableDB.query(searchEnginesTBL,new String[]{SE_IS_DEFAULT,SE_ITEM_TITLE,SE_ITEM_URL,SE_ITEM_IS_CURRENT},
                SE_KEY_ID+"=?",
                new String[]{String.valueOf(id)},null,null,null);

        SearchEngineItem searchEngineItem = new SearchEngineItem();

        cursor.moveToFirst();
        searchEngineItem.setSEIsDefault(cursor.getInt(cursor.getColumnIndexOrThrow(SE_IS_DEFAULT)));
        searchEngineItem.setSEItemTitle(cursor.getString(cursor.getColumnIndexOrThrow(SE_ITEM_TITLE)));
        searchEngineItem.setSEItemURL(cursor.getString(cursor.getColumnIndexOrThrow(SE_ITEM_URL)));
        searchEngineItem.setSEItemIsCurrent(cursor.getInt(cursor.getColumnIndexOrThrow(SE_ITEM_IS_CURRENT)));

        cursor.close();
        return searchEngineItem;
    }

    public void updateOldSeId()
    {
        Cursor cursor = readableDB.query(searchEnginesTBL,new String[]{SE_KEY_ID},
                SE_ITEM_IS_CURRENT+"=?",
                new String[]{String.valueOf(1)},null,null,null);
        int currentSEId;
        cursor.moveToFirst();
        currentSEId = cursor.getInt(cursor.getColumnIndexOrThrow(SE_KEY_ID));
        cursor.close();

        ContentValues values = new ContentValues();
        values.put(SE_ITEM_IS_CURRENT,0);

        writableDB.update(searchEnginesTBL,values,SE_KEY_ID + "=?",new String[]{String.valueOf(currentSEId)});
    }

    public void updateSearchEngineURL(String searchEngineURL)
    {
        ContentValues values = new ContentValues();
        values.put(SEARCH_ENGINE_URL, searchEngineURL);

        writableDB.update(userPreferencesTBL,values,UP_KEY_ID + "=?",new String[]{String.valueOf(1)});
    }

    public void updateSEIsCurrent(int id)
    {
        ContentValues values = new ContentValues();
        values.put(SE_ITEM_IS_CURRENT, 1);

        writableDB.update(searchEnginesTBL,values,SE_KEY_ID + "=?",new String[]{String.valueOf(id)});
    }

    public ArrayList<String> getAllSearchItems()
    {
        ArrayList<String> result = new ArrayList<>();
        Cursor cursor = readableDB.query(searchItemTBL,new String[]{S_ITEM_TITLE},null,null,null,null,
                S_KEY_ID + " DESC");

        while (cursor.moveToNext())
        {
            result.add(cursor.getString(cursor.getColumnIndexOrThrow(S_ITEM_TITLE)));
        }

        cursor.close();
        return result;
    }

    public ArrayList<String> getAllSearchItemsWithTitle(String title)
    {
        ArrayList<String> result = new ArrayList<>();
        Cursor cursor = readableDB.query(searchItemTBL,new String[]{S_ITEM_TITLE},S_ITEM_TITLE + " LIKE ?",new String[]{"%"+title+"%"},
                null,null,
                S_KEY_ID + " DESC");

        while (cursor.moveToNext())
        {
            result.add(cursor.getString(cursor.getColumnIndexOrThrow(S_ITEM_TITLE)));
        }

        cursor.close();
        return result;
    }

    public Integer getIsSaveSearchHistory()
    {

        Cursor cursor = readableDB.query(siteSettingsTBL, new String[]{SS_SAVE_SEARCH_HISTORY}
                , SS_ID + "=?",
                new String[]{String.valueOf(1)}, null, null, null
        );


        cursor.moveToFirst();
        Integer isSave = cursor.getInt(cursor.getColumnIndexOrThrow(SS_SAVE_SEARCH_HISTORY));
        cursor.close();
        return isSave;
    }

    public ArrayList<Integer> getAllSearchHistoryItemsIDs()
    {
        ArrayList<Integer> result = new ArrayList<>();
        Cursor cursor = readableDB.query(searchItemTBL,new String[]{S_KEY_ID},null,null,null,null,
                null);

        while (cursor.moveToNext())
        {
            result.add(cursor.getInt(cursor.getColumnIndexOrThrow(S_KEY_ID)));
        }

        cursor.close();
        return result;
    }

    public ArrayList<Integer> getAllSearchItemsIDsWithTitleAs(String title)
    {
        ArrayList<Integer> result = new ArrayList<>();
        Cursor cursor = readableDB.query(searchItemTBL,new String[]{S_KEY_ID},S_ITEM_TITLE + " LIKE ?",new String[]{"%"+title+"%"},
                null,null,
                S_KEY_ID + " DESC");

        while (cursor.moveToNext())
        {
            result.add(cursor.getInt(cursor.getColumnIndexOrThrow(S_KEY_ID)));
        }

        cursor.close();
        return result;
    }

    public String getSearchItemTitle(int id)
    {
        Cursor cursor = readableDB.query(searchItemTBL,new String[]{S_ITEM_TITLE},S_KEY_ID+"=?",
                new String[]{String.valueOf(id)},null,null,null,null);

        cursor.moveToFirst();
        String result = cursor.getString(cursor.getColumnIndexOrThrow(S_ITEM_TITLE));
        cursor.close();

        return result;
    }

    public void deleteSearchItem(int id)
    {
        writableDB.delete(searchItemTBL,S_KEY_ID + "=?",new String[]{String.valueOf(id)});
    }

    public SiteSettingsModel getSiteSettings()
    {
        Cursor cursor = readableDB.query(siteSettingsTBL,new String[]{SS_LOCATION,SS_COOKIES,SS_JAVA_SCRIPT,SS_SAVE_SITES_IN_HISTORY,SS_SAVE_SEARCH_HISTORY,
        SS_IS_CHANGED},SS_ID+"=?",new String[]{String.valueOf(1)},null,null,null,null);

        SiteSettingsModel siteSettingsModel = new SiteSettingsModel();

        cursor.moveToFirst();
        siteSettingsModel.setSsLocation(cursor.getInt(cursor.getColumnIndexOrThrow(SS_LOCATION)));
        siteSettingsModel.setSsCookies(cursor.getInt(cursor.getColumnIndexOrThrow(SS_COOKIES)));
        siteSettingsModel.setSsJavaScript(cursor.getInt(cursor.getColumnIndexOrThrow(SS_JAVA_SCRIPT)));
        siteSettingsModel.setSsSaveSitesInHistory(cursor.getInt(cursor.getColumnIndexOrThrow(SS_SAVE_SITES_IN_HISTORY)));
        siteSettingsModel.setSsSaveSearchHistory(cursor.getInt(cursor.getColumnIndexOrThrow(SS_SAVE_SEARCH_HISTORY)));
        siteSettingsModel.setSsIsChanged(cursor.getInt(cursor.getColumnIndexOrThrow(SS_IS_CHANGED)));
        cursor.close();

        return siteSettingsModel;
    }

    public void updateSaveSearchHistoryStatus(Integer status)
    {
        ContentValues values = new ContentValues();
        values.put(SS_SAVE_SEARCH_HISTORY,status);
        writableDB.update(siteSettingsTBL,values,SS_ID+"=?",new String[]{String.valueOf(1)});
    }

    public void updateSaveSitesInHistoryStatus(Integer status)
    {
        ContentValues values = new ContentValues();
        values.put(SS_SAVE_SITES_IN_HISTORY,status);
        writableDB.update(siteSettingsTBL,values,SS_ID+"=?",new String[]{String.valueOf(1)});
    }

    public void updateJavaScriptStatus(Integer status)
    {
        ContentValues values = new ContentValues();
        values.put(SS_JAVA_SCRIPT,status);
        writableDB.update(siteSettingsTBL,values,SS_ID+"=?",new String[]{String.valueOf(1)});
    }

    public void updateCookieStatus(Integer status)
    {
        ContentValues values = new ContentValues();
        values.put(SS_COOKIES,status);
        writableDB.update(siteSettingsTBL,values,SS_ID+"=?",new String[]{String.valueOf(1)});
    }

    public void updateLocationStatus(Integer status)
    {
        ContentValues values = new ContentValues();
        values.put(SS_LOCATION,status);
        writableDB.update(siteSettingsTBL,values,SS_ID+"=?",new String[]{String.valueOf(1)});
    }

    public void updateSiteSettingsChanged(Integer status)
    {
        ContentValues values = new ContentValues();
        values.put(SS_IS_CHANGED,status);
        writableDB.update(siteSettingsTBL,values,SS_ID+"=?",new String[]{String.valueOf(1)});
    }

    public void addSiteSettings(SiteSettingsModel siteSettingsModel)
    {
        //truncating old table
        writableDB.delete(siteSettingsTBL,null,null);

        ContentValues values = new ContentValues();
        values.put(SS_ID, siteSettingsModel.getSsId());
        values.put(SS_LOCATION, siteSettingsModel.getSsLocation());
        values.put(SS_COOKIES, siteSettingsModel.getSsCookies());
        values.put(SS_JAVA_SCRIPT, siteSettingsModel.getSsJavaScript());
        values.put(SS_SAVE_SITES_IN_HISTORY, siteSettingsModel.getSsSaveSitesInHistory());
        values.put(SS_SAVE_SEARCH_HISTORY, siteSettingsModel.getSsSaveSearchHistory());
        values.put(SS_IS_CHANGED, siteSettingsModel.getSsIsChanged());

        writableDB.insert(siteSettingsTBL,null,values);
    }

    public Integer getIsSiteSettingsChanged()
    {
        Cursor cursor = readableDB.query(siteSettingsTBL, new String[]{SS_IS_CHANGED}
                , SS_ID + "=?",
                new String[]{String.valueOf(1)}, null, null, null
        );
        cursor.moveToFirst();
        Integer isChanged = cursor.getInt(cursor.getColumnIndexOrThrow(SS_IS_CHANGED));
        cursor.close();
        return isChanged;
    }

    public ArrayList<String> getAllRecentSitesURLs()
    {
        ArrayList<String> list = new ArrayList<>();
        Cursor cursor = readableDB.query(recentSitesTBL,new String[]{RCT_SITE_URL},null,null,null,null,null);
        while (cursor.moveToNext())
        {
            list.add(cursor.getString(cursor.getColumnIndexOrThrow(RCT_SITE_URL)));
        }
        cursor.close();
        return list;
    }

    public void addRecentSiteURL(String siteURL)
    {
        ContentValues values = new ContentValues();
        values.put(RCT_SITE_URL,siteURL);
        writableDB.insert(recentSitesTBL,null,values);
    }

    public void truncateRecentSitesTable()
    {
        writableDB.delete(recentSitesTBL,null,null);
    }

    public void updateIsSaveRecentTabs(int saveVal)
    {
        ContentValues values = new ContentValues();
        values.put(IS_SAVE_RECENT_TABS,saveVal);
        writableDB.update(userPreferencesTBL,values,UP_KEY_ID + "=?",new String[]{String.valueOf(1)});
    }

    public int isSaveRecentTabs()
    {
        Cursor cursor = readableDB.query(userPreferencesTBL,new String[]{IS_SAVE_RECENT_TABS},UP_KEY_ID + "=?",
                new String[]{String.valueOf(1)},null,null,null);
        cursor.moveToFirst();
        int result = cursor.getInt(cursor.getColumnIndexOrThrow(IS_SAVE_RECENT_TABS));
        cursor.close();
        return result;
    }

    public boolean isShowBrowserTutorial()
    {
        Cursor cursor = readableDB.query(userPreferencesTBL,new String[]{BROWSER_TUTORIAL_INFO},UP_KEY_ID + "=?",
                new String[]{String.valueOf(1)},null,null,null);
        cursor.moveToFirst();
        int result = cursor.getInt(cursor.getColumnIndexOrThrow(BROWSER_TUTORIAL_INFO));
        cursor.close();
        return result == 1;
    }

    public void updateBrowserTutorialStatus()
    {
        ContentValues contentValues = new ContentValues();
        contentValues.put(BROWSER_TUTORIAL_INFO,0);

        writableDB.update(userPreferencesTBL,contentValues,UP_KEY_ID + "=?", new String[]{String.valueOf(1)});
    }

    public int getSegmentsForDownloadTask(int id)
    {
        Cursor cursor = readableDB.query(downloadTasksTBL,new String[] {SEGMENTS_FOR_DOWNLOAD_TASK},KEY_ID + "=?",
                new String[]{String.valueOf(id)},null,null,null,null);
        cursor.moveToFirst();
        int result = cursor.getInt(cursor.getColumnIndexOrThrow(SEGMENTS_FOR_DOWNLOAD_TASK));
        cursor.close();
        return result;
    }

    public void addTask(DownloadTask downloadTask)
    {
        ContentValues contentValues = new ContentValues();
        contentValues.put(FILE_NAME,downloadTask.getFileName());
        contentValues.put(URL,downloadTask.getUrl());
        contentValues.put(DIR_PATH,downloadTask.getDirPath());
        contentValues.put(TOTAL_BYTES,downloadTask.getTotalBytes());
        contentValues.put(DOWNLOADED_BYTES,downloadTask.getDownloadedBytes());
        contentValues.put(CURRENT_STATUS,downloadTask.getCurrentStatus());
        contentValues.put(CURRENT_PROGRESS,downloadTask.getCurrentProgress());
        contentValues.put(DOWNLOAD_SPEED,downloadTask.getDownloadSpeed());
        contentValues.put(TIME_LEFT,downloadTask.getTimeLeft());
        contentValues.put(PAUSE_RESUME_SUPPORTED,downloadTask.getPauseResumeSupported());
        contentValues.put(IS_PAUSE_RESUME_SUPPORTED,downloadTask.getIsPauseResumeSupported());
        contentValues.put(CHUNK_MODE,downloadTask.getChunkMode());
        contentValues.put(USER_AGENT_STRING,downloadTask.getUserAgentString());
        contentValues.put(PAGE_URL,downloadTask.getPageURL());
        contentValues.put(WHICH_ERROR,downloadTask.getWhichError());
        contentValues.put(SEGMENTS_FOR_DOWNLOAD_TASK,downloadTask.getSegmentsForDownloadTask());

        contentValues.put(TPB1,downloadTask.getTPB1());
        contentValues.put(TPB2,downloadTask.getTPB2());
        contentValues.put(TPB3,downloadTask.getTPB3());
        contentValues.put(TPB4,downloadTask.getTPB4());
        contentValues.put(TPB5,downloadTask.getTPB5());
        contentValues.put(TPB6,downloadTask.getTPB6());
        contentValues.put(TPB7,downloadTask.getTPB7());
        contentValues.put(TPB8,downloadTask.getTPB8());
        contentValues.put(TPB9,downloadTask.getTPB9());
        contentValues.put(TPB10,downloadTask.getTPB10());
        contentValues.put(TPB11,downloadTask.getTPB11());
        contentValues.put(TPB12,downloadTask.getTPB12());
        contentValues.put(TPB13,downloadTask.getTPB13());
        contentValues.put(TPB14,downloadTask.getTPB14());
        contentValues.put(TPB15,downloadTask.getTPB15());
        contentValues.put(TPB16,downloadTask.getTPB16());
        contentValues.put(TPB17,downloadTask.getTPB17());
        contentValues.put(TPB18,downloadTask.getTPB18());
        contentValues.put(TPB19,downloadTask.getTPB19());
        contentValues.put(TPB20,downloadTask.getTPB20());
        contentValues.put(TPB21,downloadTask.getTPB21());
        contentValues.put(TPB22,downloadTask.getTPB22());
        contentValues.put(TPB23,downloadTask.getTPB23());
        contentValues.put(TPB24,downloadTask.getTPB24());
        contentValues.put(TPB25,downloadTask.getTPB25());
        contentValues.put(TPB26,downloadTask.getTPB26());
        contentValues.put(TPB27,downloadTask.getTPB27());
        contentValues.put(TPB28,downloadTask.getTPB28());
        contentValues.put(TPB29,downloadTask.getTPB29());
        contentValues.put(TPB30,downloadTask.getTPB30());
        contentValues.put(TPB31,downloadTask.getTPB31());
        contentValues.put(TPB32,downloadTask.getTPB32());

        contentValues.put(TSS1,downloadTask.getTSS1());
        contentValues.put(TSS2,downloadTask.getTSS2());
        contentValues.put(TSS3,downloadTask.getTSS3());
        contentValues.put(TSS4,downloadTask.getTSS4());
        contentValues.put(TSS5,downloadTask.getTSS5());
        contentValues.put(TSS6,downloadTask.getTSS6());
        contentValues.put(TSS7,downloadTask.getTSS7());
        contentValues.put(TSS8,downloadTask.getTSS8());
        contentValues.put(TSS9,downloadTask.getTSS9());
        contentValues.put(TSS10,downloadTask.getTSS10());
        contentValues.put(TSS11,downloadTask.getTSS11());
        contentValues.put(TSS12,downloadTask.getTSS12());
        contentValues.put(TSS13,downloadTask.getTSS13());
        contentValues.put(TSS14,downloadTask.getTSS14());
        contentValues.put(TSS15,downloadTask.getTSS15());
        contentValues.put(TSS16,downloadTask.getTSS16());
        contentValues.put(TSS17,downloadTask.getTSS17());
        contentValues.put(TSS18,downloadTask.getTSS18());
        contentValues.put(TSS19,downloadTask.getTSS19());
        contentValues.put(TSS20,downloadTask.getTSS20());
        contentValues.put(TSS21,downloadTask.getTSS21());
        contentValues.put(TSS22,downloadTask.getTSS22());
        contentValues.put(TSS23,downloadTask.getTSS23());
        contentValues.put(TSS24,downloadTask.getTSS24());
        contentValues.put(TSS25,downloadTask.getTSS25());
        contentValues.put(TSS26,downloadTask.getTSS26());
        contentValues.put(TSS27,downloadTask.getTSS27());
        contentValues.put(TSS28,downloadTask.getTSS28());
        contentValues.put(TSS29,downloadTask.getTSS29());
        contentValues.put(TSS30,downloadTask.getTSS30());
        contentValues.put(TSS31,downloadTask.getTSS31());
        contentValues.put(TSS32,downloadTask.getTSS32());

        contentValues.put(MIME_TYPE,downloadTask.getMimeType());

        writableDB.insert(downloadTasksTBL,null,contentValues);
    }

    public boolean isShowOptimization()
    {
        Cursor cursor = readableDB.query(userPreferencesTBL,new String[]{SHOW_OPTIMIZATION},UP_KEY_ID + "=?",new String[]{String.valueOf(1)},
                null,null,null);
        cursor.moveToFirst();
        int result = cursor.getInt(cursor.getColumnIndexOrThrow(SHOW_OPTIMIZATION));
        cursor.close();
        return result == 1;
    }

    public void updateShowOptimizationStatus()
    {
        ContentValues contentValues = new ContentValues();
        contentValues.put(SHOW_OPTIMIZATION,0);

        writableDB.update(userPreferencesTBL,contentValues,UP_KEY_ID + "=?",new String[]{String.valueOf(1)});
    }

}


