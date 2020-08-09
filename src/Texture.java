import java.awt.image.BufferedImage;

/**
 * This class stores a copy of a BufferedImage as an integer array and
 * allows for floating-point based indexing.
 * @version Last Edited: August/08/2020
 * @author Sean Rannie
 */
public class Texture 
{
	public final static byte NUM_CHANNELs = 4;
	public final static byte R = 0;
	public final static byte G = 1;
	public final static byte B = 2;
	public final static byte A = 3;
	
	/** Width of the created {@link Texture} */
	private BinarySize width;
	/** Height of the created {@link Texture} */
	private BinarySize height;
	/** Array that stores the resized copy of the image */
	private short[][] buffer;
	
	/** Creates an empty {@link Texture} */
	public Texture()
	{
		width = BinarySize.x1;
		height = BinarySize.x1;
		buffer = new short[width.size * height.size][NUM_CHANNELs];
	}
	
	/** 
	 * Resizes a {@link BufferedImage} into a usable {@link Texture} 
	 * @param image source of the image data
	 */
	public Texture(BufferedImage image)
	{
		// Find the closest matching dimensions
		width = BinarySize.closestMatch( image.getWidth() );
		height = BinarySize.closestMatch( image.getHeight() );
		buffer = new short[width.size * height.size][NUM_CHANNELs];
		
		// Resize the image to fit within a BinarySize texture
		for(int y = 0; y < height.size; y++)
		{
			for(int x = 0; x < width.size; x++)
			{
				float xf = (float) x / width.size;
				float yf = (float) y / height.size;
				int color = image.getRGB(Math.round(xf * image.getWidth()), Math.round(yf * image.getHeight()));
				short[] pixel = buffer[x | (y << width.bits)];
				pixel[A] = (short) ((color >> 24) & 0xFF);
				pixel[R] = (short) ((color >> 16) & 0xFF);
				pixel[G] = (short) ((color >>  8) & 0xFF);
				pixel[B] = (short) ((color) & 0xFF);
			}
		}
	}
	
	/**
	 * Returns the RGBA array value stored at a floating-point x-y coordinate.
	 * @param x horizontal component
	 * @param y vertical component
	 * @return RGBA 4-channel integer array stored at the coordinates
	 */
	public short[] texture(float x, float y)
	{
		return buffer[(Math.round(x * width.size) & width.max) | 
		              ((Math.round(y * height.size) & height.max) << width.bits)];
	}
}
