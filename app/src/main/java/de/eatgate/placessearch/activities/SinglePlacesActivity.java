package de.eatgate.placessearch.activities;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;

import de.eatgate.placessearch.R;
import de.eatgate.placessearch.deprecated.EatGateReviewService;
import de.eatgate.placessearch.entities.EatGatePhotoUrl;
import de.eatgate.placessearch.entities.EatGateReview;
import de.eatgate.placessearch.entities.Review;
import de.eatgate.placessearch.global.AppGob;
import de.eatgate.placessearch.helpers.InternetManager;
import de.eatgate.placessearch.helpers.ListViewAdapterRev;

/**
 * Created by Khanh on 19.01.2015.
 */
public class SinglePlacesActivity extends Activity {
    private static ArrayList<EatGateReview> eatGateReviewList = null;
    private static ArrayList<EatGatePhotoUrl> eatGatePhotoList = null;
    private final String server = "http://192.168.70.22/EatGate/api/WWWBewertungPortal";
    private ArrayAdapter<String> listAdapter;
    private ListViewAdapterRev adapter;
    private AlertDialog.Builder builder = null;
    private AlertDialog dialog = null;
    private ImageView mImage;
    private Button btnImgs;
    private TextView mText;
    private ProgressDialog statusProgress = null;
    // private String[] arrPath = {"http://www.hs-fulda.de/fileadmin/Fachbereich_AI/News/2015/Boston/lecture-hall-bu.jpg",
    //         "http://www.hs-fulda.de/fileadmin/Fachbereich_AI/News/2015/Boston/HILLEL_falonmoran.jpg",
    //         "http://www.hs-fulda.de/fileadmin/Fachbereich_AI/News/2015/Boston/boston-university-classroom-lobby-renovations.jpg"};
    private int imgPos;
    private EatGateReviewService eatGateReviewService = null;
    private AppGob app;
    private String placeId;

    /**
     * da static ausserhalb der inneren Klasse
     *
     * @param url
     * @param objJSON
     * @return
     */
    private static int postJSONObj(String url, JSONObject objJSON, Activity ac, String service) {
        InputStream inputStream = null;
        String result = "";
        int responseCode = 0;
        try {

            // 1. create HttpClient
            HttpClient httpclient = new DefaultHttpClient();

            // 2. make POST request to the given URL
            HttpPost httpPost = new HttpPost(url);

            String json = "";
            // 4. convert JSONObject to JSON to String
            json = objJSON.toString();

            // ** Alternative way to convert Person object to JSON string usin Jackson Lib
            // ObjectMapper mapper = new ObjectMapper();
            // json = mapper.writeValueAsString(person);

            // 5. set json to StringEntity
            StringEntity se = new StringEntity(json);

            // 6. set httpPost Entity
            httpPost.setEntity(se);

            // 7. Set some headers to inform server about the type of the content
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");

            // 8. Execute POST request to the given URL
            HttpResponse httpResponse = httpclient.execute(httpPost);

            // 9. receive response as inputStream
            inputStream = httpResponse.getEntity().getContent();

            StatusLine statusLine = httpResponse.getStatusLine();
            responseCode = statusLine.getStatusCode();


            // 10. convert inputstream to string
            if (inputStream != null) {
                result = convertInputStreamToString(inputStream);
                if (service.equals("ViewPhotoDownload")) {
                    JSONArray array = new JSONArray(result);
                    if (array != null) {
                        ArrayList<EatGatePhotoUrl> arrayList = new ArrayList<EatGatePhotoUrl>();
                        for (int i = 0; i < array.length(); i++) {
                            EatGatePhotoUrl photoUrl = EatGatePhotoUrl.jsonToEatGatePhotoUrl((JSONObject) array.get(i));
                            Log.i(" EatGatePhotoService ", photoUrl.toString());
                            arrayList.add(photoUrl);
                        }
                        eatGatePhotoList = arrayList;
                    }
                } else if (service.equals("ReadBewertungen")) {
                    JSONArray array = new JSONArray(result);
                    ArrayList<EatGateReview> arrayList = new ArrayList<EatGateReview>();
                    for (int i = 0; i < array.length(); i++) {
                        EatGateReview reviewE = EatGateReview
                                .jsonToEatGateReview((JSONObject) array.get(i));
                        Log.i(" EatGate Review Service ", reviewE.toString());
                        arrayList.add(reviewE);
                    }
                    eatGateReviewList = arrayList;
                }
            } else {
                result = "Did not work!";
            }

        } catch (Exception e) {
            Log.e("JSON Result Review", e.getLocalizedMessage());
        }

        Log.i("JSON Result Review", result);


        // 11. return result Response vom Server
        return responseCode;
    }

     /**
      * da static ausserhalb der inneren Klasse
      *
      * @param inputStream
      * @return
      * @throws IOException
      */
     private static String convertInputStreamToString(InputStream inputStream) throws IOException {
         BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
         String line = "";
         String result = "";
         while ((line = bufferedReader.readLine()) != null)
             result += line;
         inputStream.close();
         return result;
    }

    private void buildInfoDialog() {
        builder = new AlertDialog.Builder(this);
        builder.setTitle("About");
        builder.setMessage("EatGate Version 1.0 2015");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("Abbruch", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // nichts weiter tun; Dialog schließen
                dialog.dismiss();
            }
        });
        builder.setCancelable(false); // nicht schließen mit ZURÜCK-Button
        dialog = builder.create();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_single_place);
        buildInfoDialog();
        app = (AppGob) getApplication();
        placeId = app.g_placeDetails.getPlace_id();

        if (!InternetManager.isOnline(this)) {
            Toast.makeText(this, "Keine Internet-Verbindung", Toast.LENGTH_LONG).show();
        } else {

            if (app != null) {

                String str_name = app.g_placeDetails.getName();
                TextView tv_name = (TextView) findViewById(R.id.name);
                tv_name.setText(str_name);

                String str_adresse = app.g_placeDetails.getVicinity();
                TextView tv_adress = (TextView) findViewById(R.id.adresse);
                tv_adress.setText(str_adresse);

                String str_rating = "" + app.g_placeDetails.getRating();
                TextView tv_rating = (TextView) findViewById(R.id.rating);
                tv_rating.setText(str_rating);

                String placeId = "" + app.g_placeDetails.getPlace_id();

                ArrayList<String> arrList = app.g_placeDetails.getWeekdays();

                if (arrList != null) {
                    Log.i("singlePlace", "arraylist: " + arrList.size());
                    TextView tv_OpenHours = (TextView) findViewById(R.id.str_oppenHours);
                    tv_OpenHours.setVisibility(View.VISIBLE);
                    int index = 1;
                    for (String str : arrList) {
                        Log.i("WEEKLY-Info", str);
                        TextView v;
                        switch (index) {
                            case 1:
                                v = (TextView) findViewById(R.id.tag_mo);
                                v.setVisibility(View.VISIBLE);
                                v.setText(str);
                                break;
                            case 2:
                                v = (TextView) findViewById(R.id.tag_di);
                                v.setVisibility(View.VISIBLE);
                                v.setText(str);
                                break;
                            case 3:
                                v = (TextView) findViewById(R.id.tag_mi);
                                v.setVisibility(View.VISIBLE);
                                v.setText(str);
                                break;
                            case 4:
                                v = (TextView) findViewById(R.id.tag_do);
                                v.setVisibility(View.VISIBLE);
                                v.setText(str);
                                break;
                            case 5:
                                v = (TextView) findViewById(R.id.tag_fr);
                                v.setVisibility(View.VISIBLE);
                                v.setText(str);
                                break;
                            case 6:
                                v = (TextView) findViewById(R.id.tag_sa);
                                v.setVisibility(View.VISIBLE);
                                v.setText(str);
                                break;
                            case 7:
                                v = (TextView) findViewById(R.id.tag_so);
                                v.setVisibility(View.VISIBLE);
                                v.setText(str);
                                break;
                            default:
                                break;
                        }
                        index++;
                    }
                }

                makeWebCalls();
            }
        }
    }

    private boolean makeWebCalls() {
        new CheckEatGatePlace(this).execute(server);
        new GetEatGatePhotos(this).execute(server);
        new GetEatGateReviews(this).execute(server);

        ArrayList<Review> reviews = app.g_placeDetails.getArrRev();

        if (reviews != null) {
            Log.i("reviews", "Reviews: " + reviews.get(0).getText());
            for (Review rv : reviews) {
                addReviewItems(rv.getText(), "" + rv.getRating());
            }
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_ex, menu);
        ActionBar bar = getActionBar(); // or MainActivity.getInstance().getActionBar()
        // Alternative, falls XML-Styles fuer Actionbar nicht funktionieren via Code
        // Style der Actionbar veraendern
        // bar.setBackgroundDrawable(new ColorDrawable(0xff00DDED));
        TextView titleView = (TextView) findViewById(R.id.action_bar_title);
        if (titleView == null) {
            int titleId = Resources.getSystem().getIdentifier("action_bar_title", "id", "android");
            titleView = (TextView) findViewById(titleId);
        }
        titleView.setText("EatGate");
        titleView.setTextColor(Color.WHITE);
        // another solution which did work
        // Spannable text = new SpannableString(bar.getTitle());
        // text.setSpan(new ForegroundColorSpan(Color.BLUE), 0, text.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        // bar.setTitle(text);
        // alternative Loesung, die funktioniert
        // bar.setTitle((Html.fromHtml("<font color=\"#FF4444\">" + "EatGate"+ "</font>")));
        bar.setDisplayShowTitleEnabled(false);  // required to force redraw, without, gray color
        bar.setDisplayShowTitleEnabled(true);
        return true;
    }

    @Override
    /**
     * reagiert auf die Klicks in der Actionbar
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        View menuItemView = findViewById(R.id.action_menu);
        if (id == R.id.action_menu) {
            onClickMainMenu(menuItemView);
        } else if (id == R.id.action_back) {
            Intent intent = new Intent(this, PlaceMapActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Erzeugt das Popup Menu und registriert den ClickListener fuer das Submenu
     *
     * @param v
     */
    public void onClickMainMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);

        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.menu_main_popsub_exsp, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_info:
                        dialog.show();
                        return true;
                    case R.id.action_close:
                        finish();
                        return true;
                    case R.id.action_makephoto:
                        onClickTakePhoto();
                        return true;
                    case R.id.action_makereview:
                        onClickWriteRev();
                        return true;
                    default:
                        return false;
                }
            }
        });
        popup.show();
    }

    public void onClickTakePhoto() {
        Intent intent = new Intent(this, TakePhotoActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        // beendet aktuelle Activity
        // finish();
    }

    public void onClickWriteRev() {
        Intent intent = new Intent(this, MakeReviewActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        // beendet aktuelle Activity
        // finish();
    }

    protected void addReviewItems(String inhalt, String voting) {
        LinearLayout linearLayout = (LinearLayout) findViewById(R.id.linearLayoutFromImage);
        TextView view1 = new TextView(this);
        view1.setPadding(5, 5, 5, 5);
        if (inhalt.isEmpty()) inhalt = inhalt + "---";
        view1.setText(inhalt);
        view1.setTextColor(Color.WHITE);
        linearLayout.addView(view1);
        TextView view2 = new TextView(this);
        view2.setPadding(5, 5, 15, 5);
        if (voting.equals("0.0")) voting = "---";
        view2.setText("Rating: " + voting);
        view2.setTextColor(Color.WHITE);
        linearLayout.addView(view2);
    }

    private void loadDataImage() {
        imgPos = 0;
        btnImgs = (Button) findViewById(R.id.btnAddImgs);

        btnImgs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (imgPos >= eatGatePhotoList.size()) {
                    Button b = (Button) v;
                    b.setEnabled(false);
                    Toast.makeText(SinglePlacesActivity.this, "Keine weiteren Fotos ...", Toast.LENGTH_SHORT).show();
                    return;
                }
                statusProgress = ProgressDialog.show(SinglePlacesActivity.this, "Bitte warten ...", "Foto wird vom Server abgerufen", true, false);
                try {
                    new DownloadTask(SinglePlacesActivity.this).execute(eatGatePhotoList.get(imgPos).getPhotoUrl());
                } catch (Exception ex) {
                    Log.e("PhotoLoadException", "Laden schlug fehl!");
                    imgPos--;
                }
                imgPos++;
            }
        });

        if (eatGatePhotoList == null || eatGatePhotoList.size() == 0) {
            btnImgs.setEnabled(false);
        } else {
            btnImgs.setEnabled(true);
        }
    }

    private class DownloadTask extends AsyncTask<String, Void, Bitmap> {
        private Activity mActivity;

        public DownloadTask(Activity ac) {
            mActivity = ac;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            // Toast.makeText(mActivity,"G", Toast.LENGTH_LONG).show();
            return loadImgStream(params);
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            // Toast.makeText(SinglePlacesActivity.this, "Successfully", Toast.LENGTH_LONG).show();
            LinearLayout linearLayoutImg = (LinearLayout) mActivity.findViewById(R.id.linearLayoutFromImage);
            ImageView image = new ImageView(mActivity);
            image.setPadding(5, 5, 5, 5);
            image.setAdjustViewBounds(true);
            image.setImageBitmap(result);
            linearLayoutImg.addView(image);
            statusProgress.dismiss();
        }

        private Bitmap loadImgStream(String... path) {
            // <aimageview>.setImageURI(Uri.parse(new File("/sdcard/cats.jpg").toString()));
            // String fileP = getPhotoFilePath().getAbsolutePath() + "/" + path[0];
            // Log.i("loadImgStream", getPhotoFilePath().getAbsolutePath() + "/" + path[0]);
            // File f = new File(fileP);
            try {
                Bitmap bitmap = getBitmapFromUri3(path[0]);
                return bitmap;
            } catch (Exception e) {
                e.printStackTrace();
                Log.e("loadImgStream", path[0]);
            }
            return null;
        }

        private File getPhotoFilePath() {
            File directory = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), getPackageName());
            if (!directory.exists()) {
                // toDo throws Exception
            }
            return directory;
        }

        private Bitmap getBitmapFromUri(Uri uri)
                throws IOException {
            ParcelFileDescriptor parcelFileDescriptor =
                    getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor
                    .getFileDescriptor();
            Bitmap image = BitmapFactory
                    .decodeFileDescriptor(fileDescriptor);
            parcelFileDescriptor.close();
            return image;
        }

        private Bitmap getBitmapFromUri3(String uriString) {
            try {
                InputStream is = (InputStream) new URL(uriString).getContent();
                Bitmap bitmap = BitmapFactory.decodeStream(is);
                return bitmap;
            } catch (Exception e) {
                // toDo Standard image "Error loading ..."
                Log.e("getBitmapFromUri3", "Error loading");
                return null;
            }
        }

        private Bitmap getBitmapFromUri2(Uri photoUri) {

            // Drawable d = Drawable.createFromStream(is, "src name");
            File imageFile = new File(photoUri.getPath());

            if (imageFile.exists()) {
                Bitmap bitmap = BitmapFactory.decodeFile(photoUri.getPath());
                Bitmap bitmapsimplesize = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() / 8, bitmap.getHeight() / 8, true);
                bitmap.recycle();
                //img1.setImageBitmap(bitmapsimplesize);
                return bitmapsimplesize;
            }
            return null;
        }
    }


    /**
     * Klasse fuer Webservice für EatGateReviews
     */
    private class GetEatGateReviews extends AsyncTask<String, Void, Integer> {

        private Activity mActivity;

        public GetEatGateReviews(Activity ac) {
            this.mActivity = ac;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(String... urls) {

            // erzeuge JSON zum senden
            app = (AppGob) getApplication();
            JSONObject reviewsJSON = new JSONObject();
            try {
                reviewsJSON.put("Service", "ReadBewertungen");
                reviewsJSON.put("Place_id", app.g_placeDetails.getPlace_id());
                Log.i("PlaceJson", "ausgefuehrt" + urls[0]);
            } catch (Exception ex) {
                // toDo
                Log.e("JSON Exception", "doInBackground Fehler");
            }

            // ausführen des Requests an den Server
            int responseCd = postJSONObj(urls[0], reviewsJSON, mActivity, "ReadBewertungen");
            Log.i("JSON Log", "" + responseCd);
            // statusProgress.dismiss();
            return responseCd;
        }

        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            LinearLayout linearLayout = (LinearLayout) SinglePlacesActivity.this.findViewById(R.id.linearLayoutFromImage);
            if (eatGateReviewList == null) return;
            for (EatGateReview rv : eatGateReviewList) {
                // add TextViews fuer EatGate Reviews
                String inhalt = rv.getInhalt();
                String voting = rv.getVoting();
                String autor = rv.getAuthorNickname();
                TextView view1 = new TextView(SinglePlacesActivity.this);
                view1.setPadding(5, 5, 5, 5);
                if (inhalt.isEmpty()) inhalt = inhalt + "---";
                view1.setText(inhalt);
                view1.setTextColor(Color.WHITE);
                linearLayout.addView(view1);
                TextView view2 = new TextView(SinglePlacesActivity.this);
                view2.setPadding(5, 5, 15, 5);
                if (voting.equals("0.0")) voting = "---";
                if (autor.isEmpty()) autor = "---";
                view2.setText("Rating: " + voting + " von " + autor);
                view2.setTextColor(Color.WHITE);
                linearLayout.addView(view2);
            }
        }
    }

    /**
     * Klasse fuer Webservice für EatGatePhotos Pfad
     */
    private class GetEatGatePhotos extends AsyncTask<String, Void, Integer> {

        private Activity mActivity;

        public GetEatGatePhotos(Activity ac) {
            this.mActivity = ac;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(String... urls) {

            // erzeuge JSON zum senden
            app = (AppGob) getApplication();
            JSONObject photosJSON = new JSONObject();
            try {
                photosJSON.put("Service", "ViewPhotoDownload");
                photosJSON.put("Place_id", app.g_placeDetails.getPlace_id());

                Log.i("PlaceJson", "ausgefuehrt" + urls[0]);
            } catch (Exception ex) {
                // toDo
                Log.e("JSON Exception", "doInBackground Fehler");
            }

            // ausführen des Requests an den Server
            int responseCd = postJSONObj(urls[0], photosJSON, mActivity, "ViewPhotoDownload");
            Log.i("JSON Log", "" + responseCd);
            // statusProgress.dismiss();
            return responseCd;


        }

        @Override
        protected void onPostExecute(Integer re) {
            // Laden der Bilder fuer Locations
            if (eatGatePhotoList != null && eatGatePhotoList.size() > 0) {
                // Button + ClickListner hinzufuegen falls Photos vorhanden
                loadDataImage(); // bereitet das Laden der Bilder aus Call GetEatGatePhotos vor
            }
        }
    }


    /**
     * Helper class for Registration
     */
    private class CheckEatGatePlace extends AsyncTask<String, Void, Integer> {
        private Activity mActivity;

        public CheckEatGatePlace(Activity ac) {
            mActivity = ac;
        }

        /**
         * Erledige bevor Ausführen von Task
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Integer doInBackground(String... urls) {

            // erzeuge JSON zum senden
            app = (AppGob) getApplication();
            JSONObject placeJSON = new JSONObject();
            try {
                placeJSON.put("Service", "AddLokation");
                placeJSON.put("Place_id", app.g_placeDetails.getPlace_id());
                placeJSON.put("Name", app.g_placeDetails.getName());
                placeJSON.put("Adresse", app.g_placeDetails.getVicinity());
                placeJSON.put("Lng", "0"); // not used
                placeJSON.put("Lat", "0"); // not used
                Log.i("PlaceJson", "ausgefuehrt" + urls[0]);
            } catch (Exception ex) {
                // toDo
                Log.e("JSON Exception", "doInBackground Fehler");
            }

            // ausführen des Requests an den Server
            int responseCd = postJSONObj(urls[0], placeJSON, mActivity, "AddLokation");
            Log.i("JSON Log", "" + responseCd);
            // statusProgress.dismiss();
            return responseCd;
        }

        /**
         * Empty - Aufgabe wird von PostReview uebernommen
         */
        @Override
        protected void onPostExecute(Integer code) {
            Log.i("JSON Log", "onPostExecut " + code);
            if (code == 201) {
                Toast.makeText(SinglePlacesActivity.this, "Success Reading EatGate Service!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(SinglePlacesActivity.this, "Error Reading EatGate Service!", Toast.LENGTH_SHORT).show();
            }
        }
    }

}




