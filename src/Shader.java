import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * This class allows for the rendering of a vertex array using bilinear interpolated shading.
 * @version Last Edited: August/08/2020
 * @author Sean Rannie
 */
public class Shader implements Paint, PaintContext
{
	public static final int R = 2;
	public static final int G = 1;
	public static final int B = 0;
	
	/** {@link Raster} that gets passed between the Graphics2D component */
	private WritableRaster raster;
	/** {@link ColorModel} used by the shader */
	private ColorModel cm;
	/** Depth Buffer used by the shader */
	public float[] depth;
	/** A copy of the rendered screen */
	public int[] frame;
	/** Buffer that is used when rending raster strips */
	private int[] buffer;
	/** Horizontal resolution of the rendering area */
	private int width;
	/** Vertical resolution of the rendering area */
	private int height;
	/** Width of a pixel relative to the total width. (1 / width) */
	private float w_scale;
	/** Height of a pixel relative to the total height (1 / eight) */
	private float h_scale;
	
	/** {@link ShaderProperty} that is used by shader */
	private ShaderProperty attribute;
	/** Stores final x position of vertex for later */
	private float[] XPos;
	/** Stores final y position of vertex for later */
	private float[] YPos;
	/** Stores final z position of vertex for later */
	private float[] ZPos;
	/** Stores result of vertex shader */
	private float[][] vertexOut;
	/** Copy of the vertex array's first vertex array */
	private float[] vs;
	/** Array of final x values used to draw the vertex array */
	private int[] xs;
	/** Array of final y values used to draw the vertex array */
	private int[] ys;
	
	/** Vector between the first and second vertices of the vertex array */
	private float[] svec;
	/** Vector between the first and third vertices of the vertex array */
	private float[] tvec;
	/** Cached vector used when rendering stripes */
	private float[] cache_vec;
	/** Holds the interpolated value between vertices, which is passed to the fragment shader */
	public float[] interpolated;
	
	private float dsvec;
	private float dtvec;
	private float dcvec;
	
	/** Pre-calculated value of the reciprocal of the t vector's y component (1 / tvec[1])*/
	private float t_invY = 0.0f;
	/** Pre-calculated value of ratio between x and y components of t vector (tvec[0] / tvec[1])*/
	private float t_ratio = 0.0f;
	/** Pre-calculated value of the s vector with respects to the t vector (1.0f / (svec[0] - svec[1] * t_ratio))*/
	private float s_factor = 0.0f;
	/** Pre-calculated y component of s vector */
	private float svecy = 0.0f;
	
	/** Vertex Shader that is applied to each vertex */
	private BiConsumer<float[], float[]> vertexShader;
	/** Fragment Shader that is applied for every rendered pixel */
	private Consumer<float[]> fragmentShader;
	
	/** {@link Texture} that is currently binded */
	public Texture texture0 = new Texture();
	
	public int GL_INDEX = 0;
	/** Returned X-component of vertex shader */
	public float GL_X = 0.0f;
	/** Returned Y-component of vertex shader */
	public float GL_Y = 0.0f;
	/** Returned Z-component of vertex shader */
	public float GL_Z = 0.0f;
	public float GL_DEPTH = 0.0f;
	
	/**
	 * Sets the resolution and shading functions used by the {@link Shader}.
	 * @param w horizontal resolution of the screen
	 * @param h vertical resolution of the screen
	 * @param property {@link ShaderProperty} that defines the vertex structure
	 * @param vShader {@link Consumer} that is used as the vertex shader
	 * @param fShader {@link Consumer} that is used as the fragment shader
	 */
	public Shader(int w, int h, ShaderProperty property, BiConsumer<float[], float[]> vShader, Consumer<float[]> fShader)
	{
		buffer = new int[w];
		depth = new float[w * h];
		frame = new int[w * h];
		
		DataBufferInt dataBuffer = new DataBufferInt(buffer, buffer.length);
		cm = new DirectColorModel(24, 0xFF0000, 0xFF00, 0xFF);
		SampleModel sm = cm.createCompatibleSampleModel(w, 1);
		raster = Raster.createWritableRaster(sm, dataBuffer, null);
		
		setResolution( w, h );
		setProperty( property );
		vertexShader = vShader;
		fragmentShader = fShader;
	}
	
	/**
	 * Updates the resolution that the {@link Shader} renders in.
	 * @param w horizontal resolution of the screen
	 * @param h vertical resolution of the screen
	 */
	public void setResolution(int w, int h)
	{
		width = w;
		height = h;
		w_scale = 1.0f / width;
		h_scale = 1.0f / height;
	}
	
	/**
	 * Sets the {@link ShaderProperty} to be used by the {@link Shader}.
	 * @param atr {@link ShaderProperty} that defines the vertex structure
	 */
	public void setProperty(ShaderProperty atr)
	{
		attribute = atr;

		xs = new int[attribute.numVertex];
		ys = new int[attribute.numVertex];
		XPos = new float[attribute.numVertex];
		YPos = new float[attribute.numVertex];
		ZPos = new float[attribute.numVertex];
		svec = new float[attribute.outputSize];
		tvec = new float[attribute.outputSize];
		cache_vec = new float[attribute.outputSize];
		interpolated = new float[attribute.outputSize];
		vertexOut = new float[attribute.numVertex][attribute.outputSize];
	}
	
	public void clearDepth()
	{
		for(int i = 0; i < depth.length; i++)
		{
			frame[i] = 0;
			depth[i] = 0.0f;
		}
	}
	
	/**
	 * Renders a vertex array using this {@link Shader}.
	 * @param gl reference to {@link Graphics2D} 
	 * @param vertexArray array of vertices
	 */
	public void render(Graphics2D gl, float[][] vertexIn)
	{
		// Apply vertex shader
		for(int i = 0; i < attribute.numVertex; i++)
		{
			vertexShader.accept(vertexIn[i], vertexOut[i]);
			XPos[i] = GL_X;
			YPos[i] = GL_Y;
			ZPos[i] = GL_Z;
			xs[i] = Math.round(GL_X * width);
			ys[i] = Math.round(GL_Y * height);
		}
		
		// Calculate vectors between vertices
		vs = vertexOut[0];
		for(int i = 0; i < attribute.outputSize; i++)
		{
			svec[i] = vertexOut[1][i] - vs[i];
			tvec[i] = vertexOut[2][i] - vs[i];
		}
		dsvec = ZPos[1] - ZPos[0];
		dtvec = ZPos[2] - ZPos[0];
		
		svecy = YPos[1] - YPos[0];
		t_invY = 1.0f / (YPos[2] - YPos[0]);
		t_ratio = (XPos[2] - XPos[0]) * t_invY;
		s_factor = 1.0f / ((XPos[1] - XPos[0]) - svecy * t_ratio);
		
		// Update cached interpolation vector
		for(int i = 0; i < attribute.outputSize; i++)
			cache_vec[i] = (svec[i] - svecy * t_invY * tvec[i]) * w_scale * s_factor;
		dcvec = (dsvec - svecy * t_invY * dtvec) * w_scale * s_factor;
		
		// Render the triangle
		gl.setPaint( this );
		gl.fillPolygon(xs, ys, attribute.numVertex);
	}

	/**
	 * Applies fragment shading to currently rendered vertex array. The {@link Raster} is always requested in horizontal strips. 
	 * Instead of returning a new {@link Raster} each time, the function returns the same {@link Raster} with the contents of the buffer modified.
	 * @param x offset along horizontal axis
	 * @param y offset along vertical axis
	 * @param w width of the current strip to render
	 * @param h height of the current strip to render (always expected to be 1)
	 */
	@Override
	public Raster getRaster(int x, int y, int w, int h) 
	{
		GL_INDEX = x + y * width;
		float vyt = y * h_scale - YPos[0];
		float vxs = ( ( x * w_scale - XPos[0] - vyt * t_ratio) * s_factor);
		vyt = (vyt - vxs * svecy) * t_invY;
		
		// Interpolate the vertex data
		for( y = 0; y < attribute.outputSize; y++)
			interpolated[y] = vs[y] + vxs * svec[y] + vyt * tvec[y] - cache_vec[y];
		GL_DEPTH = ZPos[0] + vxs * dsvec + vyt * dtvec - dcvec;
		
		// Increment variables for the remainder of the strip
		for( x = 0; x < w; x++, GL_INDEX++ )
		{
			for( y = 0; y < attribute.outputSize; y++)
				interpolated[y] += cache_vec[y];
			GL_DEPTH += dcvec;
			
			// Check depth buffer and apply fragment shader at coordinates
			if(depth[GL_INDEX] < GL_DEPTH)
				fragmentShader.accept( interpolated );
			
			buffer[x] = frame[GL_INDEX];
		}
		
		return raster;
	}

	/**
	 * Method used by {@link Paint} to receive {@link PaintContext}. 
	 * {@link Shader} implements {@link Paint} and {@link PaintContext}, which as
	 * a result causes the method to return itself each time it's called.
	 * @param cm ignored
	 * @param deviceBounds ignored
	 * @param userBounds ignored
	 * @param xform ignored
	 * @param hints ignored
	 */
	@Override
	public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds, AffineTransform xform, RenderingHints hints) 
	{
		return this; 
	}
	
	/**
	 * The {@link Shader} is always opaque.
	 * @return Transparency.OPAQUE
	 */
	@Override
	public int getTransparency()  { return Transparency.OPAQUE; }
	
	/**
	 * Disposes of contents currently used by the {@link Shader}.
	 */
	@Override
	public void dispose() { }
	
	/**
	 * Returns the {@link ColorModel} used by the {@link Shader}.
	 */
	@Override
	public ColorModel getColorModel() { return cm; }
}
