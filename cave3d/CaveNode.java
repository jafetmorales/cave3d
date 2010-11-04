package cave3d;

import java.util.HashMap;
import java.util.Stack;

import com.jme.bounding.BoundingBox;
import com.jme.math.Vector3f;
import com.jme.renderer.Camera;
import com.jme.scene.Node;

public class CaveNode extends Node {
	
	private static final long serialVersionUID = 1L;

	private final CaveScalarField scalarField;
    
    private final ScalarFieldPolygonisator polygonisator;
    
    private final Vector3f key = new Vector3f();
    
    private final Vector3f camGridCoord = new Vector3f();
    
	private final BoundingBox box;
    
    private final PolygonizationThread generatorThread = new PolygonizationThread();

	private final float meshSize;
	
	private final HashMap<Vector3f, CaveTriMesh> caveMeshes = new HashMap<Vector3f, CaveTriMesh>();
	
	private int lastMeshCount = 0;
	
	private final Stack<CaveTriMesh> meshStack = new Stack<CaveTriMesh>();

	public CaveNode(long seed, float meshSize, int gridSize) {
        this.meshSize = meshSize;
		scalarField = new CaveScalarField(seed, 128f, 4f);
		polygonisator = new ScalarFieldPolygonisator(meshSize, gridSize, scalarField, false, true);
		box = new BoundingBox(new Vector3f(), meshSize / 2f, meshSize / 2f, meshSize / 2f);
		
		Thread thread = new Thread(generatorThread, "Generator Thread");
		thread.start();
	}
	
	public void update(Camera cam) {
		camGridCoord.set(cam.getLocation()).divideLocal(meshSize);
		camGridCoord.set(Math.round(camGridCoord.x), Math.round(camGridCoord.y), Math.round(camGridCoord.z));
		
		final int d = 3;
		for(int x = -d; x <= d; x++) {
			for(int y = -d; y <= d; y++) {
				for(int z = -d; z <= d; z++) {
					key.set(camGridCoord).addLocal(x,y,z);
					box.getCenter().set(key).multLocal(meshSize);
					if(cam.contains(box) != Camera.FrustumIntersect.Outside) {
						generatorThread.addcaveMesh(key);
					}
				}
			}
		}
		
		long time = System.currentTimeMillis();
		int quantity = getQuantity();
		for (int i = quantity - 1; i >= 0; i--) {
			CaveTriMesh mesh = (CaveTriMesh) getChild(i);
			Vector3f worldCenter = mesh.getWorldBound().getCenter();
			if(worldCenter.distance(cam.getLocation()) > meshSize * 6) {
				detachChildAt(i);
				caveMeshes.remove(mesh.getCenter());
				meshStack.push(mesh);
			}
		}
		
		if(caveMeshes.size() != lastMeshCount) {
			lastMeshCount = caveMeshes.size();
			updateGeometricState(time, true);
			updateRenderState();
		}
	}
	
	private final class PolygonizationThread implements Runnable {;
		
		private final Stack<CaveTriMesh> todoStack = new Stack<CaveTriMesh>();

		private final Object lock = new Object();

		private void addcaveMesh(Vector3f center) {
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
					attachChild(mesh);
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
