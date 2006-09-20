package de.jreality.examples.vr;

import java.io.IOException;

import de.jreality.geometry.GeometryUtility;
import de.jreality.geometry.IndexedFaceSetUtility;
import de.jreality.geometry.Primitives;
import de.jreality.math.MatrixBuilder;
import de.jreality.reader.Readers;
import de.jreality.scene.Appearance;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphVisitor;
import de.jreality.scene.data.AttributeEntityUtility;
import de.jreality.shader.CommonAttributes;
import de.jreality.shader.GlslProgram;
import de.jreality.shader.ImageData;
import de.jreality.shader.ShaderUtility;
import de.jreality.shader.Texture2D;
import de.jreality.shader.TextureUtility;
import de.jreality.tools.RotateTool;
import de.jreality.ui.viewerapp.ViewerApp;
import de.jreality.util.Input;
import de.jreality.util.PickUtility;
import de.jreality.vr.ViewerVR;

public class He2Demo {
	
	public static void main(String[] args) throws IOException {
		Appearance app = new Appearance();
		SceneGraphComponent cmp = new SceneGraphComponent();
		
		app.setAttribute(CommonAttributes.EDGE_DRAW, false);
		app.setAttribute(CommonAttributes.VERTEX_DRAW, false);
		app.setAttribute("diffuseColor", java.awt.Color.white);
		cmp.setAppearance(app);
		ImageData img = ImageData.load(Input.getInput("textures/metal_basic88.png"));
		SceneGraphComponent he = Readers.read(Input.getInput("obj/He2SmallTower.obj"));
//		SceneGraphComponent he = Readers.read(Input.getInput("3ds/boy.3ds"));
//		SceneGraphComponent he = new SceneGraphComponent();
//		he.setGeometry(Primitives.torus(10, 5, 20, 20));
//		he.setGeometry(Primitives.plainQuadMesh(1, 1, 1, 1));
		cmp.addChild(he);
		he.addTool(new RotateTool());
		SceneGraphComponent boundary = Readers.read(Input.getInput("obj/He2SmallTowerBoundary.obj"));
		cmp.addChild(boundary);
		he.setAppearance(new Appearance());
		//boundary.setAppearance(new Appearance());
		Texture2D tex = TextureUtility.createTexture(he.getAppearance(), "polygonShader", img, false);
		tex.setTextureMatrix(MatrixBuilder.euclidean().scale(30).getMatrix());
		tex = TextureUtility.createTexture(boundary.getAppearance(), "polygonShader", img, false);
		tex.setTextureMatrix(MatrixBuilder.euclidean().scale(2,400,1).getMatrix());
		PickUtility.assignFaceAABBTrees(cmp);
		MatrixBuilder.euclidean()
//		.rotateY(-1)
//		.reflect( new double[]{1,0,0,0})
		.rotateX(Math.PI/2)
		.assignTo(cmp);
		
		Appearance heApp = new Appearance();
		//TextureUtility.createTexture(heApp, "polygonShader", "textures/sflgratetrans1_d.png");
		TextureUtility.createTexture(heApp, "polygonShader", "textures/metal_basic88.png");
		Texture2D normalTex = (Texture2D) AttributeEntityUtility.createAttributeEntity(Texture2D.class, "normalMap", heApp, true);
		//ImageData normalMap = ImageData.load(Input.getInput("textures/sflgratetrans1_local.png"));
		ImageData normalMap = ImageData.load(Input.getInput("textures/metal_basic88_normal.jpg"));
		normalTex.setImage(normalMap);
		GlslProgram prog = new GlslProgram(heApp, "polygonShader", Input.getInput("de/jreality/jogl/shader/resources/bumpmap.vert"), Input.getInput("de/jreality/jogl/shader/resources/bumpmap.frag"));
		heApp.setAttribute("polygonShader", "glsl");
		prog.setUniform("colorMap", 0);
		prog.setUniform("normalMap", 1);
		prog.setUniform("envMap", 2);
		he.setAppearance(heApp);
		he.accept(new SceneGraphVisitor() {
			@Override
			public void visit(SceneGraphComponent c) {
				c.childrenWriteAccept(this, false, false, false, false, true, false);
			}
			@Override
			public void visit(IndexedFaceSet ifs) {
				GeometryUtility.calculateAndSetVertexNormals(ifs);
				IndexedFaceSetUtility.assignVertexTangents(ifs);
			}
		});
		//System.setProperty("jreality.data", "/net/MathVis/data/testData3D");
		//System.setProperty("de.jreality.scene.Viewer", "de.jreality.soft.DefaultViewer");
		//System.setProperty("de.jreality.ui.viewerapp.autoRender", "false");
		System.setProperty("de.jreality.ui.viewerapp.synchRender", "true");
		ViewerVR tds = new ViewerVR();
		
		tds.setContent(cmp);
		
		ViewerApp va = tds.display();
		va.setAttachBeanShell(true);
		va.setAttachNavigator(true);
		
		va.update();
		va.display();
	}
	
}
