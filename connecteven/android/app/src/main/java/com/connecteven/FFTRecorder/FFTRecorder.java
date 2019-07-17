package com.connecteven.FFTRecorder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.util.Arrays;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.connecteven.CallBack.IFFTValue;
import com.connecteven.FFTLibrary_1.Complex;
import com.connecteven.FFTLibrary_1.FFT;

public class FFTRecorder {
    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "AudioRecorder";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    public static final int RECORDER_SAMPLERATE = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
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

    //Tao moi mot recording
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

    public void startRecording(IFFTValue ifftValue){
        Log.d("check", "yes");
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, bufferSize);
        Log.d("check", "can't");

        recorder.startRecording();

        isRecording = true;
        Log.d("check", "ok");

        recordingThread = new Thread(new Runnable() {
            public void run() {
                try {
                    writeAudioDataToFile(ifftValue);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
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

    private void writeAudioDataToFile(IFFTValue ifftValue) throws InterruptedException {

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
                Thread.sleep(10);
                ifftValue.sendSum(findSumWithoutUsingStream(absNormalizedSignal));
            }
//                if(AudioRecord.ERROR_INVALID_OPERATION != read){
//                    try {
//                        os.write(data);
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                }
        }
//        try {
//            os.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
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
}
