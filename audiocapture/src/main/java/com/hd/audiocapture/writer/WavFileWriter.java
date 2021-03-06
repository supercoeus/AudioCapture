package com.hd.audiocapture.writer;

import android.media.AudioFormat;

import com.hd.audiocapture.CaptureConfig;
import com.hd.audiocapture.CaptureState;
import com.hd.audiocapture.Utils;
import com.hd.audiocapture.callback.CaptureStreamCallback;

import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Created by hd on 2018/5/10 .
 */
public class WavFileWriter extends AudioFileWriter {

    private class WavFileHeader {

        public static final int WAV_FILE_HEADER_SIZE = 44;
        public static final int WAV_CHUNKSIZE_EXCLUDE_DATA = 36;

        public static final int WAV_CHUNKSIZE_OFFSET = 4;
        public static final int WAV_SUB_CHUNKSIZE1_OFFSET = 16;
        public static final int WAV_SUB_CHUNKSIZE2_OFFSET = 40;

        public String mChunkID = "RIFF";
        public int mChunkSize = 0;
        public String mFormat = "WAVE";

        public String mSubChunk1ID = "fmt ";
        public int mSubChunk1Size = 16;
        public short mAudioFormat = 1;
        public short mNumChannel = 1;
        public int mSampleRate = 8000;
        public int mByteRate = 0;
        public short mBlockAlign = 0;
        public short mBitsPerSample = 8;

        public String mSubChunk2ID = "data";
        public int mSubChunk2Size = 0;

        public WavFileHeader() {

        }

        public WavFileHeader(int sampleRateInHz, int channels, int bitsPerSample) {
            mSampleRate = sampleRateInHz;
            mBitsPerSample = (short) bitsPerSample;
            mNumChannel = (short) channels;
            mByteRate = mSampleRate * mNumChannel * mBitsPerSample / 8;
            mBlockAlign = (short) (mNumChannel * mBitsPerSample / 8);
        }
    }

    private WavFileHeader header;

    private int mDataSize;

    private String mFilepath;

    private DataOutputStream mDataOutputStream;

    private CaptureConfig captureConfig;

    @Override
    public void init(DataOutputStream mDataOutputStream, CaptureConfig captureConfig) {
        header = new WavFileHeader(captureConfig.getSamplingRate(), AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mDataSize = 0;
        this.mDataOutputStream = mDataOutputStream;
        this.captureConfig = captureConfig;
        this.mFilepath = captureConfig.getFile().getAbsolutePath();
    }

    @Override
    public boolean start() {
        if (mDataOutputStream == null) {
            return false;
        }
        try {
            mDataOutputStream.writeBytes(header.mChunkID);
            mDataOutputStream.write(Utils.intToByteArray(header.mChunkSize), 0, 4);
            mDataOutputStream.writeBytes(header.mFormat);
            mDataOutputStream.writeBytes(header.mSubChunk1ID);
            mDataOutputStream.write(Utils.intToByteArray(header.mSubChunk1Size), 0, 4);
            mDataOutputStream.write(Utils.shortToByteArray(header.mAudioFormat), 0, 2);
            mDataOutputStream.write(Utils.shortToByteArray(header.mNumChannel), 0, 2);
            mDataOutputStream.write(Utils.intToByteArray(header.mSampleRate), 0, 4);
            mDataOutputStream.write(Utils.intToByteArray(header.mByteRate), 0, 4);
            mDataOutputStream.write(Utils.shortToByteArray(header.mBlockAlign), 0, 2);
            mDataOutputStream.write(Utils.shortToByteArray(header.mBitsPerSample), 0, 2);
            mDataOutputStream.writeBytes(header.mSubChunk2ID);
            mDataOutputStream.write(Utils.intToByteArray(header.mSubChunk2Size), 0, 4);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean writeData(CaptureState state, byte[] buffer, int offset, int count) {
        if (mDataOutputStream == null) {
            return false;
        }
        if (CaptureState.RESUME == state) {
            try {
                byte[] filterData = null;
                if (captureConfig.getCaptureCallback() != null && captureConfig.getCaptureCallback() instanceof CaptureStreamCallback) {
                    filterData = ((CaptureStreamCallback) captureConfig.getCaptureCallback()).filterContentByte(buffer);
                }
                buffer = filterData == null || filterData.length <= 0 ? buffer : filterData;
                mDataOutputStream.write(buffer, offset, buffer.length);
                mDataSize += count;
                if (captureConfig.getCaptureCallback() != null && captureConfig.getCaptureCallback() instanceof CaptureStreamCallback) {
                    ((CaptureStreamCallback) captureConfig.getCaptureCallback()).captureContentByte(buffer);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean stop() {
        if (mDataOutputStream == null) {
            return false;
        }
        try {
            RandomAccessFile wavFile = new RandomAccessFile(mFilepath, "rw");
            wavFile.seek(WavFileHeader.WAV_CHUNKSIZE_OFFSET);
            wavFile.write(Utils.intToByteArray((mDataSize + WavFileHeader.WAV_CHUNKSIZE_EXCLUDE_DATA)), 0, 4);
            wavFile.seek(WavFileHeader.WAV_SUB_CHUNKSIZE2_OFFSET);
            wavFile.write(Utils.intToByteArray((mDataSize)), 0, 4);
            wavFile.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }


}

