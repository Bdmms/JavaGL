import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.function.Consumer;

/**
 * This class allows for the rendering of a vertex array using bilinear interpolated shading.
 * @version Last Edited: August/08/2020
 * @author Sean Rannie
 */
public class Shader implements Paint, PaintContext
{
	/** {@link Raster} that gets passed between the Graphics2D component */
	private WritableRaster raster;
	/** {@link ColorModel} used by the shader */
	private ColorModel cm;
	/** Buffer that is used when rending raster strips */
	private byte[] buffer;
	
	/** Horizontal resolution of the rendering area */
	private int width;
	/** Vertical resolution of the rendering area */
	private int height;
	/** Width of a pixel relative to the total width. (1 / width) */
	private float w_scale;
	/** Height of a pixel relative to the total height (1 / eight) */
	private float h_scale;
	
	/** {@link VertexAttribute} that is used by shader */
	private VertexAttribute attribute;
	/** Copy of the vertex array's first vertex array */
	private float[] vs;
	/** Array of final x values used to draw the vertex array */
	private int[] xs = new int[3];
	/** Array of final y values used to draw the vertex array */
	private int[] ys = new int[3];
	
	/** Vector between the first and second vertices of the vertex array */
	private float[] svec;
	/** Vector between the first and third vertices of the vertex array */
	private float[] tvec;
	/** Holds the interpolated value between vertices, which is passed to the fragment shader */
	private float[] interpolated;
	
	/** Pre-calculated value of the reciprocal of the t vector's y component (1 / tvec[1])*/
	private float t_invY = 0.0f;
	/** Pre-calculated value of ratio between x and y components of t vector (tvec[0] / tvec[1])*/
	private float t_ratio = 0.0f;
	/** Pre-calculated value of the s vector with respects to the t vector (1.0f / (svec[0] - svec[1] * t_ratio))*/
	private float s_factor = 0.0f;
	
	/** Vertex Shader that is applied to each vertex */
	private Consumer<float[]> vertexShader;
	/** Fragment Shader that is applied for every rendered pixel */
	private Consumer<float[]> fragmentShader;
	
	/** {@link Texture} that is currently binded */
	public Texture texture0 = new Texture();
	/** Returned X-component of vertex shader */
	public float GL_X = 0.0f;
	/** Returned Y-component of vertex shader */
	public float GL_Y = 0.0f;
	/** Returned Z-component of vertex shader */
	public float GL_Z = 0.0f;
	/** Returned red of fragment shader */
	public byte GL_R = 0x00;
	/** Returned green of fragment shader */
	public byte GL_G = 0x00;
	/** Returned blue of fragment shader */
	public byte GL_B = 0x00;
	
	/**
	 * Sets the resolution and shading functions used by the {@link Shader}.
	 * @param bufferSize size of the buffer used by the raster strips (screen width is recommended)
	 * @param w horizontal resolution of the screen
	 * @param h vertical resolution of the screen
	 * @param atr {@link VertexAttribute} that defines the vertex structure
	 * @param vShader {@link Consumer} that is used as the vertex shader
	 * @param fShader {@link Consumer} that is used as the fragment shader
	 */
	public Shader(int bufferSize, int w, int h, VertexAttribute atr, Consumer<float[]> vShader, Consumer<float[]> fShader)
	{
		buffer = new byte[bufferSize * 4];
		
		DataBufferByte dataBuffer = new DataBufferByte(buffer, buffer.length);
		cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB), false, true, ColorModel.OPAQUE, DataBuffer.TYPE_BYTE);
		SampleModel sm = new PixelInterleavedSampleModel(DataBuffer.TYPE_BYTE, w, 1, 3, bufferSize, new int[] { 2, 1, 0 } );
		raster = Raster.createWritableRaster(sm, dataBuffer, null);
		
		setResolution( w, h );
		setVertexAttribute( atr );
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
	 * Sets the {@link VertexAttribute} to be used by the {@link Shader}.
	 * @param atr {@link VertexAttribute} that defines the vertex structure
	 */
	public void setVertexAttribute(VertexAttribute atr)
	{
		attribute = atr;
		svec = new float[attribute.size];
		tvec = new float[attribute.size];
		interpolated = new float[attribute.size];
	}
	
	/**
	 * Renders a vertex array using this {@link Shader}.
	 * @param gl reference to {@link Graphics2D} 
	 * @param vertexArray array of vertices
	 */
	public void render(Graphics2D gl, float[][] vertexArray)
	{
		// Apply vertex shader
		for(int i = 0; i < vertexArray.length; i++)
		{
			vertexShader.accept(vertexArray[i]);
			xs[i] = Math.round(GL_X * width);
			ys[i] = Math.round(GL_Y * height);
		}
		
		// Calculate vectors between vertices
		vs = vertexArray[0];
		for(int i = 0; i < attribute.size; i++)
		{
			svec[i] = vertexArray[1][i] - vs[i];
			tvec[i] = vertexArray[2][i] - vs[i];
		}
		
		t_invY = 1.0f / tvec[1];
		t_ratio = tvec[0] * t_invY;
		s_factor = 1.0f / (svec[0] - svec[1] * t_ratio);
		
		// Render the triangle
		gl.setPaint( this );
		gl.fillPolygon(xs, ys, vertexArray.length);
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
		float vx = ((float) x * w_scale) - vs[0];
		float vy = ((float) y * h_scale) - vs[1];
		w *= 3;
		
		for(short px = 0; px < w; vx += w_scale)
		{
			float s = (vx - vy * t_ratio) * s_factor;
			float t = (vy - s * svec[1]) * t_invY;
			
			// Interpolate the vertex data
			for(byte i = 0; i < attribute.size; i++)
				interpolated[i] = vs[i] + s * svec[i] + t * tvec[i];

			// Apply fragment shader at coordinates
			fragmentShader.accept( interpolated );
			buffer[px++] = GL_B;
			buffer[px++] = GL_G;
			buffer[px++] = GL_R;
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
