package cave3d;
import java.util.Random;

import com.jme.math.Vector2f;
import com.jme.math.Vector3f;
import com.jme.renderer.ColorRGBA;

/**
 * @author mazander
 */
final class CaveScalarField implements ScalarField {

	private final Noise3D[] noises;
	
	private int voxelCount;	
	
	CaveScalarField(long seed, float size, float voxelsize) {
        Random random = new Random(seed);

		this.voxelCount = (int) (size / voxelsize);
		this.noises = new Noise3D[] {
				new Noise3D(random, voxelCount, size / 1f, false),
				new Noise3D(random, voxelCount, size / 2f, false),
				new Noise3D(random, voxelCount, size / 4f, false),
				new Noise3D(random, voxelCount, new Vector3f(size / 16, size, size / 16), 0, size / 2f, true),
//				new Noise3D(random, voxelCount, size / 8f, false),
//				new Noise3D(random, voxelCount, size / 16f),
//				new Noise3D(random, voxelCount, size / 32f),
//				new Noise3D(random, voxelCount, size / 64f),
//				new Noise3D(random, voxelCount, size / 128f),
			};
	}


	public final float calculate(final float x, final float y, final float z) {
		float density = 0;
		for (int i = 0; i < noises.length; i++) {
			density += noises[i].getNoise(x, y, z);
		}
		return density;
	}
	
	/**
	 * Computing the Normal via a Gradient
	 */
	public final void normal(Vector3f point, Vector3f result) {
		final float voxelsize = 4f;
		// x
		result.x = calculate(point.x - voxelsize, point.y, point.z) - calculate(point.x + voxelsize, point.y, point.z);
		// y
		result.y = calculate(point.x, point.y - voxelsize, point.z) - calculate(point.x, point.y + voxelsize, point.z);
		// z
		result.z = calculate(point.x, point.y, point.z - voxelsize) - calculate(point.x, point.y, point.z + voxelsize);
		
		float len = (float) Math.sqrt(result.x * result.x + result.y * result.y + result.z * result.z);
		if(len > 0) {
			len = 1f / len;
			result.x *= len;
			result.y *= len;
			result.z *= len;
		}
	}
	
	public final void textureCoords(Vector3f point, Vector2f result) {}
	
	public final void color(Vector3f point, ColorRGBA result) {
		result.set(1f, 1f, 1f, 1f);
	}
}
