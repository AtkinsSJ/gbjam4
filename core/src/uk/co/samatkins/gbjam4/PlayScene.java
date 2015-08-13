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

	private static final float FOV = 60f;
	private static final float cameraDistance = 0.5f;
	private static final float cameraHalfWidth = (float)(cameraDistance * Math.tan(Math.toRadians(FOV) / 2.0));
	private final Vector2 cameraPlane = new Vector2();
	private final Vector2 cameraFacing = new Vector2();
	private final Vector2 rayPos = new Vector2();
	private final Vector2 rayDir = new Vector2();

//	private static final float rayStepLength = 0.1f;
//	private final Vector2 rayPosition = new Vector2();
//	private final Vector2 rayStep = new Vector2();

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
		// http://lodev.org/cgtutor/raycasting.html
		cameraPlane.set(
			MathUtils.cosDeg(playerFacingDirection - 90) * cameraHalfWidth,
			MathUtils.sinDeg(playerFacingDirection - 90) * cameraHalfWidth
		);
		cameraFacing.set(
			MathUtils.cosDeg(playerFacingDirection) * cameraDistance,
			MathUtils.sinDeg(playerFacingDirection) * cameraDistance
		);
		int dirX = (cameraFacing.x > 0) ? 1 : -1;
		int dirY = (cameraFacing.y > 0) ? 1 : -1;

		for (int rayIndex=0; rayIndex < GBJam4.SCREEN_WIDTH; rayIndex++) {
			float rayCameraX = 2 * rayIndex / GBJam4.SCREEN_HALF_WIDTH - 1;
			rayPos.set(playerPosition);
			rayDir.set(
				dirX + cameraPlane.x * rayCameraX,
				dirY + cameraPlane.y * rayCameraX
			);

			int mapX = (int)rayPos.x,
				mapY = (int)rayPos.y;

			// Distance to next x or y side
			float sideDistX, sideDistY;

			// Distance from one x or y side to the next x or y side
			float deltaDistX = (float) Math.sqrt(1 + (rayDir.y * rayDir.y) / (rayDir.x * rayDir.x));
			float deltaDistY = (float) Math.sqrt(1 + (rayDir.x * rayDir.x) / (rayDir.y * rayDir.y));
			float perpWallDist;

			int stepX, stepY;
			boolean hit = false;
			boolean sideHitIsY = false; // If true, wall faces N/S, else it faces E/W

			//calculate step and initial sideDist
			if (rayDir.x < 0) {
				stepX = -1;
				sideDistX = (rayPos.x - mapX) * deltaDistX;
			} else {
				stepX = 1;
				sideDistX = (mapX + 1.0f - rayPos.x) * deltaDistX;
			}
			if (rayDir.y < 0) {
				stepY = -1;
				sideDistY = (rayPos.y - mapY) * deltaDistY;
			} else {
				stepY = 1;
				sideDistY = (mapY + 1.0f - rayPos.y) * deltaDistY;
			}

			// DDA
			while (!hit) {
				if (sideDistX < sideDistY) {
					sideDistX += deltaDistX;
					mapX += stepX;
					sideHitIsY = false;
				} else {
					sideDistY += deltaDistY;
					mapY += stepY;
					sideHitIsY = true;
				}
				// Did we hit?
				if (levelGeometry[mapX][mapY] > 0) {
					hit = true;
				}
			}

			// Calculate distance to wall
			if (!sideHitIsY) {
				perpWallDist = Math.abs((mapX - rayPos.x + (1 - stepX) / 2) / rayDir.x);
			} else {
				perpWallDist = Math.abs((mapY - rayPos.y + (1 - stepY) / 2) / rayDir.y);
			}

			int lineHeight = Math.abs((int)(GBJam4.SCREEN_HEIGHT / perpWallDist));
			lineHeight = MathUtils.clamp(lineHeight, 2, GBJam4.SCREEN_HEIGHT);

			if (sideHitIsY) {
				shapeRenderer.setColor(GBJam4.Palette.Dark);
			} else {
				shapeRenderer.setColor(GBJam4.Palette.Black);
			}

			shapeRenderer.rect(rayIndex, (GBJam4.SCREEN_HEIGHT - lineHeight) / 2, 1, lineHeight);
		}

//////////////////////////////////////
//		rayStep.set(MathUtils.cosDeg(playerFacingDirection), MathUtils.sinDeg(playerFacingDirection));
//
//		float offsetStepX = -rayStep.y * 0.01f;
//		float offsetStepY = rayStep.x * 0.01f;
//
//		rayStep.scl(rayStepLength);
//
//		for (int rayIndex = 0; rayIndex < GBJam4.SCREEN_WIDTH; rayIndex++) {
//			rayPosition.set(playerPosition);
//			float offset = (rayIndex - GBJam4.SCREEN_HALF_WIDTH);
//			rayPosition.add(offset * offsetStepX, offset * offsetStepY);
//
//			float wallDistance = 0;
//			while (true) {
//				rayPosition.add(rayStep);
//				wallDistance += rayStepLength;
//				if (levelGeometry[(int) rayPosition.x][(int) rayPosition.y] > 0) {
//					// We hit something!
//					float wallHeight = MathUtils.round(GBJam4.SCREEN_HEIGHT / wallDistance * 0.5f) * 2;
//					shapeRenderer.setColor(GBJam4.Palette.Black);
//					shapeRenderer.rect(rayIndex, (GBJam4.SCREEN_HEIGHT - wallHeight) / 2, 1, wallHeight);
////					Gdx.app.debug("Hit a wall!", "Distance: " + wallDistance + ", wall height: " + wallHeight);
//					break;
//				}
//			}
//		}

		shapeRenderer.end();
	}
}
