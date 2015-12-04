package com.utils;

/**
 * Created by HankWu_Office on 2015/11/25.
 */
public class LiveViewInfo {
    String name;
    String status;
    String path;
    LiveViewInfo(String n,String s) {
        name = n;
        status = s;
    }

    LiveViewInfo(String n,String s,String p) {
        name = n;
        status = s;
        path = p;
    }

    public void setName(String n) {
        name = n;
    }
    public void setStatus(String s) {
        status = s;
    }

    public String getName() {
        return name;
    }

    public String getStatus() {
        return status;
    }

}

