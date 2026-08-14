package com.oplus.oreality.audio;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.audiofx.AudioEffect;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.Window;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.oplus.oreality.audio.data.SettingsRepository;
import com.oplus.oreality.audio.data.SoundProfile;

import java.util.EnumMap;
import java.util.Map;

public final class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final int OREALITY_ORANGE = Color.rgb(224, 109, 32);

    private SettingsRepository settingsRepository;
    private ToggleSwitch masterSwitch;
    private EqualizerView equalizerView;
    private ColorScheme colors;
    private final Map<SoundProfile, ProfileCard> profileViews = new EnumMap<>(SoundProfile.class);
    private SharedPreferences.OnSharedPreferenceChangeListener settingsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        settingsRepository = ((ORealityApplication) getApplication()).getSettingsRepository();
        colors = ColorScheme.fromNightMode(isNightMode());
        applySystemBars();
        handleAudioEffectIntent(getIntent());
        setContentView(createContentView());
        updateUiState();
        observeSettings();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleAudioEffectIntent(intent);
    }

    private View createContentView() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(colors.background);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(42), dp(20), dp(26));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView pageTitle = label("OReality Audio", 28, true);
        pageTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        pageTitle.setIncludeFontPadding(false);
        LinearLayout.LayoutParams pageTitleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pageTitleParams.setMargins(dp(8), 0, 0, dp(18));
        content.addView(pageTitle, pageTitleParams);

        LinearLayout masterCard = card();
        masterCard.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams masterParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        masterParams.setMargins(0, 0, 0, 0);
        content.addView(masterCard, masterParams);

        LinearLayout masterText = new LinearLayout(this);
        masterText.setOrientation(LinearLayout.VERTICAL);
        TextView masterTitle = label("OReality Audio", 22, true);
        masterTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        TextView masterSummary = label(
                "OReality Audio is OPPO Audio Lab's new audio enhancement technology developed "
                        + "to improve audio performance in various scenarios for an immersive sound experience.",
                12,
                false);
        masterSummary.setPadding(0, dp(8), 0, 0);
        masterSummary.setLineSpacing(dp(2), 1f);
        masterText.addView(masterTitle);
        masterText.addView(masterSummary);

        masterSwitch = new ToggleSwitch(this, colors);
        masterSwitch.setOnCheckedChangeListener(isChecked ->
                settingsRepository.setEnabled(isChecked));

        masterCard.addView(masterText, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        LinearLayout.LayoutParams switchParams = new LinearLayout.LayoutParams(dp(50), dp(30));
        switchParams.setMargins(dp(18), 0, 0, 0);
        masterCard.addView(masterSwitch, switchParams);

        TextView profilesTitle = label("Select sound profile", 20, true);
        profilesTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        titleParams.setMargins(dp(8), dp(34), 0, dp(18));
        content.addView(profilesTitle, titleParams);

        GridLayout grid = new GridLayout(this);
        grid.setColumnCount(2);
        content.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        for (SoundProfile profile : SoundProfile.values()) {
            ProfileCard profileCard = profileCard(profile);
            profileViews.put(profile, profileCard);
            grid.addView(profileCard.root);
        }

        LinearLayout eqHeader = new LinearLayout(this);
        eqHeader.setGravity(Gravity.CENTER_VERTICAL);
        eqHeader.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams eqHeaderParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        eqHeaderParams.setMargins(dp(8), dp(6), dp(8), dp(12));
        content.addView(eqHeader, eqHeaderParams);

        TextView eqTitle = label("EQ", 18, true);
        eqTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        eqHeader.addView(eqTitle, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView reset = label("Reset", 14, true);
        reset.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        reset.setTextColor(OREALITY_ORANGE);
        reset.setGravity(Gravity.CENTER);
        reset.setPadding(dp(12), dp(8), dp(12), dp(8));
        reset.setOnClickListener(v -> settingsRepository.resetEqualizerGains(settingsRepository.getProfile()));
        eqHeader.addView(reset);

        equalizerView = new EqualizerView(this, colors);
        equalizerView.setGains(settingsRepository.getEqualizerGains(settingsRepository.getProfile()));
        equalizerView.setOnGainsChangeListener(gains -> settingsRepository.setEqualizerGains(settingsRepository.getProfile(), gains));
        content.addView(equalizerView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(188)));

        return scrollView;
    }

    private void observeSettings() {
        settingsListener = (prefs, key) -> runOnUiThread(this::updateUiState);
        settingsRepository.registerListener(settingsListener);
    }

    private void updateUiState() {
        boolean enabled = settingsRepository.isEnabled();
        SoundProfile selectedProfile = settingsRepository.getProfile();

        if (masterSwitch != null && masterSwitch.isChecked() != enabled) {
            masterSwitch.setChecked(enabled);
        }

        for (Map.Entry<SoundProfile, ProfileCard> entry : profileViews.entrySet()) {
            boolean selected = enabled && entry.getKey() == selectedProfile;
            ProfileCard card = entry.getValue();
            card.root.setEnabled(enabled);
            card.root.setAlpha(enabled ? 1f : 0.55f);
            card.artworkFrame.setBackground(artworkBackground(false));
            card.summary.setTextColor(selected ? OREALITY_ORANGE : colors.primaryText);
            card.artwork.setSelectedState(selected);
        }

        if (equalizerView != null) {
            equalizerView.setEnabled(enabled);
            equalizerView.setAlpha(enabled ? 1f : 0.55f);
            equalizerView.setGains(settingsRepository.getEqualizerGains(settingsRepository.getProfile()));
        }
    }

    private ProfileCard profileCard(SoundProfile profile) {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setOnClickListener(v -> {
            if (settingsRepository.isEnabled()) {
                settingsRepository.setProfile(profile);
            }
        });

        GridLayout.LayoutParams gridParams = new GridLayout.LayoutParams();
        gridParams.width = 0;
        gridParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        gridParams.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        gridParams.setMargins(dp(8), 0, dp(8), dp(26));
        root.setLayoutParams(gridParams);

        FrameLayout artworkFrame = new FrameLayout(this);
        artworkFrame.setClipToOutline(false);
        artworkFrame.setBackground(artworkBackground(false));
        root.addView(artworkFrame, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(124)));

        ProfileArtwork artwork = new ProfileArtwork(this, profile, colors);
        artworkFrame.addView(artwork, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView title = label(profile.getTitle(), 16, true);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(Color.WHITE);
        title.setShadowLayer(dp(2), 0, dp(1), Color.argb(90, 0, 0, 0));
        FrameLayout.LayoutParams overlayParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.LEFT | Gravity.BOTTOM);
        overlayParams.setMargins(dp(16), 0, dp(16), dp(14));
        artworkFrame.addView(title, overlayParams);

        TextView summary = label(shortSummary(profile), 14, true);
        summary.setPadding(0, dp(8), 0, 0);
        summary.setLineSpacing(dp(3), 1f);
        root.addView(summary);

        return new ProfileCard(root, artworkFrame, artwork, summary);
    }

    private String shortSummary(SoundProfile profile) {
        switch (profile) {
            case SMART:
                return "Adaptive sound for every scenario.";
            case MOVIE:
                return "Surround sound with clearer voices.";
            case GAMING:
                return "Sharper details for game audio.";
            case MUSIC:
                return "Balanced vocals and instruments.";
            default:
                return profile.getSummary();
        }
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setPadding(dp(24), dp(24), dp(24), dp(24));
        card.setBackground(cardBackground());
        return card;
    }

    private TextView label(String text, int sp, boolean bright) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(sp);
        view.setTextColor(bright ? colors.primaryText : colors.secondaryText);
        return view;
    }

    private GradientDrawable cardBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(colors.card);
        drawable.setCornerRadius(dp(26));
        return drawable;
    }

    private GradientDrawable artworkBackground(boolean selected) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(colors.artworkSurface);
        drawable.setCornerRadius(dp(22));
        drawable.setStroke(dp(selected ? 3 : 0), selected ? OREALITY_ORANGE : Color.TRANSPARENT);
        return drawable;
    }

    private void applySystemBars() {
        Window window = getWindow();
        window.setStatusBarColor(colors.background);
        window.setNavigationBarColor(colors.background);

        int flags = window.getDecorView().getSystemUiVisibility();
        if (colors.light) {
            flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        } else {
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
        }
        window.getDecorView().setSystemUiVisibility(flags);
    }

    private boolean isNightMode() {
        int nightMode = getResources().getConfiguration().uiMode
                & Configuration.UI_MODE_NIGHT_MASK;
        return nightMode == Configuration.UI_MODE_NIGHT_YES;
    }

    private void handleAudioEffectIntent(Intent intent) {
        if (intent == null
                || !AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL.equals(intent.getAction())) {
            return;
        }

        int sessionId = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, 0);
        if (sessionId > 0) {
            Log.i(TAG, "Display audio effect panel for session " + sessionId);
            ((ORealityApplication) getApplication()).getAudioEffectController()
                    .addAudioSession(sessionId);
        } else {
            Log.i(TAG, "Display audio effect panel without audio session");
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDestroy() {
        if (settingsListener != null) {
            settingsRepository.unregisterListener(settingsListener);
        }
        super.onDestroy();
    }



    private final class EqualizerView extends View {
        private static final int MIN_GAIN = -6;
        private static final int MAX_GAIN = 6;
        private final String[] labels = {"31", "62", "125", "250", "500", "1K", "2K", "4K", "8K", "16K"};
        private final ColorScheme colors;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int[] gains = new int[10];
        private OnGainsChangeListener listener;
        private int activeBand = -1;
        
        EqualizerView(Activity activity, ColorScheme colors) {
            super(activity);
            this.colors = colors;
            setClickable(true);
        }

        void setGains(int[] gains) {
            if (gains == null || gains.length != this.gains.length) {
                return;
            }
            System.arraycopy(gains, 0, this.gains, 0, this.gains.length);
            invalidate();
        }

        void setOnGainsChangeListener(OnGainsChangeListener listener) {
            this.listener = listener;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            float top = dp(18);
            float bottom = h - dp(26);
            float zeroY = gainToY(0, top, bottom);
            float labelX = dp(34);
            float leftInset = dp(54);
            float rightInset = dp(18);
            float usableWidth = Math.max(1f, w - leftInset - rightInset);
            float bandWidth = usableWidth / Math.max(1, gains.length - 1);

            paint.setTextAlign(Paint.Align.RIGHT);
            paint.setTextSize(dp(10));
            paint.setColor(colors.secondaryText);
            canvas.drawText("+6db", labelX, top + dp(4), paint);
            canvas.drawText("0db", labelX, zeroY + dp(4), paint);
            canvas.drawText("-6db", labelX, bottom + dp(4), paint);

            for (int i = 0; i < gains.length; i++) {
                float x = leftInset + bandWidth * i;
                float y = gainToY(gains[i], top, bottom);

                paint.setStrokeWidth(dp(2));
                paint.setStrokeCap(Paint.Cap.ROUND);
                paint.setColor(colors.eqTrack);
                canvas.drawLine(x, top, x, bottom, paint);
                paint.setColor(OREALITY_ORANGE);
                canvas.drawLine(x, y, x, zeroY, paint);

                paint.setStyle(Paint.Style.FILL);
                paint.setColor(colors.card);
                canvas.drawCircle(x, y, dp(10), paint);
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(dp(2));
                paint.setColor(colors.objectShade);
                canvas.drawCircle(x, y, dp(10), paint);
                paint.setStyle(Paint.Style.FILL);

                paint.setTextAlign(Paint.Align.CENTER);
                paint.setTextSize(dp(10));
                paint.setColor(colors.secondaryText);
                canvas.drawText(labels[i], x, h - dp(6), paint);
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (!isEnabled()) {
                return true;
            }

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    setParentScrollEnabled(false);
                    activeBand = findBand(event.getX());
                    updateBand(event.getY(), false);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    updateBand(event.getY(), false);
                    return true;
                case MotionEvent.ACTION_UP:
                    updateBand(event.getY(), true);
                    finishDrag();
                    performClick();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    updateBand(event.getY(), true);
                    finishDrag();
                    return true;
                default:
                    return true;
            }
        }

        @Override
        public boolean performClick() {
            super.performClick();
            return true;
        }

        private int findBand(float x) {
            float leftInset = dp(54);
            float rightInset = dp(18);
            float usableWidth = Math.max(1f, getWidth() - leftInset - rightInset);
            float bandWidth = usableWidth / Math.max(1, gains.length - 1);
            int band = Math.round((x - leftInset) / bandWidth);
            if (band < 0) {
                return 0;
            }
            if (band >= gains.length) {
                return gains.length - 1;
            }
            return band;
        }

        private void updateBand(float y, boolean commit) {
            if (activeBand < 0) {
                return;
            }
            float top = dp(18);
            float bottom = getHeight() - dp(26);
            float clampedY = Math.max(top, Math.min(bottom, y));
            float normalized = 1f - ((clampedY - top) / (bottom - top));
            int gain = Math.round(MIN_GAIN + normalized * (MAX_GAIN - MIN_GAIN));
            gain = Math.max(MIN_GAIN, Math.min(MAX_GAIN, gain));
            if (gains[activeBand] != gain) {
                gains[activeBand] = gain;
                invalidate();
            }
            if (commit && listener != null) {
                listener.onGainsChanged(gains.clone());
            }
        }

        private void finishDrag() {
            activeBand = -1;
            setParentScrollEnabled(true);
        }

        private void setParentScrollEnabled(boolean enabled) {
            ViewParent parent = getParent();
            while (parent != null) {
                parent.requestDisallowInterceptTouchEvent(!enabled);
                parent = parent.getParent();
            }
        }

        private float gainToY(int gain, float top, float bottom) {
            float normalized = (gain - MIN_GAIN) / (float) (MAX_GAIN - MIN_GAIN);
            return bottom - normalized * (bottom - top);
        }
    }

    private interface OnGainsChangeListener {
        void onGainsChanged(int[] gains);
    }

    private final class ToggleSwitch extends View {
        private final ColorScheme colors;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean checked;
        private OnCheckedChangeListener listener;

        ToggleSwitch(Activity activity, ColorScheme colors) {
            super(activity);
            this.colors = colors;
            setClickable(true);
            setOnClickListener(v -> setChecked(!checked, true));
        }

        boolean isChecked() {
            return checked;
        }

        void setChecked(boolean checked) {
            setChecked(checked, false);
        }

        void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
            this.listener = listener;
        }

        private void setChecked(boolean checked, boolean fromUser) {
            if (this.checked == checked) {
                return;
            }
            this.checked = checked;
            invalidate();
            if (fromUser && listener != null) {
                listener.onCheckedChanged(checked);
            }
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            float pad = dp(3);
            float radius = h / 2f;

            paint.setStyle(Paint.Style.FILL);
            paint.setColor(checked ? OREALITY_ORANGE : colors.switchTrackOff);
            canvas.drawRoundRect(new RectF(0, 0, w, h), radius, radius, paint);

            if (checked) {
                paint.setColor(Color.argb(55, 0, 0, 0));
                canvas.drawRoundRect(new RectF(0, 0, w, h), radius, radius, paint);
            }

            float thumbRadius = (h - pad * 2f) / 2f;
            float thumbX = checked ? w - pad - thumbRadius : pad + thumbRadius;
            float thumbY = h / 2f;
            paint.setColor(Color.argb(55, 0, 0, 0));
            canvas.drawCircle(thumbX, thumbY + dp(1), thumbRadius, paint);
            paint.setColor(checked ? colors.switchThumbOn : colors.switchThumbOff);
            canvas.drawCircle(thumbX, thumbY, thumbRadius, paint);
        }
    }

    private interface OnCheckedChangeListener {
        void onCheckedChanged(boolean checked);
    }

    private final class ProfileArtwork extends View {
        private final SoundProfile profile;
        private final ColorScheme colors;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private boolean selected;

        ProfileArtwork(Activity activity, SoundProfile profile, ColorScheme colors) {
            super(activity);
            this.profile = profile;
            this.colors = colors;
        }

        void setSelectedState(boolean selected) {
            this.selected = selected;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            drawBackdrop(canvas, w, h);

            switch (profile) {
                case SMART:
                    drawSmart(canvas, w, h);
                    break;
                case MOVIE:
                    drawMovie(canvas, w, h);
                    break;
                case GAMING:
                    drawGaming(canvas, w, h);
                    break;
                case MUSIC:
                    drawMusic(canvas, w, h);
                    break;
                default:
                    drawSmart(canvas, w, h);
                    break;
            }
        }

        private void drawBackdrop(Canvas canvas, float w, float h) {
            float stroke = selected ? dp(3) : 0;
            float inset = stroke / 2f;
            RectF bounds = new RectF(inset, inset, w - inset, h - inset);
            float radius = dp(22);

            paint.setStyle(Paint.Style.FILL);
            paint.setShader(new LinearGradient(0, 0, w, h,
                    selected ? colors.artworkStartSelected : colors.artworkStart,
                    selected ? colors.artworkEndSelected : colors.artworkEnd,
                    Shader.TileMode.CLAMP));
            canvas.drawRoundRect(bounds, radius, radius, paint);
            paint.setShader(null);

            if (selected) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(stroke);
                paint.setColor(OREALITY_ORANGE);
                canvas.drawRoundRect(bounds, radius, radius, paint);
                paint.setStyle(Paint.Style.FILL);
            }
        }

        private void drawIconBase(Canvas canvas, float w, float h) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(colors.objectLight);
            canvas.drawCircle(w * 0.50f, h * 0.42f, dp(30f), paint);
        }

        private void drawSmart(Canvas canvas, float w, float h) {
            drawIconBase(canvas, w, h);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(dp(5));
            paint.setColor(colors.objectShade);
            float center = w * 0.50f;
            float top = h * 0.27f;
            float bottom = h * 0.58f;
            canvas.drawLine(center - dp(16), top + dp(18), center - dp(16), bottom - dp(18), paint);
            paint.setColor(OREALITY_ORANGE);
            canvas.drawLine(center - dp(8), top + dp(10), center - dp(8), bottom - dp(10), paint);
            paint.setColor(colors.objectShade);
            canvas.drawLine(center, top + dp(5), center, bottom - dp(5), paint);
            paint.setColor(OREALITY_ORANGE);
            canvas.drawLine(center + dp(8), top + dp(10), center + dp(8), bottom - dp(10), paint);
            paint.setColor(colors.objectShade);
            canvas.drawLine(center + dp(16), top + dp(18), center + dp(16), bottom - dp(18), paint);
        }

        private void drawMovie(Canvas canvas, float w, float h) {
            drawIconBase(canvas, w, h);
            float cx = w * 0.50f;
            float cy = h * 0.42f;

            // Camera body
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(colors.objectShade);
            RectF body = new RectF(cx - dp(24f), cy - dp(14f), cx + dp(7f), cy + dp(14f));
            canvas.drawRoundRect(body, dp(7.5f), dp(7.5f), paint);

            // Lens
            Path lens = new Path();
            lens.moveTo(cx + dp(7f), cy - dp(7f));
            lens.lineTo(cx + dp(23f), cy - dp(13.5f));
            lens.lineTo(cx + dp(23f), cy + dp(13.5f));
            lens.lineTo(cx + dp(7f), cy + dp(7f));
            lens.close();
            canvas.drawPath(lens, paint);

            // Play icon
            paint.setColor(OREALITY_ORANGE);
            Path play = new Path();
            play.moveTo(cx - dp(11.5f), cy - dp(7.5f));
            play.lineTo(cx - dp(11.5f), cy + dp(7.5f));
            play.lineTo(cx - dp(1.5f), cy);
            play.close();
            canvas.drawPath(play, paint);
        }

        private void drawGaming(Canvas canvas, float w, float h) {
            drawIconBase(canvas, w, h);
            float cx = w * 0.50f;
            float cy = h * 0.42f;

            // Controller body & handles
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(colors.objectShade);
            RectF body = new RectF(cx - dp(24f), cy - dp(13f), cx + dp(24f), cy + dp(9f));
            canvas.drawRoundRect(body, dp(11.5f), dp(11.5f), paint);
            canvas.drawCircle(cx - dp(16.5f), cy + dp(9f), dp(9f), paint);
            canvas.drawCircle(cx + dp(16.5f), cy + dp(9f), dp(9f), paint);

            // D-Pad
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(dp(3.2f));
            paint.setColor(colors.objectLight);
            float dpadX = cx - dp(16f);
            float dpadY = cy - dp(2.5f);
            canvas.drawLine(dpadX, dpadY - dp(5f), dpadX, dpadY + dp(5f), paint);
            canvas.drawLine(dpadX - dp(5f), dpadY, dpadX + dp(5f), dpadY, paint);

            // Action Buttons
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(OREALITY_ORANGE);
            float buttonsX = cx + dp(15.5f);
            float buttonsY = cy - dp(3f);
            float buttonGap = dp(4.2f);
            float buttonRadius = dp(2.8f);
            canvas.drawCircle(buttonsX, buttonsY - buttonGap, buttonRadius, paint);
            canvas.drawCircle(buttonsX + buttonGap, buttonsY, buttonRadius, paint);
            canvas.drawCircle(buttonsX, buttonsY + buttonGap, buttonRadius, paint);
            canvas.drawCircle(buttonsX - buttonGap, buttonsY, buttonRadius, paint);

            // Center status indicator
            paint.setColor(colors.objectLight);
            canvas.drawCircle(cx, cy - dp(4f), dp(2.5f), paint);
        }

        private void drawMusic(Canvas canvas, float w, float h) {
            drawIconBase(canvas, w, h);
            float cx = w * 0.50f;
            float cy = h * 0.42f;

            // Double music note
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(4.4f));
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(colors.objectShade);
            canvas.drawLine(cx - dp(4f), cy - dp(12f), cx - dp(4f), cy + dp(12f), paint);
            canvas.drawLine(cx + dp(15f), cy - dp(17f), cx + dp(15f), cy + dp(8f), paint);
            canvas.drawLine(cx - dp(4f), cy - dp(12f), cx + dp(15f), cy - dp(17f), paint);

            paint.setStyle(Paint.Style.FILL);
            canvas.drawOval(new RectF(cx - dp(18f), cy + dp(9f), cx - dp(3f), cy + dp(21f)), paint);
            canvas.drawOval(new RectF(cx + dp(2f), cy + dp(4f), cx + dp(17f), cy + dp(16f)), paint);

            // Sound vibe arcs
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(OREALITY_ORANGE);
            paint.setStrokeWidth(dp(3.4f));
            paint.setStrokeCap(Paint.Cap.ROUND);
            RectF arc1 = new RectF(cx - dp(18f), cy - dp(2f), cx - dp(4f), cy + dp(14f));
            canvas.drawArc(arc1, 205, 70, false, paint);
            RectF arc2 = new RectF(cx - dp(22f), cy - dp(6f), cx - dp(2f), cy + dp(16f));
            canvas.drawArc(arc2, 205, 72, false, paint);
            paint.setStyle(Paint.Style.FILL);
        }

    }

    private static final class ProfileCard {
        final LinearLayout root;
        final FrameLayout artworkFrame;
        final ProfileArtwork artwork;
        final TextView summary;

        ProfileCard(LinearLayout root, FrameLayout artworkFrame, ProfileArtwork artwork, TextView summary) {
            this.root = root;
            this.artworkFrame = artworkFrame;
            this.artwork = artwork;
            this.summary = summary;
        }
    }

    private static final class ColorScheme {
        final boolean light;
        final int background;
        final int card;
        final int artworkSurface;
        final int primaryText;
        final int secondaryText;
        final int switchThumbOn;
        final int switchThumbOff;
        final int switchTrackOn;
        final int switchTrackOff;
        final int artworkStart;
        final int artworkEnd;
        final int artworkStartSelected;
        final int artworkEndSelected;
        final int objectLight;
        final int objectShade;
        final int eqTrack;

        private ColorScheme(
                boolean light,
                int background,
                int card,
                int artworkSurface,
                int primaryText,
                int secondaryText,
                int switchThumbOn,
                int switchThumbOff,
                int switchTrackOn,
                int switchTrackOff,
                int artworkStart,
                int artworkEnd,
                int artworkStartSelected,
                int artworkEndSelected,
                int objectLight,
                int objectShade,
                int eqTrack) {
            this.light = light;
            this.background = background;
            this.card = card;
            this.artworkSurface = artworkSurface;
            this.primaryText = primaryText;
            this.secondaryText = secondaryText;
            this.switchThumbOn = switchThumbOn;
            this.switchThumbOff = switchThumbOff;
            this.switchTrackOn = switchTrackOn;
            this.switchTrackOff = switchTrackOff;
            this.artworkStart = artworkStart;
            this.artworkEnd = artworkEnd;
            this.artworkStartSelected = artworkStartSelected;
            this.artworkEndSelected = artworkEndSelected;
            this.objectLight = objectLight;
            this.objectShade = objectShade;
            this.eqTrack = eqTrack;
        }

        static ColorScheme fromNightMode(boolean night) {
            if (night) {
                return new ColorScheme(
                        false,
                        Color.rgb(18, 18, 18),
                        Color.rgb(48, 48, 48),
                        Color.rgb(40, 40, 40),
                        Color.rgb(242, 242, 242),
                        Color.rgb(168, 168, 168),
                        Color.WHITE,
                        Color.rgb(148, 148, 148),
                        Color.rgb(180, 96, 24),
                        Color.rgb(72, 72, 72),
                        Color.rgb(206, 224, 234),
                        Color.rgb(226, 228, 238),
                        Color.rgb(246, 232, 235),
                        Color.rgb(230, 226, 238),
                        Color.rgb(238, 238, 236),
                        Color.rgb(76, 72, 82),
                        Color.rgb(68, 68, 68));
            }

            return new ColorScheme(
                    true,
                    Color.rgb(250, 248, 245),
                    Color.WHITE,
                    Color.WHITE,
                    Color.rgb(24, 22, 20),
                    Color.rgb(104, 98, 92),
                    Color.WHITE,
                    Color.WHITE,
                    Color.rgb(247, 198, 157),
                    Color.rgb(222, 216, 208),
                    Color.rgb(224, 238, 246),
                    Color.rgb(242, 242, 248),
                    Color.rgb(255, 239, 230),
                    Color.rgb(238, 235, 248),
                    Color.rgb(246, 246, 242),
                    Color.rgb(96, 92, 102),
                    Color.rgb(224, 220, 214));
        }
    }
}
