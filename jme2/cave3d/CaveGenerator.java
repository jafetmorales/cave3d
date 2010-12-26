package cave3d;
import java.net.URL;

import com.jme.app.SimpleGame;
import com.jme.image.Texture;
import com.jme.input.MouseInput;
import com.jme.light.PointLight;
import com.jme.renderer.Renderer;
import com.jme.scene.state.CullState;
import com.jme.scene.state.GLSLShaderObjectsState;
import com.jme.scene.state.TextureState;
import com.jme.scene.state.CullState.Face;
import com.jme.system.DisplaySystem;
import com.jme.util.TextureManager;
import com.jme.util.resource.ResourceLocatorTool;
import com.jme.util.resource.SimpleResourceLocator;

/**
 * @author mazander
 */
public class CaveGenerator extends SimpleGame {


	public static void main(String[] args) {
		CaveGenerator app = new CaveGenerator();
		app.setConfigShowMode(ConfigShowMode.AlwaysShow);
		MouseInput.get().setCursorVisible(true);
		app.start();
	}
	
    private final static long SEED = "ABC".hashCode();
 
    private CaveNode caveNode = null; 
    
	protected void simpleUpdate() {
		PointLight pl = (PointLight)lightState.get(0);
		pl.getLocation().set(cam.getLocation());
		caveNode.update(cam);
	}
	
	protected void simpleInitGame() {
		display.setTitle("Cave Generator");
		
		
		try {
			ClassLoader classLoader = CaveGenerator.class.getClassLoader();

			ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_SHADER, new SimpleResourceLocator(classLoader.getResource("cave3d/shaders/")));
			ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, new SimpleResourceLocator(classLoader.getResource("cave3d/textures/")));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		
        caveNode = new CaveNode(SEED, 64f, 12);
		
		
		lightState.setLocalViewer(true);
		lightState.setEnabled(true);
		
		// setup light
		PointLight pl = (PointLight)lightState.get(0);
		pl.setConstant(1f);
		pl.setLinear(0);
		pl.setQuadratic(0.000003f);
		pl.setAttenuate(true);	
	
		// texture states
        TextureState textureState = DisplaySystem.getDisplaySystem().getRenderer().createTextureState();
        float maxAnisotropic = textureState.getMaxAnisotropic();
		textureState.setTexture(createTexture("stone.jpg",        Texture.MinificationFilter.Trilinear, Texture.MagnificationFilter.Bilinear, maxAnisotropic), 0);
		textureState.setTexture(createTexture("stone-normal.jpg", Texture.MinificationFilter.Trilinear, Texture.MagnificationFilter.Bilinear, maxAnisotropic), 1 );
		caveNode.setRenderState(textureState);
		
		// shader state
		GLSLShaderObjectsState glslShaderState = getGLSLShaderState("triplanaltexturing");
		glslShaderState.setUniform("sampler", 0);
		glslShaderState.setUniform("samplerBump", 1);
		caveNode.setRenderState( glslShaderState);
		
		// back face culling state
		CullState cullState = DisplaySystem.getDisplaySystem().getRenderer().createCullState();
		cullState.setCullFace(Face.Back);
		caveNode.setRenderState(cullState);
		
		rootNode.attachChild(caveNode);
		
		float aspect = (float) display.getWidth() / (float) display.getHeight();
		cam.setFrustumPerspective(75f, aspect, 1, 4 * 64f);
	}
	
	private static GLSLShaderObjectsState getGLSLShaderState(String shaderName) {
		URL vertUrl = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_SHADER, shaderName + ".vert");
		URL fracUrl = ResourceLocatorTool.locateResource(ResourceLocatorTool.TYPE_SHADER, shaderName + ".frac");
		Renderer renderer = DisplaySystem.getDisplaySystem().getRenderer();
		GLSLShaderObjectsState shader = renderer.createGLSLShaderObjectsState();
		shader.load(vertUrl, fracUrl);
		return shader;
	}
	
	private Texture createTexture(String textureString, Texture.MinificationFilter min, Texture.MagnificationFilter mag, float maxAnisotropic) {
		Texture texture = TextureManager.loadTexture(textureString, min, mag, maxAnisotropic, false);
		texture.setWrap(Texture.WrapMode.Repeat);
		return texture;
	}
	


}