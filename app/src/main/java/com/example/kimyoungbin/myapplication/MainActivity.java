package com.example.kimyoungbin.myapplication;

import android.content.Intent;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class MainActivity extends AppCompatActivity {

    EditText name, age, height;
    String sex;
    Button submit;

    final static String foldername = Environment.getExternalStorageDirectory().getAbsolutePath()+"/TestLog";
    final static String filename = "logfile.txt";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        name = findViewById(R.id.name);
        age = findViewById(R.id.age);
        height = findViewById(R.id.height);
        submit = findViewById(R.id.submit);

        final RadioGroup rg = (RadioGroup)findViewById(R.id.sex);
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if(checkedId == R.id.male){
                    sex = "male";
                }else if(checkedId == R.id.female){
                    sex = "female";
                }else{
                    sex = "unknown";
                }
            }
        });

        submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnFileWrite(v);
                String data = height.getText().toString();
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                intent.putExtra("height", ""+data);
                startActivity(intent);
                finish();
            }
        });
    }

    public void mOnFileWrite(View v){
        String contents = "-----------------------\nname : "+name.getText().toString()+"\nage : "+age.getText().toString()+"\nheight : "+height.getText().toString()+"\nsex : "+sex+"\n";
        WriteTextFile(foldername, filename, contents);
    }

    public void WriteTextFile(String foldername, String filename, String contents){
        try{
            File dir = new File(foldername);
            if(!dir.exists()){
                dir.mkdir();
            }
            FileOutputStream fos = new FileOutputStream(foldername+"/"+filename, true);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fos));
            writer.write(contents);
            writer.flush();

            writer.close();
            fos.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
}
