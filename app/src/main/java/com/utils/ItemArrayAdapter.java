package com.utils;

/**
 * Created by HankWu_Office on 2015/8/28.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.via.cloudwatch.R;

import java.util.List;


public class ItemArrayAdapter extends ArrayAdapter<Item> {

    private Context c;
    private int id;
    private List<Item> items;

    public ItemArrayAdapter(Context context, int layoutResourceId,
                            List<Item> objects) {
        super(context, layoutResourceId, objects);
        c = context;
        id = layoutResourceId;
        items = objects;
    }
    public int getCount() {
        return (items != null) ? items.size() : 0;
    }
    public Item getItem(int i)
    {
        return (items != null) ? items.get(i) : null;
    }
    public long getItemId(int position) {
        return position;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(id, null);
        }
        /* create a new view of my layout and inflate it in the row */
        //convertView = ( RelativeLayout ) inflater.inflate( resource, null );

        final Item o = items.get(position);
        if (o != null) {
            if (id == R.layout.view_camera)
            {
                loadImage(v, R.id.source_setting_left_list_icon);
                loadText(v, R.id.source_setting_left_list_name, o.getName());
            } else if (id == R.layout.view_channel ) {
                loadText(v, R.id.source_setting_left_list_name, o.getName());
            }
        }
        return v;
    }

    private void loadImage(View v, int ivid) {
        ImageView iv = (ImageView) v.findViewById(ivid);
        if (iv != null )
        {
            String uri = "drawable/camera_blue";
            int imageResource = c.getResources().getIdentifier(uri, null, c.getPackageName());
            Bitmap type = BitmapFactory.decodeResource(c.getResources(), imageResource);
            iv.setImageBitmap(type);
            //int imageResource = c.getResources().getIdentifier(uri, null, c.getPackageName());
            //Drawable image = c.getResources().getDrawable(imageResource);
            //iv.setImageDrawable(image);
        }
    }

    private void loadImage(View v, int ivid, String name) {
        ImageView iv = (ImageView) v.findViewById(ivid);
        if (iv != null && name != null)
        {
            String uri = "drawable/" + name;
            int imageResource = c.getResources().getIdentifier(uri, null, c.getPackageName());
            Bitmap type = BitmapFactory.decodeResource(c.getResources(), imageResource);
            iv.setImageBitmap(type);

        }
    }

    private void loadText(View v, int tvid, String str) {
        TextView tv = (TextView) v.findViewById(tvid);
        if (tv != null && str != null)
        {
            tv.setText(str);
        }
    }
}
