package io.github.budgetninja.fairwellandroid;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.github.paolorotolo.appintro.AppIntro;

/**
 *Created by Issac on 11/23/2015.
 */
public class MyIntro extends AppIntro {

    // Please DO NOT override onCreate. Use init
    @Override
    public void init(Bundle savedInstanceState) {

/*         Add your slide's fragments here
         AppIntro will automatically generate the dots indicator and buttons.
             addSlide(first_fragment);
             addSlide(second_fragment);
             addSlide(third_fragment);
             addSlide(fourth_fragment);

         Instead of fragments, you can also use our default slide
         Just set a title, description, background and image. AppIntro will do the rest
        addSlide(AppIntroFragment.newInstance("Hello", "My name is Issac\nAwesome as always!", R.drawable.applogo, R.color.coolActionBar));
        addSlide(AppIntroFragment.newInstance("YOLO", "Second page owning!", R.drawable.simple_background_small, R.color.coolActionBar));
        addSlide(AppIntroFragment.newInstance("DONE", "Whatssupp", R.drawable.ninja, R.color.coolActionBar));*/

        addSlide(SampleSlide.newInstance(R.layout.intro));
        addSlide(SampleSlide.newInstance(R.layout.intro2));
        addSlide(SampleSlide.newInstance(R.layout.intro3));
        addSlide(SampleSlide.newInstance(R.layout.intro4));

        // OPTIONAL METHODS
        // Override bar/separator color
        //setBarColor(Color.parseColor("#3F51B5"));
        //setSeparatorColor(Color.parseColor("#2196F3"));

        // Hide Skip/Done button
        showSkipButton(true);
        showDoneButton(true);

        // Turn vibration on and set intensity
        // NOTE: you will probably need to ask VIBRATE permesssion in Manifest
        //setVibrate(true);
        //setVibrateIntensity(30);
    }

    @Override
    public void onSkipPressed() {
        // Do something when users tap on Skip button.
        loadMainActivity();
    }

    private void loadMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void getStarted(View v) {
        loadMainActivity();
    }

    @Override
    public void onDonePressed() {
        loadMainActivity();
        // Do something when users tap on Done button.
    }

    public static class SampleSlide extends Fragment {

        private static final String ARG_LAYOUT_RES_ID = "layoutResId";

        public static SampleSlide newInstance(int layoutResId) {
            SampleSlide sampleSlide = new SampleSlide();
            Bundle args = new Bundle();
            args.putInt(ARG_LAYOUT_RES_ID, layoutResId);
            sampleSlide.setArguments(args);

            return sampleSlide;
        }

        private int layoutResId;

        public SampleSlide() {

        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (getArguments() != null && getArguments().containsKey(ARG_LAYOUT_RES_ID)) {
                layoutResId = getArguments().getInt(ARG_LAYOUT_RES_ID);
            }
        }

        @Nullable @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            return inflater.inflate(layoutResId, container, false);
        }
    }
}