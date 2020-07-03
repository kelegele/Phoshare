package com.kelegele.phoshare.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.kelegele.phoshare.R;
import com.kelegele.phoshare.Utils;
import com.kelegele.phoshare.utils.CountDownTimerUtils;

import butterknife.BindView;
import butterknife.OnClick;
import cn.leancloud.AVUser;
import cn.leancloud.sms.AVSMS;
import cn.leancloud.sms.AVSMSOption;
import cn.leancloud.types.AVNull;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

public class LoginActivity extends BaseDrawerActivity {

    static final int LOGIN = 1;
    static final int REG = 0;

    private int stats = LOGIN;


    @BindView(R.id.etPhonenumber)
    EditText etPhonenumber;
    @BindView(R.id.etPassword)
    EditText etPassword;
    @BindView(R.id.etUserName)
    EditText etUserName;
    @BindView(R.id.etCheck)
    EditText etCheck;
    @BindView(R.id.btnAction)
    Button btnLogin;
    @BindView(R.id.tvSwitch)
    TextView tvSwitch;
    @BindView(R.id.progressBar)
    View progressView;
    @BindView(R.id.actionView)
    View actionView;
    @BindView(R.id.llCheck)
    View llCheck;
    @BindView(R.id.btnCheck)
    Button btnCheck;

    public static boolean checkLogin(Activity openingActivity) {
        if (AVUser.getCurrentUser() == null) {
            loginActivity(openingActivity);
            return false;
        } else {
            return true;
        }
    }

    public static void logOut() {
        AVUser.logOut();
    }

    public static void loginActivity(Activity openingActivity) {
        Intent intent = new Intent(openingActivity, LoginActivity.class);
        openingActivity.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }

    @OnClick(R.id.btnAction)
    public void onActionClick() {
        if (stats == LOGIN) {
            attemptLogin();

        } else if (stats == REG) {
            attemptRegister();
        }
    }

    @OnClick(R.id.btnCheck)
    public void getCheckCode() {
        etPhonenumber.setError(null);

        final String phoneNumber = etPhonenumber.getText().toString();

        if (!Utils.validateMobilePhone(phoneNumber)) {
            etPhonenumber.setError("非法手机号");
            etPhonenumber.requestFocus();
        } else {
            CountDownTimerUtils countDownTimerUtils = new CountDownTimerUtils(btnCheck, 60000, 1000);
            countDownTimerUtils.start();

            AVSMSOption option = new AVSMSOption();
            option.setSignatureName("phoshare"); //设置短信签名名称
            AVSMS.requestSMSCodeInBackground(phoneNumber, option).subscribe(new Observer<AVNull>() {
                @Override
                public void onSubscribe(Disposable disposable) {
                }

                @Override
                public void onNext(AVNull avNull) {
                    Timber.d("Result: succeed to request SMSCode.");
                   Toast.makeText(getApplicationContext(),avNull.toString(),Toast.LENGTH_SHORT).show();

                }

                @Override
                public void onError(Throwable throwable) {
                    Timber.d("Result: failed to request SMSCode. cause:%s", throwable.getMessage());
                }

                @Override
                public void onComplete() {
                }
            });
        }

    }

    @OnClick(R.id.tvSwitch)
    public void onSwitchClick() {
        if (stats == LOGIN) {
            stats = REG;
            llCheck.setVisibility(View.VISIBLE);
            btnLogin.setText("注 册");
            tvSwitch.setText("已有帐号，现在登录");
        } else if (stats == REG) {
            stats = LOGIN;
            llCheck.setVisibility(View.INVISIBLE);
            btnLogin.setText("登 录");
            tvSwitch.setText("没有账号？注册一个");
        }
    }

    private void attemptLogin() {
        etPhonenumber.setError(null);
        etPassword.setError(null);

        final String phoneNumber = etPhonenumber.getText().toString();
        final String password = etPassword.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (password.isEmpty()) {
            etPassword.setError("密码不能为空");
            focusView = etPassword;
            cancel = true;
        }

        if (!Utils.validateMobilePhone(phoneNumber)) {
            etPhonenumber.setError("非法手机号");
            focusView = etPhonenumber;
            cancel = true;
        }

        if (cancel) {
            focusView.requestFocus();
        } else {
            showProgress(true);
            //校验数据
            AVUser.loginByMobilePhoneNumber(phoneNumber, password).subscribe(new Observer<AVUser>() {
                public void onSubscribe(Disposable disposable) {
                }

                public void onNext(AVUser user) {
                    // 登录成功
                    showProgress(false);
                    LoginActivity.this.finish();
                }

                public void onError(Throwable throwable) {
                    // 登录失败（可能是密码错误）
                    // 登录成功
                    showProgress(false);
                    Toast.makeText(getApplicationContext(), "登陆失败", Toast.LENGTH_SHORT).show();
                }

                public void onComplete() {
                }
            });


        }

    }

    private void attemptRegister() {
        etPhonenumber.setError(null);
        etPassword.setError(null);

        final String phoneNumber = etPhonenumber.getText().toString();
        final String passWord = etPassword.getText().toString();
        final String userName = etUserName.getText().toString();
        final String check = etCheck.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (!Utils.validateMobilePhone(phoneNumber)) {
            etPhonenumber.setError("非法手机号");
            focusView = etPhonenumber;
            cancel = true;
        }

        if (!passWord.isEmpty() && !isPasswordValid(passWord)) {
            etPassword.setError("密码大于4位");
            focusView = etPassword;
            cancel = true;
        }

        if (check.isEmpty()) {
            etCheck.setError("此项为必填项");
            focusView = etCheck;
            cancel = true;
        }

        if (userName.isEmpty()) {
            etUserName.setError("此项为必填项");
            focusView = etUserName;
            cancel = true;
        }

        if (passWord.isEmpty()) {
            etPassword.setError("此项为必填项");
            focusView = etPassword;
            cancel = true;
        }

        if (phoneNumber.isEmpty()) {
            etPhonenumber.setError("此项为必填项");
            focusView = etPhonenumber;
            cancel = true;
        }


        if (cancel) {
            focusView.requestFocus();
        } else {
            showProgress(true);
            // 创建实例
            AVUser user = new AVUser();

            user.setUsername(userName);
            user.setPassword(passWord);
            user.setMobilePhoneNumber(phoneNumber);


            user.signUpInBackground().subscribe(new Observer<AVUser>() {
                public void onSubscribe(Disposable disposable) {}
                public void onNext(AVUser user) {
                    // 注册成功
                    System.out.println("注册成功。objectId：" + user.getObjectId());

                    Toast.makeText(getApplicationContext(),user.getUsername() + " 注册成功！",Toast.LENGTH_LONG).show();
//                    attemptLogin();
                    showProgress(false);
                }
                public void onError(Throwable throwable) {
                    // 注册失败（通常是因为用户名已被使用）
                    Toast.makeText(getApplicationContext(),throwable.getMessage(),Toast.LENGTH_LONG).show();
                    showProgress(false);
                }
                public void onComplete() {}
            });


/*短信注册（False）
            user.signUpOrLoginByMobilePhoneInBackground(phoneNumber, check).subscribe(new Observer<AVUser>() {
                public void onSubscribe(Disposable disposable) { }

                public void onNext(AVUser user) {
                    // 注册成功
                    System.out.println("注册成功。objectId：" + user.getObjectId());
                    Toast.makeText(getApplicationContext(),user.getUsername() + " 注册成功！",Toast.LENGTH_LONG).show();
                    showProgress(false);
                }

                public void onError(Throwable throwable) {
                    // 验证码不正确
                    Toast.makeText(getApplicationContext(),throwable.getMessage(),Toast.LENGTH_LONG).show();
                    showProgress(false);
                }

                public void onComplete() { }
            });
*/


        }

    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            actionView.setVisibility(show ? View.GONE : View.VISIBLE);
            actionView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    actionView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            progressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    progressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {

            progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            actionView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

}
