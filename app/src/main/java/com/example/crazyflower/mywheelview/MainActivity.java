package com.example.crazyflower.mywheelview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MyWheelView myWheelView = findViewById(R.id.test_wheelview);

        List<String> data = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            data.add(i + "");
        }
        data.add("123453212");
        data.add("123jhfsagdfsdafds");
        for (int i = 0; i < 10; i++) {
            data.add(i + "");
        }
        myWheelView.setDataWithSelectedItemIndex(data, 0);

        textView = findViewById(R.id.selected_text);

        myWheelView.setWheelViewSelectedListener(new IWheelViewSelectedListener() {
            @Override
            public void wheelViewSelectedChanged(MyWheelView myWheelView, List<String> data, int position) {
                Log.d(TAG, "wheelViewSelectedChanged: " + data.get(position));
                textView.setText(data.get(position));
            }
        });
    }
}
