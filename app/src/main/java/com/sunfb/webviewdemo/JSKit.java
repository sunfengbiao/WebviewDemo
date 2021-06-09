package com.sunfb.webviewdemo;

import android.webkit.JavascriptInterface;

/**
 * @Author: S_Fbuner
 * @CreateDate: 2021/6/9 4:57 PM
 * @UpdateDate: 2021/6/9 4:57 PM
 * @Version: 1.0
 * @Description: 桥接方法
 */
public class JSKit {

    // 定义JS需要调用的方法，被JS调用的方法必须加入@JavascriptInterface注解
    @JavascriptInterface
    public void hello(String msg) {
        System.out.println("JS传递的参数："+msg);
        System.out.println("JS成功调用了Android的hello方法");
    }
}

