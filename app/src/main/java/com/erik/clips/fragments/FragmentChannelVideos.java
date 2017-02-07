package com.erik.clips.fragments;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.lsjwzh.widget.materialloadingprogressbar.CircleProgressBar;
import com.marshalchen.ultimaterecyclerview.ItemTouchListenerAdapter;
import com.marshalchen.ultimaterecyclerview.UltimateRecyclerView;
import com.erik.clips.R;
import com.erik.clips.adapters.AdapterList;
import com.erik.clips.utils.MySingleton;
import com.erik.clips.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 *
 * FragmentChannelVideos создается, чтобы отображать видео данные канала YouTube или список воспроизведения.
 * Созданный с использованием фрагмента.
 */
public class FragmentChannelVideos extends Fragment implements View.OnClickListener {

    // Создать тег для журнала
    private static final String TAG = FragmentChannelVideos.class.getSimpleName();
    // Создание view objects
    private TextView mLblNoResult;
    private LinearLayout mLytRetry;
    private CircleProgressBar mPrgLoading;
    private UltimateRecyclerView mUltimateRecyclerView;
    private AdView mAdView;

    // Создать переменную для обработки AdMob видимости
    private boolean mIsAdmobVisible;

    // Создать переменную для хранения идентификатора канала и тип видео
    private int mVideoType;
    private String mChannelId;

    // Создание слушателя
    private OnVideoSelectedListener mCallback;

    // Создать объект AdapterList
    private AdapterList mAdapterList = null;

    // Create arraylist variable to store video data before get video duration
    // Создать Arraylist переменную для хранения видеоданных до того как получим продолжительность видео
    private ArrayList<HashMap<String, String>> mTempVideoData = new ArrayList<>();

    // Создать Arraylist переменную для хранения final данных
    private ArrayList<HashMap<String, String>> mVideoData     = new ArrayList<>();

    private String mNextPageToken = "";
    private String mVideoIds = "";
    private String mDuration = "00:00";

    // Параметр (истина = данные по-прежнему существуют в сервере, ложный = данные уже все загружены )
    private boolean mIsStillLoading = true;

    // Параметр (истина = это означает  первый раз, ложь = не первый)
    private boolean mIsAppFirstLaunched = true;

    // Создать переменную, чтобы проверить первое видео
    private boolean mIsFirstVideo = true;

    // Interface, activity that use FragmentChannelVIdeo must implement onVideoSelected method
    // Интерфейс, основной вид деятельности, которые используют FragmentChannelVIdeo должны реализовать метод onVideoSelected
    public interface OnVideoSelectedListener {
        public void onVideoSelected(String ID);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        View view = inflater.inflate(R.layout.fragment_video_list, container, false);
        setHasOptionsMenu(true);
        // Получить данные Bundle
        Bundle bundle = this.getArguments();

        //Получить данные из ActivityVideo
        mVideoType = Integer.parseInt(bundle.getString(Utils.TAG_VIDEO_TYPE));
        mChannelId = bundle.getString(Utils.TAG_CHANNEL_ID);

        // Подключение view объектов и просматривать идентификаторы из XML
        mUltimateRecyclerView       = (UltimateRecyclerView)
                view.findViewById(R.id.ultimate_recycler_view);
        mLblNoResult                = (TextView) view.findViewById(R.id.lblNoResult);
        mLytRetry                   = (LinearLayout) view.findViewById(R.id.lytRetry);
        mPrgLoading                 = (CircleProgressBar) view.findViewById(R.id.prgLoading);
        AppCompatButton btnRetry    = (AppCompatButton) view.findViewById(R.id.raisedRetry);
        mAdView                     = (AdView) view.findViewById(R.id.adView);

        // Установить  слушателя кнопки btnRetry
        btnRetry.setOnClickListener(this);
        // Установите (circular bar)круговой цвет бар и видимость
        mPrgLoading.setColorSchemeResources(R.color.accent_color);
        mPrgLoading.setVisibility(View.VISIBLE);

        // Установите значение true по умолчанию
        mIsAppFirstLaunched = true;
        mIsFirstVideo = true;

        // Получить AdMob значение видимости
        mIsAdmobVisible = Utils.admobVisibility(mAdView, Utils.IS_ADMOB_VISIBLE);

        // Загрузка  в фоновом режиме, используя класс AsyncTask
        new SyncShowAd(mAdView).execute();

        // Установить ArrayList переменную из videoData
        mVideoData = new ArrayList<>();

        // Set mAdapterList to UltimateRecyclerView object
        mAdapterList = new AdapterList(getActivity(), mVideoData);
        mUltimateRecyclerView.setAdapter(mAdapterList);
        mUltimateRecyclerView.setHasFixedSize(false);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());
        mUltimateRecyclerView.setLayoutManager(linearLayoutManager);
        mUltimateRecyclerView.enableLoadmore();

        // Set layout for custom circular bar when load more
        // Установить макет для  circular bar, когда нужно загрузить больше
        mAdapterList.setCustomLoadMoreView(LayoutInflater.from(getActivity())
                .inflate(R.layout.loadmore_progressbar, null));

        // Listener for handle load more
        // Слушатель для управления дополнительной загрузкой
        mUltimateRecyclerView.setOnLoadMoreListener(new UltimateRecyclerView.OnLoadMoreListener() {
            @Override
            public void loadMore(int itemsCount, final int maxLastVisiblePosition) {
                // если true это означает, данные в сервере до сих пор
                if (mIsStillLoading) {
                    mIsStillLoading = false;
                    // Set layout for custom circular bar when load more.
                    // mAdapter is set again because when load data is response error
                    // setCustomLoadMoreView is null to clear view loading

                    // Установить макет для circular bar, когда нужно загрузить больше.
                    // MAdapter устанавливается снова, потому что, когда нужно загрузить больше ошибка ответа
                    // SetCustomLoadMoreView равна нулю, чтобы очистить view loading
                    mAdapterList.setCustomLoadMoreView(LayoutInflater.from(getActivity())
                            .inflate(R.layout.loadmore_progressbar, null));

                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        public void run() {
                            getVideoData();

                        }
                    }, 1000);
                } else {
                    disableLoadmore();
                }

            }
        });

        // Состояние, когда элемент в списке нажат
        ItemTouchListenerAdapter itemTouchListenerAdapter =
                new ItemTouchListenerAdapter(mUltimateRecyclerView.mRecyclerView,
            new ItemTouchListenerAdapter.RecyclerViewOnItemClickListener() {
                @Override
                public void onItemClick(RecyclerView parent, View clickedView, int position) {
                    // To handle when position  = locationsData.size means loading view is click
                    // Для обработки когда положение = locationsData.size означает loading view нажат
                    if (position < mVideoData.size()) {
                        // Передавать данные onVideoSelected в ActivityVideo
                        mCallback.onVideoSelected(mVideoData.get(position).get(Utils.KEY_VIDEO_ID));
                    }
                }

                @Override
                public void onItemLongClick(RecyclerView recyclerView, View view, int i) {
                }
            });

        // Включить touch listener
        mUltimateRecyclerView.mRecyclerView.addOnItemTouchListener(itemTouchListenerAdapter);

        // Получить данные с сервера в первый раз, когда фрагмент создан
        getVideoData();

        return view;
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        // This makes sure that the container activity has implemented

        // the callback interface. If not, it throws an exception.

        // Это гарантирует, что контейнер activity был осуществлен

        // интерфейс обратного вызова. Если нет, то он вызывает исключение.
        try {
            mCallback = (OnVideoSelectedListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnVideoSelectedListener");
        }
    }

    // AsyncTask класс для загрузки AdMob в фоновом режиме
    public class SyncShowAd extends AsyncTask<Void, Void, Void> {

        final AdView AD;
        AdRequest adRequest;

        public SyncShowAd(AdView AD){
            this.AD = AD;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            // Check ad visibility. If visible, create adRequest

            // Проверка видимости ad. Если видимо, создать AdRequest
            if(mIsAdmobVisible) {
                // Создать запрос объявления(ad)
                if (Utils.IS_ADMOB_IN_DEBUG) {
                    adRequest = new AdRequest.Builder().
                            addTestDevice(AdRequest.DEVICE_ID_EMULATOR).build();
                } else {
                    adRequest = new AdRequest.Builder().build();
                }
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            // Check ad visibility. If visible, display ad banner and interstitial

            // Проверка видимости объявлений. Если видно, дисплей рекламный баннер и интерстициальный
            if(mIsAdmobVisible) {
                // Начать загрузку объявление
                AD.loadAd(adRequest);
            }
        }
    }

    //Метод, чтобы получить видео данные канала YouTube из списка воспроизведения
    private void getVideoData() {

        // Набор mVideoIds пустой, нам нужно идентификатор видео, чтобы получить продолжительность видео
        mVideoIds = "";
        // Create array variable to store first video id of the videos channel

        // Создать переменную массива для хранения первого видео идентификатора,  канала видео
        final String[] videoId = new String[1];

        // Создать переменную для хранения URL использования API YouTube
        String url;
        // Check whether it is channel or playlist (in this case is "Acoustic Covers")

        // Проверьте, является ли  канал или playlist (в данном случае это "Acoustic Covers»)
        if(mVideoType == 2) {
            // Youtube API URL для списка воспроизведения
            url = Utils.API_YOUTUBE + Utils.FUNCTION_PLAYLIST_ITEMS_YOUTUBE +
                    Utils.PARAM_PART_YOUTUBE + "snippet,id&" +
                    Utils.PARAM_FIELD_PLAYLIST_YOUTUBE + "&" +
                    Utils.PARAM_KEY_YOUTUBE + getResources().getString(R.string.youtube_apikey) + "&" +
                    Utils.PARAM_PLAYLIST_ID_YOUTUBE + mChannelId + "&" +
                    Utils.PARAM_PAGE_TOKEN_YOUTUBE + mNextPageToken + "&" +
                    Utils.PARAM_MAX_RESULT_YOUTUBE + Utils.PARAM_RESULT_PER_PAGE;
        }else {
            // Youtube API URL для канала
            url = Utils.API_YOUTUBE + Utils.FUNCTION_SEARCH_YOUTUBE +
                    Utils.PARAM_PART_YOUTUBE + "snippet,id&" + Utils.PARAM_ORDER_YOUTUBE + "&" +
                    Utils.PARAM_TYPE_YOUTUBE + "&" +
                    Utils.PARAM_FIELD_SEARCH_YOUTUBE + "&" +
                    Utils.PARAM_KEY_YOUTUBE + getResources().getString(R.string.youtube_apikey) + "&" +
                    Utils.PARAM_CHANNEL_ID_YOUTUBE + mChannelId + "&" +
                    Utils.PARAM_PAGE_TOKEN_YOUTUBE + mNextPageToken + "&" +
                    Utils.PARAM_MAX_RESULT_YOUTUBE + Utils.PARAM_RESULT_PER_PAGE;
        }

        JsonObjectRequest request = new JsonObjectRequest(url, null,
            new Response.Listener<JSONObject>() {
                JSONArray dataItemArray;
                JSONObject itemIdObject, itemSnippetObject, itemSnippetThumbnailsObject,
                        itemSnippetResourceIdObject;
                @Override
                public void onResponse(JSONObject response) {
                    // Чтобы убедиться, что активность по-прежнему на переднем плане
                    Activity activity = getActivity();
                    if(activity != null && isAdded()){
                        try {
                            // Получить все элементы массива JSon с сервера
                            dataItemArray = response.getJSONArray(Utils.ARRAY_ITEMS);

                            if (dataItemArray.length() > 0) {
                                haveResultView();
                                for (int i = 0; i < dataItemArray.length(); i++) {
                                    HashMap<String, String> dataMap = new HashMap<>();

                                    //Detail Array в Item

                                    //Деталь Массива в Item
                                    JSONObject itemsObject = dataItemArray.getJSONObject(i);
                                    // Array snippet  чтобы получить заголовок и эскизы
                                    itemSnippetObject = itemsObject.
                                            getJSONObject(Utils.OBJECT_ITEMS_SNIPPET);
                                    if(mVideoType == 2){
                                        // Get video ID in playlist

                                        // Получить идентификатор видео в плейлист
                                        itemSnippetResourceIdObject = itemSnippetObject.
                                                getJSONObject(Utils.OBJECT_ITEMS_SNIPPET_RESOURCEID);
                                        dataMap.put(Utils.KEY_VIDEO_ID,
                                                itemSnippetResourceIdObject.
                                                        getString(Utils.KEY_VIDEO_ID));
                                        videoId[0] = itemSnippetResourceIdObject.
                                                getString(Utils.KEY_VIDEO_ID);

                                        // Concat all video IDs and use it as parameter to
                                        // get all video durations.

                                        // Concat всех идентификаторов видео и использовать его в качестве параметра
                                        mVideoIds = mVideoIds + itemSnippetResourceIdObject.
                                                getString(Utils.KEY_VIDEO_ID) + ",";
                                    }else {
                                        // Получить идентификатор видео в канале
                                        itemIdObject = itemsObject.
                                                getJSONObject(Utils.OBJECT_ITEMS_ID);
                                        dataMap.put(Utils.KEY_VIDEO_ID,
                                                itemIdObject.getString(Utils.KEY_VIDEO_ID));
                                        videoId[0] = itemIdObject.getString(Utils.KEY_VIDEO_ID);

                                        // Concat all video IDs and use it as parameter to
                                        // get all video durations.

                                        // Concat всех идентификаторов видео и использовать его в качестве параметра
                                        //  получить все видео длительностей.
                                        mVideoIds = mVideoIds + itemIdObject.
                                                getString(Utils.KEY_VIDEO_ID) + ",";
                                    }

                                    // Когда фрагмент сначала создал первое видео для отображения
                                    // видео-плеер.
                                    if(mIsFirstVideo && i == 0) {
                                        mIsFirstVideo = false;
                                        mCallback.onVideoSelected(videoId[0]);
                                    }

                                    // Get video title

                                    // Получить название видео
                                    dataMap.put(Utils.KEY_TITLE,
                                            itemSnippetObject.getString(Utils.KEY_TITLE));

                                    // Convert ISO 8601 date to string

                                    // Преобразование ISO 8601 даты в строку
                                    String formattedPublishedDate = Utils.formatPublishedDate(
                                            getActivity(),
                                            itemSnippetObject.getString(Utils.KEY_PUBLISHEDAT));

                                    // Получить дату публикации
                                    dataMap.put(Utils.KEY_PUBLISHEDAT, formattedPublishedDate);

                                    // Получить миниатюру видео
                                    itemSnippetThumbnailsObject = itemSnippetObject.
                                            getJSONObject(Utils.OBJECT_ITEMS_SNIPPET_THUMBNAILS);
                                    itemSnippetThumbnailsObject = itemSnippetThumbnailsObject.
                                            getJSONObject
                                                    (Utils.OBJECT_ITEMS_SNIPPET_THUMBNAILS_MEDIUM);
                                    dataMap.put(Utils.KEY_URL_THUMBNAILS,
                                            itemSnippetThumbnailsObject.getString
                                                    (Utils.KEY_URL_THUMBNAILS));

                                    // Видео временного хранения данных, чтобы получить
                                    // продолжительность видео
                                    mTempVideoData.add(dataMap);
                                }

                                // After finish getting video IDs, titles, and thumbnails
                                // now get video duration.

                                // После завершения получения идентификаторов видео, названия
                                // и эскизы, теперь получаем продолжительность видео.
                                getDuration();

                                // Condition if dataItemArray == result perpage it means maybe
                                // server still have data

                                // Условие, если dataItemArray == результат perpage это означает,
                                // что, возможно, на сервере все еще есть данные
                                if (dataItemArray.length() == Utils.PARAM_RESULT_PER_PAGE) {
                                    // To get next page data youtube have parameter Next Page Token

                                    // Чтобы получить следующую информацию Ютюбе есть параметр Next Page Token
                                    mNextPageToken = response.getString(Utils.ARRAY_PAGE_TOKEN);

                                    // No data anymore in this URL

                                    // Нет данных больше в этом URL
                                } else {
                                    // Clear mNextPageToken

                                    // Очищаем mNextPageToken
                                    mNextPageToken = "";
                                    disableLoadmore();
                                }

                                // If success get data, it means next it is not first time again.

                                // Если успешно получили данные, то это означает следующее это
                                // не первый раз.
                                mIsAppFirstLaunched = false;

                                // Data from server already load all or no data in server

                                // Данные сервера уже все загрузились или нет данных в сервере
                            } else {
                                if (mIsAppFirstLaunched &&
                                        mAdapterList.getAdapterItemCount() <= 0) {
                                    noResultView();
                                }
                                disableLoadmore();
                            }

                        } catch (JSONException e) {
                            Log.d(Utils.TAG_PONGODEV + TAG, "JSON Parsing error: " +
                                    e.getMessage());
                            mPrgLoading.setVisibility(View.GONE);
                        }
                        mPrgLoading.setVisibility(View.GONE);
                    }
                }
            },

            new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    // To make sure Activity is still in the foreground
                    // Чтобы убедиться, что активность по-прежнему на переднем плане
                    Activity activity = getActivity();
                    if(activity != null && isAdded()){
                        Log.d(Utils.TAG_PONGODEV + TAG, "on Error Response: " + error.getMessage());
                        // "try-catch" To handle when still in process and then application closed

                        // "try-catch" Для того, чтобы убедится  все еще в процессе,
                        // а затем закрыть приложение
                        try {
                            String msgSnackBar;
                            if (error instanceof NoConnectionError) {
                                msgSnackBar = getResources().getString(R.string.no_internet_connection);
                            } else {
                                msgSnackBar = getResources().getString(R.string.response_error);
                            }

                            // To handle when no data in mAdapter and then get error because no
                            // connection or problem in server.

                            // Чтобы справиться, когда нет данных в адаптер, а затем получить ошибку,
                            // потому что нет связи или проблемы в сервере.
                            if (mVideoData.size() == 0) {
                                retryView();

                                // Condition when loadmore, it have data when loadmore then
                                // get error because no connection.

                                // Состояние, когда loadmore, то есть данные  загружены  затем
                                // получить ошибку, потому что нет связи.
                            } else {
                                mAdapterList.setCustomLoadMoreView(null);
                                mAdapterList.notifyDataSetChanged();
                            }

                            //Utils.showSnackBar(getActivity(), msgSnackBar);
                            mPrgLoading.setVisibility(View.GONE);

                        } catch (Exception e) {
                            Log.d(Utils.TAG_PONGODEV + TAG, "failed catch volley " + e.toString());
                            mPrgLoading.setVisibility(View.GONE);
                        }
                    }
                }
            }
        );
        request.setRetryPolicy(new DefaultRetryPolicy(Utils.ARG_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        MySingleton.getInstance(getActivity()).getRequestQueue().add(request);

    }

    // Method to get duration of the video

    // Метод, чтобы получить продолжительность видео
    private void getDuration() {
        // Youtube API url to get video duration

        // Youtube API URL, чтобы получить продолжительность видео
        String url = Utils.API_YOUTUBE+Utils.FUNCTION_VIDEO_YOUTUBE+
                Utils.PARAM_PART_YOUTUBE+"contentDetails&"+
                Utils.PARAM_FIELD_VIDEO_YOUTUBE+"&"+
                Utils.PARAM_KEY_YOUTUBE+getResources().getString(R.string.youtube_apikey)+"&"+
                Utils.PARAM_VIDEO_ID_YOUTUBE+mVideoIds;

        JsonObjectRequest request = new JsonObjectRequest(url, null,
            new Response.Listener<JSONObject>() {
                JSONArray dataItemArrays;
                JSONObject itemContentObject;
                @Override
                public void onResponse(JSONObject response) {
                    // To make sure Activity is still in the foreground

                    // Чтобы убедиться, что активность по-прежнему на переднем плане
                    Activity activity = getActivity();
                    if(activity != null && isAdded()){
                        try {
                            haveResultView();
                            dataItemArrays = response.getJSONArray(Utils.ARRAY_ITEMS);
                            if (dataItemArrays.length() > 0 && !mTempVideoData.isEmpty()) {
                                for (int i = 0; i < dataItemArrays.length(); i++) {
                                    HashMap<String, String> dataMap = new HashMap<>();

                                    // Detail Array per Item

                                    //  Массив в пункте
                                    JSONObject itemsObjects = dataItemArrays.getJSONObject(i);

                                    // Item to get duration

                                    // Пункт, чтобы получить продолжительность
                                    itemContentObject = itemsObjects.
                                            getJSONObject(Utils.OBJECT_ITEMS_CONTENT_DETAIL);
                                    mDuration         = itemContentObject.
                                            getString(Utils.KEY_DURATION);

                                    // Convert ISO 8601 time to string

                                    // Преобразование ISO 8601 Время в строку
                                    String mDurationInTimeFormat = Utils.
                                            getTimeFromString(mDuration);

                                    // Store titles, video IDs, and thumbnails from mTempVideoData
                                    // to dataMap.

                                    // названия магазина, идентификаторы видео и эскизы из mTempVideoData
                                    // к dataMap.
                                    dataMap.put(Utils.KEY_DURATION, mDurationInTimeFormat);
                                    dataMap.put(Utils.KEY_URL_THUMBNAILS,
                                            mTempVideoData.get(i).get(Utils.KEY_URL_THUMBNAILS));
                                    dataMap.put(Utils.KEY_TITLE,
                                            mTempVideoData.get(i).get(Utils.KEY_TITLE));
                                    dataMap.put(Utils.KEY_VIDEO_ID,
                                            mTempVideoData.get(i).get(Utils.KEY_VIDEO_ID));
                                    dataMap.put(Utils.KEY_PUBLISHEDAT,
                                            mTempVideoData.get(i).get(Utils.KEY_PUBLISHEDAT));

                                    // And store dataMap to videoData

                                    // И магазин dataMap к видеоданными
                                    mVideoData.add(dataMap);

                                    // Insert 1 by 1 to mAdapter

                                    // Вставка 1 на 1 до mAdapter
                                    mAdapterList.notifyItemInserted(mVideoData.size());

                                }
                                mIsStillLoading = true;

                                // Clear mTempVideoData after it done to insert all in videoData

                                // Очистить mTempVideoData после того, как это было сделано,
                                // чтобы вставить все в видеоданные
                                mTempVideoData.clear();
                                mTempVideoData = new ArrayList<>();

                            // Data from server already load all or no data in server

                            // Данные сервера уже все загрузились  или нет данных на сервере
                            }else {
                                if (mIsAppFirstLaunched && mAdapterList.getAdapterItemCount() <= 0)
                                {
                                    noResultView();
                                }
                                disableLoadmore();
                            }

                        } catch (JSONException e) {
                            Log.d(Utils.TAG_PONGODEV + TAG,
                                    "JSON Parsing error: " + e.getMessage());
                            mPrgLoading.setVisibility(View.GONE);
                        }
                        mPrgLoading.setVisibility(View.GONE);
                    }
                }
            },

            new Response.ErrorListener() {

                @Override
                public void onErrorResponse(VolleyError error) {
                    // To make sure Activity is still in the foreground

                    // Чтобы убедиться, что активность по-прежнему на переднем плане
                    Activity activity = getActivity();
                    if(activity != null && isAdded()){
                        Log.d(Utils.TAG_PONGODEV + TAG, "on Error Response: " + error.getMessage());
                        // "try-catch" Для обработки, когда все еще в процессе, а затем приложение закрыто
                        try {
                            String msgSnackBar;
                            if (error instanceof NoConnectionError) {
                                msgSnackBar = getResources().getString(R.string.no_internet_connection);
                            } else {
                                msgSnackBar = getResources().getString(R.string.response_error);
                            }

                            // Чтобы выполнить, когда нет данных в адаптере, а затем получить ошибку, потому что нет
                            // соединение или проблема в сервере.
                            if (mVideoData.size() == 0) {
                                retryView();
                                // Condition when loadmore it has data,
                                // when loadmore then get error because no connection.

                                // Состояние, когда загрузка  имеет данные,
                                // при загрузке  получить ошибку, потому что нет связи.
                            }

                            //Utils.showSnackBar(getActivity(), msgSnackBar);
                            mPrgLoading.setVisibility(View.GONE);

                        } catch (Exception e) {
                            Log.d(Utils.TAG_PONGODEV + TAG, "failed catch volley " + e.toString());
                            mPrgLoading.setVisibility(View.GONE);
                        }
                    }
                }
            }
        );
        request.setRetryPolicy(new DefaultRetryPolicy(Utils.ARG_TIMEOUT_MS,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        MySingleton.getInstance(getActivity()).getRequestQueue().add(request);
    }

    // Method to hide other view and display retry layout

    // Метод, чтобы скрыть другой вид и расположение дисплея от повторных попыток
    private void retryView() {
        mLytRetry.setVisibility(View.VISIBLE);
        mUltimateRecyclerView.setVisibility(View.GONE);
        mLblNoResult.setVisibility(View.GONE);
    }

    // Method to display Recyclerview and hide other view

    // Метод для отображения Recyclerview и скрытия другого вида
    private void haveResultView() {
        mLytRetry.setVisibility(View.GONE);
        mUltimateRecyclerView.setVisibility(View.VISIBLE);
        mLblNoResult.setVisibility(View.GONE);
    }

    // Method to display no result view and hide other view

    // Метод для отображения результата без view и скрыть другой вид
    private void noResultView() {
        mLytRetry.setVisibility(View.GONE);
        mUltimateRecyclerView.setVisibility(View.GONE);
        mLblNoResult.setVisibility(View.VISIBLE);

    }

    // Method to disable loadmore
    // Метод отключения загрузки
    private void disableLoadmore() {
        mIsStillLoading = false;
        if (mUltimateRecyclerView.isLoadMoreEnabled()) {
            mUltimateRecyclerView.disableLoadmore();
        }
        mAdapterList.notifyDataSetChanged();
    }


    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mAdView != null) {
            mAdView.destroy();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mAdView != null) {
            mAdView.resume();
        }
    }

    @Override
    public void onClick(View view) {

        switch (view.getId()){
            case R.id.raisedRetry:
                //Повторно загрузка видеоканала
                mPrgLoading.setVisibility(View.VISIBLE);
                haveResultView();
                getVideoData();
                break;
            default:
                break;
        }
    }
}
