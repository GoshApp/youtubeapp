/*
 * Copyright 2012 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.erik.clips.activities;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeApiServiceUtil;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer.OnFullscreenListener;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.erik.clips.R;
import com.erik.clips.fragments.FragmentChannelVideos;
import com.erik.clips.fragments.FragmentNewVideos;
import com.erik.clips.fragments.FragmentVideo;
import com.erik.clips.utils.Utils;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * A sample Activity showing how to manage multiple YouTubeThumbnailViews in an adapter for display
 *
 * Образец активности, показывающий, как управлять несколькими YouTubeThumbnailViews в адаптер для отображения
 * в списке. Когда элементы списка нажат, видео воспроизводится с помощью приложения.
 *
 * Демо поддерживает пользовательский полный экран и переходы между книжной и альбомной ориентацией без буферизации.
 */
@TargetApi(13)
public final class ActivityVideo extends FragmentActivity implements
       OnFullscreenListener,
     FragmentChannelVideos.OnVideoSelectedListener,
        FragmentNewVideos.OnVideoSelectedListener{

  /** Отступы между списком видео листом и видео в альбомной ориентации. */
  private static final int LANDSCAPE_VIDEO_PADDING_DP = 5;
  /** Запрос кода при вызове startActivityForResult, чтобы избежать  ошибки службы API.*/
  private static final int RECOVERY_DIALOG_REQUEST = 1;
  // Создать объект FragmentVideo
  private FragmentVideo mFragmentVideo;
  // Создать переменную для обработки полного экрана
  private boolean isFullscreen;

  // Создание view objects
  private Drawer mDrawer = null;
  private Toolbar mToolbar;

  // Создание переменных для сохранения каналов и списков воспроизведения данных
  private String[] mChannelNames;
  private String[] mVideoTypes;
  private String[] mChannelIds;

  // Установить по умолчанию выбранный drawer item
  private int mSelectedDrawerItem = 0;

  private Fragment mFragment;
  private FrameLayout frmLayoutList;
    
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_video);

    // Подключение объектов view с view идентификаторами в XML
    frmLayoutList  = (FrameLayout) findViewById(R.id.fragment_container);
    mToolbar       = (Toolbar) findViewById(R.id.toolbar);

    // Установить объект FragmentVideo
    mFragmentVideo =
        (FragmentVideo) getFragmentManager().findFragmentById(R.id.video_fragment_container);

    // Получить данные из каналов strings.xml
    mChannelNames  = getResources().getStringArray(R.array.channel_names);
    mVideoTypes    = getResources().getStringArray(R.array.video_types);
    mChannelIds    = getResources().getStringArray(R.array.channel_ids);

    // Проверить YouTube API
    checkYouTubeApi();

      // Установить количество объектов PrimaryDrawerItem в зависимости от количества каналов и списков воспроизведения данных
      PrimaryDrawerItem[] mPrimaryDrawerItem = new PrimaryDrawerItem[mChannelIds.length];

      // Установить объект PrimaryDrawerItem для каждого канала данных
      for(int i = 0; i < mChannelIds.length; i++) {
          mPrimaryDrawerItem[i] = new PrimaryDrawerItem()
                  .withName(mChannelNames[i])
                  .withIdentifier(i)
                  .withSelectable(false);

      }

      // Создание drawer menu
      mDrawer = new DrawerBuilder(this)
          .withActivity(ActivityVideo.this)
          .withToolbar(mToolbar)
          .withRootView(R.id.drawer_container)
          .withActionBarDrawerToggleAnimated(true)
          .withSavedInstance(savedInstanceState)
                  // Add menu items to the drawer
          .addDrawerItems(
                  mPrimaryDrawerItem
          )
          .addStickyDrawerItems(
                  new SecondaryDrawerItem()
                          .withName(getString(R.string.about))
                          .withIdentifier(mChannelIds.length-1)
                          .withSelectable(false)
          )
          .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {

              @Override
              public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                  // Проверка, что drawerItem установлен.
                  // Есть разные причины по которым drawerItem может быть нулевым
                  // -> Нажатие на header-заголовок
                  // -> Нажатие на footer
                  // Эти элементы не содержат drawerItem
                  mSelectedDrawerItem = position;
                  if (drawerItem != null) {
                      if (drawerItem.getIdentifier() == 0 && mSelectedDrawerItem != -1) {
                          // Набор инструментов и название выбранного drawer item
                          setToolbarAndSelectedDrawerItem(mChannelNames[0], 0);

                          // Передать все имена каналов и идентификаторы FragmentNewVideos
                          // Для отображения последнего видео для каждого канала и воспроизведения.
                          Bundle bundle = new Bundle();
                          bundle.putStringArray(Utils.TAG_CHANNEL_NAMES, mChannelNames);
                          bundle.putStringArray(Utils.TAG_VIDEO_TYPE, mVideoTypes);
                          bundle.putStringArray(Utils.TAG_CHANNEL_IDS, mChannelIds);

                          // Создать объект FragmentNewVideos
                          mFragment = new FragmentNewVideos();
                          mFragment.setArguments(bundle);

                          // Заменить фрагмент в fragment_container с FragmentNewVideos
                          getSupportFragmentManager().beginTransaction()
                                  .replace(R.id.fragment_container, mFragment)
                                  .commit();

                      } else if (drawerItem.getIdentifier() > 0 && mSelectedDrawerItem != -1) {
                          // Set toolbar title and selected drawer item
                          // Набор инструментов и название выбранного drawer item
                          setToolbarAndSelectedDrawerItem(
                                  mChannelNames[mSelectedDrawerItem],
                                  (mSelectedDrawerItem)
                          );

                          // Pass selected video types and channel ids to FragmentChannelVideos
                          // Пропустить выбранные типы видео и идентификаторы каналов в FragmentChannelVideos
                          Bundle bundle = new Bundle();
                          bundle.putString(Utils.TAG_VIDEO_TYPE,
                                  mVideoTypes[mSelectedDrawerItem]);
                          bundle.putString(Utils.TAG_CHANNEL_ID,
                                  mChannelIds[mSelectedDrawerItem]);

                          // Создать объект FragmentChannelVideos
                          mFragment = new FragmentChannelVideos();
                          mFragment.setArguments(bundle);

                          // Заменить фрагмент в fragment_container с FragmentChannelVideos
                          getSupportFragmentManager().beginTransaction()
                                  .replace(R.id.fragment_container, mFragment)
                                  .commit();
                      } else if (mSelectedDrawerItem == -1) {
                          // Открыть страницу по вызову ActivityAbout.java
                          // Open about page by calling ActivityAbout.java
                          Intent aboutIntent = new Intent(getApplicationContext(),
                                  ActivityAbout.class);
                          startActivity(aboutIntent);
                          overridePendingTransition(R.anim.open_next, R.anim.close_main);
                      }
                  }

                  return false;
              }
          })
          .withSavedInstance(savedInstanceState)
          .withShowDrawerOnFirstLaunch(true)
          .build();

    // Set toolbar title and selected drawer item with first data in default
    // Установить  toolbar title  и выбранный drawer item с первыми данными по умолчанию
    setToolbarAndSelectedDrawerItem(mChannelNames[0], 0);

    // In default display FragmentNewVideos first.
    // Pass all channel names and ids to FragmentNewVideos
    // to display the latest video for each channel.

      // FragmentNewVideos отображать по умолчанию в первую очередь.
      // Передать все имена каналов и идентификаторы FragmentNewVideos
      // Для отображения последнего видео для каждого канала.
    Bundle bundle = new Bundle();
    bundle.putStringArray(Utils.TAG_CHANNEL_NAMES, mChannelNames);
    bundle.putStringArray(Utils.TAG_VIDEO_TYPE, mVideoTypes);
    bundle.putStringArray(Utils.TAG_CHANNEL_IDS, mChannelIds);

    // Создание FragmentNewVideos и установить его в качестве фрагмента по умолчанию
    mFragment = new FragmentNewVideos();
    mFragment.setArguments(bundle);

    // Заменить фрагмент в fragment_container с FragmentNewVideos
    getSupportFragmentManager().beginTransaction()
            .replace(R.id.fragment_container, mFragment)
            .commit();

    getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {

      @Override
      public void onBackStackChanged() {
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (f != null) {
          updateTitleAndDrawer(f);
        }

      }
    });

    // Only set the active selection or active profile if we do not recreate the activity
    // Устанавливать только active selection или active profile, если мы не воссоздать деятельность
    if (savedInstanceState == null) {
        // Set the selection to the item with the identifier 10
        // Установить выделение на элемент с идентификатором 10
      mDrawer.setSelection(0, false);
    }
  }

  private void checkYouTubeApi() {
    YouTubeInitializationResult errorReason =
        YouTubeApiServiceUtil.isYouTubeApiServiceAvailable(this);
    if (errorReason.isUserRecoverableError()) {
      errorReason.getErrorDialog(this, RECOVERY_DIALOG_REQUEST).show();
    } else if (errorReason != YouTubeInitializationResult.SUCCESS) {
      String errorMessage =
          String.format(getString(R.string.error_player),
                  errorReason.toString());
      Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
    }
  }

  // Method to set toolbar title and active drawer item base on selected drawer item
    //Метод для установки toolbar title  и активной  drawer item базы на предмет выбранного элемента drawer item
  private void setToolbarAndSelectedDrawerItem(String title, int selectedDrawerItem){
    mToolbar.setTitle(title);
    mDrawer.setSelection(selectedDrawerItem, false);
  }

  // Метод обновления toolbar title
  private void updateTitleAndDrawer (Fragment mFragment){
    String fragClassName = mFragment.getClass().getName();

    if (fragClassName.equals(FragmentNewVideos.class.getName())){
      setToolbarAndSelectedDrawerItem(mChannelNames[0], 0);
    } else if (fragClassName.equals(FragmentChannelVideos.class.getName())){
      setToolbarAndSelectedDrawerItem(mChannelNames[mSelectedDrawerItem ],
              (mSelectedDrawerItem ));
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Накачать меню; это добавляет элементы в action bar, если он присутствует.
    getMenuInflater().inflate(R.menu.activity_video, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.

      // Handle action bar item нажимается здесь. Action bar  будет
      // автоматически обрабатывать клики на кнопку Home/Up, до тех пор,
      // Как вы укажете родительскую активность в AndroidManifest.xml.
    switch (item.getItemId()) {
      case R.id.menuAbout:
        // Открыть about страницу по вызову активности ActivityAbout.java
        Intent aboutIntent = new Intent(getApplicationContext(),
                ActivityAbout.class);
        startActivity(aboutIntent);
        overridePendingTransition(R.anim.open_next, R.anim.close_main);
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == RECOVERY_DIALOG_REQUEST) {
      // Возобновить активность, если пользователь выполнил действия по восстановлению
      recreate();
    }
  }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    layout();
  }

  @Override
  public void onFullscreen(boolean isFullscreen) {
    this.isFullscreen = isFullscreen;
    layout();
  }

  /**
       * Устанавливает макет  программно для трех различных состояний. Портрет, пейзаж или
       * Полноэкранный режим + пейзаж. Это должно быть сделано программно, так как мы обрабатываем ориентацию
       * которая изменяет себя, чтобы получить текушие полноэкранные переходы, поэтому макет XML-ресурсы
       * не перезагружаются.
   */
  private void layout() {
    boolean isPortrait =
            getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;

    if (isFullscreen) {
        mToolbar.setVisibility(View.GONE);
        frmLayoutList.setVisibility(View.GONE);
        setLayoutSize(mFragmentVideo.getView(), MATCH_PARENT, MATCH_PARENT);
    } else if (isPortrait) {
        mToolbar.setVisibility(View.VISIBLE);
        frmLayoutList.setVisibility(View.VISIBLE);
        setLayoutSize(mFragmentVideo.getView(), WRAP_CONTENT, WRAP_CONTENT);
    } else {
        mToolbar.setVisibility(View.VISIBLE);
        frmLayoutList.setVisibility(View.VISIBLE);
        int screenWidth = dpToPx(getResources().getConfiguration().screenWidthDp);
        int videoWidth = screenWidth - screenWidth / 4 - dpToPx(LANDSCAPE_VIDEO_PADDING_DP);
        setLayoutSize(mFragmentVideo.getView(), videoWidth, WRAP_CONTENT);
    }
  }

    @Override
    public void onVideoSelected(String ID) {
    FragmentVideo mFragmentVideo =
            (FragmentVideo) getFragmentManager().findFragmentById(R.id.video_fragment_container);
    mFragmentVideo.setVideoId(ID);
    }

    // Method to convert dp to pixel
    //Метод для преобразования dp в пиксели
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density + 0.5f);
    }

    // Метод, чтобы установить размер макета
    private static void setLayoutSize(View view, int width, int height) {
        LayoutParams params = view.getLayoutParams();
        params.width = width;
        params.height = height;
        view.setLayoutParams(params);
    }

    @Override
    public void onBackPressed() {
        if (isFullscreen){
            mFragmentVideo.backnormal();
            mToolbar.setVisibility(View.VISIBLE);
            frmLayoutList.setVisibility(View.VISIBLE);
            setLayoutSize(mFragmentVideo.getView(), WRAP_CONTENT, WRAP_CONTENT);
        } else{
            super.onBackPressed();
        }
    }
}
