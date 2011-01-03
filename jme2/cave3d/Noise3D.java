package cave3d;
import java.util.Random;

import com.jme.math.Vector3f;

/**
 * @author mazander
 */
final class Noise3D {

	private final float[][][] noise;
	
	private final Vector3f waveLength;
	
	private final float amplitude;
	
	private final int size;

	private final float min;

	private final float max;
	
	Noise3D(Random random, final int size, final Vector3f waveLength, final float min, final float max, final boolean gaussian) {
		this.size = size;
		this.waveLength = waveLength;
		this.min = min;
		this.max = max;
		this.amplitude = max - min;
		this.noise = new float[size][size][size];
		for (int i = 0; i < size; i++) {
			for (int j = 0; j < size; j++) {
				for (int k = 0; k < size; k++) {
					if(gaussian) {
						float r = 0.15f * (float) random.nextGaussian();
						if(r < 0f) r = -r;
						if(r > 1f) r = 1f;
						noise[i][j][k] = min + amplitude * r;
					} else {
						noise[i][j][k] = min + amplitude * ((float) random.nextFloat());
					}
				}
			}
		}
	}
	
	public float getAmplitude() {
		return amplitude;
	}
	
	public float getMin() {
		return min;
	}
	
	public float getMax() {
		return max;
	}
	
	Noise3D(Random random, final int size, final float waveLength, final float amplitude, final boolean gaussian) {
		this(random, size, new Vector3f(waveLength, waveLength, waveLength), -0.5f * amplitude, 0.5f * amplitude, gaussian);
	}

	Noise3D(Random random, final int size, final float strength, final boolean gaussian) {
		this(random, size, strength, strength, gaussian);
	}
	
	
	public final float getNoise(final float x, final float y, final float z) {
		// X
		float ax0 = x / waveLength.x;
        int x0 = (int) ax0;
        if(ax0 < 0f) x0--;
    	ax0 -= x0;
    	x0 %= size; 
    	if(x0 < 0) x0 += size;
        int x1 = (x0 + 1) % size;
        float ax1 = 1f - ax0;
    	
        // Y
		float ay0 = y / waveLength.y;
        int y0 = (int) ay0;
        if(ay0 < 0f) y0--;
    	ay0 -= y0;
    	y0 %= size; 
    	if(y0 < 0) y0 += size;
        int y1 = (y0 + 1) % size;
        float ay1 = 1f - ay0;
    	
        // Z
		float az0 = z / waveLength.z;
        int z0 = (int) az0;
        if(az0 < 0f) z0--;
    	az0 -= z0;
    	z0 %= size;
    	if(z0 < 0) z0 += size;
        int z1 = (z0 + 1) % size;
        float az1 = 1f - az0;

       
        return noise[x0][y0][z0] * ax1 * ay1 * az1 +
        	   noise[x0][y0][z1] * ax1 * ay1 * az0 +
               noise[x0][y1][z0] * ax1 * ay0 * az1 +
               noise[x0][y1][z1] * ax1 * ay0 * az0 +
               noise[x1][y0][z0] * ax0 * ay1 * az1 +
               noise[x1][y0][z1] * ax0 * ay1 * az0 +
               noise[x1][y1][z0] * ax0 * ay0 * az1 +
               noise[x1][y1][z1] * ax0 * ay0 * az0 ;
	}
}