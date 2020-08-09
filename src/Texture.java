import java.awt.image.BufferedImage;

/**
 * This class stores a copy of a BufferedImage as an integer array and
 * allows for floating-point based indexing.
 * @version Last Edited: August/08/2020
 * @author Sean Rannie
 */
public class Texture 
{
	public final static byte NUM_CHANNELS = 4;
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
		buffer = new short[width.size * height.size][NUM_CHANNELS];
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
		buffer = new short[width.size * height.size][NUM_CHANNELS];
		
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
	
	/**
	 * This enumerator generates sizes that allow for binary operations.
	 * The maximum supported size is 65536.
	 */
	public static enum BinarySize 
	{
		x1(0),
		x2(1),
		x4(2),
		x8(3),
		x16(4),
		x32(5),
		x64(6),
		x128(7),
		x256(8),
		x512(9),
		x1024(10),
		x2048(11),
		x4096(12),
		x8192(13),
		x16384(14),
		x32768(15),
		x65536(16);
		
		/** The size of the given value. */
		public int size;
		/** The maximum value that will be less than size. Always equal to size - 1. */
		public int max;
		/** Number of bits required to iterate through an array of this size */
		public int bits;
		
		/** Internal Constructor for {@link BinarySize} */
		private BinarySize(int bits)
		{
			this.bits = bits;
			size = 1 << bits;
			max = size - 1;
		}
		
		/**
		 * Returns minimum {@link BinarySize} required to match the given dimensions
		 * @param size specified size to match
		 * @return {@link BinarySize} that is appropriate for size
		 */
		public static BinarySize closestMatch(int size)
		{
				 if(size <= 1)  return x1;
			else if(size <= 2)  return x2;
			else if(size <= 4)  return x4;
			else if(size <= 8)  return x8;
			else if(size <= 16)  return x16;
			else if(size <= 32)  return x32;
			else if(size <= 64)  return x64;
			else if(size <= 128)  return x128;
			else if(size <= 256)  return x256;
			else if(size <= 512)  return x512;
			else if(size <= 1024) return x1024;
			else if(size <= 2048) return x2048;
			else if(size <= 4096) return x4096;
			else if(size <= 8192) return x8192;
			else if(size <= 16384) return x16384;
			else if(size <= 32768) return x32768;
			else return x65536;
		}
	}
}
