package io.github.budgetninja.fairwellandroid;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.util.LruCache;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.GetCallback;
import com.parse.GetDataCallback;
import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import net.simonvt.menudrawer.MenuDrawer;

import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.os.Environment.isExternalStorageRemovable;
import static io.github.budgetninja.fairwellandroid.Utility.getDPI;


//Do not modify this code - it is part of the side panel.

public class ContentActivity extends AppCompatActivity {

    private static final String STATE_ACTIVE_POSITION = "net.simonvt.menudrawer.samples.ContentActivity.activePosition";
    private static final String STATE_CONTENT_TEXT = "net.simonvt.menudrawer.samples.ContentActivity.contentText";
    private static final int INDEX_VIEW_STATEMENT = 1;
    private static final int INDEX_ADD_STATEMENT = 2;
    private static final int INDEX_RESOLVE_STATEMENT = 3;
    private static final int POSITION_HOME = 0;
    //private static final int POSITION_FEATURES = 1;
    private static final int POSITION_FRIENDS = 2;
    private static final int POSITION_SMART_SOLVE = 3;
    //private static final int POSITION_SETTING = 4;
    private static final int POSITION_ACCOUNT_SETTING = 5;
    private static final int POSITION_NOTIFICATION_SETTING = 6;
    //private static final int POSITION_OTHERS = 7;
    private static final int POSITION_RATE_THIS_APP = 8;
    private static final int POSITION_ABOUT_US = 9;
    private static final int POSITION_LOGOUT = 10;
    private static int REQUEST_PICTURE =1;
    private MenuDrawer mMenuDrawer;
    private MenuAdapter mAdapter;
    private ListView mList;
    private int mActivePosition = -1;
    private String mContentText;
    private ConnectivityManager connMgr;
    private ParseUser user;
    private Uri photoUri;
    boolean doubleBackToExitPressedOnce = false;
    ImageView userPhotoView;
    private int DPI;
    private int PIXEL_PHOTO;
    private ParseFile userPhotoFile;

    private LruCache<String, Bitmap> mMemoryCache;
    private DiskLruCache mDiskLruCache;
    private final Object mDiskCacheLock = new Object();
    private boolean mDiskCacheStarting = true;
    private static final int DISK_CACHE_COUNT = 1;
    private static final long DISK_CACHE_SIZE = 1024 * 1024 * 10; // 10MB
    public static final String DISK_CACHE_SUBDIR = "images";
    private static final String TAG = "ImageCache";
    private static final int DISK_CACHE_INDEX = 0;
    @Override
    protected void onCreate(Bundle inState) {
        super.onCreate(inState);
        if(getSupportActionBar() != null) {
            getSupportActionBar().setElevation(0);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.nav_icon);
        }

        // Get max available VM memory, exceeding this amount will throw an
        // OutOfMemory exception. Stored in kilobytes as LruCache takes an
        // int in its constructor.
        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);

        // Use 1/8th of the available memory for this memory cache.
        final int cacheSize = maxMemory / 8;

        mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
                return bitmap.getByteCount() / 1024;
            }
        };
        // Initialize disk cache on background thread
        File cacheDir = getDiskCacheDir(getApplicationContext(), DISK_CACHE_SUBDIR);
        new InitDiskCacheTask().execute(cacheDir);
        DPI = getDPI(this.getApplicationContext());
        PIXEL_PHOTO = 200 * (DPI / 160);
        user = ParseUser.getCurrentUser();
        connMgr = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        checkForUpdate();

        if (inState != null) {
            mActivePosition = inState.getInt(STATE_ACTIVE_POSITION);
            mContentText = inState.getString(STATE_CONTENT_TEXT);
        }

        mMenuDrawer = MenuDrawer.attach(this, MenuDrawer.MENU_DRAG_CONTENT);
        mMenuDrawer.setContentView(R.layout.activity_content);
        mMenuDrawer.setTouchMode(MenuDrawer.TOUCH_MODE_FULLSCREEN);

        List<Object> items = new ArrayList<>();
        items.add(new Item(getString(R.string.home), R.drawable.ic_action_select_all_dark));
        items.add(new Category(getString(R.string.features)));
        items.add(new Item(getString(R.string.friends), R.drawable.ic_action_select_all_dark));
        items.add(new Item(getString(R.string.smart_solve), R.drawable.ic_action_select_all_dark));
        items.add(new Category(getString(R.string.setting)));
        items.add(new Item(getString(R.string.account_setting), R.drawable.ic_action_select_all_dark));
        items.add(new Item(getString(R.string.notification_setting), R.drawable.ic_action_select_all_dark));
        items.add(new Category(getString(R.string.others)));
        items.add(new Item(getString(R.string.rate_this_app), R.drawable.ic_action_select_all_dark));
        items.add(new Item(getString(R.string.about_us), R.drawable.ic_action_select_all_dark));
        items.add(new Item(getString(R.string.logout), R.drawable.ic_action_select_all_dark));

        mList = new ListView(this);
        mAdapter = new MenuAdapter(items);
        mList.setAdapter(mAdapter);
        mList.setOnItemClickListener(mItemClickListener);

        mMenuDrawer.setMenuView(mList);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mMenuDrawer.setOnInterceptMoveEventListener(new MenuDrawer.OnInterceptMoveEventListener() {
            @Override
            public boolean isViewDraggable(View v, int dx, int x, int y) {
                return v instanceof SeekBar;
            }
        });

        //3 Buttons Functions
        Button addStatementButton = (Button) findViewById(R.id.addStatementButton);
        addStatementButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                checkForUpdate();
                Intent i = new Intent(ContentActivity.this, ContainerActivity.class);
                i.putExtra("Index", INDEX_ADD_STATEMENT);
                startActivity(i);
            }
        });

        Button resolveStatementButton = (Button) findViewById(R.id.resolveStatementButton);
        resolveStatementButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                checkForUpdate();
                Intent i = new Intent(ContentActivity.this, ContainerActivity.class);
                i.putExtra("Index", INDEX_RESOLVE_STATEMENT);
                startActivity(i);
            }
        });

        Button viewStatementButton = (Button) findViewById(R.id.viewStatementButton);
        viewStatementButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                checkForUpdate();
                Intent i = new Intent(ContentActivity.this, ContainerActivity.class);
                i.putExtra("Index", INDEX_VIEW_STATEMENT);
                startActivity(i);
            }
        });

        //Display Full Name
        TextView name = (TextView) findViewById(R.id.name);
        name.setText(Utility.getUserName(user));
        userPhotoView = (ImageView) findViewById(R.id.user_photo);
        userPhotoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivityForResult(Intent.createChooser(
                        new Intent().setType("image/*").setAction(Intent.ACTION_GET_CONTENT), "Select picture"), REQUEST_PICTURE);
            }
        });
        if(user!=null){
            userPhotoFile = user.getParseFile("photo");
            if(userPhotoFile!=null) {
                loadParseFiletoImageView(userPhotoFile, userPhotoView,userPhotoFile.getName().substring(0, 48));
            }
        }
        //Prompt Facebook and Twitter User to setup email
        if(isNetworkConnected()) {
            if (user.getEmail() == null) {
                setEmailFacebookTwitterUser();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_ACTIVE_POSITION, mActivePosition);
        outState.putString(STATE_CONTENT_TEXT, mContentText);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                mMenuDrawer.toggleMenu();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        checkForUpdate();
        final int drawerState = mMenuDrawer.getDrawerState();
        if (drawerState == MenuDrawer.STATE_OPEN || drawerState == MenuDrawer.STATE_OPENING) {
            mMenuDrawer.closeMenu();
            return;
        }
        if (doubleBackToExitPressedOnce) {
            // this is to close the app entirely, but it will still be in the stack
            Intent a = new Intent(Intent.ACTION_MAIN);
            a.addCategory(Intent.CATEGORY_HOME);
            a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(a);
        }
        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    private AdapterView.OnItemClickListener mItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mActivePosition = position;
            mMenuDrawer.setActiveView(view, position);
            mMenuDrawer.closeMenu();
            checkForUpdate();

            switch(position) {
                case POSITION_HOME:
                    // Nothing really need to be done...
                    break;

                case POSITION_FRIENDS:
                    Intent intent = new Intent(ContentActivity.this,FriendsActivity.class);
                    startActivity(intent);
                    break;

                case POSITION_SMART_SOLVE:
                    Toast.makeText(getApplicationContext(), "Smart solve! ", Toast.LENGTH_SHORT).show();
                    break;

                case POSITION_ACCOUNT_SETTING:
                    Intent intent2 = new Intent(ContentActivity.this,AccountSettingActivity.class);
                    startActivity(intent2);
                    break;

                case POSITION_NOTIFICATION_SETTING:
                    Intent intent3 = new Intent(ContentActivity.this,NotificationSettingActivity.class);
                    startActivity(intent3);
                    break;

                case POSITION_RATE_THIS_APP:
                    Toast.makeText(getApplicationContext(), "Coming soon!", Toast.LENGTH_SHORT).show();
                    //do something
                    break;

                case POSITION_ABOUT_US:
                    if(!isNetworkConnected()) {
                        Toast.makeText(getApplicationContext(), "Check Internet Connection", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    Intent i = new Intent();
                    i.setAction(Intent.ACTION_VIEW);
                    i.addCategory(Intent.CATEGORY_BROWSABLE);
                    i.setData(Uri.parse("http://budgetninja.github.io"));
                    startActivity(i);
                    break;

                case POSITION_LOGOUT:
                    if(!isNetworkConnected()) {
                        Toast.makeText(getApplicationContext(), "Check Internet Connection", Toast.LENGTH_SHORT).show();
                        break;
                    }
                    ParseUser.logOutInBackground(new LogOutCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                Intent intent = new Intent(ContentActivity.this, MainActivity.class);
                                ContentActivity.this.finish();
                                Utility.resetExistingFriendList();
                                Utility.setChangedRecord();
                                startActivity(intent);
                            } else {
                                Toast.makeText(getApplicationContext(), getString(R.string.logout_failed), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                    break;
            }
        }
    };





    private void checkForUpdate(){
        if(isNetworkConnected()){
            if(Utility.checkNewEntryField()){
                Utility.setChangedRecord();
                Utility.generateRawFriendList(user);
            }
        }
    }

    private boolean isNetworkConnected(){
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private void setEmailFacebookTwitterUser(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(ContentActivity.this);
        final LinearLayout layout = new LinearLayout(ContentActivity.this);
        final TextView message = new TextView(ContentActivity.this);
        final EditText userInput = new EditText(ContentActivity.this);
        layout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams para = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        para.setMargins(20, 20, 20, 0);
        message.setText("An email address is required for some functionality. Please link your email address to the account.");
        message.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        message.setLayoutParams(para);
        userInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        userInput.setLayoutParams(para);
        layout.addView(message);
        layout.addView(userInput);
        builder.setTitle("Link your Email");
        builder.setView(layout);

        builder.setPositiveButton("Add", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String email = userInput.getText().toString();
                user.setEmail(email);
                user.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        if(e == null){
                            Toast.makeText(getApplicationContext(), "Success. A verification email was sent to " + email,
                                    Toast.LENGTH_SHORT).show();
                            Utility.setNewEntryFieldForAllFriend();
                            return;
                        }
                        Toast.makeText(getApplicationContext(), "Invalid Email Address", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        builder.setNegativeButton("Do it Later", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.show();
    }



    //Related to Side-Menu
    private static class Item {
        String mTitle;
        int mIconRes;

        Item(String title, int iconRes) {
            mTitle = title;
            mIconRes = iconRes;
        }
    }

    private static class Category {
        String mTitle;

        Category(String title) {
            mTitle = title;
        }
    }

    private class MenuAdapter extends BaseAdapter {
        private List<Object> mItems;

        MenuAdapter(List<Object> items) {
            mItems = items;
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemViewType(int position) {
            return getItem(position) instanceof Item ? 0 : 1;
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public boolean isEnabled(int position) {
            return getItem(position) instanceof Item;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            Object item = getItem(position);

            if (item instanceof Category) {
                if (v == null) {
                    v = getLayoutInflater().inflate(R.layout.menu_row_category, parent, false);
                }
                ((TextView) v).setText(((Category) item).mTitle);
            } else {
                if (v == null) {
                    v = getLayoutInflater().inflate(R.layout.menu_row_item, parent, false);
                }
                TextView tv = (TextView) v;
                tv.setText(((Item) item).mTitle);
                tv.setCompoundDrawablesWithIntrinsicBounds(((Item) item).mIconRes, 0, 0, 0);
            }

            v.setTag(R.id.mdActiveViewPosition, position);

            if (position == mActivePosition) {
                mMenuDrawer.setActiveView(v, position);
            }

            return v;
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode==REQUEST_PICTURE&&resultCode == RESULT_OK) {
            //Uri imageAboutToCrop = Uri.fromFile(fileAboutToCrop);
            photoUri = data.getData();
            //Bitmap bitmapInDisk = getBitmapFromDiskCache(userPhotoFile.getName().substring(0, 48) + "_large");
            ParseFile newPhoto = new ParseFile("photo.JPEG", getBytesFromBitmap(bitmapCompress(getBitmapFromURI(photoUri),50)));
            user.put("photo", newPhoto);
            user.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if(e==null) {
                        loadBitmap(getBytesFromBitmap(getBitmapFromURI(photoUri)),
                                userPhotoView, String.valueOf(photoUri.hashCode()), PIXEL_PHOTO, PIXEL_PHOTO, null);
                    }else{
                        Toast.makeText(getApplicationContext(),"Failed to upload new profile picture, please try again.",Toast.LENGTH_SHORT).show();
                        Log.d("User","Failed to upload profile picture");
                    }
                    //loadBitmap(getBytesFromBitmap(getBitmapFromURI(photoUri)), userPhotoView, String.valueOf(photoUri.hashCode()),PIXEL_PHOTO,PIXEL_PHOTO,bitmapInDisk);
                }
            });
        }

    }
    public static Bitmap bitmapCompress(Bitmap b,int rate){
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.JPEG, rate, stream);
        //BitmapFactory.Options o = new BitmapFactory.Options();
        //o.inJustDecodeBounds = true;
        return BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.toByteArray().length);
    }
    public static byte[] getBytesFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }

    private void loadParseFiletoImageView(ParseFile pf, final ImageView iv, final String keyProvided){
        final Bitmap bitmapInDisk = getBitmapFromDiskCache(keyProvided);
        if (bitmapInDisk!=null) {
            loadBitmap(null, iv, keyProvided, PIXEL_PHOTO, PIXEL_PHOTO,bitmapInDisk);
        }else if(pf!=null){
            pf.getDataInBackground(new GetDataCallback() {
                @Override
                public void done(byte[] bytes, ParseException e) {
                    if (e == null) {
                        loadBitmap(bytes, iv, keyProvided, PIXEL_PHOTO, PIXEL_PHOTO,bitmapInDisk);
                    } else {
                        Log.d("GetData", e.getMessage());
                        Toast.makeText(getApplicationContext(),"Failed to load image",Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
    public Bitmap getBitmapFromURI(Uri u){
        try {
            return MediaStore.Images.Media.getBitmap(this.getContentResolver(), u);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        System.out.println("outHeight and outWidth"+height+","+width);
        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {

                inSampleSize *= 2;
            }
        }
        System.out.println("inSampleSize="+inSampleSize);
        return inSampleSize;
    }
    public static Bitmap decodeSampledBitmapFromByteArray(byte[] res,
                                                          int reqWidth, int reqHeight) {
        System.out.println("decodeSampleBitmapFromByteArray: reqWidth+reqHeight = " + reqWidth+","+reqHeight);
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(res, 0, res.length, options);
        System.out.println("decodeSampleBitmapFromByteArray InjustdecodeBounds: outWidth+outHeight = " + options.outWidth + "," + options.outHeight);
        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        Bitmap result = BitmapFactory.decodeByteArray(res, 0, res.length, options);
        System.out.println("decodeSampleBitmapFromByteArray: resultWidth+resultHeight = " + result.getWidth()+","+result.getHeight());
        return result;
    }
    class BitmapWorkerTask extends AsyncTask<byte[], Void, Bitmap> {
        private final WeakReference<ImageView> imageViewReference;
        private byte[] data;
        private String keyProvided;
        private int reqWidth;
        private int reqHeight;

        public BitmapWorkerTask(ImageView imageView, String s, int w,int h) {
            // Use a WeakReference to ensure the ImageView can be garbage collected
            imageViewReference = new WeakReference<ImageView>(imageView);
            keyProvided = s;
            reqWidth = w;
            reqHeight = h;
        }

        // Decode image in background.
        @Override
        protected Bitmap doInBackground(byte[]... params) {
            data = params[0];
            // Check disk cache in background thread
            Bitmap bitmap = getBitmapFromDiskCache(keyProvided);
            if (bitmap == null) {
                bitmap = decodeSampledBitmapFromByteArray(data,reqWidth, reqHeight);
                addBitmapToMemoryCache(keyProvided, bitmap);
                addBitmapToCache(keyProvided, bitmap);
            }
            return bitmap;
        }

        // Once complete, see if ImageView is still around and set bitmap.
        @Override
        protected void onPostExecute(Bitmap bitmap) {
            if (isCancelled()) {
                bitmap = null;
            }
            if (imageViewReference != null && bitmap != null) {
                final ImageView imageView = imageViewReference.get();
                final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
                if (BitmapWorkerTask.this == bitmapWorkerTask && imageView != null) {
                    imageView.setImageBitmap(bitmap);
                }
            }
        }
    }
    public void addBitmapToMemoryCache(String key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }
    }

    public static Bitmap getBitmapFromByte(byte[] bt){
        return BitmapFactory.decodeByteArray(bt, 0, bt.length);
    }
    public Bitmap getBitmapFromMemCache(String key) {
        return mMemoryCache.get(key);
    }


    public void loadBitmap(byte[] sourceByteArray, ImageView imageView, String keyProvided, int w, int h, Bitmap bitmapInDisk) {
        if(sourceByteArray==null){
            imageView.setImageBitmap(bitmapInDisk);
        }else if (cancelPotentialWork(sourceByteArray, imageView)) {
            Bitmap bitmap = getBitmapFromMemCache(keyProvided);
            if(bitmap!=null){
                imageView.setImageBitmap(bitmap);
            }else {
                bitmap = getBitmapFromDiskCache(keyProvided);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                } else {
                    final BitmapWorkerTask task = new BitmapWorkerTask(imageView, keyProvided,w,h);
                    final AsyncDrawable asyncDrawable =
                            new AsyncDrawable(getResources(), BitmapFactory.decodeResource(getResources(), R.drawable.profilepic), task);
                    imageView.setImageDrawable(asyncDrawable);
                    task.execute(sourceByteArray);
                }
            }
        }
    }

    public static boolean cancelPotentialWork(byte[] data, ImageView imageView) {
        final BitmapWorkerTask bitmapWorkerTask = getBitmapWorkerTask(imageView);
        if(data==null){
            return true;
        }
        if (bitmapWorkerTask != null) {
            final byte[] bitmapData = bitmapWorkerTask.data;
            // If bitmapData is not yet set or it differs from the new data
            if (bitmapData.length==0 || bitmapData != data) {
                // Cancel previous task
                bitmapWorkerTask.cancel(true);
            } else {
                // The same work is already in progress
                return false;
            }
        }
        // No task associated with the ImageView, or an existing task was cancelled
        return true;
    }
    private static BitmapWorkerTask getBitmapWorkerTask(ImageView imageView) {
        if (imageView != null) {
            final Drawable drawable = imageView.getDrawable();
            if (drawable instanceof AsyncDrawable) {
                final AsyncDrawable asyncDrawable = (AsyncDrawable) drawable;
                return asyncDrawable.getBitmapWorkerTask();
            }
        }
        return null;
    }
    static class AsyncDrawable extends BitmapDrawable {
        private final WeakReference<BitmapWorkerTask> bitmapWorkerTaskReference;

        public AsyncDrawable(Resources res, Bitmap bitmap,
                             BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference =
                    new WeakReference<BitmapWorkerTask>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }


    class InitDiskCacheTask extends AsyncTask<File, Void, Void> {
        @Override
        protected Void doInBackground(File... params) {
            synchronized (mDiskCacheLock) {
                File cacheDir = params[0];
                try {
                    mDiskLruCache = DiskLruCache.open(cacheDir, FairwellApplication.APP_VERSION,DISK_CACHE_COUNT,DISK_CACHE_SIZE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mDiskCacheStarting = false; // Finished initialization
                mDiskCacheLock.notifyAll(); // Wake any waiting threads
            }
            return null;
        }
    }



    public void addBitmapToCache(String key, Bitmap bitmap) {
        // Add to memory cache as before
        if (getBitmapFromMemCache(key) == null) {
            mMemoryCache.put(key, bitmap);
        }

        // Also add to disk cache
        synchronized (mDiskCacheLock) {
            try {
                if (mDiskLruCache != null && mDiskLruCache.get(key) == null) {
                    final DiskLruCache.Editor editor = mDiskLruCache.edit(key);
                    OutputStream out;
                    if (editor != null) {
                        out = editor.newOutputStream(DISK_CACHE_INDEX);
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                        editor.commit();
                        out.close();
                    }
                    //mDiskLruCache.put(key, bitmap);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Bitmap getBitmapFromDiskCache(String key) {
        Bitmap bitmap = null;
        synchronized (mDiskCacheLock) {
            // Wait while disk cache is started from background thread
            while (mDiskCacheStarting) {
                try {
                    mDiskCacheLock.wait();
                } catch (InterruptedException e) {}
            }
            if (mDiskLruCache != null) {
                try {
                    final DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);

                    InputStream inputStream = null;
                    if (snapshot != null) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Disk cache hit");
                        }
                        inputStream = snapshot.getInputStream(DISK_CACHE_INDEX);
                        if (inputStream != null) {
                            FileDescriptor fd = ((FileInputStream) inputStream).getFD();

                            // Decode bitmap, but we don't want to sample so give
                            // MAX_VALUE as the target dimensions
                            bitmap = getBitmapFromByte(IOUtils.toByteArray(inputStream));
                            //bitmap = ImageResizer.decodeSampledBitmapFromDescriptor(fd, Integer.MAX_VALUE, Integer.MAX_VALUE, this);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return bitmap;
    }

    // Creates a unique subdirectory of the designated app cache directory. Tries to use external
// but if not mounted, falls back on internal storage.
    public static File getDiskCacheDir(Context context, String uniqueName) {
        // Check if media is mounted or storage is built-in, if so, try and use external cache dir
        // otherwise use internal cache dir
        final String cachePath =
                Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                        !isExternalStorageRemovable() ? getExternalCacheDir(context).getPath() :
                        context.getCacheDir().getPath();
        return new File(cachePath + File.separator + uniqueName);
    }

    /**
     * Get the external app cache directory.
     *
     * @param context The context to use
     * @return The external cache dir
     */
    @TargetApi(Build.VERSION_CODES.FROYO)
    public static File getExternalCacheDir(Context context) {
        if (VersionChecker.hasFroyo()) {
            return context.getExternalCacheDir();
        }

        // Before Froyo we need to construct the external cache dir ourselves
        final String cacheDir = "/Android/data/" + context.getPackageName() + "/cache/";
        return new File(Environment.getExternalStorageDirectory().getPath() + cacheDir);
    }


}
