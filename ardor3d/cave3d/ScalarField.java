package cave3d;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.math.type.ReadOnlyVector3;

public interface ScalarField {

	public double calculate(final double x, final double y, final double z);

	public void normal( final ReadOnlyVector3 point, final Vector3 result );

	public void textureCoords( final ReadOnlyVector3 point, final Vector2 result );

    public void color( final ReadOnlyVector3 point, final ColorRGBA result );
}