/*
 * Created on Nov 25, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package de.jreality.jogl;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.rsasign.p;

import net.java.games.jogl.DebugGL;
import net.java.games.jogl.GL;
import net.java.games.jogl.GLCanvas;
import net.java.games.jogl.GLDrawable;
import net.java.games.jogl.GLU;
import net.java.games.jogl.util.BufferUtils;
import de.jreality.geometry.SphereHelper;
import de.jreality.geometry.TubeUtility;
import de.jreality.jogl.pick.JOGLPickAction;
import de.jreality.jogl.shader.DefaultGeometryShader;
import de.jreality.jogl.shader.RenderingHintsShader;
import de.jreality.scene.Camera;
import de.jreality.scene.Geometry;
import de.jreality.scene.Graphics3D;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.IndexedLineSet;
import de.jreality.scene.PointSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphNode;
import de.jreality.scene.SceneGraphPath;
import de.jreality.scene.SceneGraphVisitor;
import de.jreality.scene.Sphere;
import de.jreality.scene.Transformation;
import de.jreality.scene.event.AppearanceEvent;
import de.jreality.scene.event.AppearanceListener;
import de.jreality.scene.event.GeometryEvent;
import de.jreality.scene.event.GeometryListener;
import de.jreality.scene.event.SceneAncestorListener;
import de.jreality.scene.event.SceneContainerEvent;
import de.jreality.scene.event.SceneContainerListener;
import de.jreality.scene.event.SceneHierarchyEvent;
import de.jreality.scene.event.SceneTreeListener;
import de.jreality.scene.event.TransformationEvent;
import de.jreality.scene.event.TransformationListener;
import de.jreality.scene.pick.PickPoint;
import de.jreality.util.CameraUtility;
import de.jreality.util.ClippingPlaneCollector;
import de.jreality.util.EffectiveAppearance;
import de.jreality.util.LightCollector;
import de.jreality.util.Rectangle3D;
import de.jreality.util.Rn;
/**
 * TODO implement  isVisible   bit in SceneGraphNode
 * TODO implement collectAncestorVisitor (see method geometryChanged() at end of file )
 * @author gunn
 *
 */
public class JOGLRenderer extends SceneGraphVisitor implements JOGLRendererInterface {

	final static Logger theLog;
	static boolean debugGL = false, collectFrameRate = true;
	static {
		theLog	= Logger.getLogger("de.jreality.jogl");
//		theLog.setLevel(Level.FINEST);
		String foo = System.getProperty("jreality.jogl.debugGL");
		if (foo != null) { if (foo.equals("false")) debugGL = false; else debugGL =true;}
//		theLog.setLevel(Level.FINEST);
	}
	
	public final static int MAX_STACK_DEPTH = 30;
	protected int stackDepth;
	JOGLRenderer globalHandle = null;
	SceneGraphPath currentPath = new SceneGraphPath();
	protected int whichEye;
	
	
	de.jreality.jogl.Viewer theViewer;
	SceneGraphComponent theRoot, auxiliaryRoot;
	JOGLPeerComponent thePeerRoot = null;
	JOGLPeerComponent thePeerAuxilliaryRoot = null;
	JOGLRendererHelper helper;

	GLCanvas theCanvas;
	Graphics3D gc;
	GL globalGL;
	GLU globalGLU;
	public  boolean texResident;
	int numberTries = 0;		// how many times we have tried to make textures resident
	boolean  useDisplayLists;
	private boolean globalIsReflection = false;

	// pick-related stuff
	boolean pickMode = false;
	private final double pickScale = 10000.0;
	Transformation pickT = new Transformation();
	PickPoint[] hits;

	boolean screenShot = false;
	double framerate;
	int lightCount = 0;
	int nodeCount = 0;
	Hashtable geometries = new Hashtable();
	boolean geometryRemoved = false, lightListDirty = true;

	/**
	 * @param viewer
	 */
	public JOGLRenderer(de.jreality.jogl.Viewer viewer) {
		super();
		theViewer = viewer;
		theCanvas = ((GLCanvas) viewer.getViewingComponent());
		theRoot = viewer.getSceneRoot();
		useDisplayLists = true;
		theLog.log(Level.FINER, "Looked up logger successfully");
		
		globalHandle = this;
		helper = new JOGLRendererHelper();
		
		javax.swing.Timer followTimer = new javax.swing.Timer(1000, new ActionListener()	{
			public void actionPerformed(ActionEvent e) {updateGeometryHashtable(); } } );
		followTimer.start();
		
	}



	public SceneGraphComponent getAuxiliaryRoot() {
		return auxiliaryRoot;
	}
	public void setAuxiliaryRoot(SceneGraphComponent auxiliaryRoot) {
		this.auxiliaryRoot = auxiliaryRoot;
		if (auxiliaryRoot != null) {
			if (thePeerAuxilliaryRoot != null) thePeerAuxilliaryRoot.dispose();
			thePeerAuxilliaryRoot = constructPeerForSceneGraphComponent(auxiliaryRoot, null);
		}

	}

	/* (non-Javadoc)
	 * @see de.jreality..SceneGraphVisitor#init()
	 */
	public Object visit() {
		//System.err.println("Initializing Visiting");
		// check to see that the root hasn't changed; all other changes handled by hierarchy events
		if (thePeerRoot == null || theViewer.getSceneRoot() != thePeerRoot.getOriginalComponent())	{
			if (thePeerRoot != null) thePeerRoot.dispose();
			theRoot = theViewer.getSceneRoot();
			thePeerRoot = constructPeerForSceneGraphComponent(theRoot, null);
		}
		
		gc  = new Graphics3D(theViewer);
		JOGLRendererHelper.initializeGLState(theCanvas);
		
		globalGL.glMatrixMode(GL.GL_PROJECTION);
		globalGL.glLoadIdentity();

		if (!pickMode)
			JOGLRendererHelper.handleBackground(theCanvas, theRoot.getAppearance());
		if (pickMode)
			globalGL.glMultTransposeMatrixd(pickT.getMatrix());

		theCanvas.setAutoSwapBufferMode(!pickMode);
		
		// We "inline" the visit to the camera since it cannot be visited in the traversal order
		// load the projection transformation
		globalGL.glMultTransposeMatrixd(CameraUtility.getCamera(theViewer).getCameraToNDC(whichEye));

		// prepare for rendering the geometry
		globalGL.glMatrixMode(GL.GL_MODELVIEW);
		globalGL.glLoadIdentity();
		double[] w2c = gc.getWorldToCamera();
		globalGL.glLoadTransposeMatrixd(w2c);
		globalIsReflection = (theViewer.isFlipped != (Rn.determinant(w2c) < 0.0));
		if (!pickMode) processLights();
		
		processClippingPlanes();
		
		nodeCount = 0;			// for profiling info
		texResident = true;		// assume the best ...
		thePeerRoot.render();		
		//System.out.println("Nodes visited in render traversal: "+nodeCount);
		if (!pickMode && thePeerAuxilliaryRoot != null) thePeerAuxilliaryRoot.render();
		globalGL.glLoadIdentity();
		forceResidentTextures();
		
		lightListDirty = false;
		return null;
	}
	
	/**
	 * @param theRoot2
	 * @return
	 */
	protected JOGLPeerComponent constructPeerForSceneGraphComponent(SceneGraphComponent sgc, JOGLPeerComponent p) {
		if (sgc == null) return null;
		JOGLPeerComponent peer = null;
		synchronized(sgc.getChildLock())	{
			ConstructPeerGraphVisitor constructPeer = new ConstructPeerGraphVisitor( sgc, p);
			peer = (JOGLPeerComponent) constructPeer.visit();				
		}
		return peer;
	}



	/**
	 * 
	 */
	private void processClippingPlanes() {
		List clipPlanes = null;
		if (clipPlanes == null)	{
			ClippingPlaneCollector lc = new ClippingPlaneCollector(theRoot);
			clipPlanes = (List) lc.visit();			
		}
		helper.processClippingPlanes(globalGL, clipPlanes);
	}

	// TODO convert this to peer structure
	static List lights = null;
	/**
	 * @param theRoot2
	 * @param globalGL2
	 * @param lightListDirty2
	 */
	private void processLights( ) {
		if (lights == null || lights.size() == 0 || lightListDirty) {
			LightCollector lc = new LightCollector(theRoot);
			lights = (List) lc.visit();
		}
		helper.processLights(globalGL, lights);
	}



	/**
	 * 
	 */
	private void forceResidentTextures() {
		// Try to force textures to be resident if they're not already
		if (!texResident && numberTries < 3)	{
			final Viewer theV = theViewer;
			TimerTask rerenderTask = new TimerTask()	{
				public void run()	{
					theV.render();
				}
			};
			Timer doIt = new Timer();
			doIt.schedule(rerenderTask, 10);
			forceNewDisplayLists();
			//System.out.println("Attempting to force textures resident");
			numberTries++;		// don't keep trying indefinitely
		} else numberTries = 0;
	}


	/**
	 * 
	 */
	private void forceNewDisplayLists() {
		if (thePeerRoot != null) thePeerRoot.setDisplayListDirty(true);
		if (thePeerAuxilliaryRoot != null) thePeerAuxilliaryRoot.setDisplayListDirty(true);
	}


	int frameCount = 0;
	long[] history = new long[20];
	
	/* (non-Javadoc)
	 * @see net.java.games.jogl.GLEventListener#init(net.java.games.jogl.GLDrawable)
	 */
	public void init(GLDrawable drawable) {
		if (debugGL) {
			drawable.setGL(new DebugGL(drawable.getGL()));
		}
		globalGL = theCanvas.getGL();
		globalGLU = theCanvas.getGLU();

		String vv = globalGL.glGetString(GL.GL_VERSION);
		theLog.log(Level.INFO,"version: "+vv);
		
//		otime = System.currentTimeMillis();

		if (CameraUtility.getCamera(theViewer) == null || theCanvas == null) return;
		CameraUtility.getCamera(theViewer).setAspectRatio(((double) theCanvas.getWidth())/theCanvas.getHeight());
		globalGL.glViewport(0,0, theCanvas.getWidth(), theCanvas.getHeight());

}

	/* (non-Javadoc)
	 * @see net.java.games.jogl.GLEventListener#display(net.java.games.jogl.GLDrawable)
	 */
	public void display(GLDrawable drawable) {
		//if (pickMode) return;
		long beginTime = 0;
		if (collectFrameRate) beginTime = System.currentTimeMillis();
		theCanvas = (GLCanvas) drawable;
		globalGL = theCanvas.getGL();
		Camera theCamera = CameraUtility.getCamera(theViewer);
		if (theCamera != CameraUtility.getCamera(theViewer))	{
			theCamera = CameraUtility.getCamera(theViewer);
			theCamera.setAspectRatio(((double) theCanvas.getWidth())/theCanvas.getHeight());
			globalGL.glViewport(0,0, theCanvas.getWidth(), theCanvas.getHeight());
		}

		//theCamera.update();
		// TODO for split screen stereo, may want to have a real rectangle here, not always at (0,0)
		// for now just do cross-eyed stereo
		if (theCamera.isStereo())		{
			int which = theViewer.getStereoType();
			if (which == Viewer.CROSS_EYED_STEREO)		{
				int w = theCanvas.getWidth()/2;
				int h = theCanvas.getHeight();
				theCamera.setAspectRatio(((double) w)/h);
				theCamera.update();
				whichEye = Camera.RIGHT_EYE;
				globalGL.glClear (GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
				globalGL.glViewport(0,0, w,h);
				visit();
				whichEye = Camera.LEFT_EYE;
				globalGL.glViewport(w, 0, w,h);
				visit();
			} 
			else if (which >= Viewer.RED_BLUE_STEREO &&  which <= Viewer.RED_CYAN_STEREO) {
				theCamera.setAspectRatio(((double) theCanvas.getWidth())/theCanvas.getHeight());
				globalGL.glViewport(0,0, theCanvas.getWidth(), theCanvas.getHeight());
				globalGL.glClear (GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
				whichEye = Camera.RIGHT_EYE;
		        if (which == Viewer.RED_GREEN_STEREO) globalGL.glColorMask(false, true, false, true);
		        else if (which == Viewer.RED_BLUE_STEREO) globalGL.glColorMask(false, false, true, true);
		        else if (which == Viewer.RED_CYAN_STEREO) globalGL.glColorMask(false, true, true, true);
				visit();
				whichEye = Camera.LEFT_EYE;
		        globalGL.glColorMask(true, false, false, true);
				globalGL.glClear (GL.GL_DEPTH_BUFFER_BIT);
				visit();
		        globalGL.glColorMask(true, true, true, true);
			} 
			else	{
				theCamera.setAspectRatio(((double) theCanvas.getWidth())/theCanvas.getHeight());
				globalGL.glViewport(0,0, theCanvas.getWidth(), theCanvas.getHeight());
				whichEye = Camera.RIGHT_EYE;
				globalGL.glDrawBuffer(GL.GL_BACK_RIGHT);
				globalGL.glClear (GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
				visit();
				whichEye = Camera.LEFT_EYE;
				globalGL.glDrawBuffer(GL.GL_BACK_LEFT);
				globalGL.glClear (GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
				visit();
			}
		} 
		else {
			globalGL.glClear (GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
			theCamera.setAspectRatio(((double) theCanvas.getWidth())/theCanvas.getHeight());
			globalGL.glViewport(0,0, theCanvas.getWidth(), theCanvas.getHeight());
			if (!pickMode)	visit();
			if (pickMode)	{
				// set up the "pick transformation"
				IntBuffer selectBuffer = BufferUtils.newIntBuffer(bufsize);
				
				double[] pp3 = new double[3];
				pp3[0] = -pickScale * pickPoint[0]; pp3[1] = -pickScale * pickPoint[1]; pp3[2] = 0.0;
				
				pickT.setTranslation(pp3);
				double[] stretch = {pickScale, pickScale, 1.0};
				pickT.setStretch(stretch);
				boolean store = isUseDisplayLists();
				useDisplayLists = false;
				globalGL.glSelectBuffer(bufsize, selectBuffer);		
				globalGL.glRenderMode(GL.GL_SELECT);
				globalGL.glInitNames();
				globalGL.glPushName(0);
				visit();
				pickMode = false;
				int numberHits = globalGL.glRenderMode(GL.GL_RENDER);
				//System.out.println(numberHits+" hits");
				hits = JOGLPickAction.processOpenGLSelectionBuffer(numberHits, selectBuffer, pickPoint,theViewer);
				useDisplayLists = store;
				display(drawable);
			}
		}
		if (screenShot)   JOGLRendererHelper.saveScreenShot(theCanvas, screenShotFile);
//		if (++frameCount % 100 == 0) {
//			long time = System.currentTimeMillis();
//			framerate = (100000.0/(time-otime));
//			otime = time;
//		} 
		if (collectFrameRate)	{
			++frameCount;
			history[(frameCount % 20)]  = System.currentTimeMillis() - beginTime;			
		}
	}

	/* (non-Javadoc)
	 * @see net.java.games.jogl.GLEventListener#displayChanged(net.java.games.jogl.GLDrawable, boolean, boolean)
	 */
	public void displayChanged(GLDrawable arg0, boolean arg1, boolean arg2) {
	}

	/* (non-Javadoc)
	 * @see net.java.games.jogl.GLEventListener#reshape(net.java.games.jogl.GLDrawable, int, int, int, int)
	 */
	public void reshape(GLDrawable arg0,int arg1,int arg2,int arg3,int arg4) {
		CameraUtility.getCamera(theViewer).setAspectRatio(((double) theCanvas.getWidth())/theCanvas.getHeight());
		globalGL.glViewport(0,0, theCanvas.getWidth(), theCanvas.getHeight());
	}

	/**
	 * @return
	 */
	public boolean isUseDisplayLists() {
		return useDisplayLists;
	}

	/**
	 * @param b
	 */
	public void setUseDisplayLists(boolean b) {
		useDisplayLists = b;
		if (useDisplayLists)	forceNewDisplayLists();
	}

//	public void geometryChanged(GeometryEvent e) {
//		}
		/*
		CollectAncestorsVisitor cav = new CollectAncestorsVisitor(theRoot, sg);
		List anc = (List) cav.visit();
		for (int i = 0; i<anc.size(); ++i)		{
			Object ancx = anc.get(i);
			//System.err.println("ancestor: "+ancx);
			foo = dlTable.get(ancx);
			if (foo!= null) dlTable.remove(ancx);
		}
		*/
//	}
	private final static int POINTDL = 0;
	private final static int LINEDL = 1;
	private final static int PROXY_LINEDL = 2;
	private final static int FLAT_POLYGONDL = 3;
	private final static int SMOOTH_POLYGONDL = 4;
    private final static int NUM_DLISTS = 5;
    private final static int POINTS_CHANGED = 1;
    private final static int LINES_CHANGED = 2;
    private final static int FACES_CHANGED = 4;
    
	private class DisplayListInfo	{
		private boolean useDisplayList, 	// can decide based on dynamic evaluation whether it makes sense 
					insideDisplayList,
					realDLDirty[] = new boolean[NUM_DLISTS];
		private int dl[];
		private int changeCount;
		private long frameCountAtLastChange;
		DisplayListInfo()	{
			super();
			dl = new int[NUM_DLISTS];
			for (int i = 0; i<NUM_DLISTS; ++i) { realDLDirty[i] = true; dl[i] = -1;}
			insideDisplayList = false;
			frameCountAtLastChange = frameCount;
			changeCount = 0;
		}
		boolean useDisplayList()	{
			if (changeCount <= 3) return true;
			long df = frameCount - frameCountAtLastChange;
			if (useDisplayList && df <= 5) useDisplayList = false;
			else if (!useDisplayList && df >= 5) useDisplayList = true;
			return useDisplayList;
		}

		public void setFlatPolygonDisplayListID(int id)	{
			dl[FLAT_POLYGONDL] = id;
		}
		
		public int getFlatPolygonDisplayListID() {
			return dl[FLAT_POLYGONDL];
		}
		
		public void setSmoothPolygonDisplayListID(int id)	{
			dl[SMOOTH_POLYGONDL] = id;
		}
		
		public int getSmoothPolygonDisplayListID() {
			return dl[SMOOTH_POLYGONDL];
		}
		
		public void setLineDisplayListID(int id)	{
			dl[LINEDL] = id;
		}
		
		public int getLineDisplayListID() {
			return dl[LINEDL];
		}
		
		public int getTubeDisplayListID() {
			return dl[PROXY_LINEDL];
		}
		
		public void setTubeDisplayListID(int id)	{
			dl[PROXY_LINEDL] = id;
		}
		
		public void setPointDisplayListID(int id)	{
			dl[POINTDL] = id;
		}
		
		public int getPointDisplayListID() {
			return dl[POINTDL];
		}

		public void setDisplayListID(int type, int id)	{
			dl[type] = id;
		}
		
		public int getDisplayListID(int type) {
			return dl[type];
		}

		public boolean isDisplayListDirty(int type) {
			return realDLDirty[type];
		}
		
		public void setDisplayListDirty(int type, boolean b) {
			realDLDirty[type] = b;
		}
		
		// mark ALL display lists as dirty.
		public void setDisplayListsDirty() {
			for (int i = 0; i<NUM_DLISTS; ++i) realDLDirty[i] = true;
		}
		
		public boolean isInsideDisplayList() {
			return insideDisplayList;
		}
		
		public void setInsideDisplayList(boolean b) {
			insideDisplayList = b;
		}
		
		public boolean isValidDisplayList(int type) {
			return false;
		}
		/**
		 * 
		 */
		public void setChange() {
			frameCountAtLastChange = frameCount;
			changeCount++;
			setDisplayListsDirty();
		}
	}
	
	
	public double getFramerate()	{
		long totalTime = 0;
		for (int i = 0; i<20; ++i)	totalTime += history[i];
		framerate = 20*1000.0 / totalTime;
		return framerate;
	}

	private static  int bufsize = 4096;
	double[] pickPoint = new double[2];
	public PickPoint[] performPick(double[] p)	{
		if (CameraUtility.getCamera(theViewer).isStereo())		{
			System.out.println("Can't pick in stereo mode");
			return null;
		}
		pickPoint[0] = p[0];  pickPoint[1] = p[1];
		pickMode = true;
		theCanvas.display();	// this calls out display() method  directly
		return hits;
	}
	
	public class JOGLPeerNode	{
		String name;
		
		public String getName()	{
			return name;
		}
		
		public void setName(String n)	{
			name = n;
		}
	}

	/**
	 * 
	 */
	int geomDiff = 0;
	protected void updateGeometryHashtable() {
		Logger.getLogger("de.jreality.jogl").log(Level.FINEST, "Memory usage: "+getMemoryUsage());
		if (geometries == null) return;
		if (!geometryRemoved) return;
		final Hashtable newG = new Hashtable();
		SceneGraphVisitor cleanup = new SceneGraphVisitor()	{
			public void visit(SceneGraphComponent c) {
				if (c.getGeometry() != null) {
					Object wawa = c.getGeometry();
					Object peer = geometries.get(wawa);
					newG.put(wawa, peer);
				}
				c.childrenAccept(this);
			}
		};
		cleanup.visit(theRoot);
		geometryRemoved = false;
		//TODO dispose of the peer geomtry nodes which are no longer in the graph
		if (geometries.size() - newG.size() != geomDiff)	{
			System.out.println("Old, new hash size: "+geometries.size()+" "+newG.size());
			geomDiff = geometries.size() - newG.size() ;
		}
		return;
//		ArrayList removedGeoms = new ArrayList();
//		java.util.Enumeration foo = geometries.keys();
//		while (foo.hasMoreElements())	{
//			Object key = foo.nextElement();
//			if (!newG.containsKey(key)) removedGeoms.add(geometries.get(key));
//		}
		// switch over the hash table
//		synchronized(geometries)	{
//			geometries = newG;
//			//System.out.println("Removing "+removedGeoms.size()+" geometry peers");
//			Iterator iter = removedGeoms.iterator();
//			while (iter.hasNext())	{
//				Object el = iter.next();
//				if (el instanceof JOGLPeerGeometry)	{
//					((JOGLPeerGeometry)el).dispose();
//				}
//			}
//			geometryRemoved = false;
//		}
	}

	public static String getMemoryUsage() {
        Runtime r = Runtime.getRuntime();
        int block = 1024;
        return
                "(memory usage: " + ((r.totalMemory() / block) - (r.freeMemory() / block)) + " kB)";
    }
	
	public class JOGLPeerGeometry extends JOGLPeerNode implements GeometryListener	{
		Geometry originalGeometry;
		Geometry[] tubeGeometry, proxyPolygonGeometry;
		Vector proxyGeometry;
		DisplayListInfo dlInfo;
		IndexedFaceSet ifs;
		IndexedLineSet ils;
		PointSet ps;
		int refCount = 0;
		
		protected JOGLPeerGeometry(Geometry g)	{
			super();
			originalGeometry = g;
			name = "JOGLPeer:"+g.getName();
			dlInfo = new DisplayListInfo();
			ifs = null; ils = null; ps = null;
			if (g instanceof IndexedFaceSet) ifs = (IndexedFaceSet) g;
			if (g instanceof IndexedLineSet) ils = (IndexedLineSet) g;
			if (g instanceof PointSet) ps = (PointSet) g;
			originalGeometry.addGeometryListener(this);
		}
		
		public void dispose()		{
			refCount--;
			if (refCount < 0)	{
				System.out.println("Negative reference count!");
			}
			if (refCount == 0)	{
				//System.out.println("Geometry is no longer referenced");
				originalGeometry.removeGeometryListener(this);	
				geometries.remove(originalGeometry);
			}
		}
		
		public void geometryChanged(GeometryEvent ev) {
			//System.err.println("JOGLPeerGeometry: geometryChanged");
			//TODO make more differentiated response based on event ev
			final SceneGraphNode sg = (SceneGraphNode) ev.getSource();
			dlInfo.setChange();
		}
		
		public void geometryChanged(int type)	{
			if ((type & POINTS_CHANGED) != 0)	
				dlInfo.setDisplayListDirty(POINTDL, true);
			if ((type & LINES_CHANGED) != 0)	{
				dlInfo.setDisplayListDirty(LINEDL, true);
				dlInfo.setDisplayListDirty(PROXY_LINEDL, true);
			}
			if ((type & FACES_CHANGED) != 0)	{
				dlInfo.setDisplayListDirty(FLAT_POLYGONDL, true);
				dlInfo.setDisplayListDirty(SMOOTH_POLYGONDL, true);
			}
			//System.out.println("Setting display lists dirty with flag: "+type);
		}
		
		/**
		 * 
		 */
		public void render(JOGLPeerComponent jpc) {
			//System.out.println("In JOGLPeerGeometry render() for "+originalGeometry.getName());
			RenderingHintsShader renderingHints = jpc.renderingHints;
			DefaultGeometryShader geometryShader = jpc.geometryShader;
			renderingHints.render(globalHandle);
			if (originalGeometry instanceof Sphere)	{
//				IndexedFaceSet foo = getProxyFor((Sphere) originalGeometry);
//				ifs = foo;
//				ils = foo;
//				ps = foo;
				geometryShader.polygonShader.render(globalHandle);
				boolean ss = geometryShader.polygonShader.isSmoothShading();
				// TODO figure out which sphere proxy to use based on distance, LOD, etc
				
				int dlist = JOGLSphereHelper.getSphereDLists(2, globalHandle.theCanvas.getGL());
				if (pickMode) globalGL.glPushName(10000);
				globalGL.glCallList(dlist);
				if (pickMode) globalGL.glPopName();
				return;
			}
			if (geometryShader.isFaceDraw() && ifs != null)	{
				geometryShader.polygonShader.render(globalHandle);
				double alpha = geometryShader.polygonShader.getDiffuseColor().getAlpha()/255.0;
				boolean ss = geometryShader.polygonShader.isSmoothShading();
				int type = ss ? SMOOTH_POLYGONDL : FLAT_POLYGONDL;
				boolean proxy = geometryShader.polygonShader.providesProxyGeometry();
				if (dlInfo.isDisplayListDirty(FLAT_POLYGONDL) && dlInfo.isDisplayListDirty(SMOOTH_POLYGONDL)) proxyPolygonGeometry = null;
				if (proxy && (proxyPolygonGeometry == null))	{
					System.out.println("Asking for proxy geometry ");
					proxyPolygonGeometry = geometryShader.polygonShader.proxyGeometryFor(ils);
					System.out.println("proxy geometry of length "+proxyPolygonGeometry.length);
				}
				if (!processDisplayListState(type))		 // false return implies no display lists used
					if (proxy)	{
						int n = proxyPolygonGeometry.length;
						for (int x=0; x<n; ++x)	{
							JOGLRendererHelper.drawFaces((IndexedFaceSet) proxyPolygonGeometry[x], theCanvas.getGL(), pickMode, true, alpha);								
						}
					} else
						JOGLRendererHelper.drawFaces(ifs, theCanvas.getGL(),pickMode, ss, alpha);
				else // we are using display lists
					if (dlInfo.isInsideDisplayList())	{		// display list wasn't clean, so we have to regenerate it
						if (proxy)	{
							int n = proxyPolygonGeometry.length;
							for (int x=0; x<n; ++x)	{
								JOGLRendererHelper.drawFaces((IndexedFaceSet) proxyPolygonGeometry[x], theCanvas.getGL(), pickMode, true, alpha);								
							}
						} else
							JOGLRendererHelper.drawFaces(ifs, theCanvas.getGL(),pickMode, ss, alpha);
						globalGL.glEndList();	
						dlInfo.setDisplayListDirty(type, false);
						dlInfo.setInsideDisplayList(false);
					}
			}
			if (geometryShader.isEdgeDraw() && ils != null)	{
				geometryShader.lineShader.render(globalHandle);
				boolean tubes = geometryShader.lineShader.providesProxyGeometry();
				double alpha = geometryShader.lineShader.getDiffuseColor().getAlpha()/255.0;
				int type = tubes ? PROXY_LINEDL : LINEDL;
				if (tubes && (tubeGeometry == null || dlInfo.isDisplayListDirty(PROXY_LINEDL)))	{
					System.out.println("Recalculating tubes");
					tubeGeometry = geometryShader.lineShader.proxyGeometryFor(ils);
				}
				if (!processDisplayListState(type))		 // false return implies no display lists used
					if (tubes)	{
						int n = tubeGeometry.length;
						for (int x=0; x<n; ++x)	{
							JOGLRendererHelper.drawFaces((IndexedFaceSet) tubeGeometry[x], theCanvas.getGL(), pickMode, true, alpha);								
						}
					} else
						JOGLRendererHelper.drawLines(ils, theCanvas, alpha);			
				else // we are using display lists
					if (dlInfo.isInsideDisplayList())	{		// display list wasn't clean, so we have to regenerate it
						if (tubes)	{
							int n = tubeGeometry.length;
							for (int x=0; x<n; ++x)	{
								JOGLRendererHelper.drawFaces((IndexedFaceSet) tubeGeometry[x], theCanvas.getGL(), pickMode, true, alpha);								
							}
						} else
							JOGLRendererHelper.drawLines(ils, theCanvas, alpha);			
						globalGL.glEndList();	
						dlInfo.setDisplayListDirty(type, false);
						dlInfo.setInsideDisplayList(false);
					}
			}
			if (geometryShader.isVertexDraw() && ps != null)	{
				geometryShader.pointShader.render(globalHandle);
				double alpha = geometryShader.pointShader.getDiffuseColor().getAlpha()/255.0;
				int type = POINTDL;
				boolean spheres = geometryShader.pointShader.isSphereDraw();
				if (spheres || !processDisplayListState(type))		 // false return implies no display lists used
					JOGLRendererHelper.drawVertices(ps, globalHandle, geometryShader.pointShader.isSphereDraw(), geometryShader.pointShader.getPointRadius(), alpha);			
				else // we are using display lists
					if (dlInfo.isInsideDisplayList())	{		// display list wasn't clean, so we have to regenerate it
						JOGLRendererHelper.drawVertices(ps, globalHandle, geometryShader.pointShader.isSphereDraw(), geometryShader.pointShader.getPointRadius(), alpha);			
						globalGL.glEndList();	
						dlInfo.setDisplayListDirty(type, false);
						dlInfo.setInsideDisplayList(false);
					}
			}
		}

		private boolean processDisplayListState(int type)	{
			DisplayListInfo dl = dlInfo;
			
			//System.out.println("Valid display list for "+pc.getOriginalComponent().getName()+"is "+dl.isValidDisplayList());
			dl.setInsideDisplayList(false);
			if (!useDisplayLists || !dl.useDisplayList()) {
				return false;
			}
			if (!dl.isDisplayListDirty(type))	{
				//System.out.println("Using display list");
				globalGL.glCallList(dlInfo.getDisplayListID(type));
				return true;
			}
			
			if (dl.getDisplayListID(type) != -1) {
				globalGL.glDeleteLists(dl.getDisplayListID(type), 1);
				dl.setDisplayListID(type, -1);
			}
			int nextDL = globalGL.glGenLists(1);

			dl.setDisplayListID(type, nextDL);
			globalGL.glNewList(dl.getDisplayListID(type), GL.GL_COMPILE_AND_EXECUTE);
			//System.out.println("Beginning display list for "+originalGeometry.getName());
			dl.setInsideDisplayList(true);
			return true;
		}

		/**
		 * @param sphere
		 * @return
		 */
		private IndexedFaceSet getProxyFor(Sphere sphere) {
			return SphereHelper.spheres[2];
		}
	}
	
//	public void visit(Sphere sg) {
//		//Primitives.sharedIcosahedron.accept(this);
//		//double lod = renderingHints.getLevelOfDetail();
//		SceneGraphComponent helper = null;
//		System.out.println("Rendering sphere");
//		//if (lod == 0.0) 
//			
//		else	{
//			double area = GeometryUtility.getNDCArea(sg, gc.getObjectToNDC());
//			if (area < .01)	helper = SphereHelper.SPHERE_COARSE;
//			else if (area < .1 ) helper = SphereHelper.SPHERE_FINE;
//			else if (area < .5) helper = SphereHelper.SPHERE_FINER;
//			else helper = SphereHelper.SPHERE_FINEST;
//		}
//		SphereHelper.SPHERE_FINE.accept(this);
//		//Rectangle3D uc = Rectangle3D.unitCube;
//		//SphereHelper.SPHERE_BOUND.accept(this);
//	}
	
	public  JOGLPeerGeometry getJOGLPeerGeometryFor(Geometry g)	{
		JOGLPeerGeometry pg;
		synchronized(geometries)	{
			pg = (JOGLPeerGeometry) geometries.get(g);
			if (pg != null) return pg;
			pg = new JOGLPeerGeometry(g);
			geometries.put(g, pg);			
		}
		return pg;
	}
	
	// register for geometry change events
	static Hashtable goBetweenTable = new Hashtable();
	public  GoBetween goBetweenFor(SceneGraphComponent sgc)	{
		if (sgc == null) return null;
		GoBetween gb = null;
		Object foo = goBetweenTable.get(sgc);
		if (foo == null)	{
			gb = globalHandle.new GoBetween(sgc);
			goBetweenTable.put(sgc, gb);
			return gb;
		}
		return ((GoBetween) foo);
	}
	
	protected class GoBetween implements TransformationListener, AppearanceListener,
	SceneAncestorListener, SceneContainerListener, SceneTreeListener	{
		SceneGraphComponent originalComponent;
		ArrayList peers = new ArrayList();
		JOGLPeerGeometry peerGeometry;

		protected GoBetween(SceneGraphComponent sgc)	{
			super();
			originalComponent = sgc;
			if (originalComponent.getGeometry() != null)  {
				peerGeometry = getJOGLPeerGeometryFor(originalComponent.getGeometry());
				peerGeometry.refCount++;
			} else peerGeometry = null;
			originalComponent.addSceneAncestorListener(this);
			originalComponent.addSceneContainerListener(this);
			originalComponent.addSceneTreeListener(this);
			if (originalComponent.getAppearance() != null) originalComponent.getAppearance().addAppearanceListener(this);				
		}
		
		public void dispose()	{
			originalComponent.removeSceneAncestorListener(this);
			originalComponent.removeSceneContainerListener(this);
			originalComponent.removeSceneTreeListener(this);
			if (originalComponent.getAppearance() != null) originalComponent.getAppearance().removeAppearanceListener(this);
			if (peerGeometry != null)		peerGeometry.dispose();
		}
		
		
		public void addJOGLPeer(JOGLPeerComponent jpc)	{
			if (peers.contains(jpc)) return;
			peers.add(jpc);
		}
		
		public void removeJOGLPeer(JOGLPeerComponent jpc)	{
			if (!peers.contains(jpc)) return;
			peers.remove(jpc);
			if (peers.size() == 0)	{
				//System.out.println("GoBetween for "+originalComponent.getName()+" has no peers left");
				goBetweenTable.remove(originalComponent);
				dispose();
			}
		}
		
		public JOGLPeerGeometry getPeerGeometry() {
			return peerGeometry;
		}
		
		public void transformationMatrixChanged(TransformationEvent ev) {
			Iterator iter = peers.iterator();
			while (iter.hasNext())	{
				JOGLPeerComponent peer = (JOGLPeerComponent) iter.next();
				peer.transformationMatrixChanged(ev);
			}
		}
		
		public void appearanceChanged(AppearanceEvent ev) {
			Iterator iter = peers.iterator();
			String key = ev.getKey();
			int changed = 0;
			// TODO shaders should register keywords somehow and which geometries might be changed
			if (key.indexOf("implodeFactor") != -1 ) changed |= (FACES_CHANGED);
			else if (key.indexOf("transparency") != -1) changed |= (FACES_CHANGED);
			else if (key.indexOf("tubeRadius") != -1) changed |= (LINES_CHANGED);
			else if (key.indexOf("pointRadius") != -1) changed |= (POINTS_CHANGED);
//			peerGeometry.geometryChanged(changed);
			while (iter.hasNext())	{
				JOGLPeerComponent peer = (JOGLPeerComponent) iter.next();
				peer.appearanceChanged(ev);
				peer.propagateGeometryChanged(changed);
			}
			//System.out.println("setting display list dirty flag: "+changed);
		}
		
		public void ancestorAttached(SceneHierarchyEvent ev) {
			Iterator iter = peers.iterator();
			while (iter.hasNext())	{
				JOGLPeerComponent peer = (JOGLPeerComponent) iter.next();
				peer.ancestorAttached(ev);
			}
		}
		public void ancestorDetached(SceneHierarchyEvent ev) {
			Iterator iter = peers.iterator();
			while (iter.hasNext())	{
				JOGLPeerComponent peer = (JOGLPeerComponent) iter.next();
				peer.ancestorDetached(ev);
			}
		}
		public void childAdded(SceneContainerEvent ev) {
			if  (ev.getChildType() ==  SceneContainerEvent.CHILD_TYPE_GEOMETRY) {
					if (peerGeometry != null)	{
						peerGeometry.dispose();
						geometryRemoved = true;
						System.out.println("Warning: Adding geometry while old one still valid");
						peerGeometry=null;
					}
					if (originalComponent.getGeometry() != null)  {
						peerGeometry = getJOGLPeerGeometryFor(originalComponent.getGeometry());
						peerGeometry.refCount++;
					} 
					return;
			}
			Iterator iter = peers.iterator();
			while (iter.hasNext())	{
				JOGLPeerComponent peer = (JOGLPeerComponent) iter.next();
				peer.childAdded(ev);
			}
		}
		public void childRemoved(SceneContainerEvent ev) {
			if  (ev.getChildType() ==  SceneContainerEvent.CHILD_TYPE_GEOMETRY) {
				if (peerGeometry != null) {
					peerGeometry.dispose();		// really decreases reference count
					peerGeometry = null;
					geometryRemoved = true;
				}
				return;
			}
			Iterator iter = peers.iterator();
			while (iter.hasNext())	{
				JOGLPeerComponent peer = (JOGLPeerComponent) iter.next();
				peer.childRemoved(ev);
			}
		}
		
		public void childReplaced(SceneContainerEvent ev) {
			if  (ev.getChildType() ==  SceneContainerEvent.CHILD_TYPE_GEOMETRY) {
					if (peerGeometry != null && peerGeometry.originalGeometry == originalComponent.getGeometry()) return;		// no change, really
					if (peerGeometry != null) {
						peerGeometry.dispose();
						geometryRemoved=true;
						peerGeometry = null;
					}
					if (originalComponent.getGeometry() != null)  {
						peerGeometry = getJOGLPeerGeometryFor(originalComponent.getGeometry());
						peerGeometry.refCount++;
					} 
					return;
			}
			Iterator iter = peers.iterator();
			while (iter.hasNext())	{
				JOGLPeerComponent peer = (JOGLPeerComponent) iter.next();
				peer.childReplaced(ev);
			}
		}
		public void childAdded(SceneHierarchyEvent ev) {
			Iterator iter = peers.iterator();
			while (iter.hasNext())	{
				JOGLPeerComponent peer = (JOGLPeerComponent) iter.next();
				peer.childAdded(ev);
			}
		}
		public void childRemoved(SceneHierarchyEvent ev) {
			Iterator iter = peers.iterator();
			while (iter.hasNext())	{
				JOGLPeerComponent peer = (JOGLPeerComponent) iter.next();
				peer.childRemoved(ev);
			}
		}
		public void childReplaced(SceneHierarchyEvent ev) {
			Iterator iter = peers.iterator();
			while (iter.hasNext())	{
				JOGLPeerComponent peer = (JOGLPeerComponent) iter.next();
				peer.childReplaced(ev);
			}
		}
		
		public SceneGraphComponent getOriginalComponent() {
			return originalComponent;
		}
	}
	private class ConstructPeerGraphVisitor extends SceneGraphVisitor	{
		SceneGraphComponent myRoot;
		JOGLPeerComponent thePeerRoot, myParent;
		SceneGraphPath sgp;
		boolean topLevel = true;
		public ConstructPeerGraphVisitor(SceneGraphComponent r, JOGLPeerComponent p)	{
			super();
			myRoot = r;
			sgp = new SceneGraphPath();
			myParent = p;
			//System.out.println("Constructing peer");
			//thePeerRoot = p;
		}
		
		private ConstructPeerGraphVisitor(ConstructPeerGraphVisitor pv, JOGLPeerComponent p)	{
			super();
			sgp = (SceneGraphPath) pv.sgp.clone();
			myParent = p;
			topLevel = false;
		}
		
		public void visit(SceneGraphComponent c) {
			sgp.push(c);
			JOGLPeerComponent peer = new JOGLPeerComponent(sgp, myParent);
			// we don't add the top-level node here to its parent; 
			// that has to be done carefully using a childLock by the caller
			if (topLevel) thePeerRoot = peer;
			else if (myParent != null) {
				int n = myParent.children.size();
				myParent.children.add(peer);
				peer.childIndex = n;
			}
		  	c.childrenAccept(new ConstructPeerGraphVisitor(this, peer));
		  	//peer.setIndexOfChildren();
		  	sgp.pop();
		}

		public Object visit()	{
			visit(myRoot);
			return thePeerRoot;
		}
		
	}
	public class JOGLPeerComponent extends JOGLPeerNode implements TransformationListener, AppearanceListener,
		SceneAncestorListener, SceneContainerListener, SceneTreeListener {
		
		public int[] bindings = new int[2];
		//SceneGraphComponent originalComponent;
		EffectiveAppearance eAp;
		Vector children;
		JOGLPeerComponent parent;
		int childIndex;
		GoBetween goBetween;
		//JOGLPeerGeometry peerGeometry;
		
		Rectangle3D childrenBound;
		Rectangle2D ndcExtent;
		SceneGraphPath	pathToHere;
		boolean isReflection = false, cumulativeIsReflection = false;
		double determinant = 0.0;
		
		// for now, keep the primitive shading support
		RenderingHintsShader renderingHints;
		DefaultGeometryShader geometryShader;
		
		Object childLock = new Object();		
		
		boolean appearanceChanged,
			appearanceIsDirty,
			geometryIsDirty,
			boundIsDirty,
			object2WorldDirty;

		public JOGLPeerComponent(SceneGraphPath sgp, JOGLPeerComponent p)		{
			super();
			if (sgp == null || !(sgp.getLastElement() instanceof SceneGraphComponent))  {
				System.out.println("Invalid parameters to constructor JOGLPeerComponent");
			} else {
				pathToHere = sgp;
				goBetween = goBetweenFor(sgp.getLastComponent());
				goBetween.addJOGLPeer(this);
				name = "JOGLPeer:"+goBetween.getOriginalComponent().getName();
				appearanceChanged = false;
				appearanceIsDirty = true;
				geometryIsDirty = true;
				boundIsDirty = true;
				object2WorldDirty = true;
				children = new Vector();		// always have a child list, even if it's empty
				parent = p;
				updateTransformationInfo();
			}
		}
		
		public void dispose()	{
			//synchronized(childLock)	{
				int n = children.size();
				for (int i = n-1; i>=0; --i)	{
					JOGLPeerComponent child = (JOGLPeerComponent) children.get(i);
					child.dispose();
				}	
				goBetween.removeJOGLPeer(this);
			//}

		}
		public void render()		{
			//System.out.println("Rendering "+goBetween.getOriginalComponent().getName());
			if (!goBetween.getOriginalComponent().isVisible()) return;
						
			// the following looks very dangerous
			//goBetween.getOriginalComponent().preRender(gc);
			
			nodeCount++;
			currentPath.push(goBetween.getOriginalComponent());
			gc.setCurrentPath(currentPath);
			Transformation thisT = goBetween.getOriginalComponent().getTransformation();
			
			//System.out.println("In JOGLPeerComponent render() for "+goBetween.getOriginalComponent().getName());
			if (thisT != null)	{
				if (stackDepth <= MAX_STACK_DEPTH) {
					globalGL.glPushMatrix();
					globalGL.glMultTransposeMatrixd(thisT.getMatrix());
				}
				else globalGL.glLoadTransposeMatrixd(gc.getObjectToCamera());				
			}  
			// should depend on camera transformation ...
			if (parent != null) cumulativeIsReflection = (isReflection != parent.cumulativeIsReflection);
			else cumulativeIsReflection = (isReflection != globalIsReflection);
			globalGL.glFrontFace(cumulativeIsReflection ? GL.GL_CW : GL.GL_CCW);

			if (appearanceChanged)  	propagateAppearanceChanged();
			if (appearanceIsDirty)	updateAppearance();
			gc.setEffectiveAppearance(eAp);
			
			// render the geometry
			if (goBetween.getPeerGeometry() != null)	goBetween.getPeerGeometry().render(this);
			
			//synchronized(childLock)	{
				// render the children
				int n = children.size();
				for (int i = 0; i<n; ++i)	{		
					JOGLPeerComponent child = (JOGLPeerComponent) children.get(i);
					
					if (pickMode)	globalGL.glPushName(child.childIndex);
					child.render();
					if (pickMode)	globalGL.glPopName();
				}				
			//}
			
			if (thisT != null)	{
				if (stackDepth <= MAX_STACK_DEPTH) globalGL.glPopMatrix();
				stackDepth--;
			}			
			currentPath.pop();
		}
		
		public void setIndexOfChildren()	{
			synchronized(childLock){
				int n = goBetween.getOriginalComponent().getChildComponentCount();
				//synchronized(goBetween.getOriginalComponent().getChildLock()) {
					for (int i = 0; i<n; ++i)	{
						SceneGraphComponent sgc = goBetween.getOriginalComponent().getChildComponent(i);
						JOGLPeerComponent jpc = getPeerForChildComponent(sgc);
						if (jpc == null)	{
							System.out.println("No peer for sgc "+sgc.getName());
							jpc.childIndex = -1;
						} else jpc.childIndex = i;
					}									
				//}
			}
		}

		
		private void updateAppearance()	{
			if (parent == null)	{
				if (eAp == null) eAp = EffectiveAppearance.create();
				if (goBetween.getOriginalComponent().getAppearance() != null )	
					eAp = eAp.create(goBetween.getOriginalComponent().getAppearance());
				// TODO figure out why I put this here in the first place
				//else eAp = eAp.create(new Appearance());				
			} else {
				if ( parent.eAp == null)	{
					System.out.println("No effective appearance in parent "+parent.getName());
					return;
				}
				if (goBetween.getOriginalComponent().getAppearance() != null )	
					eAp = parent.eAp.create(goBetween.getOriginalComponent().getAppearance());
				else eAp = parent.eAp;				
			}
			geometryShader = DefaultGeometryShader.createFromEffectiveAppearance(eAp, "");
			//System.out.println("component "+goBetween.getOriginalComponent().getName()+" vertex draw is "+geometryShader.isVertexDraw());
			renderingHints = RenderingHintsShader.createFromEffectiveAppearance(eAp, "");
			appearanceIsDirty = false;
		}
		
		public void ancestorAttached(SceneHierarchyEvent ev) {
		}
		
		public void ancestorDetached(SceneHierarchyEvent ev) {
		}
		
		/**
		 * @param sgc
		 * @return
		 */
		private JOGLPeerComponent getPeerForChildComponent(SceneGraphComponent sgc) {
			
			int n = children.size();
			for (int i = 0; i<n; ++i)	{
				JOGLPeerComponent jpc = (JOGLPeerComponent) children.get(i);
				if ( jpc.goBetween.getOriginalComponent() == sgc) { // found!
					return jpc;
				}
			}
			return null;
		}

		public void childAdded(SceneContainerEvent ev) {
			//System.out.println("Container Child added to: "+goBetween.getOriginalComponent().getName());
			//System.out.println("Event is: "+ev.toString());
			switch (ev.getChildType() )	{
				case SceneContainerEvent.CHILD_TYPE_COMPONENT:
					SceneGraphComponent sgc = (SceneGraphComponent) ev.getNewChildElement();
					JOGLPeerComponent pc = globalHandle.constructPeerForSceneGraphComponent(sgc, this);
					synchronized(childLock)	{
				    //System.out.println("Before adding child count is "+children.size());
						children.add(pc);						
					  
					//System.out.println("After adding child count is "+children.size());
					}
					setIndexOfChildren();
					break;
//				case SceneContainerEvent.CHILD_TYPE_GEOMETRY:
//					if (peerGeometry != null)	{
//						peerGeometry.dispose();
//						geometryRemoved = true;
//						System.out.println("Warning: Adding geometry while old one still valid");
//						peerGeometry=null;
//					}
//					if (goBetween.getOriginalComponent().getGeometry() != null)  {
//						peerGeometry = getJOGLPeerGeometryFor(goBetween.getOriginalComponent().getGeometry());
//						peerGeometry.refCount++;
//					} 
//					break;
				case SceneContainerEvent.CHILD_TYPE_LIGHT:
					lightListDirty = true;
					break;
				case SceneContainerEvent.CHILD_TYPE_TRANSFORMATION:
					updateTransformationInfo();
					break;
				default:
					System.out.println("Taking no action for addition of child type "+ev.getChildType());
					break;
			}
		}
		
		public void childRemoved(SceneContainerEvent ev) {
			//System.out.println("Container Child removed from: "+goBetween.getOriginalComponent().getName());
			switch (ev.getChildType() )	{
				case SceneContainerEvent.CHILD_TYPE_COMPONENT:
					SceneGraphComponent sgc = (SceneGraphComponent) ev.getOldChildElement();
				    JOGLPeerComponent jpc = getPeerForChildComponent(sgc);
				    if (jpc == null) return;
				    //System.out.println("removing peer "+jpc.getName());
				    //System.out.println("Before removal child count is "+children.size());
					synchronized(childLock)	{
					    children.remove(jpc);						
					}
					//System.out.println("After removal child count is "+children.size());
				    jpc.dispose();		// there are no other references to this child
				    setIndexOfChildren();
					break;
					// geometry handled now by gobetween
//				case SceneContainerEvent.CHILD_TYPE_GEOMETRY:
//					if (peerGeometry != null) {
//						peerGeometry.dispose();		// really decreases reference count
//						peerGeometry = null;
//						geometryRemoved = true;
//					}
//					break;
				case SceneContainerEvent.CHILD_TYPE_LIGHT:
					lightListDirty = true;
					break;
				case SceneContainerEvent.CHILD_TYPE_TRANSFORMATION:
					updateTransformationInfo();
					break;			
				default:
					System.out.println("Taking no action for removal of child type "+ev.getChildType());
					break;
		}
		}
		
		public void childReplaced(SceneContainerEvent ev) {
			//System.out.println("Container Child replaced at: "+goBetween.getOriginalComponent().getName());
			switch(ev.getChildType())	{
				case SceneContainerEvent.CHILD_TYPE_APPEARANCE:
					appearanceChanged = true;
					break;
				case SceneContainerEvent.CHILD_TYPE_LIGHT:
					lightListDirty = true;
					break;
				case SceneContainerEvent.CHILD_TYPE_TRANSFORMATION:
					updateTransformationInfo();
					break;
				default:
					System.out.println("Taking no action for replacement of child type "+ev.getChildType());
					break;
			}
		}
		
		public void childAdded(SceneHierarchyEvent ev) {
			System.out.println("Hierarchy Child added");
		}
		
		public void childRemoved(SceneHierarchyEvent ev) {
			System.out.println("Hierarchy Child removed");
		}
		
		public void childReplaced(SceneHierarchyEvent ev) {
			System.out.println("Hierarchy Child replaced");
		}
		
		public void transformationMatrixChanged(TransformationEvent ev) {
			// TODO notify ancestors that their bounds are no longer valid
			updateTransformationInfo();
		}
		
		/**
		 * 
		 */
		private void updateTransformationInfo() {
			if (goBetween.getOriginalComponent().getTransformation() != null) {
				isReflection = goBetween.getOriginalComponent().getTransformation().getIsReflection();
			} else {
				determinant  = 0.0;
				isReflection = false;
			}
		}

		public void appearanceChanged(AppearanceEvent ev) {
			//System.out.println("Appearance change "+goBetween.getOriginalComponent().getName());
			appearanceChanged = true;
		}
		
		private void propagateAppearanceChanged()	{
			appearanceIsDirty = true;	
			int n = children.size();
			for (int i = 0; i<n; ++i)	{		
				JOGLPeerComponent child = (JOGLPeerComponent) children.get(i);
				child.propagateAppearanceChanged();
			}
			appearanceChanged = false;
		}
		
		/**
		 * @param changed
		 */
		public void propagateGeometryChanged(int changed) {
			if (goBetween != null && goBetween.getPeerGeometry() != null) goBetween.getPeerGeometry().geometryChanged(changed);
			synchronized(childLock)	{
				int n = children.size();
				for (int i = 0; i<n; ++i)	{		
					JOGLPeerComponent child = (JOGLPeerComponent) children.get(i);
					child.propagateGeometryChanged(changed);
				}				
			}
		}

		private void setDisplayListDirty(boolean b)	{
			propagateGeometryChanged(POINTS_CHANGED | LINES_CHANGED | FACES_CHANGED);
		}
		
		public SceneGraphComponent getOriginalComponent() {
			return goBetween.getOriginalComponent();
		}
	}
	/**
	 * @return
	 */
	public GLCanvas getCanvas()	{
		return theCanvas;
	}
	/**
	 * @param file
	 */
	File screenShotFile = null;
	public void saveScreenShot(File file)	{
		screenShot = true;
		screenShotFile = file;
		theCanvas.display();
		screenShot = false;
	}
}
