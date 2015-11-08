package se.frand.app.redditimages;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

/**
 * Created by victorfrandsen on 11/7/15.
 */
public class RedditImageAdapter extends BaseAdapter {
    private ArrayList<RedditImage> list;
    private Context mContext;
    private LayoutInflater inflater;

    public RedditImageAdapter(Context context) {
        list = new ArrayList<RedditImage>();
        mContext = context;
        inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final RedditImage rImage = (RedditImage) getItem(position);

        if(convertView == null) {
            convertView = inflater.inflate(R.layout.grid_item,null);
        }

        final ImageView imageView = (ImageView) convertView.findViewById(R.id.grid_item_image);

        rImage.imageView = imageView;

        final ImageTask task = new ImageTask();

        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,rImage.thumbnailUrl);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    imageView.setImageBitmap(task.get());
                } catch (InterruptedException | ExecutionException e) {
                    Log.d("getView thread", "can't get image");
                }
            }
        }).run();

        return convertView;
    }

    public boolean add(RedditImage image) {
        boolean added = this.list.add(image);
        if (added) notifyDataSetChanged();
        return added;
    }

    public boolean addAll(Collection<RedditImage> list) {
        boolean added = this.list.addAll(list);
        if (added) notifyDataSetChanged();
        return added;
    }

    public class ImageTask extends AsyncTask<URL,Void,Bitmap> {

        private final String LOG_TAG = ImageTask.class.getSimpleName();

        @Override
        protected Bitmap doInBackground(URL... params) {
            Bitmap returnBitmap = null;
            try {
                Log.v(LOG_TAG,"loading image " + params[0].toString());
                returnBitmap = BitmapFactory.decodeStream(params[0].openConnection().getInputStream());
            } catch (IOException e) {
                Log.e(LOG_TAG, "Failed loading image");
            }
            return returnBitmap;
        }
    }
}
