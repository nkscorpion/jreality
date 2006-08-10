package de.jreality.tools;

import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.IndexedLineSet;
import de.jreality.scene.PointSet;
import de.jreality.scene.data.Attribute;
import de.jreality.scene.pick.PickResult;
import de.jreality.scene.tool.AbstractTool;
import de.jreality.scene.tool.InputSlot;
import de.jreality.scene.tool.ToolContext;


public class DragEventTool extends AbstractTool {
	

	protected PointDragListener pointDragListener;
	protected LineDragListener lineDragListener;
	protected FaceDragListener faceDragListener;
	
  private static final InputSlot pointerSlot = InputSlot.getDevice("PointerTransformation");
  private static InputSlot alongZPointerSlot = InputSlot.getDevice("DragAlongViewDirection");
  
	public DragEventTool(String dragSlotName){
		super(InputSlot.getDevice(dragSlotName));
		addCurrentSlot(pointerSlot, "triggers drag events");
    addCurrentSlot(alongZPointerSlot);
	}
	
	public DragEventTool(){
		  this("AllDragActivation");      
	}
	
  protected boolean active;
  private boolean dragInViewDirection;
  protected PointSet pointSet;
  protected IndexedLineSet lineSet;
  protected IndexedFaceSet faceSet;
  protected int index=-1;
  protected double[] pickPoint=new double[4];;
  private int pickType=PickResult.PICK_TYPE_OBJECT;
    
  private Matrix pointerToPoint = new Matrix();
    
	public void activate(ToolContext tc) {
		active = true;    
    try {
      if (tc.getAxisState(alongZPointerSlot).isPressed()) 
        dragInViewDirection = true;
      else 
        dragInViewDirection = false;
    }catch (Exception me) {dragInViewDirection = false;}  
    
    tc.getTransformationMatrix(pointerSlot).toDoubleArray(pointerToPoint.getArray());     
    pointerToPoint.invert();     
    pointerToPoint.multiplyOnRight(tc.getRootToLocal().getMatrix(null));    
    
    Matrix root2cam=new Matrix(tc.getViewer().getCameraPath().getMatrix(null));
    root2cam.setColumn(3,new double[]{0,0,0,1});  
    distDir[0]=Rn.normalize(null,root2cam.multiplyVector(dir2ScaleZDrag[0]));
    distDir[1]=Rn.normalize(null,root2cam.multiplyVector(dir2ScaleZDrag[1]));
          
    if (tc.getCurrentPick().getPickType() == PickResult.PICK_TYPE_POINT) {
      if (pointDragListener == null) {
        active=false;
        tc.reject();
        return;
      }
	    pickType=PickResult.PICK_TYPE_POINT;
	    pointSet = (PointSet) tc.getCurrentPick().getPickPath().getLastElement();
	    index=tc.getCurrentPick().getIndex();  
      double[] pickPointTemp = pointSet.getVertexAttributes(Attribute.COORDINATES).toDoubleArrayArray().getValueAt(index).toDoubleArray(null);
      if(pickPointTemp.length==3) Pn.homogenize(pickPoint,pickPointTemp);
      else Pn.dehomogenize(pickPoint,pickPointTemp);
      MatrixBuilder.euclidean(pointerToPoint).translate(pickPoint);  
	    firePointDragStart(pickPoint);        
	  }else if (tc.getCurrentPick().getPickType() == PickResult.PICK_TYPE_LINE) {	            
	    if (lineDragListener == null) {
	      active=false;
        tc.reject();
        return;
      }
	    pickType=PickResult.PICK_TYPE_LINE;
	    lineSet = (IndexedLineSet) tc.getCurrentPick().getPickPath().getLastElement();
	    index=tc.getCurrentPick().getIndex();	            
	    double[] pickPointTemp=tc.getCurrentPick().getObjectCoordinates();
      if(pickPointTemp.length==3) Pn.homogenize(pickPoint,pickPointTemp);
      else Pn.dehomogenize(pickPoint,pickPointTemp);
	    MatrixBuilder.euclidean(pointerToPoint).translate(pickPoint);	            
	    fireLineDragStart(new double[]{0,0,0,1});        
	  }else if (tc.getCurrentPick().getPickType() == PickResult.PICK_TYPE_FACE) {
      if (faceDragListener == null) {
        active=false;
        tc.reject();
        return;
      }
      pickType=PickResult.PICK_TYPE_FACE;
	    faceSet = (IndexedFaceSet) tc.getCurrentPick().getPickPath().getLastElement();
	    index=tc.getCurrentPick().getIndex();
	    double[] pickPointTemp=tc.getCurrentPick().getObjectCoordinates();
      if(pickPointTemp.length==3) Pn.homogenize(pickPoint,pickPointTemp);
      else Pn.dehomogenize(pickPoint,pickPointTemp);
	    MatrixBuilder.euclidean(pointerToPoint).translate(pickPoint); 	            
	    fireFaceDragStart(new double[]{0,0,0,1});        
	  }else {
      active=false;
      tc.reject();
    }
	}

  
  private final double[][] dir2ScaleZDrag=new double[][]{{1,0,0},{0,1,0}};   //Richtung in Weltkoordinaten, die die Staerke des drag in z-Richtung bestimmt
  private double[][] distDir=new double[dir2ScaleZDrag.length][dir2ScaleZDrag[0].length];
  private double f=2*Math.sin(Math.PI/4);
  private Matrix result=new Matrix();
	
	public void perform(ToolContext tc) {		
 		if (!active) return;    
    tc.getTransformationMatrix(pointerSlot).toDoubleArray(result.getArray());     
    result.multiplyOnRight(pointerToPoint);
    result.multiplyOnLeft(tc.getRootToLocal().getInverseMatrix(null));
    
    double[] newPoint3=new double[3];   Pn.dehomogenize(newPoint3,result.getColumn(3));
    double[] pickPoint3=new double[3];  Pn.dehomogenize(pickPoint3,pickPoint); 
    double[] translation3=Rn.subtract(null,newPoint3,pickPoint3);
    if(dragInViewDirection){       
      double[] dir=new double[3];
      Pn.dehomogenize(dir,pointerToPoint.getInverse().getColumn(2));
      Rn.normalize(dir,dir);
      Matrix root2local=new Matrix(tc.getRootToLocal().getMatrix(null));
      root2local.setColumn(3,new double[]{0,0,0,1});      
      Pn.dehomogenize(translation3,root2local.multiplyVector(Pn.homogenize(null,translation3)));        
      double factor=(Rn.innerProduct(distDir[0],translation3)+Rn.innerProduct(distDir[1],translation3))/f;
     //double factor=Rn.innerProduct(distDir[0],translation3);
      factor=factor/Rn.euclideanNorm(root2local.multiplyVector(dir)); //teilen durch rueck-skalierung
      Rn.times(translation3,factor,dir);
    }   
    double[] translation={translation3[0],translation3[1],translation3[2],1};
    
	  if (pickType == PickResult.PICK_TYPE_POINT) {      
	    firePointDragged(Rn.add(translation,translation,pickPoint));
	  }else if (pickType == PickResult.PICK_TYPE_LINE) {
	    fireLineDragged(translation);    	
	  }else if (pickType == PickResult.PICK_TYPE_FACE) {
      fireFaceDragged(translation);	    	
	  }
	}

	public void deactivate(ToolContext tc) {
		  if (!active) return;   
	      if (pickType == PickResult.PICK_TYPE_POINT) firePointDragEnd(new double[]{0,0,0,1});
	      else if (pickType == PickResult.PICK_TYPE_LINE) fireLineDragEnd(new double[]{0,0,0,1});
	      else if (pickType == PickResult.PICK_TYPE_FACE) fireFaceDragEnd(new double[]{0,0,0,1});
	      index=-1;
	      pointSet=null;
	      lineSet=null;
	      faceSet=null;
	      active = false;	
	      result=new Matrix();
	      pickType=PickResult.PICK_TYPE_OBJECT;
	}	
	
    public void addPointDragListener(PointDragListener listener) {
        pointDragListener = PointDragEventMulticaster.add(pointDragListener, listener);
    }
    public void removePointDragListener(PointDragListener listener) {
    	pointDragListener = PointDragEventMulticaster.remove(pointDragListener, listener);
    }    
    public void addLineDragListener(LineDragListener listener) {
        lineDragListener = LineDragEventMulticaster.add(lineDragListener, listener);
    }
    public void removeLineDragListener(LineDragListener listener) {
    	lineDragListener = LineDragEventMulticaster.remove(lineDragListener, listener);
    }    
    public void addFaceDragListener(FaceDragListener listener) {
    	faceDragListener = FaceDragEventMulticaster.add(faceDragListener, listener);
    }
    public void removeFaceDragListener(FaceDragListener listener) {
    	faceDragListener = FaceDragEventMulticaster.remove(faceDragListener, listener);
    }
	
    protected void firePointDragStart(double[] location) {
        final PointDragListener l=pointDragListener;
        if (l != null) l.pointDragStart(new PointDragEvent(pointSet, index, location));
    }
    protected void firePointDragged(double[] location) {
        final PointDragListener l=pointDragListener;
        if (l != null) l.pointDragged(new PointDragEvent(pointSet, index, location));
    }      
    protected void firePointDragEnd(double[] location) {
        final PointDragListener l=pointDragListener;
        if (l != null) l.pointDragEnd(new PointDragEvent(pointSet, index, location));
    }
    
	protected void fireLineDragStart(double[] translation) {
	    final LineDragListener l=lineDragListener;
		if (l != null) l.lineDragStart(new LineDragEvent(lineSet, index, translation));
	}
    protected void fireLineDragged(double[] translation) {
		final LineDragListener l=lineDragListener;
		if (l != null) l.lineDragged(new LineDragEvent(lineSet, index, translation));
	}
	protected void fireLineDragEnd(double[] translation) {
		final LineDragListener l=lineDragListener;
		if (l != null) l.lineDragEnd(new LineDragEvent(lineSet, index, translation));
	}
		   
	protected void fireFaceDragStart(double[] translation) {
		final FaceDragListener l=faceDragListener;
		if (l != null) l.faceDragStart(new FaceDragEvent(faceSet, index, translation));
	}
    protected void fireFaceDragged(double[] translation) {
		final FaceDragListener l=faceDragListener;
		if (l != null) l.faceDragged(new FaceDragEvent(faceSet, index, translation));
	}
    protected void fireFaceDragEnd(double[] translation) {
		final FaceDragListener l=faceDragListener;
		if (l != null) l.faceDragEnd(new FaceDragEvent(faceSet, index, translation));
	}
}
