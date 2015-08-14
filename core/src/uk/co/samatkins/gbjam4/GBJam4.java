package uk.co.samatkins.gbjam4;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GBJam4 extends ApplicationAdapter {

	public interface Scene extends InputProcessor{
		void render(float delta, SpriteBatch batch, ShapeRenderer shapeRenderer);
		void dispose();
	}
	private Scene scene;

	public static final int SCREEN_WIDTH = 160,
							SCREEN_HEIGHT = 144,
							SCREEN_HALF_WIDTH = SCREEN_WIDTH / 2,
							SCREEN_HALF_HEIGHT = SCREEN_HEIGHT / 2;

	public TextureAtlas textureAtlas;
	public Viewport viewport;
	private ShapeRenderer shapeRenderer;
	private SpriteBatch spriteBatch;

	@Override
	public void create () {
		Gdx.app.setLogLevel(Application.LOG_DEBUG);

		viewport = new FitViewport(SCREEN_WIDTH, SCREEN_HEIGHT);
		shapeRenderer = new ShapeRenderer();
		spriteBatch = new SpriteBatch();

		textureAtlas = new TextureAtlas("packed.atlas");

		setScene(new MenuScene(this));
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		viewport.update(width, height, true);
	}

	@Override
	public void render () {
		float delta = Gdx.graphics.getDeltaTime();

		Gdx.gl.glClearColor(0, 0, 0, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
		spriteBatch.setProjectionMatrix(viewport.getCamera().combined);
//
//		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
//
//		shapeRenderer.setColor(Palette.Black);
//		shapeRenderer.rect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);
//
//		shapeRenderer.end();

		scene.render(delta, spriteBatch, shapeRenderer);
	}

	private void setScene(Scene scene) {
		if (this.scene != null) {
			this.scene.dispose();
		}
		this.scene = scene;
//		this.scene.onCreate();
	}

	public void startGame() {
		setScene(new PlayScene(this));
	}
}
