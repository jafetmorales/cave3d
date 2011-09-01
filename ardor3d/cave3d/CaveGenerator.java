package cave3d;
import java.io.IOException;

import com.ardor3d.example.ExampleBase;
import com.ardor3d.image.Texture;
import com.ardor3d.image.Texture.MagnificationFilter;
import com.ardor3d.image.Texture.MinificationFilter;
import com.ardor3d.light.PointLight;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;
import com.ardor3d.renderer.state.CullState;
import com.ardor3d.renderer.state.GLSLShaderObjectsState;
import com.ardor3d.renderer.state.TextureState;
import com.ardor3d.renderer.state.ZBufferState;
import com.ardor3d.util.ReadOnlyTimer;
import com.ardor3d.util.TextureManager;
import com.ardor3d.util.resource.ResourceLocatorTool;

/**
 * @author mazander
 */
public class CaveGenerator extends ExampleBase  {
	

    private static final String CAVE3D_TEXTURES = "cave3d/textures/";

	private static final String CAVE3D_SHADERS = "cave3d/shaders/";


	public static void main(final String[] args) {
        start(CaveGenerator.class);
    }
    
    private final static long SEED = "ABC".hashCode();
    
    private CaveNode caveNode = null; 

    @Override
    protected void updateExample(final ReadOnlyTimer timer) {
		PointLight pl = (PointLight)_lightState.get(0);
		Camera camera = _canvas.getCanvasRenderer().getCamera();
		ReadOnlyVector3 location = camera.getLocation();
		pl.setLocation(location);
		caveNode.update(camera);
    }
    
    @Override
    protected void initExample() {
        _canvas.setTitle("CaveGenerator");

		
        caveNode = new CaveNode(SEED, 64f, 12);
		
		ZBufferState zBufferState = new ZBufferState();
		caveNode.setRenderState(zBufferState);
        
		// setup light
		PointLight pl = (PointLight)_lightState.get(0);
		pl.setConstant(1f);
		pl.setLinear(0);
		pl.setQuadratic(0.000003f);
		pl.setAttenuate(true);	
	
		// texture states
        TextureState textureState = new TextureState();
		textureState.setTexture(createTexture("stone.jpg"), 0);
		textureState.setTexture(createTexture("stone-normal.jpg"), 1 );
		caveNode.setRenderState(textureState);
		
		// shader state
		try {
			GLSLShaderObjectsState glslShaderState = getGLSLShaderState("triplanaltexturing");
			glslShaderState.setUniform("sampler", 0);
			glslShaderState.setUniform("samplerBump", 1);
			caveNode.setRenderState( glslShaderState);
		} catch (IOException e) {
			e.printStackTrace();
		}

		
		// back face culling state
		CullState cullState = new CullState();
		cullState.setCullFace(CullState.Face.Back);
		caveNode.setRenderState(cullState);
		
		_root.attachChild(caveNode);
		
		Camera camera = _canvas.getCanvasRenderer().getCamera();
		float aspect = (float) camera.getWidth() / (float) camera.getHeight();
		camera.setFrustumPerspective(75f, aspect, 1, 4 * 64f);
	}
	
	private static GLSLShaderObjectsState getGLSLShaderState(String shaderName) throws IOException {
		GLSLShaderObjectsState shader = new GLSLShaderObjectsState();
		shader.setVertexShader(ResourceLocatorTool.getClassPathResourceAsStream(CaveGenerator.class, CAVE3D_SHADERS + shaderName + ".vert"));
		shader.setFragmentShader(ResourceLocatorTool.getClassPathResourceAsStream(CaveGenerator.class, CAVE3D_SHADERS +shaderName + ".frac"));
		return shader;
	}
	
	private Texture createTexture(String textureString) {
		Texture texture = TextureManager.load(CAVE3D_TEXTURES + textureString, MinificationFilter.Trilinear, false);
		texture.setMagnificationFilter(MagnificationFilter.Bilinear);
		texture.setWrap(Texture.WrapMode.Repeat);
		return texture;
	}
	


}