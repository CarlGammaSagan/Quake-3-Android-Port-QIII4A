/*
	Copyright (C) 2012 n0n3m4
	
    This file is part of QIII4A.

    QIII4A is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 2 of the License, or
    (at your option) any later version.

    QIII4A is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with RTCW4A.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.n0n3m4.QIII4A;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UTFDataFormatException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.RandomAccess;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import com.n0n3m4.q3e.Q3EInterface;
import com.n0n3m4.q3e.Q3EJNI;
import com.n0n3m4.q3e.Q3EKeyCodes;
import com.n0n3m4.q3e.Q3EMain;
import com.n0n3m4.q3e.Q3EUiConfig;
import com.n0n3m4.q3e.Q3EUtils;
import com.n0n3m4.QIII4A.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TabHost;
import android.widget.TabHost.TabContentFactory;

public class GameLauncher extends Activity{		
	
	final String default_gamedata="/sdcard/qiii4a";
	
	//OPENARENA DOWNLOADING
	ProgressDialog mProgressDialog;
	boolean canceled=false;
	
	private class DownloadFile extends AsyncTask<String, Integer, String> {
		
		public String dir;
		
	    @Override
	    protected String doInBackground(String... sUrl) {
	        try {
	            URL url = new URL(sUrl[0]);
	            URLConnection connection = url.openConnection();
	            connection.connect();
	            int fileLength = connection.getContentLength();
	            InputStream input = new BufferedInputStream(url.openStream());
	            dir=sUrl[1];
	            new File(dir).mkdirs();
	            OutputStream output = new FileOutputStream(dir+"tmp.zip");

	            byte data[] = new byte[1024];
	            long total = 0;
	            int count;
	            while ((count = input.read(data)) != -1) {
	            	if (canceled)
	            	{
	            		output.close();
	            		input.close();
	            		return null;
	            	}
	                total += count;
	                publishProgress((int) (total * 100 / fileLength));
	                output.write(data, 0, count);	                
	            }

	            output.flush();
	            output.close();
	            input.close();
	        } catch (Exception e) {
	        }
	        return null;
	    }
	    
	    @Override
	    protected void onPreExecute() {
	        super.onPreExecute();
	        canceled=false;
	        mProgressDialog = new ProgressDialog(GameLauncher.this);
	        mProgressDialog.setMessage("Downloading OpenArena...");
	        mProgressDialog.setIndeterminate(false);
	        mProgressDialog.setMax(100);
	        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	        mProgressDialog.setCancelable(false);
	        mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					canceled=true;
				}
			});
	        mProgressDialog.show();
	    }
	    
	    @Override
	    protected void onPostExecute(String result) {
	    	mProgressDialog.dismiss();
	    	if (!canceled)
	    	new UnpackFiles().execute(dir+"tmp.zip",dir);
	    	super.onPostExecute(result);
	    }

	    @Override
	    protected void onProgressUpdate(Integer... progress) {
	        super.onProgressUpdate(progress);
	        mProgressDialog.setProgress(progress[0]);
	    }
	}	
	
	private class UnpackFiles extends AsyncTask<String, Integer, String> {
	    @Override
	    protected String doInBackground(String... args) {
	        try {	        	
	            ZipInputStream input = new ZipInputStream(new FileInputStream(args[0]));
	            String dir=args[1];
	            
	            ZipEntry ze;
	            int pr=0;
	            (new File(dir)).mkdirs();
	            while ((ze=input.getNextEntry())!=null)
	            {	            	
	            	if (ze.getName().endsWith(".pk3"))
	            	{
	            		String name=ze.getName();
	            		if (name.contains("baseoa"))
	            		{
	            		name=name.substring(name.indexOf("baseoa"));
	            		new File(dir+"baseoa").mkdirs();
	            		}
	            		if (name.contains("missionpack"))
	            		{
	            		name=name.substring(name.indexOf("missionpack"));
	            		new File(dir+"missionpack").mkdirs();
	            		}
	            		FileOutputStream output=new FileOutputStream(dir+name);
	            		byte data[] = new byte[1024];	    	            
	    	            int count;
	    	            while ((count = input.read(data)) != -1) {
	    	            	if (canceled)
	    	            	{
	    	            		output.close();
	    	            		input.close();
	    	            		return null;
	    	            	}	    	                
	    	                output.write(data, 0, count);	                
	    	            }
	    	            output.flush();
	    	            output.close();
	    	            publishProgress(pr++);
	            	}
	            	input.closeEntry();
	            }
	            
	            input.close();
	            new File(args[0]).delete();
	        } catch (Exception e) {
	        }
	        return null;
	    }
	    
	    @Override
	    protected void onPreExecute() {
	        super.onPreExecute();
	        canceled=false;
	        mProgressDialog = new ProgressDialog(GameLauncher.this);
	        mProgressDialog.setMessage("Unpacking OpenArena...");
	        mProgressDialog.setIndeterminate(false);
	        mProgressDialog.setMax(10);
	        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
	        mProgressDialog.setCancelable(false);
	        mProgressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					canceled=true;
				}
			});
	        mProgressDialog.show();
	    }
	    
	    @Override
	    protected void onPostExecute(String result) {
	    	mProgressDialog.dismiss();
	    	if (!canceled)
	    	{
	    	if (!((EditText)findViewById(R.id.edt_cmdline)).getText().toString().contains("+set com_basegame"))
	    	((EditText)findViewById(R.id.edt_cmdline)).setText(((EditText)findViewById(R.id.edt_cmdline)).getText()+" +set com_basegame baseoa");
	    	}
	    	super.onPostExecute(result);
	    }

	    @Override
	    protected void onProgressUpdate(Integer... progress) {
	        super.onProgressUpdate(progress);
	        mProgressDialog.setProgress(progress[0]);
	    }
	}
	
	
	public void install_oa(View v)
	{
		DownloadFile df=new DownloadFile();
		df.execute("http://www.openarena.ws/request.php?4",((EditText)findViewById(R.id.edt_path)).getText().toString()+"/");
	}
	
	//END
	
	final int UI_JOYSTICK=0;
	final int UI_SHOOT=1;
	final int UI_JUMP=2;
	final int UI_CROUCH=3;
	final int UI_WPNBAR=4;	
	final int UI_ACTION=5;	
	final int UI_PAUSE=6;	
	final int UI_SCORE=7;
	final int UI_1=8;
	final int UI_2=9;
	final int UI_3=10;	
	final int UI_KBD=11;
	final int UI_SIZE=UI_KBD+1;	
	
	
	
	
	public void InitQ3E(Context cnt, int width, int height)
	{					
		Q3EKeyCodes.InitQ3Keycodes();		
		Q3EInterface q3ei=new Q3EInterface();
		q3ei.isQ3=true;		
		q3ei.UI_SIZE=UI_SIZE;
		
		int r=Q3EUtils.dip2px(cnt,75);
		int rightoffset=r*3/4;
		int sliders_width=Q3EUtils.dip2px(cnt,125);
				
		q3ei.defaults_table=new String[UI_SIZE];
		q3ei.defaults_table[UI_JOYSTICK] =(r*4/3)+" "+(height-r*4/3)+" "+r+" "+30;
		q3ei.defaults_table[UI_SHOOT]    =(width-r/2-rightoffset)+" "+(height-r/2-rightoffset)+" "+r*3/2+" "+30;
		q3ei.defaults_table[UI_JUMP]     =(width-r/2)+" "+(height-r-2*rightoffset)+" "+r+" "+30;
		q3ei.defaults_table[UI_CROUCH]   =(width-r/2)+" "+(height-r/2)+" "+r+" "+30;
		q3ei.defaults_table[UI_WPNBAR]=(width-sliders_width/2-rightoffset/3)+" "+(sliders_width*3/8)+" "+sliders_width+" "+30;		
		q3ei.defaults_table[UI_ACTION]   =(width-r-2*rightoffset)+" "+(height-r/2)+" "+r+" "+30;		
		q3ei.defaults_table[UI_PAUSE]     =r*3/4+" "+r*3/4+" "+r*3/2+" "+30;
		q3ei.defaults_table[UI_SCORE]   =(width-2*r-2*rightoffset)+" "+(height-r/2)+" "+r+" "+30;
		
		for (int i=UI_SCORE+1;i<UI_SIZE;i++)
			q3ei.defaults_table[i]=(r/2+r*(i-UI_SCORE-1))+" "+(height+r/2*3/2)+" "+r+" "+30;
		
		q3ei.arg_table=new int[UI_SIZE*4];
		q3ei.type_table=new int[UI_SIZE];		
		
		q3ei.type_table[UI_JOYSTICK]=Q3EUtils.TYPE_JOYSTICK;
		q3ei.type_table[UI_SHOOT]=Q3EUtils.TYPE_BUTTON;
		q3ei.type_table[UI_JUMP]=Q3EUtils.TYPE_BUTTON;
		q3ei.type_table[UI_CROUCH]=Q3EUtils.TYPE_BUTTON;
		q3ei.type_table[UI_WPNBAR]=Q3EUtils.TYPE_SLIDER;		
		q3ei.type_table[UI_ACTION]=Q3EUtils.TYPE_BUTTON;		
		q3ei.type_table[UI_PAUSE]=Q3EUtils.TYPE_BUTTON;
		for (int i=UI_PAUSE+1;i<UI_SIZE;i++)
		q3ei.type_table[i]=Q3EUtils.TYPE_BUTTON;		
		
		q3ei.arg_table[UI_SHOOT*4]=Q3EKeyCodes.KeyCodes.K_MOUSE1;
		q3ei.arg_table[UI_SHOOT*4+1]=0;
		q3ei.arg_table[UI_SHOOT*4+2]=0;
		q3ei.arg_table[UI_SHOOT*4+3]=0;
		
		
		q3ei.arg_table[UI_JUMP*4]=Q3EKeyCodes.KeyCodes.K_SPACE;
		q3ei.arg_table[UI_JUMP*4+1]=0;
		q3ei.arg_table[UI_JUMP*4+2]=0;
		q3ei.arg_table[UI_JUMP*4+3]=0;
		
		q3ei.arg_table[UI_CROUCH*4]=99;
		q3ei.arg_table[UI_CROUCH*4+1]=1;
		q3ei.arg_table[UI_CROUCH*4+2]=1;
		q3ei.arg_table[UI_CROUCH*4+3]=0;
		
		q3ei.arg_table[UI_WPNBAR*4]=91;
		q3ei.arg_table[UI_WPNBAR*4+1]=114;
		q3ei.arg_table[UI_WPNBAR*4+2]=93;
		q3ei.arg_table[UI_WPNBAR*4+3]=0;
		
		q3ei.arg_table[UI_ACTION*4]=Q3EKeyCodes.KeyCodes.K_ENTER;
		q3ei.arg_table[UI_ACTION*4+1]=0;
		q3ei.arg_table[UI_ACTION*4+2]=0;
		q3ei.arg_table[UI_ACTION*4+3]=0;				
		
		q3ei.arg_table[UI_PAUSE*4]=27;
		q3ei.arg_table[UI_PAUSE*4+1]=0;
		q3ei.arg_table[UI_PAUSE*4+2]=3;
		q3ei.arg_table[UI_PAUSE*4+3]=0;
		
		q3ei.arg_table[UI_SCORE*4]=Q3EKeyCodes.KeyCodes.K_TAB;
		q3ei.arg_table[UI_SCORE*4+1]=0;
		q3ei.arg_table[UI_SCORE*4+2]=0;
		q3ei.arg_table[UI_SCORE*4+3]=0;
		
		q3ei.arg_table[UI_1*4]=49;
		q3ei.arg_table[UI_1*4+1]=0;
		q3ei.arg_table[UI_1*4+2]=0;
		q3ei.arg_table[UI_1*4+3]=0;
		
		q3ei.arg_table[UI_2*4]=50;
		q3ei.arg_table[UI_2*4+1]=0;
		q3ei.arg_table[UI_2*4+2]=0;
		q3ei.arg_table[UI_2*4+3]=0;
		
		q3ei.arg_table[UI_3*4]=51;
		q3ei.arg_table[UI_3*4+1]=0;
		q3ei.arg_table[UI_3*4+2]=0;
		q3ei.arg_table[UI_3*4+3]=0;
		
		q3ei.arg_table[UI_KBD*4]=Q3EUtils.K_VKBD;
		q3ei.arg_table[UI_KBD*4+1]=0;
		q3ei.arg_table[UI_KBD*4+2]=0;
		q3ei.arg_table[UI_KBD*4+3]=0;
		
		q3ei.default_path=default_gamedata;
		if (Q3EJNI.detectNeon())
		q3ei.libname="libq3_neon.so";
		else
		q3ei.libname="libq3.so";
		
		q3ei.texture_table=new String[UI_SIZE];
		q3ei.texture_table[UI_JOYSTICK]="";
		q3ei.texture_table[UI_SHOOT]="btn_sht.png";
		q3ei.texture_table[UI_JUMP]="btn_jump.png";
		q3ei.texture_table[UI_CROUCH]="btn_crouch.png";
		q3ei.texture_table[UI_WPNBAR]="slider_wpn.png";		
		q3ei.texture_table[UI_ACTION]="btn_use.png";		
		q3ei.texture_table[UI_PAUSE]="btn_pause.png";
		q3ei.texture_table[UI_SCORE]="btn_score.png";
		q3ei.texture_table[UI_1]="btn_1.png";
		q3ei.texture_table[UI_2]="btn_2.png";
		q3ei.texture_table[UI_3]="btn_3.png";
		q3ei.texture_table[UI_KBD]="btn_keyboard.png";
		
		Q3EUtils.q3ei=q3ei;
	}
	
	public void DonateDialog()
	{
		AlertDialog.Builder bldr=new AlertDialog.Builder(this);
		bldr.setTitle("Do you want to support the developer?");
		bldr.setPositiveButton("Donate by PayPal", new AlertDialog.OnClickListener() {			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent ppIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=kosleb1169%40gmail%2ecom&lc=US&item_name=n0n3m4&no_note=0&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donate_SM%2egif%3aNonHostedGuest"));
				ppIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				startActivity(ppIntent);				
				dialog.dismiss();
			}
		});
		bldr.setNegativeButton("Don't ask", new AlertDialog.OnClickListener() {			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
			}
		});
		bldr.setNeutralButton("More apps by n0n3m4", new AlertDialog.OnClickListener() {			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=pub:n0n3m4"));
				marketIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
				startActivity(marketIntent);				
				dialog.dismiss();
			}
		});
		AlertDialog dl=bldr.create();
		dl.setCancelable(false);
		dl.show();		
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		Q3EUtils.LoadAds(this);
		super.onConfigurationChanged(newConfig);
	}
	
	public void support(View vw)
	{
		DonateDialog();
	}
	
	public void UpdateMouseMenu(boolean show)
	{
		((LinearLayout)findViewById(R.id.layout_mouseconfig)).setVisibility(show?LinearLayout.VISIBLE:LinearLayout.GONE);
	}
	
	public void UpdateMouseManualMenu(boolean show)
	{
		((LinearLayout)findViewById(R.id.layout_manualmouseconfig)).setVisibility(show?LinearLayout.VISIBLE:LinearLayout.GONE);
	}
	
	public void SelectCheckbox(int cbid,int id)
	{
		RadioGroup rg=(RadioGroup)findViewById(cbid);
		rg.check(rg.getChildAt(id).getId());
	}
	
	public int GetIdCheckbox(int cbid)
	{
		RadioGroup rg=(RadioGroup)findViewById(cbid);
		return rg.indexOfChild(findViewById(rg.getCheckedRadioButtonId()));
	}
	
	public void onCreate(Bundle savedInstanceState) 
	{		
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		super.onCreate(savedInstanceState);				
		setContentView(R.layout.main);
		
		Display display = getWindowManager().getDefaultDisplay(); 
		int width = Math.max(display.getWidth(),display.getHeight());
		int height = Math.min(display.getWidth(),display.getHeight());						
		
		InitQ3E(this, width, height);
		
		TabHost th=(TabHost)findViewById(R.id.tabhost);
		th.setup();					
	    th.addTab(th.newTabSpec("tab1").setIndicator("General").setContent(R.id.launcher_tab1));	    
	    th.addTab(th.newTabSpec("tab2").setIndicator("Controls").setContent(R.id.launcher_tab2));	    
	    th.addTab(th.newTabSpec("tab3").setIndicator("Graphics").setContent(R.id.launcher_tab3));		
		SharedPreferences mPrefs=PreferenceManager.getDefaultSharedPreferences(this);				
		
		((EditText)findViewById(R.id.edt_cmdline)).setText(mPrefs.getString(Q3EUtils.pref_params, "ioquake3.arm"));
		((EditText)findViewById(R.id.edt_mouse)).setText(mPrefs.getString(Q3EUtils.pref_eventdev, "/dev/input/event???"));
		((EditText)findViewById(R.id.edt_path)).setText(mPrefs.getString(Q3EUtils.pref_datapath, default_gamedata));
		((CheckBox)findViewById(R.id.hideonscr)).setOnCheckedChangeListener(new OnCheckedChangeListener() {			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				UpdateMouseMenu(isChecked);
			}
		});
		((CheckBox)findViewById(R.id.hideonscr)).setChecked(mPrefs.getBoolean(Q3EUtils.pref_hideonscr, false));
		
		UpdateMouseMenu(((CheckBox)findViewById(R.id.hideonscr)).isChecked());
		
		((CheckBox)findViewById(R.id.gfx_32bit)).setChecked(mPrefs.getBoolean(Q3EUtils.pref_32bit, false));
		((CheckBox)findViewById(R.id.mapvol)).setChecked(mPrefs.getBoolean(Q3EUtils.pref_mapvol, false));
		((CheckBox)findViewById(R.id.secfinglmb)).setChecked(mPrefs.getBoolean(Q3EUtils.pref_2fingerlmb, false));
		((CheckBox)findViewById(R.id.smoothjoy)).setChecked(mPrefs.getBoolean(Q3EUtils.pref_analog, true));
		((CheckBox)findViewById(R.id.detectmouse)).setOnCheckedChangeListener(new OnCheckedChangeListener() {			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				UpdateMouseManualMenu(!isChecked);
			}
		});						
		((CheckBox)findViewById(R.id.detectmouse)).setChecked(mPrefs.getBoolean(Q3EUtils.pref_detectmouse, true));
		
		UpdateMouseManualMenu(!((CheckBox)findViewById(R.id.detectmouse)).isChecked());
		
		SelectCheckbox(R.id.rg_curpos,mPrefs.getInt(Q3EUtils.pref_mousepos, 3));
		SelectCheckbox(R.id.rg_scrres,mPrefs.getInt(Q3EUtils.pref_scrres, 0));
		SelectCheckbox(R.id.rg_msaa,mPrefs.getInt(Q3EUtils.pref_msaa, 0));
		
		((EditText)findViewById(R.id.res_x)).setText(mPrefs.getString(Q3EUtils.pref_resx, "640"));
		((EditText)findViewById(R.id.res_y)).setText(mPrefs.getString(Q3EUtils.pref_resy, "480"));			
		
		Q3EUtils.LoadAds(this);
	}
	
	public void start(View vw)
	{
		Editor mEdtr=PreferenceManager.getDefaultSharedPreferences(this).edit();
		mEdtr.putString(Q3EUtils.pref_params, ((EditText)findViewById(R.id.edt_cmdline)).getText().toString());
		mEdtr.putString(Q3EUtils.pref_eventdev, ((EditText)findViewById(R.id.edt_mouse)).getText().toString());
		mEdtr.putString(Q3EUtils.pref_datapath, ((EditText)findViewById(R.id.edt_path)).getText().toString());
		mEdtr.putBoolean(Q3EUtils.pref_hideonscr, ((CheckBox)findViewById(R.id.hideonscr)).isChecked());
		mEdtr.putBoolean(Q3EUtils.pref_32bit, ((CheckBox)findViewById(R.id.gfx_32bit)).isChecked());
		mEdtr.putBoolean(Q3EUtils.pref_mapvol, ((CheckBox)findViewById(R.id.mapvol)).isChecked());
		mEdtr.putBoolean(Q3EUtils.pref_analog, ((CheckBox)findViewById(R.id.smoothjoy)).isChecked());
		mEdtr.putBoolean(Q3EUtils.pref_2fingerlmb, ((CheckBox)findViewById(R.id.secfinglmb)).isChecked());
		mEdtr.putBoolean(Q3EUtils.pref_detectmouse, ((CheckBox)findViewById(R.id.detectmouse)).isChecked());
		mEdtr.putInt(Q3EUtils.pref_mousepos, GetIdCheckbox(R.id.rg_curpos));
		mEdtr.putInt(Q3EUtils.pref_scrres, GetIdCheckbox(R.id.rg_scrres));
		mEdtr.putInt(Q3EUtils.pref_msaa, GetIdCheckbox(R.id.rg_msaa));
		mEdtr.putString(Q3EUtils.pref_resx, ((EditText)findViewById(R.id.res_x)).getText().toString());
		mEdtr.putString(Q3EUtils.pref_resy, ((EditText)findViewById(R.id.res_y)).getText().toString());
		mEdtr.commit();
		finish();
		startActivity(new Intent(this,Q3EMain.class));		
	}
	
	public void resetcontrols(View vw)
	{
		Editor mEdtr=PreferenceManager.getDefaultSharedPreferences(this).edit();
		for (int i=0;i<UI_SIZE;i++)
			mEdtr.putString(Q3EUtils.pref_controlprefix+i, null);
		mEdtr.commit();
	}
	
	public void controls(View vw)
	{		
		startActivity(new Intent(this,Q3EUiConfig.class));
	}
}
