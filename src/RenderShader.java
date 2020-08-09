import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class RenderShader 
{
	/** Vertex Shader that is applied to each vertex */
	private BiConsumer<float[], float[]> vertexShader;
	/** Fragment Shader that is applied for every rendered pixel */
	private Consumer<float[]> fragmentShader;
	
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
	/** Copy of the {@link Triangle}'s first vertex array */
	private float[] vs;
	/** Vector between the first and second vertices of the {@link Triangle} */
	private float[] svec;
	/** Vector between the first and third vertices of the {@link Triangle} */
	private float[] tvec;
	/** Holds the interpolated value between vertices, which is passed to the fragment shader */
	private float[] interpolated;
	
	/** Pre-calculated value of the reciprocal of the t vector's y component (1 / tvec[1])*/
	private float t_invY = 0.0f;
	/** Pre-calculated value of ratio between x and y components of t vector (tvec[0] / tvec[1])*/
	private float t_ratio = 0.0f;
	/** Pre-calculated value of the s vector with respects to the t vector (1.0f / (svec[0] - svec[1] * t_ratio))*/
	private float s_factor = 0.0f;
	
	/** {@link Texture} that is currently binded */
	public Texture texture0 = new Texture();
	/** Returned red of fragment shader */
	public byte GL_R = 0x00;
	/** Returned green of fragment shader */
	public byte GL_G = 0x00;
	/** Returned blue of fragment shader */
	public byte GL_B = 0x00;
	
	private float[] v1 = new float[3];
	private float[] v2 = new float[3];
	private float[] v3 = new float[3];
	
	public RenderShader(int w, int h, VertexAttribute atr, BiConsumer<float[], float[]> vShader, Consumer<float[]> fShader)
	{
		setResolution( w, h );
		setVertexAttribute( atr );
		vertexShader = vShader;
		fragmentShader = fShader;
	}
	
	public void setResolution(int w, int h)
	{
		width = w;
		height = h;
		w_scale = 1.0f / width;
		h_scale = 1.0f / height;
	}
	
	public void setVertexAttribute(VertexAttribute atr)
	{
		attribute = atr;
		svec = new float[attribute.size];
		tvec = new float[attribute.size];
		interpolated = new float[attribute.size];
	}
	
	public void render(int[] buffer, Triangle tri)
	{
		// Apply vertex shader
		vertexShader.accept( tri.v[0], v1 );
		vertexShader.accept( tri.v[1], v2 );
		vertexShader.accept( tri.v[2], v3 );
		
		// Calculate vectors between vertices
		vs = tri.v[0];
		for(int i = 0; i < attribute.size; i++)
		{
			svec[i] = tri.v[1][i] - vs[i];
			tvec[i] = tri.v[2][i] - vs[i];
		}
		
		t_invY = 1.0f / tvec[1];
		t_ratio = tvec[0] * t_invY;
		s_factor = 1.0f / (svec[0] - svec[1] * t_ratio);
				
		// Order the vertices
		float[] x_min, x_max, y_min, y_max;
		
		// X-points
		if(v1[0] > v2[0])
			if(v1[0] > v3[0]) { x_max = v1; x_min = v2[0] > v3[0] ? v3 : v2; }
			else 			  { x_max = v3; x_min = v2; }
		else
			if(v1[0] < v3[0]) { x_min = v1; x_max = v2[0] < v3[0] ? v3 : v2; }
			else 			  { x_max = v2; x_min = v3; }
		
		// Y-points
		if(v1[1] > v2[1])
			if(v1[1] > v3[1]) { y_max = v1; y_min = v2[1] > v3[1] ? v3 : v2; }
			else 			  { y_max = v3; y_min = v2; }
		else
			if(v1[1] < v3[1]) { y_min = v1; y_max = v2[1] < v3[1] ? v3 : v2; }
			else 			  { y_max = v2; y_min = v3; }
		
		int xmin = x_min[0] < 0.0f ? 0 : Math.round(x_min[0] * width);
		int ymin = y_min[1] < 0.0f ? 0 : Math.round(y_min[1] * height);
		int xmax = x_max[0] > 1.0f ?  width - 1 : Math.round(x_max[0] * width);
		int ymax = y_max[1] > 1.0f ? height - 1 : Math.round(y_max[1] * height);
		
		float ovx = ((float) xmin * w_scale) - vs[0];
		float vy = ((float) ymin * h_scale) - vs[1];
		
		// Draw triangle
		for(int y = ymin; y < ymax; y++, vy += h_scale)
		{
			float vx = ovx;
			for(int x = xmin; x < xmax; x++, vx += w_scale)
			{
				float s = (vx - vy * t_ratio) * s_factor;
				float t = (vy - s * svec[1]) * t_invY;
				if(s < 0.0f || t < 0.0f || s + t > 1.0f)
					continue;
					
				// Interpolate the vertex data
				for(byte i = 0; i < attribute.size; i++)
					interpolated[i] = vs[i] + s * svec[i] + t * tvec[i];
				
				// Apply fragment shader at coordinates
				fragmentShader.accept( interpolated );
				buffer[x + y * width] = 0xFF000000 | (GL_R << 16) | (GL_G << 8) | GL_B;
			}
		}
	}
}
