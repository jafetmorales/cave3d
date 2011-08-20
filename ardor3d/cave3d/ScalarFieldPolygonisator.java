package cave3d;

/*
 * Copyright (c) 2003-2009 jMonkeyEngine
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'jMonkeyEngine' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */



import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import com.ardor3d.math.ColorRGBA;
import com.ardor3d.math.Vector2;
import com.ardor3d.math.Vector3;
import com.ardor3d.scenegraph.FloatBufferData;
import com.ardor3d.scenegraph.Mesh;
import com.ardor3d.scenegraph.MeshData;
import com.ardor3d.util.geom.BufferUtils;



/**
 * Based on Paul Bourke's code from "Polygonising a Scalar Field Using Tetrahedrons"
 * http://local.wasp.uwa.edu.au/~pbourke/geometry/polygonise/
 *
 * @author Daniel Gronau
 * @author Irrisor (replaced array lists by using buffers)
 * @author basixs (replaced creation of objects with global 'calc' vectors, for better performance)
 * @author mazander (modified for cave3d)
 */
final class ScalarFieldPolygonisator {
 
    private final Vector3 boxSize;
    private final double cubeSize;
	private final boolean doTexCoords;
	private final boolean doColors;
    private final double[][][] values;
    private final Vector3[] cellPoints = new Vector3[]{
        new Vector3(), new Vector3(), new Vector3(), new Vector3(),
        new Vector3(), new Vector3(), new Vector3(), new Vector3()
    };
    private final int[] cellIso = new int[ 8 ];
    private final Map<Edge, Integer> interpol = new HashMap<Edge, Integer>( 5000 );
    private final Edge tmpEdge = new Edge();
    private final int xSize;
    private final int ySize;
    private final int zSize;
    private final ScalarField field;
    private FloatBuffer vertexBuffer;
    private FloatBuffer normalBuffer;
    private IntBuffer indexBuffer;
    private FloatBuffer textureBuffer;
    private FloatBuffer colorBuffer;
    //
    // Temporary calc vars
    private final ColorRGBA tmpColor = new ColorRGBA();
    private final Vector3 tmpVector1 = new Vector3();
    private final Vector3 tmpVector2 = new Vector3();
    private final Vector2 tmpVector2f = new Vector2();
	private final Vector3 offset = new Vector3();


    public ScalarFieldPolygonisator(double boxSize, int size, final ScalarField field, boolean doTexCoords, boolean doColors) {
		this.doTexCoords = doTexCoords;
		this.doColors = doColors;
		this.boxSize = new Vector3(boxSize, boxSize, boxSize);
        this.boxSize.divideLocal(2f);
        this.cubeSize = boxSize / size;
        xSize = size;
        ySize = size;
        zSize = size;
        values = new double[ xSize + 1 ][ ySize + 1 ][ zSize + 1 ];
        this.field = field;
    }

    public void calculate( Mesh mesh, Vector3 offset, double iso ) {
    	final MeshData meshData = mesh.getMeshData();
        vertexBuffer = meshData.getVertexBuffer();
        if( vertexBuffer == null ){
            vertexBuffer = BufferUtils.createFloatBuffer( 16000 );
        } else{
            vertexBuffer.clear();
        }

        normalBuffer = meshData.getNormalBuffer();
        if( normalBuffer == null ){
            normalBuffer = BufferUtils.createFloatBuffer( 16000 );
        } else{
            normalBuffer.clear();
        }

        if(doColors) {
	        colorBuffer = meshData.getColorBuffer();
	        if( colorBuffer == null ){
	            colorBuffer = BufferUtils.createFloatBuffer( 16000 );
	        } else{
	            colorBuffer.clear();
	        }
        }

        if(doTexCoords) {
//	        final FloatBufferData texCoords = meshData.getTextureCoords( 0 );
//	        if( texCoords != null ){
//	            textureBuffer = texCoords.coords;
//	        } else{
//	            textureBuffer = null;
//	        }
//	        if( textureBuffer == null ){
//	            textureBuffer = BufferUtils.createFloatBuffer( 16000 );
//	        } else{
//	            textureBuffer.clear();
//	        }
        }

        indexBuffer = (IntBuffer) meshData.getIndexBuffer();
        if( indexBuffer == null ){
            indexBuffer = BufferUtils.createIntBuffer( 16000 );
        } else{
            indexBuffer.clear();
        }
        
        interpol.clear();
    	this.offset.set(offset);

        if(populateFieldArray( field, iso )) {
        	calculateCells( iso );
        }

        indexBuffer.flip();
        meshData.setIndexBuffer( indexBuffer );
        vertexBuffer.flip();
        meshData.setVertexBuffer( vertexBuffer );
        normalBuffer.flip();
        meshData.setNormalBuffer( normalBuffer );
        if(doColors) {
        	colorBuffer.flip();
        	meshData.setColorBuffer( colorBuffer );
        }
        
        if(doTexCoords) {
//        	textureBuffer.flip();
//        	meshData.setTextureCoords( new TexCoords( textureBuffer ) );
        }
    }

    private void gridToWorld( final int x, final int y, final int z, final Vector3 store ) {
        store.setX( x * cubeSize - boxSize.getX() + offset.getX() );
        store.setY( y * cubeSize - boxSize.getY() + offset.getY() );
        store.setZ( z * cubeSize - boxSize.getZ() + offset.getZ() );
    }

    private void calculateCells( double iso ) {
        for( int xk = 0; xk < xSize; xk++ ){
            for( int yk = 0; yk < ySize; yk++ ){
                for( int zk = 0; zk < zSize; zk++ ){
                    calculateCell( iso, xk, yk, zk );
                }
            }
        }
    }
    
    private int populateField(ScalarField field, final double iso, final int xMin, final int xMax,
			final int yMin, final int yMax, final int zMin, final int zMax) {
		int isoCount = 0;
		for (int x = xMin; x <= xMax; x++) {
			for (int y = yMin; y <= yMax; y++) {
				for (int z = zMin; z <= zMax; z++) {
					gridToWorld(x, y, z, tmpVector1);
					values[x][y][z] = field.calculate(tmpVector1.getX(), tmpVector1.getY(), tmpVector1.getZ());
					if (values[x][y][z] > iso) {
						isoCount++;
					} else {
						isoCount--;
					}
				}
			}
		}
		return isoCount;
    }
	
    private void clearField( final int xMin, final int xMax,
			final int yMin, final int yMax, final int zMin, final int zMax) {
		for (int x = xMin; x <= xMax; x++) {
			for (int y = yMin; y <= yMax; y++) {
				for (int z = zMin; z <= zMax; z++) {
					values[x][y][z] = 0;
				}
			}
		}
    }
    
    private boolean populateFieldArray( ScalarField field, final double iso ) {
    	int borderIsoCount = 0;
    	// Populate Z border face
    	borderIsoCount += populateField(field, iso, 0,     xSize,     0,     ySize, 0,     0);
    	borderIsoCount += populateField(field, iso, 0,     xSize,     0,     ySize, zSize, zSize);
    	// Populate Y border face
    	borderIsoCount += populateField(field, iso, 0,     0,         0,     ySize, 1,     zSize - 1);
    	borderIsoCount += populateField(field, iso, xSize, xSize,     0,     ySize, 1,     zSize - 1);
    	// Populate X border face
    	borderIsoCount += populateField(field, iso, 1,     xSize - 1, 0,     0,     1,     zSize - 1);
    	borderIsoCount += populateField(field, iso, 1,     xSize - 1, ySize, ySize, 1,     zSize - 1);
    	
    	// optimization populate center

    	final int maxCount = ((xSize + 1) * (ySize + 1) * (zSize + 1)) - ((xSize - 1) * (ySize - 1) * (zSize - 1));
    	boolean hasPolygons = borderIsoCount != maxCount & borderIsoCount != -maxCount;
    	if(hasPolygons) {
    		populateField(field, iso, 1, xSize - 1, 1, ySize - 1, 1, zSize - 1);
    	} else {
    		clearField(1, xSize - 1, 1, ySize - 1, 1, zSize - 1);
    	}
        return hasPolygons;
    }

    private int calculateCell( final double iso, final int xk, final int yk, final int zk ) {

        int bits = 0;

        cellIso[0] = values[xk][yk][zk] > iso ? 1 : 0;
        cellIso[1] = values[xk + 1][yk][zk] > iso ? 1 : 0;
        cellIso[2] = values[xk + 1][yk][zk + 1] > iso ? 1 : 0;
        cellIso[3] = values[xk][yk][zk + 1] > iso ? 1 : 0;
        cellIso[4] = values[xk][yk + 1][zk] > iso ? 1 : 0;
        cellIso[5] = values[xk + 1][yk + 1][zk] > iso ? 1 : 0;
        cellIso[6] = values[xk + 1][yk + 1][zk + 1] > iso ? 1 : 0;
        cellIso[7] = values[xk][yk + 1][zk + 1] > iso ? 1 : 0;

        for( int i = 0; i < cellIso.length; ++i ){
            bits |= cellIso[i] == 1 ? ( 1 << i ) : 0;
        }

        if( bits == 0 || bits == 255 ){
            return bits;
        }

        tmpVector1.setX(xk * cubeSize - boxSize.getX());
        tmpVector1.setY(yk * cubeSize - boxSize.getY());
        tmpVector1.setZ(zk * cubeSize - boxSize.getZ());
        tmpVector2.setX(tmpVector1.getX() + cubeSize);
        tmpVector2.setY(tmpVector1.getY() + cubeSize);
        tmpVector2.setZ(tmpVector1.getZ() + cubeSize);

        cellPoints[0].set( tmpVector1.getX(), tmpVector1.getY(), tmpVector1.getZ() );
        cellPoints[1].set( tmpVector2.getX(), tmpVector1.getY(), tmpVector1.getZ() );
        cellPoints[2].set( tmpVector2.getX(), tmpVector1.getY(), tmpVector2.getZ() );
        cellPoints[3].set( tmpVector1.getX(), tmpVector1.getY(), tmpVector2.getZ() );
        cellPoints[4].set( tmpVector1.getX(), tmpVector2.getY(), tmpVector1.getZ() );
        cellPoints[5].set( tmpVector2.getX(), tmpVector2.getY(), tmpVector1.getZ() );
        cellPoints[6].set( tmpVector2.getX(), tmpVector2.getY(), tmpVector2.getZ() );
        cellPoints[7].set( tmpVector1.getX(), tmpVector2.getY(), tmpVector2.getZ() );

        calculateTetra( iso, 0, 4, 7, 6, xk, yk, zk );
        calculateTetra( iso, 0, 4, 6, 5, xk, yk, zk );
        calculateTetra( iso, 0, 2, 6, 3, xk, yk, zk );
        calculateTetra( iso, 0, 1, 6, 2, xk, yk, zk );
        calculateTetra( iso, 0, 3, 6, 7, xk, yk, zk );
        calculateTetra( iso, 0, 1, 5, 6, xk, yk, zk );

        // return the cell triangle bits
        return bits;
    }

    private void calculateTetra( final double iso, final int v0, final int v1,
            final int v2, final int v3, final int xk, final int yk, final int zk ) {

        final int triindex = cellIso[v0] + cellIso[v1] * 2 + cellIso[v2] * 4 + cellIso[v3] * 8;

        /* Form the vertices of the triangles for each case */
        switch( triindex ){
            case 0x00:
            case 0x0F:
                break;
            case 0x0E:
                addIndex( interpolate( iso, v0, v3, xk, yk, zk ) );
                addIndex( interpolate( iso, v0, v2, xk, yk, zk ) );
                addIndex( interpolate( iso, v0, v1, xk, yk, zk ) );
                break;
            case 0x01:
                addIndex( interpolate( iso, v0, v2, xk, yk, zk ) );
                addIndex( interpolate( iso, v0, v3, xk, yk, zk ) );
                addIndex( interpolate( iso, v0, v1, xk, yk, zk ) );
                break;
            case 0x0D:
                addIndex( interpolate( iso, v1, v2, xk, yk, zk ) );
                addIndex( interpolate( iso, v1, v3, xk, yk, zk ) );
                addIndex( interpolate( iso, v0, v1, xk, yk, zk ) );
                break;
            case 0x02:
                addIndex( interpolate( iso, v1, v3, xk, yk, zk ) );
                addIndex( interpolate( iso, v1, v2, xk, yk, zk ) );
                addIndex( interpolate( iso, v0, v1, xk, yk, zk ) );
                break;
            case 0x0C: {
                int temp1 = interpolate( iso, v0, v2, xk, yk, zk );
                int temp2 = interpolate( iso, v1, v3, xk, yk, zk );
                addIndex( temp1 );
                addIndex( temp2 );
                addIndex( interpolate( iso, v0, v3, xk, yk, zk ) );
                addIndex( temp1 );
                addIndex( interpolate( iso, v1, v2, xk, yk, zk ) );
                addIndex( temp2 );
                break;
            }
            case 0x03: {
                int temp1 = interpolate( iso, v0, v2, xk, yk, zk );
                int temp2 = interpolate( iso, v1, v3, xk, yk, zk );
                addIndex( temp2 );
                addIndex( temp1 );
                addIndex( interpolate( iso, v0, v3, xk, yk, zk ) );
                addIndex( interpolate( iso, v1, v2, xk, yk, zk ) );
                addIndex( temp1 );
                addIndex( temp2 );
                break;
            }
            case 0x0B:
                addIndex( interpolate( iso, v2, v3, xk, yk, zk ) );
                addIndex( interpolate( iso, v1, v2, xk, yk, zk ) );
                addIndex( interpolate( iso, v0, v2, xk, yk, zk ) );
                break;
            case 0x04:
                addIndex( interpolate( iso, v1, v2, xk, yk, zk ) );
                addIndex( interpolate( iso, v2, v3, xk, yk, zk ) );
                addIndex( interpolate( iso, v0, v2, xk, yk, zk ) );
                break;
            case 0x0A: {
                int temp1 = interpolate( iso, v0, v1, xk, yk, zk );
                int temp2 = interpolate( iso, v2, v3, xk, yk, zk );
                addIndex( interpolate( iso, v0, v3, xk, yk, zk ) );
                addIndex( temp2 );
                addIndex( temp1 );
                addIndex( temp2 );
                addIndex( interpolate( iso, v1, v2, xk, yk, zk ) );
                addIndex( temp1 );
                break;
            }
            case 0x05: {
                int temp1 = interpolate( iso, v0, v1, xk, yk, zk );
                int temp2 = interpolate( iso, v2, v3, xk, yk, zk );
                addIndex( temp2 );
                addIndex( interpolate( iso, v0, v3, xk, yk, zk ) );
                addIndex( temp1 );
                addIndex( interpolate( iso, v1, v2, xk, yk, zk ) );
                addIndex( temp2 );
                addIndex( temp1 );
                break;
            }
            case 0x09: {
                int temp1 = interpolate( iso, v0, v1, xk, yk, zk );
                int temp2 = interpolate( iso, v2, v3, xk, yk, zk );
                addIndex( temp2 );
                addIndex( interpolate( iso, v1, v3, xk, yk, zk ) );
                addIndex( temp1 );
                addIndex( interpolate( iso, v0, v2, xk, yk, zk ) );
                addIndex( temp2 );
                addIndex( temp1 );
                break;
            }
            case 0x06: {
                int temp1 = interpolate( iso, v0, v1, xk, yk, zk );
                int temp2 = interpolate( iso, v2, v3, xk, yk, zk );
                addIndex( interpolate( iso, v1, v3, xk, yk, zk ) );
                addIndex( temp2 );
                addIndex( temp1 );
                addIndex( temp2 );
                addIndex( interpolate( iso, v0, v2, xk, yk, zk ) );
                addIndex( temp1 );
                break;
            }
            case 0x07:
                addIndex( interpolate( iso, v1, v3, xk, yk, zk ) );
                addIndex( interpolate( iso, v2, v3, xk, yk, zk ) );
                addIndex( interpolate( iso, v0, v3, xk, yk, zk ) );
                break;
            case 0x08:
                addIndex( interpolate( iso, v2, v3, xk, yk, zk ) );
                addIndex( interpolate( iso, v1, v3, xk, yk, zk ) );
                addIndex( interpolate( iso, v0, v3, xk, yk, zk ) );
                break;
        }
    }

	private void addIndex(final int index) {
		indexBuffer = enlargeIfNeeded(indexBuffer);
		indexBuffer.put(index);
	}

	private void addVertex(final Vector3 v) {
		vertexBuffer = enlargeIfNeeded(vertexBuffer);
		vertexBuffer.put(v.getXf()).put(v.getYf()).put(v.getZf());
	}

	private void addNormal(final Vector3 n) {
		normalBuffer = enlargeIfNeeded(normalBuffer);
		normalBuffer.put(n.getXf()).put(n.getYf()).put(n.getZf());
	}

	private void addColor(final ColorRGBA c) {
		colorBuffer = enlargeIfNeeded(colorBuffer);
		colorBuffer.put(c.getRed()).put(c.getGreen()).put(c.getBlue()).put(c.getAlpha());
	}

	private void addTextureCoord(final Vector2 t) {
		textureBuffer = enlargeIfNeeded(textureBuffer);
		textureBuffer.put(t.getXf()).put(t.getYf());
	}

    private IntBuffer enlargeIfNeeded( IntBuffer buffer ) {
        if( buffer.capacity() == buffer.position() ){
            final IntBuffer oldBuffer = buffer;
            buffer = BufferUtils.createIntBuffer( oldBuffer.capacity() * 2 );
            oldBuffer.flip();
            buffer.put( oldBuffer );
        }
        return buffer;
    }

    private FloatBuffer enlargeIfNeeded( FloatBuffer buffer ) {
        if( buffer.capacity() < buffer.position() + 4 ){
            final FloatBuffer oldBuffer = buffer;
            buffer = BufferUtils.createFloatBuffer( oldBuffer.capacity() * 2 );
            oldBuffer.flip();
            buffer.put( oldBuffer );
        }
        return buffer;
    }

    private int interpolate( double iso, int v1, int v2, int xk, int yk, int zk ) {
        if( v1 > v2 ){
            final int tmp = v2;
            v2 = v1;
            v1 = tmp;
        }
        switch( v1 ){
            default:
                tmpEdge.x1 = xk;
                tmpEdge.y1 = yk;
                tmpEdge.z1 = zk;
                break;
            case 1:
                tmpEdge.x1 = xk + 1;
                tmpEdge.y1 = yk;
                tmpEdge.z1 = zk;
                break;
            case 2:
                tmpEdge.x1 = xk + 1;
                tmpEdge.y1 = yk;
                tmpEdge.z1 = zk + 1;
                break;
            case 3:
                tmpEdge.x1 = xk;
                tmpEdge.y1 = yk;
                tmpEdge.z1 = zk + 1;
                break;
            case 4:
                tmpEdge.x1 = xk;
                tmpEdge.y1 = yk + 1;
                tmpEdge.z1 = zk;
                break;
            case 5:
                tmpEdge.x1 = xk + 1;
                tmpEdge.y1 = yk + 1;
                tmpEdge.z1 = zk;
                break;
            case 6:
                tmpEdge.x1 = xk + 1;
                tmpEdge.y1 = yk + 1;
                tmpEdge.z1 = zk + 1;
                break;
            case 7:
                tmpEdge.x1 = xk;
                tmpEdge.y1 = yk + 1;
                tmpEdge.z1 = zk + 1;
                break;
        }

        switch( v2 ){
            default:
                tmpEdge.x2 = xk;
                tmpEdge.y2 = yk;
                tmpEdge.z2 = zk;
                break;
            case 1:
                tmpEdge.x2 = xk + 1;
                tmpEdge.y2 = yk;
                tmpEdge.z2 = zk;
                break;
            case 2:
                tmpEdge.x2 = xk + 1;
                tmpEdge.y2 = yk;
                tmpEdge.z2 = zk + 1;
                break;
            case 3:
                tmpEdge.x2 = xk;
                tmpEdge.y2 = yk;
                tmpEdge.z2 = zk + 1;
                break;
            case 4:
                tmpEdge.x2 = xk;
                tmpEdge.y2 = yk + 1;
                tmpEdge.z2 = zk;
                break;
            case 5:
                tmpEdge.x2 = xk + 1;
                tmpEdge.y2 = yk + 1;
                tmpEdge.z2 = zk;
                break;
            case 6:
                tmpEdge.x2 = xk + 1;
                tmpEdge.y2 = yk + 1;
                tmpEdge.z2 = zk + 1;
                break;
            case 7:
                tmpEdge.x2 = xk;
                tmpEdge.y2 = yk + 1;
                tmpEdge.z2 = zk + 1;
                break;
        }

        final Integer index = interpol.get( tmpEdge );
        if( index != null ){
            return index;
        }
        double ratio1 = 0.5f, ratio2 = 0.5f;
        final double value1 = tmpEdge.getValue1( this );
        final double value2 = tmpEdge.getValue2( this );
        if( value1 != value2 ){
            ratio1 = ( value1 - iso ) / ( value1 - value2 );
            ratio2 = 1f - ratio1;
        }

        final int currentVertexIndex = vertexBuffer.position() / 3;
        tmpVector1.setX(cellPoints[v2].getX() * ratio1 + cellPoints[v1].getX() * ratio2 + offset.getX());
        tmpVector1.setY(cellPoints[v2].getY() * ratio1 + cellPoints[v1].getY() * ratio2 + offset.getY());
        tmpVector1.setZ(cellPoints[v2].getZ() * ratio1 + cellPoints[v1].getZ() * ratio2 + offset.getZ());
        
        addVertex( tmpVector1 );

        field.normal( tmpVector1, tmpVector2 );
        addNormal( tmpVector2 );

        if(doTexCoords) {
        	field.textureCoords( tmpVector1, tmpVector2f );
        	addTextureCoord( tmpVector2f );
        }

        if(doColors) {
        	field.color( tmpVector1, tmpColor );
        	addColor( tmpColor );
        }

        interpol.put( new Edge( tmpEdge ), currentVertexIndex );

        return currentVertexIndex;
    }



    private static final class Edge {

        int x1, y1, z1, x2, y2, z2;

        private Edge() {
        }

        public Edge( Edge e ) {
            x1 = e.x1;
            y1 = e.y1;
            z1 = e.z1;
            x2 = e.x2;
            y2 = e.y2;
            z2 = e.z2;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 59 * hash + this.x1;
            hash = 61 * hash + this.y1;
            hash = 67 * hash + this.z1;
            hash = 71 * hash + this.x2;
            hash = 73 * hash + this.y2;
            hash = 79 * hash + this.z2;
            return hash;
        }

        @Override
        public boolean equals( Object o ) {
            Edge e = (Edge) o;
            return x1 == e.x1 && y1 == e.y1 && z1 == e.z1 &&
                    x2 == e.x2 && y2 == e.y2 && z2 == e.z2;
        }

        public double getValue1( ScalarFieldPolygonisator t ) {
            return t.values[x1][y1][z1];
        }

        public double getValue2( ScalarFieldPolygonisator t ) {
            return t.values[x2][y2][z2];
        }
    }
}