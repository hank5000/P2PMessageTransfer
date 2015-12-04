package com.utils;

public class Item implements Comparable<Item> {
    private String name;
    private String data;
    private String date;
    private String path;
    private int    pathtype;
    private String icon;
    private String icontype;

    public Item(String n,String d, String dt, String p, String ic) // view_localfile
    {
        name = n;
        data = d;
        date = dt;
        path = p;
        icon = ic;
    }
    public Item(String n, String p, int pt, String ic, String ictp) // view_camera
    {
        name = n;
        path = p;
        pathtype = pt;
        icon = ic;
        icontype = ictp;
    }

    public Item(String n) // view_camera
    {
        name = n;

    }

    public String getName()
    {
        return name;
    }
    public String getData()
    {
        return data;
    }
    public String getDate()
    {
        return date;
    }
    public String getPath()
    {
        return path;
    }
    public int getPathType()
    {
        return pathtype;
    }
    public String getIcon() {
        return icon;
    }
    public String getIconType() {
        return icontype;
    }

    public int compareTo(Item o) {
        if(this.name != null)
            return this.name.toLowerCase().compareTo(o.getName().toLowerCase());
        else
            throw new IllegalArgumentException();
    }
}
