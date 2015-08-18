package uk.co.samatkins.gbjam4.client;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.backends.gwt.GwtApplication;
import com.badlogic.gdx.backends.gwt.GwtApplicationConfiguration;
import uk.co.samatkins.gbjam4.GBJam4;

public class HtmlLauncher extends GwtApplication {

        @Override
        public GwtApplicationConfiguration getConfig () {
                return new GwtApplicationConfiguration(640, 576);
        }

        @Override
        public ApplicationListener getApplicationListener () {
                return new GBJam4();
        }
}