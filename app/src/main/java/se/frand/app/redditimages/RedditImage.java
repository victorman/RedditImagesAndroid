package se.frand.app.redditimages;

import android.widget.ImageView;

import java.net.URL;

/**
 * Created by victorfrandsen on 11/7/15.
 */


public class RedditImage {
    public ImageView imageView;
    public URL thumbnailUrl;
    public URL imageUrl;
    public RedditImage (ImageView view, URL thumb, URL image) {
        this.imageView = view;
        this.thumbnailUrl = thumb;
        this.imageUrl = image;
    }
}
