package io.github.budgetninja.fairwellandroid;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.util.Log;
import android.util.LruCache;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
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
import java.util.List;
import java.util.Locale;

import io.github.budgetninja.fairwellandroid.StatementObject.Statement;
import io.github.budgetninja.fairwellandroid.StatementObject.SubStatement;

import static io.github.budgetninja.fairwellandroid.AddStatementFragment.SPLIT_BY_RATIO;
import static io.github.budgetninja.fairwellandroid.AddStatementFragment.SPLIT_EQUALLY;
import static io.github.budgetninja.fairwellandroid.AddStatementFragment.SPLIT_UNEQUALLY;
import static io.github.budgetninja.fairwellandroid.HomepageFragment.decodeSampledBitmapFromByteArray;
import static io.github.budgetninja.fairwellandroid.HomepageFragment.getDiskCacheDir;
import static io.github.budgetninja.fairwellandroid.StatementObject.CONFIRM;
import static io.github.budgetninja.fairwellandroid.StatementObject.DELETE;
import static io.github.budgetninja.fairwellandroid.StatementObject.REJECT;
import static io.github.budgetninja.fairwellandroid.Utility.getDPI;

/**
 *Created by Tim on 11/09/15.
 */
public class StatementSummaryFragment extends Fragment{

    private TextView descriptionView, categoryView, dateView, deadlineView, totalAmountView, modeView, sumbitByView, noteView;
    private ImageView pictureView;
    private TextView payeeField, amountField;
    private LinearLayout paymentOptionLayout;
    private Button confirmButton, rejectButton, deleteButton, confirmPaymentButton, denyPaymentButton;
    private TableLayout layout;
    private DateFormat dateFormat;
    private Statement data;
    private ParseUser user;
    private ContentActivity parent;
    private Boolean[] tempResult;
    private View previousState;
    private LruCache<String, Bitmap> mMemoryCache;
    private DiskLruCache mDiskLruCache;

    private int DPI;
    private int PIXEL_PHOTO;
    private boolean isImageFitToScreen;

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem item = menu.findItem(R.id.action_refresh);
        item.setVisible(false);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onCreate(Bundle bundle){
        super.onCreate(bundle);
        setHasOptionsMenu(true);
        parent = (ContentActivity)getActivity();
        dateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
        user = ParseUser.getCurrentUser();
        previousState = null;
        isImageFitToScreen = false;
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
        DPI = getDPI(parent.getApplicationContext());
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
        if (previousState != null) {
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
        payeeField = (TextView) view.findViewById(R.id.summary_payee_subtitle);
        amountField = (TextView) view.findViewById(R.id.summary_amount_subtitle);
        layout = (TableLayout) view.findViewById(R.id.summary_tableLayout);
        paymentOptionLayout = (LinearLayout) view.findViewById(R.id.resolve_Option_layout);
        confirmPaymentButton = (Button) view.findViewById(R.id.confirmPendingPaymentButton);
        denyPaymentButton = (Button) view.findViewById(R.id.denyPendingPaymentButton);
        confirmButton = (Button) view.findViewById(R.id.summary_confirmButton);
        rejectButton = (Button) view.findViewById(R.id.summary_rejectButton);
        deleteButton = (Button) view.findViewById(R.id.summary_deleteButton);

        Button cancelButton = (Button) view.findViewById(R.id.summary_cancelButton);
        cancelButton.setVisibility(View.GONE);
        Button modifyButton = (Button) view.findViewById(R.id.summary_modifyButton);
        modifyButton.setVisibility(View.GONE);
        Button submitButton = (Button) view.findViewById(R.id.summary_submitButton);
        submitButton.setVisibility(View.GONE);

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

    public void setData(Statement data){
        this.data = data;
        displayData();
    }

    private void displayData(){
        if(data.picture != null) {
            showProgressBar();
            pictureView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    LinearLayout.LayoutParams params;
                    if (isImageFitToScreen) {
                        isImageFitToScreen = false;
                        if(data.note != null){
                            if(!data.note.isEmpty()){ ((LinearLayout)noteView.getParent()).setVisibility(View.VISIBLE); }
                        }
                        params = new LinearLayout.LayoutParams(Utility.getPixel(100, getResources()), Utility.getPixel(100, getResources()));
                        params.gravity = Gravity.CENTER_HORIZONTAL;
                        pictureView.setLayoutParams(params);
                        pictureView.setAdjustViewBounds(true);
                    } else {
                        isImageFitToScreen = true;
                        ((LinearLayout)noteView.getParent()).setVisibility(View.GONE);
                        params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                        params.gravity = Gravity.CENTER_HORIZONTAL;
                        pictureView.setLayoutParams(params);
                        pictureView.setScaleType(ImageView.ScaleType.FIT_XY);
                    }
                }
            });
            loadParseFiletoImageView(data.picture, pictureView, data.picture.getName().substring(0, 48));
        } else {
            ((LinearLayout)pictureView.getParent()).setVisibility(View.GONE);
        }

        if(data.note != null){
            if(data.note.isEmpty()){ ((LinearLayout)noteView.getParent()).setVisibility(View.GONE); }
            else{ noteView.setText(data.note); }
        } else {
            ((LinearLayout)noteView.getParent()).setVisibility(View.GONE);
        }

        descriptionView.setText(data.description);
        categoryView.setText(data.category);
        dateView.setText(dateFormat.format(data.date));
        deadlineView.setText(dateFormat.format(data.deadline));
        totalAmountView.setText("$ " + String.format("%.2f", data.totalAmount));
        switch (data.mode){
            case SPLIT_EQUALLY:
                modeView.setText("Split Equally");
                break;
            case SPLIT_UNEQUALLY:
                modeView.setText("Split Unequally");
                break;
            case SPLIT_BY_RATIO:
                modeView.setText("Split by Ratio");
                break;
        }
        sumbitByView.setText(data.submitBy);

        if(data.isPayee){
            displayDataPayee();
        } else {
            displayDataPayer();
        }
    }

    private void displayDataPayer(){
        final SubStatement subStatement = data.findPayerStatement(user);
        TableRow memberRow = new TableRow(parent);
        memberRow.setPadding(0, 0, 0, Utility.getPixel(2, getResources()));

        TextView payee = new TextView(parent);
        payee.setGravity(Gravity.CENTER);
        payee.setText(subStatement.payerName);

        TextView payer = new TextView(parent);
        payer.setGravity(Gravity.CENTER);
        payer.setText("YOU");

        TextView amount = new TextView(parent);
        amount.setGravity(Gravity.CENTER);
        amount.setText("$ " + String.format("%.2f", subStatement.payerAmount));

        memberRow.addView(payer, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        memberRow.addView(payee, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        memberRow.addView(amount, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layout.addView(memberRow);
        deleteButton.setVisibility(View.GONE);
        paymentOptionLayout.setVisibility(View.GONE);

        if(subStatement.payerConfirm){
            confirmButton.setVisibility(View.GONE);
            rejectButton.setVisibility(View.GONE);
        } else {
            confirmButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    subStatement.setPayerConfirm(parent);
                    confirmButton.setVisibility(View.GONE);
                    rejectButton.setVisibility(View.GONE);
                }
            });

            rejectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    subStatement.setPayerReject(parent);
                }
            });
        }
    }

    private void displayDataPayee(){
        payeeField.setText("Amount");
        amountField.setText("Status");
        TableRow memberRow;
        TextView payer, amount, status;
        boolean deletable = true;
        boolean pendingPayment = false;

        for(int i = 0; i < data.payerList.size(); i++){
            SubStatement item = data.payerList.get(i);

            memberRow = new TableRow(parent);
            memberRow.setPadding(0, 0, 0, Utility.getPixel(2, getResources()));

            payer = new TextView(parent);
            payer.setGravity(Gravity.CENTER);
            payer.setText(item.payerName);

            amount = new TextView(parent);
            amount.setGravity(Gravity.CENTER);
            amount.setText("$ " + String.format("%.2f", item.payerAmount));

            status = new TextView(parent);
            status.setGravity(Gravity.CENTER);
            if(item.payerConfirm){
                if(item.payerPaid){
                    status.setText("Paid");
                } else if(item.paymentPending){
                    status.setText("Resolving");
                    deletable = false;
                    pendingPayment = true;
                } else {
                    status.setText("Confirmed");
                    deletable = false;
                }
            } else if(item.payerReject){
                status.setText("Denied");
            } else {
                status.setText("Pending");
                deletable = false;
            }

            memberRow.addView(payer, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            memberRow.addView(amount, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            memberRow.addView(status, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layout.addView(memberRow);
        }

        if(data.unknown != 0){
            memberRow = new TableRow(parent);
            memberRow.setPadding(0, 0, 0, Utility.getPixel(2, getResources()));

            payer = new TextView(parent);
            payer.setGravity(Gravity.CENTER);
            if(data.unknown == 1){
                payer.setText("(1 non-user)");
            } else if(data.unknown > 1) {
                payer.setText("(" + Integer.toString(data.unknown) + " non-users)");
            } else {
                payer.setText("(Some non-users)");
            }

            amount = new TextView(parent);
            amount.setGravity(Gravity.CENTER);
            amount.setText("$ " + String.format("%.2f", data.unknownAmount));

            status = new TextView(parent);
            status.setGravity(Gravity.CENTER);
            status.setText("N/A");

            memberRow.addView(payer, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            memberRow.addView(amount, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            memberRow.addView(status, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layout.addView(memberRow);
        }

        if(data.payeeConfirm){
            confirmButton.setVisibility(View.GONE);
            rejectButton.setVisibility(View.GONE);
            if(!deletable){
                deleteButton.setVisibility(View.GONE);
            } else {
                deleteButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Statement.PayeeStatementProcess task = data.new PayeeStatementProcess(parent, DELETE);
                        task.execute();
                    }
                });
            }
            if(!pendingPayment){
                paymentOptionLayout.setVisibility(View.GONE);
            } else {
                confirmPaymentButton.setOnClickListener(new paymentOption(CONFIRM));
                denyPaymentButton.setOnClickListener(new paymentOption(REJECT));
            }
        } else {
            deleteButton.setVisibility(View.GONE);
            paymentOptionLayout.setVisibility(View.GONE);
            confirmButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    data.setPayeeConfirm(parent);
                    confirmButton.setVisibility(View.GONE);
                    rejectButton.setVisibility(View.GONE);
                }
            });

            rejectButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Statement.PayeeStatementProcess task = data.new PayeeStatementProcess(parent, REJECT);
                    task.execute();
                }
            });
        }
    }

    private class paymentOption implements View.OnClickListener{

        private int type;

        public paymentOption(int type){
            this.type = type;
        }

        @Override
        public void onClick(View v) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(parent);
            final ListView container = new ListView(parent);
            final List<SubStatement> list = new ArrayList<>();
            for(int i = 0; i < data.payerList.size(); i++){
                if(data.payerList.get(i).paymentPending){
                    list.add(data.payerList.get(i));
                }
            }
            tempResult = new Boolean[list.size()];

            PayerSelectionAdaptor adaptor = new PayerSelectionAdaptor(parent, R.layout.item_add_member_one, list);
            container.setAdapter(adaptor);
            container.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    CheckBox checkBox = (CheckBox) view.findViewById(R.id.memberCheckBox);
                    if (checkBox.isChecked()) {
                        checkBox.setChecked(false);
                        tempResult[position] = false;
                    } else {
                        checkBox.setChecked(true);
                        tempResult[position] = true;
                    }
                }
            });
            builder.setView(container);
            if(type == CONFIRM){
                builder.setTitle("Confirm Pending Payment");
                builder.setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PendingPaymentProcess task = new PendingPaymentProcess(parent, tempResult, list, CONFIRM);
                        task.execute();
                    }
                });
            } else {
                builder.setTitle("Deny Pending Payment");
                builder.setPositiveButton("Deny", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PendingPaymentProcess task = new PendingPaymentProcess(parent, tempResult, list, REJECT);
                        task.execute();
                    }
                });
            }
            builder.setNegativeButton("Cancel", null);
            final AlertDialog dialog = builder.create();
            dialog.show();
        }
    }

    private class PayerSelectionAdaptor extends ArrayAdapter<SubStatement> {

        Context mContext;
        int mResource;
        List<SubStatement> mObject;

        public PayerSelectionAdaptor(Context context, int resource, List<SubStatement> objects){
            super(context, resource, objects);
            mContext = context;
            mResource = resource;
            mObject = objects;
        }

        private class ViewHolder{
            TextView nameText;
            CheckBox box;
            int position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parentGroup){
            SubStatement currentItem = mObject.get(position);
            final ViewHolder viewHolder;
            if(convertView == null){
                convertView = parent.getLayoutInflater().inflate(mResource, parentGroup, false);
                viewHolder = new ViewHolder();
                viewHolder.nameText = (TextView) convertView.findViewById(R.id.memberName);
                viewHolder.box = (CheckBox) convertView.findViewById(R.id.memberCheckBox);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            viewHolder.position = position;
            viewHolder.nameText.setText(currentItem.payerName);
            if(tempResult[position] == null){
                viewHolder.box.setChecked(false);
            } else {
                viewHolder.box.setChecked(tempResult[position]);
            }

            return convertView;
        }
    }

    private class PendingPaymentProcess extends AsyncTask<Boolean, Void, Boolean> {
        private ProgressDialog dialog;
        private Context activity;
        private Boolean[] result;
        private List<SubStatement> list;
        private int type;

        public PendingPaymentProcess(Context activity, Boolean[] result, List<SubStatement> list, int type) {
            dialog = new ProgressDialog(activity);
            this.activity = activity;
            this.result = result;
            this.list = list;
            this.type = type;
        }

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Processing... Please Wait...");
            dialog.setCancelable(false);
            dialog.show();
        }

        @Override
        protected Boolean doInBackground(Boolean... params) {
            try {
                for(int i = 0; i < list.size(); i++){
                    if(result[i] != null){
                        if(result[i] && type == CONFIRM) {
                            list.get(i).setPaymentApproved();
                        } else if(result[i] && type == REJECT){
                            list.get(i).setPaymentDenied();
                        }
                    }
                }
                return true;
            } catch (ParseException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            if(result){
                Toast.makeText(activity, "Statement processed", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(activity, "Failed to complete, Please retry", Toast.LENGTH_SHORT).show();
            }
            ((ContentActivity)activity).fragMgr.popBackStack();
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
                    if(e == null) {
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
                if(bitmap != null) {
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
            if(bitmapData.length == 0 || bitmapData != data) {
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
                    if(editor != null) {
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
