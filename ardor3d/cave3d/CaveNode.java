package cave3d;

import java.util.HashMap;
import java.util.Stack;

import com.ardor3d.bounding.BoundingBox;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;
import com.ardor3d.renderer.Camera;

public class CaveNode extends com.ardor3d.scenegraph.Node {
	
	private static final long serialVersionUID = 1L;

	private final CaveScalarField scalarField;
    
    private final ScalarFieldPolygonisator polygonisator;
    
    private final Vector3 center = new Vector3();
    
    private final Vector3 key = new Vector3();
    
    private final Vector3 camGridCoord = new Vector3();
    
	private final BoundingBox box;
    
    private final PolygonizationThread generatorThread = new PolygonizationThread();

	private final float meshSize;
	
	private final HashMap<Vector3, CaveTriMesh> caveMeshes = new HashMap<Vector3, CaveTriMesh>();
	
	private int lastMeshCount = 0;
	
	private final Stack<CaveTriMesh> meshStack = new Stack<CaveTriMesh>();

	public CaveNode(long seed, float meshSize, int gridSize) {
        this.meshSize = meshSize;
		scalarField = new CaveScalarField(seed, 128f, 4f);
		polygonisator = new ScalarFieldPolygonisator(meshSize, gridSize, scalarField, false, true);
		box = new BoundingBox(new Vector3(), meshSize / 2f, meshSize / 2f, meshSize / 2f);
		
		Thread thread = new Thread(generatorThread, "Generator Thread");
		thread.start();
	}
	
	public CaveScalarField getScalarField() {
		return scalarField;
	}
	
	public void update(Camera cam) {
		camGridCoord.set(cam.getLocation()).divideLocal(meshSize);
		camGridCoord.set(Math.round(camGridCoord.getX()), Math.round(camGridCoord.getY()), Math.round(camGridCoord.getZ()));
		
		final int d = 3;
		for(int x = -d; x <= d; x++) {
			for(int y = -d; y <= d; y++) {
				for(int z = -d; z <= d; z++) {
					if(Math.sqrt(x * x + y * y + z * z) <= 4.5) {
						key.set(camGridCoord).addLocal(x,y,z);
						key.multiply(meshSize, center);
						box.setCenter(center);
						if(cam.contains(box) != Camera.FrustumIntersect.Outside) {
							generatorThread.addcaveMesh(key);
						}
					}
				}
			}
		}
		
		long time = System.currentTimeMillis();
		int quantity = getNumberOfChildren();
		for (int i = quantity - 1; i >= 0; i--) {
			CaveTriMesh mesh = (CaveTriMesh) getChild(i);
			if(mesh.getWorldBound() != null) {
				ReadOnlyVector3 worldCenter = mesh.getWorldBound().getCenter();
				if(worldCenter.distance(cam.getLocation()) > meshSize * 6) {
					detachChildAt(i);
					caveMeshes.remove(mesh.getCenter());
					meshStack.push(mesh);
				}
			}
		}
		
		if(caveMeshes.size() != lastMeshCount) {
			lastMeshCount = caveMeshes.size();
			updateGeometricState(time, true);
		}
	}
	
	private final class PolygonizationThread implements Runnable {;
		
		private final Stack<CaveTriMesh> todoStack = new Stack<CaveTriMesh>();

		private final Object lock = new Object();

		private void addcaveMesh(Vector3 center) {
			if (caveMeshes.get(center) == null) {
				CaveTriMesh mesh = meshStack.isEmpty() ? new CaveTriMesh() : meshStack.pop();
				mesh.setCenter(center, meshSize);
				todoStack.add(mesh);
				caveMeshes.put(mesh.getCenter(), mesh);
			}
			synchronized (lock) {
				lock.notify();
			}
		}

		public void run() {
			while (true) {
				while (!todoStack.isEmpty()) {
					CaveTriMesh mesh = todoStack.pop();
					polygonisator.calculate(mesh, mesh.getWorldCenter(), 0f);
			        mesh.setModelBound(new BoundingBox());
			        mesh.updateModelBound();
				    if(mesh.getMeshData().getVertexCount() > 0) {
						attachChild(mesh);
						mesh.updateWorldBound(false);
					}
					//System.out.println(todoStack.size() + "  " + getQuantity());
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
