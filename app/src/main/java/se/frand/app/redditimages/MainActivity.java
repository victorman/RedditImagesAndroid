package se.frand.app.redditimages;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.GridView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

    RedditImageAdapter adapter;
    Handler handler;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler();

        GridView gridView = (GridView) findViewById(R.id.gridview);

        adapter = new RedditImageAdapter(this);
        gridView.setAdapter(adapter);

    }

    public void setSubreddit(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reddit url");

        final EditText editText = new EditText(this);
        editText.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(editText);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {

                    final RedditRequestTask task = new RedditRequestTask();
                    task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new URL(editText.getText().toString()));

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Log.d("thread", "executing handler post");
                                        adapter.addAll(task.get());
                                        Log.d("thread", "after task.get");
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    } catch (ExecutionException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }
                    }).start();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private class RedditRequestTask extends AsyncTask<URL,Void,List<RedditImage>> {

        private final String LOG_TAG = RedditRequestTask.class.getSimpleName();

        @Override
        protected List<RedditImage> doInBackground(URL... params) {
            List<RedditImage> list = new ArrayList<RedditImage>();
            Log.d(LOG_TAG, "Starting sync");

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            String jsonStr;

            try {

                Uri builtUri = Uri.parse(params[0].toString()).buildUpon()
                        .appendPath(".json")
                        .build();
                Log.v(LOG_TAG, builtUri.toString());

                URL url = new URL(builtUri.toString());

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();

                if(inputStream == null) {
                    return null;
                }

                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while((line = reader.readLine()) != null) {
                    buffer.append(line + "\n");
                }

                if(buffer.length() == 0) {
                    return null;
                }

                jsonStr = buffer.toString();
                list.addAll(getRedditImagesFromJson(jsonStr));
            } catch (IOException  e) {
                Log.e(LOG_TAG, "Error ", e);
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

            return list;
        }

        private List<? extends RedditImage> getRedditImagesFromJson(String jsonStr) {
            List<RedditImage> list = new ArrayList<RedditImage>();
            try {
                JSONObject jsonData = new JSONObject(jsonStr).getJSONObject("data");
                JSONArray childrenJsonArray = jsonData.getJSONArray("children");

                for(int i = 0; i < childrenJsonArray.length(); i++) {
                    JSONObject child = childrenJsonArray.getJSONObject(i);
                    if(child.getString("kind").compareTo("t3") != 0) {
                        continue;
                    }
                    JSONObject data = child.getJSONObject("data");
                    String s = null;
                    if((s = data.getString("thumbnail")) != null && !s.isEmpty()) {
                        URL thumbUrl = null;
                        URL imageUrl = null;
                        try {
                            thumbUrl = new URL(s);
                            imageUrl = new URL(data.getString("url"));
                            list.add(new RedditImage(null,thumbUrl,imageUrl));
                        } catch (MalformedURLException e) {

                        }
                    } else {
                        continue;
                    }
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.v(LOG_TAG, "added " + list.size() + " items");
            for(RedditImage rimage : list) {

                Log.d(LOG_TAG, rimage.thumbnailUrl.toString());
            }
            return list;
        }
    }
}
