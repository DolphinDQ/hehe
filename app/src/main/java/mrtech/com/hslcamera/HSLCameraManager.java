package mrtech.com.hslcamera;

import android.bluetooth.BluetoothClass;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import hsl.p2pipcam.nativecaller.DeviceSDK;

/**
 * HSL摄像头管理器。
 * Created by zdqa1 on 2015/11/25.
 */
public class HSLCameraManager {
    private static HSLCameraManager ourInstance = new HSLCameraManager();
    private boolean mIsInited;
    private final ArrayList<HSLCamera> mCameras = new ArrayList<>();
    private SettingsListener settingsListener;
    private RecorderListener recorderListener;
    private PlayListener playListener;
    private DeviceStatusListener deviceStatusListener;
    private Handler mHandler;

    private static void trace(String msg) {
        Log.e(HSLCameraManager.class.getName(), msg);
    }

    private static void trace(String msg, Throwable ex) {
        Log.d(HSLCameraManager.class.getName(), msg, ex);
    }

    public static HSLCameraManager getInstance() {
        return ourInstance;
    }

    private HSLCameraManager() {

    }

    public void init() {
        if (mIsInited) return;
        mIsInited = true;
        trace("DeviceSDK init...");
        DeviceSDK.initialize("");
        DeviceSDK.setCallback(this.new JNICallback());
        DeviceSDK.networkDetect();
    }

    public void destroy() {
        if (mIsInited) {
            mIsInited = false;
            trace("DeviceSDK destroy...");
            DeviceSDK.unInitSearchDevice();
        }
    }

    public void addCamera(HSLCamera cam) {
        if (getCamera(cam.mCamId) == null) {
            cam.mStatus = this.new CameraStatus();
            mCameras.add(cam);
            LinkCam(cam);
        }
    }

    private void LinkCam(HSLCamera cam) {
        new LinkCameraTask().execute(cam);
    }

    public HSLCamera[] getCameraList() {
        return mCameras.toArray(new HSLCamera[0]);
    }

    public HSLPlayer createCameraPlayer(GLSurfaceView glSurfaceView) {
        VideoRenderer videoRenderer = this.new VideoRenderer(glSurfaceView);
        glSurfaceView.setRenderer(videoRenderer);
        return new Player(videoRenderer, this);
    }

    public void removeCamera(HSLCamera cam) {
        long id = cam.mStatus.getHandle();
        DeviceSDK.closeDevice(id);
        mCameras.remove(cam);
    }

    public void removeCamera(String deviceId) {
        for (HSLCamera cam : mCameras) {
            if (cam.mCamId == deviceId) {
                mCameras.remove(cam);
                return;
            }
        }
    }

    public HSLCamera getCamera(String deviceId) {
        for (HSLCamera cam : mCameras) {
            if (cam.mCamId == deviceId)
                return cam;
        }
        return null;
    }

    private HSLCamera getCamera(long handle) {

        for (HSLCamera cam : mCameras) {
            if (cam.mStatus.getHandle() == handle)
                return cam;
        }
        return null;
    }

    public void setDeviceStatusListener(DeviceStatusListener listener) {
        deviceStatusListener = listener;
    }

    public void setPlayListener(PlayListener listener) {

        playListener = listener;
    }

    public void setRecorderListener(RecorderListener listener) {
        recorderListener = listener;
    }

    public void setSettingsListener(SettingsListener listener) {
        settingsListener = listener;
    }

    public void setCallbackLoop(Looper loop) {
        if (loop == null) {
            mHandler = null;
        } else {
            mHandler = new Handler(loop);
        }
    }

    //-----------------------以下是内部类---------------------------------------------

    private abstract class CallbackBase {

        public abstract void callback();

        public final void run() {
            if (mHandler != null) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        callback();
                    }
                });
            } else {
                callback();
            }
        }
    }

    public class LinkCameraTask extends AsyncTask<HSLCamera, Void, Void> {
        @Override
        protected Void doInBackground(HSLCamera... params) {
            HSLCamera cam = params[0];
            trace("linking camera :" + cam.mCamId);
            if (cam.mStatus.getHandle() == 0)
                ((CameraStatus) cam.mStatus).mHandle = DeviceSDK.createDevice(cam.mUserName, cam.mPassword, "", 0, cam.mCamId, 1);
            trace("create camera " + cam.mCamId + "->" + cam.mStatus.getHandle());
            if (cam.mStatus.getHandle() > 0) {
                long open = DeviceSDK.openDevice(cam.mStatus.getHandle());
                trace("open camera:" + open);
            }
            return null;
        }
    }

    private class CameraStatus implements HSLCamera.HSLCameraStatus {
        public long mHandle;
        public int mStatus;
        public boolean mIsPlaying;

        @Override
        public long getHandle() {
            return mHandle;
        }

        @Override
        public int getStatus() {
            return mStatus;
        }

        @Override
        public boolean isPlaying() {
            return mIsPlaying;
        }
    }

    private class JNICallback {
        public void CallBack_SnapShot(final long UserID, final byte[] buff, final int len) {
        }

        public void CallBack_GetParam(final long UserID, final long nType, final String param) {
            if (settingsListener != null) {
                new CallbackBase() {
                    @Override
                    public void callback() {
                        settingsListener.callBack_getParam(UserID, nType, param);
                    }
                }.run();
            }
        }

        public void CallBack_SetParam(final long UserID, final long nType, final int nResult) {
            if (settingsListener != null) {
                if (mHandler != null) {
                    new CallbackBase() {
                        @Override
                        public void callback() {
                            settingsListener.callBack_setParam(UserID, nType, nResult);
                        }
                    }.run();
                }
            }
        }

        public void CallBack_Event(final long UserID, long nType) {
            final int status = new Long(nType).intValue();
            final HSLCamera cam;
            if ((cam = getCamera(UserID)) != null) {
                ((CameraStatus) cam.mStatus).mStatus = status;
                trace("camera " + cam.mCamId + " state changed to " + status);
                if (status == 101) {
                    new AsyncTask<HSLCamera, Void, Void>() {
                        @Override
                        protected Void doInBackground(HSLCamera... params) {
                            try {
                                Thread.sleep(1000);
                                removeCamera(params[0]);
                                addCamera(params[0]);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            return null;
                        }
                    }.execute(cam);
                }
            }
            if (deviceStatusListener != null) {
                new CallbackBase() {
                    @Override
                    public void callback() {
                        deviceStatusListener.receiveDeviceStatus(UserID, status);
                    }
                };
            }

        }

        public void VideoData(long UserID, byte[] VideoBuf, int h264Data, int nLen, int Width, int Height, int time) {

        }

        public void callBackAudioData(final long nUserID, final byte[] pcm, final int size) {
            if (playListener != null)
                new CallbackBase() {
                    @Override
                    public void callback() {
                        playListener.callBackAudioData(nUserID, pcm, size);
                    }
                };
            if (recorderListener != null) {
                new CallbackBase() {
                    @Override
                    public void callback() {
                        recorderListener.callBackAudioData(nUserID, pcm, size);
                    }
                };
            }
        }

        public void CallBack_RecordFileList(final long UserID, final int filecount, final String fname, final String strDate, final int size) {
            if (settingsListener != null)
                new CallbackBase() {
                    @Override
                    public void callback() {
                        settingsListener.recordFileList(UserID, filecount, fname, strDate, size);
                    }
                };
        }

        public void CallBack_P2PMode(long UserID, int nType) {
        }

        public void CallBack_RecordPlayPos(long userid, int pos) {
        }

        public void CallBack_VideoData(long nUserID, byte[] data, int type, int size) {
        }

        public void CallBack_AlarmMessage(long UserID, int nType) {

        }

        public void showNotification(String message, BluetoothClass.Device device, int nType) {
        }

    }

    private class VideoRenderer implements GLSurfaceView.Renderer {
        int mHeight = 0;
        ByteBuffer mUByteBuffer = null;
        ByteBuffer mVByteBuffer = null;
        int mWidth = 0;
        ByteBuffer mYByteBuffer = null;
        FloatBuffer positionBuffer = null;
        final float[] positionBufferData;
        int positionSlot = 0;
        int programHandle = 0;
        int texRangeSlot = 0;
        int[] texture = new int[3];
        int[] textureSlot = new int[3];
        int vertexShader = 0;
        int yuvFragmentShader = 0;
        byte[] yuvData = null;
        final float[] textCoodBufferData;
        FloatBuffer textCoodBuffer = null;
        boolean bNeedSleep = true;

        private RenderListener listener;
        private boolean isTakePicture = false;

        private int compileShader(String paramString, int paramInt) {
            int i = GLES20.glCreateShader(paramInt);
            if (i != 0) {
                int[] arrayOfInt = new int[1];
                GLES20.glShaderSource(i, paramString);
                GLES20.glCompileShader(i);
                GLES20.glGetShaderiv(i, 35713, arrayOfInt, 0);
                if (arrayOfInt[0] == 0) {
                    GLES20.glDeleteShader(i);
                    i = 0;
                }
            }
            return i;
        }

        public void setTakePicture(boolean isTakePicture) {
            this.isTakePicture = isTakePicture;
        }

        public void setListener(RenderListener listener) {
            this.listener = listener;
        }

        public VideoRenderer(GLSurfaceView paramGLSurfaceView) {
            float[] arrayOfFloat1 = new float[16];

            arrayOfFloat1[0] = 0.0F;
            arrayOfFloat1[1] = 0.0F;
            arrayOfFloat1[2] = 0.0F;
            arrayOfFloat1[3] = 1.0F;

            arrayOfFloat1[4] = 0.0F;
            arrayOfFloat1[5] = 1.0F;
            arrayOfFloat1[6] = 0.0F;
            arrayOfFloat1[7] = 1.0F;

            arrayOfFloat1[8] = 1.0F;
            arrayOfFloat1[9] = 0.0F;
            arrayOfFloat1[10] = 0.0F;
            arrayOfFloat1[11] = 1.0F;

            arrayOfFloat1[12] = 1.0F;
            arrayOfFloat1[13] = 1.0F;
            arrayOfFloat1[14] = 0.0F;
            arrayOfFloat1[15] = 1.0F;

            this.textCoodBufferData = arrayOfFloat1;

            float[] arrayOfFloat = new float[16];

            arrayOfFloat[0] = -1.0F;
            arrayOfFloat[1] = 1.0F;
            arrayOfFloat[2] = 0.0F;
            arrayOfFloat[3] = 1.0F;

            arrayOfFloat[4] = -1.0F;
            arrayOfFloat[5] = -1.0F;
            arrayOfFloat[6] = 0.0F;
            arrayOfFloat[7] = 1.0F;

            arrayOfFloat[8] = 1.0F;
            arrayOfFloat[9] = 1.0F;
            arrayOfFloat[10] = 0.0F;
            arrayOfFloat[11] = 1.0F;

            arrayOfFloat[12] = 1.0F;
            arrayOfFloat[13] = -1.0F;
            arrayOfFloat[14] = 0.0F;
            arrayOfFloat[15] = 1.0F;

            this.positionBufferData = arrayOfFloat;

            paramGLSurfaceView.setEGLContextClientVersion(2);
        }

        public long createShaders() {
            String fragmentShaderCode = "uniform sampler2D Ytex;\n";
            fragmentShaderCode += "uniform sampler2D Utex;\n";
            fragmentShaderCode += "uniform sampler2D Vtex;\n";
            fragmentShaderCode += "precision mediump float;  \n";
            fragmentShaderCode += "varying vec4 VaryingTexCoord0; \n";
            fragmentShaderCode += "vec4 color;\n";
            fragmentShaderCode += "void main()\n";
            fragmentShaderCode += "{\n";
            fragmentShaderCode += "float yuv0 = (texture2D(Ytex,VaryingTexCoord0.xy)).r;\n";
            fragmentShaderCode += "float yuv1 = (texture2D(Utex,VaryingTexCoord0.xy)).r;\n";
            fragmentShaderCode += "float yuv2 = (texture2D(Vtex,VaryingTexCoord0.xy)).r;\n";
            fragmentShaderCode += "\n";
            fragmentShaderCode += "color.r = yuv0 + 1.4022 * yuv2 - 0.7011;\n";
            fragmentShaderCode += "color.r = (color.r < 0.0) ? 0.0 : ((color.r > 1.0) ? 1.0 : color.r);\n";
            fragmentShaderCode += "color.g = yuv0 - 0.3456 * yuv1 - 0.7145 * yuv2 + 0.53005;\n";
            fragmentShaderCode += "color.g = (color.g < 0.0) ? 0.0 : ((color.g > 1.0) ? 1.0 : color.g);\n";
            fragmentShaderCode += "color.b = yuv0 + 1.771 * yuv1 - 0.8855;\n";
            fragmentShaderCode += "color.b = (color.b < 0.0) ? 0.0 : ((color.b > 1.0) ? 1.0 : color.b);\n";
            fragmentShaderCode += "gl_FragColor = color;\n";
            fragmentShaderCode += "}\n";

            String vertexShaderCode = "uniform mat4 uMVPMatrix;   \n";
            vertexShaderCode += "attribute vec4 vPosition;  \n";
            vertexShaderCode += "attribute vec4 myTexCoord; \n";
            vertexShaderCode += "varying vec4 VaryingTexCoord0; \n";
            vertexShaderCode += "void main(){               \n";
            vertexShaderCode += "VaryingTexCoord0 = myTexCoord; \n";
            vertexShaderCode += "gl_Position = vPosition; \n";
            vertexShaderCode += "}  \n";

            int[] arrayOfInt = new int[1];
            int i = compileShader(vertexShaderCode, 35633);
            this.vertexShader = i;

            int j = compileShader(fragmentShaderCode, 35632);
            this.yuvFragmentShader = j;
            this.programHandle = GLES20.glCreateProgram();
            GLES20.glAttachShader(this.programHandle, this.vertexShader);
            GLES20.glAttachShader(this.programHandle, this.yuvFragmentShader);
            GLES20.glLinkProgram(this.programHandle);
            GLES20.glGetProgramiv(this.programHandle, 35714, arrayOfInt, 0);

            if (arrayOfInt[0] == 0) {
                destroyShaders();
            }

            this.texRangeSlot = GLES20.glGetAttribLocation(this.programHandle, "myTexCoord");

            this.textureSlot[0] = GLES20.glGetUniformLocation(this.programHandle, "Ytex");
            this.textureSlot[1] = GLES20.glGetUniformLocation(this.programHandle, "Utex");
            this.textureSlot[2] = GLES20.glGetUniformLocation(this.programHandle, "Vtex");

            this.positionSlot = GLES20.glGetAttribLocation(this.programHandle, "vPosition");
            return 0;
        }

        public long destroyShaders() {
            if (this.programHandle != 0) {
                GLES20.glDetachShader(this.programHandle, this.yuvFragmentShader);
                GLES20.glDetachShader(this.programHandle, this.vertexShader);
                GLES20.glDeleteProgram(this.programHandle);
                this.programHandle = 0;
            }
            if (this.yuvFragmentShader != 0) {
                GLES20.glDeleteShader(this.yuvFragmentShader);
                this.yuvFragmentShader = 0;
            }
            if (this.vertexShader != 0) {
                GLES20.glDeleteShader(this.vertexShader);
                this.vertexShader = 0;
            }
            return 0L;
        }

        public int draw(ByteBuffer paramByteBuffer1, ByteBuffer paramByteBuffer2, ByteBuffer paramByteBuffer3, int paramInt1, int paramInt2) {
            GLES20.glClear(16384);
            GLES20.glClearColor(0.0F, 0.0F, 0.0F, 1.0F);
            GLES20.glUseProgram(this.programHandle);
            paramByteBuffer1.position(0);
            GLES20.glActiveTexture(33984);
            loadTexture(this.texture[0], paramInt1, paramInt2, paramByteBuffer1);
            paramByteBuffer2.position(0);
            GLES20.glActiveTexture(33985);
            loadTexture(this.texture[1], paramInt1 >> 1, paramInt2 >> 1, paramByteBuffer2);
            paramByteBuffer3.position(0);
            GLES20.glActiveTexture(33986);
            loadTexture(this.texture[2], paramInt1 >> 1, paramInt2 >> 1, paramByteBuffer3);
            GLES20.glUniform1i(this.textureSlot[0], 0);
            GLES20.glUniform1i(this.textureSlot[1], 1);
            GLES20.glUniform1i(this.textureSlot[2], 2);

            this.positionBuffer.position(0);
            GLES20.glEnableVertexAttribArray(this.positionSlot);
            GLES20.glVertexAttribPointer(this.positionSlot, 4, GLES20.GL_FLOAT, false, 0, this.positionBuffer);

            this.textCoodBuffer.position(0);

            GLES20.glEnableVertexAttribArray(this.texRangeSlot);
            GLES20.glVertexAttribPointer(this.texRangeSlot, 4, GLES20.GL_FLOAT, false, 0, this.textCoodBuffer);

            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
            GLES20.glDisableVertexAttribArray(this.positionSlot);

            GLES20.glDisableVertexAttribArray(this.texRangeSlot);
            return 0;
        }

        public int loadTexture(int paramInt1, int paramInt2, int paramInt3, Buffer paramBuffer) {
            GLES20.glBindTexture(3553, paramInt1);
            GLES20.glTexParameteri(3553, 10241, 9729);
            GLES20.glTexParameteri(3553, 10240, 9729);
            GLES20.glTexParameteri(3553, 10242, 33071);
            GLES20.glTexParameteri(3553, 10243, 33071);
            GLES20.glTexImage2D(3553, 0, 6409, paramInt2, paramInt3, 0, 6409, GLES20.GL_UNSIGNED_BYTE, paramBuffer);
            return 0;
        }

        public int loadVBOs() {
            this.textCoodBuffer = ByteBuffer.allocateDirect(4 * this.textCoodBufferData.length).order(ByteOrder.nativeOrder()).asFloatBuffer();
            this.textCoodBuffer.put(this.textCoodBufferData).position(0);

            this.positionBuffer = ByteBuffer.allocateDirect(4 * this.positionBufferData.length).order(ByteOrder.nativeOrder()).asFloatBuffer();
            this.positionBuffer.put(this.positionBufferData).position(0);

            return 0;
        }

        public void onDrawFrame(GL10 paramGL10) {
            GLES20.glClear(16384);
            synchronized (this) {
                if ((this.mWidth == 0) || (this.mHeight == 0) || (this.mYByteBuffer == null) || (this.mUByteBuffer == null) || (this.mVByteBuffer == null))
                    return;
                if (bNeedSleep) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                bNeedSleep = true;
                draw(this.mYByteBuffer, this.mUByteBuffer, this.mVByteBuffer, this.mWidth, this.mHeight);

            }
        }

        public void onSurfaceChanged(GL10 paramGL10, int width, int height) {
            GLES20.glViewport(0, 0, width, height);
        }

        public void onSurfaceCreated(GL10 paramGL10, EGLConfig paramEGLConfig) {
            GLES20.glClearColor(0.0F, 0.0F, 0.0F, 1.0F);
            GLES20.glGenTextures(3, this.texture, 0);
            createShaders();
            loadVBOs();
        }

        public int unloadVBOs() {
            if (this.positionBuffer != null)
                this.positionBuffer = null;
            return 0;
        }

        public void writeSample(final byte[] paramArrayOfByte, final int width, final int height) {
            synchronized (this) {
                if ((width == 0) || (height == 0)) {
                    return;
                }
                if (listener != null) {
                    new CallbackBase() {
                        @Override
                        public void callback() {
                            listener.initComplete(paramArrayOfByte.length, width, height);
                        }
                    };
                }

                //拍照
                if (isTakePicture) {
                    isTakePicture = false;
                    if (listener != null)
                        new CallbackBase() {
                            @Override
                            public void callback() {
                                listener.takePicture(paramArrayOfByte, width, height);
                            }
                        };
                }

                if ((width != this.mWidth) || (height != this.mHeight)) {
                    this.mWidth = width;
                    this.mHeight = height;
                    this.mYByteBuffer = ByteBuffer.allocate(this.mWidth * this.mHeight);
                    this.mUByteBuffer = ByteBuffer.allocate(this.mWidth * this.mHeight / 4);
                    this.mVByteBuffer = ByteBuffer.allocate(this.mWidth * this.mHeight / 4);
                }

                if (this.mYByteBuffer != null) {
                    this.mYByteBuffer.position(0);
                    this.mYByteBuffer.put(paramArrayOfByte, 0, this.mWidth * this.mHeight);
                    this.mYByteBuffer.position(0);
                }

                if (this.mUByteBuffer != null) {
                    this.mUByteBuffer.position(0);
                    this.mUByteBuffer.put(paramArrayOfByte, this.mWidth * this.mHeight, this.mWidth * this.mHeight / 4);
                    this.mUByteBuffer.position(0);
                }

                if (this.mVByteBuffer != null) {
                    this.mVByteBuffer.position(0);
                    this.mVByteBuffer.put(paramArrayOfByte, 5 * (this.mWidth * this.mHeight) / 4, this.mWidth * this.mHeight / 4);
                    this.mVByteBuffer.position(0);
                }

                bNeedSleep = false;
                //return 1;
            }
        }
    }

    public interface RenderListener {

        void initComplete(int size, int width, int height);

        void takePicture(byte[] imageBuffer, int width, int height);
    }

    public class Player implements HSLPlayer {

        private final VideoRenderer mRenderer;
        private final HSLCameraManager mManager;
        private HSLCamera mPlaying;

        public Player(VideoRenderer videoRenderer, HSLCameraManager manager) {
            mRenderer = videoRenderer;
            mManager = manager;
        }


        @Override
        public void play(String deviceId) {
            play(mManager.getCamera(deviceId));
        }

        @Override
        public void play(HSLCamera cam) {
            stop();
            if (cam != null) {
                mPlaying = cam;
                new LoadTask().execute(cam.mStatus.getHandle());
            } else {
                trace("指定播放摄像头为空...");
            }
        }

        @Override
        public HSLCamera getPlaying() {
            return mPlaying;
        }

        @Override
        public HSLCamera[] getPlayingList() {
            HSLCamera[] cameraList = mManager.getCameraList();
            ArrayList<HSLCamera> cameras = new ArrayList<>();
            for (HSLCamera cam : cameraList) {
                if (cam.mStatus.getStatus() == 100) {
                    cameras.add(cam);
                }
            }
            return cameras.toArray(new HSLCamera[0]);
        }

        @Override
        public void stop() {
            if (mPlaying != null) {
                DeviceSDK.stopPlayStream(mPlaying.mStatus.getHandle());
                mPlaying = null;
            }
        }

        private class LoadTask extends AsyncTask<Long, Void, Void> {
            @Override
            protected Void doInBackground(Long... arg0) {
                long userid = arg0[0];
                DeviceSDK.setRender(userid, mRenderer);
                DeviceSDK.startPlayStream(userid, 10, 1);

                try {
                    JSONObject obj = new JSONObject();
                    obj.put("param", 13);
                    obj.put("value", 1024);
                    DeviceSDK.setDeviceParam(userid, 0x2026, obj.toString());

                    JSONObject obj1 = new JSONObject();
                    obj1.put("param", 6);
                    obj1.put("value", 15);
                    DeviceSDK.setDeviceParam(userid, 0x2026, obj1.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }

        @Override
        public void setRanderListener(RenderListener listener) {
            mRenderer.setListener(listener);
        }
    }
}
