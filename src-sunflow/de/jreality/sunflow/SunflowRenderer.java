/**
 *
 * This file is part of jReality. jReality is open source software, made
 * available under a BSD license:
 *
 * Copyright (c) 2003-2006, jReality Group: Charles Gunn, Tim Hoffmann, Markus
 * Schmies, Steffen Weissmann.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * - Neither the name of jReality nor the names of its contributors nor the
 *   names of their associated organizations may be used to endorse or promote
 *   products derived from this software without specific prior written
 *   permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */

package de.jreality.sunflow;

import java.util.Arrays;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Stack;

import org.sunflow.SunflowAPI;
import org.sunflow.core.Display;
import org.sunflow.core.camera.PinholeLens;
import org.sunflow.core.display.FrameDisplay;
import org.sunflow.core.light.ImageBasedLight;
import org.sunflow.core.light.MeshLight;
import org.sunflow.core.primitive.CornellBox;
import org.sunflow.core.primitive.Mesh;
import org.sunflow.core.shader.DiffuseShader;
import org.sunflow.core.shader.GlassShader;
import org.sunflow.core.shader.MirrorShader;
import org.sunflow.image.Color;
import org.sunflow.math.Matrix4;
import org.sunflow.math.Point3;
import org.sunflow.math.Vector3;
import org.sunflow.system.ImagePanel;

import de.jreality.math.Matrix;
import de.jreality.math.Rn;
import de.jreality.scene.Appearance;
import de.jreality.scene.Camera;
import de.jreality.scene.Geometry;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphPath;
import de.jreality.scene.SceneGraphVisitor;
import de.jreality.scene.Sphere;
import de.jreality.shader.DefaultGeometryShader;
import de.jreality.shader.DefaultPolygonShader;
import de.jreality.shader.EffectiveAppearance;
import de.jreality.shader.ShaderUtility;


public class SunflowRenderer extends SunflowAPI {

	IdentityHashMap<Geometry, String> geom2name = new IdentityHashMap<Geometry, String>();
	HashMap<String, Geometry> name2geom = new HashMap<String, Geometry>();
	
	private class Visitor extends SceneGraphVisitor {
		
		SceneGraphPath path=new SceneGraphPath();
		EffectiveAppearance eapp;
		DefaultGeometryShader dgs;
		DefaultPolygonShader dps;
		
		int appCount=0;
		
		@Override
		public void visit(SceneGraphComponent c) {
			path.push(c);
			Geometry g = c.getGeometry();
			if (c.getAppearance() != null) c.getAppearance().accept(this);
			if (g != null) {
			  parameter("transform", new Matrix(path.getMatrix(null)));
			  g.accept(this);
			  parameter("shaders", "default-shader"+appCount);
			  String geomName = getName(g);
			  instance(geomName+".instance", geomName);
			}
			  for (int i=0; i < c.getChildComponentCount(); i++) {
				  c.getChildComponent(i).accept(this);
			  }
			path.pop();
		}
		
		@Override
		public void visit(Sphere s) {
			geometry(getName(s), new org.sunflow.core.primitive.Sphere());
		}
		
		@Override
		public void visit(Appearance a) {
			appCount++;
			System.out.println("Visitor.visit(Appearance)");
			eapp = EffectiveAppearance.create(path);
			dgs = ShaderUtility.createDefaultGeometryShader(eapp);
			dps = (DefaultPolygonShader) dgs.getPolygonShader();
			parameter("diffuse", dps.getDiffuseColor());
			shader("default-shader"+appCount, new DiffuseShader());
		}
	}
	
	public SunflowRenderer() {
		String dataDir = System.getProperty("jreality.data","/net/MathVis/data/testData3D");
		addTextureSearchPath(dataDir+"/textures");
	}
	
	public void render(SceneGraphComponent sceneRoot, SceneGraphPath cameraPath, Display display, int width, int height) {
		
		// light
		parameter("texture", "sky_small.hdr");
		parameter("center", new Vector3( 1,0, -1));
		parameter("up", new Vector3(0, 1, 0));
		parameter("samples", 200);
		ImageBasedLight light = new ImageBasedLight();
		light.init("skylight", this);
		
        // visit
        new Visitor().visit(sceneRoot);
        
        // camera
		float aspect = width/(float)height;
		parameter("aspect",aspect);
		Camera c = (Camera) cameraPath.getLastElement();
		Matrix m = new Matrix(cameraPath.getMatrix(null));
		parameter("transform",m);
		parameter("fov", c.getFieldOfView());
		String name = getUniqueName("camera");
		camera(name, new PinholeLens());
		parameter("camera", name);
		
		// sunflow rendering
		parameter("sampler", "bucket");
		parameter("resolutionX", width);
        parameter("resolutionY", height);
        options(SunflowAPI.DEFAULT_OPTIONS);
        render(SunflowAPI.DEFAULT_OPTIONS, display);
	}
	
	public String getName(Geometry geom) {
		String ret;
		if (geom2name.containsKey(geom)) ret = geom2name.get(geom);
		else {
			if (!name2geom.containsKey(geom.getName())) {
				geom2name.put(geom, geom.getName());
				ret = geom.getName();
			} else {
		        int counter = 1;
		        String name, prefix=geom.getName();
		        do {
		            name = String.format("%s_%d", prefix, counter);
		            counter++;
		        } while (name2geom.containsKey(name));
		        name2geom.put(name, geom);
		        geom2name.put(geom, name);
		        ret = name;
			}
		}
		System.out.println("geomName="+ret);
		return ret;
	}

	public void parameter(String string, java.awt.Color c) {
		System.out.println(string+"="+c);
		parameter(string, new Color(c.getRed()/255f, c.getGreen()/255f, c.getBlue()/255f));
	}

	public void parameter(String name, Matrix m) {
		parameter(name, new Matrix4(
				(float) m.getEntry(0, 0),
				(float) m.getEntry(0, 1),
				(float) m.getEntry(0, 2),
				(float) m.getEntry(0, 3),
				(float) m.getEntry(1, 0),
				(float) m.getEntry(1, 1),
				(float) m.getEntry(1, 2),
				(float) m.getEntry(1, 3),
				(float) m.getEntry(2, 0),
				(float) m.getEntry(2, 1),
				(float) m.getEntry(2, 2),
				(float) m.getEntry(2, 3),
				(float) m.getEntry(3, 0),
				(float) m.getEntry(3, 1),
				(float) m.getEntry(3, 2),
				(float) m.getEntry(3, 3)				
		));
	}

	public void parameterPoint(String name, double[] column) {
		System.out.println(name+"="+Arrays.toString(column));
		parameter(name, new Point3((float) column[0], (float) column[1], (float) column[2]));
	}

	public void parameterVector(String name, double[] column) {
		System.out.println(name+"="+Arrays.toString(column));
		parameter(name, new Vector3((float) column[0], (float) column[1], (float) column[2]));
	}

	public void parameter(String name, double val) {
		parameter(name, (float) val);
	}
}
