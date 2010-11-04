package cave3d;
import java.util.Random;

import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;

/**
 * @author mazander
 */
final class CaveScalarField implements ScalarField {

	private final Vector3f tmp = new Vector3f();
	
	private final Noise3D[] noises;
	
	private final Noise3D stalactites;
	
	private final Noise3D colors;

	private int voxelCount;	
	
	CaveScalarField(long seed, float size, float voxelsize) {
        Random random = new Random(seed);

		this.voxelCount = (int) (size / voxelsize);
		this.noises = new Noise3D[] {
				new Noise3D(random, voxelCount, size / 1f, false),
				new Noise3D(random, voxelCount, size / 2f, false),
				new Noise3D(random, voxelCount, size / 4f, false),
//				new Noise3D(random, voxelCount, size / 8f, false),
//				new Noise3D(random, voxelCount, size / 16f),
//				new Noise3D(random, voxelCount, size / 32f),
//				new Noise3D(random, voxelCount, size / 64f),
//				new Noise3D(random, voxelCount, size / 128f),
			};
		stalactites = new Noise3D(random, voxelCount, new Vector3f(size / 16, size, size / 16), 0, size / 10f, true);
		colors = new Noise3D(random, voxelCount, new Vector3f(size / 4, size / 4, size / 4), 0.7f, 1f, true);
	}


	public float calculate(final Vector3f point) {
		float density = 0;
		for (int i = 0; i < noises.length; i++) {
			density += noises[i].getNoise(point);
		}
	
		density += stalactites.getNoise(point);
		return density;
	}
	
	/**
	 * Computing the Normal via a Gradient
	 */
	public void normal(Vector3f point, Vector3f result) {
		final float voxelsize = 4f;
		// x
		tmp.set(point.x - voxelsize, point.y, point.z);
		result.x = calculate(tmp);
		tmp.set(point.x + voxelsize, point.y, point.z);
		result.x -= calculate(tmp);
		// y
		tmp.set(point.x, point.y - voxelsize, point.z);
		result.y = calculate(tmp);
		tmp.set(point.x, point.y + voxelsize, point.z);
		result.y -= calculate(tmp);
		// z
		tmp.set(point.x, point.y, point.z - voxelsize);
		result.z = calculate(tmp);
		tmp.set(point.x, point.y, point.z + voxelsize);
		result.z -= calculate(tmp);
		
		result.normalizeLocal();
	}
	
	public void textureCoords(Vector3f point, Vector2f result) {}
	
	public void color(Vector3f point, ColorRGBA result) {
		float c1 = colors.getNoise(point);
		float c2 = stalactites.getNoise(point) / stalactites.getAmplitude();
		result.set(c2, c2, c2, 1f);

	}

}
