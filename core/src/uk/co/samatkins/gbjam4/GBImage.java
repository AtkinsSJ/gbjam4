package uk.co.samatkins.gbjam4;

import com.badlogic.gdx.graphics.Pixmap;

public class GBImage {

	public final int width, height;
	public final Palette data[][];

	public GBImage(Pixmap source) {
		width = source.getWidth();
		height = source.getHeight();
		data = new Palette[width][height];

		for (int x=0; x<width; x++) {
			for (int y=0; y<height; y++) {
				data[x][y] = Palette.find(source.getPixel(x,y));
			}
		}
	}
}
