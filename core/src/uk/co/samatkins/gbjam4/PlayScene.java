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

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class PlayScene extends InputAdapter
	implements GBJam4.Scene {

	private final GBJam4 game;

	private int levelWidth, levelHeight;
	private int levelGeometry[][];

	private final Vector2 playerPosition = new Vector2();
	private float playerFacingDirection;
	private final Array<GBImage> collectedFruit;

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
	private boolean visibleTiles[][];

	private final GBImage fruit[];

	private final GBImage wall;
	private final GBImage compass;
	private static final float compassLineLength = 12f;

	private static class Coord {
		int x, y;

		public Coord(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	public enum Direction {
		N(0,1),
		E(1,0),
		S(0,-1),
		W(-1,0);

		public final int x, y;

		Direction(int x, int y) {
			this.x = x;
			this.y = y;
		}
	}

	private void setColor(ShapeRenderer shapeRenderer, Palette color) {
		shapeRenderer.setColor(color.r, color.g, color.b, 1);
	}

	private void putPixel(ShapeRenderer shapeRenderer, int x, int y, Palette color) {
		if (color != Palette.Transparent) {
			shapeRenderer.setColor(color.r, color.g, color.b, 1);
			shapeRenderer.rect(x, y, 1, 1);
		}
	}

	private GBImage gbImage(String fileName) {
		Pixmap pixmap = new Pixmap(Gdx.files.internal(fileName));
		GBImage image = new GBImage(pixmap);
		pixmap.dispose();

		return image;
	}

	public PlayScene(GBJam4 game) {
		this.game = game;

		fruit = new GBImage[] {
			gbImage("apple.png"),
			gbImage("banana.png"),
			gbImage("grapes.png"),
			gbImage("orange.png"),
			gbImage("strawberry.png"),
		};

		wall = gbImage("wall.png");
		compass = gbImage("compass.png");

		entities = new Array<Entity>(128);
		collectedFruit = new Array<GBImage>(fruit.length);

//		loadLevelFromImageFile("level.png");
		generateLevel(7, 7);
	}

	private void loadLevelFromImageFile(String filename) {
		Pixmap pixmap = new Pixmap(Gdx.files.internal(filename));

		levelWidth = pixmap.getWidth();
		levelHeight = pixmap.getHeight();
		levelGeometry = new int[levelWidth][levelHeight];
		visibleTiles = new boolean[levelWidth][levelHeight];

		int fruitIndex = 0;

		for (int y=0; y<pixmap.getWidth(); y++) {
			for (int x=0; x<pixmap.getHeight(); x++) {
				int pixel = pixmap.getPixel(x,y);
				switch (pixel) {
					case 0x000000ff: levelGeometry[x][y] = 1;  break;
					case 0xffff00ff: playerPosition.set(x + 0.5f, y + 0.5f); break;
					case 0xff0000ff: entities.add(new Entity(x+0.5f, y+0.5f, fruit[fruitIndex++ % fruit.length])); break;
				}
			}
		}

		pixmap.dispose();
	}

	private void generateLevel(int width, int height) {
		levelWidth = 2 * width + 1;
		levelHeight = 2 * height + 1;
		levelGeometry = new int[levelWidth][levelHeight];
		visibleTiles = new boolean[levelWidth][levelHeight];

		// Fill with walls
		for (int y=0; y<levelHeight; y++) {
			for (int x=0; x<levelWidth; x++) {
				levelGeometry[x][y] = 1;
			}
		}

		// Perfect maze generation
		Random random = new Random(System.currentTimeMillis());
		Array<Coord> tiles = new Array<Coord>(false, width*height);
		Array<Direction> validDirections = new Array<Direction>(false, 4);

		Coord start = new Coord(random.nextInt(width), random.nextInt(height));
		tiles.add(start);
		levelGeometry[(start.x * 2 + 1)][(start.y * 2 + 1)] = 0;

		while (tiles.size > 0) {
			// Get a random edge tile, try and connect it.
			int tileIndex = random.nextInt(tiles.size);
			Coord t = tiles.get(tileIndex);

			for (Direction direction : Direction.values()) {
				int x = (t.x + direction.x) * 2 + 1,
					y = (t.y + direction.y) * 2 + 1;
				if (x >= 0 && x < levelWidth && y >= 0 && y < levelHeight
					&& levelGeometry[x][y] > 0) {
					validDirections.add(direction);
				}
			}

			// If all 4 sides are taken, remove this tile!
			if (validDirections.size == 0) {
				tiles.removeIndex(tileIndex);
			} else {
				// Otherwise, pick a random direction
				Direction direction = validDirections.get(random.nextInt(validDirections.size));
				Coord newTile = new Coord(t.x + direction.x, t.y + direction.y);
				tiles.add(newTile);

				// Dig the path
				levelGeometry[(t.x * 2 + 1 + direction.x)][(t.y * 2 + 1 + direction.y)] = 0;
				levelGeometry[(newTile.x * 2 + 1)][(newTile.y * 2 + 1)] = 0;
			}
			validDirections.clear();
		}

		// Level should be a perfect maze now!
		// Place player
		playerPosition.set(start.x * 2 + 1.5f, start.y * 2 + 1.5f);
		// Place fruit
		Set<Coord> takenPositions = new HashSet<Coord>();
		takenPositions.add(start);
		for (int fruitIndex=0; fruitIndex<fruit.length; fruitIndex++) {
			Coord pos = new Coord(random.nextInt(width), random.nextInt(height));
			while (takenPositions.contains(pos)) {
				pos.x = random.nextInt(width);
				pos.y = random.nextInt(height);
			}
			entities.add(new Entity(pos.x * 2 + 1.5f, pos.y * 2 + 1.5f, fruit[fruitIndex]));
			takenPositions.add(pos);
		}
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

			for (int entityIndex=0; entityIndex < entities.size; entityIndex++) {
				Entity entity = entities.get(entityIndex);
				float dist2 = Math.abs(entity.x - playerPosition.x) + Math.abs(entity.y - playerPosition.y);
				if (dist2 < 0.7f) {
					entities.removeIndex(entityIndex);
					entityIndex++;
					collectedFruit.add(entity.image);
				}
			}

			if (entities.size == 0) {
				Gdx.app.debug("Game", "YOU WIN!");
			}
		}

		if (debugRenderingEnabled) {
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
				depth[rayIndex] = perpWallDist;

				int lineHeight = Math.abs((int) (screenHeight / perpWallDist));
				lineHeight = MathUtils.clamp(lineHeight, 2, (int) screenHeight);
				int bottom = (GBJam4.SCREEN_HEIGHT - lineHeight) / 2;

				// Draw wall
				if (false) {
					float wallX;
					if (sideHitIsY) {
						wallX = rayPos.x + ((mapX - rayPos.y + (1 - stepY) / 2f) / rayDir.y) * rayDir.x;
					} else {
						wallX = rayPos.y + ((mapX - rayPos.x + (1 - stepX) / 2f) / rayDir.x) * rayDir.y;
					}
					wallX -= (int)wallX;
					if (wallX < 0) wallX++;

					int texX = (int)(wallX * wall.width);

					// Flip
					if ((sideHitIsY && rayDir.y < 0)
					|| (!sideHitIsY && rayDir.x < 0)) {
						texX = wall.width - texX - 1;
					}

					// Draw it!
					for (int y=0; y<lineHeight; y++) {
						int texY = wall.height - 1 - (y * wall.height / lineHeight);
//						Gdx.app.debug("Raycast wall", "texX,Y = " + texX + ", " + texY);
						putPixel(shapeRenderer, rayIndex, bottom + y, wall.data[texX][texY]);
					}

				} else {
					if (sideHitIsY) {
						setColor(shapeRenderer, Palette.Dark);
					} else {
						setColor(shapeRenderer, Palette.Black);
					}
					shapeRenderer.rect(rayIndex, bottom, 1, lineHeight);
				}
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
							int texY = y * entity.image.height / size;
							putPixel(shapeRenderer, stripe, bottom+y, entity.image.data[texX][texY]);
						}
					}
				}
			}
		}

		// Collected fruit!
		{
			int left = 2;
			int bottom = 2;

			for (int fruitIndex = 0; fruitIndex < collectedFruit.size; fruitIndex++) {
				GBImage f = collectedFruit.get(fruitIndex);

				for (int x=0; x<f.width; x++) {
					for (int y=0; y<f.height; y++) {
						putPixel(shapeRenderer, left+x, bottom+y, f.data[x][y]);
					}
				}

				left += 16 + 2;
			}
		}

		// Compass!
		{
			int left = GBJam4.SCREEN_WIDTH - compass.width - 2;
			int bottom = 2;
			for (int x=0; x<compass.width; x++) {
				for (int y=0; y<compass.height; y++) {
					putPixel(shapeRenderer, left+x, bottom+y, compass.data[x][y]);
				}
			}

			// Draw line to North
			float x = left + compass.width/2,
				y = bottom + compass.height/2;
			float dX = -cameraFacing.x,
				dY = cameraFacing.y;

			for (int i=0; i<compassLineLength * 2; i++) {
				x += dX;
				y += dY;
				putPixel(shapeRenderer, (int)x, (int)y, Palette.Black);
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
