
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
		VertexAttribute attribute = new VertexAttribute( 8 );
		
		// Shader Configuration
		shader = new Shader(bounds.width * 3, bounds.width, bounds.height, attribute, 
				(vertex)-> {
					shader.GL_X = vertex[0];
					shader.GL_Y = vertex[1];
					shader.GL_Z = 0.0f;
				},
				(fragment)-> {
					short[] col = shader.texture0.texture(fragment[3], fragment[4]);
					shader.GL_R = (byte)(col[Texture.R]);
					shader.GL_G = (byte)(col[Texture.G]);
					shader.GL_B = (byte)(col[Texture.B]);
				}
		);
		
		float[] v1 = new float[attribute.size];
		float[] v2 = new float[attribute.size];
		float[] v3 = new float[attribute.size];	
		float[] v4 = new float[attribute.size];	
		
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
		
		float timer = 0.0f;
		frame.setVisible(true);
		window.setVisible(true);
		window.createBufferStrategy(2);
		strategy = window.getBufferStrategy();
		//Time.vsyncEnabled = true;
		
		// Render loop
		while(true) 
		{
			v1[0] = 0.5f + aspectRatio * 0.25f * (float)Math.cos( timer + Math.PI * 0.5 );
			v2[0] = 0.5f + aspectRatio * 0.25f * (float)Math.cos( timer );
			v3[0] = 0.5f + aspectRatio * 0.25f * (float)Math.cos( timer - Math.PI * 0.5 );
			v4[0] = 0.5f + aspectRatio * 0.25f * (float)Math.cos( timer + Math.PI );
			
			v1[1] = 0.5f + 0.25f * (float)Math.sin( timer + Math.PI * 0.5 );
			v2[1] = 0.5f + 0.25f * (float)Math.sin( timer );
			v3[1] = 0.5f + 0.25f * (float)Math.sin( timer - Math.PI * 0.5 );
			v4[1] = 0.5f + 0.25f * (float)Math.sin( timer - Math.PI );
			
			Graphics2D gl = (Graphics2D)strategy.getDrawGraphics();
			
        	gl.setColor(Color.BLACK);
        	gl.fillRect(0, 0, bounds.width, bounds.height);
        	shader.render(gl, t1);
        	shader.render(gl, t2);
			
        	gl.setColor(Color.WHITE);
        	gl.drawString("FPS: " + Time.frameRate, 5, 20);
			strategy.show();
			
			timer += 0.01f;
			Time.frame();
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

