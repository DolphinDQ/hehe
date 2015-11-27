package object.p2pipcam.nativecaller;

public class NativeCaller {

	static {
		System.loadLibrary("avi_utils");
	};
	
	public native static int OpenAvi(String filename, String forcc, int height,int width, int framerate);
	public native static int CloseAvi();
	public native static int WriteData(byte[] data, int len, int keyframe);
}