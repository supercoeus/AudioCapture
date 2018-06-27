package com.hd.audiocapture.capture;

import android.media.MediaRecorder;
import android.os.Build;
import android.util.Log;

import com.hd.audiocapture.CaptureState;
import com.hd.audiocapture.CaptureType;

import java.io.File;

/**
 * Created by hd on 2018/5/9 .
 */
public class MediaRecorderCapture extends Capture {

    private final String TAG = MediaRecorderCapture.class.getSimpleName();

    private MediaRecorder mMediaRecorder;

    @Override
    void startRecord() {
        try {
            mMediaRecorder = new MediaRecorder();
            File mRecorderFile = createAudioFile();
            if (mRecorderFile == null) {
                if(captureConfig.allowLog())
                Log.e(TAG, "create file error");
                notAllowEnterNextStep();
                return;
            }
            record.set(true);
            if (callback != null)
                callback.captureStatus(CaptureState.START);
            mMediaRecorder.setAudioSource(captureConfig.getAudioSource());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                mMediaRecorder.setOutputFormat(CaptureType.AAC_FORMAT.equals(mode) ?//
                               MediaRecorder.OutputFormat.AAC_ADTS : MediaRecorder.OutputFormat.MPEG_4);
            } else {
                mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
            }
            mMediaRecorder.setAudioSamplingRate(captureConfig.getSamplingRate());
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
            mMediaRecorder.setAudioEncodingBitRate(captureConfig.getBitrate());
            mMediaRecorder.setOutputFile(mRecorderFile.getAbsolutePath());
            mMediaRecorder.prepare();
            mMediaRecorder.start();
            if (callback != null)
                callback.captureStatus(CaptureState.RESUME);
            initVolumeThread();
        } catch (Exception e) {
            e.printStackTrace();
            if(captureConfig.allowLog())
            Log.e(TAG, "create media recorder error :" + e);
            notAllowEnterNextStep();
        }
    }

    @Override
    void stopRecord() {
        if (mMediaRecorder != null)
            try {
                mMediaRecorder.stop();
                mMediaRecorder.reset();
            } catch (Exception e) {
                e.printStackTrace();
            }
        if(captureConfig.allowLog())
        Log.d(TAG, "MediaRecorderCapture stop record");
    }

    @Override
    void release() {
        if (mMediaRecorder != null) {
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
        if(captureConfig.allowLog())
        Log.d(TAG, "MediaRecorderCapture release");
    }

    private void initVolumeThread() {
        new Thread(() -> {
            while (record.get() && mMediaRecorder != null) {
                try {
                    double ratio = (double) mMediaRecorder.getMaxAmplitude() / 1;
                    if (ratio > 1) {
                        double db = 20 * Math.log10(ratio);
                        if (callback != null)
                            callback.captureVolume(db);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}
