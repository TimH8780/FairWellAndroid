package io.github.budgetninja.fairwellandroid;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.camera.CropImageIntentBuilder;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;
import static android.os.Environment.isExternalStorageRemovable;
import static io.github.budgetninja.fairwellandroid.ContentActivity.ALL_REFRESH;
import static io.github.budgetninja.fairwellandroid.ContentActivity.INDEX_ADD_STATEMENT;
import static io.github.budgetninja.fairwellandroid.ContentActivity.INDEX_RESOLVE_STATEMENT;
import static io.github.budgetninja.fairwellandroid.ContentActivity.INDEX_VIEW_STATEMENT;
import static io.github.budgetninja.fairwellandroid.ContentActivity.OWE_BALANCE;
import static io.github.budgetninja.fairwellandroid.ContentActivity.OWN_BALANCE;
import static io.github.budgetninja.fairwellandroid.Utility.getBytesFromBitmap;
import static io.github.budgetninja.fairwellandroid.Utility.getDPI;


public class HomepageFragment extends Fragment {

    private final static int REQUEST_PICTURE =1;
    private final static int REQUEST_CROP_PICTURE = 2;
    private final static int REQUEST_CAMERA = 3;

    private TextView oweBalanceView;
    private TextView ownBalanceView;
    private ParseUser user;
    private ContentActivity parent;
    private DecimalFormat format;
    private int PIXEL_PHOTO;
    private LruCache<String, Bitmap> mMemoryCache;
    private DiskLruCache mDiskLruCache;
    ImageView userPhotoView;

    private String mCurrentPhotoPath;

    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setHasOptionsMenu(true);
        user = ParseUser.getCurrentUser();
        parent = (ContentActivity)getActivity();
        format = new DecimalFormat("$#,###,###,##0.00;- $#,###,###,##0.00");

        // Get max available VM memory, exceeding this capacity will throw an
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
        File cacheDir = getDiskCacheDir(parent.getApplicationContext(), FairwellApplication.DISK_CACHE_SUBDIR);
        new InitDiskCacheTask().execute(cacheDir);
        int DPI = getDPI(parent.getApplicationContext());
        PIXEL_PHOTO = 200 * (DPI / 160);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        ActionBar actionBar =  parent.getSupportActionBar();
        if(actionBar != null) {
            actionBar.setHomeAsUpIndicator(R.drawable.nav_icon);
        }
        parent.setTitle("Fairwell");

        oweBalanceView = (TextView) view.findViewById(R.id.homepage_balance_owe);
        if (OWE_BALANCE >= 0){
            oweBalanceView.setText("- " + format.format(OWE_BALANCE));
        } else{
            oweBalanceView.setText(format.format(OWE_BALANCE));
        }

        ownBalanceView = (TextView) view.findViewById(R.id.homepage_balance_own);
        ownBalanceView.setText("+ " +format.format(OWN_BALANCE));

        //3 Buttons Functions
        Button addStatementButton = (Button) view.findViewById(R.id.addStatementButton);
        addStatementButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                parent.layoutManage(INDEX_ADD_STATEMENT);
            }
        });

        Button resolveStatementButton = (Button) view.findViewById(R.id.resolveStatementButton);
        resolveStatementButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                parent.layoutManage(INDEX_RESOLVE_STATEMENT);
            }
        });

        Button viewStatementButton = (Button) view.findViewById(R.id.viewStatementButton);
        viewStatementButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                parent.layoutManage(INDEX_VIEW_STATEMENT);
            }
        });

        //Display Full Name
        TextView name = (TextView) view.findViewById(R.id.name);
        name.setText(Utility.getProfileName(user));

        //Picture
        userPhotoView = (ImageView) view.findViewById(R.id.user_photo);
        userPhotoView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                if(parent.isNetworkConnected()) {
                    promptUploadPhotoDialog();
                    return true;
                } else {
                    Toast.makeText(parent, "Check Internet Connection", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });

        int DPI = getDPI(parent.getApplication());

        if(DPI < 300){
//            ImageView icon_logo = (ImageView) view.findViewById(R.id.user_photo);
//            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//            lp.setMargins(120, 10, 0, 10);
//
//            icon_logo.setLayoutParams(lp);
        }




        if(user != null){
            ParseFile userPhotoFile = user.getParseFile("photo");
            if(userPhotoFile != null) {
                loadParseFiletoImageView(userPhotoFile, userPhotoView, userPhotoFile.getName().substring(0, 48));
            }
        }

        return view;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                parent.mMenuDrawer.toggleMenu();
                return true;

            case R.id.action_refresh:
                ContentActivity.UpdateInBackground task = parent.new UpdateInBackground(parent, ALL_REFRESH);
                task.execute();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        final File croppedImageFile = new File(getActivity().getFilesDir(), "temp.jpg");
        Uri croppedImageUri = Uri.fromFile(croppedImageFile);
        if (requestCode == REQUEST_PICTURE && resultCode == RESULT_OK) {
            CropImageIntentBuilder cropImage = new CropImageIntentBuilder(PIXEL_PHOTO, PIXEL_PHOTO, croppedImageUri);
            cropImage.setOutlineColor(0xFF03A9F4);
            cropImage.setSourceImage(data.getData());
            startActivityForResult(cropImage.getIntent(getContext()), REQUEST_CROP_PICTURE);
        }
        else if(requestCode == REQUEST_CAMERA && resultCode == RESULT_OK){
            Uri contentUri = Uri.fromFile(new File(mCurrentPhotoPath));
            galleryAddPic();  //add photo to gallery so that system media controller could access to it
            mCurrentPhotoPath = null;
            CropImageIntentBuilder cropImage = new CropImageIntentBuilder(PIXEL_PHOTO, PIXEL_PHOTO, croppedImageUri);
            cropImage.setOutlineColor(0xFF03A9F4);
            cropImage.setSourceImage(contentUri);
            startActivityForResult(cropImage.getIntent(getContext()), REQUEST_CROP_PICTURE);
        }
        else if (requestCode == REQUEST_CROP_PICTURE && resultCode == Activity.RESULT_OK) {
            showProgressBar();
            final Bitmap photoBitmap = BitmapFactory.decodeFile(croppedImageFile.getAbsolutePath());
            ParseFile newPhoto = new ParseFile("photo.JPEG", getBytesFromBitmap(photoBitmap, 50));
            user.put("photo", newPhoto);
            user.saveInBackground(new SaveCallback() {
                @Override
                public void done(ParseException e) {
                    if (e == null) {
                        loadBitmap(getBytesFromBitmap(photoBitmap, 50),
                                userPhotoView, user.getParseFile("photo").getName().substring(0, 48), PIXEL_PHOTO, PIXEL_PHOTO, null);
                    } else {
                        Toast.makeText(parent.getApplicationContext(), "Failed to upload new profile picture, please try again.", Toast.LENGTH_SHORT).show();
                        Log.d("User", "Failed to upload profile picture");
                    }
                }
            });
        }
    }

    protected void setBalance(){
        if(oweBalanceView !=null) {
            if(OWE_BALANCE >= 0){
                oweBalanceView.setText("- " + format.format(OWE_BALANCE));
            } else{
                oweBalanceView.setText(format.format(OWE_BALANCE));
            }
        }
        if(ownBalanceView != null){
            ownBalanceView.setText("+ " +format.format(OWN_BALANCE));
        }
    }

    private void loadParseFiletoImageView(ParseFile pf, final ImageView iv, final String keyProvided){
        final Bitmap bitmapInDisk = getBitmapFromDiskCache(keyProvided);
        if (bitmapInDisk != null) {
            loadBitmap(null, iv, keyProvided, PIXEL_PHOTO, PIXEL_PHOTO, bitmapInDisk);
        } else if(pf != null){
            pf.getDataInBackground(new GetDataCallback() {
                @Override
                public void done(byte[] bytes, ParseException e) {
                    if (e == null) {
                        loadBitmap(bytes, iv, keyProvided, PIXEL_PHOTO, PIXEL_PHOTO, bitmapInDisk);
                    } else {
                        Log.d("GetData", e.getMessage());
                        Toast.makeText(parent.getApplicationContext(),"Failed to load image",Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }

    public static Bitmap decodeSampledBitmapFromByteArray(byte[] res, int reqWidth, int reqHeight) {
        System.out.println("decodeSampleBitmapFromByteArray: reqWidth+reqHeight = " + reqWidth + "," + reqHeight);
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(res, 0, res.length, options);
        System.out.println("decodeSampleBitmapFromByteArray InjustdecodeBounds: outWidth+outHeight = " + options.outWidth + "," + options.outHeight);
        // Calculate inSampleSize
        options.inSampleSize = Utility.calculateInSampleSize(options, reqWidth, reqHeight);
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        Bitmap result = BitmapFactory.decodeByteArray(res, 0, res.length, options);
        System.out.println("decodeSampleBitmapFromByteArray: resultWidth+resultHeight = " + result.getWidth() + "," + result.getHeight());
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
            imageViewReference = new WeakReference<>(imageView);
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
                bitmap = decodeSampledBitmapFromByteArray(data, reqWidth, reqHeight);
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
                hideProgressBar();
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
        if(sourceByteArray == null){
            imageView.setImageBitmap(bitmapInDisk);
            hideProgressBar();
        } else if (cancelPotentialWork(sourceByteArray, imageView)) {
            Bitmap bitmap = getBitmapFromMemCache(keyProvided);
            if(bitmap != null){
                imageView.setImageBitmap(bitmap);
                hideProgressBar();
            } else {
                bitmap = getBitmapFromDiskCache(keyProvided);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                    hideProgressBar();
                } else if(isAdded()){
                    Log.d("loadBitmap", "Fragment Attached");
                    final BitmapWorkerTask task = new BitmapWorkerTask(imageView, keyProvided,w,h);
                    final AsyncDrawable asyncDrawable =
                            new AsyncDrawable(getResources(), BitmapFactory.decodeResource(getResources(), R.drawable.profilepic), task);
                    imageView.setImageDrawable(asyncDrawable);
                    task.execute(sourceByteArray);
                    hideProgressBar();
                } else {
                    Log.d("loadBitmap", "Fragment Not Attached");
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
            if (bitmapData.length == 0 || bitmapData != data) {
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

        public AsyncDrawable(Resources res, Bitmap bitmap, BitmapWorkerTask bitmapWorkerTask) {
            super(res, bitmap);
            bitmapWorkerTaskReference = new WeakReference<>(bitmapWorkerTask);
        }

        public BitmapWorkerTask getBitmapWorkerTask() {
            return bitmapWorkerTaskReference.get();
        }
    }

    class InitDiskCacheTask extends AsyncTask<File, Void, Void> {
        @Override
        protected Void doInBackground(File... params) {
            synchronized (FairwellApplication.mDiskCacheLock) {
                File cacheDir = params[0];
                try {
                    mDiskLruCache = DiskLruCache.open(cacheDir, FairwellApplication.APP_VERSION,FairwellApplication.DISK_CACHE_COUNT,FairwellApplication.DISK_CACHE_SIZE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                FairwellApplication.mDiskCacheStarting = false;    // Finished initialization
                FairwellApplication.mDiskCacheLock.notifyAll();     // Wake any waiting threads
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
        synchronized (FairwellApplication.mDiskCacheLock) {
            try {
                if (mDiskLruCache != null && mDiskLruCache.get(key) == null) {
                    final DiskLruCache.Editor editor = mDiskLruCache.edit(key);
                    OutputStream out;
                    if (editor != null) {
                        out = editor.newOutputStream(FairwellApplication.DISK_CACHE_INDEX);
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
        synchronized (FairwellApplication.mDiskCacheLock) {
            // Wait while disk cache is started from background thread
            while (FairwellApplication.mDiskCacheStarting) {
                try {
                    FairwellApplication.mDiskCacheLock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (mDiskLruCache != null) {
                try {
                    final DiskLruCache.Snapshot snapshot = mDiskLruCache.get(key);
                    InputStream inputStream;
                    if (snapshot != null) {
                        if (BuildConfig.DEBUG) {
                            Log.d(FairwellApplication.TAG, "Disk cache hit");
                        }
                        inputStream = snapshot.getInputStream(FairwellApplication.DISK_CACHE_INDEX);
                        if (inputStream != null) {
                            //FileDescriptor fd = ((FileInputStream) inputStream).getFD();

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
        final String cachePath = (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) ||
                !isExternalStorageRemovable()) ? getExternalCacheDir(context).getPath() : context.getCacheDir().getPath();
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

    public void promptUploadPhotoDialog(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Upload a New Picture as Profile Photo?");
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //startActivityForResult(Intent.createChooser(new Intent().setType("image/*").setAction(Intent.ACTION_GET_CONTENT), "Select picture"), REQUEST_PICTURE);
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

                // Set dialog properties
                builder.setItems(new String[]{"Gallery", "Camera"}, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dlg, int which) {
                        // The 'which' argument contains the index position
                        // of the selected item
                        if (which == 0) { //select from gallery
                            Intent intent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            startActivityForResult(intent, REQUEST_PICTURE);
                        } else if (which == 1) { //select to take a photo
                            dispatchTakePictureIntent();
                        }
                    }
                });
                final AlertDialog dlg = builder.create();
                dlg.show();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        final AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     *  Save the Image file of photo captured
     **/
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(parent.getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.i("IOException camera","IOException creating image file");
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri mCurrentPhotoUri = Uri.fromFile(photoFile);
                //takePictureIntent.setData(tempUri);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCurrentPhotoUri);
                startActivityForResult(takePictureIntent, REQUEST_CAMERA);
            }
        }
    }

    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        parent.sendBroadcast(mediaScanIntent);
    }

    private void showProgressBar(){
        View progressView = getActivity().findViewById(R.id.loadingPanel);
        if(progressView != null){
            progressView.setVisibility(View.VISIBLE);
            progressView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                }
            });
        }
    }

    private void hideProgressBar(){
        View progressView = getActivity().findViewById(R.id.loadingPanel);
        if(progressView != null){
            progressView.setVisibility(View.GONE);
        }
    }
}
