package cave3d;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme.app.SimpleGame;
import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.input.KeyBindingManager;
import com.jme.input.KeyInput;
import com.jme.light.PointLight;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;
import com.jme.renderer.Renderer;
import com.jme.scene.shape.Sphere;
import com.jme.scene.state.CullState;
import com.jme.scene.state.GLSLShaderObjectsState;
import com.jme.scene.state.MaterialState;
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
		Logger.getLogger("com.jme").setLevel(Level.WARNING);
		CaveGenerator app = new CaveGenerator();
		app.setConfigShowMode(ConfigShowMode.AlwaysShow);
		app.start();
	}
	
    private final static long SEED = "ABC".hashCode();
 
    private CaveNode caveNode = null; 
    
    private final ArrayList<CollisionNode> collisionNodes = new ArrayList<CollisionNode>();
    
    
	protected void simpleUpdate() {
		PointLight pl = (PointLight)lightState.get(0);
		pl.getLocation().set(cam.getLocation());
		caveNode.update(cam);

		float time = timer.getTimePerFrame();

		for (int i = 0; i < collisionNodes.size(); i++) {
			collisionNodes.get(i).update(time, caveNode.getScalarField());
		}
		if ( KeyBindingManager.getKeyBindingManager().isValidCommand(
                "shoot", false ) ) {
			shoot();
		}
	}
	
	private void shoot() {
       	Vector3f velocity = new Vector3f(cam.getDirection());
       	velocity.multLocal(20f);
		CollisionNode node = new CollisionNode(velocity);
    	node.attachChild(new Sphere("Sphere", 10, 10, 0.5f));
    	node.setLocalTranslation(new Vector3f(cam.getLocation()));
    	node.setModelBound(new BoundingBox());
    	node.updateModelBound();
    	MaterialState yellow =  display.getRenderer().createMaterialState();
		yellow.setAmbient(new ColorRGBA(0.1f,0.1f,0.1f,1.0f));
		yellow.setEmissive(new ColorRGBA(0,0,0,0));
		yellow.setDiffuse(new ColorRGBA(1.0f,1.0f,0.0f,1.0f));
		yellow.setSpecular(new ColorRGBA(1,0,0,1.0f));
		yellow.setShininess(32);
		node.setRenderState(yellow);
    	rootNode.attachChild(node);
    	rootNode.updateRenderState();
    	collisionNodes.add(node);
	}
	
	protected void simpleInitGame() {
		// setCursorVisible(true) for debugging in Ubuntu
		//MouseInput.get().setCursorVisible(true);
		display.setTitle("Cave Generator");
		
        KeyBindingManager.getKeyBindingManager().set( "shoot",
                KeyInput.KEY_SPACE );
		
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