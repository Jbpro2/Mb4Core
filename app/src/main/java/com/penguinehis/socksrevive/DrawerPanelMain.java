package com.penguinehis.socksrevive;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.penguinehis.socksrevive.activities.ConfigGeralActivity;
import com.penguinehis.socksrevive.util.Utils;
import com.google.android.material.navigation.NavigationView;

import me.dawson.proxyserver.ui.ProxySettings;

public class DrawerPanelMain
	implements NavigationView.OnNavigationItemSelectedListener
{
	private AppCompatActivity mActivity;
	
	public DrawerPanelMain(AppCompatActivity activity) {
		mActivity = activity;
	}
	

	private DrawerLayout drawerLayout;
	private ActionBarDrawerToggle toggle;

	public void setDrawer(Toolbar toolbar) {
		NavigationView drawerNavigationView = (NavigationView) mActivity.findViewById(R.id.drawerNavigationView);
		drawerLayout = (DrawerLayout) mActivity.findViewById(R.id.drawerLayoutMain);

		// set drawer
		toggle = new ActionBarDrawerToggle(mActivity,
			drawerLayout, toolbar, R.string.open, R.string.cancel);

        drawerLayout.setDrawerListener(toggle);

		toggle.syncState();

		// set app info
		PackageInfo pinfo = Utils.getAppInfo(mActivity);
		if (pinfo != null) {
			String version_nome = pinfo.versionName;
			int version_code = pinfo.versionCode;
			String header_text = String.format("v. %s (%d)", version_nome, version_code);

			View view = drawerNavigationView.getHeaderView(0);

			TextView app_info_text = view.findViewById(R.id.nav_headerAppVersion);
			app_info_text.setText(header_text);
		}

		// set navigation view
		drawerNavigationView.setNavigationItemSelectedListener(this);
	}
	
	public ActionBarDrawerToggle getToogle() {
		return toggle;
	}
	
	public DrawerLayout getDrawerLayout() {
		return drawerLayout;
	}

	@Override
	public boolean onNavigationItemSelected(@NonNull MenuItem item) {
		int id = item.getItemId();

		if (id == R.id.hostshare) {
			Intent hostshare2 = new Intent(mActivity, ProxySettings.class);
			mActivity.startActivity(hostshare2);
			return true;
		} else if (id == R.id.miSettings) {
			Intent intent = new Intent(mActivity, ConfigGeralActivity.class);
			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mActivity.startActivity(intent);
			return true;
		}else if (id == R.id.miContato) {
			String url1 = "https://t.me/dragoncoresshgp";
			Intent intent4 = new Intent(Intent.ACTION_VIEW, Uri.parse(url1));
			intent4.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			mActivity.startActivity(Intent.createChooser(intent4, mActivity.getText(R.string.open_with)));
			return true;
		}

		return false;
	}

}
