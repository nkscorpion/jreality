/*
 * Created on Apr 15, 2004
 *
 */
package de.jreality.examples.jogl;
import de.jreality.jogl.InteractiveViewerDemo;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.FactoredTransformation;
import de.quasitiler.alexanderplatz.Alex3DModel;


/**
 * @author gunn
 *
 */
public class  AlexDemo extends InteractiveViewerDemo {
	SceneGraphComponent icokit;
	public SceneGraphComponent makeWorld() {
		SceneGraphComponent world = new SceneGraphComponent();
		world.setTransformation(new FactoredTransformation());
		world.getTransformation().setRotation(-Math.PI/2., 1,0,0);
        SceneGraphComponent sgc = Alex3DModel.createRoot(6, true, true, true);
        SceneGraphComponent scaleComp = new SceneGraphComponent();
        FactoredTransformation t = new FactoredTransformation();
        t.setStretch(3.);
        scaleComp.setTransformation(t);
        scaleComp.addChild(sgc);
        world.addChild(scaleComp);
        return world;
	}


	public static void main(String argv[])	{
		AlexDemo test = new AlexDemo();
		test.begin();
	}


}
