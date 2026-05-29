package com.gxdevs.mindmint.Services;

import android.content.Context;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.util.Log;
import java.util.ArrayList;

public class VoiceCommandListener implements RecognitionListener {
    
    private static final String TAG = "VoiceCommandListener";
    private final Context context;
    private final SpeechRecognizer speechRecognizer;
    private final VoiceCommandCallback callback;
    
    public interface VoiceCommandCallback {
        void onStartFocusSession(int minutes);
        void onStopFocusSession();
        void onAddTask(String taskTitle);
        void onBlockApp(String appName);
        void onGetStats();
        void onError(String error);
    }
    
    public VoiceCommandListener(Context context, VoiceCommandCallback callback) {
        this.context = context;
        this.callback = callback;
        this.speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        this.speechRecognizer.setRecognitionListener(this);
    }
    
    public void startListening() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.getPackageName());
            speechRecognizer.startListening(recognizerIntent);
            Log.d(TAG, "Voice recognition started");
        }
    }
    
    public void stopListening() {
        speechRecognizer.stopListening();
    }
    
    @Override
    public void onReadyForSpeech(android.os.Bundle params) {}
    @Override
    public void onBeginningOfSpeech() {}
    @Override
    public void onRmsChanged(float rmsdB) {}
    @Override
    public void onBufferReceived(byte[] buffer) {}
    @Override
    public void onEndOfSpeech() {}
    @Override
    public void onEvent(int eventType, android.os.Bundle params) {}
    @Override
    public void onPartialResults(android.os.Bundle partialResults) {}
    
    @Override
    public void onError(int error) {
        if (callback != null) callback.onError("Voice error: " + error);
    }
    
    @Override
    public void onResults(android.os.Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            processVoiceCommand(matches.get(0).toLowerCase());
        }
    }
    
    private void processVoiceCommand(String command) {
        if (command.contains("start") && command.contains("focus")) {
            int duration = extractDuration(command);
            if (callback != null) callback.onStartFocusSession(duration > 0 ? duration : 25);
        } else if (command.contains("stop")) {
            if (callback != null) callback.onStopFocusSession();
        } else if (command.contains("add")) {
            if (callback != null) callback.onAddTask("Voice Task");
        }
    }
    
    private int extractDuration(String command) {
        for (String d : new String[]{"25", "30", "45", "60", "90"}) {
            if (command.contains(d)) return Integer.parseInt(d);
        }
        return 0;
    }
    
    public void destroy() {
        if (speechRecognizer != null) speechRecognizer.destroy();
    }
}
