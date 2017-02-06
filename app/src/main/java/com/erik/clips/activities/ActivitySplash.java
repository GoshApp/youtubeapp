package com.erik.clips.activities;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.erik.clips.R;

/**
 * Дизайн и разработка stepByStep.com
 *
 * ActivitySplash создан, чтобы отобразить экран приветствия.
 * Созданный с использованием AppCompatActivity.
 */
public class ActivitySplash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Конфигурация в Android API ниже 21, чтобы установить окно на весь экран.
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_splash);

        // Создать Loading подождать несколько секунд перед отображением ActivityHome
        new Loading().execute();
    }

    // Asynctask класс для обработки загрузки в фоновом режиме
    public class Loading extends AsyncTask<Void, Void, Void>{

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                Thread.sleep(4000);
            }catch(InterruptedException ie){
                ie.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            //Когда загрузка закончена, откройть ActivityHome
            Intent homeIntent = new Intent(getApplicationContext(), ActivityHome.class);
            startActivity(homeIntent);
            overridePendingTransition(R.anim.open_next, R.anim.close_main);
        }
    }

    // Конфигурация в Android API 21, чтобы установить окно на весь экран.
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
            if (hasFocus) {
                getWindow().getDecorView()
                        .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN
                                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            }
        }
    }


}