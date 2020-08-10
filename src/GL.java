
import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferStrategy;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * Showcase of the capabilities of {@link Shader}
 * @author Sean Rannie
 */
public class GL implements KeyListener
{
	// Display window
	private Window window;				//Display window
	private Frame frame;				//Display frame
	private BufferStrategy strategy;	//Buffer strategy
	private Rectangle bounds;			//Boundaries of screen
	private float aspectRatio;
	
	/** {@link Shader} used in rendering */
	private Shader shader;
			
	public static void main(String[] args)
	{
		double data = ((double)0x3F800000 * 0x3F800000);
		System.out.println( "Required cache size = " + (data / (1024.0 * 1024 * 1024 * 1024))  + " TB");
		//System.out.println( Long.toHexString(data));
		
		//testFloat();
		new GL().show();
	}
	
	/**
	 * Initializes the window and frame
	 */
	public GL()
	{
		//System.loadLibrary("immintrin");
		
		GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0].getConfigurations()[0];
		bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0].getConfigurations()[0].getBounds();
		
		//bounds.width /= 2;
		//bounds.height /= 2;
		
		// Frame Configuration
		frame = new Frame(gc);
		frame.setSize(100, 100);
		frame.setTitle("Java GL");
		frame.addKeyListener(this);
		
		// Window Configuration
		window = new Window(frame, gc);
		window.setSize(bounds.width, bounds.height);
		window.setLocation(bounds.x, bounds.y);
		
		aspectRatio = (float)bounds.height / bounds.width;
	}
	
	public static void testFloat()
	{
		for(float x = 0.0f; x <= 1.0f; x += 0.001f)
		{
			System.out.println(x + ": 0x" + Integer.toHexString(Float.floatToIntBits(x)) );
		}
	}
	
	/**
	 * Shows the contents of the window/frame.
	 */
	public void show()
	{
		// Attribute tells shader what to expect as input and output
		ShaderProperty attribute = new ShaderProperty( 8, 3, 3 );
		
		// Shader Configuration
		shader = new Shader(bounds.width, bounds.height, attribute, 
				(in, out)-> {
					shader.GL_X = in[0];
					shader.GL_Y = in[1];
					shader.GL_Z = in[2]; // Depth
					out[0] = in[3]; // TX
					out[1] = in[4]; // TY
				},
				(fragment)-> {
					int i = shader.texture0.texture(fragment[0], fragment[1]);
					float vx = fragment[0] - 0.5f;
					float vy = fragment[1] - 0.5f;
					float dst = (float)Math.sqrt(vx * vx + vy * vy) * 2;
					
					if(dst >= 1.0f)
						return;
					
					float lt = 1.0f - dst;
					shader.depth[shader.GL_INDEX] = shader.GL_DEPTH; 
					shader.frame[shader.GL_INDEX] = ((byte)(shader.texture0.buffer[i | Texture.R] * lt) << 16) | 
							 						 ((byte)(shader.texture0.buffer[i | Texture.G] * lt) << 8) | 
							 						  (byte)(shader.texture0.buffer[i | Texture.B] * lt) ;
				}
		);
		
		float[] v1 = new float[8];
		float[] v2 = new float[8];
		float[] v3 = new float[8];	
		float[] v4 = new float[8];	
		
		v1[2] = 0.1f;
		v2[2] = 0.1f;
		v3[2] = 0.2f;
		v4[2] = 0.1f;
		v1[3] = 0.0f; v1[4] = 0.0f; v1[5] = 1.0f; v1[6] = 0.0f; v1[7] = 0.0f;
		v2[3] = 1.0f; v2[4] = 0.0f; v2[5] = 0.0f; v2[6] = 1.0f; v2[7] = 0.0f; 
		v3[3] = 1.0f; v3[4] = 1.0f; v3[5] = 0.0f; v3[6] = 0.0f; v3[7] = 1.0f;
		v4[3] = 0.0f; v4[4] = 1.0f; v4[5] = 1.0f; v4[6] = 1.0f; v4[7] = 1.0f;
		
		float[][] t1 = new float[][] { v1, v2, v3 };
		float[][] t2 = new float[][] { v1, v3, v4 };
		
		try 
		{
			shader.texture0 = new Texture(ImageIO.read(new File("wood_floor.jpg")));
		} 
		catch (IOException e1) { e1.printStackTrace(); }
		
		double timer = 0.0f;
		frame.setVisible(true);
		window.setVisible(true);
		window.createBufferStrategy(2);
		strategy = window.getBufferStrategy();
		//Time.vsyncEnabled = true;
		
		// Render loop
		while(true) 
		{
			// Update vertex position
			v1[0] = 0.55f + aspectRatio * 0.25f * (float)Math.cos( timer + Math.PI * 0.5 );
			v2[0] = 0.55f + aspectRatio * 0.25f * (float)Math.cos( timer );
			v3[0] = 0.55f + aspectRatio * 0.25f * (float)Math.cos( timer - Math.PI * 0.5 );
			v4[0] = 0.55f + aspectRatio * 0.25f * (float)Math.cos( timer + Math.PI );
			v1[1] = 0.5f + 0.25f * (float)Math.sin( timer + Math.PI * 0.5 );
			v2[1] = 0.5f + 0.25f * (float)Math.sin( timer );
			v3[1] = 0.5f + 0.25f * (float)Math.sin( timer - Math.PI * 0.5 );
			v4[1] = 0.5f + 0.25f * (float)Math.sin( timer - Math.PI );
			
			// Get graphics
			Graphics2D gl = (Graphics2D)strategy.getDrawGraphics();
        	gl.setColor(Color.BLACK);
        	gl.fillRect(0, 0, bounds.width, bounds.height);
        	
        	// Render triangles
        	shader.clearDepth();
        	shader.render(gl, t1);
        	shader.render(gl, t2);
        	
        	v1[0] -= 0.1f; 
        	v2[0] -= 0.1f; 
        	v3[0] -= 0.1f; 
        	v4[0] -= 0.1f;
        	
        	shader.render(gl, t1);
        	shader.render(gl, t2);
			
        	// Show FPS
        	gl.setColor(Color.WHITE);
        	gl.drawString("FPS: " + Time.frameRate, 5, 20);
			strategy.show();
			
			Time.frame();
			timer += Time.deltaTime;
		}
	}
	
	/**
	 * Any key press will end the program.
	 * @param e {@link KeyEvent} for key press
	 */
	public void keyPressed(KeyEvent e) 
	{
		window.dispose();
		frame.dispose();
		System.exit(-1);
	}
	
	@Deprecated
	public void keyReleased(KeyEvent arg0) {}
	@Deprecated
	public void keyTyped(KeyEvent arg0) {}
}	

