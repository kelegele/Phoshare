package com.kelegele.phoshare.utils;

import android.graphics.Color;
import android.os.CountDownTimer;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.Button;

public class CountDownTimerUtils extends CountDownTimer {
    private Button mButton;

    /**
     * @param btn
     *
     * @param millisInFuture    The number of millis in the future from the call
     *                          to {@link #start()} until the countdown is done and {@link #onFinish()}
     *                          is called.
     * @param countDownInterval The interval along the way to receive
     *                          {@link #onTick(long)} callbacks.
     */
    public CountDownTimerUtils(Button btn, long millisInFuture, long countDownInterval) {
        super(millisInFuture, countDownInterval);
        this.mButton = btn;
    }


    @Override
    public void onTick(long millisUntilFinished) {
        mButton.setClickable(false); //设置不可点击
        mButton.setText(millisUntilFinished / 1000 + "秒后获取");  //设置倒计时时间
        //mButton.setBackgroundColor(Color.parseColor("#cccccc")); //设置按钮为灰色，这时是不能点击的
        SpannableString spannableString = new SpannableString(mButton.getText().toString());  //获取按钮上的文字
        ForegroundColorSpan span = new ForegroundColorSpan(Color.BLACK);
        spannableString.setSpan(span, 0, 2, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);//将倒计时的时间设置为红色
        mButton.setText(spannableString);
    }

    @Override
    public void onFinish() {
        mButton.setText("重新获取");
        mButton.setClickable(true);//重新获得点击
        //mButton.setBackgroundColor(getResources().getColor(R.color.design_default_color_background) );  //还原背景色
    }
}


