package com.example.babylearning;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

import static android.speech.tts.TextToSpeech.ERROR;

public class EduActivity extends AppCompatActivity{

    private WebView mWebView;
    private WebSettings mWebSettings;
    private TextToSpeech enTTS, koTTS;
    public ValueCallback<Uri[]> filePathCallbackLollipop;
    public ValueCallback<Uri> filePathCallbackNormal;
    public final static int FILECHOOSER_NORMAL_REQ_CODE = 2001;
    public final static int FILECHOOSER_LOLLIPOP_REQ_CODE = 2002;
    private Uri cameraImageUri = null;
    private static final String ENTRY_URL = "http://192.168.1.97/test.html";
    private static MediaPlayer mp;
    private ProgressDialog pd;
    private String str;
    private String[] enArr = new String[105];
    private String[] koArr = new String[105];
    private int num = 0;
    private final Handler handler = new Handler();
    private int playbackPosition = 0;
    private String[] permissions = {
        Manifest.permission.CAMERA,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
        };
    private static final int MULTIPLE_PERMISSIONS = 101;

    public void showProgress(){
        if(pd == null){
            pd = new ProgressDialog(this);
            pd.setCancelable(true);
        }
        pd.setMessage("잠시만 기다려주세요!!");
        pd.setProgressStyle(android.R.style.Widget_ProgressBar_Horizontal);

        pd.show();
    }

    public void hideProgress(){
        if(pd !=null && pd.isShowing()){
            pd.dismiss();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edu);

        //권한획
        if(Build.VERSION.SDK_INT >=23){
            checkPermissions();
        }

        readTxt();

        mp = MediaPlayer.create(this, R.raw.start_2);
        mp.setLooping(true);
        mp.setVolume(0.7f,0.7f);

        mWebView = (WebView) findViewById(R.id.webView);
        mWebSettings = mWebView.getSettings();
        mWebSettings.setJavaScriptEnabled(true); //자바스크립트 허용
        mWebView.loadUrl(ENTRY_URL);
        mWebView.setBackgroundColor(0);
        mWebView.setBackgroundResource(R.drawable.webview_background);
        mWebView.setWebChromeClient(new WebChromeClientClass());
        mWebView.setOnTouchListener(new View.OnTouchListener(){
            public boolean onTouch(View v, MotionEvent event){
                return(event.getAction() == MotionEvent.ACTION_MOVE);
            }
        });
        mWebView.setWebViewClient(new WebViewClientClass());


        enTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener(){
            @Override
            public void onInit(int i) {
                if(i != ERROR){
                    enTTS.setLanguage(Locale.US);
                    enTTS.setSpeechRate(0.5f);
                }
            }
        });

        koTTS = new TextToSpeech(this, new TextToSpeech.OnInitListener(){
            @Override
            public void onInit(int i) {
                if(i != ERROR){
                    koTTS.setLanguage(Locale.KOREAN);
                    koTTS.setSpeechRate(0.5f);
                }
            }
        });

        //웹뷰를 통한 기능 구현
        mWebView.addJavascriptInterface(new Object(){

            @JavascriptInterface
            public void sendPhoto(){
                showProgress();
            }

            //사진 속 물체 선택 시 단어 보여주기
            @JavascriptInterface
            public void getClassNum(String keyword){

                num = Integer.parseInt(keyword);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        String script = "javascript:function test(){"
                                +"document.getElementById('en').value = '"+enArr[num]+"';"
                                +"document.getElementById('kor').value = '"+koArr[num]+"';"
                                +"};"
                                +"test();";
                        mWebView.loadUrl(script);
                    }
                });
            }

            //tts로 단어 읽어주는 기능 구현
            @JavascriptInterface
            public void readEnglish(){

                playbackPosition = mp.getCurrentPosition();
                mp.pause();
                enTTS.speak(enArr[num], TextToSpeech.QUEUE_FLUSH, null);

                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mp.seekTo(playbackPosition);
                        mp.start();
                    }
                },1000);
            }

            @JavascriptInterface
            public void readKorean(){

                playbackPosition = mp.getCurrentPosition();
                mp.pause();
                koTTS.speak(koArr[num], TextToSpeech.QUEUE_FLUSH, null);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        mp.seekTo(playbackPosition);
                        mp.start();
                    }
                },1000);
            }
        },"Learning");

    }

    private class WebChromeClientClass extends WebChromeClient{
        // 자바스크립트의 alert창
        @Override
        public boolean onJsAlert(WebView view, String url, String message, final JsResult result) {
            new AlertDialog.Builder(view.getContext())
                    .setTitle("Alert")
                    .setMessage(message)
                    .setPositiveButton(android.R.string.ok,
                            new AlertDialog.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    result.confirm();
                                }
                            })
                    .setCancelable(false)
                    .create()
                    .show();
            return true;
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message,
                                   final JsResult result) {
            new AlertDialog.Builder(view.getContext())
                    .setTitle("Confirm")
                    .setMessage(message)
                    .setPositiveButton("Yes",
                            new AlertDialog.OnClickListener(){
                                public void onClick(DialogInterface dialog, int which) {
                                    result.confirm();
                                }
                            })
                    .setNegativeButton("No",
                            new AlertDialog.OnClickListener(){
                                public void onClick(DialogInterface dialog, int which) {
                                    result.cancel();
                                }
                            })
                    .setCancelable(false)
                    .create()
                    .show();
            return true;
        }

        // For Android 5.0+ 카메라 - input type="file" 태그를 선택했을 때 반응
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public boolean onShowFileChooser(
                WebView webView, ValueCallback<Uri[]> filePathCallback,
                FileChooserParams fileChooserParams) {
            Log.d("MainActivity", "5.0+");

            // Callback 초기화 (중요!)
            if (filePathCallbackLollipop != null) {
                filePathCallbackLollipop.onReceiveValue(null);
                filePathCallbackLollipop = null;
            }
            filePathCallbackLollipop = filePathCallback;

            boolean isCapture = fileChooserParams.isCaptureEnabled();
            runCamera(isCapture);
            return true;
        }
    }

    private class WebViewClientClass extends WebViewClient{
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            mp.start();
            hideProgress();

        }

        @Override
        public void onPageFinished(WebView view, String url) {

            //결과화면 로드 이후 기능 구현
            if (url.equals("http://192.168.1.97/index.html")){
                String english = " ";
                String korean = "사진 클릭!!";
                String script = "javascript:function afterLoad(){"
                        + "document.getElementById('en').value = '" + english + "';"
                        + "document.getElementById('kor').value = '" + korean + "';"
                        + "document.forms[1].setAttribute('onclick', 'window.Learning.readEnglish(); return true;');"
                        + "document.forms[2].setAttribute('onclick', 'window.Learning.readKorean(); return true;');"
                        + "};"
                        + "afterLoad();";
                view.loadUrl(script);
            }
        }
    }

    //물체 리스트 파일에서 문자열 받아오기
    private void readTxt(){
        int i = 0;

        try{
            BufferedReader in = new BufferedReader(new InputStreamReader(this.getResources().openRawResource(R.raw.korean_slash), "utf-8"));
            while((str = in.readLine())!=null){
                StringTokenizer st = new StringTokenizer(str,"/");
                enArr[i] = st.nextToken();
                koArr[i] = st.nextToken();
                i++;
            }
            in.close();

        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    //권한 설정
    private boolean checkPermissions(){
        int result;
        List<String> permissionList = new ArrayList<>();
        for(String pm : permissions){
            result = ContextCompat.checkSelfPermission(this, pm);
            if(result != PackageManager.PERMISSION_GRANTED){
                permissionList.add(pm);
            }
        }
        if(!permissionList.isEmpty()){
            ActivityCompat.requestPermissions(this, permissionList.toArray(new String[permissionList.size()]), MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        Log.e("onRequestPermission", "들어옴");

        if (requestCode == 1)
        {
            if (grantResults.length > 0)
            {
                for (int i=0; i<grantResults.length; ++i)
                {
                    if (grantResults[i] == PackageManager.PERMISSION_DENIED)
                    {
                        // 카메라, 저장소 중 하나라도 거부한다면 앱실행 불가 메세지 띄움
                        new AlertDialog.Builder(this).setTitle("알림").setMessage("권한을 허용해주셔야 앱을 이용할 수 있습니다.")
                                .setPositiveButton("종료", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        finish();
                                    }
                                }).setNegativeButton("권한 설정", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                        .setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                                getApplicationContext().startActivity(intent);
                            }
                        }).setCancelable(false).show();

                        return;
                    }
                }
                Toast.makeText(this, "Succeed Read/Write external storage !", Toast.LENGTH_SHORT).show();
                //startApp();
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode)
        {
            case FILECHOOSER_NORMAL_REQ_CODE:
                if (resultCode == RESULT_OK)
                {
                    if (filePathCallbackNormal == null) return;
                    Uri result = (data == null || resultCode != RESULT_OK) ? null : data.getData();
                    //  onReceiveValue 로 파일을 전송한다.
                    filePathCallbackNormal.onReceiveValue(result);
                    filePathCallbackNormal = null;
                }
                break;
            case FILECHOOSER_LOLLIPOP_REQ_CODE:
                if (resultCode == RESULT_OK)
                {
                    if (filePathCallbackLollipop == null) return;
                    if (data == null)
                        data = new Intent();
                    if (data.getData() == null)
                        data.setData(cameraImageUri);

                    filePathCallbackLollipop.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
                    filePathCallbackLollipop = null;
                }
                else
                {
                    if (filePathCallbackLollipop != null)
                    {   //  resultCode에 RESULT_OK가 들어오지 않으면 null 처리하지 한다.(이렇게 하지 않으면 다음부터 input 태그를 클릭해도 반응하지 않음)
                        filePathCallbackLollipop.onReceiveValue(null);
                        filePathCallbackLollipop = null;
                    }

                    if (filePathCallbackNormal != null)
                    {
                        filePathCallbackNormal.onReceiveValue(null);
                        filePathCallbackNormal = null;
                    }
                }
                break;
            default:

                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }


    private void runCamera(boolean _isCapture){

        Intent intentCamera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        File path = getFilesDir();
        File file = new File(path, "edu_image.png");
        // File 객체의 URI 를 얻는다.
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            String strpa = getApplicationContext().getPackageName();
            cameraImageUri = FileProvider.getUriForFile(this, strpa + ".fileprovider", file);
        }
        else{
            cameraImageUri = Uri.fromFile(file);
        }
        intentCamera.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);

        if (!_isCapture){

            // 선택팝업 카메라, 갤러리 둘다 띄우고 싶을 때
            Intent pickIntent = new Intent(Intent.ACTION_PICK);
            pickIntent.setType(MediaStore.Images.Media.CONTENT_TYPE);
            pickIntent.setData(MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

            String pickTitle = "사진 가져올 방법을 선택하세요.";
            Intent chooserIntent = Intent.createChooser(pickIntent, pickTitle);

            // 카메라 intent 포함시키기..
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Parcelable[]{intentCamera});
            startActivityForResult(chooserIntent, FILECHOOSER_LOLLIPOP_REQ_CODE);
        }
        else{
            // 바로 카메라 실행..
            startActivityForResult(intentCamera, FILECHOOSER_LOLLIPOP_REQ_CODE);
        }
    }

}
