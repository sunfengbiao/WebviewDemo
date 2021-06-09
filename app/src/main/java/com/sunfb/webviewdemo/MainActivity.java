package com.sunfb.webviewdemo;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import java.util.HashMap;
import java.util.Set;

/**
 * https://juejin.cn/post/6844904153605505032 感谢作者SoarYuan
 */
public class MainActivity extends AppCompatActivity {
    WebView mWebView;
    Button btn_loadJs;

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /**
         * js调Android原生
         */
//        addJavaScriptInterface();
//        shouldOverrideUrlLoading();
//        interceptMethod();
        /**
         * Android原生调js
         */
//        loadJs();
        evaluateJavascript();
    }

    /**
     * 1⃣️ JS调用Android原生 1）对象映射
     * 通过WebView的addJavascriptInterface()进行对象映射
     * 优缺点
     * 优点：使用简单，仅将Android对象和JS对象映射即可；
     * 缺点：
     * 对于Android 4.2以下，有安全漏洞，需要采用拦截prompt()的方式进行漏洞修复；
     * 对于Android 4.2以上，则只需要对被JS调用的函数以 @JavascriptInterface进行注解
     * <p>
     * 作者：SoarYuan
     * 链接：https://juejin.cn/post/6844904153605505032
     * 来源：掘金
     * 著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。
     */
    private void addJavaScriptInterface() {
        mWebView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        //参数1：Java对象名 参数2：Javascript对象名
        //通过addJavascriptInterface() AJavaScriptInterface类对象映射到JS的mjs对象
        mWebView.addJavascriptInterface(new JSKit(), "mjs");
        // 加载JS代码
        mWebView.loadUrl("file:///android_asset/javascript_map.html");
        //1、Alert无法弹出
        //应该是没有设置WebChromeClient,按照以下代码设置：
        mWebView.setWebChromeClient(new WebChromeClient());
    }

    /**
     * 通过WebViewClient的shouldOverrideUrlLoading()方法回调拦截url；
     * 具体原理
     * <p>
     * Android通过WebViewClient 的回调方法shouldOverrideUrlLoading()拦截url,解析该url的协议。如果检测到是预先约定好的协议，就调用相应方法，即JS需要调用Android中的方法。
     * <p>
     * 优缺点
     * 优点：不存在addJavascriptInterface()方式的漏洞；
     * 缺点：JS获取Android方法的返回值复杂。如果JS想要得到Android方法的返回值，只能通过WebView的 loadUrl()去执行JS方法把返回值传递回去，相关的代码如下：
     * <p>
     * 作者：SoarYuan
     * 链接：https://juejin.cn/post/6844904153605505032
     * 来源：掘金
     * 著作权归作者所有。商业转载请联系作者获得授权，非商业转载请注明出处。
     */
    private void shouldOverrideUrlLoading() {
        mWebView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true); // 设置与Js交互的权限
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);// 设置允许JS弹窗

        //步骤1：加载JS代码,格式规定为:file:///android_asset/文件名.html
        mWebView.loadUrl("file:///android_asset/javascript_intercept.html");

        //复写WebViewClient类的shouldOverrideUrlLoading方法
        mWebView.setWebViewClient(new WebViewClient() {

            //该重载方法不建议使用了，7.0系统以上已经摒弃了
            //shouldOverrideUrlLoading(WebView view, String url)此方法，
            //如果要拦截URL，需要做兼容性处理，重写
            //shouldOverrideUrlLoading(WebView view, WebResourceRequest request)方法，
            //获取得到的可正常使用的URL
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return super.shouldOverrideUrlLoading(view, url);
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {

                // 步骤2：根据协议的参数，判断是否是所需要的url
                // 一般根据scheme（协议格式） & authority（协议名）判断（前两个参数）
                //假定传入进来的 url = "js://webview?arg1=111&arg2=222"（同时也是约定好的需要拦截的）
                Uri uri;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    uri = request.getUrl();
                } else {
                    uri = Uri.parse(request.toString());
                }
                // 如果url的协议 = 预先约定的 js 协议,就解析往下解析参数
                if (uri.getScheme().equals("js")) {
                    // 如果 authority = 预先约定协议里的webview，即代表都符合约定的协议
                    // 所以拦截url,下面JS开始调用Android需要的方法
                    if (uri.getAuthority().equals("webview")) {

                        // 步骤3：执行JS所需要调用的逻辑
                        System.out.println("js调用了Android的方法");
                        // 可以在协议上带有参数并传递到Android上
                        HashMap<String, String> params = new HashMap<>();
                        Set<String> collection = uri.getQueryParameterNames();
                        for (String arg : collection) {
                            System.out.println(arg + " = " + uri.getQueryParameter(arg));
                        }
                        String result = "Android回调给JS的数据为useid=123456";
                        view.loadUrl("javascript:returnResult(\"" + result + "\")");

                    }
                    return true;
                }
                return super.shouldOverrideUrlLoading(view, request);
            }
        });
        //1、Alert无法弹出
        //应该是没有设置WebChromeClient,按照以下代码设置：
        mWebView.setWebChromeClient(new WebChromeClient());

    }

    /**
     * 通过WebChromeClient的onJsAlert()、onJsConfirm()、onJsPrompt()方法回调拦截JS对话框alert()、confirm()、prompt()方法，对消息message进行拦截
     * 具体原理
     * Android通过 WebChromeClient的onJsAlert()、onJsConfirm()、onJsPrompt（）方法回调分别拦截JS对话框（即上述三个方法），得到他们的消息内容，然后解析即可。
     * 优缺点
     * <p>
     * 优点：
     * <p>
     * 不存在addJavascriptInterface()方式的漏洞；
     * <p>
     * 能满足大多数情况下的交互场景；
     * <p>
     * result.confirm()方便回调给JS获取的数据；
     * <p>
     * 缺点：适应复杂，需要进行协议的约束
     */

    private void interceptMethod() {
        mWebView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true); // 设置与Js交互的权限
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);// 设置允许JS弹窗

        //步骤1：加载JS代码,格式规定为:file:///android_asset/文件名.html
        mWebView.loadUrl("file:///android_asset/javascript_method.html");

        mWebView.setWebChromeClient(new WebChromeClient() {
            // 拦截输入框(原理同方式shouldOverrideUrlLoading)
            // 参数message:代表promt())的内容（不是url）
            // 参数result:代表输入框的返回值
            @Override
            public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {

                // 步骤2：根据协议的参数，判断是否是所需要的url(原理同方式2)
                // 一般根据scheme（协议格式） & authority（协议名）判断（前两个参数）
                //传入进来的 url="js://webview?arg1=111&arg2=222"（同时也是约定好的需要拦截的）
                Uri uri = Uri.parse(message);
                // 如果url的协议 = 预先约定的 js 协议,就解析往下解析参数
                if (uri.getScheme().equals("js")) {
                    // 如果 authority = 预先约定协议里的webview，即代表符合约定的协议
                    // 所以拦截url,下面JS开始调用Android需要的方法
                    if (uri.getAuthority().equals("webview")) {

                        // 步骤3：执行JS所需要调用的逻辑
                        System.out.println("js调用了Android的方法");
                        // 可以在协议上带有参数并传递到Android上
                        HashMap<String, String> params = new HashMap<>();
                        Set<String> collection = uri.getQueryParameterNames();

                        //参数result:代表消息框的返回值(输入值)
                        result.confirm("Android回调给JS的数据为useid=123456 webview");
                    } else if (uri.getAuthority().equals("demo")) {
                        //参数result:代表消息框的返回值(输入值)
                        result.confirm("Android回调给JS的数据为useid=123456 demo");
                    }
                    return true;
                }
                return super.onJsPrompt(view, url, message, defaultValue, result);
            }

            // 拦截JS的警告框
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                return super.onJsAlert(view, url, message, result);
            }

            // 拦截JS的确认框
            @Override
            public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
                return super.onJsConfirm(view, url, message, result);
            }
        });

    }

    private void loadJs() {
        mWebView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true); // 设置与Js交互的权限
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);// 设置允许JS弹窗

        mWebView.loadUrl("file:///android_asset/common_javascript.html");
        btn_loadJs = (Button) findViewById(R.id.btn_load);
        btn_loadJs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 通过Handler发送消息
                mWebView.post(new Runnable() {
                    @Override
                    public void run() {
                        // 调用javascript的callJS()方法
                        mWebView.loadUrl("javascript:callJS()");
                    }
                });
            }
        });

        // 由于JS中需要alert弹窗显示结果，所以要设置setWebChromeClient
        mWebView.setWebChromeClient(new WebChromeClient());


    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void evaluateJavascript() {
        mWebView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true); // 设置与Js交互的权限
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);// 设置允许JS弹窗
        mWebView.loadUrl("file:///android_asset/common_javascript.html");
        if (Build.VERSION.SDK_INT < 18) {
            mWebView.loadUrl("javascript:callJS()");
        } else {
            mWebView.evaluateJavascript("javascript:callJS()", new ValueCallback<String>() {
                @Override
                public void onReceiveValue(String value) {
                    //此处为 js 返回的结果
                    System.out.println("value="+value);
                }
            });
        }
        mWebView.setWebChromeClient(new WebChromeClient());
    }
}
