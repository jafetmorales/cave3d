package cave3d;
import java.util.HashMap;

import com.jme.math.Vector3f;
import com.jme.renderer.Camera.FrustumIntersect;
import com.jme.scene.TriMesh;

/**
 * @author mazander
 */
final class CaveTriMesh extends TriMesh {

	private static final long serialVersionUID = 1L;
	
	private static final HashMap<Vector3f, CaveTriMesh> cache = new HashMap<Vector3f, CaveTriMesh>();

	private final Vector3f center;
	
	private final Vector3f worldCenter;

	private final float meshSize;
	
	private long lastFrustumTime = System.currentTimeMillis();

	public CaveTriMesh(Vector3f cornerCoordinates, float meshSize) {
		this.meshSize = meshSize;
		this.center = new Vector3f(cornerCoordinates);
		this.worldCenter = new Vector3f(center).multLocal(meshSize);
	}
	
	public Vector3f getCenter() {
		return center;
	}
	
	public Vector3f getWorldCenter() {
		return worldCenter;
	}

	
	@Override
	public String toString() {
		return "caveTriMesh: "+ center + ": " + getTriangleCount();
	}

	public boolean isInFrustum(long time, FrustumIntersect fi) {
		if(fi == FrustumIntersect.Outside) {
			System.out.println(fi);
			long outsideTime = time - lastFrustumTime;
			return outsideTime < 5000L;
		} else {
			lastFrustumTime = time;
			return true;
		}
	}
}
