package com.tech.embdes.embdesdemo;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.jjoe64.graphview.DefaultLabelFormatter;
import com.jjoe64.graphview.LegendRenderer;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.DataPointInterface;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.OnDataPointTapListener;
import com.jjoe64.graphview.series.Series;
import com.tech.embdes.embdesdemo.data.Analysis;
import com.tech.embdes.embdesdemo.data.DbContract;
import com.tech.embdes.embdesdemo.data.Device;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Objects;

import static android.content.ContentValues.TAG;

public class GraphView extends BaseActivity {
    static final int DATE_DIALOG_ID = 0;
    static final int FROM_DATE_DIALOG_ID = 2;
    Handler mHandler = new Handler();
    Runnable mtimer1, mtimer2, mtimer3;
    LineGraphSeries series, series1, series2;
    double graphxvalue = 2d;
    Calendar calendar;
    boolean flag = false;
    int i = 0;
    TextView toDatetext, fromDate;
    long epoch;
    int j = 0;
    String temp, heart, spo2;
    Date d1;
    com.jjoe64.graphview.GraphView graphView;
    ArrayList<String> servicelist;
    TextView from, to;
    ImageButton calender1, calender2;
    Button ok;
    DatePickerDialog toDate;
    String mdeviceAddress, mDeviceName;
    RecyclerView recyclerView;
    boolean isShow = false;
    PowerManager.WakeLock wakeLock;
    private int mYear, mMonth, mDay, mHour, mMinute;
    private Device mDevice;
    private Calendar toDateCalender = Calendar.getInstance();
    private Calendar fromDateCalender = Calendar.getInstance();
    SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
    // Register  TimePickerDialog listener
    private TimePickerDialog.OnTimeSetListener fromTimeSetListener =
            new TimePickerDialog.OnTimeSetListener() {
                // the callback received when the user "sets" the TimePickerDialog in the dialog
                @SuppressLint({"DefaultLocale", "SetTextI18n"})
                public void onTimeSet(TimePicker view, int hourOfDay, int min) {
                    fromDateCalender.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    fromDateCalender.set(Calendar.MINUTE, min);
                    TextView fromDate = findViewById(R.id.fromDateView);
                    fromDate.setText(dateTimeFormat.format(new Date(fromDateCalender.getTimeInMillis())));
                }
            };
    private DatePickerDialog.OnDateSetListener fromDateSetListener =
            new DatePickerDialog.OnDateSetListener() {                 // the callback received when the user "sets" the Date in the DatePickerDialog
                @SuppressLint({"DefaultLocale", "SetTextI18n"})
                public void onDateSet(DatePicker view, int yearSelected,
                                      int monthOfYear, int dayOfMonth) {
                    if (isShow) {
                        fromDateCalender.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        fromDateCalender.set(Calendar.MONTH, monthOfYear);
                        fromDateCalender.set(Calendar.YEAR, yearSelected);
                        isShow = false;
                        fromTime();
                    }
                }
            };
    // Register  TimePickerDialog listener
    private TimePickerDialog.OnTimeSetListener toTimeSetListener =
            new TimePickerDialog.OnTimeSetListener() {
                // the callback received when the user "sets" the TimePickerDialog in the dialog
                @SuppressLint({"DefaultLocale", "SetTextI18n"})
                public void onTimeSet(TimePicker view, int hourOfDay, int min) {
                    toDateCalender.set(Calendar.HOUR_OF_DAY, hourOfDay);
                    toDateCalender.set(Calendar.MINUTE, min);
                    TextView toDateTime = findViewById(R.id.toDateView);
                    toDateTime.setText(dateTimeFormat.format(new Date(toDateCalender.getTimeInMillis())));
                }
            };
    private DatePickerDialog.OnDateSetListener toDateSetListener =
            new DatePickerDialog.OnDateSetListener() {                 // the callback received when the user "sets" the Date in the DatePickerDialog
                @SuppressLint({"DefaultLocale", "SetTextI18n"})
                public void onDateSet(DatePicker view, int yearSelected,
                                      int monthOfYear, int dayOfMonth) {
                    if (isShow) {
                        toDateCalender.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        toDateCalender.set(Calendar.MONTH, monthOfYear);
                        toDateCalender.set(Calendar.YEAR, yearSelected);
                        isShow = false;
                        toTime();
                    }
                }
            };

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    @Override
    public void onAnalysisAvailable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            /*graphView.getGridLabelRenderer().setLabelFormatter(new DefaultLabelFormatter() {
                @Override
                public String formatLabel(double value, boolean isValueX) {
                    if (isValueX) {
                        Log.e(TAG, "millis:"+((long)value) +":date:"+getFormattedTime((long) value));
                        return getFormattedTime((long) value);
                    }
                    return super.formatLabel(value, false);
                }
            });*/
            series.resetData(getdatapoint());
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_graph);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "MyWakelockTag");
        graphView = findViewById(R.id.graphview);
        servicelist = new ArrayList<>();
        calendar = Calendar.getInstance();
        from = findViewById(R.id.from);
        to = findViewById(R.id.to);
        calender1 = findViewById(R.id.calender);
        calender2 = findViewById(R.id.calender1);
        recyclerView = findViewById(R.id.recycler);
        ok = findViewById(R.id.done);
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        mDevice = (Device) bundle.getSerializable(DbContract.DEVICES);
        if (mDevice != null) {
            mDeviceName = mDevice.getName();
            mdeviceAddress = mDevice.getAddress();
            getActionBar().setTitle(mDeviceName);
        }
        mdeviceAddress = intent.getStringExtra("Device_Address");
        mDeviceName = intent.getStringExtra("Device_Name");
        Log.d("Embdes", "onCreate: " + i);
        series = new LineGraphSeries();
        series1 = new LineGraphSeries();
        series2 = new LineGraphSeries();
        series.setColor(Color.CYAN);
        series.setDrawDataPoints(true);
        series.setDataPointsRadius(4);
        series.setThickness(2);
        series.setTitle("Heart_Rate");
        //
        series1.setColor(Color.BLUE);
        series1.setDrawDataPoints(true);
        series1.setDataPointsRadius(4);
        series1.setThickness(2);
        series1.setTitle("Spo2");
        //
        series2.setColor(Color.GRAY);
        series2.setDrawDataPoints(true);
        series2.setDataPointsRadius(4);
        series2.setThickness(2);
        series2.setTitle("Temperature");
        //
        graphView.getViewport().setYAxisBoundsManual(true);
        graphView.getViewport().setMaxY(150);

        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setMinX(0);
        graphView.getViewport().setMaxX(40);

        graphView.getLegendRenderer().setVisible(true);
        graphView.getLegendRenderer().setAlign(LegendRenderer.LegendAlign.TOP);
        graphView.getViewport().setScrollable(true); // enables horizontal scrolling
        graphView.getViewport().setScrollableY(true); // enables vertical scrolling
        graphView.getViewport().setScalable(true); // enables horizontal zooming and scrolling
        graphView.getViewport().setScalableY(true); // enables vertical zooming and scrolling
        //
        graphView.getGridLabelRenderer().setHumanRounding(false);
        graphView.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(GraphView.this, new SimpleDateFormat("dd.MM.yyyy \n HH:mm:ss")));
        graphView.getGridLabelRenderer().setNumHorizontalLabels(5);
        //
        graphView.addSeries(series);
        graphView.addSeries(series1);
        graphView.addSeries(series2);
        series.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Toast.makeText(GraphView.this, "" + dataPoint, Toast.LENGTH_SHORT).show();
            }
        });
        series1.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Toast.makeText(GraphView.this, "" + dataPoint, Toast.LENGTH_SHORT).show();
            }
        });
        series2.setOnDataPointTapListener(new OnDataPointTapListener() {
            @Override
            public void onTap(Series series, DataPointInterface dataPoint) {
                Toast.makeText(GraphView.this, "" + dataPoint, Toast.LENGTH_SHORT).show();
            }
        });
        toDatetext = findViewById(R.id.toDateView);
        toDatetext.setText(dateTimeFormat.format(new Date(toDateCalender.getTimeInMillis())));
        fromDate = findViewById(R.id.fromDateView);
        fromDate.setText(dateTimeFormat.format(new Date(fromDateCalender.getTimeInMillis())));
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
                } else {
                    Toast.makeText(GraphView.this, "Select a valid date and time..", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (flag) {
            flag = true;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private DataPoint[] getdatapoint() {
        DataPoint[] dp = new DataPoint[mAnalyses.size()];
        for (int l = 0; l < mAnalyses.size(); l++) {
            long date = mAnalyses.get(l).getTime();
            dp[l] = new DataPoint(date, Double.parseDouble(mAnalyses.get(l).getHeartRate()));
        }
        return dp;
    }

    @Override
    protected void onPause() {
        super.onPause();
        //scanLeDevice(false);
        mHandler.removeCallbacks(mtimer1);
        mHandler.removeCallbacks(mtimer2);
        mHandler.removeCallbacks(mtimer3);

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
}

