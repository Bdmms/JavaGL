/**
 * This class generates sizes that allow for binary operations.
 * The maximum supported size is 65536.
 * @version Last Edited: August/08/2020
 * @author Sean Rannie
 */
public enum BinarySize 
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
	
	/**
	 * Returns {@link BinarySize} that matches the given dimensions, if one exists.
	 * @param size specified size to match
	 * @return {@link BinarySize} that matches size. Returns null if size cannot be matched.
	 */
	public static BinarySize match(int size)
	{
		switch(size)
		{
		case 1: return x1;
		case 2: return x2;
		case 4: return x4;
		case 8: return x8;
		case 16: return x16;
		case 32: return x32;
		case 64: return x64;
		case 128: return x128;
		case 256: return x256;
		case 512: return x512;
		case 1024: return x1024;
		case 2048: return x2048;
		case 4096: return x4096;
		case 8192: return x8192;
		case 16384: return x16384;
		case 32768: return x32768;
		case 65536: return x65536;
		default: return null;
		}
	}
}
