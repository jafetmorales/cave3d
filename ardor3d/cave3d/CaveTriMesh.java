package cave3d;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.Mesh;

/**
 * @author mazander
 */
final class CaveTriMesh extends Mesh {

	private static final long serialVersionUID = 1L;
	
	private final Vector3 center = new Vector3();
	
	private final Vector3 worldCenter = new Vector3();
	
	public void setCenter(Vector3 cornerCoordinates, double meshSize) {
		this.center.set(cornerCoordinates);
		this.worldCenter.set(center).multiplyLocal(meshSize);
	}
	
	public Vector3 getCenter() {
		return center;
	}
	
	public Vector3 getWorldCenter() {
		return worldCenter;
	}
}
