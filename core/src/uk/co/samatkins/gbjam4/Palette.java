package uk.co.samatkins.gbjam4;

public enum Palette {
	White(215, 232, 148),
	Light(174, 196, 64),
	Dark(82, 127, 57),
	Black(32, 70, 49),
	Transparent(0,0,0);

	private int ir, ig, ib;
	public final float r, g, b;

	Palette(int r, int g, int b) {

		this.ir = r;
		this.ig = g;
		this.ib = b;

		this.r = r / 255f;
		this.g = g / 255f;
		this.b = b / 255f;
	}

	public static Palette find(int pixelValue) {
		// FIXME: Currently we ignore thr red channel because it was coming out as negative and complicating things!
		// For now, that's fine as we only have 4 colours and can easily differentiate them, but it's something to bear in mind.

		int r = (pixelValue & 0xff000000) / 0x01000000;
		int g = (pixelValue & 0x00ff0000) / 0x00010000;
		int b = (pixelValue & 0x0000ff00) / 0x00000100;
		int a = (pixelValue & 0x000000ff);

		if (a == 0) return Transparent;

		for (Palette p: Palette.values()) {
			if (/*p.ir == r && */ p.ig == g && p.ib == b) {
				return p;
			}
		}

		throw new RuntimeException("Failed to locate colour!");
	}
}
