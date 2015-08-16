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
import com.badlogic.gdx.utils.Array;

public class PlayScene extends InputAdapter
	implements GBJam4.Scene {

	private final GBJam4 game;

	private final int levelWidth, levelHeight;
	private final int levelGeometry[][];

	private final Vector2 playerPosition = new Vector2();
	private float playerFacingDirection;

	private final Array<Entity> entities;

	private static final float FOV = 60f;
	private static final float cameraDistance = 0.5f;
	private static final float cameraHalfWidth = (float)(cameraDistance * Math.tan(Math.toRadians(FOV) / 2.0));
	private final Vector2 cameraPlane = new Vector2();
	private final Vector2 cameraFacing = new Vector2();
	private final Vector2 rayPos = new Vector2();
	private final Vector2 rayDir = new Vector2();
	private final float[] depth = new float[GBJam4.SCREEN_WIDTH];

	private boolean debugRenderingEnabled = false;
	private final boolean visibleTiles[][];
	private final GBImage strawberry;

	private void setColor(ShapeRenderer shapeRenderer, Palette color) {
		shapeRenderer.setColor(color.r, color.g, color.b, 1);
	}

	public PlayScene(GBJam4 game) {
		this.game = game;

		Pixmap pixmap = new Pixmap(Gdx.files.internal("level.png"));

		Pixmap strawberryPix = new Pixmap(Gdx.files.internal("strawberry.png"));
		strawberry = new GBImage(strawberryPix);
		strawberryPix.dispose();

		levelWidth = pixmap.getWidth();
		levelHeight = pixmap.getHeight();
		levelGeometry = new int[levelWidth][levelHeight];
		visibleTiles = new boolean[levelWidth][levelHeight];
		entities = new Array<Entity>(128);

		for (int y=0; y<pixmap.getWidth(); y++) {
			for (int x=0; x<pixmap.getHeight(); x++) {
				int pixel = pixmap.getPixel(x,y);
				switch (pixel) {
					case 0x000000ff: levelGeometry[x][y] = 1;  break;
					case 0xffff00ff: playerPosition.set(x, y); break;
					case 0xff0000ff: entities.add(new Entity(x,y, strawberry)); break;
				}
			}
		}

		pixmap.dispose();
	}

	@Override
	public void render(float delta, SpriteBatch batch, ShapeRenderer shapeRenderer) {

		final float screenWidth = GBJam4.SCREEN_WIDTH,
					screenHeight = GBJam4.SCREEN_HEIGHT,
					screenHalfWidth = GBJam4.SCREEN_HALF_WIDTH,
					screenHalfHeight = GBJam4.SCREEN_HALF_HEIGHT;

		// Player controls
		{
			if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
				playerFacingDirection += delta * 90;
			} else if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
				playerFacingDirection -= delta * 90;
			}

			playerFacingDirection = (playerFacingDirection + 360) % 360;

			cameraFacing.set(
				MathUtils.cosDeg(playerFacingDirection) * cameraDistance,
				MathUtils.sinDeg(playerFacingDirection) * cameraDistance
			);
			cameraPlane.set(
				MathUtils.cosDeg(playerFacingDirection - 90f) * cameraHalfWidth,
				MathUtils.sinDeg(playerFacingDirection - 90f) * cameraHalfWidth
			);

			if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
				if (levelGeometry[(int) (playerPosition.x + cameraFacing.x)][(int) (playerPosition.y)] == 0) {
					playerPosition.x += MathUtils.cosDeg(playerFacingDirection) * delta;
				}
				if (levelGeometry[(int) (playerPosition.x)][(int) (playerPosition.y + cameraFacing.y)] == 0) {
					playerPosition.y += MathUtils.sinDeg(playerFacingDirection) * delta;
				}

			} else if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {

				if (levelGeometry[(int) (playerPosition.x - cameraFacing.x)][(int) (playerPosition.y)] == 0) {
					playerPosition.x -= MathUtils.cosDeg(playerFacingDirection) * delta;
				}
				if (levelGeometry[(int) (playerPosition.x)][(int) (playerPosition.y - cameraFacing.y)] == 0) {
					playerPosition.y -= MathUtils.sinDeg(playerFacingDirection) * delta;
				}
			}
		}

		if (debugRenderingEnabled) {
			Gdx.app.debug("playerFacingDirection", playerFacingDirection + "");

			Gdx.gl.glEnable(GL20.GL_BLEND);
			Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

			for (int x = 0; x < levelWidth; x++) {
				for (int y = 0; y < levelHeight; y++) {
					visibleTiles[x][y] = false;
				}
			}
		}
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

		// Floor and ceiling
		setColor(shapeRenderer, Palette.White);
		shapeRenderer.rect(0, GBJam4.SCREEN_HALF_HEIGHT, GBJam4.SCREEN_WIDTH, GBJam4.SCREEN_HALF_HEIGHT);
		setColor(shapeRenderer, Palette.Light);
		shapeRenderer.rect(0, 0, GBJam4.SCREEN_WIDTH, GBJam4.SCREEN_HALF_HEIGHT);

		// RAYS!
		// http://lodev.org/cgtutor/raycasting.html
		{
			for (int rayIndex = 0; rayIndex < GBJam4.SCREEN_WIDTH; rayIndex++) {

				float rayCameraX = 2 * rayIndex / screenWidth - 1;
				rayPos.set(playerPosition);
				rayDir.set(
					cameraFacing.x + cameraPlane.x * rayCameraX,
					cameraFacing.y + cameraPlane.y * rayCameraX
				);

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
				lineHeight = MathUtils.clamp(lineHeight, 2, (int) screenHeight);

				if (sideHitIsY) {
					setColor(shapeRenderer, Palette.Dark);
				} else {
					setColor(shapeRenderer, Palette.Black);
				}

				// Draw wall
				shapeRenderer.rect(rayIndex, (GBJam4.SCREEN_HEIGHT - lineHeight) / 2, 1, lineHeight);
				depth[rayIndex] = perpWallDist;
			}
		}

		// Sprites!
		{
			// Sort by distance to player (furthest first)
			for (Entity entity : entities) {
				float dX = entity.x - playerPosition.x;
				float dY = entity.y - playerPosition.y;
				entity.distanceToPlayer = (float)Math.sqrt(dX*dX + dY*dY);
			}
			entities.sort();

			// Draw them!
			for (Entity entity : entities) {
				float relX = entity.x - playerPosition.x;
				float relY = entity.y - playerPosition.y;

				float invDet = 1.0f / (cameraPlane.x * cameraFacing.y - cameraPlane.y * cameraFacing.x);
				float transformX = invDet * (cameraFacing.y * relX - cameraFacing.x * relY);
				float transformY = invDet * (cameraPlane.x * relY - cameraPlane.y * relX);

				int spriteScreenCentreX = (int)(screenHalfWidth * (1 + transformX / transformY));

				// size
				int size = Math.abs((int)(screenHeight / transformY));
				if (size < 1) continue;
				if (size > GBJam4.SCREEN_HEIGHT) size = GBJam4.SCREEN_HEIGHT;

				int bottom = (GBJam4.SCREEN_HEIGHT - size)/2;

				int left = spriteScreenCentreX - (size/2);
				int right = left + size;

				if (left < 0) left = 0;
				if (right >= GBJam4.SCREEN_WIDTH) right = GBJam4.SCREEN_WIDTH-1;

				for (int stripe=left; stripe<right; stripe++) {

					int texX = (stripe - (-size/2 + spriteScreenCentreX)) * entity.image.width / size;
					if (transformY > 0
						&& transformY < depth[stripe]
						&& stripe > 0
						&& stripe < GBJam4.SCREEN_WIDTH)
					{
						for (int y=0; y<size; y++) {
							int texY = entity.image.height - 1 - (y * entity.image.height / size);

							Palette pixel = entity.image.data[texX][texY];
							if (pixel != Palette.Transparent) {
								setColor(shapeRenderer, pixel);
								shapeRenderer.rect(stripe, bottom+y, 1, 1);
							}
						}
					}
				}
			}
		}

		// Debug rendering!
		if (debugRenderingEnabled) {
			float scale = Math.min(screenWidth, screenHeight) / Math.max(levelWidth, levelHeight);
			for (int x = 0; x < levelWidth; x++) {
				for (int y = 0; y < levelHeight; y++) {

					/*if (visibleTiles[x][y]) {
						shapeRenderer.setColor(1, 1, 1, 0.5f);
						shapeRenderer.rect(x*scale, y*scale, 1*scale, 1*scale);
					} else*/ if (levelGeometry[x][y] > 0) {
						shapeRenderer.setColor(0, 0, 0, 0.5f);
						shapeRenderer.rect(x*scale, y*scale, 1*scale, 1*scale);
					}
				}
			}

			// Player
			shapeRenderer.setColor(1, 0, 0, 0.5f);
			float px = playerPosition.x * scale;
			float py = playerPosition.y * scale;
			float cx = px + (cameraFacing.x * scale);
			float cy = py + (cameraFacing.y * scale);

			shapeRenderer.circle(px, py, 0.3f * scale);

			// Facing
			shapeRenderer.rectLine(px, py, cx, cy, 1);

			// Camera
			shapeRenderer.setColor(0, 0, 1, 0.5f);
			shapeRenderer.rectLine(
				cx - (cameraPlane.x * scale),
				cy - (cameraPlane.y * scale),
				cx + (cameraPlane.x * scale),
				cy + (cameraPlane.y * scale),
				1
			);

			// Entities
			shapeRenderer.setColor(1,1,0,0.5f);
			for (Entity entity : entities) {
				shapeRenderer.circle(entity.x * scale, entity.y * scale, 0.3f * scale);
			}
		}

		shapeRenderer.end();
		if (debugRenderingEnabled) {
			Gdx.gl.glDisable(GL20.GL_BLEND);
		}
	}

	@Override
	public void dispose() {
	}
}
