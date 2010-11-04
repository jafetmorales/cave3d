package cave3d;
import com.jme.math.Vector3f;
import com.jme.scene.TriMesh;

/**
 * @author mazander
 */
final class CaveTriMesh extends TriMesh {

	private static final long serialVersionUID = 1L;
	
	private final Vector3f center = new Vector3f();
	
	private final Vector3f worldCenter = new Vector3f();
	
	public void setCenter(Vector3f cornerCoordinates, float meshSize) {
		this.center.set(cornerCoordinates);
		this.worldCenter.set(center).multLocal(meshSize);
	}
	
	public Vector3f getCenter() {
		return center;
	}
	
	public Vector3f getWorldCenter() {
		return worldCenter;
	}
}
