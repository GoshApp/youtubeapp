package com.erik.clips.adapters;

import android.animation.Animator;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.toolbox.ImageLoader;
import com.marshalchen.ultimaterecyclerview.UltimateRecyclerviewViewHolder;
import com.marshalchen.ultimaterecyclerview.UltimateViewAdapter;
import com.marshalchen.ultimaterecyclerview.animators.internal.ViewHelper;
import com.erik.clips.R;
import com.erik.clips.utils.MySingleton;
import com.erik.clips.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;

/**
 *
 * AdapterList создан для создания видео-элемента в виде списка.
 * Созданный с использованием UltimateViewAdapter.
 *
 */
public class AdapterList extends UltimateViewAdapter<RecyclerView.ViewHolder> {

    // Создание ArrayList для хранения данных
    private final ArrayList<HashMap<String,String>> DATA;
    // Создать объект ImageLoader для обработки загрузки изображения в фоновом режиме
    private final ImageLoader IMAGE_LOADER;

    // Создать объект Interpolator для элемента анимации
    private Interpolator mInterpolator = new LinearInterpolator();

    // Установить последнюю позицию
    private int mLastPosition = 5;
    // Установить продолжительность анимации по умолчанию
    private final int ANIMATION_DURATION = 300;

    public AdapterList(Context context, ArrayList<HashMap<String, String>> list){
        IMAGE_LOADER = MySingleton.getInstance(context).getImageLoader();
        DATA = list;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.adapter_video_list, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

        if (position < getItemCount() && (customHeaderView != null ? position <= DATA.size() :
                position < DATA.size()) && (customHeaderView == null || position > 0)) {
            HashMap<String, String> item;
            item = DATA.get(customHeaderView != null ? position - 1 : position);
            // Set data to the view
            // Набор данных для view
            ((ViewHolder) holder).mTxtTitle.setText(item.get(Utils.KEY_TITLE));
            ((ViewHolder) holder).mTxtDuration.setText(item.get(Utils.KEY_DURATION));
            ((ViewHolder) holder).mTxtPublishedAt.setText(item.get(Utils.KEY_PUBLISHEDAT));

            // Установить изображение ImageView
            IMAGE_LOADER.get(item.get((Utils.KEY_URL_THUMBNAILS)),
                    ImageLoader.getImageListener(((ViewHolder) holder).mImgThumbnail,
                            R.mipmap.empty_photo, R.mipmap.empty_photo));
        }

        boolean isFirstOnly = true;
        if (!isFirstOnly || position > mLastPosition) {
            // Добавить анимацию к элементу
            for (Animator anim : getAdapterAnimations(holder.itemView,
                    AdapterAnimationType.SlideInLeft)) {
                anim.setDuration(ANIMATION_DURATION).start();
                anim.setInterpolator(mInterpolator);
            }
            mLastPosition = position;
        } else {
            ViewHelper.clear(holder.itemView);
        }
    }

    @Override
    public int getAdapterItemCount() {
        return DATA.size();
    }

    @Override
    public RecyclerView.ViewHolder getViewHolder(View view) {
        return new UltimateRecyclerviewViewHolder(view);
    }

    @Override
    public long generateHeaderId(int i) {
        return 0;
    }

    @Override
    public RecyclerView.ViewHolder onCreateHeaderViewHolder(ViewGroup viewGroup) {
        return null;
    }

    @Override
    public void onBindHeaderViewHolder(RecyclerView.ViewHolder viewHolder, int i) {

    }

    public static class ViewHolder extends UltimateRecyclerviewViewHolder {
        // Создание view objects
        private TextView mTxtTitle, mTxtPublishedAt, mTxtDuration;
        private ImageView mImgThumbnail;

        public ViewHolder(View v) {
            super(v);
            mTxtTitle     = (TextView) v.findViewById(R.id.txtTitle);   // заголовок
            mTxtDuration  = (TextView) v.findViewById(R.id.txtDuration); // продолжительность
            mImgThumbnail = (ImageView) v.findViewById(R.id.imgThumbnail); // привью
            mTxtPublishedAt = (TextView) v.findViewById(R.id.txtPublishedAt); // название канала
        }
    }

}
