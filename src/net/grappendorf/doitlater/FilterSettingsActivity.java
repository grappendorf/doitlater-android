package net.grappendorf.doitlater;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.CheckBox;

public class FilterSettingsActivity extends Activity
{
	private SharedPreferences preferences;

	private CheckBox showCompleted;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.filter_settings);
		preferences = PreferenceManager.getDefaultSharedPreferences(this);
		showCompleted = (CheckBox) findViewById(R.id.show_completed);
		showCompleted.setChecked(preferences.getBoolean("show_completed", false));
	}

	public void onApply(@SuppressWarnings("unused") View source)
	{
		SharedPreferences.Editor prefsEdit = preferences.edit();
		prefsEdit.putBoolean("show_completed", showCompleted.isChecked());
		prefsEdit.commit();
		setResult(RESULT_OK);
		finish();
	}

	public void onCancel(@SuppressWarnings("unused") View source)
	{
		setResult(RESULT_CANCELED);
		finish();
	}
}