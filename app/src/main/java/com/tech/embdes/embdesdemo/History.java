package com.tech.embdes.embdesdemo;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;

import com.tech.embdes.embdesdemo.data.Analysis;
import com.tech.embdes.embdesdemo.data.DbContract;
import com.tech.embdes.embdesdemo.data.Device;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import static android.content.ContentValues.TAG;

public class History extends BaseActivity {
    static final int DATE_DIALOG_ID = 0;
    static final int FROM_DATE_DIALOG_ID = 2;
    TextView from, to;
    ImageButton calender1, calender2;
    Button ok;
    DatePickerDialog toDate;
    TextView titlebar, date;
    String mname, mDeviceAddress;
    RecyclerView recyclerView;
    MyRecyclerViewAdapter myRecyclerViewAdapter;
    String TABLE_CONTACTS = "HEART_RATE";
    boolean isShow = false;
    private int mYear, mMonth, mDay, mHour, mMinute;
    private Calendar toDateCalender = Calendar.getInstance();
    private Calendar fromDateCalender = Calendar.getInstance();
    SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    // Register  TimePickerDialog listener
    private TimePickerDialog.OnTimeSetListener fromTimeSetListener =
            new TimePickerDialog.OnTimeSetListener() {
                // the callback received when the user "sets" the TimePickerDialog in the dialog
                public void onTimeSet(TimePicker view, int hourOfDay, int min) {
                    fromDateCalender.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    fromDateCalender.set(Calendar.MINUTE, min);
                    TextView fromDate = (TextView) findViewById(R.id.fromDateView);
                    fromDate.setText(dateTimeFormat.format(new Date(fromDateCalender.getTimeInMillis())));
                }
            };
    // Register  fromDatePickerDialog listener
    private DatePickerDialog.OnDateSetListener fromDateSetListener =
            new DatePickerDialog.OnDateSetListener() {                 // the callback received when the user "sets" the Date in the DatePickerDialog
                public void onDateSet(DatePicker view, int yearSelected,
                                      int monthOfYear, int dayOfMonth) {
                    if (isShow == true) {
                        fromDateCalender.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        fromDateCalender.set(Calendar.MONTH, monthOfYear);
                        fromDateCalender.set(Calendar.YEAR, yearSelected);

                        isShow = false;
                        TextView fromDate = (TextView) findViewById(R.id.fromDateView);
                        fromTime();
                    }
                }
            };
    // Register  TimePickerDialog listener
    private TimePickerDialog.OnTimeSetListener toTimeSetListener =
            new TimePickerDialog.OnTimeSetListener() {
                // the callback received when the user "sets" the TimePickerDialog in the dialog
                public void onTimeSet(TimePicker view, int hourOfDay, int min) {
                    toDateCalender.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    toDateCalender.set(Calendar.MINUTE, min);
                    TextView toDateTime = (TextView) findViewById(R.id.toDateView);
                    toDateTime.setText(dateTimeFormat.format(new Date(toDateCalender.getTimeInMillis())));
                }
            };
    // Register  toDatePickerDialog listener
    private DatePickerDialog.OnDateSetListener toDateSetListener =
            new DatePickerDialog.OnDateSetListener() {                 // the callback received when the user "sets" the Date in the DatePickerDialog
                public void onDateSet(DatePicker view, int yearSelected,
                                      int monthOfYear, int dayOfMonth) {
                    if (isShow == true) {
                        toDateCalender.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        toDateCalender.set(Calendar.MONTH, monthOfYear);
                        toDateCalender.set(Calendar.YEAR, yearSelected);

                        isShow = false;
                        toTime();
                    }
                }
            };
    private Device mDevice;

    @Override
    public void onAnalysisAvailable() {
        myRecyclerViewAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_data);

        from = findViewById(R.id.from);
        to = findViewById(R.id.to);
        calender1 = findViewById(R.id.calender);
        calender2 = findViewById(R.id.calender1);
        recyclerView = findViewById(R.id.recycler);
        ok = findViewById(R.id.done);
        myRecyclerViewAdapter = new MyRecyclerViewAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(myRecyclerViewAdapter);
        final TextView toDate = findViewById(R.id.toDateView);
        toDate.setText(dateTimeFormat.format(new Date(toDateCalender.getTimeInMillis())));
        final TextView fromDate = findViewById(R.id.fromDateView);
        fromDate.setText(dateTimeFormat.format(new Date(fromDateCalender.getTimeInMillis())));
        Bundle bundle = getIntent().getExtras();
        mDevice = (Device)bundle.getSerializable(DbContract.DEVICES);
        if (mDevice != null) {
            mname = mDevice.getName();
            mDeviceAddress = mDevice.getAddress();
            getActionBar().setTitle(mname);
        }
        calender1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Calendar c = Calendar.getInstance();
                isShow = true;
                mYear = c.get(Calendar.YEAR);
                mMonth = c.get(Calendar.MONTH);
                mDay = c.get(Calendar.DAY_OF_MONTH);
                mHour = c.get(Calendar.HOUR_OF_DAY);
                mMinute = c.get(Calendar.MINUTE);
                showDialog(FROM_DATE_DIALOG_ID);
            }
        });
        calender2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Calendar c = Calendar.getInstance();
                isShow = true;
                mYear = c.get(Calendar.YEAR);
                mMonth = c.get(Calendar.MONTH);
                mDay = c.get(Calendar.DAY_OF_MONTH);
                mHour = c.get(Calendar.HOUR_OF_DAY);
                mMinute = c.get(Calendar.MINUTE);
                showDialog(DATE_DIALOG_ID);
            }
        });
        ok.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.KITKAT)
            @Override
            public void onClick(View view) {
                if (fromDateCalender.getTimeInMillis() < toDateCalender.getTimeInMillis()) {
                    readAnalysis(fromDateCalender.getTimeInMillis(), toDateCalender.getTimeInMillis(), mDevice != null ? mDevice.getId() : 0);
                    Log.d(TAG, "from date:" + fromDateCalender.getTimeInMillis() + " todate:" + toDateCalender.getTimeInMillis());
                } else {
                    AlertDialog.Builder build = new AlertDialog.Builder(History.this);
                    build.setTitle("Error");
                    build.setMessage("Invalid Time");
                    build.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                        }
                    });
                    AlertDialog a = build.create();
                    a.show();
                }

            }
        });

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        Intent intent = new Intent(History.this, DeviceScanAcitivity2.class);
        startActivity(intent);
    }

    protected Dialog onCreateDialog(int id) {
        switch (id) {

            case FROM_DATE_DIALOG_ID:
                final DatePickerDialog fromDate = new DatePickerDialog(this,
                        fromDateSetListener,
                        mYear, mMonth, mDay);
                fromDate.getDatePicker().setDescendantFocusability(DatePicker.FOCUS_BLOCK_DESCENDANTS);
                fromDate.setCancelable(true);
                fromDate.setCanceledOnTouchOutside(true);
                fromDate.getDatePicker().setMaxDate(System.currentTimeMillis());
                fromDate.setButton(DialogInterface.BUTTON_NEGATIVE,
                        "Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                isShow = false; //Cancel flag, used in mTimeSetListener
                            }
                        });
                return fromDate;

            case DATE_DIALOG_ID:
                toDate = new DatePickerDialog(this,
                        toDateSetListener,
                        mYear, mMonth, mDay);
                toDate.getDatePicker().setDescendantFocusability(DatePicker.FOCUS_BLOCK_DESCENDANTS);
                toDate.getDatePicker().setMaxDate(System.currentTimeMillis());
                toDate.setButton(DialogInterface.BUTTON_NEGATIVE,
                        "Cancel",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                isShow = false; //Cancel flag, used in mTimeSetListener
                            }
                        });
                return toDate;
        }
        return null;
    }

    public void toTime() {
        TimePickerDialog toTime = new TimePickerDialog(this,
                toTimeSetListener, mHour, mMinute, false);
        toTime.show();
    }

    public void fromTime() {
        TimePickerDialog fromTime = new TimePickerDialog(this,
                fromTimeSetListener, mHour, mMinute, false);
        fromTime.show();
    }

    public class MyRecyclerViewAdapter extends RecyclerView.Adapter<MyRecyclerViewAdapter.ViewHolder> {
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = getLayoutInflater().inflate(R.layout.recyclerview, parent, false);
            ViewHolder viewHolder = new ViewHolder(view);
            return viewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Analysis analysis = mAnalyses.get(position);
            holder.tv1.setText(analysis.getHeartRate());
            holder.tv2.setText(analysis.getSpo2());
            holder.tv3.setText(analysis.getTemperature());
            holder.tv4.setText(getFormattedTime(analysis.getTime()));
        }

        @Override
        public int getItemCount() {
            return mAnalyses.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            TextView tv1, tv2, tv3, tv4;

            public ViewHolder(View itemView) {
                super(itemView);
                tv1 = (TextView) itemView.findViewById(R.id.heartvalues);
                tv2 = (TextView) itemView.findViewById(R.id.spo2values);
                tv3 = (TextView) itemView.findViewById(R.id.temperaturevalues);
                tv4 = (TextView) itemView.findViewById(R.id.date);
            }
        }
    }
}
