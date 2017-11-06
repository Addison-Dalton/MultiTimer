package com.addisondalton.multitimer;

import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.CountDownTimer;
import android.os.Vibrator;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity{

    //class for each instance of a timer
    private class MultiTimer{
        //three EditText views for hours, minutes, seconds
        private EditText textHours;
        private EditText textMinutes;
        private EditText textSeconds;
        //the EditText for the timer's title/name
        private EditText timerTitle;
        //timer button
        private Button timerButton;
        //linearLayout inwhich everything above sits within
        private LinearLayout timerLayout;
        //variables to hold the hours, minutes, seconds in long format from the EditText views
        private long hours;
        private long minutes;
        private long seconds;
        //holds milliseconds, converted from previous three variables
        private long millisecondsLeft;
        //holds the literal string value of hours, minutes, seconds.
        //used to check that the EditText views aren't empty and get converted to long format variables above
        private String stringHours;
        private String stringMinutes;
        private String stringSeconds;
        //holds the initially entered hours, minutes, seconds for a timer. Used to reset timer.
        private String initialHours;
        private String initialMinutes;
        private String initialSeconds;
        //the timer
        private CountDownTimer timer;
        //holds the alarm sound when timer finishes
        private Ringtone alarmSound;
        //used to play the alarmSound
        private AudioManager audioManager;
        //holds the user's original volume so it can be set back when timer is canceled.
        private int userVolume;
        //used to vibrate
        private Vibrator vibrator;

        private int tickUnder1000 = 0;
        private Boolean firstTick = true;

        private int timerNotStartedColor = Color.parseColor("#a3a3a3");
        private int timerRunningColor = Color.parseColor("#4faa00");
        private int timerPausedColor = Color.parseColor("#d6e502");
        private int timerEndedColor = Color.parseColor("#c41a07");

        //TODO . Solution to stop skips when paused causes a "0" to briefly be shown before seconds goes to 59 after a minute passes. (Maybe fix)
        //class for each timer, it takes the three EditTexts and button.
        private MultiTimer(int hours, int minutes, int seconds, int timerButton, int timerTitle, int timerLayout){
            //assign all EditText
            this.textHours = findViewById(hours); this.textMinutes = findViewById(minutes); this.textSeconds = findViewById(seconds);

            this.timerButton = findViewById(timerButton);
            this.timerLayout = findViewById(timerLayout);
            this.timerTitle = findViewById(timerTitle);

            //addTextChangedListener to the three EditText
            this.textHours.addTextChangedListener(new TimeTextWatcher(this.textHours, true));
            this.textMinutes.addTextChangedListener(new TimeTextWatcher(this.textMinutes, false));
            this.textSeconds.addTextChangedListener(new TimeTextWatcher(this.textSeconds, false));

            //assign audioManager to set volume to max when needed to sound alarm.
            audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
            this.userVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);

            //assign vibrator for use in reset and alarm
            this.vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

            this.timerButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MultiTimer.this.stringHours = MultiTimer.this.textHours.getText().toString();
                    MultiTimer.this.stringMinutes = MultiTimer.this.textMinutes.getText().toString();
                    MultiTimer.this.stringSeconds = MultiTimer.this.textSeconds.getText().toString();

                    //check that timer is not started
                    if(!(stringHours.matches("") && stringMinutes.matches("") && stringSeconds.matches(""))) {
                        if(MultiTimer.this.timerButton.getText().toString().matches("Start")){
                            //set any empty EditText to 0
                            if(stringHours.matches("")){
                                MultiTimer.this.textHours.setText("0");
                                stringHours = MultiTimer.this.textHours.getText().toString();
                            }
                            if(stringMinutes.matches("")){
                                MultiTimer.this.textMinutes.setText("0");
                                stringMinutes = MultiTimer.this.textMinutes.getText().toString();
                            }
                            if(stringSeconds.matches("")){
                                MultiTimer.this.textSeconds.setText("0");
                                stringSeconds = MultiTimer.this.textSeconds.getText().toString();
                            }

                            //keep the initial hours, minutes, and seconds so the timer can reset to those values
                            MultiTimer.this.initialHours = stringHours;
                            MultiTimer.this.initialMinutes = stringMinutes;
                            MultiTimer.this.initialSeconds = stringSeconds;

                            //start the timer
                            MultiTimer.this.startTimer();

                        }else if(MultiTimer.this.timerButton.getText().toString().matches("Pause")){//if pause is pressed
                            MultiTimer.this.pauseTimer();
                        }else if(MultiTimer.this.timerButton.getText().toString().matches("Resume")){//if resume is pressed
                            MultiTimer.this.resumeTimer();
                        }else if(MultiTimer.this.timerButton.getText().toString().matches("Stop")){ //if stop is pressed
                            MultiTimer.this.stopAlarm();
                            MultiTimer.this.defaultTimer();
                        }
                    }else{ //tell the user to input a time to start the timer.
                        Toast.makeText(getApplicationContext(), "Input a time to start.", Toast.LENGTH_SHORT).show();
                    }
                }
            });

            this.timerButton.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {

                    if(!MultiTimer.this.timerButton.getText().toString().matches("Start")){
                        MultiTimer.this.resetTimer();
                        return true;
                    }
                    return false;
                }
            });
        }

        private void enableEditText(){
            this.textHours.setEnabled(true);
            this.textMinutes.setEnabled(true);
            this.textSeconds.setEnabled(true);
        }

        //so the user cannot enter new time while the timer is running
        private void disableEditText(){
            this.textHours.setEnabled(false);
            this.textMinutes.setEnabled(false);
            this.textSeconds.setEnabled(false);
        }

        private void updateEditText(){
            this.textHours.setText(Long.toString(this.hours));
            this.textMinutes.setText(Long.toString(this.minutes));
            this.textSeconds.setText(Long.toString(this.seconds));
        }

        private void setTimerEditTextToZero(){
            this.textHours.setText("0");
            this.textMinutes.setText("0");
            this.textSeconds.setText("0");
        }

        private void startTimer(){
            //set any empty EditText to 0
            if(stringHours.matches("")){
                this.textHours.setText("0");
                this.stringHours = this.textHours.getText().toString();
            }
            if(stringMinutes.matches("")){
                this.textMinutes.setText("0");
                this.stringMinutes = this.textMinutes.getText().toString();
            }
            if(stringSeconds.matches("")){
                this.textSeconds.setText("0");
                this.stringSeconds = this.textSeconds.getText().toString();
            }

            //disable the EditText while timer is running
            this.disableEditText();

            //set button text to "pause"
            this.timerButton.setText(R.string.button_pause);

            //set tickUnder1000 to 0 so the EditText does not update until the first second has passed
            this.tickUnder1000 = 0;

            //get the hours, minutes, seconds from respective EditText views
            this.hours = Long.parseLong(stringHours);
            this.minutes =  Long.parseLong(stringMinutes);
            this.seconds =  Long.parseLong(stringSeconds);

            this.timerLayout.setBackgroundColor(timerRunningColor);
            this.millisecondsLeft = TimeUnit.HOURS.toMillis(this.hours) + TimeUnit.MINUTES.toMillis(this.minutes) + TimeUnit.SECONDS.toMillis(this.seconds);
            this.timer = new CountDownTimer(this.millisecondsLeft, 100){
                @Override
                public void onTick(long millisUntilFinished){
                    MultiTimer.this.performTick(millisUntilFinished);
                }
                @Override
                public void onFinish(){
                    MultiTimer.this.endTimer();
                }
            }.start();

        }

        private void pauseTimer(){
            this.timer.cancel();
            this.timerButton.setText(R.string.button_resume);
            this.timerLayout.setBackgroundColor(timerPausedColor);
            this.enableEditText();
        }

        private void resumeTimer(){
            this.startTimer();
            this.timerButton.setText(R.string.button_pause);
        }

        private void resetTimer(){
            this.vibrator.vibrate(1000);
            this.timer.cancel();
            this.timerButton.setText(R.string.button_start);
            this.timerLayout.setBackgroundColor(timerNotStartedColor);
            this.textHours.setText(initialHours);
            this.textMinutes.setText(initialMinutes);
            this.textSeconds.setText(initialSeconds);

            this.enableEditText();
        }

        private void endTimer(){
            this.timer.cancel();
            this.setTimerEditTextToZero(); //the final tick does not go through, so this is used to set timer EditText views to 0
            this.timerButton.setText(R.string.button_stop);
            this.timerLayout.setBackgroundColor(timerEndedColor);

            this.playAlarm();
        }

        private void playAlarm(){
            try{
                audioManager.setStreamVolume(AudioManager.STREAM_RING, audioManager.getStreamMaxVolume(AudioManager.STREAM_RING), 0);
                String uri = "android.resource://" + getPackageName() + "/" + R.raw.multitimer_alarm_sound;
                Uri alarm = Uri.parse(uri);
                this.alarmSound = RingtoneManager.getRingtone(getApplicationContext(), alarm);
                this.alarmSound.play();
            } catch (Exception e){
                e.printStackTrace();
            }


        }
        //this is called to stop the alarm sound
        private void stopAlarm(){
            this.alarmSound.stop();
            audioManager.setStreamVolume(AudioManager.STREAM_RING, this.userVolume, 0);
        }

        //called to reset timer to default condition
        private void defaultTimer(){
            this.textHours.setText("");
            this.textMinutes.setText("");
            this.textSeconds.setText("");
            this.timerTitle.setText("");

            this.textHours.setHint(R.string.hours);
            this.textMinutes.setHint(R.string.minutes);
            this.textSeconds.setHint(R.string.seconds);
            this.timerTitle.setHint(R.string.timer_title);

            this.timerButton.setText(R.string.button_start);
            this.timerLayout.setBackgroundColor(timerNotStartedColor);
            this.enableEditText();
        }


        private void performTick(long millisUntilFinished){
            MultiTimer.this.millisecondsLeft = millisUntilFinished;
            if(this.tickUnder1000 < 800){
                this.tickUnder1000+=100;
            }else{
                MultiTimer.this.seconds = Math.round((float) MultiTimer.this.millisecondsLeft / 1000.0f) % 60;
                MultiTimer.this.minutes = (MultiTimer.this.millisecondsLeft / (1000 * 60)) % 60;
                MultiTimer.this.hours = (MultiTimer.this.millisecondsLeft / (1000 * 60 * 60)) % 60;
                MultiTimer.this.updateEditText(); //updates each timer EditText view to their current respective times
            }
        }
    }

    //custom TextWatcher class for each time EditText
    private class TimeTextWatcher implements TextWatcher{
        private EditText editText;
        private Boolean isHours; //used to check if the time is hours, instead of minutes/seconds
        private TimeTextWatcher(EditText editText, Boolean isHours){
            this.editText = editText;
            this.isHours = isHours;
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2){}
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

        public void afterTextChanged(Editable editable){
            this.editText.removeTextChangedListener(this); //to prevent textChange loop
            String text = editable.toString();

            //if the text is empty, don't execute the following
            if(!text.matches("")){
                int time = Integer.parseInt(text);
                time = timeRange(time);
                this.editText.setText(Integer.toString(time));
            }

            this.editText.addTextChangedListener(this);
            this.editText.setSelection(this.editText.getText().length());

        }
        //check if the time entered is in hours or minutes/seconds, then make sure it is withing acceptable range. Change to maxRange if not.
        private int timeRange(int time){
            int maxHours = 99;
            int maxMinSec = 59;
            if(this.isHours){
                if(time > maxHours){
                   return maxHours;
                }else{
                    return time;
                }
            }else{
                if(time > maxMinSec){
                    return maxMinSec;
                }else{
                    return time;
                }
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MultiTimer timerTL = new MultiTimer(R.id.hoursTL, R.id.minutesTL, R.id.secondsTL, R.id.timerStartTL, R.id.timerNameTL, R.id.linearLayoutTL);
        MultiTimer timerTR = new MultiTimer(R.id.hoursTR, R.id.minutesTR, R.id.secondsTR, R.id.timerStartTR, R.id.timerNameTR, R.id.linearLayoutTR);
        MultiTimer timerML = new MultiTimer(R.id.hoursML, R.id.minutesML, R.id.secondsML, R.id.timerStartML, R.id.timerNameML, R.id.linearLayoutML);
        MultiTimer timerMR = new MultiTimer(R.id.hoursMR, R.id.minutesMR, R.id.secondsMR, R.id.timerStartMR, R.id.timerNameMR, R.id.linearLayoutMR);
        MultiTimer timerBL = new MultiTimer(R.id.hoursBL, R.id.minutesBL, R.id.secondsBL, R.id.timerStartBL, R.id.timerNameBL, R.id.linearLayoutBL);
        MultiTimer timerBR = new MultiTimer(R.id.hoursBR, R.id.minutesBR, R.id.secondsBR, R.id.timerStartBR, R.id.timerNameBR, R.id.linearLayoutBR);

    }
}
