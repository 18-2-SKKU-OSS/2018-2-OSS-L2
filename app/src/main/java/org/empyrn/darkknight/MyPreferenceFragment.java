package org.empyrn.darkknight;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceManager;


import com.nemesis.materialchess.R;

public class MyPreferenceFragment extends PreferenceFragment 
{
	final CharSequence[] items = {"Red", "Blue", "Purple", "Green", "Orange", "Grey"};
	int choice,theme;
	Context context;
    SharedPreferences.Editor editor;
    SharedPreferences sharedPreferences;
    Preference myPref;
    @Override
    public void onCreate(final Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        context=Preferences.getContextOfApplication();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        editor = sharedPreferences.edit();
        theme = sharedPreferences.getInt("Theme",R.style.AppThemeOrange);
		choice = sharedPreferences.getInt("Choice",4);
		
        
        myPref = (Preference) findPreference("theme");
        myPref.setSummary(items[choice]);
        myPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                     public boolean onPreferenceClick(Preference preference) {
                    	 //open browser or intent here 
                    	 
                    	 Dialog dialog = onCreateDialogSingleChoice();
                    	 dialog.show();
                    	 
                    	 
						return true;
                        
                     }
                 });
    }
    
    
    public Dialog onCreateDialogSingleChoice() {

    	//Initialize the Alert Dialog
        //경고 대화상자 초기화
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
    	//Source of the data in the DIalog
        //대화상자의 데이터 소스
    	

    	// Set the dialog title
        // 대화 상자의 제목을 설정한다.
    	builder.setTitle(R.string.dtitle)
    	// Specify the list array, the items to be selected by default (null for none),
    	// and the listener through which to receive callbacks when items are selected
    	.setSingleChoiceItems(items, choice, new DialogInterface.OnClickListener() {

    	@Override
    	public void onClick(DialogInterface dialog, int which) {
    	// TODO Auto-generated method stub
            myPref.setSummary(items[which]);
            switch(which)
            {
                case 0:
                    editor.putInt("Theme",R.style.AppThemeRed);
                    editor.putInt("Choice",which);
                    editor.commit();
                    break;
                case 1:
                    editor.putInt("Theme",R.style.AppThemeBlue);
                    editor.putInt("Choice",which);
                    editor.commit();
                    break;
                case 2:
                    editor.putInt("Theme",R.style.AppThemePurple);
                    editor.putInt("Choice",which);
                    editor.commit();
                    break;
                case 3:
                    editor.putInt("Theme",R.style.AppThemeGreen);
                    editor.putInt("Choice",which);
                    editor.commit();
                    break;
                case 4:
                    editor.putInt("Theme",R.style.AppThemeOrange);
                    editor.putInt("Choice",which);
                    editor.commit();
                    break;
                case 5:
                    editor.putInt("Theme",R.style.AppThemeGrey);
                    editor.putInt("Choice",which);
                    editor.commit();
                    break;
            }

    	}
    	})

    	// Set the action buttons
        // action 버튼을 설정한다.
    	.setPositiveButton(R.string.dok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK, so save the result somewhere
                // or return them to the component that opened the dialog
                // 유저가 OK를 클릭하면 결과를 어딘가에 저장한다.
                // 혹은 대화로그를 연 컴포넌트에 반환할 수 있다.
                //Preferences.restart();
                //theme = sharedPreferences.getInt("Theme", R.style.AppThemeRed);
                startActivity(new Intent(context, Preferences.class));
            }
        })
    	.setNegativeButton(R.string.dcancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {

            }
        });

    	return builder.create();
    	}
    
}
