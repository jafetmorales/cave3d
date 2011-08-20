package cave3d;
import java.util.Random;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;

/**
 * @author mazander
 */
final class CaveScalarField implements ScalarField {

	private final Noise3D[] noises;
	
	private int voxelCount;	
	
	CaveScalarField(long seed, double size, double voxelsize) {
        Random random = new Random(seed);

		this.voxelCount = (int) (size / voxelsize);
		this.noises = new Noise3D[] {
				new Noise3D(random, voxelCount, size / 1.0, false),
				new Noise3D(random, voxelCount, size / 2.0, false),
				new Noise3D(random, voxelCount, size / 4.0, false),
				new Noise3D(random, voxelCount, new Vector3(size / 16.0, size, size / 16.0), 0, size / 2.0, true),
//				new Noise3D(random, voxelCount, size / 8f, false),
//				new Noise3D(random, voxelCount, size / 16f),
//				new Noise3D(random, voxelCount, size / 32f),
//				new Noise3D(random, voxelCount, size / 64f),
//				new Noise3D(random, voxelCount, size / 128f),
			};
	}


	public final double calculate(final double x, final double y, final double z) {
		double density = 0;
		for (int i = 0; i < noises.length; i++) {
			density += noises[i].getNoise(x, y, z);
		}
		return density;
	}
	
	/**
	 * Computing the Normal via a Gradient
	 */
	public final void normal(final ReadOnlyVector3 p, final Vector3 result) {
		final double voxelsize = 4f;
		result.setX(calculate(p.getX() - voxelsize, p.getY(), p.getZ()) - calculate(p.getX() + voxelsize, p.getY(), p.getZ()));
		result.setY(calculate(p.getX(), p.getY() - voxelsize, p.getZ()) - calculate(p.getX(), p.getY() + voxelsize, p.getZ()));
		result.setZ(calculate(p.getX(), p.getY(), p.getZ() - voxelsize) - calculate(p.getX(), p.getY(), p.getZ() + voxelsize));
		result.normalizeLocal();
	}
	
	public final void textureCoords(final ReadOnlyVector3 point, final Vector2 result) {}
	
	public final void color(final ReadOnlyVector3 point, final ColorRGBA result) {
		result.set(1f, 1f, 1f, 1f);
	}
}
