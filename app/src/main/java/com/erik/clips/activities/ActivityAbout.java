package com.erik.clips.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.lb.material_preferences_library.PreferenceActivity;
import com.lb.material_preferences_library.custom_preferences.Preference;
import com.erik.clips.R;

/**
 *
 * Cоздается для отображения информации приложений такой информации как название приложения,
 * версии и имя разработчика. Созданный с использованием PreferenceActivity
 */
public class ActivityAbout extends PreferenceActivity
		implements Preference.OnPreferenceClickListener {

	@Override
	protected void onCreate(final Bundle savedInstanceState)
	{
		// Установить предпочтения темы, вы можете настроить цветовую тему
		// с помощью res/values/styles.xml
		setTheme(R.style.AppTheme_Dark);
		super.onCreate(savedInstanceState);

		// Подключение preference объекта с ключом preference в XML-файле
		Preference prefShareKey      = (Preference) findPreference(getString(R.string.pref_share_key));
		Preference prefRateReviewKey = (Preference) findPreference(getString(R.string.pref_rate_review_key));

		// Установливаем слушателя preference
		prefShareKey.setOnPreferenceClickListener(this);
		prefRateReviewKey.setOnPreferenceClickListener(this);
	}

	@Override
	protected int getPreferencesXmlId()
	{
		// Подключаем preference activity с preference XML
		return R.xml.pref_about;
	}

	@Override
	public boolean onPreferenceClick(android.preference.Preference preference) {
		if(preference.getKey().equals(getString(R.string.pref_share_key))) {
			// Рассказать Google Play URL с помощью других приложений,
			// таких как сообщения, электронная почта, VK и т.д.
			Intent shareIntent = new Intent(Intent.ACTION_SEND);
			shareIntent.setType("text/plain");
			shareIntent.putExtra(Intent.EXTRA_SUBJECT,
					getString(R.string.subject));
			shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.message) +
					" " + getString(R.string.google_play_url));
			startActivity(Intent.createChooser(shareIntent, getString(R.string.share_to)));
		}else if(preference.getKey().equals(getString(R.string.pref_rate_review_key))) {
			// Open App Google Play page so that user can rate and review the app.
			Intent rateReviewIntent = new Intent(Intent.ACTION_VIEW);
			rateReviewIntent.setData(Uri.parse(
					getString(R.string.google_play_url)));
			startActivity(rateReviewIntent);
		}
		return true;
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
		overridePendingTransition(R.anim.open_main, R.anim.close_next);
	}
}
