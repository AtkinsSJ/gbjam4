package uk.co.samatkins.gbjam4;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class PlayScene extends InputAdapter
	implements GBJam4.Scene {

	private final GBJam4 game;

	private final int levelGeometry[][];

	private final Vector2 playerPosition = new Vector2();
	private float playerFacingDirection;

	private static final float rayStepLength = 0.1f;
	private final Vector2 rayPosition = new Vector2();
	private final Vector2 rayStep = new Vector2();

	public PlayScene(GBJam4 game) {
		this.game = game;

		Pixmap pixmap = new Pixmap(Gdx.files.internal("level.png"));

		levelGeometry = new int[pixmap.getWidth()][pixmap.getHeight()];
		for (int y=0; y<pixmap.getWidth(); y++) {
			for (int x=0; x<pixmap.getHeight(); x++) {
				int pixel = pixmap.getPixel(x,y);
				switch (pixel) {
					case 0x000000ff: levelGeometry[x][y] = 1;  break;
					case 0xffff00ff: playerPosition.set(x, y); break;
				}
			}
		}

		pixmap.dispose();
	}

	@Override
	public void render(float delta, SpriteBatch batch, ShapeRenderer shapeRenderer) {

		// Player controls
		if (Gdx.input.isKeyPressed(Input.Keys.A)) {
			playerFacingDirection -= delta * 90;
		} else if (Gdx.input.isKeyPressed(Input.Keys.D)) {
			playerFacingDirection += delta * 90;
		}

		if (Gdx.input.isKeyPressed(Input.Keys.W)) {
			playerPosition.add(MathUtils.cosDeg(playerFacingDirection) * delta, MathUtils.sinDeg(playerFacingDirection) * delta);
		} else if (Gdx.input.isKeyPressed(Input.Keys.S)) {
			playerPosition.sub(MathUtils.cosDeg(playerFacingDirection) * delta, MathUtils.sinDeg(playerFacingDirection) * delta);
		}


		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

		// Floor and ceiling
		shapeRenderer.setColor(GBJam4.Palette.White);
		shapeRenderer.rect(0, GBJam4.SCREEN_HALF_HEIGHT, GBJam4.SCREEN_WIDTH, GBJam4.SCREEN_HALF_HEIGHT);
		shapeRenderer.setColor(GBJam4.Palette.Light);
		shapeRenderer.rect(0, 0, GBJam4.SCREEN_WIDTH, GBJam4.SCREEN_HALF_HEIGHT);

		// RAYS!
		rayStep.set(MathUtils.cosDeg(playerFacingDirection), MathUtils.sinDeg(playerFacingDirection));

		float offsetStepX = -rayStep.y * 0.01f;
		float offsetStepY = rayStep.x * 0.01f;

		rayStep.scl(rayStepLength);

		for (int rayIndex = 0; rayIndex < GBJam4.SCREEN_WIDTH; rayIndex++) {
			rayPosition.set(playerPosition);
			float offset = (rayIndex - GBJam4.SCREEN_HALF_WIDTH);
			rayPosition.add(offset * offsetStepX, offset * offsetStepY);

			float wallDistance = 0;
			while (true) {
				rayPosition.add(rayStep);
				wallDistance += rayStepLength;
				if (levelGeometry[(int) rayPosition.x][(int) rayPosition.y] > 0) {
					// We hit something!
					float wallHeight = GBJam4.SCREEN_HEIGHT / wallDistance;
					shapeRenderer.setColor(GBJam4.Palette.Black);
					shapeRenderer.rect(rayIndex, (GBJam4.SCREEN_HEIGHT - wallHeight) / 2, 1, wallHeight);
//					Gdx.app.debug("Hit a wall!", "Distance: " + wallDistance + ", wall height: " + wallHeight);
					break;
				}
			}
		}

		shapeRenderer.end();
	}
}
