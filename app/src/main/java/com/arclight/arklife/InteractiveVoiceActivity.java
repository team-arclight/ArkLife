package com.arclight.arklife;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobileconnectors.lex.interactionkit.Response;
import com.amazonaws.mobileconnectors.lex.interactionkit.config.InteractionConfig;
import com.amazonaws.mobileconnectors.lex.interactionkit.ui.InteractiveVoiceView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Locale;
import java.util.Map;

public class InteractiveVoiceActivity extends Activity
        implements InteractiveVoiceView.InteractiveVoiceListener {
    private static final String TAG = "VoiceActivity";
    private static final int REQUEST_RECORDING_PERMISSIONS_RESULT = 75;
    private InteractiveVoiceView voiceView;
    private TextView transcriptTextView;
    private TextView responseTextView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interactive_voice);
        transcriptTextView = findViewById(R.id.transcriptTextView);
        responseTextView = findViewById(R.id.responseTextView);
        init();
    }

    @Override
    public void onBackPressed() {
        exit();
    }

    private void init() {
        voiceView = findViewById(R.id.voiceInterface);
        voiceView.setInteractiveVoiceListener(this);
        // Starting with Marshmallow we need to explicitly ask if we can record audio
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) !=
                    PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_RECORDING_PERMISSIONS_RESULT);
            }
        }
        AWSMobileClient.getInstance().initialize(this, new Callback<UserStateDetails>() {
            @Override
            public void onResult(UserStateDetails result) {
                Log.d(TAG, "onResult: ");
                voiceView.getViewAdapter().setCredentialProvider(AWSMobileClient.getInstance());
                AWSMobileClient.getInstance().getCredentials();

                String identityId = AWSMobileClient.getInstance().getIdentityId();
                String botName = null;
                String botAlias = null;
                String botRegion = null;
                JSONObject lexConfig;
                try {
                    // AWSConfiguration.optJsonObject(String name) takes 1st key in main which is
                    // config (e.g. "Default")
                    lexConfig = AWSMobileClient.getInstance().getConfiguration().optJsonObject("Lex");
                    // lexConfig = lexConfig.getJSONObject(lexConfig.keys().next());

                    botName = lexConfig.getString("Name");
                    botAlias = lexConfig.getString("Alias");
                    botRegion = lexConfig.getString("Region");
                } catch (JSONException e) {
                    Log.e(TAG, "onResult: Failed to read configuration", e);
                }

                InteractionConfig lexInteractionConfig = new InteractionConfig(
                        botName,
                        botAlias,
                        identityId);
                voiceView.getViewAdapter().setInteractionConfig(lexInteractionConfig);
                voiceView.getViewAdapter().setAwsRegion(botRegion);
            }

            @Override
            public void onError(Exception e) {
                Log.e(TAG, "onError: ", e);
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_RECORDING_PERMISSIONS_RESULT) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(getApplicationContext(),
                        "ArkLife will not be able to use the voice feature", Toast.LENGTH_SHORT).show();
                onBackPressed();
            }
        }
    }

    private void exit() {
        finish();
    }

    @Override
    public void dialogReadyForFulfillment(final Map<String, String> slots, final String intent) {
        Log.d(TAG, String.format(
                Locale.US,
                "Dialog ready for fulfillment:\n\tIntent: %s\n\tSlots: %s",
                intent,
                slots.toString()));
    }

    @Override
    public void onResponse(Response response) {
        Log.d(TAG, "Bot response: " + response.getTextResponse());
        Log.d(TAG, "Transcript: " + response.getInputTranscript());

        responseTextView.setText(response.getTextResponse());
        transcriptTextView.setText(response.getInputTranscript());
    }

    @Override
    public void onError(final String responseText, final Exception e) {
        Log.e(TAG, "Error: " + responseText, e);
    }
}
