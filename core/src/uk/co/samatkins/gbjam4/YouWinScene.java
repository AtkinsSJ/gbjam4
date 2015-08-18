package uk.co.samatkins.gbjam4;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class YouWinScene extends InputAdapter
						implements GBJam4.Scene {

	private final GBJam4 game;
	private final TextureAtlas.AtlasRegion youWinTexture;

	public YouWinScene(GBJam4 game) {

		this.game = game;
		youWinTexture = game.textureAtlas.findRegion("youwin");
	}

	@Override
	public void render(float delta, SpriteBatch batch, ShapeRenderer shapeRenderer) {

		if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
			game.showMenu();
		}

		batch.begin();

		batch.draw(youWinTexture, 0, 0);

		batch.end();
	}

	@Override
	public void dispose() {

	}
}
