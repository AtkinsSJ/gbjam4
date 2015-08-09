package uk.co.samatkins.gbjam4;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class MenuScene extends InputAdapter
						implements GBJam4.Scene {

	private final GBJam4 game;
	private final TextureAtlas.AtlasRegion menuTexture;

	public MenuScene(GBJam4 game) {
		this.game = game;
		menuTexture = game.textureAtlas.findRegion("menu");
	}

	@Override
	public void render(float delta, SpriteBatch batch, ShapeRenderer shapeRenderer) {
		batch.begin();

		batch.draw(menuTexture, 0, 0);

		batch.end();
	}
}
