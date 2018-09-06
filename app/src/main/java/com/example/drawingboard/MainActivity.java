package com.example.drawingboard;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ImageView imageView;
    private Bitmap copyBitmap;
    private Paint paint;
    private Canvas canvas;
    private long lastMillis;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 用来显示我们画的内容
        imageView = (ImageView) findViewById(R.id.iv);
        // 获取原图
        Bitmap srcBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.drawablebg);
        // 获取原图的副本，相当于空白纸
        copyBitmap = Bitmap.createBitmap(srcBitmap.getWidth(), srcBitmap.getHeight(), srcBitmap.getConfig());
        // 创建画笔
        paint = new Paint();
        // 创建画布
        canvas = new Canvas(copyBitmap);
        // 开始作画
        canvas.drawBitmap(srcBitmap, new Matrix(), paint); // 此时copyBitmap就有内容了
        //canvas.drawLine(20f, 30f, 50f, 80f, paint);
        imageView.setImageBitmap(copyBitmap);
        imageView.setOnTouchListener(new View.OnTouchListener() {
            float startX = 0f;
            float startY = 0f;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                // 获取当前时间的类型
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_DOWN:
                        Log.d(TAG, "触摸: ");
                        // 获取开始位置(划线)
                        startX = event.getX();
                        startY = event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        Log.d(TAG, "移动: ");

                        // 获取结束位置
                        float stopX = event.getX();
                        float stopY = event.getY();
                        // 不停的划线
                        canvas.drawLine(startX, startY, stopX, stopY, paint);
                        // 再次显示到iv
                        imageView.setImageBitmap(copyBitmap);
                        // 获取开始位置(划线)
                        startX = event.getX();
                        startY = event.getY();
                        break;
                    case MotionEvent.ACTION_UP:
                        Log.d(TAG, "抬起: ");
                        break;
                }
                return true; // 如果为false，只能执行第一个处理的事件，认为还未处理完
            }
        });
    }

    public void click(View view) {
        switch (view.getId()) {
            case R.id.btn1:
                paint.setColor(Color.RED);
                break;
            case R.id.btn2:
                paint.setStrokeWidth(15);
                break;
            case R.id.btn3:
                applyPermissions();
                save();
                break;
        }
    }

    private void save() {
        // 保存图片是耗时操作
        new Thread() {
            @Override
            public void run() {
                /*
                 * 第一个参数format：保存图片的格式
                 * 第二个参数quality：保存图片的质量
                 * 第三个参数是输出流
                 * 其中命名用了SystemClock.uptimeMillis()是当前手机已开机的时间
                 * */
                // 简单防抖实现
                long millis = SystemClock.uptimeMillis();
                if (millis - lastMillis < 200) { // 200ms,防止快速点击，可以根须需要设置
                    return;
                }
                String fileName = millis + "test.png";
                File file = new File(Environment.getExternalStorageDirectory().getPath(), fileName);
                lastMillis = millis;
                try {
                    FileOutputStream fos = new FileOutputStream(file);
                    copyBitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                // action为Intent.ACTION_MEDIA_SCANNER_SCAN_FILE扫描指定文件
                Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                Uri uri = Uri.fromFile(file);
                intent.setData(uri);
                sendBroadcast(intent);
            }
        }.start();
    }

    private void applyPermissions() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            save();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    save();
                } else {
                    Toast.makeText(this, "权限被拒绝", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }
}
