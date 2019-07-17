package com.hfad.testrecording;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.hfad.testrecording.FFTLibrary_1.Complex;
import com.hfad.testrecording.FFTLibrary_1.FFT;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    public static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int REQUEST_MICROPHONE = 10;
    short[] audioData;

    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    Complex[] fftTempArray;
    Complex[] fftArray;
    int[] bufferData;
    int bytesRecorded;
    private int mPeakPos;

    Button btnstart;

    private void init(){
        btnstart = findViewById(R.id.btnstart);

        btnstart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("check", "onClick: ");
                startRecording();
            }
        });
    }

    private void requestRecordAudioPermission() {
        //check API version, do nothing if API version < 23!
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion > android.os.Build.VERSION_CODES.LOLLIPOP){

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    Log.d("Activity", "Granted!");
                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Log.d("Activity", "Denied!");
                    finish();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


       requestRecordAudioPermission();

        requestRecordAudioPermission();
        init();
        initRecorder();
    }

    public void initRecorder(){
        Log.d("check", "init2: ");

        bufferSize = AudioRecord.getMinBufferSize
                (RECORDER_SAMPLERATE,RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING)*3;
        Log.d("check", RECORDER_SAMPLERATE +"");

        audioData = new short [bufferSize]; //short array that pcm data is put into.
    }


    public String getFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        return (file.getAbsolutePath() + "/" + System.currentTimeMillis() + AUDIO_RECORDER_FILE_EXT_WAV);
    }

    public void convert(){

    }

    public void calculate(){
        Complex[] fftTempArray = new Complex[bufferSize];
        for (int i=0; i<bufferSize; i++)
        {
            fftTempArray[i] = new Complex(audioData[i], 0);
        }
        Complex[] fftArray = FFT.fft(fftTempArray);

        double[] micBufferData = new double[bufferSize];
        final int bytesPerSample = 2;
        final double amplification = 100.0;
        for (int index = 0, floatIndex = 0; index < bytesRecorded - bytesPerSample + 1; index += bytesPerSample, floatIndex++) {
            double sample = 0;
            for (int b = 0; b < bytesPerSample; b++) {
                int v = bufferData[index + b];
                if (b < bytesPerSample - 1 || bytesPerSample == 1) {
                    v &= 0xFF;
                }
                sample += v << (b * 8);
            }
            double sample32 = amplification * (sample / 32768.0);
            micBufferData[floatIndex] = sample32;
        }
    }

    public String getTempFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        File tempFile = new File(filepath,AUDIO_RECORDER_TEMP_FILE);

        if(tempFile.exists())
            tempFile.delete();

        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }

    public void startRecording(){
        Log.d("check", "yes");
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, bufferSize);
        Log.d("check", "can't");

        recorder.startRecording();

        isRecording = true;
        Log.d("check", "ok");

        recordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile();
            }
        },"AudioRecorder Thread");

        recordingThread.start();
    }

    public void stopRecording(){
        if(null != recorder){
            isRecording = false;

            recorder.stop();
            recorder.release();

            recorder = null;
            recordingThread = null;
        }

        copyWaveFile(getTempFilename(),getFilename());
        // deleteTempFile();
    }

    private void deleteTempFile() {
        File file = new File(getTempFilename());
        file.delete();
    }

    public void copyWaveFile(String inFilename,String outFilename){
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 2;
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;

        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            // AppLog.logString("File size: " + totalDataLen);

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            while(in.read(data) != -1){
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {
        //another code

    }

    private void writeAudioDataToFile() {

        byte data[] = new byte[bufferSize];
//        String filename = getTempFilename();
//        FileOutputStream os = null;
//        try {
//            os = new FileOutputStream(filename);
//        } catch (FileNotFoundException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
        int read = 0;

        while (isRecording) {
            read = recorder.read(data, 0, bufferSize);
            Log.d("check", "ok");
            Log.d("check", read + "");
            if (read > 0) {
                double[] absNormalizedSignal = calculateFFT(data);
                Log.d("check", absNormalizedSignal.length + "");
                Log.d("check", Arrays.toString(absNormalizedSignal));
                Log.d("check", Arrays.toString(data));
                Log.d("check", findSumWithoutUsingStream(absNormalizedSignal) + "");

                //ifftValue.sendSum(findSumWithoutUsingStream(absNormalizedSignal));
            }
//                if(AudioRecord.ERROR_INVALID_OPERATION != read){
//                    try {
//                        os.write(data);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
        }

    }


    public  double findSumWithoutUsingStream(double[] array) {
        double sum = 0;
        for (double value : array) {
            sum += value;
        }
        return sum;
    }

    public double[] calculateFFT(byte[] signal)
    {
        final int mNumberOfFFTPoints =1024;
        double mMaxFFTSample;
        double temp;
        Complex[] y;
        Complex[] complexSignal = new Complex[mNumberOfFFTPoints];
        double[] absSignal = new double[mNumberOfFFTPoints/2];

        for(int i = 0; i < mNumberOfFFTPoints; i++){
            temp = (double)((signal[2*i] & 0xFF) | (signal[2*i+1] << 8)) / 32768.0F;
            complexSignal[i] = new Complex(temp,0.0);
        }

        y = FFT.fft(complexSignal); // --> Here I use FFT class

        mMaxFFTSample = 0.0;
        mPeakPos = 0;
        for(int i = 0; i < (mNumberOfFFTPoints/2); i++)
        {
            absSignal[i] = Math.sqrt(Math.pow(y[i].re(), 2) + Math.pow(y[i].im(), 2));
            if(absSignal[i] > mMaxFFTSample)
            {
                mMaxFFTSample = absSignal[i];
                mPeakPos = i;
            }
        }
        return absSignal;
    }

    //////////////////
//    public void initRecorder(){
//
//        bufferSize = AudioRecord.getMinBufferSize
//                (RECORDER_SAMPLERATE,RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING)*3;
//
//        audioData = new short [bufferSize]; //short array that pcm data is put into.
//    }
//
//    public String getFilename(){
//        String filepath = Environment.getExternalStorageDirectory().getPath();
//        File file = new File(filepath,AUDIO_RECORDER_FOLDER);
//
//        if(!file.exists()){
//            file.mkdirs();
//        }
//
//        return (file.getAbsolutePath() + "/" + System.currentTimeMillis() + AUDIO_RECORDER_FILE_EXT_WAV);
//    }
//
//    public void convert(){
//
//
//
//    }
//
//    public void calculate(){
//        Complex[] fftTempArray = new Complex[bufferSize];
//        for (int i=0; i<bufferSize; i++)
//        {
//            fftTempArray[i] = new Complex(audioData[i], 0);
//        }
//        Complex[] fftArray = FFT.fft(fftTempArray);
//
//        double[] micBufferData = new double[bufferSize];
//        final int bytesPerSample = 2;
//        final double amplification = 100.0;
//        for (int index = 0, floatIndex = 0; index < bytesRecorded - bytesPerSample + 1; index += bytesPerSample, floatIndex++) {
//            double sample = 0;
//            for (int b = 0; b < bytesPerSample; b++) {
//                int v = bufferData[index + b];
//                if (b < bytesPerSample - 1 || bytesPerSample == 1) {
//                    v &= 0xFF;
//                }
//                sample += v << (b * 8);
//            }
//            double sample32 = amplification * (sample / 32768.0);
//            micBufferData[floatIndex] = sample32;
//        }
//    }
//
//    public String getTempFilename(){
//        String filepath = Environment.getExternalStorageDirectory().getPath();
//        File file = new File(filepath,AUDIO_RECORDER_FOLDER);
//
//        if(!file.exists()){
//            file.mkdirs();
//        }
//
//        File tempFile = new File(filepath,AUDIO_RECORDER_TEMP_FILE);
//
//        if(tempFile.exists())
//            tempFile.delete();
//
//        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
//    }
//
//    public void startRecording(){
//        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
//                RECORDER_SAMPLERATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, bufferSize);
//
//        recorder.startRecording();
//        Log.d("check", "startRecording: ");
//
//        isRecording = true;
//
//        recordingThread = new Thread(new Runnable() {
//
//            public void run() {
//                writeAudioDataToFile();
//            }
//        },"AudioRecorder Thread");
//
//        recordingThread.start();
//    }
//
//    public void stopRecording(){
//        if(null != recorder){
//            isRecording = false;
//
//            recorder.stop();
//            recorder.release();
//
//            recorder = null;
//            recordingThread = null;
//        }
//
//        copyWaveFile(getTempFilename(),getFilename());
//        // deleteTempFile();
//    }
//
//    private void deleteTempFile() {
//        File file = new File(getTempFilename());
//        file.delete();
//    }
//
//    public void copyWaveFile(String inFilename,String outFilename){
//        FileInputStream in = null;
//        FileOutputStream out = null;
//        long totalAudioLen = 0;
//        long totalDataLen = totalAudioLen + 36;
//        long longSampleRate = RECORDER_SAMPLERATE;
//        int channels = 2;
//        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;
//
//        byte[] data = new byte[bufferSize];
//
//        try {
//            in = new FileInputStream(inFilename);
//            out = new FileOutputStream(outFilename);
//            totalAudioLen = in.getChannel().size();
//            totalDataLen = totalAudioLen + 36;
//
//            // AppLog.logString("File size: " + totalDataLen);
//
//            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
//                    longSampleRate, channels, byteRate);
//
//            while(in.read(data) != -1){
//                out.write(data);
//            }
//
//            in.close();
//            out.close();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void WriteWaveFileHeader(
//            FileOutputStream out, long totalAudioLen,
//            long totalDataLen, long longSampleRate, int channels,
//            long byteRate) throws IOException {
//        //another code
//
//    }
//
//    private void writeAudioDataToFile() {
//
//        byte data[] = new byte[bufferSize];
////        String filename = getTempFilename();
////        FileOutputStream os = null;
////        try {
////            os = new FileOutputStream(filename);
////        } catch (FileNotFoundException e) {
////            // TODO Auto-generated catch block
////            e.printStackTrace();
////        }
//        int read = 0;
//
//        while (isRecording) {
//            read = recorder.read(data, 0, bufferSize);
//            if (read > 0) {
//                double[] absNormalizedSignal = calculateFFT(data);
//                Log.d("check", absNormalizedSignal.length + "");
//                Log.d("check", Arrays.toString(absNormalizedSignal));
//                Log.d("check", Arrays.toString(data));
//                Log.d("check", findSumWithoutUsingStream(absNormalizedSignal) + "");
//            }
////                if(AudioRecord.ERROR_INVALID_OPERATION != read){
////                    try {
////                        os.write(data);
////                    } catch (IOException e) {
////                        e.printStackTrace();
////                    }
////                }
//        }
//
//    }
//
//
//    public  double findSumWithoutUsingStream(double[] array) {
//        double sum = 0;
//        for (double value : array) {
//            sum += value;
//        }
//        return sum;
//    }
//
//    public double[] calculateFFT(byte[] signal)
//    {
//        final int mNumberOfFFTPoints =1024;
//        double mMaxFFTSample;
//        double temp;
//        Complex[] y;
//        Complex[] complexSignal = new Complex[mNumberOfFFTPoints];
//        double[] absSignal = new double[mNumberOfFFTPoints/2];
//
//        for(int i = 0; i < mNumberOfFFTPoints; i++){
//            temp = (double)((signal[2*i] & 0xFF) | (signal[2*i+1] << 8)) / 32768.0F;
//            complexSignal[i] = new Complex(temp,0.0);
//        }
//
//        y = FFT.fft(complexSignal); // --> Here I use FFT class
//
//        mMaxFFTSample = 0.0;
//        mPeakPos = 0;
//        for(int i = 0; i < (mNumberOfFFTPoints/2); i++)
//        {
//            absSignal[i] = Math.sqrt(Math.pow(y[i].re(), 2) + Math.pow(y[i].im(), 2));
//            if(absSignal[i] > mMaxFFTSample)
//            {
//                mMaxFFTSample = absSignal[i];
//                mPeakPos = i;
//            }
//        }
//        return absSignal;
//    }


}
