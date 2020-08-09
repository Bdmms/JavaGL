/**
 * This class manages the passing of time between frames.
 * @version Last Edited: August/08/2020
 * @author Sean Rannie
 */
public class Time 
{
	/** Time of next second interval */
	private static long nextSecond = System.currentTimeMillis();
	/** The current frame count of the second */
	private static int frameCount = 0;
	/** The time of the last frame */
	private static long lastFrame = 0;
	/** The time of the current frame */
	private static long currentFrame = 0;
	/** Time of next frame */
	private static long nextFrame = System.nanoTime();
	
	/** The current frame rate */
	public static int frameRate = 0;
	/** The number of frames counted */
	public static long globalCount = 0;
	/** Difference in time from last frame */
	public static float deltaTime = 1.0f;				
	/** Enables VSync */
	public static boolean vsyncEnabled = false;
	
	/**
	 * Used to pause until the next 60 Hz tick
	 */
	public static void vSync()
	{
		while(nextFrame > currentFrame)
			currentFrame = System.nanoTime();
		nextFrame += 16666666; //16.6 ms
	}
	
	/**
	 * Calculates time change between frames
	 */
	public static void frame()
	{
     	frameCount++;
     	
     	// Update frame rate when next second passes
 		if(System.currentTimeMillis() >= nextSecond)
 		{
 			frameRate = frameCount;
 			frameCount = 0;
 			nextSecond += 1000; //1.0 s
 		}
 		
 		lastFrame = currentFrame;
 		currentFrame = System.nanoTime();
 		deltaTime = (float) (currentFrame - lastFrame) / 1000000000;
		
		globalCount++;
		
		if( vsyncEnabled )
			vSync();
	}
}
