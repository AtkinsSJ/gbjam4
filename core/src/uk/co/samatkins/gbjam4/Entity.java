package uk.co.samatkins.gbjam4;

public class Entity implements Comparable<Entity> {

	public final float x, y;
	public final GBImage image;

	public float distanceToPlayer;

	public Entity(float x, float y, GBImage image) {
		this.x = x;
		this.y = y;
		this.image = image;
	}

	@Override
	public int compareTo(Entity other) {
		return (int)((other.distanceToPlayer - this.distanceToPlayer) * 100);
	}
}
