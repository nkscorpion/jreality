 /*
 * Created on May 12, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package de.jreality.worlds;
import java.awt.Color;

import de.jreality.geometry.BezierPatchMesh;
import de.jreality.geometry.GeometryUtility;
import de.jreality.geometry.Primitives;
import de.jreality.geometry.QuadMeshShape;
import de.jreality.geometry.TubeUtility;
import de.jreality.geometry.TubeUtilityNew;
import de.jreality.jogl.DiscreteSpaceCurve;
import de.jreality.jogl.shader.DefaultVertexShader;
import de.jreality.scene.Appearance;
import de.jreality.scene.CommonAttributes;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.IndexedLineSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.Transformation;
import de.jreality.scene.data.Attribute;
import de.jreality.util.ConfigurationAttributes;
import de.jreality.util.Pn;
import de.jreality.util.SceneGraphUtilities;

/**
 * @author Charles Gunn
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TestTubes extends AbstractLoadableScene {

	static double x = 1;
	static double[][] circle =  {{x, 0, 0, 1},	 {x, 1,0, 1}, {0,2, 0, 2}, {-x, 1,0, 1}, {-x, 0, 0, 1},{-x, -1,0, 1},{0,-2, 0, 2},{x, -1,0, 1},{x, 0, 0, 1}};
	/*
	 { 
 			{ 0.5,     0.0, 0.0, 1.0},      
 			{ 0.5,     0.27614 ,  0.0, 1.0},
 			{ 0.27614,      0.5, 0.0, 1.0}, 
 			{ 0.0,       0.5,  0.0, 1.0}, 
 			{ -0.27614,      0.5, 0.0, 1.0}, 
 			{ -0.5,     0.27614 ,  0.0, 1.0},     
 			{ -0.5,     0.0, 0.0, 1.0},      
	 		{ -0.5,     -0.27614 ,  0.0, 1.0},     
 			{ -0.27614 ,     -0.5, 0.0, 1.0}, 
 			{ 0.0,       -0.5,  0.0, 1.0}, 
 			{ 0.27614 ,     -0.5, 0.0, 1.0}, 
 			{ 0.5,     -0.27614 ,  0.0, 1.0},     
 			{ 0.5,     0.0, 0.0, 1.0}
	};
	*/
	
	static double octant[][][] = {
			{{0, 0, 1, 1},		{0, 1, 1, 1},		{0, 2, 0, 2}}, 
			{{1, 0, 1, 1},		{1, 1, 1, 1},		{2, 2, 0, 2}}, 
			{{2, 0, 0, 2},		{2 ,0, 0, 2},		{4, 0, 0, 4}}};

	static double[][] form = {{1,1,1}, {1,-1,1},{1,-1,-1},{1,1,-1},{-1,1,-1},{-1,-1,-1},{-1,-1,1},{-1,1,1}};
	static double[][] form2 = {{1,1,1}, {1,0,1}, {1,-1,1},{1,-1,0},{1,-1,-1},{1,0,-1},{1,1,-1},{0,1,-1},
			{-1,1,-1},{-1,0,-1},{-1,-1,-1},{-1,-1,0},{-1,-1,1},{-1,0,1},{-1,1,1}};

	static double[][] otherDirection	= {{1,0,0,0},{0,0,1,0},{0,0,2,0},{1,0,3,0}};
	
	static double[][][] patch;
	static 
	{
		patch = new double[circle.length][otherDirection.length][4];
		for (int i = 0; i<circle.length; ++i)	{
			for (int j = 0; j<otherDirection.length; ++j)	{
				for (int k = 0; k<4; ++k)	
					patch[i][j][k] = circle[i][k] + otherDirection[j][k];
			}
		}
	}

	public SceneGraphComponent makeWorld() {
		SceneGraphComponent root = new SceneGraphComponent();
		root.setTransformation(new Transformation());
		root.setAppearance(new Appearance());
		root.getAppearance().setAttribute(CommonAttributes.VERTEX_DRAW, false);

		double[][] rod = new double[8][3];
		for (int i = 0; i<8; ++i)	{
			rod[i][0] = .1*i;
			rod[i][1] = rod[i][2] = 0.0;
		}
		TubeUtilityNew.makeTubeAsIFS(rod, .04,null,TubeUtilityNew.FRENET,false, Pn.HYPERBOLIC);
		
		boolean simple = true;
		
		double[][] square = {{1,1,0},{-1,1,0},{-1,-1,0},{1,-1,0}};
		
		double[][] profile = {{0,0,0}, {0,.1,0},{1,.1,0},{1,.2,0},{1.4,0,0}};
		double[][] profile2 = {{1,.2,0}, {.2, .2,0}, {0,.4,0}, {-.2, .2, 0},{-1,.2,0}, {-1,-.2,0},{-.2, -.2,0}, {0,-.4,0}, {.2, -.2, 0},{1,-.2,0}};
	   DiscreteSpaceCurve torus1 = DiscreteSpaceCurve.discreteTorusKnot(1,.25, 2, 9, 60);
	   double[][] tpts = torus1.getVertexAttributes(Attribute.COORDINATES).toDoubleArrayArray(null);
	   //torus1.addGeometryListener(torus1);
	   //pts = square;
	   double[][] pts = form2;
	   
	   //QuadMeshShape torust = TubeUtility.makeTubeAsIFS(tpts, .2,  null, TubeUtility.PARALLEL, false);
	   //GeometryUtility.calculateAndSetNormals(torust);
	   SceneGraphComponent torussgc = SceneGraphUtilities.createFullSceneGraphComponent("torus knot");
	   torussgc.getAppearance().setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.POLYGON_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, 
	   		new Color(120,0,  120));
	   torussgc.getAppearance().setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.TUBE_RADIUS, .03);
	   if (!simple)	{
		   QuadMeshShape torus1Tubes = TubeUtilityNew.makeTubeAsIFS(tpts, .04, null, TubeUtilityNew.PARALLEL, true, Pn.EUCLIDEAN);
		   GeometryUtility.calculateAndSetNormals(torus1Tubes);
		   torussgc.setGeometry(torus1Tubes); //torus1);
		   torussgc.getTransformation().setStretch(.9);
		   root.addChild(torussgc);	   	
	   }
	   
	   IndexedFaceSet arrow = GeometryUtility.surfaceOfRevolutionAsIFS(profile, 24, Math.PI * 2);
	   SceneGraphComponent globeNode= SceneGraphUtilities.createFullSceneGraphComponent("container");
	   SceneGraphComponent globeNode2= SceneGraphUtilities.createFullSceneGraphComponent("curve");
	   Appearance ap1 = globeNode2.getAppearance();
	   ap1.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, DefaultVertexShader.BLACK);
	   //ap1.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.TUBES_DRAW, true);
	   //ap1.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.LINE_WIDTH,2.0);
	   ap1.setAttribute(CommonAttributes.FACE_DRAW,true);
	   ap1.setAttribute(CommonAttributes.EDGE_DRAW,true);
	   ap1.setAttribute(CommonAttributes.VERTEX_DRAW,false);
	   //ap1.setAttribute(CommonAttributes.POINT_SHADER+"."+CommonAttributes.SPHERES_DRAW,true);
	   ap1.setAttribute(CommonAttributes.POINT_SHADER+"."+CommonAttributes.POINT_RADIUS,.03);
	   ap1.setAttribute(CommonAttributes.POINT_SHADER+"."+CommonAttributes.POINT_SIZE, 3.0);
	   ap1.setAttribute(CommonAttributes.POINT_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, DefaultVertexShader.RED);
	   //IndexedLineSet croxl = GeometryUtility.createCurveFromPoints(form, true);
	   //globeNode2.addChild(TubeUtility.ballAndStick(croxl, .05, .03, java.awt.Color.RED, java.awt.Color.BLUE, Pn.EUCLIDEAN));
	   if (!simple)	{
		   QuadMeshShape qms = TubeUtilityNew.makeTubeAsIFS(pts, .04, null, TubeUtility.PARALLEL, true, Pn.EUCLIDEAN);
		   GeometryUtility.calculateAndSetNormals(qms);	   	
		   globeNode2.setGeometry(qms); //croxl);
	   }
	   
	   SceneGraphComponent globeNode4= SceneGraphUtilities.createFullSceneGraphComponent("patch");
	   
	   double[][][] tubePoints = TubeUtilityNew.makeTubeAsBezierPatchMesh(form, .2, circle, TubeUtility.PARALLEL,true, Pn.EUCLIDEAN);
	   BezierPatchMesh bpm = new BezierPatchMesh(2, 3, tubePoints);
	   for (int i = 0; i<3; ++i)	{ bpm.refine();}
	   QuadMeshShape qmpatch = GeometryUtility.representBezierPatchMeshAsQuadMesh(bpm);	   
	   globeNode4.setGeometry(qmpatch);

	   SceneGraphComponent tubie = TubeUtilityNew.ballAndStick(Primitives.sharedIcosahedron, .10, .05, java.awt.Color.YELLOW, java.awt.Color.GREEN, Pn.EUCLIDEAN); //TubeUtility.createTubesOnEdges(Primitives.sharedIcosahedron, .05); //TubeUtility.makeTubeAsIFS(p1, p2, .3, null);
	   tubie.setTransformation(new Transformation());
	   tubie.getTransformation().setStretch(.5);
	   tubie.setAppearance(new Appearance());
	   globeNode4.addChild(tubie);
	   ap1 = new Appearance();
	   ap1.setAttribute(CommonAttributes.POLYGON_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, Color.BLUE);
	   ap1.setAttribute(CommonAttributes.SPECULAR_EXPONENT, 100.0);
	   ap1.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, Color.BLACK);
	   ap1.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, Color.BLACK);
	   ap1.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.TUBES_DRAW, false);
	   ap1.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.LINE_WIDTH,1.0);
	   ap1.setAttribute(CommonAttributes.POINT_RADIUS,3.0);
	   globeNode4.setAppearance(ap1);
 
	   root.addChild(globeNode);
	   globeNode.addChild(globeNode2);
	   globeNode.addChild(globeNode4);
	  return root;
	}
	 
	public void setConfiguration(ConfigurationAttributes config) {
	}

	public int getSignature() {
		return Pn.EUCLIDEAN;
	}

	public boolean addBackPlane() {
		return true;
	}
	public boolean isEncompass() {
		return true;
	}

}
