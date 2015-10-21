package cn.slzhong.focuz.Activities;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.neurosky.thinkgear.TGDevice;

import org.json.JSONObject;

import cn.slzhong.focuz.Constants.CODES;
import cn.slzhong.focuz.Constants.URLS;
import cn.slzhong.focuz.Models.Recorder;
import cn.slzhong.focuz.Models.User;
import cn.slzhong.focuz.R;

/**
 * MainActivity
 */
public class MainActivity extends AppCompatActivity {

    // views
    private RelativeLayout root;
    private RelativeLayout welcome;
    private TextView welcomeTitle;
    private TextView welcomeCopyright;
    private ImageView welcomeIcon;
    private View welcomeBackground;
    private View welcomeBackgroundPseudo;
    private RelativeLayout login;
    private EditText loginAccount;
    private EditText loginPassword;
    private EditText loginConfirm;
    private Button loginSignin;
    private Button loginSignup;
    private RelativeLayout main;
    private Button mainTimer;
    private Button mainStopwatch;
    private Button mainHistory;
    private Button mainStop;
    private TextView mainStatus;
    private RelativeLayout rate;

    // data
    private Handler animationHandler;
    private Handler tgHandler;
    private BluetoothAdapter bluetoothAdapter;
    private TGDevice tgDevice;
    private Recorder tgRecorder;
    private Runnable tgRunnable;
    private User user;

    // flags and temps
    private boolean tgConnected = false;
    private int tgAttention = 0;
    private int tgMeditation = 0;
    private int tgTime = -1;
    private int tgCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initData();
        showWelcome();
    }

    @Override
    protected void onResume() {
        super.onResume();
        enterFullscreen();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    private void initViews() {
        // init components
        root = (RelativeLayout) findViewById(R.id.fl_root);
        root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enterFullscreen();
            }
        });
        welcome = (RelativeLayout) findViewById(R.id.rl_welcome);
        welcomeTitle = (TextView) findViewById(R.id.tv_welcome_title);
        welcomeCopyright = (TextView) findViewById(R.id.tv_welcome_copyright);
        welcomeIcon = (ImageView) findViewById(R.id.iv_welcome_icon);
        welcomeBackground = findViewById(R.id.ll_welcome_background);
        welcomeBackgroundPseudo = findViewById(R.id.ll_welcome_background_pseudo);

        login = (RelativeLayout) findViewById(R.id.rl_login);
        loginAccount = (EditText) findViewById(R.id.et_account);
        loginPassword = (EditText) findViewById(R.id.et_password);
        loginConfirm = (EditText) findViewById(R.id.et_confirm);
        loginSignin = (Button) findViewById(R.id.bt_signin);
        loginSignup = (Button) findViewById(R.id.bt_signup);
        loginSignin.setOnClickListener(new SigninListener());
        loginSignup.setOnClickListener(new SignupListener());

        main = (RelativeLayout) findViewById(R.id.rl_main);
        mainTimer = (Button) findViewById(R.id.bt_timer);
        mainStopwatch = (Button) findViewById(R.id.bt_stopwatch);
        mainHistory = (Button) findViewById(R.id.bt_history);
        mainStop = (Button) findViewById(R.id.bt_stop);
        mainStatus = (TextView) findViewById(R.id.tv_status);
        mainTimer.setOnClickListener(new TimerListener());
        mainStopwatch.setOnClickListener(new StopwatchListener());
        mainStop.setOnClickListener(new StopListener());

        rate = (RelativeLayout) findViewById(R.id.rl_rate);

        // hide actionbar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        // hide status bar
        enterFullscreen();
    }

    private void initData() {
        user = User.getInstance(this);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        animationHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                if (msg.what == CODES.SIGN_SUCCESS) {
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(root.getWindowToken(), 0);
                    hideScene(login);
                    showScene(main);
                    connectTG();
                }
            }
        };

        tgHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case TGDevice.MSG_STATE_CHANGE:
                        switch (msg.arg1) {
                            case TGDevice.STATE_IDLE:
                                break;
                            case TGDevice.STATE_CONNECTING:
                                break;
                            case TGDevice.STATE_CONNECTED:
                                tgDevice.start();
                                tgConnected = true;
                                mainStatus.setText("");
                                break;
                            case TGDevice.STATE_DISCONNECTED:
                                mainStatus.setText("THINK GEAR DEVICE NOT DISCONNECTED");
                                break;
                            case TGDevice.STATE_NOT_FOUND:
                            case TGDevice.STATE_NOT_PAIRED:
                                mainStatus.setText("THINK GEAR DEVICE NOT FOUND");
                                break;
                            default:
                                break;
                        }
                        break;
                    case TGDevice.MSG_MEDITATION:
                        System.out.println("***** m" + msg.arg1);
                        tgMeditation = msg.arg1;
                        break;
                    case TGDevice.MSG_ATTENTION:
                        System.out.println("***** a" + msg.arg1);
                        tgAttention = msg.arg1;
                        break;
                    case TGDevice.MSG_RAW_DATA:
                        break;
                    default:
                        break;
                }
            }
        };
    }

    private void enterFullscreen() {
        root.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    /**
     * functions of animations
     */
    private void showWelcome() {
        showWelcomeAnimation();

        animationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                moveTitleAnimation();
            }
        }, 2100);
        animationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                showCopyrightAnimation();
            }
        }, 2800);
        animationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                hideWelcomeAnimation();
            }
        }, 5200);
        animationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (user.hasSignedIn) {
                    showScene(main);
                    connectTG();
                } else {
                    showScene(login);
                }
            }
        }, 5300);
    }

    private void showWelcomeAnimation() {
        AnimationSet animationSet = new AnimationSet(false);
        animationSet.setFillEnabled(true);
        animationSet.setFillAfter(true);

        TranslateAnimation moveUp = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, -4);
        moveUp.setDuration(600);
        moveUp.setStartOffset(100);
        moveUp.setInterpolator(new DecelerateInterpolator());
        animationSet.addAnimation(moveUp);

        TranslateAnimation dropDown = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 3.5f);
        dropDown.setDuration(525);
        dropDown.setStartOffset(800);
        dropDown.setInterpolator(new AccelerateInterpolator());
        animationSet.addAnimation(dropDown);

        TranslateAnimation expandTranslate = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0.1f);
        expandTranslate.setDuration(200);
        expandTranslate.setStartOffset(1325);
        expandTranslate.setInterpolator(new LinearInterpolator());
        animationSet.addAnimation(expandTranslate);

        ScaleAnimation expandScale = new ScaleAnimation(
                1, 20, 1, 20,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);

        expandScale.setDuration(300);
        expandScale.setStartOffset(1325);
        expandScale.setInterpolator(new LinearInterpolator());
        animationSet.addAnimation(expandScale);

        welcomeBackground.startAnimation(animationSet);
    }

    private void moveTitleAnimation() {
        TranslateAnimation translateAnimation = new TranslateAnimation(
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 0,
                Animation.RELATIVE_TO_SELF, 1f);
        translateAnimation.setDuration(500);
        translateAnimation.setFillEnabled(true);
        translateAnimation.setFillAfter(true);
        translateAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
        welcomeTitle.startAnimation(translateAnimation);

        AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
        alphaAnimation.setDuration(500);
        welcomeIcon.setVisibility(View.VISIBLE);
        welcomeIcon.startAnimation(alphaAnimation);
    }

    private void showCopyrightAnimation() {
        AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
        alphaAnimation.setDuration(200);
        welcomeCopyright.setVisibility(View.VISIBLE);
        welcomeCopyright.startAnimation(alphaAnimation);
    }

    private void hideWelcomeAnimation() {
        welcomeBackground.setVisibility(View.GONE);
        welcomeBackgroundPseudo.setVisibility(View.VISIBLE);

        ScaleAnimation scaleContent = new ScaleAnimation(
                1, 0, 1, 0,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scaleContent.setDuration(300);
        scaleContent.setFillAfter(true);
        welcome.startAnimation(scaleContent);

        ScaleAnimation scaleBackground = new ScaleAnimation(
                2, 0, 2, 0,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        scaleBackground.setDuration(300);
        scaleBackground.setFillAfter(true);
        welcomeBackgroundPseudo.startAnimation(scaleBackground);
    }

    private void showScene(RelativeLayout scene) {
        scene.setVisibility(View.VISIBLE);

        AnimationSet animationSet = new AnimationSet(true);
        animationSet.setDuration(300);
        animationSet.setFillEnabled(true);
        animationSet.setFillAfter(true);

        AlphaAnimation alphaAnimation = new AlphaAnimation(0, 1);
        animationSet.addAnimation(alphaAnimation);

        ScaleAnimation scaleAnimation = new ScaleAnimation(
                0.7f, 1, 0.7f, 1,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);
        animationSet.addAnimation(scaleAnimation);

        scene.startAnimation(animationSet);
    }

    private void hideScene(final RelativeLayout scene) {
        AlphaAnimation alphaAnimation = new AlphaAnimation(1, 0);
        alphaAnimation.setDuration(300);
        alphaAnimation.setFillEnabled(true);
        alphaAnimation.setFillAfter(true);
        scene.startAnimation(alphaAnimation);
        animationHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                scene.setVisibility(View.GONE);
            }
        }, 310);
    }

    /**
     * funcitions of notices like alert and toast
     */
    private void showAlert(String msg) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Looper.prepare();
        }
        new AlertDialog.Builder(this)
                .setTitle("NOTICE")
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Looper.loop();
        }
    }

    /**
     * functions of recording data
     */
    private void connectTG() {
        if (bluetoothAdapter != null) {
            if (tgDevice != null) {
                tgDevice.close();
                tgDevice = null;
            }
            tgDevice = new TGDevice(bluetoothAdapter, tgHandler);
            tgDevice.connect(true);
            mainStatus.setText("CONNECTING TO THINK GEAR DEVICE...");
        }
    }

    private void startLoop() {
        if (tgRecorder != null) {
            tgRunnable = new Runnable() {
                @Override
                public void run() {
                    tgCount++;
                    System.out.println("***** count:" + tgCount);
                    tgRecorder.pushAttention(tgAttention);
                    tgRecorder.pushMeditation(tgMeditation);
                    tgHandler.postDelayed(this, 15000);
                }
            };
            tgHandler.postDelayed(tgRunnable, 0);
        }
    }

    /**
     * private classes
     */
    private class SigninListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (loginAccount.getText().length() * loginPassword.getText().length() == 0) {
                showAlert("PLEASE ENTER ACCOUNT AND PASSWORD");
            } else {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject result = user.signInAndUp(loginAccount.getText().toString(), loginPassword.getText().toString(), URLS.SIGNIN);
                            if (result == null) {
                                showAlert("UNKNOWN ERROR");
                            } else if (result.getInt("code") == 500) {
                                showAlert(result.getString("msg"));
                            } else {
//                                showAlert("SUCCESS");
                                Message message = new Message();
                                message.what = CODES.SIGN_SUCCESS;
                                animationHandler.sendMessage(message);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            showAlert("UNKNOWN ERROR");
                        }
                    }
                }).start();
            }
        }
    }

    private class SignupListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (loginConfirm.getVisibility() == View.GONE) {
                loginConfirm.setVisibility(View.VISIBLE);
            } else if (!loginPassword.getText().toString().equals(loginConfirm.getText().toString())) {
                showAlert("PASSWORDS DO NOT MATCH");
            } else {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            JSONObject result = user.signInAndUp(loginAccount.getText().toString(), loginPassword.getText().toString(), URLS.SIGNUP);
                            if (result == null) {
                                showAlert("UNKNOWN ERROR");
                            } else if (result.getInt("code") == 500) {
                                showAlert(result.getString("msg"));
                            } else {
                                Message message = new Message();
                                message.what = CODES.SIGN_SUCCESS;
                                animationHandler.sendMessage(message);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            showAlert("UNKNOWN ERROR");
                        }
                    }
                }).start();
            }
        }
    }

    private class TimerListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (tgConnected) {
                tgRecorder = new Recorder();
                mainTimer.setVisibility(View.GONE);
                mainStopwatch.setVisibility(View.GONE);
                mainHistory.setVisibility(View.GONE);
                mainStop.setVisibility(View.VISIBLE);
                startLoop();
            } else {
                showAlert("CONNECT TO A THINK GEAR DEVICE BEFORE STARTING A TASK");
            }
        }
    }

    private class StopwatchListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            if (tgConnected) {

            } else {
                showAlert("CONNECT TO A THINK GEAR DEVICE BEFORE STARTING A TASK");
            }
        }
    }

    private class StopListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            tgRecorder.stop();
            tgRecorder.save();
            tgCount = 0;
            tgHandler.removeCallbacks(tgRunnable);
        }
    }

}
