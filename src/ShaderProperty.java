
/**
 * This class defines the data used in the shading process.
 * It also can be used to define properties of the shading operation.
 * @version Last Edited: August/09/2020
 * @author Sean Rannie
 */
public class ShaderProperty 
{
	/** Size of vertex going into vertex shader */
	public final int inputSize;
	/** Size of vertex returned from vertex shader */
	public final int outputSize;
	/** Number of vertices in the drawn shape */
	public final int numVertex;
	
	/** Defines a Shader with no interpolated elements */
	public ShaderProperty()
	{
		this(0,0,3);
	}
	
	/**
	 * Defines the shader input/output properties
	 * @param in size of vertex going into vertex shader
	 * @param out size of vertex returned from vertex shader
	 * @param sz number of vertices in the drawn shape
	 */
	public ShaderProperty( int in, int out, int sz )
	{
		if(in < 0 || out < 0)
		{
			System.err.println( "Invalid input/output vertex length!" );
			in = 0;
			out = 0;
		}
		
		if(sz != 3)
		{
			System.err.println( "Non-triangular shapes not supported yet!" );
			sz = 3;
		}
		
		inputSize = in;
		outputSize = out;
		numVertex = sz;
	}
}
