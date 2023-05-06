package com.ginxdroid.flamebrowseranddownloader;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

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



    public DatabaseHandler(Context context) {
        super(context, "flameDatabase", null, 6);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        createTablesIfNotExists(sqLiteDatabase);
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        switch (oldVersion)
        {
            case 1:
                createDifferentThemesTbl(sqLiteDatabase);
                addDarkThemeColumnToUP(sqLiteDatabase);
                addHomePageURLColumnToUP(sqLiteDatabase);
                createQuickLinkTBLWhenModifying(sqLiteDatabase);
                break;
            case 2:
                addDarkThemeColumnToUP(sqLiteDatabase);
                insertDifferentThemes(sqLiteDatabase);
                addHomePageURLColumnToUP(sqLiteDatabase);
                createQuickLinkTBLWhenModifying(sqLiteDatabase);
                break;
            case 3:
                insertDifferentThemes(sqLiteDatabase);
                addHomePageURLColumnToUP(sqLiteDatabase);
                createQuickLinkTBLWhenModifying(sqLiteDatabase);
                break;
            case 4:
                addHomePageURLColumnToUP(sqLiteDatabase);
                createQuickLinkTBLWhenModifying(sqLiteDatabase);
                break;
            case 5:
                createQuickLinkTBLWhenModifying(sqLiteDatabase);
                break;
        }
    }

    private void createQuickLinkTBLWhenModifying(SQLiteDatabase db)
    {
        String CREATE_QUICK_LINKS_TABLE = "CREATE TABLE IF NOT EXISTS "+quickLinksTBL+"("
                +QL_KEY_ID+" INTEGER PRIMARY KEY,"+QL_ITEM_FAVICON_PATH+" TEXT,"+
                QL_ITEM_TITLE+" TEXT,"+QL_ITEM_URL+" TEXT,"+QL_VISIBLE_POSITION+" INTEGER);";

        db.execSQL(CREATE_QUICK_LINKS_TABLE);

        //add default quick link items after table is created.
        {
            QuickLinkModel quickLinkModel = new QuickLinkModel();
            quickLinkModel.setQlURL("https://www.google.com/");
            quickLinkModel.setQlTitle("Google");
            quickLinkModel.setQlFaviconPath("R.drawable");
            quickLinkModel.setQlVisiblePosition(1);

            addQuickLinkItemWhenModifying(db,quickLinkModel);
        }

        {
            QuickLinkModel quickLinkModel = new QuickLinkModel();
            quickLinkModel.setQlURL("https://twitter.com/login?lang=en");
            quickLinkModel.setQlTitle("Twitter");
            quickLinkModel.setQlFaviconPath("R.drawable");
            quickLinkModel.setQlVisiblePosition(2);

            addQuickLinkItemWhenModifying(db,quickLinkModel);
        }

        {
            QuickLinkModel quickLinkModel = new QuickLinkModel();
            quickLinkModel.setQlURL("https://www.instagram.com/accounts/login/?hl=en");
            quickLinkModel.setQlTitle("Instagram");
            quickLinkModel.setQlFaviconPath("R.drawable");
            quickLinkModel.setQlVisiblePosition(3);

            addQuickLinkItemWhenModifying(db,quickLinkModel);
        }

        {
            QuickLinkModel quickLinkModel = new QuickLinkModel();
            quickLinkModel.setQlURL("https://www.facebook.com/");
            quickLinkModel.setQlTitle("Facebook");
            quickLinkModel.setQlFaviconPath("R.drawable");
            quickLinkModel.setQlVisiblePosition(4);

            addQuickLinkItemWhenModifying(db,quickLinkModel);
        }

        {
            QuickLinkModel quickLinkModel = new QuickLinkModel();
            quickLinkModel.setQlURL("https://www.wikipedia.org/");
            quickLinkModel.setQlTitle("Wikipedia");
            quickLinkModel.setQlFaviconPath("R.drawable");
            quickLinkModel.setQlVisiblePosition(5);

            addQuickLinkItemWhenModifying(db,quickLinkModel);
        }

    }

    private void createQuickLinkTBL(SQLiteDatabase db)
    {
        String CREATE_QUICK_LINKS_TABLE = "CREATE TABLE IF NOT EXISTS "+quickLinksTBL+"("
                +QL_KEY_ID+" INTEGER PRIMARY KEY,"+QL_ITEM_FAVICON_PATH+" TEXT,"+
                QL_ITEM_TITLE+" TEXT,"+QL_ITEM_URL+" TEXT,"+QL_VISIBLE_POSITION+" INTEGER);";

        db.execSQL(CREATE_QUICK_LINKS_TABLE);
    }

    private void addQuickLinkItemWhenModifying(SQLiteDatabase db,QuickLinkModel quickLinkModel)
    {
        ContentValues values = new ContentValues();
        values.put(QL_ITEM_TITLE,quickLinkModel.getQlTitle());
        values.put(QL_ITEM_FAVICON_PATH,quickLinkModel.getQlFaviconPath());
        values.put(QL_ITEM_URL,quickLinkModel.getQlURL());
        values.put(QL_VISIBLE_POSITION,quickLinkModel.getQlVisiblePosition());

        //insert our model
        db.insert(quickLinksTBL,null,values);
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

    private void addDarkThemeColumnToUP(SQLiteDatabase db)
    {

        db.execSQL("ALTER TABLE "+userPreferencesTBL+" ADD COLUMN "+DARK_THEME +" INTEGER;");

        ContentValues values = new ContentValues();
        values.put(DARK_THEME, 2);

        //update row
        db.update(userPreferencesTBL, values, UP_KEY_ID + "=?",
                new String[]{String.valueOf(1)});
    }

    private void addHomePageURLColumnToUP(SQLiteDatabase db)
    {

        db.execSQL("ALTER TABLE "+userPreferencesTBL+" ADD COLUMN "+HOME_PAGE_URL +" TEXT;");

        ContentValues values = new ContentValues();
        values.put(HOME_PAGE_URL, "NewTab");

        //update row
        db.update(userPreferencesTBL, values, UP_KEY_ID + "=?",
                new String[]{String.valueOf(1)});
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
    }

    //CRUD: CREATE, READ, UPDATE and DELETE operations

    private void createUserPreferencesTbl(SQLiteDatabase db)
    {
        String CREATE_USER_PREFERENCES_TABLE = "CREATE TABLE IF NOT EXISTS "+ userPreferencesTBL +"("+UP_KEY_ID+" INTEGER PRIMARY KEY,"+
                CURRENT_THEME_ID+" INTEGER,"+IS_DARK_WEB_UI+" INTEGER,"+DARK_THEME+" INTEGER,"+HOME_PAGE_URL+" TEXT);";

        db.execSQL(CREATE_USER_PREFERENCES_TABLE);
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

}


