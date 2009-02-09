package de.jreality.tutorial.intro;

import static de.jreality.shader.CommonAttributes.DIFFUSE_COLOR;
import static de.jreality.shader.CommonAttributes.LINE_SHADER;
import static de.jreality.shader.CommonAttributes.OPAQUE_TUBES_AND_SPHERES;
import static de.jreality.shader.CommonAttributes.POINT_RADIUS;
import static de.jreality.shader.CommonAttributes.POINT_SHADER;
import static de.jreality.shader.CommonAttributes.POLYGON_SHADER;
import static de.jreality.shader.CommonAttributes.SMOOTH_SHADING;
import static de.jreality.shader.CommonAttributes.TEXTURE_2D;
import static de.jreality.shader.CommonAttributes.TRANSPARENCY;
import static de.jreality.shader.CommonAttributes.TRANSPARENCY_ENABLED;
import static de.jreality.shader.CommonAttributes.TUBE_RADIUS;

import java.awt.Color;
import java.io.IOException;
import java.net.URL;

import de.jreality.geometry.Primitives;
import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.Rn;
import de.jreality.reader.Readers;
import de.jreality.scene.Appearance;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphNode;
import de.jreality.scene.data.AttributeEntityUtility;
import de.jreality.shader.DefaultGeometryShader;
import de.jreality.shader.DefaultLineShader;
import de.jreality.shader.DefaultPointShader;
import de.jreality.shader.DefaultPolygonShader;
import de.jreality.shader.ImageData;
import de.jreality.shader.RenderingHintsShader;
import de.jreality.shader.ShaderUtility;
import de.jreality.shader.Texture2D;
import de.jreality.tools.PickShowTool;
import de.jreality.ui.viewerapp.ViewerApp;
import de.jreality.util.CameraUtility;
import de.jreality.util.Input;
import de.jreality.util.SceneGraphUtility;

/**
 * This class contains the third in a series of 8 simple introductory examples which mimic the
 * functionality of the 
 * <a href="http://www3.math.tu-berlin.de/jreality/mediawiki/index.php/User_Tutorial"> jReality User Tutorial 
 *</a>.  
 *
 * Same as {@link Intro02} but material properties changed (using {@link Appearance#setAttribute(String, Object)
 * 
 * @author Charles Gunn
 *
 */
public class Intro03 {

	public static void main(String[] args)	{
				SceneGraphComponent dodecSGC = readDodec();
				ViewerApp va = myViewerApp(dodecSGC);
				va.update();
				CameraUtility.encompass(va.getViewerSwitch());
				Appearance ap = dodecSGC.getAppearance();
				// change the color and size of the tubes and spheres
				// do so without using shader interfaces
				ap.setAttribute(LINE_SHADER+"."+DIFFUSE_COLOR, Color.yellow);
				ap.setAttribute(LINE_SHADER+"."+TUBE_RADIUS, .05);
				ap.setAttribute(POINT_SHADER+"."+DIFFUSE_COLOR, Color.red);
				ap.setAttribute(POINT_SHADER+"."+POINT_RADIUS, .1);
				ap.setAttribute(POLYGON_SHADER+"."+SMOOTH_SHADING, false);
				// turn on transparency for faces but keep tubes and spheres opaque
				ap.setAttribute(TRANSPARENCY_ENABLED, true);
				ap.setAttribute(OPAQUE_TUBES_AND_SPHERES, true);
				ap.setAttribute(POLYGON_SHADER+"."+TRANSPARENCY, .4);
	}

	private static SceneGraphComponent readDodec() {
		URL url = Intro03.class.getResource("dodec.off");
		SceneGraphComponent scp = null;
		try {
			scp = Readers.read(Input.getInput(url));
// alternative to access the file as a URL
//			scp = Readers.read(Input.getInput("http://www3.math.tu-berlin.de/jreality/download/data/dodec.off"));
			scp.setName("Dodecahedron");
		} catch (IOException e) {
			e.printStackTrace();
		}
		return scp;
	}

	private static ViewerApp myViewerApp(SceneGraphComponent myscene) {
		ViewerApp va = ViewerApp.display(myscene);
		va.setAttachNavigator(true);
		va.setExternalNavigator(false);
		return va;
	}



}
