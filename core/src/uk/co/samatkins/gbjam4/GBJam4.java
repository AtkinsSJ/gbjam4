package uk.co.samatkins.gbjam4;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GBJam4 extends ApplicationAdapter {

	public static class Palette {
		public static final Color White = new Color(215f/255f, 232f/255f, 148f/255f, 1),
								Light = new Color(174f/255f, 196f/255f,  64f/255f, 1),
								Dark  = new Color( 82f/255f, 127f/255f,  57f/255f, 1),
								Black = new Color( 32f/255f,  70f/255f,  49f/255f, 1);
	}

	public static final int SCREEN_WIDTH = 160,
							SCREEN_HEIGHT = 144;

	public Viewport viewport;
	private ShapeRenderer shapeRenderer;
	
	@Override
	public void create () {
		Gdx.app.setLogLevel(Application.LOG_DEBUG);

		viewport = new FitViewport(SCREEN_WIDTH, SCREEN_HEIGHT);
		shapeRenderer = new ShapeRenderer();
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
		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

		shapeRenderer.setColor(Palette.Black);
		shapeRenderer.rect(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT);

		shapeRenderer.setColor(Palette.White);
		shapeRenderer.rect(4, 4, SCREEN_WIDTH-8, SCREEN_HEIGHT-8);

		shapeRenderer.end();
	}
}
