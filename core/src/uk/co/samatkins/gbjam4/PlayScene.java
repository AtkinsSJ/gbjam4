package uk.co.samatkins.gbjam4;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

public class PlayScene extends InputAdapter
	implements GBJam4.Scene {

	private final GBJam4 game;

	private final int levelWidth, levelHeight;
	private final int levelGeometry[][];
	private final boolean visibleTiles[][];

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

		levelWidth = pixmap.getWidth();
		levelHeight = pixmap.getHeight();
		levelGeometry = new int[levelWidth][levelHeight];
		visibleTiles = new boolean[levelWidth][levelHeight];
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

		for (int x = 0; x < levelWidth; x++) {
			for (int y = 0; y < levelHeight; y++) {
				visibleTiles[x][y] = false;
			}
		}

		final float screenWidth = GBJam4.SCREEN_WIDTH,
					screenHeight = GBJam4.SCREEN_HEIGHT,
					screenHalfWidth = GBJam4.SCREEN_HALF_WIDTH,
					screenHalfHeight = GBJam4.SCREEN_HALF_HEIGHT;

		// Player controls
		if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
			playerFacingDirection += delta * 90;
		} else if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
			playerFacingDirection -= delta * 90;
		}

		playerFacingDirection = (playerFacingDirection + 360) % 360;
		Gdx.app.debug("playerFacingDirection", playerFacingDirection + "");

		cameraFacing.set(
			MathUtils.cosDeg(playerFacingDirection) * cameraHalfWidth,
			MathUtils.sinDeg(playerFacingDirection) * cameraHalfWidth
		);
		cameraPlane.set(
			MathUtils.cosDeg(playerFacingDirection - 90f) * cameraHalfWidth,
			MathUtils.sinDeg(playerFacingDirection - 90f) * cameraHalfWidth
		);
		int dirX = (cameraFacing.x > 0) ? 1 : -1;
		int dirY = (cameraFacing.y > 0) ? 1 : -1;

		if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
			if (levelGeometry[(int)(playerPosition.x + dirX)][(int)(playerPosition.y)] == 0) {
				playerPosition.x += MathUtils.cosDeg(playerFacingDirection) * delta;
			}
			if (levelGeometry[(int)(playerPosition.x)][(int)(playerPosition.y + dirY)] == 0) {
				playerPosition.y += MathUtils.sinDeg(playerFacingDirection) * delta;
			}

		} else if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {

			if (levelGeometry[(int)(playerPosition.x - dirX)][(int)(playerPosition.y)] == 0) {
				playerPosition.x -= MathUtils.cosDeg(playerFacingDirection) * delta;
			}
			if (levelGeometry[(int)(playerPosition.x)][(int)(playerPosition.y - dirY)] == 0) {
				playerPosition.y -= MathUtils.sinDeg(playerFacingDirection) * delta;
			}
		}

		Gdx.gl.glEnable(GL20.GL_BLEND);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

		// Floor and ceiling
		shapeRenderer.setColor(GBJam4.Palette.White);
		shapeRenderer.rect(0, GBJam4.SCREEN_HALF_HEIGHT, GBJam4.SCREEN_WIDTH, GBJam4.SCREEN_HALF_HEIGHT);
		shapeRenderer.setColor(GBJam4.Palette.Light);
		shapeRenderer.rect(0, 0, GBJam4.SCREEN_WIDTH, GBJam4.SCREEN_HALF_HEIGHT);

		// RAYS!
		// http://lodev.org/cgtutor/raycasting.html
		{

			for (int rayIndex = 0; rayIndex < GBJam4.SCREEN_WIDTH; rayIndex++) {
				float rayCameraX = 2 * rayIndex / screenWidth - 1;
				rayPos.set(playerPosition);
				rayDir.set(
					dirX + cameraPlane.x * rayCameraX,
					dirY + cameraPlane.y * rayCameraX
				);
//				Gdx.app.debug("Raycast", "Ray #" + rayIndex + ", angle " + rayDir.angle());

				int mapX = (int) rayPos.x,
					mapY = (int) rayPos.y;

				// Distance to next x or y side
				float sideDistX, sideDistY;

				// Distance from one x or y side to the next x or y side
				float deltaDistX = (float) Math.sqrt(1f + (rayDir.y * rayDir.y) / (rayDir.x * rayDir.x));
				float deltaDistY = (float) Math.sqrt(1f + (rayDir.x * rayDir.x) / (rayDir.y * rayDir.y));
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
					visibleTiles[mapX][mapY] = true;
					// Did we hit?
					if (levelGeometry[mapX][mapY] > 0) {
						hit = true;
					}
				}

				// Calculate distance to wall
				if (!sideHitIsY) {
					perpWallDist = Math.abs((mapX - rayPos.x + (1 - stepX) / 2f) / rayDir.x);
				} else {
					perpWallDist = Math.abs((mapY - rayPos.y + (1 - stepY) / 2f) / rayDir.y);
				}

				int lineHeight = Math.abs((int) (screenHeight / perpWallDist));
				lineHeight = MathUtils.clamp(lineHeight, 2, GBJam4.SCREEN_HEIGHT);

				if (sideHitIsY) {
					shapeRenderer.setColor(GBJam4.Palette.Dark);
				} else {
					shapeRenderer.setColor(GBJam4.Palette.Black);
				}

//				shapeRenderer.setColor(sideHitIsY ? 1 : 0, (float)rayIndex /  screenWidth, 0, 1);

				shapeRenderer.rect(rayIndex, (GBJam4.SCREEN_HEIGHT - lineHeight) / 2, 1, lineHeight);
			}
		}

		Gdx.app.debug("Raycast", "Done!");
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

		// Debug rendering!
		{
			float scale = Math.min(screenWidth, screenHeight) / Math.max(levelWidth, levelHeight);
			for (int x = 0; x < levelWidth; x++) {
				for (int y = 0; y < levelHeight; y++) {

					if (visibleTiles[x][y]) {
						shapeRenderer.setColor(1, 1, 1, 0.5f);
						shapeRenderer.rect(x*scale, y*scale, 1*scale, 1*scale);
					} else if (levelGeometry[x][y] > 0) {
						shapeRenderer.setColor(0, 0, 0, 0.5f);
						shapeRenderer.rect(x*scale, y*scale, 1*scale, 1*scale);
					}
				}
			}

			shapeRenderer.setColor(1, 0, 0, 0.5f);
			float px = playerPosition.x * scale;
			float py = playerPosition.y * scale;
			float cx = px + (cameraFacing.x * 4 * scale);
			float cy = py + (cameraFacing.y * 4 * scale);

			shapeRenderer.circle(px, py, 1f * scale);

			// Facing
			shapeRenderer.rectLine(px, py, cx, cy, 1);

			// Camera
			shapeRenderer.setColor(0, 0, 1, 0.5f);
			shapeRenderer.rectLine(
				cx - (cameraPlane.x * 4 * scale),
				cy - (cameraPlane.y * 4 * scale),
				cx + (cameraPlane.x * 4 * scale),
				cy + (cameraPlane.y * 4 * scale),
				1
			);
		}

		shapeRenderer.end();
		Gdx.gl.glDisable(GL20.GL_BLEND);

	}
}
