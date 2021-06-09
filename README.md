# WebviewDemo
Android WebView与JS的交互方式总结
## 一、Android与JS的交互-桥梁WebView
(一) JS调用Android方法
JS调用Android方法有三种，下面依次介绍 原理、使用、优缺点。

1、通过WebView的addJavascriptInterface()进行对象映射
具体原理

Android和JS通过webview.addJavascriptInterface(new JSKit(),"mjs")方法形成对象映射,JS中的mjs对象就可以调用Android中的JSKit对象中的方法了。

具体使用

步骤1：在Android里通过WebView设置Android类与JS代码的映射

public class MainActivity extends AppCompatActivity {
    WebView mWebView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWebView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true);//设置与Js交互的权限
        //参数1：Java对象名 参数2：Javascript对象名
        //通过addJavascriptInterface() AJavaScriptInterface类对象映射到JS的mjs对象
        mWebView.addJavascriptInterface(new JSKit(),"mjs");
        // 加载JS代码
        mWebView.loadUrl("file:///android_asset/javascript.html");
复制代码
步骤2：定义一个与JS对象映射关系的Android类：JSKit

public class JSKit {

    // 定义JS需要调用的方法，被JS调用的方法必须加入@JavascriptInterface注解
    @JavascriptInterface
    public void hello(String msg) {
        System.out.println("JS成功调用了Android的hello方法");
    }
}
复制代码
步骤3：将需要调用的JS代码javascript.html格式放到src/main/assets文件夹里

<!DOCTYPE html>
<html>
   <head>
      <meta charset="utf-8">
      <title>Carson</title>  
      <script>
         function callAndroid(){
            // 由于对象映射，所以调用test对象等于调用Android映射的对象
            mjs.hello("js去调用了android中的hello方法");
         }
      </script>
   </head>
   <body>
      //点击按钮则调用callAndroid函数
      <button type="button" id="button1" onclick="callAndroid()"></button>
   </body>
</html>
复制代码
优缺点

优点：使用简单，仅将Android对象和JS对象映射即可；

缺点：

对于Android 4.2以下，有安全漏洞，需要采用拦截prompt()的方式进行漏洞修复；

对于Android 4.2以上，则只需要对被JS调用的函数以 @JavascriptInterface进行注解

2、通过WebViewClient的shouldOverrideUrlLoading()方法回调拦截url；
具体原理

Android通过WebViewClient 的回调方法shouldOverrideUrlLoading()拦截url,解析该url的协议。如果检测到是预先约定好的协议，就调用相应方法，即JS需要调用Android中的方法。

具体使用

步骤1：在JS约定所需要的Url协议 JS代码：javascript.html，放到src/main/assets文件夹里

<!DOCTYPE html>
<html>
   <head>
      <meta charset="utf-8">
      <title>SoarYuan</title>
     <script>
         function callAndroid(){
            /*约定的url协议为：js://webview?arg1=111&arg2=222*/
            document.location = "js://webview?arg1=111&arg2=222";
         }
      </script>
</head>

<!-- 点击按钮则调用callAndroid()方法  -->
   <body>
     <button type="button" id="button2" onclick="callAndroid()">点击调用Android方法1</button>
   </body>
</html>
复制代码
当该JS通过Android的mWebView.loadUrl("file:///android_asset/javascript.html")加载后，就会回调shouldOverrideUrlLoading()。

步骤2：在Android通过WebViewClient复写shouldOverrideUrlLoading()

public class MainActivity extends AppCompatActivity {
    WebView mWebView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWebView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true); // 设置与Js交互的权限
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);// 设置允许JS弹窗
        
        //步骤1：加载JS代码,格式规定为:file:///android_asset/文件名.html
        mWebView.loadUrl("file:///android_asset/javascript.html");

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
             public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request){
    
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
                    if ( uri.getScheme().equals("js")) {
                        // 如果 authority = 预先约定协议里的webview，即代表都符合约定的协议
                        // 所以拦截url,下面JS开始调用Android需要的方法
                        if (uri.getAuthority().equals("webview")) {
                        
                            // 步骤3：执行JS所需要调用的逻辑
                            System.out.println("js调用了Android的方法");
                            // 可以在协议上带有参数并传递到Android上
                            HashMap<String, String> params = new HashMap<>();
                            Set<String> collection = uri.getQueryParameterNames();
                            
                            String result = "Android回调给JS的数据为useid=123456"; 
                            view.loadUrl("javascript:returnResult(\"" + result + "\")");
                            
                        }
                         return true;
                    }
                    return super.shouldOverrideUrlLoading(view, request);
                }
         });
    
   }
}
复制代码
优缺点

优点：不存在addJavascriptInterface()方式的漏洞；

缺点：JS获取Android方法的返回值复杂。如果JS想要得到Android方法的返回值，只能通过WebView的 loadUrl()去执行JS方法把返回值传递回去，相关的代码如下：

// Android：MainActivity.java
mWebView.loadUrl("javascript:returnResult(" + result + ")");

// JS：javascript.html
function returnResult(result){
    alert("result is" + result);
}
复制代码
3、通过WebChromeClient的onJsAlert()、onJsConfirm()、onJsPrompt()方法回调拦截JS对话框alert()、confirm()、prompt()方法，对消息message进行拦截
具体原理

Android通过 WebChromeClient的onJsAlert()、onJsConfirm()、onJsPrompt（）方法回调分别拦截JS对话框（即上述三个方法），得到他们的消息内容，然后解析即可。

具体使用

常用的拦截是JS的输入框即prompt()方法，因为只有prompt()可以返回任意类型的值，操作最全面方便、更加灵活；而alert()对话框没有返回值；confirm()对话框只能返回两种状态（确定/取消）两个值。

步骤1：加载JS代码，如下：javascript.html放到src/main/assets文件夹里

<!DOCTYPE html>
<html>
   <head>
      <meta charset="utf-8">
      <title>SoarYuan</title>
     <script>
        function clickprompt(){
            // 调用prompt()
            var result=prompt("js://demo?arg1=111&arg2=222");
            alert("demo " + result);
        }
      </script>
</head>

<!-- 点击按钮则调用clickprompt()  -->
   <body>
     <button type="button" id="button3" onclick="clickprompt()">点击调用Android方法2</button>
   </body>
</html>
复制代码
当使用mWebView.loadUrl("file:///android_asset/javascript.html")加载了上述JS代码后，就会触发回调onJsPrompt()，具体如下：

如果是拦截警告框，即alert()，则触发回调onJsAlert(); 如果是拦截确认框，即confirm()，则触发回调onJsConfirm();

步骤2：在Android通过WebChromeClient复写onJsPrompt()

public class MainActivity extends AppCompatActivity {
    WebView mWebView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWebView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true); // 设置与Js交互的权限
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);// 设置允许JS弹窗
        
        //步骤1：加载JS代码,格式规定为:file:///android_asset/文件名.html
        mWebView.loadUrl("file:///android_asset/javascript.html");

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
            if ( uri.getScheme().equals("js")) {
                // 如果 authority = 预先约定协议里的webview，即代表符合约定的协议
                // 所以拦截url,下面JS开始调用Android需要的方法
                if (uri.getAuthority().equals("webview")) {
                
                // 步骤3：执行JS所需要调用的逻辑
                    System.out.println("js调用了Android的方法");
                    // 可以在协议上带有参数并传递到Android上
                    HashMap<String, String> params = new HashMap<>();
                    Set<String> collection = uri.getQueryParameterNames();

                    //参数result:代表消息框的返回值(输入值)
                    result.confirm("Android回调给JS的数据为useid=123456");
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
}

复制代码
优缺点

优点：

不存在addJavascriptInterface()方式的漏洞；

能满足大多数情况下的交互场景；

result.confirm()方便回调给JS获取的数据；

缺点：适应复杂，需要进行协议的约束

##(二) Android调用JS方法
Android调用JS方法有两种，下面依次介绍使用方法。

1、通过WebbView.loadUrl()
步骤1：加载JS代码，如下：javascript.html放到src/main/assets文件夹里

<!DOCTYPE html>
<html>
   <head>
      <meta charset="utf-8">
      <title>Carson_Ho</title>
     <script>
        // Android需要调用的方法
         function callJS(){
             alert("Android调用了JS的callJS方法");
         }
    </script>
   </head>
</html>

复制代码
步骤2：在Android里通过WebView设置调用JS代码

 public class MainActivity extends AppCompatActivity {
    WebView mWebView;
    Button button;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mWebView =(WebView) findViewById(R.id.webview);
        WebSettings webSettings = mWebView.getSettings();
        webSettings.setJavaScriptEnabled(true); // 设置与Js交互的权限
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true);// 设置允许JS弹窗
        
        mWebView.loadUrl("file:///android_asset/javascript.html");
        button = (Button) findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
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
}
复制代码
2、通过WebView.evaluateJavascript()-Android4.4之后
evaluateJavascript()效率更高、使用更简洁。但该方法的执行不会使页面刷新，而loadUrl()的执行则会。

mWebView.evaluateJavascript("javascript:callJS()", new ValueCallback<String>() {
    @Override 
    public void onReceiveValue(String value) { 
        //此处为 js 返回的结果     
    }
}); 
复制代码
3、使用建议
两种方法混合使用，即Android 4.4以下使用loadUrl()，Android 4.4以上使用evaluateJavascript()

if (Build.VERSION.SDK_INT < 18) {
    mWebView.loadUrl("javascript:callJS()");
} else {
    mWebView.evaluateJavascript（"javascript:callJS()", new ValueCallback<String>() {
        @Override
        public void onReceiveValue(String value) {
            //此处为 js 返回的结果
        }
    });
}
复制代码
二、开发时遇到的问题
1、Alert无法弹出
应该是没有设置WebChromeClient,按照以下代码设置：

myWebView.setWebChromeClient(new WebChromeClient());
复制代码
2、Uncaught ReferenceError: functionName is not defined
问题出现原因，网页的JS代码没有加载完成，就调用了js方法。解决方法是在网页加载完成之后调用JS方法。例如：刚打开html页面，就去调用JS中的returnResult方法

webview.loadUrl("file:///android_asset/javascript.html");
   
String result = "Android回调给JS的数据为useid=123456";
webview.loadUrl("javascript:returnResult(\"" + result + "\")");
复制代码
按照以下代码设置：

webview.setWebViewClient(new WebViewClient(){
      @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);

                String result = "Android回调给JS的数据为useid=123456";
                webview.loadUrl("javascript:returnResult(\"" + result + "\")");
            }
});
复制代码
3、Uncaught TypeError: Object [object Object] has no method
安全限制问题，出现在Android4.2以上的机器。

解决方法： 将targetSdkVersion设置成17或更高，引入@JavascriptInterface注释。

代码混淆问题，如果在没有混淆的版本运行正常，在混淆后的版本的代码运行错误，并提示Uncaught TypeError: Object [object Object] has no method，那就是你没有做混淆例外处理。 在混淆文件加入类似这样的代码 解决方法：

在proguard-rules.pro中添加混淆。
-keepattributes *Annotation*  
-keepattributes *JavascriptInterface*
-keep public class xx.xxx.JSKit{
   public <methods>;
}
其中xx.xxx..JSKit 是不需要混淆的类

复制代码
4、All WebView methods must be called on the same thread
在JS调用后的Java回调线程并不是主线程。解决上述的异常，将webview操作放在主线程中即可。

webView.post(new Runnable() {
    @Override
    public void run() {
        webView.loadUrl(YOUR_URL).
    }
});
