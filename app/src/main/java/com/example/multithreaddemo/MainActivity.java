package com.example.multithreaddemo;

import android.app.Activity;
import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final static int START_NUM = 1;
    private final static int ADDING_NUM = 2;
    private final static int ENDING_NUM = 3;
    private final static int CANCEL_NUM = 4;
    private static final String DOWNLOAD_URL ="https://desk-fd.zol-img.com.cn/t_s1920x1080c5/g5/M00/07/07/ChMkJlXw8QmIO6kEABYKy-RYbJ4AACddwM0pT0AFgrj303.jpg";



    private Button btn_start_handler, btn_other, btn_start_async, btn_download_handler, btn_download_async;
    TextView view;
    ImageView imageView;
    ProgressBar progressBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        view = findViewById(R.id.view);
        imageView = findViewById(R.id.img);
        btn_start_handler = findViewById(R.id.btn_start_handler);
        btn_other = findViewById(R.id.btn_other);
        btn_start_async = findViewById(R.id.btn_start_async);
        btn_download_handler = findViewById(R.id.btn_download_handler);
        btn_download_async = findViewById(R.id.btn_download_async);
        progressBar = findViewById(R.id.progress);

        btn_start_handler.setOnClickListener(this);
        btn_other.setOnClickListener(this);
        btn_start_async.setOnClickListener(this);
        btn_download_handler.setOnClickListener(this);
        btn_download_async.setOnClickListener(this);
    }


    private MyHandler myHandler = new MyHandler(this);
    private CalculateThread calculateThread;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_start_handler:
                calculateThread = new CalculateThread();
                calculateThread.start();
                break;

            case R.id.btn_start_async:
                new MyAsyncTask(this).execute(100);
                break;
            case R.id.btn_download_handler:
                new Thread(new DownloadImageFetcher(DOWNLOAD_URL)).start();
                break;
            case R.id.btn_download_async:
                new DownloadImage(this).execute(DOWNLOAD_URL);
                break;
            case R.id.btn_other:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        btn_other.setText("runOnUiThread方式更新");
                        view.setText("runOnUiThread方式更新TextView的内容");
                    }
                });
                break;
        }
    }

    //自定义Handler类
    static class MyHandler extends Handler {
        //定义弱引用对象
        private WeakReference<Activity> ref;

        //在构造方法中创建此对象
        public MyHandler(Activity activity) {
            this.ref = new WeakReference<>((activity));
        }

        //重写handler方法
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            // 1.获取弱引用指向的Activity对象
            MainActivity activity = (MainActivity) ref.get();
            if (activity == null) {
                return;
            }
            // 2.根据Message的what属性值处理消息
            switch (msg.what) {
                case START_NUM:
                    activity.progressBar.setVisibility(View.VISIBLE);
                    break;
                case ADDING_NUM:
                    activity.progressBar.setProgress(msg.arg1);
                    activity.view.setText("计算已完成" + msg.arg1 + "%");
                    break;
                case ENDING_NUM:
                    activity.progressBar.setVisibility(View.GONE);
                    activity.view.setText("计算已完成，结果为：" + msg.arg1);
                    activity.myHandler.removeCallbacks(activity.calculateThread);
                    break;
                case CANCEL_NUM:
                    activity.progressBar.setProgress(0);
                    activity.progressBar.setVisibility(View.GONE);
                    activity.view.setText("计算已取消");
                    break;

            }

        }

    }

    //            计算的子线程
    class CalculateThread extends Thread {
        @Override
        public void run() {
            int result = 0;// 存放结果的变量
            boolean isCancel = false;
            // 1.刚开始发送一个空消息
            myHandler.sendEmptyMessage(START_NUM);
            // 2.计算过程，要求:每隔100ms算- 次
            for (int i = 0; i <= 100; i++) {
                try {
                    Thread.sleep(100);
                    result += i;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    isCancel = true;
                    break;
                }
                // 2.发送进度条更新的消息
                if (i % 5 == 0) {
                    //获取消息对象
                    Message msg = Message.obtain();
                    //给消息的参数赋值
                    msg.what = ADDING_NUM;
                    msg.arg1 = i;
                    //发送消息
                    myHandler.sendMessage(msg);

                }

            }
            if (!isCancel) {
                Message msg = myHandler.obtainMessage();
                msg.what = ENDING_NUM;
                msg.arg1 = result;
                myHandler.sendMessage(msg);

            }
        }
    }
    private static final int MSG_SHOW_PROGRESS = 11;
    private static final int MSG_SHOW_IMAGE = 12;

    //自定义Handler类
    static class MyUIHandler extends Handler {
        //定义弱引用对象
        private WeakReference<Activity> ref;

        //在构造方法中创建此对象
        public MyUIHandler(Activity activity) {
            this.ref = new WeakReference<>((activity));
        }

        //重写handler方法
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            // 1.获取弱引用指向的Activity对象
            MainActivity activity = (MainActivity) ref.get();
            if (activity == null) {
                return;
            }
            // 2.根据Message的what属性值处理消息
            switch (msg.what) {
                case MSG_SHOW_PROGRESS:
                    //显示进度条
                    activity.progressBar.setVisibility(View.VISIBLE);
                    break;
                case MSG_SHOW_IMAGE:
                    //显示下载图片
                    activity.progressBar.setVisibility(View.GONE);
                    activity.imageView.setImageBitmap((Bitmap) msg.obj); // 给ImageView设置图片
                    break;

            }

        }
    }

        private MyUIHandler uiHandler = new MyUIHandler(this);

//    下载图片的线程
        private class DownloadImageFetcher implements Runnable{
            private String imgUrl;
            public DownloadImageFetcher(String strUrl){
                this.imgUrl = strUrl;
            }
            @Override
            public void run() {
                InputStream in = null;
                //发一个空消息到handleMessage ()去处理，显示进度条
                uiHandler.obtainMessage(MSG_SHOW_PROGRESS).sendToTarget();
                // 1.将url字符串转为URL对象
                try {
                    URL url = new URL(imgUrl);
                    // 2.打开ur7对象的http连接
                    HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                     // 3.获取这个连接的输入流
                    in = connection.getInputStream();
                    // 4。将输入流解码为Bitmap图片
                    Bitmap bitmap = BitmapFactory.decodeStream(in);
                    // 5.通过handler发送消息
//                    uiHandler.obtainMessage(MSG_SHOW_PROGRESS,bitmap).sendToTarget();
                    Message msg = uiHandler.obtainMessage();
                    msg.what = MSG_SHOW_IMAGE;
                    msg.obj = bitmap;
                    uiHandler.sendMessage(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }finally {
                    if(in != null){
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

    //自定义Handler类
    static class MyAsyncTask extends AsyncTask<Integer, Integer, Integer> {
        //定义弱引用对象
        private WeakReference<AppCompatActivity> ref;

        //在构造方法中创建此对象
        public MyAsyncTask(AppCompatActivity activity) {
            this.ref = new WeakReference<>((activity));
        }
        // 执行线程任务前的操作
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            MainActivity activity = (MainActivity) this.ref.get();
            activity.progressBar.setVisibility(View.VISIBLE);
        }

        // 接收输入参数、执行任务中的耗时操作、返回线程任务执行的结果
        @Override
        protected Integer doInBackground(Integer... integers) {
            int sleep = integers[0];
            int result = 0;

            for(int i=0;i<101;i++){
                try {
                    Thread.sleep(sleep);
                    result += i;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(i%5==0){
//                报告进度
                    publishProgress(i);
                }
//            是否外界提出了取消的请求
                if(isCancelled()){
                    break;
                }
            }
//        返回处理结果
            return result;

        }

        // 在主线程中显示线程任务执行的进度
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            MainActivity activity = (MainActivity) this.ref.get();
            activity.progressBar.setProgress(values[0]);
            activity.view.setText("计算已完成" + values[0] + "%");
        }

        // 接收线程任务执行结果、将执行结果显示到UI组件
        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            MainActivity activity = (MainActivity) this.ref.get();
            activity.view.setText("已计算完成，结果为：" + result);
            activity.progressBar.setVisibility(View.GONE);
        }

        // 将异步任务设置为：取消状态
        @Override
        protected void onCancelled() {
            super.onCancelled();

            MainActivity activity = (MainActivity) this.ref.get();
            activity.view.setText("计算已取消");

            activity.progressBar.setProgress(0);
            activity.progressBar.setVisibility(View.GONE);
        }
    }

    static class DownloadImage extends AsyncTask<String, Bitmap, Bitmap>{
        //定义弱引用对象
        private WeakReference<AppCompatActivity> ref;

        //在构造方法中创建此对象
        public DownloadImage(AppCompatActivity activity) {
            this.ref = new WeakReference<>((activity));
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            MainActivity activity = (MainActivity) this.ref.get();
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            String url = strings[0];
            return downloadImage(url);
        }

        private Bitmap downloadImage(String strUrl) {
            InputStream stream = null;
            Bitmap bitmap = null;

            MainActivity activity = (MainActivity) this.ref.get();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            try {
                URL url = new URL(strUrl);
                HttpURLConnection connection =(HttpURLConnection) url.openConnection();
                int totalLen = connection.getContentLength();
                if(totalLen==0){
                    activity.progressBar.setProgress(0);
                }

                if(connection.getResponseCode() == 200){
                    stream = connection.getInputStream();
                }

                int len = -1;
                int progress = 0;
                byte[] tmps = new byte[1024];
                while((len = stream.read(tmps)) != -1){
                    progress += len;
                    activity.progressBar.setProgress(progress);
                    bos.write(tmps,0,len);
                }
                bitmap = BitmapFactory.decodeByteArray(bos.toByteArray(),0,bos.size());

            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                if(stream != null){
                    try {
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

            MainActivity activity = (MainActivity) this.ref.get();
            if (bitmap != null) {
                activity.imageView.setImageBitmap(bitmap);
            }
        }

    }
    }

