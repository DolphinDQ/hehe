package mrtech.com.hslcamera;

import java.util.Objects;

/**
 * HSL 摄像头对象接口。
 * Created by zdqa1 on 2015/11/25.
 */
public class HSLCamera {
    public HSLCamera(Objects source, String camId, String userName, String password) {
        mSource = source;
        mCamId = camId;
        mUserName = userName;
        mPassword = password;
    }

    public final Objects mSource;
    public final String mCamId;
    public final String mUserName;
    public final String mPassword;
    public HSLCameraStatus mStatus;

    /**
     * HSL摄像头状态描述接口。
     * Created by zdqa1 on 2015/11/26.
     */
    public interface HSLCameraStatus {
        long getHandle();

        int getStatus();

        boolean isPlaying();
    }
}
