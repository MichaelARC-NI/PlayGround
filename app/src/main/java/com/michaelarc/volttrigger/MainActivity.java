package com.michaelarc.volttrigger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.EnumMap;
import java.util.Map;

public final class MainActivity extends Activity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<Trigger, TextView[]> modeButtons = new EnumMap<>(Trigger.class);
    private final Map<Trigger, TextView> coordinateViews = new EnumMap<>(Trigger.class);
    private final Map<Trigger, TextView> modeHints = new EnumMap<>(Trigger.class);

    private SharedPreferences preferences;
    private boolean updatingUi;
    private boolean resumed;

    private Switch mappingSwitch;
    private Switch bubbleSwitch;
    private Switch hapticsSwitch;
    private Switch flashSwitch;
    private TextView accessHint;
    private TextView accessAction;
    private TextView statusPill;
    private View accessDot;
    private TextView durationValue;
    private SeekBar durationSeek;

    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceListener =
            (sharedPreferences, key) -> {
                if (!updatingUi) {
                    refreshUi();
                }
            };

    private final Runnable servicePoll = new Runnable() {
        @Override
        public void run() {
            if (resumed) {
                refreshUi();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = AppPrefs.get(this);
        configureWindow();
        buildUi();
        refreshUi();
    }

    @Override
    protected void onStart() {
        super.onStart();
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener);
    }

    @Override
    protected void onResume() {
        super.onResume();
        resumed = true;
        TriggerAccessibilityService.setHostVisible(true);
        refreshUi();
        if (SettingsUtils.isAccessibilityEnabled(this)
                && !TriggerAccessibilityService.isRunning()) {
            handler.removeCallbacks(servicePoll);
            handler.postDelayed(servicePoll, 800L);
        }
    }

    @Override
    protected void onPause() {
        resumed = false;
        handler.removeCallbacks(servicePoll);
        TriggerAccessibilityService.setHostVisible(false);
        super.onPause();
    }

    @Override
    protected void onStop() {
        preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener);
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    private void configureWindow() {
        Window window = getWindow();
        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.setNavigationBarContrastEnforced(false);
            window.setStatusBarContrastEnforced(false);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        }
    }

    private void buildUi() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setClipToPadding(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setBackgroundColor(color(R.color.bg));
        scrollView.setOnApplyWindowInsetsListener((view, insets) -> {
            int left;
            int top;
            int right;
            int bottom;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets bars = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                left = bars.left;
                top = bars.top;
                right = bars.right;
                bottom = bars.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }
            view.setPadding(left, top + dp(8), right, bottom + dp(16));
            return insets;
        });

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(2), dp(20), dp(18));
        scrollView.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scrollView);

        add(content, buildBrandRow(), 0);
        add(content, buildHeroCard(), 18);
        add(content, buildStatusCard(), 14);

        add(content, sectionLabel("TUS GATILLOS"), 26);
        add(content, buildTriggerCard(Trigger.LEFT), 10);
        add(content, buildTriggerCard(Trigger.RIGHT), 12);

        add(content, sectionLabel("PREFERENCIAS"), 26);
        add(content, buildPreferencesCard(), 10);

        add(content, sectionLabel("GUÍA RÁPIDA"), 26);
        add(content, buildHelpCard(), 10);

        TextView footer = text(
                "Hecho para jugar sin root. Sin red, sin cuentas y sin anuncios.",
                12, color(R.color.text_dim), Typeface.NORMAL);
        footer.setGravity(Gravity.CENTER);
        add(content, footer, 24);
    }

    private View buildBrandRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView logo = text("V", 22, 0xFF180A04, Typeface.BOLD);
        logo.setGravity(Gravity.CENTER);
        logo.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        logo.setBackground(rounded(0xFFFF6B35, 14));
        row.addView(logo, new LinearLayout.LayoutParams(dp(44), dp(44)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(12);
        row.addView(titles, titleParams);

        TextView brand = text("VOLT TRIGGER", 15, color(R.color.text), Typeface.BOLD);
        brand.setLetterSpacing(0.14f);
        titles.addView(brand);

        TextView tagline = text("Gatillos por volumen", 12, color(R.color.text_muted),
                Typeface.NORMAL);
        tagline.setLetterSpacing(0.02f);
        titles.addView(tagline);

        TextView version = text("v1.0", 11, color(R.color.text_muted), Typeface.NORMAL);
        version.setGravity(Gravity.CENTER);
        version.setPadding(dp(11), dp(5), dp(11), dp(5));
        version.setBackgroundResource(R.drawable.bg_pill);
        row.addView(version);
        return row;
    }

    private View buildHeroCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_hero);
        card.setPadding(dp(22), dp(22), dp(22), dp(10));

        TextView badge = text("SIN ROOT  ·  ANDROID 8+", 10, 0xFFBBD1E5, Typeface.BOLD);
        badge.setLetterSpacing(0.14f);
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(dp(12), dp(6), dp(12), dp(6));
        badge.setBackground(rounded(0x2615C3FF, 99, 1, 0x3D35D2FF));
        LinearLayout.LayoutParams badgeParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        card.addView(badge, badgeParams);

        TextView title = text("Convierte Vol+ y Vol−\nen gatillos L / R.", 29,
                color(R.color.text), Typeface.BOLD);
        title.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        title.setLineSpacing(dp(-1), 1f);
        add(card, title, 14);

        TextView body = text(
                "Elige un punto de la pantalla para cada tecla y dispara como en un "
                        + "K40 o K50 Gaming. Toque rápido o pulsación mantenida.",
                14, color(R.color.text_muted), Typeface.NORMAL);
        body.setLineSpacing(dp(3), 1f);
        add(card, body, 10);

        HeroArtView art = new HeroArtView(this);
        LinearLayout.LayoutParams artParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(146));
        artParams.topMargin = dp(8);
        card.addView(art, artParams);
        return card;
    }

    private View buildStatusCard() {
        LinearLayout card = card();

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView label = sectionLabel("ESTADO");
        header.addView(label, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        statusPill = text("FALTA ACCESIBILIDAD", 10, color(R.color.warning), Typeface.BOLD);
        statusPill.setLetterSpacing(0.1f);
        statusPill.setGravity(Gravity.CENTER);
        statusPill.setPadding(dp(10), dp(5), dp(10), dp(5));
        header.addView(statusPill);
        card.addView(header);

        LinearLayout accessRow = new LinearLayout(this);
        accessRow.setOrientation(LinearLayout.HORIZONTAL);
        accessRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams accessParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        accessParams.topMargin = dp(16);
        card.addView(accessRow, accessParams);

        accessDot = new View(this);
        accessRow.addView(accessDot, new LinearLayout.LayoutParams(dp(10), dp(10)));

        LinearLayout accessTexts = new LinearLayout(this);
        accessTexts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams accessTextParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        accessTextParams.leftMargin = dp(11);
        accessTextParams.rightMargin = dp(10);
        accessRow.addView(accessTexts, accessTextParams);

        accessTexts.addView(text("Servicio de accesibilidad", 15, color(R.color.text),
                Typeface.BOLD));

        accessHint = text("Concede acceso para enviar los toques.", 12,
                color(R.color.text_muted), Typeface.NORMAL);
        accessHint.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams hintParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hintParams.topMargin = dp(3);
        accessTexts.addView(accessHint, hintParams);

        accessAction = compactAction("Activar");
        accessAction.setOnClickListener(view -> openAccessibilitySettings());
        accessRow.addView(accessAction);

        addDivider(card);
        mappingSwitch = newSwitch();
        mappingSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
            if (updatingUi) {
                return;
            }
            AppPrefs.setMappingEnabled(this, checked);
            refreshUi();
            if (checked && !SettingsUtils.isAccessibilityEnabled(this)) {
                showAccessibilityDialog();
            }
        });
        add(card, switchRow("Activar mapeo",
                "Consume Vol + / Vol − mientras esté encendido.", mappingSwitch), 6);

        addDivider(card);
        bubbleSwitch = newSwitch();
        bubbleSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
            if (updatingUi) {
                return;
            }
            AppPrefs.setBubbleEnabled(this, checked);
            refreshUi();
            if (checked && !SettingsUtils.isAccessibilityEnabled(this)) {
                showAccessibilityDialog();
            }
        });
        add(card, switchRow("Burbuja en juegos",
                "Abre el editor flotante sin salir del juego.", bubbleSwitch), 6);

        TextView bubbleNote = text(
                "La burbuja V aparece con el mapeo activo. Arrástrala si tapa un control.",
                11, color(R.color.text_dim), Typeface.NORMAL);
        bubbleNote.setLineSpacing(dp(2), 1f);
        add(card, bubbleNote, 8);
        return card;
    }

    private View buildTriggerCard(Trigger trigger) {
        LinearLayout card = card();

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView badge = text(trigger.badge, 20, trigger.color, Typeface.BOLD);
        badge.setTypeface(Typeface.create("sans-serif-black", Typeface.BOLD));
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(rounded(withAlpha(trigger.color, 0x26), 99, 1,
                withAlpha(trigger.color, 0x77)));
        header.addView(badge, new LinearLayout.LayoutParams(dp(46), dp(46)));

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        titleParams.leftMargin = dp(12);
        header.addView(titles, titleParams);

        titles.addView(text(trigger.title, 16, color(R.color.text), Typeface.BOLD));
        TextView key = text(trigger.keyLabel, 12, trigger.color, Typeface.NORMAL);
        key.setLetterSpacing(0.04f);
        LinearLayout.LayoutParams keyParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        keyParams.topMargin = dp(2);
        titles.addView(key, keyParams);
        card.addView(header);

        TextView modeLabel = sectionLabel("MODO");
        add(card, modeLabel, 16);

        LinearLayout modeGroup = new LinearLayout(this);
        modeGroup.setOrientation(LinearLayout.HORIZONTAL);
        modeGroup.setPadding(dp(4), dp(4), dp(4), dp(4));
        modeGroup.setBackground(rounded(0xFF0B1119, 14, 1, color(R.color.line_soft)));
        LinearLayout.LayoutParams groupParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        groupParams.topMargin = dp(7);
        card.addView(modeGroup, groupParams);

        TextView tapButton = modeButton("Toque", trigger, AppPrefs.Mode.TAP);
        TextView holdButton = modeButton("Mantener", trigger, AppPrefs.Mode.HOLD);
        LinearLayout.LayoutParams firstParams = new LinearLayout.LayoutParams(
                0, dp(40), 1f);
        modeGroup.addView(tapButton, firstParams);
        LinearLayout.LayoutParams secondParams = new LinearLayout.LayoutParams(
                0, dp(40), 1f);
        secondParams.leftMargin = dp(4);
        modeGroup.addView(holdButton, secondParams);
        modeButtons.put(trigger, new TextView[]{tapButton, holdButton});

        TextView modeHint = text("", 11, color(R.color.text_dim), Typeface.NORMAL);
        modeHints.put(trigger, modeHint);
        add(card, modeHint, 8);

        addDivider(card);

        LinearLayout targetRow = new LinearLayout(this);
        targetRow.setOrientation(LinearLayout.HORIZONTAL);
        targetRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView targetLabel = text("Punto en pantalla", 13, color(R.color.text_muted),
                Typeface.NORMAL);
        targetRow.addView(targetLabel, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        TextView coordinate = text("10% · 30%", 14, trigger.color, Typeface.BOLD);
        coordinate.setTypeface(Typeface.create("monospace", Typeface.BOLD));
        coordinateViews.put(trigger, coordinate);
        targetRow.addView(coordinate);
        add(card, targetRow, 4);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout.LayoutParams actionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        actionParams.topMargin = dp(13);
        card.addView(actions, actionParams);

        TextView move = actionButton("Mover en pantalla", false, false);
        move.setBackground(ripple(rounded(0xFF182430, 14, 1,
                withAlpha(trigger.color, 0x55))));
        move.setTextColor(trigger.color);
        move.setOnClickListener(view -> openEditor());
        actions.addView(move, new LinearLayout.LayoutParams(0, dp(46), 1f));

        TextView test = actionButton("Probar", false, true);
        test.setBackground(ripple(rounded(0x00000000, 14, 1, color(R.color.line))));
        test.setOnClickListener(view -> testTrigger(trigger));
        LinearLayout.LayoutParams testParams = new LinearLayout.LayoutParams(
                dp(88), dp(46));
        testParams.leftMargin = dp(8);
        actions.addView(test, testParams);

        return card;
    }

    private View buildPreferencesCard() {
        LinearLayout card = card();

        hapticsSwitch = newSwitch();
        hapticsSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
            if (updatingUi) {
                return;
            }
            AppPrefs.setHapticsEnabled(this, checked);
        });
        card.addView(switchRow("Vibración",
                "Un pulso corto al disparar.", hapticsSwitch));

        addDivider(card);
        flashSwitch = newSwitch();
        flashSwitch.setOnCheckedChangeListener((buttonView, checked) -> {
            if (updatingUi) {
                return;
            }
            AppPrefs.setFlashEnabled(this, checked);
        });
        card.addView(switchRow("Destello en el punto",
                "Muestra dónde se envió el toque.", flashSwitch));

        addDivider(card);
        LinearLayout durationHeader = new LinearLayout(this);
        durationHeader.setOrientation(LinearLayout.HORIZONTAL);
        durationHeader.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout durationTexts = new LinearLayout(this);
        durationTexts.setOrientation(LinearLayout.VERTICAL);
        durationHeader.addView(durationTexts, new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        durationTexts.addView(text("Duración del toque", 15, color(R.color.text),
                Typeface.BOLD));
        TextView durationHint = text("Solo para el modo Toque.", 12,
                color(R.color.text_muted), Typeface.NORMAL);
        LinearLayout.LayoutParams durationHintParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        durationHintParams.topMargin = dp(2);
        durationTexts.addView(durationHint, durationHintParams);

        durationValue = text("60 ms", 13, color(R.color.accent), Typeface.BOLD);
        durationValue.setTypeface(Typeface.create("monospace", Typeface.BOLD));
        durationHeader.addView(durationValue);
        add(card, durationHeader, 4);

        durationSeek = new SeekBar(this);
        durationSeek.setMax(230);
        durationSeek.setProgress(AppPrefs.getTapDurationMs(this) - 20);
        tintSeekBar(durationSeek);
        durationSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress + 20;
                if (durationValue != null) {
                    durationValue.setText(value + " ms");
                }
                if (fromUser && !updatingUi) {
                    AppPrefs.setTapDurationMs(MainActivity.this, value);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        LinearLayout.LayoutParams seekParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(34));
        seekParams.topMargin = dp(4);
        card.addView(durationSeek, seekParams);

        addDivider(card);
        TextView reset = actionButton("Restablecer posiciones L / R", false, true);
        reset.setBackground(ripple(rounded(0x00000000, 12, 1,
                withAlpha(color(R.color.danger), 0x55))));
        reset.setTextColor(color(R.color.danger));
        reset.setOnClickListener(view -> confirmResetPositions());
        add(card, reset, 6);
        return card;
    }

    private View buildHelpCard() {
        LinearLayout card = card();
        card.addView(text("De volumen a gatillo en 3 pasos", 18, color(R.color.text),
                Typeface.BOLD));

        add(card, helpStep("1", "Activa la accesibilidad",
                "Ajustes > Accesibilidad > Aplicaciones instaladas > Volt Trigger."), 14);
        add(card, helpStep("2", "Elige modo y posición",
                "Toque para un tap corto. Mantener para un botón presionado."), 12);
        add(card, helpStep("3", "Calibra sobre el juego",
                "Entra al juego, pulsa la burbuja V y arrastra L y R."), 12);

        addDivider(card);

        LinearLayout warning = new LinearLayout(this);
        warning.setOrientation(LinearLayout.HORIZONTAL);
        View bar = new View(this);
        bar.setBackground(rounded(color(R.color.warning), 2));
        warning.addView(bar, new LinearLayout.LayoutParams(dp(3), dp(58)));
        TextView warningText = text(
                "Android puede cancelar un dedo que ya estés apoyando cuando se "
                        + "inyecta un gatillo. Es una limitación del servicio de "
                        + "accesibilidad, no del juego.",
                12, color(R.color.text_muted), Typeface.NORMAL);
        warningText.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams warningParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        warningParams.leftMargin = dp(10);
        warning.addView(warningText, warningParams);
        add(card, warning, 14);

        TextView xiaomi = text(
                "En MIUI / HyperOS: Apps > Volt Trigger > Ahorro de batería > Sin "
                        + "restricciones. Así el servicio no se cierra en segundo plano.",
                12, color(R.color.text_muted), Typeface.NORMAL);
        xiaomi.setLineSpacing(dp(2), 1f);
        add(card, xiaomi, 12);

        TextView appSettings = actionButton("Abrir ajustes de la app", false, true);
        appSettings.setBackground(ripple(rounded(0xFF182430, 14, 1,
                color(R.color.line))));
        appSettings.setOnClickListener(view -> SettingsUtils.openAppSettings(this));
        add(card, appSettings, 14);
        return card;
    }

    private View helpStep(String number, String title, String body) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        TextView badge = text(number, 12, 0xFF180A04, Typeface.BOLD);
        badge.setGravity(Gravity.CENTER);
        badge.setBackground(rounded(color(R.color.accent), 99));
        row.addView(badge, new LinearLayout.LayoutParams(dp(24), dp(24)));

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textParams.leftMargin = dp(11);
        row.addView(texts, textParams);
        texts.addView(text(title, 14, color(R.color.text), Typeface.BOLD));
        TextView description = text(body, 12, color(R.color.text_muted), Typeface.NORMAL);
        description.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descriptionParams.topMargin = dp(2);
        texts.addView(description, descriptionParams);
        return row;
    }

    private void refreshUi() {
        boolean access = SettingsUtils.isAccessibilityEnabled(this);
        boolean running = TriggerAccessibilityService.isRunning();
        boolean mapping = AppPrefs.isMappingEnabled(this);

        updatingUi = true;
        try {
            mappingSwitch.setChecked(mapping);
            bubbleSwitch.setChecked(AppPrefs.isBubbleEnabled(this));
            hapticsSwitch.setChecked(AppPrefs.isHapticsEnabled(this));
            flashSwitch.setChecked(AppPrefs.isFlashEnabled(this));

            int dotColor = access && running
                    ? color(R.color.success)
                    : (access ? color(R.color.warning) : color(R.color.danger));
            accessDot.setBackground(oval(dotColor));

            if (!access) {
                accessHint.setText("Concede acceso para enviar los toques.");
                accessHint.setTextColor(color(R.color.text_muted));
                accessAction.setText("Activar");
                setStatusPill("FALTA ACCESIBILIDAD", color(R.color.warning));
            } else if (!running) {
                accessHint.setText("Servicio habilitado. Esperando conexión…");
                accessHint.setTextColor(color(R.color.warning));
                accessAction.setText("Revisar");
                setStatusPill("INICIANDO", color(R.color.warning));
            } else if (!mapping) {
                accessHint.setText("Servicio activo. Enciende el mapeo para jugar.");
                accessHint.setTextColor(color(R.color.success));
                accessAction.setText("Ajustes");
                setStatusPill("LISTO · APAGADO", color(R.color.text_muted));
            } else {
                accessHint.setText("Servicio activo. Vol + / Vol − ya son gatillos.");
                accessHint.setTextColor(color(R.color.success));
                accessAction.setText("Ajustes");
                setStatusPill("MAPEO ACTIVO", color(R.color.success));
            }

            for (Trigger trigger : Trigger.values()) {
                refreshTriggerMode(trigger);
                TextView coordinate = coordinateViews.get(trigger);
                if (coordinate != null) {
                    coordinate.setText(percent(AppPrefs.getTargetX(this, trigger))
                            + " · " + percent(AppPrefs.getTargetY(this, trigger)));
                }
            }

            int duration = AppPrefs.getTapDurationMs(this);
            durationSeek.setProgress(duration - 20);
            durationValue.setText(duration + " ms");
        } finally {
            updatingUi = false;
        }

        if (access && !running && resumed) {
            handler.removeCallbacks(servicePoll);
            handler.postDelayed(servicePoll, 900L);
        }
    }

    private void refreshTriggerMode(Trigger trigger) {
        TextView[] buttons = modeButtons.get(trigger);
        if (buttons == null) {
            return;
        }
        AppPrefs.Mode mode = AppPrefs.getMode(this, trigger);
        styleModeButton(buttons[0], trigger, mode == AppPrefs.Mode.TAP);
        styleModeButton(buttons[1], trigger, mode == AppPrefs.Mode.HOLD);

        TextView hint = modeHints.get(trigger);
        if (hint != null) {
            hint.setText(mode == AppPrefs.Mode.TAP
                    ? "Un tap corto de " + AppPrefs.getTapDurationMs(this)
                            + " ms en el punto elegido."
                    : "Mantiene el toque mientras pulses " + trigger.keyLabel + ".");
        }
    }

    private void styleModeButton(TextView button, Trigger trigger, boolean selected) {
        if (selected) {
            button.setBackground(rounded(withAlpha(trigger.color, 0x24), 10, 1,
                    withAlpha(trigger.color, 0x88)));
            button.setTextColor(trigger.color);
        } else {
            button.setBackground(rounded(0x00000000, 10));
            button.setTextColor(color(R.color.text_muted));
        }
    }

    private void setStatusPill(String text, int pillColor) {
        statusPill.setText(text);
        statusPill.setTextColor(pillColor);
        statusPill.setBackground(rounded(withAlpha(pillColor, 0x20), 99, 1,
                withAlpha(pillColor, 0x55)));
    }

    private TextView modeButton(String label, Trigger trigger, AppPrefs.Mode mode) {
        TextView button = text(label, 13, color(R.color.text_muted), Typeface.BOLD);
        button.setGravity(Gravity.CENTER);
        button.setClickable(true);
        button.setFocusable(true);
        button.setOnClickListener(view -> {
            if (updatingUi) {
                return;
            }
            AppPrefs.setMode(this, trigger, mode);
            refreshUi();
        });
        styleModeButton(button, trigger, AppPrefs.getMode(this, trigger) == mode);
        return button;
    }

    private LinearLayout switchRow(String title, String subtitle, Switch toggle) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        textParams.rightMargin = dp(12);
        row.addView(texts, textParams);
        texts.addView(text(title, 15, color(R.color.text), Typeface.BOLD));

        TextView description = text(subtitle, 12, color(R.color.text_muted),
                Typeface.NORMAL);
        description.setLineSpacing(dp(2), 1f);
        LinearLayout.LayoutParams descriptionParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        descriptionParams.topMargin = dp(2);
        texts.addView(description, descriptionParams);
        row.addView(toggle);
        return row;
    }

    private void openAccessibilitySettings() {
        showAccessibilityDialog();
    }

    private void showAccessibilityDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Activa la accesibilidad")
                .setMessage("Volt Trigger necesita el servicio de accesibilidad para "
                        + "interceptar Vol + / Vol − y enviar toques a la pantalla.\n\n"
                        + "Ajustes > Accesibilidad > Aplicaciones instaladas > Volt Trigger.\n\n"
                        + "El servicio no lee el contenido de tus apps ni usa Internet.")
                .setPositiveButton("Abrir ajustes", (dialog, which) ->
                        SettingsUtils.openAccessibilitySettings(this))
                .setNegativeButton("Ahora no", null)
                .show();
    }

    private void openEditor() {
        if (!SettingsUtils.isAccessibilityEnabled(this)) {
            showAccessibilityDialog();
            return;
        }
        if (!TriggerAccessibilityService.requestEditor()) {
            toast("El servicio de accesibilidad aún no está listo.");
            handler.postDelayed(this::refreshUi, 700L);
            return;
        }
        toast("Arrastra L y R y pulsa Guardar.");
    }

    private void testTrigger(Trigger trigger) {
        if (!SettingsUtils.isAccessibilityEnabled(this)
                || !TriggerAccessibilityService.isRunning()) {
            showAccessibilityDialog();
            return;
        }
        if (TriggerAccessibilityService.testTrigger(trigger)) {
            toast("Prueba enviada a " + percent(AppPrefs.getTargetX(this, trigger))
                    + " · " + percent(AppPrefs.getTargetY(this, trigger)) + ".");
        }
    }

    private void confirmResetPositions() {
        new AlertDialog.Builder(this)
                .setTitle("Restablecer posiciones")
                .setMessage("L volverá al 10% · 30% y R al 90% · 30% de la pantalla.")
                .setPositiveButton("Restablecer", (dialog, which) -> {
                    AppPrefs.setTarget(this, Trigger.LEFT,
                            Trigger.LEFT.defaultX, Trigger.LEFT.defaultY);
                    AppPrefs.setTarget(this, Trigger.RIGHT,
                            Trigger.RIGHT.defaultX, Trigger.RIGHT.defaultY);
                    refreshUi();
                    toast("Posiciones restablecidas.");
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void addDivider(LinearLayout parent) {
        View divider = new View(this);
        divider.setBackgroundColor(color(R.color.line_soft));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        params.topMargin = dp(14);
        params.bottomMargin = dp(14);
        parent.addView(divider, params);
    }

    private void add(LinearLayout parent, View child, int topMarginDp) {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = dp(topMarginDp);
        parent.addView(child, params);
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setElevation(dp(1));
        return card;
    }

    private TextView sectionLabel(String text) {
        TextView label = text(text, 11, color(R.color.accent), Typeface.BOLD);
        label.setLetterSpacing(0.16f);
        label.setPadding(0, 0, 0, 0);
        return label;
    }

    private TextView text(String value, float sizeSp, int textColor, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(sizeSp);
        view.setTextColor(textColor);
        view.setTypeface(Typeface.create("sans-serif", style));
        view.setIncludeFontPadding(false);
        return view;
    }

    private Switch newSwitch() {
        Switch toggle = new Switch(this);
        toggle.setShowText(false);
        toggle.setButtonTintList(null);
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{}
        };
        toggle.setThumbTintList(new ColorStateList(states,
                new int[]{color(R.color.accent), 0xFF687788}));
        toggle.setTrackTintList(new ColorStateList(states,
                new int[]{0x66FF6B35, 0xFF263445}));
        toggle.setPadding(dp(4), 0, dp(4), 0);
        return toggle;
    }

    private TextView compactAction(String label) {
        TextView view = text(label, 12, color(R.color.text), Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setPadding(dp(12), 0, dp(12), 0);
        view.setClickable(true);
        view.setFocusable(true);
        view.setMinHeight(dp(36));
        view.setBackground(ripple(rounded(0xFF182430, 12, 1, color(R.color.line))));
        return view;
    }

    private TextView actionButton(String label, boolean primary, boolean ghost) {
        TextView view = text(label, 13,
                primary ? 0xFF180A04 : color(R.color.text), Typeface.BOLD);
        view.setGravity(Gravity.CENTER);
        view.setClickable(true);
        view.setFocusable(true);
        view.setPadding(dp(12), 0, dp(12), 0);
        if (primary) {
            view.setBackground(ripple(rounded(color(R.color.accent), 14)));
        } else if (!ghost) {
            view.setBackground(ripple(rounded(0xFF182430, 14, 1, color(R.color.line))));
        }
        return view;
    }

    private void tintSeekBar(SeekBar seekBar) {
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_pressed},
                new int[]{}
        };
        ColorStateList accent = new ColorStateList(states, new int[]{
                color(R.color.accent), color(R.color.accent)});
        seekBar.setProgressTintList(accent);
        seekBar.setThumbTintList(accent);
        seekBar.setProgressBackgroundTintList(new ColorStateList(states, new int[]{
                0xFF263445, 0xFF263445}));
    }

    private Drawable ripple(Drawable content) {
        return new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF), content, null);
    }

    private GradientDrawable rounded(int fillColor, float radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fillColor);
        drawable.setCornerRadius(dpF(radiusDp));
        return drawable;
    }

    private GradientDrawable rounded(int fillColor, float radiusDp,
            int strokeWidthDp, int strokeColor) {
        GradientDrawable drawable = rounded(fillColor, radiusDp);
        drawable.setStroke(dp(strokeWidthDp), strokeColor);
        return drawable;
    }

    private GradientDrawable oval(int fillColor) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(fillColor);
        return drawable;
    }

    private static int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    private String percent(float value) {
        return Math.round(value * 100f) + "%";
    }

    private int color(int resourceId) {
        return getColor(resourceId);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private float dpF(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
