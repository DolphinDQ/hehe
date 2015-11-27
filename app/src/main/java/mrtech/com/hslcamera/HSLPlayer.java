package mrtech.com.hslcamera;

/**
 * Created by zdqa1 on 2015/11/26.
 */

/**
 * HSL 摄像头播放器
 * Created by zdqa1 on 2015/11/26.
 */
public interface HSLPlayer {

    /**
     * 播放指定ID的是像头。
     *
     * @param deviceId 摄像头ID。
     */
    void play(String deviceId);

    /**
     * 播放指定摄像头
     *
     * @param cam 指定摄像头
     */
    void play(HSLCamera cam);

    /**
     * 获取当前播放摄像头对象。
     *
     * @return 当前播放摄像头对象。
     */
    HSLCamera getPlaying();

    /**
     * 获取当期播放列表。
     *
     * @return 当期播放列表。
     */
    HSLCamera[] getPlayingList();

    /**
     * 停止播放。
     */
    void stop();

    /**
     * 设置渲染回调
     *
     * @param listener
     */
    void setRanderListener(HSLCameraManager.RenderListener listener);
}
