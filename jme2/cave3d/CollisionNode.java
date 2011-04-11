package cave3d;

import com.jme.math.Vector3f;
import com.jme.scene.Node;

class CollisionNode extends Node {

	private static final long serialVersionUID = 1L;
	
	private final Vector3f velocity;
	
	CollisionNode(Vector3f velocity) {
		this.velocity = velocity;
	}
	
	void setVelocity(Vector3f velocity) {
		this.velocity.set(velocity);
	}
	
	Vector3f getVelocity() {
		return velocity;
	}
	
	// temporary variables
	private final Vector3f p2 = new Vector3f();
	private final Vector3f hit = new Vector3f();
	private final Vector3f normal = new Vector3f();
	
	void update(float time, CaveScalarField scalarField) {
		// apply gravitation
		velocity.y -= 5f * time;
		
		Vector3f p1 = getLocalTranslation();
		p2.scaleAdd(time, velocity, p1);
		// check collision
		float density2 = scalarField.calculate(p2.x, p2.y, p2.z);
		if(density2 > 0f) {
			float density1 = scalarField.calculate(p1.x, p1.y, p1.z);
			if(density1 != density2) {
				float alpha = density1 / (density1 - density2);
				hit.interpolate(p1, p2, alpha);
				scalarField.normal(hit, normal);
				velocity.scaleAdd(-2f * normal.dot(velocity), normal, velocity);
				p2.scaleAdd((1f - alpha) * time, velocity, hit);
			}
		} 
		setLocalTranslation(p2.x, p2.y, p2.z);
	}
}
