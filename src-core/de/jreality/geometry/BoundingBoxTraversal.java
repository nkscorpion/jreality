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


package de.jreality.geometry;

import java.awt.geom.Rectangle2D;

import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.scene.Appearance;
import de.jreality.scene.ClippingPlane;
import de.jreality.scene.Cylinder;
import de.jreality.scene.Geometry;
import de.jreality.scene.PointSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphVisitor;
import de.jreality.scene.Sphere;
import de.jreality.scene.Transformation;
import de.jreality.scene.data.Attribute;
import de.jreality.scene.data.DataList;
import de.jreality.shader.EffectiveAppearance;
import de.jreality.util.Rectangle3D;

/**
 * This class traverses a scene graph starting from the given "root" scene
 * graph component. The children of each sgc are visited in the following
 * order: First the appearance is visited then the current transformation
 * is popped to the transformationstack and a copy of it gets multiplied by
 * the transformation of the sgc. This copy is then visited. Then all
 * geometries are visited. 
 * Finally the transformation gets poped from the stack.
 * @version 1.0
 * @author <a href="mailto:hoffmann@math.tu-berlin.de">Tim Hoffmann</a>
 *
 */
class BoundingBoxTraversal extends SceneGraphVisitor {

  private boolean shaderUptodate;

  private Bound bound;
  private double tmpVec[] = new double[4];

  protected EffectiveAppearance eAppearance;
  
  double[]  initialTrafo,   currentTrafo;
  //private   Transformation  initialTransformation;
  protected BoundingBoxTraversal reclaimableSubcontext;

  /**
   * 
   */
  public BoundingBoxTraversal() {
    super();
    eAppearance=EffectiveAppearance.create();
    bound =new Bound();
  }

  protected BoundingBoxTraversal(BoundingBoxTraversal parentContext) {
    eAppearance=parentContext.eAppearance;
    initializeFromParentContext(parentContext);
  }


  protected void initializeFromParentContext(BoundingBoxTraversal parentContext) {
    BoundingBoxTraversal p=parentContext;

    eAppearance=parentContext.eAppearance;

    currentTrafo=initialTrafo=parentContext.currentTrafo;
    this.bound = parentContext.bound;
        
  }

  /**
   * Sets the initialTransformation.
   * @param initialTransformation The initialTransformation to set
   */
  public void setInitialMatrix(double[] initialMatrix) {
    this.initialTrafo= initialMatrix;
  }

  BoundingBoxTraversal subContext() {
    if (reclaimableSubcontext != null) {
      reclaimableSubcontext.initializeFromParentContext(this);
      reclaimableSubcontext.shaderUptodate = false;
      return reclaimableSubcontext;
    } else
      return reclaimableSubcontext= new BoundingBoxTraversal(this);
  }
  /**
   * This starts the traversal of a SceneGraph starting form root.
   * @param root
   */
  public void traverse(SceneGraphComponent root) {
    if (initialTrafo == null) {
      initialTrafo= new double[16];
//    if (initialTransformation != null)
//      initialTransformation.getMatrix(initialTrafo);
//    else
      Rn.setIdentityMatrix(initialTrafo);
    }
    currentTrafo= initialTrafo;
    visit(root);
    //pipeline.setMatrix(initialTrafo);
  }

  public void visit(SceneGraphComponent c) {
      if(c.isVisible())
          c.childrenAccept(subContext());
  }

  public void visit(Transformation t) {
    if (initialTrafo == currentTrafo)
      currentTrafo= new double[16];
    Rn.copy(currentTrafo, initialTrafo);
    Rn.times(currentTrafo, currentTrafo, t.getMatrix());
    //pipeline.setMatrix(currentTrafo);
  }

  public void visit(Appearance app) {
    eAppearance = eAppearance.create(app);
    shaderUptodate = false;
  }
  private void setupShader()
  {

    shaderUptodate = true;
  }

  public void visit(Geometry g) {
  	Object bbox = g.getGeometryAttributes(GeometryUtility.BOUNDING_BOX);
  	if (bbox != null && bbox instanceof Rectangle3D)	{
  		Rectangle3D box = (Rectangle3D) bbox;
     	 unionBox(box);		
  	}
    //System.err.println("Warning: unknown geometry type " + g);
  }
  
  public void visit(ClippingPlane p) {
  }
  
  public void visit(Cylinder c) {
      if(!shaderUptodate) setupShader();
      //TODO better to make this by transforming center and a
      // point on the sphere or something like that...
 	 unionBox(Rectangle3D.unitCube);
  }
  public void visit(PointSet p) {
  // Following code should only be activated if we have listeners installed to update 
  // the bounding box when it goes out of date.
  	Object bbox = p.getGeometryAttributes(GeometryUtility.BOUNDING_BOX);
	if (bbox != null && bbox instanceof Rectangle3D)	{
  		Rectangle3D box = (Rectangle3D) bbox;
    	 	unionBox(box);		
    	 	return;
  	}
  	Object domain = p.getGeometryAttributes(GeometryUtility.REGULAR_DOMAIN_QUAD_MESH_SHAPE);
	if (domain != null && domain instanceof Rectangle2D)	{
 		Rectangle2D box = (Rectangle2D) domain;
  	  	double[][] data = p.getVertexAttributes(Attribute.COORDINATES).toDoubleArrayArray(null);
 		double[][] zbnds = new double[2][1];
 		Rn.calculateBounds(zbnds, data);
		double[][] xyzbnds = new double[2][3];
		xyzbnds[0][0] = box.getMinX();
		xyzbnds[1][0] = box.getMaxX();
		xyzbnds[0][1] = box.getMinY();
		xyzbnds[1][1] = box.getMaxY();
		xyzbnds[0][2] = zbnds[0][0];
		xyzbnds[1][2] = zbnds[1][0];
		Rectangle3D box3 = new Rectangle3D(xyzbnds);		
	 	unionBox(box3);		
	 	return;
		
	}
  if(!shaderUptodate) setupShader();
    DataList vv = p.getVertexAttributes(Attribute.COORDINATES);
    if (vv == null)	{
    	//signal error
    	return;
    }
     unionVectors(vv);
        
  }
  
  public void visit(Sphere s) {
    if(!shaderUptodate) setupShader();
    //TODO better to make this by transforming center and a
    // point on the sphere or something like that...
	 unionBox(Rectangle3D.unitCube);
  }

  private final  void unionVectors(DataList dl) {
  	double[][] data = dl.toDoubleArrayArray(null);
	double[][] tmpVec = new double[2][3];
	int length = data.length;
  if (length == 0) return;
	int vectorLength = data[0].length;
	if (vectorLength<3 || vectorLength > 4) return;
	Rn.matrixTimesVector(data, currentTrafo, data);
	if (vectorLength == 4)	{
		Pn.calculateBounds(tmpVec, data);
	} else if (vectorLength == 3){
		Rn.calculateBounds(tmpVec, data);
	}
	bound.xmin = Math.min(bound.xmin,tmpVec[0][0]);
	bound.xmax = Math.max(bound.xmax,tmpVec[1][0]);
	bound.ymin = Math.min(bound.ymin,tmpVec[0][1]);
	bound.ymax = Math.max(bound.ymax,tmpVec[1][1]);
	bound.zmin = Math.min(bound.zmin,tmpVec[0][2]);
	bound.zmax = Math.max(bound.zmax,tmpVec[1][2]);
 }
  
  private final void unionBox(Rectangle3D bbox) {
	if (bbox.isEmpty()) return;
    Rectangle3D tbox = bbox.transformByMatrix(null, currentTrafo);
  	double[][] bnds = tbox.getBounds();
    bound.xmin = Math.min(bound.xmin,bnds[0][0]);
    bound.xmax = Math.max(bound.xmax,bnds[1][0]);
    bound.ymin = Math.min(bound.ymin,bnds[0][1]);
    bound.ymax = Math.max(bound.ymax,bnds[1][1]);
    bound.zmin = Math.min(bound.zmin,bnds[0][2]);
    bound.zmax = Math.max(bound.zmax,bnds[1][2]);
}
/**
 * @return Returns the xmax.
 */
public double getXmax() {
    return bound.xmax;
}

/**
 * @return Returns the xmin.
 */
public double getXmin() {
    return bound.xmin;
}

/**
 * @return Returns the ymax.
 */
public double getYmax() {
    return bound.ymax;
}

/**
 * @return Returns the ymin.
 */
public double getYmin() {
    return bound.ymin;
}

/**
 * @return Returns the zmax.
 */
public double getZmax() {
    return bound.zmax;
}

/**
 * @return Returns the zmin.
 */
public double getZmin() {
    return bound.zmin;
}

public double[] getBoundingBoxCenter(double[] c) {
    if(c == null) c =new double[3];
    c[0] =(bound.xmin+bound.xmax)/2.;
    c[1] =(bound.ymin+bound.ymax)/2.;
    c[2] =(bound.zmin+bound.zmax)/2.;
    return c;
}
private class Bound {
    double xmin,xmax,ymin,ymax,zmin,zmax;
    public Bound() {
        super();
        xmin =ymin = zmin = Double.MAX_VALUE;
        xmax =ymax = zmax =-Double.MAX_VALUE;
    }
}

	/**
	 * Convert result into Rectangle3D instance (see {@link de.jreality.util.Rectangle3D}
	 */
	 public Rectangle3D getBoundingBox()	{
		
		Rectangle3D rect3d = new Rectangle3D();
		double[][] bnds = rect3d.getBounds();
		bnds[0][0] = getXmin();
		bnds[1][0] = getXmax();
		bnds[0][1] = getYmin();
		bnds[1][1] = getYmax();
		bnds[0][2] = getZmin();
		bnds[1][2] = getZmax();
		rect3d.setBounds(bnds);
		return rect3d;
	}

	 public static Rectangle3D getBoundingBox(double[] initialMatrix, SceneGraphComponent sgc)	{
	 	BoundingBoxTraversal bt = new BoundingBoxTraversal();
	 	if (initialMatrix != null) bt.setInitialMatrix(initialMatrix);
	 	bt.traverse(sgc);
	 	return bt.getBoundingBox();
	 }
}