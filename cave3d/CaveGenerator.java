package cave3d;
import java.net.URL;
import java.util.HashMap;
import java.util.Stack;

import com.jme.app.SimpleGame;
import com.jme.bounding.BoundingBox;
import com.jme.image.Texture;
import com.jme.light.PointLight;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.renderer.Renderer;
import com.jme.scene.Node;
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
		//MouseInput.get().setCursorVisible(true);
		app.start();
	}
	
    private final static long SEED = "jme".hashCode();
    
    private final static float MESH_SIZE = 64f;
    
    private final static float HALF_MESH_SIZE = MESH_SIZE / 2f;
	
	private final HashMap<Vector3f, CaveTriMesh> caveMeshes = new HashMap<Vector3f, CaveTriMesh>();
	
	private int lastMeshCount = 0;
	
    private Node caveNode = new Node(); 
    
    private CaveScalarField scalarField;
    private ScalarFieldPolygonisator polygonisator;

    private final Vector3f key = new Vector3f();
    
    private final Vector3f camGridCoord = new Vector3f();
    
    private final PolygonizationThread generatorThread = new PolygonizationThread();
    
	private final BoundingBox box = new BoundingBox(new Vector3f(), HALF_MESH_SIZE, HALF_MESH_SIZE, HALF_MESH_SIZE);
    
	protected void simpleUpdate() {
		PointLight pl = (PointLight)lightState.get(0);
		pl.getLocation().set(cam.getLocation());
		
		camGridCoord.set(cam.getLocation()).divideLocal(MESH_SIZE);
		camGridCoord.set(Math.round(camGridCoord.x), Math.round(camGridCoord.y), Math.round(camGridCoord.z));
		
		final int d = 3;
		for(int x = -d; x <= d; x++) {
			for(int y = -d; y <= d; y++) {
				for(int z = -d; z <= d; z++) {
					key.set(camGridCoord).addLocal(x,y,z);
					box.getCenter().set(key).multLocal(MESH_SIZE);
					if(cam.contains(box) != Camera.FrustumIntersect.Outside) {
						generatorThread.addcaveMesh(key);
					}
				}
			}
		}
		
		long time = System.currentTimeMillis();
		int quantity = caveNode.getQuantity();
		for (int i = quantity - 1; i >= 0; i--) {
			CaveTriMesh mesh = (CaveTriMesh) caveNode.getChild(i);
			Vector3f worldCenter = mesh.getWorldBound().getCenter();
			if(worldCenter.distance(cam.getLocation()) > MESH_SIZE * 6) {
				caveNode.detachChildAt(i);
				caveMeshes.remove(mesh.getCenter());
			}
		}
		
		if(caveMeshes.size() != lastMeshCount) {
			lastMeshCount = caveMeshes.size();
			caveNode.updateGeometricState(time, true);
			caveNode.updateRenderState();
			System.out.println("Mesh count: " + caveNode.getQuantity());
		}
	}
	
	protected void simpleInitGame() {
		
		
		try {
			ClassLoader classLoader = CaveGenerator.class.getClassLoader();

			ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_SHADER, new SimpleResourceLocator(Thread
					.currentThread().getContextClassLoader().getResource("cave3d/shaders/")));
			ResourceLocatorTool.addResourceLocator(ResourceLocatorTool.TYPE_TEXTURE, new SimpleResourceLocator(
					classLoader.getResource("cave3d/textures/")));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		display.setTitle("Cave Generator");
		lightState.setLocalViewer(true);
		lightState.setEnabled(true);
		
		PointLight pl = (PointLight)lightState.get(0);
		pl.setConstant(1f);
		pl.setLinear(0);
		pl.setQuadratic(0.000001f);
		pl.setAttenuate(true);
		
		
        caveNode = new Node();
        scalarField = new CaveScalarField(SEED, 128f, 4f);
		polygonisator = new ScalarFieldPolygonisator(MESH_SIZE, 8, scalarField, false, false);	
		
	
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
		cam.setFrustumPerspective(75f, aspect, 1, 4 * MESH_SIZE);
		
		Thread thread = new Thread(generatorThread, "Generator Thread");
		thread.start();
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
	
	private final class PolygonizationThread implements Runnable {
		private final Stack<CaveTriMesh> stack = new Stack<CaveTriMesh>();

		private final Object lock = new Object();

		private void addcaveMesh(Vector3f center) {
			if (caveMeshes.get(center) == null) {
				CaveTriMesh mesh = new CaveTriMesh(center, MESH_SIZE);
				stack.add(mesh);
				caveMeshes.put(mesh.getCenter(), mesh);
			}
			synchronized (lock) {
				lock.notify();
			}

		}

		public void run() {
			while (true) {
				while (!stack.isEmpty()) {
					CaveTriMesh mesh = stack.pop();
					polygonisator.calculate(mesh, mesh.getWorldCenter(), 0f);
			        mesh.setModelBound(new BoundingBox());
			        mesh.updateModelBound();
					caveNode.attachChild(mesh);
				}
				try {
					synchronized (lock) {
						lock.wait();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

}