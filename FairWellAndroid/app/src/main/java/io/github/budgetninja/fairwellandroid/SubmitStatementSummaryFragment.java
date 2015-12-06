package io.github.budgetninja.fairwellandroid;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.util.LruCache;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseUser;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import io.github.budgetninja.fairwellandroid.FriendObject.Friend;
import io.github.budgetninja.fairwellandroid.StatementObject.Statement;
import io.github.budgetninja.fairwellandroid.StatementObject.SummaryStatement;

import static io.github.budgetninja.fairwellandroid.AddStatementFragment.SPLIT_BY_RATIO;
import static io.github.budgetninja.fairwellandroid.AddStatementFragment.SPLIT_EQUALLY;
import static io.github.budgetninja.fairwellandroid.AddStatementFragment.SPLIT_UNEQUALLY;
import static io.github.budgetninja.fairwellandroid.ContentActivity.POSITION_HOME;
import static io.github.budgetninja.fairwellandroid.HomepageFragment.decodeSampledBitmapFromByteArray;
import static io.github.budgetninja.fairwellandroid.HomepageFragment.getDiskCacheDir;
import static io.github.budgetninja.fairwellandroid.Utility.getDPI;


public class SubmitStatementSummaryFragment extends Fragment {

    private View previousState;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem item = menu.findItem(R.id.action_refresh);
        item.setVisible(false);
        super.onCreateOptionsMenu(menu, inflater);
    }

    private DateFormat dateFormat;
    private ParseUser user;
    private ContentActivity parent;
    private ParseFile picture;
    private String descriptionText, categoryText, sumbitByText, noteText;
    private Date date, deadline;
    private int modeNum, unknownNum;
    private double amountNum, runningDif;
    private Friend payee;
    private List<Pair<Friend, Double>> payer;
    private TextView noteView, descriptionView, categoryView, dateView, deadlineView, totalAmountView, modeView, sumbitByView;
    private ImageView pictureView;
    private LinearLayout layout;
    private LruCache<String, Bitmap> mMemoryCache;
    private DiskLruCache mDiskLruCache;

    private int PIXEL_PHOTO;

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setHasOptionsMenu(true);
        parent = (ContentActivity)getActivity();
        user = ParseUser.getCurrentUser();
        dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH);
        previousState = null;
        runningDif = 0.00;

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
        PIXEL_PHOTO = 100 * (DPI / 160);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ActionBar actionBar = parent.getSupportActionBar();
        if (actionBar != null) {
            final Drawable upArrow = ContextCompat.getDrawable(getContext(), R.drawable.abc_ic_ab_back_mtrl_am_alpha);
            upArrow.setColorFilter(ContextCompat.getColor(getContext(), R.color.coolBackground), PorterDuff.Mode.SRC_ATOP);
            actionBar.setHomeAsUpIndicator(upArrow);
        }
        parent.setTitle("Summary");
        if(previousState != null){
            return previousState;
        }

        View view = inflater.inflate(R.layout.fragment_statement_summary, container, false);
        noteView = (TextView)view.findViewById(R.id.summary_note);
        descriptionView = (TextView) view.findViewById(R.id.summary_description);
        categoryView = (TextView) view.findViewById(R.id.summary_category);
        dateView = (TextView) view.findViewById(R.id.summary_date);
        deadlineView = (TextView) view.findViewById(R.id.summary_deadline);
        totalAmountView = (TextView) view.findViewById(R.id.summary_totalAmount);
        pictureView = (ImageView) view.findViewById(R.id.picture);
        modeView = (TextView) view.findViewById(R.id.summary_mode);
        sumbitByView = (TextView) view.findViewById(R.id.summary_submitBy);
        layout = (LinearLayout) view.findViewById(R.id.summary_tableLayout);
        Button cancelButton = (Button) view.findViewById(R.id.summary_cancelButton);
        Button modifyButton = (Button) view.findViewById(R.id.summary_modifyButton);
        Button submitButton = (Button) view.findViewById(R.id.summary_submitButton);
        Button deleteButton = (Button) view.findViewById(R.id.summary_deleteButton);
        deleteButton.setVisibility(View.GONE);
        Button confirmButton = (Button) view.findViewById(R.id.summary_confirmButton);
        confirmButton.setVisibility(View.GONE);
        Button rejectButton = (Button) view.findViewById(R.id.summary_rejectButton);
        rejectButton.setVisibility(View.GONE);
        LinearLayout paymentOptionLayout = (LinearLayout) view.findViewById(R.id.resolve_Option_layout);
        paymentOptionLayout.setVisibility(View.GONE);

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.layoutManage(POSITION_HOME);
            }
        });

        modifyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                parent.mMenuDrawer.closeMenu(false);
                parent.fragMgr.popBackStack();
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SubmitStatementLoading submitStatementLoading = new SubmitStatementLoading(parent);
                submitStatementLoading.execute();
            }
        });

        previousState = view;
        return view;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                parent.mMenuDrawer.closeMenu(false);
                parent.fragMgr.popBackStack();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void setData(SummaryStatement data){
        noteText = data.note;
        picture = data.picture;
        descriptionText = data.description;
        categoryText = data.category;
        date = data.date;
        deadline = data.deadline;
        sumbitByText = Utility.getProfileName(user);
        modeNum = data.mode;
        unknownNum = data.unknown;
        amountNum = data.totalAmount;
        this.payee = data.payee;
        this.payer = data.payer;
        displayData();
    }

    private void displayData(){
        descriptionView.setText(descriptionText);
        categoryView.setText(categoryText);
        dateView.setText(dateFormat.format(date));
        deadlineView.setText(dateFormat.format(deadline));
        totalAmountView.setText("$ " + String.format("%.2f", this.amountNum));
        modeView.setText(Integer.toString(modeNum));
        sumbitByView.setText("YOU");

        if(picture != null) {
            showProgressBar();
            loadParseFiletoImageView(picture, pictureView, picture.getName().substring(0, 48));
        } else {
            ((LinearLayout)pictureView.getParent()).setVisibility(View.GONE);
        }

        if(noteText != null){
            if(noteText.isEmpty()){ ((LinearLayout)noteView.getParent()).setVisibility(View.GONE); }
            else{ noteView.setText(noteText); }
        } else {
            ((LinearLayout)noteView.getParent()).setVisibility(View.GONE);
        }

        String payeeName;
        if(this.payee == null){ payeeName = "YOU"; }
        else { payeeName = this.payee.displayName; }

        LinearLayout memberRow;
        TextView payee, payer, amount;
        runningDif = this.amountNum;

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.CENTER_VERTICAL;
        params.weight = 1f;

        for(int i = 0; i < this.payer.size(); i++){
            String payerName = this.payer.get(i).first.displayName;
            memberRow = new LinearLayout(parent);
            memberRow.setPadding(0, 0, 0,Utility.getPixel(2, getResources()));

            payee = new TextView(parent);
            payee.setGravity(Gravity.CENTER);
            payee.setText(payeeName);
            payee.setLayoutParams(params);

            payer = new TextView(parent);
            payer.setGravity(Gravity.CENTER);
            payer.setText(payerName.equals("Self") ? "YOU" : payerName);
            payer.setLayoutParams(params);

            amount = new TextView(parent);
            amount.setGravity(Gravity.CENTER);
            amount.setText("$ " + String.format("%.2f", this.payer.get(i).second));
            amount.setLayoutParams(params);
            runningDif -= this.payer.get(i).second;

            memberRow.addView(payer);
            memberRow.addView(payee);
            memberRow.addView(amount);
            layout.addView(memberRow);
        }
        if(runningDif > -0.009 && runningDif < 0.009 ) { runningDif = 0.00; }

        switch (modeNum){
            case SPLIT_EQUALLY:
                modeView.setText("Split Equally");

                if(unknownNum > 0 && runningDif > 0.009){
                    String entry;
                    if(unknownNum == 1) { entry = "(1 non-user)"; }
                    else{ entry = "(" + Integer.toString(unknownNum) + " non-users)"; }

                    memberRow = new LinearLayout(parent);
                    memberRow.setPadding(0, 0, 0, Utility.getPixel(2, getResources()));

                    payee = new TextView(parent);
                    payee.setGravity(Gravity.CENTER);
                    payee.setText(payeeName);
                    payee.setLayoutParams(params);

                    payer = new TextView(parent);
                    payer.setGravity(Gravity.CENTER);
                    payer.setText(entry);
                    payer.setLayoutParams(params);

                    amount = new TextView(parent);
                    amount.setGravity(Gravity.CENTER);
                    amount.setText("$ " + String.format("%.2f", runningDif));
                    amount.setLayoutParams(params);

                    memberRow.addView(payer);
                    memberRow.addView(payee);
                    memberRow.addView(amount);
                    layout.addView(memberRow);
                }
                break;

            case SPLIT_UNEQUALLY:
                modeView.setText("Split Unequally");

                if(runningDif > 0.009){
                    memberRow = new LinearLayout(parent);
                    memberRow.setPadding(0, 0, 0, Utility.getPixel(2, getResources()));

                    payee = new TextView(parent);
                    payee.setGravity(Gravity.CENTER);
                    payee.setText(payeeName);
                    payee.setLayoutParams(params);

                    payer = new TextView(parent);
                    payer.setGravity(Gravity.CENTER);
                    payer.setText("(Some non-users)");
                    payer.setLayoutParams(params);

                    amount = new TextView(parent);
                    amount.setGravity(Gravity.CENTER);
                    amount.setText("$ " + String.format("%.2f", runningDif));
                    amount.setLayoutParams(params);

                    memberRow.addView(payer);
                    memberRow.addView(payee);
                    memberRow.addView(amount);
                    layout.addView(memberRow);
                }
                break;

            case SPLIT_BY_RATIO:
                modeView.setText("Split by Ratio");
                break;
        }
    }

    private class SubmitStatementLoading extends AsyncTask<Boolean, Void, Boolean> {
        private ProgressDialog dialog;

        public SubmitStatementLoading(Context activity) {
            dialog = new ProgressDialog(activity);
        }

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Submitting Statement... Please Wait...");
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected Boolean doInBackground(Boolean... params) {
            try {
                ParseObject object = new ParseObject("StatementGroup");
                Pair<Boolean, Boolean> isCurrentUserInvolved = new Pair<>(false, false);

                if (payee == null) {              //payee == current user
                    object.put("payee", user);
                    object.put("payeeConfirm", true);
                    isCurrentUserInvolved = new Pair<>(true, true);
                } else {                           //payee == someone else
                    object = payee.insertParseUser(object, "payee");
                    object.put("payeeConfirm", false);
                }

                object.put("description", descriptionText);
                object.put("category", categoryText);
                object.put("paymentAmount", amountNum);
                object.put("submittedBy", sumbitByText);
                object.put("mode", modeNum);
                object.put("date", date);
                object.put("deadline", deadline);
                object.put("unknown", unknownNum);
                object.put("unknownAmount", runningDif);
                if(picture != null){ object.put("picture",picture); }
                if(noteText != null){ object.put("note",noteText); }

                ArrayList<ParseObject> statementArray = new ArrayList<>();
                for (int i = 0; i < payer.size(); i++) {
                    Pair<Friend, Double> item = payer.get(i);
                    if (item.first.displayName.equals("Self") && payee == null) {       //The two cases when payer == payee
                        continue;
                    }
                    if (!item.first.displayName.equals("Self") && payee != null) {
                        if (item.first.isEqual(payee)) {
                            continue;
                        }
                    }

                    ParseObject statementObject = new ParseObject("Statement");
                    if (item.first.displayName.equals("Self") && payee != null) {       //The case when payee is someone else and payer is current user
                        statementObject = payee.insertFriendship(statementObject, "friendship");
                        payee.setPendingStatement();
                        statementObject.put("payer", user);
                        isCurrentUserInvolved = new Pair<>(true, false);
                    } else {
                        if (payee != null) {                                    //The case when payee is someone else
                            ParseObject temp = payee.generateFriendToFriendRelationship(item.first);
                            temp.put("pendingStatement", true);
                            temp.save();
                            statementObject.put("friendship", temp);
                            item.first.notifyChange(null, null);
                        } else {                                                //The case when payee is current user
                            statementObject = item.first.insertFriendship(statementObject, "friendship");
                            item.first.setPendingStatement();
                        }
                        statementObject = item.first.insertParseUser(statementObject, "payer");
                    }
                    statementObject.put("payerConfirm", false);
                    statementObject.put("payerReject", false);
                    statementObject.put("payerPaid", false);
                    statementObject.put("paymentPending", false);
                    statementObject.put("amount", item.second);
                    statementObject.save();
                    statementArray.add(statementObject);
                }

                SystemClock.sleep(2000);                        //Attempt to resolve the following issue

                object.put("payer", statementArray);
                ParseObject objectCopy = object;
                object.save();                               //For unknown reason, sometime this line causes a ConcurrentModificationException
                if (isCurrentUserInvolved.first) {
                    Statement statement = new Statement(objectCopy, isCurrentUserInvolved.second);
                    ParseObject temp = Utility.getRawListLocation();
                    temp.getList("statementList").add(objectCopy);
                    temp.pinInBackground();
                    Utility.addToExistingStatementList(statement);
                }

                //Update Dashboard after everything is successful
                if (payee == null) {
                    Utility.editNewEntryField(user, "A new statement, <" + descriptionText + ">, was added");
                    for (int i = 0; i < payer.size(); i++) {
                        Pair<Friend, Double> item = payer.get(i);
                        if (!item.first.displayName.equals("Self")) {
                            item.first.notifyChange(null, "A new statement, <" + descriptionText + ">, was added");
                        }
                    }
                }
                else{ payee.notifyChange(null, "A new statement, <" + descriptionText + ">, was added"); }

                return true;
            } catch (ParseException|ConcurrentModificationException e){
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            parent.layoutManage(POSITION_HOME);
            if(result) {
                Toast.makeText(parent, "Statement Submitted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(parent, "Submission Failed", Toast.LENGTH_SHORT).show();
            }
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
            while (FairwellApplication.mDiskCacheStarting) try{
                FairwellApplication.mDiskCacheLock.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (mDiskLruCache != null) try {
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
        return bitmap;
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
