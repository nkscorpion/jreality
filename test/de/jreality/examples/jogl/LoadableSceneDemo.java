/*
 * Created on Feb 8, 2005
 *
 */
package de.jreality.examples.jogl;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

import de.jreality.jogl.InteractiveViewer;
import de.jreality.jogl.InteractiveViewerDemo;
import de.jreality.util.Pn;
import discreteGroup.TriangleGroup;

/**
 * @author gunn
 *
 */
public class LoadableSceneDemo extends InteractiveViewerDemo {

//	/**
//	 * @param split_pane
//	 * @param b
//	 */
//	public LoadableSceneDemo(int split_pane, boolean b) {
//		super(split_pane, b);
//	}

	public static void main(String[] args) throws Exception {
		LoadableSceneDemo iv = new LoadableSceneDemo();//InteractiveViewerDemo.SPLIT_PANE, false);
		iv.initializeScene();
		if (args != null) iv.loadScene(args[0]);
		else iv.loadScene("de.jreality.worlds.AnimationDemo");
    }
    String root = "de.jreality.worlds.";
    String[] loadableScenes = {"de.jreality.worlds.AlexDemo",
    			"de.jreality.worlds.AnimationDemo",
			"de.jreality.worlds.BouncingSpheres",
 			"de.jreality.worlds.DebugLattice",
			"de.jreality.worlds.HopfFibration",
			"de.jreality.worlds.ElephantTrunk",
			"de.jreality.worlds.Icosahedra",
			"de.jreality.worlds.ImplodedTori",
			"de.jreality.worlds.LabelSetDemo",
  			"de.jreality.worlds.JOGLSkyBox",
   			"de.jreality.worlds.StandardDemo",
			"de.jreality.worlds.TestClippingPlane",
			"de.jreality.worlds.TestSphereDrawing",
			"de.jreality.worlds.TestTubes",
  			"discreteGroup.demo.ArchimedeanSolids",
  			"discreteGroup.demo.Cell120",
			"discreteGroup.demo.SoccerBall",
            "de.jreality.worlds.Quake3Demo"};
   
    Class c;
    public void lookupClasses()	{
    		Class c = null;
			try {
				c = Class.forName("de.jreality.util.LoadableScene");
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Interface is "+c.toString());
		Class[] classes = c.getDeclaredClasses();
    }
    
	public JMenuBar createMenuBar() {
		JMenuBar menuBar =  super.createMenuBar();
		//lookupClasses();
		JMenu sceneM = new JMenu("Scenes");
		menuBar.add(sceneM);
		ButtonGroup bg = new ButtonGroup();
		for (int i = 0; i<loadableScenes.length; ++i)	{
			final int j = i;
			JMenuItem jm = sceneM.add(new JRadioButtonMenuItem(loadableScenes[i]));
			jm.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent e)	{
					unloadScene();
					loadScene(loadableScenes[j]);
					viewer.render();
					viewer.getViewingComponent().requestFocus();
				}
			});
			bg.add(jm);
		}
		return menuBar;
	}
}
