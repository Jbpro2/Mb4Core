package com.penguinehis.socksrevive;

import static com.penguinehis.ultrasshservice.util.StartDNSTT.CharlieProtect;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.LinkProperties;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.PersistableBundle;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.PreferenceManager;

import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.penguinehis.socksrevive.activities.BaseActivity;
import com.penguinehis.socksrevive.activities.ConfigGeralActivity;
import com.penguinehis.socksrevive.util.KillThis;
import com.penguinehis.socksrevive.util.Utils;
import com.penguinehis.ultrasshservice.SocksReviveService;
import com.penguinehis.ultrasshservice.LaunchVpn;
import com.penguinehis.ultrasshservice.config.ConfigParser;
import com.penguinehis.ultrasshservice.config.Settings;
import com.penguinehis.ultrasshservice.logger.ConnectionStatus;
import com.penguinehis.ultrasshservice.logger.SkStatus;
import com.penguinehis.ultrasshservice.tunnel.TunnelManagerHelper;
import com.penguinehis.ultrasshservice.tunnel.TunnelUtils;
import com.penguinehis.ultrasshservice.util.CustomNativeLoader;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.penguinehis.ultrasshservice.util.StartDNSTT;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Activity Principal
 * @author PenguinEHIS
 */

public class SocksReviveMainActivity extends BaseActivity
		implements DrawerLayout.DrawerListener,
		View.OnClickListener, RadioGroup.OnCheckedChangeListener,
		CompoundButton.OnCheckedChangeListener, SkStatus.StateListener,NetworkStateReceiver.NetworkStateReceiverListener
{
	private static final String TAG = SocksReviveMainActivity.class.getSimpleName();
	private static final String UPDATE_VIEWS = "MainUpdate";
	public static final String OPEN_LOGS = "com.penguinehis.socksrevive:openLogs";
	private static final String DNS_BIN = "libstartdns";
	private DrawerLog mDrawer;
	public static int UPDATE_CODE = 22;
	private DrawerPanelMain mDrawerPanel;
	private Button abrirDialog;
	private Settings mConfig;
	private Toolbar toolbar_main;
	private Handler mHandler;
	private LinearLayout mainLayout;
	private Button starterButton;
	private TextView timerrt;
	private LinearLayout loginLayout;
	private ImageButton inputPwShowPass;
	private String cake2 = "0";
	private String cake3 = "0";
	private TextInputEditText inputPwUser;
	private TextInputEditText inputPwPass;
	private Spinner spinnerCategory, spinnerServer;
	AppUpdateManager appUpdateManager;
	private TextView mTextViewCountDown;
	private Button mButtonSet;
	private Button mButtonStartPause;
	private Button mButtonReset;
	private CountDownTimer mCountDownTimer;
	private boolean mTimerRunning;
	private long mStartTimeInMillis;
	private long mTimeLeftInMillis;
	private long mEndTime;
	private EditText mEditTextInput;
	private boolean isUserInteraction = false;

	private Process dnsProcess;
	private File filedns;
	private List<String> categoryList = new ArrayList<>();
	private List<String> filteredServers = new ArrayList<>();
	private Map<String, List<String>> categoryServerMap = new HashMap<>();

	private SwitchCompat AutoReconnectSwitch;
	private LinearLayout AutoReconnectLayout;
	private AppCompatActivity mActivity;

	private FloatingActionButton logs;

	private ImageView contato;
	private ConnectivityManager connMgr;

	private long mStartRX = 0;
	private long mStartTX = 0;
	private long mStartYX = 0;
	private long mStartUX = 0;
	private NetworkStateReceiver networkStateReceiver;
	private List<String> ListaServidores = new ArrayList<>();
	private List<ServerInfo> serverInfoList = new ArrayList<>();

	private static final String PREF_TIME_LEFT = "time_left_millis";
	private static final String PREF_LAST_TIMESTAMP = "last_saved_timestamp";

	// Timer Reconexão 1
	private void pauseTimer() {
		mCountDownTimer.cancel();
		mTimerRunning = false;
		updateWatchInterface();
	}
	private void resetTimer() {
		mTimeLeftInMillis = mStartTimeInMillis;
		updateCountDownText();
		updateWatchInterface();
	}

	private void updateWatchInterface() {
		if (mTimerRunning) {
			mEditTextInput.setVisibility(View.INVISIBLE);
			mButtonSet.setVisibility(View.INVISIBLE);
			mButtonReset.setVisibility(View.INVISIBLE);
			mButtonStartPause.setText("PAUSAR");
		} else {
			mTextViewCountDown.setVisibility(View.VISIBLE);
			mEditTextInput.setVisibility(View.VISIBLE);
			mButtonSet.setVisibility(View.VISIBLE);
			mButtonStartPause.setText("INICIAR");
			mButtonReset.setText("RESETAR");
			if (mTimeLeftInMillis < 1000) {
				mButtonStartPause.setVisibility(View.INVISIBLE);
				mButtonSet.setVisibility(View.INVISIBLE);
				mEditTextInput.setVisibility(View.INVISIBLE);
				mTextViewCountDown.setVisibility(View.INVISIBLE);
				mButtonReset.setText("PARAR");
			} else {
				mButtonStartPause.setVisibility(View.VISIBLE);
			}
			if (mTimeLeftInMillis < mStartTimeInMillis) {
				mButtonReset.setVisibility(View.VISIBLE);
			} else {
				mButtonReset.setVisibility(View.INVISIBLE);
			}
		}
	}

	private void closeKeyboard() {
		View view = this.getCurrentFocus();
		if (view != null) {
			InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
			imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
		SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putLong("startTimeInMillis", mStartTimeInMillis);
		editor.putLong("millisLeft", mTimeLeftInMillis);
		editor.putBoolean("timerRunning", mTimerRunning);
		editor.putLong("endTime", mEndTime);
		editor.apply();

	}

	@Override
	protected void onStart() {
		super.onStart();
		SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
		mStartTimeInMillis = prefs.getLong("startTimeInMillis", 3600000);
		mTimeLeftInMillis = prefs.getLong("millisLeft", mStartTimeInMillis);
		mTimerRunning = prefs.getBoolean("timerRunning", false);
		updateCountDownText();
		updateWatchInterface();
		if (mTimerRunning) {
			mEndTime = prefs.getLong("endTime", 0);
			mTimeLeftInMillis = mEndTime - System.currentTimeMillis();
			if (mTimeLeftInMillis < 0) {
				mTimeLeftInMillis = 0;
				mTimerRunning = false;
				updateCountDownText();
				updateWatchInterface();
			} else {
				startTimer();
			}
		}
	}

	private void updateCountDownText() {
		int hours = (int) (mTimeLeftInMillis / 1000) / 3600;
		int minutes = (int) ((mTimeLeftInMillis / 1000) % 3600) / 60;
		int seconds = (int) (mTimeLeftInMillis / 1000) % 60;
		String timeLeftFormatted;
		if (hours > 0) {
			timeLeftFormatted = String.format(Locale.getDefault(),
											  "%d:%02d:%02d", hours, minutes, seconds);
		} else {
			timeLeftFormatted = String.format(Locale.getDefault(),
											  "%02d:%02d", minutes, seconds);
		}
		mTextViewCountDown.setText(timeLeftFormatted);
	}

	private void setTime(long milliseconds) {
		mStartTimeInMillis = milliseconds;
		resetTimer();
		closeKeyboard();
	}

	private void startTimer() {
		mEndTime = System.currentTimeMillis() + mTimeLeftInMillis;
		mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, 1000) {


			@Override
			public void onTick(long millisUntilFinished) {
				mTimeLeftInMillis = millisUntilFinished;
				updateCountDownText();
			}
			@Override
			public void onFinish() {
				mTimerRunning = false;
				updateWatchInterface();
				resetTimer();
				startTimer();

				Intent reconTunnel = new Intent(SocksReviveService.TUNNEL_SSH_RESTART_SERVICE);
				LocalBroadcastManager.getInstance(SocksReviveMainActivity.this).sendBroadcast(reconTunnel);

			}
		}.start();
		mTimerRunning = true;
		updateWatchInterface();
	}
 // #####################

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState)
	{

		super.onCreate(savedInstanceState);
		mHandler = new Handler();
		mConfig = new Settings(this);
		mDrawer = new DrawerLog(this);
		mDrawerPanel = new DrawerPanelMain(this);
		connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		SharedPreferences prefs = getSharedPreferences(SocksReviveApp.PREFS_GERAL, Context.MODE_PRIVATE);

		boolean showFirstTime = prefs.getBoolean("connect_first_time", true);
		int lastVersion = prefs.getInt("last_version", 0);

		// se primeira vez
		if (showFirstTime)
		{
			SharedPreferences.Editor pEdit = prefs.edit();
			pEdit.putBoolean("connect_first_time", false);
			pEdit.apply();

			Settings.setDefaultConfig(this);

			showBoasVindas();
		}

		try {
			int idAtual = ConfigParser.getBuildId(this);

			if (lastVersion < idAtual) {
				SharedPreferences.Editor pEdit = prefs.edit();
				pEdit.putInt("last_version", idAtual);
				pEdit.apply();

				// se estiver atualizando
				if (!showFirstTime) {
					if (lastVersion <= 12) {
						Settings.setDefaultConfig(this);
						Settings.clearSettings(this);

						Toast.makeText(this, "As configurações foram limpas para evitar bugs",
								Toast.LENGTH_LONG).show();
					}
				}

			}
		} catch(IOException e) {}

		StartDNSTT.init(this);
		StartDNSTT.CharlieProtect();

		// set layout
		doLayout();
		inAppUp();
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
			requestPermissions(new String[] {android.Manifest.permission.POST_NOTIFICATIONS}, 1);
		} else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
			if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

				requestPermissions(new String[] {android.Manifest.permission.POST_NOTIFICATIONS}, 1);

			}
			else {
				requestPermissions(new String[] {android.Manifest.permission.POST_NOTIFICATIONS}, 1);
			}
		}

		//inAppUp();
		// verifica se existe algum problema
		//SkProtect.CharlieProtect();

		// recebe local dados
		IntentFilter filter = new IntentFilter();
		filter.addAction(UPDATE_VIEWS);
		filter.addAction(OPEN_LOGS);

		LocalBroadcastManager.getInstance(this)
				.registerReceiver(mActivityReceiver, filter);

		doUpdateLayout();

		mStartRX = TrafficStats.getTotalRxBytes();
		mStartTX = TrafficStats.getTotalTxBytes();
		mStartYX = TrafficStats.getTotalRxBytes();
		mStartUX = TrafficStats.getTotalTxBytes();
		if (mStartRX == TrafficStats.UNSUPPORTED || mStartTX == TrafficStats.UNSUPPORTED) {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle("Uh Oh!");
			alert.setMessage("Your device does not support traffic stat monitoring.");
			alert.show();
		} else {
			mHandler.postDelayed(mRunnable, 1000);
		}

		startNetworkBroadcastReceiver(this);
	}


	/**
	 * Layout
	 */

	private void doLayout() {
		CharlieProtect();
		setContentView(R.layout.activity_main_drawer);

		toolbar_main = (Toolbar) findViewById(R.id.toolbar_main);
		mDrawerPanel.setDrawer(toolbar_main);
		setSupportActionBar(toolbar_main);
		spinnerCategory = findViewById(R.id.spinnerCategory);

		mDrawer.setDrawer(this);

		if (!BuildConfig.DEBUG) {
		}

        mEditTextInput=(EditText)findViewById(R.id.time);
		mTextViewCountDown = (TextView) findViewById(R.id.duration);
		mButtonSet = (Button) findViewById(R.id.set);

		mButtonSet.setOnClickListener(new View.OnClickListener() {
				//Vibrator vb = (Vibrator)   getSystemService(Context.VIBRATOR_SERVICE);
				@Override
				public void onClick(View v) {
					String input = mEditTextInput.getText().toString();
					if (input.length() == 0) {
						Toast.makeText((SocksReviveMainActivity.this), "INSIRA UM TEMPO", Toast.LENGTH_SHORT).show();

						return;
					}
					long millisInput = Long.parseLong(input) * 60000;
					if (millisInput == 0) {
						Toast.makeText((SocksReviveMainActivity.this), "INSIRA UM TEMPO VALIDO", Toast.LENGTH_SHORT).show();

						return;
					}
					setTime(millisInput);
					mEditTextInput.setText("");

					startTimer();
				}
			});


		mButtonStartPause = (Button) findViewById(R.id.start);
		mButtonStartPause.setOnClickListener(new View.OnClickListener() {
				//Vibrator vb = (Vibrator)   getSystemService(Context.VIBRATOR_SERVICE);
				@Override
				public void onClick(View v) {
					if (mTimerRunning) {
						pauseTimer();
					} else {
						startTimer();
						Toast.makeText(SocksReviveMainActivity.this, "RECONEXÃO ATIVADA", Toast.LENGTH_SHORT).show();
					}

				}
			});

		mButtonReset = (Button) findViewById(R.id.reset);
		mButtonReset.setOnClickListener(new View.OnClickListener() {
				//Vibrator vb = (Vibrator)   getSystemService(Context.VIBRATOR_SERVICE);

				@Override
				public void onClick(View v) {
					resetTimer();

				}
			});

		mainLayout = (LinearLayout) findViewById(R.id.activity_mainLinearLayout);
		loginLayout = (LinearLayout) findViewById(R.id.activity_mainInputPasswordLayout);
		starterButton = (Button) findViewById(R.id.activity_starterButtonMain);
		timerrt = (TextView) findViewById(R.id.timerrt);

		inputPwUser = (TextInputEditText) findViewById(R.id.activity_mainInputPasswordUserEdit);
		inputPwPass = (TextInputEditText) findViewById(R.id.activity_mainInputPasswordPassEdit);

		inputPwShowPass = (ImageButton) findViewById(R.id.activity_mainInputShowPassImageButton);
		abrirDialog = (Button) findViewById(R.id.cake);

		contato = (ImageView) findViewById(R.id.contato);
		contato.setOnClickListener(this);


		AutoReconnectLayout = (LinearLayout) findViewById(R.id.activity_AutoReconnectLayout);

		// Atual estado do Layout
		AutoReconnectLayout.setVisibility(View.INVISIBLE);

		AutoReconnectSwitch = (SwitchCompat) findViewById(R.id.activity_AutoReconnectSwitch);

         // Atual estado do Switch
        AutoReconnectSwitch.setChecked(false);
		AutoReconnectSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked) {
                       // Quando está marcado

					    AutoReconnectLayout.setVisibility(View.VISIBLE);
					    showAvisoUseTimer();

                     } else {
                        // Quando estiver desmarcado

						if (mTimerRunning) {
							pauseTimer();
							resetTimer();
						}
						//updateWatchInterface();
						AutoReconnectLayout.setVisibility(View.INVISIBLE);
                     }
                }
            });

		abrirDialog.setOnClickListener(this);


		abrirDialog.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {

				// COLOCA O ALERT AQUI

				final SharedPreferences slowprefs = mConfig.getPrefsPrivate();
				AlertDialog.Builder alert = new AlertDialog.Builder(SocksReviveMainActivity.this);
				final SharedPreferences.Editor edit = slowprefs.edit();
				alert.setTitle("Dns Custom");
				alert.setMessage("insira o DNS");


				String currentdns = slowprefs.getString(Settings.SLOW_DNSKEY, "slowdns");

				final EditText input = new EditText(getApplicationContext());
				alert.setView(input);
				input.setText(currentdns);

				alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						String value = input.getText().toString();
						// Do something with value!
						if (input == null || input.getText().toString().isEmpty() || input.equals("0")){


						}else{
							String dnscustom   =  input.getText().toString();

							edit.putString(Settings.SLOW_DNSKEY, dnscustom).apply();
						}
					}
				});

				alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int whichButton) {
						// Canceled.
					}
				});

				alert.show();

			}
		});


		logs = (FloatingActionButton) findViewById(R.id.logs);

		starterButton.setOnClickListener(this);

        logs.setOnClickListener(this);



		inputPwShowPass.setOnClickListener(this);

        //Onde fica salvo as coisas no aparelho
		final SharedPreferences sPrefs = mConfig.getPrefsPrivate();

        //Declarando o xml do spinner



		//Sistema de usuario e senha na tela inicial by @SocksRevive
		final SharedPreferences prefsTxt = mConfig.getPrefsPrivate();
		inputPwUser.setText(prefsTxt.getString(Settings.USUARIO_KEY, ""));
		inputPwPass.setText(prefsTxt.getString(Settings.SENHA_KEY, ""));
		inputPwUser.addTextChangedListener(new TextWatcher() {

			public void afterTextChanged(Editable s) {
				if(!s.toString().isEmpty()) {
					prefsTxt.edit().putString(Settings.USUARIO_KEY, s.toString()).apply();
				}
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			public void onTextChanged(CharSequence s, int start, int before, int count) {}
		});
		inputPwPass.addTextChangedListener(new TextWatcher() {

			public void afterTextChanged(Editable s) {
				if(!s.toString().isEmpty()) {
					prefsTxt.edit().putString(Settings.SENHA_KEY, s.toString()).apply();
				}
			}

			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			public void onTextChanged(CharSequence s, int start, int before, int count) {}
		});
		spinnerCategory = (Spinner) findViewById(R.id.spinnerCategory);
		spinnerServer = (Spinner) findViewById(R.id.spinnerServer);
		loadSavedJson();
		fetchServerData();
		showRemainingTime(this, timerrt);
	}

	private void doUpdateLayout() {
		final SharedPreferences prefs = mConfig.getPrefsPrivate();

		int up = View.VISIBLE;
		boolean isRunning = SkStatus.isTunnelActive();
		setStarterButton(starterButton, this);

		boolean enabled_radio = !isRunning;

		setStarterButton(starterButton, this);
	}


	private synchronized void doSaveData() {
		SharedPreferences prefs = mConfig.getPrefsPrivate();
		SharedPreferences.Editor edit = prefs.edit();

		if (mainLayout != null && !isFinishing())
			mainLayout.requestFocus();


		edit.apply();
	}

	private String decryptAES(String base64CipherText, String password) throws Exception {
		byte[] cipherBytes = Base64.getDecoder().decode(base64CipherText);

		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] key = digest.digest(password.getBytes(StandardCharsets.UTF_8));

		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		SecretKeySpec secretKey = new SecretKeySpec(key, "AES");
		IvParameterSpec iv = new IvParameterSpec(new byte[16]);

		cipher.init(Cipher.DECRYPT_MODE, secretKey, iv);
		byte[] decryptedBytes = cipher.doFinal(cipherBytes);

		return new String(decryptedBytes, StandardCharsets.UTF_8);
	}


	private void fetchServerData() {
		OkHttpClient client = new OkHttpClient();

		File file = new File(getFilesDir(), "config.json");
		String savedJson = "";
		if (file.exists()) {
			try {
				savedJson = new String(Files.readAllBytes(file.toPath()));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		Request request = new Request.Builder()
				.url("https://panel.dr2.site/api2")
				.post(RequestBody.create(new byte[0], null)) // empty body
				.addHeader("Accept", "application/json")
				.addHeader("Connection", "keep-alive")
				.addHeader("dragoncore-token", "b4e7d550-b8e3-4c88-b187-0e6ed42d637e")
				.addHeader("dragoncore-update", "app_config")
				.addHeader("User-Agent", "Dragoncore (@penguinehis, @sisudragon)")
				.build();

		String finalSavedJson = savedJson;

		client.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Call call, IOException e) {
				e.printStackTrace();
				Log.e("JSON_API", "Failed to fetch config.json from API");
				loadSavedJson();
			}

			@Override
			public void onResponse(Call call, Response response) throws IOException {
				if (response.isSuccessful()) {
					String encryptedResponse = response.body().string();

					try {
						JSONObject jsonObject = new JSONObject(encryptedResponse);
						String encryptedData = jsonObject.getString("data");

						// 🔐 Decrypt AES data
						String decryptedJson = decryptAES(encryptedData, "05VE1b3kx10ntsfzvsSmZD3KYuilFXyS");

						if (!decryptedJson.equals(finalSavedJson)) {
							Log.d("JSON_API", "New config found, saving...");

							try (FileOutputStream fos = new FileOutputStream(file)) {
								fos.write(decryptedJson.getBytes());
							}

							Log.d("JSON_SAVE", "config.json updated successfully");
							parseJson(decryptedJson);
						} else {
							Log.d("JSON_API", "No changes in config.json");
							parseJson(finalSavedJson);
						}
					} catch (Exception e) {
						e.printStackTrace();
						Log.e("DECRYPT", "Failed to decrypt AES data");
						loadSavedJson();
					}
				} else {
					Log.e("JSON_API", "API responded with error code: " + response.code());
					loadSavedJson();
				}
			}
		});
	}



	private void loadSavedJson() {
		try {
			File file = new File(getFilesDir(), "config.json");

			// Check if the file exists
			if (!file.exists()) {
				Log.e("JSON_LOAD", "No saved config.json found.");
				runOnUiThread(() -> Toast.makeText(SocksReviveMainActivity.this, "No saved config available", Toast.LENGTH_SHORT).show());
				return;
			}

			// Read the file content
			FileInputStream fis = new FileInputStream(file);
			InputStreamReader isr = new InputStreamReader(fis);
			BufferedReader bufferedReader = new BufferedReader(isr);
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				sb.append(line);
			}
			bufferedReader.close();
			isr.close();
			fis.close();

			String savedJsonData = sb.toString();
			Log.d("JSON_LOAD", "Loaded saved config.json successfully.");

			// Parse the saved JSON data
			parseJson(savedJsonData);

		} catch (Exception e) {
			e.printStackTrace();
			Log.e("JSON_LOAD", "Error loading saved config.json");
		}
	}

	private void parseJson(String jsonData) {
		try {
			JSONArray jsonArray = new JSONArray(jsonData);

			ListaServidores.clear();
			serverInfoList.clear();

			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject jsonObject = jsonArray.getJSONObject(i);

				String name = jsonObject.optString("name", "Unknown");
				JSONObject categoryObject = jsonObject.optJSONObject("category");
				String categoryName = (categoryObject != null) ? categoryObject.optString("name", "Unknown") : "Unknown";
				int categorysorter = (categoryObject != null) ? categoryObject.optInt("sorter", 0) : 0;
				Log.d("JSON_PARSE", "Server: " + name + ", Category: " + categoryName);

				serverInfoList.add(new ServerInfo(
						name, jsonObject.optString("description", "No description"),
						jsonObject.optString("mode", "Unknown"), jsonObject.optString("tls_version", "Unknown"),
						jsonObject.optInt("sorter", Integer.MAX_VALUE), // Default to MAX_VALUE if sorter is missing
						jsonObject.optString("status", "Unknown"),
						jsonObject.optJSONObject("auth") != null ? jsonObject.optJSONObject("auth").optString("username", "Unknown") : "",
						jsonObject.optJSONObject("auth") != null ? jsonObject.optJSONObject("auth").optString("password", "Unknown") : "",
						jsonObject.optJSONObject("config_payload") != null ? jsonObject.optJSONObject("config_payload").optString("payload", "") : "",
						jsonObject.optJSONObject("config_payload") != null ? jsonObject.optJSONObject("config_payload").optString("sni", "") : "",
						jsonObject.optString("config_v2ray", ""),
						jsonObject.optJSONObject("auth") != null ? jsonObject.optJSONObject("auth").optString("v2ray_uuid", "") : "",
						jsonObject.optJSONObject("dns_server") != null ? jsonObject.optJSONObject("dns_server").optString("dns1", "8.8.8.8") : "8.8.8.8",
						jsonObject.optJSONObject("dns_server") != null ? jsonObject.optJSONObject("dns_server").optString("dns2", "8.8.4.4") : "8.8.4.4",
						jsonObject.optString("icon", ""),
						jsonObject.optJSONObject("proxy") != null ? jsonObject.optJSONObject("proxy").optString("host", "") : "",
						jsonObject.optJSONObject("proxy") != null ? jsonObject.optJSONObject("proxy").optInt("port", 0) : 0,
						jsonObject.optJSONObject("server") != null ? jsonObject.optJSONObject("server").optString("host", "") : "",
						jsonObject.optJSONObject("server") != null ? jsonObject.optJSONObject("server").optInt("port", 0) : 0,
						jsonObject.optString("dnstt_key", ""),
						jsonObject.optString("dnstt_name_server", ""),
						jsonObject.optString("dnstt_server", ""),
						jsonObject.optJSONArray("udp_ports") != null ? getUdpPorts(jsonObject.optJSONArray("udp_ports")) : new ArrayList<>(),
						categoryName,
						categorysorter
				));

				ListaServidores.add(name);
			}

			// ✅ Sort the servers by "sorter" (ascending order, lower numbers first)
			Collections.sort(serverInfoList, Comparator.comparingInt(s -> s.sorter));

			runOnUiThread(this::updateSpinner);

		} catch (Exception e) {
			e.printStackTrace();
			runOnUiThread(() -> Toast.makeText(this, "Error parsing JSON", Toast.LENGTH_LONG).show());
		}
	}


	private List<Integer> getUdpPorts(JSONArray udpArray) {
		List<Integer> udpPorts = new ArrayList<>();
		if (udpArray != null) {
			for (int j = 0; j < udpArray.length(); j++) {
				udpPorts.add(udpArray.optInt(j));
			}
		}
		return udpPorts;
	}




	private void updateSpinner() {
		if (spinnerCategory == null || spinnerServer == null) {
			Log.e("SPINNER_ERROR", "One or both spinners are null!");
			return;
		}

		categoryList.clear();
		categoryServerMap.clear();

		Log.d("SPINNER_UPDATE", "Starting category processing...");

		for (ServerInfo server : serverInfoList) {
			String category = server.categoryName.trim();

			if (!categoryServerMap.containsKey(category)) {
				categoryServerMap.put(category, new ArrayList<>());
				categoryList.add(category);
			}
			categoryServerMap.get(category).add(server.name);
		}

		Collections.sort(categoryList, new Comparator<String>() {
			@Override
			public int compare(String category1, String category2) {
				return Integer.compare(getCategorySorterValue(category1), getCategorySorterValue(category2));
			}
		});

		Log.d("SPINNER_UPDATE", "Final Sorted Category List: " + categoryList.toString());

		if (categoryList.isEmpty()) {
			Log.e("SPINNER_ERROR", "No categories found!");
			return;
		}

		// Load last selected category
		SharedPreferences prefs = getSharedPreferences("SPINNER_PREFS", MODE_PRIVATE);
		String lastCategory = prefs.getString("LAST_CATEGORY", categoryList.get(0)); // Default to first category
		String lastServer = prefs.getString("LAST_SERVER", null); // Default to null

		runOnUiThread(() -> {
			ArrayAdapter<String> categoryAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, categoryList);
			categoryAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown);
			spinnerCategory.setAdapter(categoryAdapter);

			int categoryIndex = categoryList.indexOf(lastCategory);
			if (categoryIndex != -1) {
				spinnerCategory.setSelection(categoryIndex); // ✅ Set last selected category
			}

			// Now update servers with last selected server
			updateServerSpinner(lastCategory, lastServer);
		});

		spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (isUserInteraction) { // Ensures execution only on manual selection
					String selectedCategory = categoryList.get(position);
					saveSelectedCategory(selectedCategory);  // ✅ Save selection
					updateServerSpinner(selectedCategory, null); // Load servers but reset last server
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});

		spinnerCategory.setOnTouchListener((v, event) -> {
			isUserInteraction = true;
			return false;  // Let the event propagate
		});
	}

	// Helper function to get the categorySorter value for each category
	private int getCategorySorterValue(String category) {
		for (ServerInfo server : serverInfoList) {
			if (server.categoryName.trim().equals(category)) {
				return server.categorysorter; // Assuming categorySorter is an int in ServerInfo
			}
		}
		return Integer.MAX_VALUE; // Default if no sorter found
	}




	private void updateServerSpinner(String category2, String lastServer) {
		if (!categoryServerMap.containsKey(category2)) {
			Log.e("SPINNER_ERROR", "Category not found in map: " + category2);
			return;
		}

		Log.d("SPINNER_UPDATE", "Updating servers for category: " + category2);

		List<String> serversForCategory = categoryServerMap.get(category2);

		if (serversForCategory == null || serversForCategory.isEmpty()) {
			Log.e("SPINNER_ERROR", "No servers found for category: " + category2);
			return;
		}

		filteredServers.clear();
		filteredServers.addAll(serversForCategory);

		Log.d("SPINNER_UPDATE", "Filtered Servers: " + filteredServers.toString());

		runOnUiThread(() -> {
			ArrayAdapter<String> serverAdapter = new ArrayAdapter<>(this, R.layout.custom_spinner_item, filteredServers);
			serverAdapter.setDropDownViewResource(R.layout.custom_spinner_dropdown);
			spinnerServer.setAdapter(serverAdapter);

			int serverIndex = (lastServer != null) ? filteredServers.indexOf(lastServer) : 0; // Default to first
			if (serverIndex != -1) {
				spinnerServer.setSelection(serverIndex);
			}
		});

		spinnerServer.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				String selectedServer = filteredServers.get(position);
				  // ✅ Save selection
				int absoluteIndex = getAbsoluteIndex(selectedServer, category2);
				saveSelectedServer(selectedServer);
				if (absoluteIndex != -1) {
					saveServerSettings(absoluteIndex);
				} else {
					Log.e("SPINNER_ERROR", "Server index not found: " + selectedServer);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {}
		});
	}


	private int getAbsoluteIndex(String serverName, String category) {
		for (int i = 0; i < serverInfoList.size(); i++) {
			ServerInfo server = serverInfoList.get(i);
			if (server.name.equals(serverName) && server.categoryName.trim().equals(category)) {
				return i; // ✅ Ensure the correct server is returned
			}
		}
		return -1; // Return -1 if not found (should not happen)
	}


	private void saveSelectedCategory(String category) {
		SharedPreferences prefs = getSharedPreferences("SPINNER_PREFS", MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("LAST_CATEGORY", category);
		editor.apply();
	}

	private void saveSelectedServer(String server) {
		SharedPreferences prefs = getSharedPreferences("SPINNER_PREFS", MODE_PRIVATE);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("LAST_SERVER", server);
		editor.apply();
	}



	private void saveServerSettings(int position) {
		if (position < 0 || position >= serverInfoList.size()) {
			return; // Prevent crash if index is invalid
		}
		final SharedPreferences sPrefs = mConfig.getPrefsPrivate();
		ServerInfo selectedServer = serverInfoList.get(position);

		sPrefs.edit().putInt("Servidor", position).apply();
		sPrefs.edit().putString(Settings.SERVIDOR_KEY, selectedServer.serverHost).apply();
		sPrefs.edit().putInt(Settings.SERVIDOR_PORTA_KEY, selectedServer.serverPort).apply();
		sPrefs.edit().putString(Settings.PROXY_IP_KEY, selectedServer.proxyHost).apply();
		sPrefs.edit().putInt(Settings.PROXY_PORTA_KEY, selectedServer.proxyPort).apply();
		sPrefs.edit().putString(Settings.CUSTOM_PAYLOAD_KEY, selectedServer.payload).apply();
		sPrefs.edit().putBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, false).apply();
		SharedPreferences sPrefs2 = PreferenceManager.getDefaultSharedPreferences(this);
		SharedPreferences.Editor editor = sPrefs2.edit();
		SharedPreferences sPrefs3 = PreferenceManager.getDefaultSharedPreferences(this);
		String lastServerId = sPrefs3.getString("last_selected_server_id", null);
		String lastServerId2 = sPrefs3.getString("last_selected_server_id2", null);
		final SharedPreferences mPrefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this);
		if (!selectedServer.sni.isEmpty()) {
			sPrefs.edit().putString(Settings.CUSTOM_SNI, selectedServer.sni).apply();
		}

		Log.d("LOGIN_CHECK", "usernameFromJson: " + selectedServer.username);
		Log.d("LOGIN_CHECK", "savedUsername: " + selectedServer.password);

		TextInputLayout inputLayoutUser = findViewById(R.id.fragment_userTextInputLayout);
		TextInputLayout inputLayoutPass = findViewById(R.id.fragment_userpassTextInputLayout);
		String premiumSenha = sPrefs.getString("PREMIUM_SENHA_KEY", null);
		String premiumUsuario = sPrefs.getString("PREMIUM_USUARIO_KEY", null);

// Username logic
		if (selectedServer.username != null && !selectedServer.username.equals("null")) {
			sPrefs.edit().putString(Settings.USUARIO_KEY, selectedServer.username).apply();
			inputLayoutUser.setVisibility(View.GONE);
			editor.putString("last_selected_server_id", selectedServer.username);
			editor.apply();
		} else {
			String currentUsuario = sPrefs.getString(Settings.USUARIO_KEY, null);
			if (Objects.equals(lastServerId, currentUsuario)) {
				if (premiumUsuario != null) {
					sPrefs.edit().putString(Settings.USUARIO_KEY, premiumUsuario).apply();
					inputPwUser.setText(premiumUsuario);
				} else {
					sPrefs.edit().putString(Settings.USUARIO_KEY, "").apply();
					inputPwUser.setText(null);
				}
				inputLayoutUser.setVisibility(View.VISIBLE);
			} else {
				if (currentUsuario != null) {
					sPrefs.edit().putString("PREMIUM_USUARIO_KEY", currentUsuario).apply();
					sPrefs.edit().putString(Settings.USUARIO_KEY, currentUsuario).apply();
					inputPwUser.setText(currentUsuario);
				}
				inputLayoutUser.setVisibility(View.VISIBLE);
			}
		}

// Password logic
		if (selectedServer.password != null && !selectedServer.password.equals("null")) {
			sPrefs.edit().putString(Settings.SENHA_KEY, selectedServer.password).apply();
			inputLayoutPass.setVisibility(View.GONE);
			inputPwShowPass.setVisibility(View.GONE);
			editor.putString("last_selected_server_id2", selectedServer.password);
			editor.putString("freepaid", "free");
			editor.apply();
		} else {
			editor.putString("freepaid", "paid").apply();
			String currentSenha = sPrefs.getString(Settings.SENHA_KEY, null);
			if (Objects.equals(lastServerId2, currentSenha)) {
				if (premiumSenha != null) {
					sPrefs.edit().putString(Settings.SENHA_KEY, premiumSenha).apply();
					inputPwPass.setText(premiumSenha);
				} else {
					sPrefs.edit().putString(Settings.SENHA_KEY, "").apply();
					inputPwPass.setText(null);
				}
				inputLayoutPass.setVisibility(View.VISIBLE);
				inputPwShowPass.setVisibility(View.VISIBLE);
			} else {
				if (currentSenha != null) {
					sPrefs.edit().putString("PREMIUM_SENHA_KEY", currentSenha).apply();
					sPrefs.edit().putString(Settings.SENHA_KEY, currentSenha).apply();
					inputPwPass.setText(currentSenha);
				}
				inputLayoutPass.setVisibility(View.VISIBLE);
				inputPwShowPass.setVisibility(View.VISIBLE);
			}



		}


		// Set connection mode based on "mode" field
		int tunnelType;
		switch (selectedServer.mode) {
			case "SSH_PROXY":
				sPrefs.edit().putBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, false).apply();
				tunnelType = Settings.bTUNNEL_TYPE_SSH_PROXY;
				break;
			case "SSH_DIRECT":
				sPrefs.edit().putBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, false).apply();
				tunnelType = Settings.bTUNNEL_TYPE_SSH_DIRECT;
				break;
			case "SSL_DIRECT":
				sPrefs.edit().putBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, false).apply();
				tunnelType = Settings.bTUNNEL_TYPE_SSH_SSL;
				break;
			case "SSL_PROXY":
				sPrefs.edit().putBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, false).apply();
				tunnelType = Settings.bTUNNEL_TYPE_SSH_SSL_Proxy;
				break;
			case "SSH_DNSTT":
				tunnelType = Settings.bTUNNEL_TYPE_SSH_DNSTT;
				sPrefs.edit().putBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, true).apply();
				sPrefs.edit().putString(Settings.SERVIDOR_KEY, "127.0.0.1").apply();
				sPrefs.edit().putInt(Settings.SERVIDOR_PORTA_KEY, selectedServer.serverPort > 0 ? selectedServer.serverPort : 2222).apply();
				sPrefs.edit().putString(Settings.SLOW_DNSKEY, selectedServer.dnsttServer).apply();
				sPrefs.edit().putString(Settings.SLOW_NAMESERVER_KEY, selectedServer.dnsttNameServer).apply();
				sPrefs.edit().putString(Settings.SLOW_CHAVE_KEY, selectedServer.dnsttKey).apply();
				mPrefs.edit().putString(Settings.FILTER_APPS_LIST, selectedServer.dnsttServer).apply();
				break;
			case "V2RAY":
				tunnelType = Settings.bTUNNEL_TYPE_XRAY;
				inputLayoutUser.setVisibility(View.GONE);
				inputLayoutPass.setVisibility(View.GONE);
				inputPwShowPass.setVisibility(View.GONE);
				// Save raw V2Ray/Xray config (often base64 or link)
				if (selectedServer.v2rayConfig != null && !selectedServer.v2rayConfig.isEmpty() && !selectedServer.v2rayConfig.equals("null")) {
					sPrefs.edit().putString(Settings.XRAY_CONFIG_KEY, selectedServer.v2rayConfig).apply();
				} else {
					sPrefs.edit().remove(Settings.XRAY_CONFIG_KEY).apply();
				}
				// Optional UUID override (panel auth.v2ray_uuid)
				if (selectedServer.v2rayUuid != null && !selectedServer.v2rayUuid.isEmpty() && !selectedServer.v2rayUuid.equals("null")) {
					sPrefs.edit().putString(Settings.XRAY_UUID_KEY, selectedServer.v2rayUuid).apply();
				} else {
					sPrefs.edit().remove(Settings.XRAY_UUID_KEY).apply();
				}
				// Avoid payload checks for XRAY
				sPrefs.edit().putBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, true).apply();
				break;
			default:
				sPrefs.edit().putBoolean(Settings.PROXY_USAR_DEFAULT_PAYLOAD, false).apply();
				tunnelType = Settings.bTUNNEL_TYPE_SSH_DIRECT;
				break;
		}

		sPrefs.edit().putInt(Settings.TUNNELTYPE_KEY, tunnelType).apply();
	}


	// Server Info Model
	private static class ServerInfo {
		String name, description, mode, tlsVersion, status, username, password;
		String payload, sni, v2rayConfig, v2rayUuid, dns1, dns2, icon, proxyHost, serverHost;
		String dnsttKey, dnsttNameServer, dnsttServer;
		int proxyPort, serverPort, sorter;
		List<Integer> udpPorts;
		String categoryName;
		int categorysorter;

		ServerInfo(String name, String description, String mode, String tlsVersion, int sorter, String status,
				   String username, String password, String payload, String sni, String v2rayConfig, String v2rayUuid,
				   String dns1, String dns2, String icon, String proxyHost, int proxyPort,
				   String serverHost, int serverPort, String dnsttKey, String dnsttNameServer, String dnsttServer,
				   List<Integer> udpPorts, String categoryName,int categorysorter) {
			this.name = name;
			this.description = description;
			this.mode = mode;
			this.tlsVersion = tlsVersion;
			this.sorter = sorter;
			this.status = status;
			this.username = username;
			this.password = password;
			this.payload = payload;
			this.sni = sni;
			this.v2rayConfig = v2rayConfig;
			this.v2rayUuid = v2rayUuid;
			this.dns1 = dns1;
			this.dns2 = dns2;
			this.icon = icon;
			this.proxyHost = proxyHost;
			this.proxyPort = proxyPort;
			this.serverHost = serverHost;
			this.serverPort = serverPort;
			this.dnsttKey = dnsttKey;
			this.dnsttNameServer = dnsttNameServer;
			this.dnsttServer = dnsttServer;
			this.udpPorts = udpPorts;
			this.categoryName = categoryName;
			this.categorysorter = categorysorter;
		}
	}


	/**
	 * Tunnel SSH
	 */

	public void startOrStopTunnel(Activity activity) {
		CharlieProtect();
		SharedPreferences prefs = mConfig.getPrefsPrivate();
		SharedPreferences.Editor edit = prefs.edit();
		if (SkStatus.isTunnelActive()) {
			Random random = new Random();
			int randomNumber = random.nextInt(2000);
			cake3 = String.valueOf(randomNumber);
			cake2 = String.valueOf(randomNumber);
			Log.d("cake2", cake3);
			Log.d("cake2", cake2);
			TunnelManagerHelper.stopSocksRevive(activity);
		} else {
			int currentTunnelType = prefs.getInt(Settings.TUNNELTYPE_KEY, Settings.bTUNNEL_TYPE_SSH_DIRECT);
			Log.d(String.valueOf(currentTunnelType), "cake");
			if (currentTunnelType == Settings.bTUNNEL_TYPE_SSH_DNSTT) {
				try {
					startdnsvoid(activity);

				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				startvpn(activity);
			}


		}
	}



	private CountDownTimer usageTimer;

	public void setStarterButton(Button starterButton, Activity activity) {
		String state = SkStatus.getLastState();
		boolean isRunning = SkStatus.isTunnelActive();

		if (starterButton != null) {
			int resId;
			SharedPreferences prefsPrivate = new Settings(activity).getPrefsPrivate();


			if (SkStatus.SSH_INICIANDO.equals(state)) {
				cake3 = "10";
				cake2 = "22";
				
				resId = R.string.stop;
				starterButton.setEnabled(true);
				inputPwUser.setEnabled(false);
				inputPwPass.setEnabled(false);
				spinnerServer.setEnabled(false);
				spinnerCategory.setEnabled(false);
				mDrawer.clearLogs();
			}
			else if (SkStatus.SSH_CONECTADO.equals(state)) {
				cake3 = "33";
				cake2 = "44";
				
				resId = R.string.stop;
				starterButton.setEnabled(true);
				inputPwUser.setEnabled(false);
				inputPwPass.setEnabled(false);
				spinnerServer.setEnabled(false);
				spinnerCategory.setEnabled(false);
				SharedPreferences sPrefs3 = PreferenceManager.getDefaultSharedPreferences(this);
				String freepaid2 = sPrefs3.getString("freepaid", null);
//				if(freepaid2.equals("free")){
//				startUsageTimer(activity, timerrt); // Pass starterButton here
//				}


			}
			else if (SkStatus.SSH_PARANDO.equals(state)) {
				resId = R.string.state_stopping;
				starterButton.setEnabled(false);
				new Timer().schedule(new TimerTask() {
					@Override
					public void run() {
						if (SkStatus.SSH_PARANDO.equals(state) && cake3.equals(cake2)){
							SkStatus.updateStateString(SkStatus.SSH_DESCONECTADO, "Forced Stop");
							SkStatus.logInfo("Forced STOP");
						}else{
							SkStatus.logInfo("No problems Found");
						}
					}
				}, 4000);
			}
			else if (SkStatus.SSH_DESCONECTADO.equals(state)) {
				starterButton.setEnabled(true);
				
				inputPwUser.setEnabled(true);
				inputPwPass.setEnabled(true);
				spinnerServer.setEnabled(true);
				spinnerCategory.setEnabled(true);
				stopdns();
				if (usageTimer != null) {
					usageTimer.cancel();
					usageTimer = null;
				}

				long timeLeft = getRemainingTime(activity);


				if (timeLeft <= 0) {
					addTime(this,5, timerrt);
					timerrt.setText(R.string.notimeleft2);
				}

				resId = R.string.start;
			}

			else {
				resId = isRunning ? R.string.stop : R.string.start;
				starterButton.setEnabled(true);
			}

			starterButton.setText(resId);
		}
	}

	private long getRemainingTime(Activity activity) {
		SharedPreferences prefs = new Settings(activity).getPrefsPrivate();
		return prefs.getLong(PREF_TIME_LEFT, 60); // default 60 minutes
	}

	private void saveRemainingTime(Activity activity, long minutesLeft) {
		SharedPreferences prefs = new Settings(activity).getPrefsPrivate();
		prefs.edit()
				.putLong(PREF_TIME_LEFT, minutesLeft)
				.apply();
	}

	public void addTime(Activity activity, int minutesToAdd, TextView timerrt) {
		CharlieProtect();
		Log.d("TimerDebug", "Time to add: " + minutesToAdd + " minutes");

		long currentLeft = getRemainingTime(activity);
		long newTime = currentLeft + minutesToAdd;
		Log.d("TimerDebug", "New total time: " + newTime + " minutes");

		SharedPreferences prefs = new Settings(activity).getPrefsPrivate();
		prefs.edit()
				.putLong(PREF_TIME_LEFT, newTime)
				.apply();

		// Convert newTime to hours, minutes, and seconds
		long hours = newTime / 60;
		long minutes = newTime % 60;
		long seconds = 0; // Since we're adding minutes, seconds will always start at 0

		// Update UI
		if (timerrt != null) {
			timerrt.setText(getString(R.string.notimeleft3) + ": "
					+ hours + "h "
					+ minutes + "m "
					+ seconds + "s");
		}

		// If tunnel is active, restart the timer with the new total time
		if (SkStatus.isTunnelActive()) {
			startUsageTimer(activity, timerrt);
		}
	}




	private void startUsageTimer(Activity activity, TextView timerrt) {
		long timeLeft = getRemainingTime(activity); // in minutes
		Log.d("TimerDebug", "Time left: " + timeLeft + " minutes");

		if (timeLeft <= 0) {
			TunnelManagerHelper.stopSocksRevive(activity);
			if (timerrt != null) {
				addTime(this,5, timerrt);
				timerrt.setText(R.string.notimeleft2);
			}
			return;
		}

		if (usageTimer != null) {
			usageTimer.cancel();
		}

		usageTimer = new CountDownTimer(timeLeft * 60 * 1000L, 1000) {
			public void onTick(long millisUntilFinished) {
				long hoursRemaining = millisUntilFinished / (60 * 60 * 1000);
				long minutesRemaining = (millisUntilFinished / (60 * 1000)) % 60;
				long secondsRemaining = (millisUntilFinished / 1000) % 60;

				saveRemainingTime(activity, millisUntilFinished / (60 * 1000)); // Save in minutes

				if (timerrt != null) {
					timerrt.setText(getString(R.string.notimeleft3) + ": "
							+ hoursRemaining + "h "
							+ minutesRemaining + "m "
							+ secondsRemaining + "s");
				}
			}


			public void onFinish() {
				new Handler(Looper.getMainLooper()).postDelayed(() -> {
					long stillRemaining = getRemainingTime(activity);
					if (stillRemaining > 0) {
						Log.d("TimerDebug", "Timer finished but new time was added. Skipping shutdown.");
						return;
					}

					saveRemainingTime(activity, 0);
					TunnelManagerHelper.stopSocksRevive(activity);
					Toast.makeText(activity, R.string.notimeleft, Toast.LENGTH_LONG).show();
					if (timerrt != null) {
						timerrt.setText(R.string.notimeleft2);
					}
				}, 500); // Delay 500ms to ensure addTime() runs first
			}

		};

		usageTimer.start();
	}

	private void showRemainingTime(Activity activity, TextView timerrt) {
		long timeLeft = getRemainingTime(activity); // in minutes

		// Convert minutes into hours, minutes, and seconds
		long hours = timeLeft / 60;
		long minutes = timeLeft % 60;

		if (timerrt != null) {
			timerrt.setText(getString(R.string.notimeleft3) + ": "
					+ hours + "h "
					+ minutes + "m 0s");
		}
	}






	@Override
	public void onPostCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
		super.onPostCreate(savedInstanceState, persistentState);
		if (mDrawerPanel.getToogle() != null)
			mDrawerPanel.getToogle().syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		if (mDrawerPanel.getToogle() != null)
			mDrawerPanel.getToogle().onConfigurationChanged(newConfig);
	}

	private boolean isMostrarSenha = false;

	@Override
	public void onClick(View p1) {
		SharedPreferences prefs = mConfig.getPrefsPrivate();

		int id = p1.getId();

		if (id == R.id.activity_starterButtonMain) {
			doSaveData();
			startOrStopTunnel(this);
		} else if (id == R.id.activity_mainInputShowPassImageButton) {
			isMostrarSenha = !isMostrarSenha;
			if (isMostrarSenha) {
				inputPwPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
			} else {
				inputPwPass.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
			}
		} else if (id == R.id.logs) {
			showLogWindow();
		} else if (id == R.id.contato) {
			openlink();
		}
	}


	private void openlink() {
		 // Adds 60 seconds


	}


	@Override
	public void onCheckedChanged(RadioGroup p1, int p2)
	{
		SharedPreferences.Editor edit = mConfig.getPrefsPrivate().edit();


		edit.apply();

		doSaveData();
		doUpdateLayout();
	}

	@Override
	public void onCheckedChanged(CompoundButton p1, boolean p2)
	{
		SharedPreferences prefs = mConfig.getPrefsPrivate();
		SharedPreferences.Editor edit = prefs.edit();

		edit.apply();

		doSaveData();
	}

	protected void showBoasVindas() {
		new AlertDialog.Builder(this)
				. setTitle(R.string.attention)
				. setMessage(R.string.first_start_msg)
				. setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface di, int p) {
						// ok
					}
				})
				. setCancelable(false)
				. show();
	}

	protected void showAvisoUseTimer() {
		new AlertDialog.Builder(this)
				. setTitle("Use apenas se souber!")
				. setMessage("Isto define um tempo para o app reconectar.\n\n1 - Clique em TEMPO e coloque um valor,por exemplo 5 para 5 minutos,30 para 30 minutos,etc\n2 - Clique em INSERIR\n\n4 - Agora o app vai ficar reconectando no tempo definido\n\n5 - Para parar clique em PAUSAR,ou desative o Auto Reconexão\n6 - Para limpar clique em RESETAR\n\nObs.:Isto é útil se sua internet cai ou fica lenta após um tempo(De 5 em 5 minutos,por exemplo),você pode colocar para reconectar neste tempo de 5 minutos")
				. setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface di, int p) {
						// ok
					}
				})
				. setCancelable(false)
				. show();
	}

	private void showLogWindow() {
		Intent updateView = new Intent("com.penguinehis.socksrevive:openLogs");
		LocalBroadcastManager.getInstance(this)
				.sendBroadcast(updateView);
	}

	@Override
	public void updateState(final String state, String msg, int localizedResId, final ConnectionStatus level, Intent intent)
	{
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				doUpdateLayout();
			}
		});

		switch (state) {
			case SkStatus.SSH_CONECTADO:
				// carrega ads banner
				break;
		}
	}


	/**
	 * Recebe locais Broadcast
	 */

	private BroadcastReceiver mActivityReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action == null)
				return;

			if (action.equals(UPDATE_VIEWS) && !isFinishing()) {
				doUpdateLayout();
			}
			else if (action.equals(OPEN_LOGS)) {
				if (mDrawer != null && !isFinishing()) {
					DrawerLayout drawerLayout = mDrawer.getDrawerLayout();

					if (!drawerLayout.isDrawerOpen(GravityCompat.END)) {
						drawerLayout.openDrawer(GravityCompat.END);
					}
				}
			}
		}
	};


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (mDrawerPanel.getToogle() != null && mDrawerPanel.getToogle().onOptionsItemSelected(item)) {
			return true;
		}

		int id = item.getItemId();

		if (id == R.id.miSettings) {
			Intent intentSettings = new Intent(this, ConfigGeralActivity.class);
			startActivity(intentSettings);
			return true;  // Ensure the event is consumed
		} else if (id == R.id.miLimparLogs) {
			if (mDrawer != null) {
				mDrawer.clearLogs();
			} else {
				Log.e("Menu", "mDrawer is null, cannot clear logs.");
			}
			return true;
		} else if (id == R.id.miExit) {
			if (Build.VERSION.SDK_INT >= 16) {
				finishAffinity();
			}
			return true;  // Remove System.exit(0) to avoid forceful app termination
		}else if (id == R.id.updateconfig) {
		fetchServerData();
		}else if (id == R.id.logs) {
			showLogWindow();
		}

		return super.onOptionsItemSelected(item);
	}


	@Override
	public void onBackPressed() {
        super.onBackPressed();
        DrawerLayout layout = mDrawer.getDrawerLayout();

		if (mDrawerPanel.getDrawerLayout().isDrawerOpen(GravityCompat.START)) {
			mDrawerPanel.getDrawerLayout().closeDrawers();
		}
		else if (layout.isDrawerOpen(GravityCompat.END)) {
			// fecha drawer
			layout.closeDrawers();
		}
		else {
			// mostra opção para sair
			showExitDialog();
		}
	}

	@Override
	public void onResume() {
		super.onResume();

		mDrawer.onResume();

		//doSaveData();
		//doUpdateLayout();

		SkStatus.addStateListener(this);

		registerNetworkBroadcastReceiver(this);
		super.onResume();

	}

	@Override
	protected void onPause()
	{
		unregisterNetworkBroadcastReceiver(this);

		super.onPause();

		doSaveData();

		SkStatus.removeStateListener(this);


	}


	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		mDrawer.onDestroy();

		LocalBroadcastManager.getInstance(this)
				.unregisterReceiver(mActivityReceiver);

	}


	/**
	 * DrawerLayout Listener
	 */

	@Override
	public void onDrawerOpened(View view) {
		if (view.getId() == R.id.activity_mainLogsDrawerLinear) {
			toolbar_main.getMenu().clear();
			getMenuInflater().inflate(R.menu.logs_menu, toolbar_main.getMenu());
		}
	}

	@Override
	public void onDrawerClosed(View view) {
		if (view.getId() == R.id.activity_mainLogsDrawerLinear) {
			toolbar_main.getMenu().clear();
			getMenuInflater().inflate(R.menu.main_menu, toolbar_main.getMenu());
		}
	}

	@Override
	public void onDrawerStateChanged(int stateId) {}
	@Override
	public void onDrawerSlide(View view, float p2) {}


	/**
	 * Utils
	 */

	public static void updateMainViews(Context context) {
		Intent updateView = new Intent(UPDATE_VIEWS);
		LocalBroadcastManager.getInstance(context)
				.sendBroadcast(updateView);
	}

	public void showExitDialog() {
		AlertDialog dialog = new AlertDialog.Builder(this).
				create();
		dialog.setTitle(getString(R.string.attention));
		dialog.setMessage(getString(R.string.alert_exit));

		dialog.setButton(DialogInterface.BUTTON_POSITIVE, getString(R.
						string.exit),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which)
					{
						Utils.exitAll(SocksReviveMainActivity.this);
					}
				}
		);

		dialog.setButton(DialogInterface.BUTTON_NEGATIVE, getString(R.
						string.minimize),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						// minimiza app
						Intent startMain = new Intent(Intent.ACTION_MAIN);
						startMain.addCategory(Intent.CATEGORY_HOME);
						startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(startMain);
					}
				}
		);

		dialog.show();
	}
	private final Runnable mRunnable = new Runnable() {

		public void run() {
			TextView RX = (TextView) findViewById(R.id.RX);
			TextView TX = (TextView) findViewById(R.id.TX);
			long resetdownload=TrafficStats.getTotalRxBytes();
			long rxBytes = TrafficStats.getTotalRxBytes() - mStartRX;
			RX.setText(Long.toString(rxBytes) + " bytes");
			if(rxBytes>=1024){
				//KB or more
				long rxKb = rxBytes/1024;
				RX.setText(Long.toString(rxKb) + " KBs");
				if(rxKb>=1024){
					//MB or more
					long rxMB = rxKb/1024;
					RX.setText(Long.toString(rxMB) + " MBs");
					if(rxMB>=1024){
						//GB or more
						long rxGB = rxMB/1024;
						RX.setText(Long.toString(rxGB) + " GBs");
					}//rxMB>1024
				}//rxKb > 1024
			}//rxBytes>=1024
			mStartRX=resetdownload;

			long resetupload=TrafficStats.getTotalTxBytes();
			long txBytes = TrafficStats.getTotalTxBytes() - mStartTX;
			TX.setText(Long.toString(txBytes) + " bytes");
			if(txBytes>=1024){
				//KB or more
				long txKb = txBytes/1024;
				TX.setText(Long.toString(txKb) + " KBs");
				if(txKb>=1024){
					//MB or more
					long txMB = txKb/1024;
					TX.setText(Long.toString(txMB) + " MBs");
					if(txMB>=1024){
						//GB or more
						long txGB = txMB/1024;
						TX.setText(Long.toString(txGB) + " GBs");
					}//txMB>1024
				}//txKb > 1024
			}//txBytes>=1024
			mStartTX=resetupload;

			TextView YX = (TextView) findViewById(R.id.YX);
			TextView UX = (TextView) findViewById(R.id.UX);
			long yxBytes = TrafficStats.getTotalRxBytes() - mStartYX;
			YX.setText(Long.toString(yxBytes) + " bytes");
			if(yxBytes>=1024){
				//KB or more
				long yxKb = yxBytes/1024;
				YX.setText(Long.toString(yxKb) + " KB");
				if(yxKb>=1024){
					//MB or more
					long yxMB = yxKb/1024;
					YX.setText(Long.toString(yxMB) + " MB");
					if(yxMB>=1024){
						//GB or more
						long yxGB = yxMB/1024;
						YX.setText(Long.toString(yxGB) + " GB");
					}//yxMB>1024
				}//yxKb > 1024
			}//yxBytes>=1024

			long uxBytes = TrafficStats.getTotalTxBytes() - mStartUX;
			UX.setText(Long.toString(uxBytes) + " bytes");
			if(uxBytes>=1024){
				//KB or more
				long uxKb = uxBytes/1024;
				UX.setText(Long.toString(uxKb) + " KB");
				if(uxKb>=1024){
					//MB or more
					long uxMB = uxKb/1024;
					UX.setText(Long.toString(uxMB) + " MB");
					if(uxMB>=1024){
						//GB or more
						long uxGB = uxMB/1024;
						UX.setText(Long.toString(uxGB) + " GB");
					}//uxMB>1024
				}//uxKb > 1024
			}//uxBytes>=1024
			mHandler.postDelayed(mRunnable, 1000);
		}
	};

	protected String getIpPublic() {

		final android.net.NetworkInfo network = connMgr
				.getActiveNetworkInfo();

		if (network != null && network.isConnectedOrConnecting()) {
			return TunnelUtils.getLocalIpAddress();
		}
		else {
			return "Indisponivel";
		}
	}

	public void startNetworkBroadcastReceiver(Context currentContext) {
		networkStateReceiver = new NetworkStateReceiver();
		networkStateReceiver.addListener((NetworkStateReceiver.NetworkStateReceiverListener) currentContext);
		registerNetworkBroadcastReceiver(currentContext);
	}

	/**
	 * Register the NetworkStateReceiver with your activity
	 * @param currentContext
	 */
	public void registerNetworkBroadcastReceiver(Context currentContext) {
		currentContext.registerReceiver(networkStateReceiver, new IntentFilter(android.net.ConnectivityManager.CONNECTIVITY_ACTION));
	}

	/**
	 Unregister the NetworkStateReceiver with your activity
	 * @param currentContext
	 */
	public void unregisterNetworkBroadcastReceiver(Context currentContext) {
		currentContext.unregisterReceiver(networkStateReceiver);
	}

	@Override
	public void networkAvailable() {
		TextView iplocal = (TextView) findViewById(R.id.iplocal);
		Log.i(TAG, "networkAvailable()");
		TelephonyManager manager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		String carrierName = manager.getNetworkOperatorName();
		String ipatual = (String.valueOf(getIpPublic()));
		iplocal.setText(ipatual);


	}

	@Override
	public void networkUnavailable() {
		TextView iplocal = (TextView) findViewById(R.id.iplocal);
		Log.i(TAG, "networkUnavailable()");
		iplocal.setText("Desconectado!");

	}


	@RequiresApi(api = Build.VERSION_CODES.M)
	private void local_dns(){
		final SharedPreferences.Editor edit = mConfig.getPrefsPrivate().edit();
		try  {
			//OBTEM DNS LOCAL
			ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
			for (Network network : connectivityManager.getAllNetworks()) {
				NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
				if (networkInfo.isConnected()) {
					LinkProperties linkProperties = connectivityManager.getLinkProperties(network);
					Log.d("DnsInfo", "iface = " + linkProperties.getInterfaceName());
					Log.d("DnsInfo", "dns = " + linkProperties.getDnsServers());
					linkProperties.getDnsServers();

					//MOSTRA RESULTADO INICIAL NO LOG
					Log.d("LocalDNS Result", String.valueOf(linkProperties.getDnsServers()));

					//OBTEM ARRAY DE TODOS DNS LOCAL
					System.out.println("LocalDNS Array "+linkProperties.getDnsServers().get(0));
					String current_dns_string = String.valueOf(linkProperties.getDnsServers().get(0));

					//SPLITA DNS
					String final_dns = current_dns_string.replace("/", "");

					//APLICA DNS
					edit.putString(Settings.SLOW_DNSKEY, final_dns).apply();

					Log.d("LocalDNS Splited", final_dns);


					return;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	public void startvpn (Activity activity){
		//INICIA NORMAL
		// oculta teclado se vísivel, tá com bug, tela verde
		//Utils.hideKeyboard(activity);

		//#######################################################

		Settings config = new Settings(activity);
		if (config.getPrefsPrivate()
				.getBoolean(Settings.CONFIG_INPUT_PASSWORD_KEY, false)) {
			if (inputPwUser.getText().toString().isEmpty() ||
					inputPwPass.getText().toString().isEmpty()) {
				Toast.makeText(this, R.string.error_userpass_empty, Toast.LENGTH_SHORT)
						.show();
				return;
			}
		}
		Intent intent = new Intent(activity, LaunchVpn.class);
		intent.setAction(Intent.ACTION_MAIN);
		if (config.getHideLog()) {
			intent.putExtra(LaunchVpn.EXTRA_HIDELOG, true);
		}
		activity.startActivity(intent);
	}
	private boolean isPackageInstalled(String packageName, PackageManager packageManager) {
		try {
			packageManager.getPackageInfo(packageName, 0);
			return true;
		} catch (PackageManager.NameNotFoundException e) {
			return false;
		}
	}

	//##################### DNS #####################

	private void startdnsvoid(Activity activity) throws IOException {


		//Onde fica salvo as coisas no aparelho
		final SharedPreferences slowprefs = mConfig.getPrefsPrivate();

		StringBuilder cmd1 = new StringBuilder();
		filedns = CustomNativeLoader.loadNativeBinary(this, DNS_BIN, new File(this.getFilesDir(), DNS_BIN));

		if (filedns == null) {
			throw new IOException("Bin DNS não encontrado");
		}


		final String chave = slowprefs.getString(Settings.SLOW_CHAVE_KEY, "slowchave");
		final String nameserver = slowprefs.getString(Settings.SLOW_NAMESERVER_KEY, "slowns");
		final String dns = slowprefs.getString(Settings.SLOW_DNSKEY, "slowdns");
		final int localPort = slowprefs.getInt(Settings.SERVIDOR_PORTA_KEY, 2222);
		final String localEndpoint = "127.0.0.1:" + localPort;
		// executa comando real do DNSTT: libstartdns -udp <resolver> -pubkey <key> <nameserver> <local-listen>
		dnsProcess = new ProcessBuilder(
				filedns.getCanonicalPath(),
				"-udp", dns,
				"-pubkey", chave,
				nameserver,
				localEndpoint
		).redirectErrorStream(true).start();

		try {
			startvpn(activity);

		} catch (Exception e) {
			SkStatus.logDebug("BIN Error: " + e);
		}


	}

	private void stopdns() {

		if (dnsProcess != null)
			dnsProcess.destroy();

		try {
			if (filedns != null)
				KillThis.killProcess(filedns);
		} catch (Exception e) {
		}

		dnsProcess = null;
		filedns = null;


	}

	private void inAppUp() {

		appUpdateManager = AppUpdateManagerFactory.create(this);
		Task<AppUpdateInfo> task = appUpdateManager.getAppUpdateInfo();
		task.addOnSuccessListener(appUpdateInfo -> {

			if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
					&& appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
			) {

				try {
					appUpdateManager.startUpdateFlowForResult(appUpdateInfo, AppUpdateType.IMMEDIATE,
							SocksReviveMainActivity.this,
							UPDATE_CODE
					);
				} catch (IntentSender.SendIntentException e) {
					e.printStackTrace();
					//Log.d("updateerror", "onSuccess: " + e.toString());
				}
			}
		});

		appUpdateManager.registerListener(listener);
	}
	InstallStateUpdatedListener listener = installState -> {
		if (installState.installStatus() == InstallStatus.DOWNLOADED){
			popUp();
		}
	};

	private void popUp() {

		Snackbar snackbar = Snackbar.make(
				findViewById(android.R.id.content),
				R.string.updateapp,
				Snackbar.LENGTH_INDEFINITE
		);

		snackbar.setAction(R.string.reloadapp, new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				appUpdateManager.completeUpdate();

			}
		});
		snackbar.setActionTextColor(Color.parseColor("#FFFFFF"));
		snackbar.show();
	}

}


