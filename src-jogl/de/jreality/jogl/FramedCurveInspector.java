/*
 * Created on Aug 25, 2004
 *
 */
package de.jreality.jogl;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JTextField;
import javax.swing.Timer;

import de.jreality.jogl.pick.JOGLPickAction;
import de.jreality.scene.Camera;
import de.jreality.scene.Graphics3D;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphPath;
import de.jreality.scene.FactoredTransformation;
import de.jreality.scene.pick.PickPoint;
import de.jreality.util.CameraUtility;
import de.jreality.util.Rn;
import de.jreality.util.SceneGraphUtilities;

/**
 * @author gunn
 *
 */
public class FramedCurveInspector extends JFrame {
	
	FramedCurve theCurve;
	protected Camera fciCamera;
	boolean showCurve;
	boolean moveWorld = false;		// default: means move the camera
	boolean loopPlay = false;
	FramedCurve.ControlPoint currentPoint = null;
	protected Viewer parent;
	SceneGraphPath camPath, origCamPath;
	SceneGraphComponent myCameraNode, worldNode, target;
	int inspectedPoint = 0;
	//double currentTime = 0.0;
	MyKeyListener cdkl;
	final Component me;
	int totalTicks;
	static int framesPerSecond = 30;
	static double ycoord = 0.0;
	double totalSeconds;
	int timerInterval;
	final JTextField keyNo;
	final JTextField timeValue;
	final JTextField durationValue;
	final JTextField playtime	;
	final JTextField filename;
	final JTextField playBackF;
	JButton playButton;
	PlayAction playAction;
	protected Timer cameraMove, cameraEnd;
	protected double playbackFactor = 1.0;
	SceneGraphComponent worldSGC = null;
	
	
	static String resourceDir = null;
	public static double globalPlaybackFactor = 1;
	static {
		String foo = System.getProperty("framedCurveInspector.resourceDir");
		if (foo != null) resourceDir = foo;
		//System.out.println("FCI resource dir is: "+resourceDir);
		foo = System.getProperty("framedCurveInspector.frameRate");
		if (foo != null) framesPerSecond = Integer.parseInt(foo);
		foo = System.getProperty("framedCurveInspector.ycoord");
		if (foo != null) ycoord = Double.parseDouble(foo);
		foo = System.getProperty("mars.playSpeed");
		if (foo != null) globalPlaybackFactor = Double.parseDouble(foo);
	}

	public FramedCurveInspector(Viewer v)	{
		this(v, null);
	}
	
	public FramedCurveInspector(Viewer v, SceneGraphComponent world)	{
		super();
		setWorldNode(world);
		//worldNode = world;
		
		me = this;
		parent = v;
		cameraEnd = null;
		
		cameraMove = new Timer(timerInterval, new ActionListener() {
			public void actionPerformed(ActionEvent e)	{
				updateTime();
			}
		});
		
		setFrameRate(framesPerSecond);
		
		//setInspectedCurve(fc);

		cdkl = new MyKeyListener();
		//parent.getViewingComponent().addKeyListener(cdkl);
		//frame = new JFrame();
		
		SceneGraphComponent root = parent.getSceneRoot();
		camPath = new SceneGraphPath();
		camPath.push(root);
		
		target = myCameraNode = SceneGraphUtilities.createFullSceneGraphComponent("FramedCurveInspectorCamera");
		setWorldNode(null);
		fciCamera = new Camera();	
		Camera oldCam = CameraUtility.getCamera(parent);
		// TODO implement clone() for Camera
		fciCamera.setNear(oldCam.getNear());
		fciCamera.setFar(oldCam.getFar());
		//System.out.println("Camera is: "+fciCamera);
		myCameraNode.setCamera(fciCamera);
		myCameraNode.getTransformation().setMatrix( CameraUtility.getCameraNode(parent).getTransformation().getMatrix());
		root.addChild(myCameraNode);
		camPath.push(myCameraNode);
		camPath.push(myCameraNode.getCamera());
		
		// initialize with a default framed curve
		theCurve = new FramedCurve();
		double[][] mp =  new double[1][];
		mp[0] = myCameraNode.getTransformation().getMatrix();
		theCurve.setControlPoints(mp);
		
		int panelwidth = 600;
		int panelheight = 40;
		Box col = Box.createVerticalBox();
		Box panel = Box.createHorizontalBox();
		panel.setSize(panelwidth, panelheight);
		JMenuBar mb = new JMenuBar();
		JMenu testM = new JMenu("File");
		JMenuItem jcb = new JMenuItem("New");
		testM.add(jcb);
		jcb.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)	{
				FramedCurve fc = new FramedCurve();
				double[][] mp =  new double[1][];
				mp[0] = myCameraNode.getTransformation().getMatrix();
				fc.setControlPoints(mp);
				setInspectedCurve(fc );
				parent.render();
				parent.getViewingComponent().requestFocus();
			}
		});
		jcb = new JMenuItem("Open...");
		testM.add(jcb);
		jcb.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)	{
				FramedCurve fc = openFramedCurve();
				if (fc == null) return;
				setInspectedCurve(fc );
				parent.render();
				parent.getViewingComponent().requestFocus();
			}
		});
		jcb = new JMenuItem("Close");
		testM.add(jcb);
		jcb.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)	{
				System.out.println("Not yet implemented");
				parent.render();
				parent.getViewingComponent().requestFocus();
			}
		});
		jcb = new JMenuItem("Save");
		testM.add(jcb);
		jcb.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)	{
				theCurve.writeToFile(theCurve.getSourceFile());
				parent.render();
				parent.getViewingComponent().requestFocus();
			}
		});
		jcb = new JMenuItem("Save As...");
		testM.add(jcb);
		jcb.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)	{
				saveFramedCurve(theCurve);
				updateFilename();
			}
		});
		mb.add(testM);
		// the inspected key frame
		testM = new JMenu("View");
		final JCheckBoxMenuItem scp = new JCheckBoxMenuItem("Show camera path");
		scp.setSelected(showCurve);
		testM.add(scp);
		scp.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)	{
				showCurve = !showCurve;
				handleShowCurve();
			}
		});
		final JCheckBoxMenuItem scw = new JCheckBoxMenuItem("Move world");
		scw.setSelected(moveWorld);
		testM.add(scw);
		scw.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)	{
				moveWorld = !moveWorld;
				handleMoveWorld();
			}
		});
		mb.add(testM);
		
		panel.add(mb);
		panel.add(Box.createHorizontalGlue());
		col.add(Box.createVerticalStrut(10));
		col.add(panel);
		col.add(Box.createVerticalStrut(10));
		
		panel = Box.createHorizontalBox();
		panel.setSize(panelwidth, panelheight);
		JLabel label = new JLabel("Filename:", JLabel.LEFT);
		panel.add(label);
		filename = new JTextField();
		filename.setText("Untitled");
		filename.setEnabled(false);
		panel.add(filename);
		col.add(Box.createVerticalGlue());
		col.add(panel);
		
		panel = Box.createHorizontalBox();
		label = new JLabel("Key frame", JLabel.LEFT);
		panel.add(label);
		label = new JLabel("#=", JLabel.RIGHT);
		panel.add(label);
		keyNo = new JTextField(4);
		keyNo.setText("0");
		keyNo.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)	{
				int value = Integer.parseInt(keyNo.getText());
				if (value < 0 || value >= theCurve.getNumberControlPoints()) {
					keyNo.setText(Integer.toString(inspectedPoint)); 
					return; 
				}
				inspectedPoint = value;
				inspectKeyFrame();
			}
		});
		panel.add(keyNo);

		panel.add(new JButton(new RetreatAction()));
		panel.add(new JButton(new AdvanceAction()));
		
		label = new JLabel("t=", JLabel.RIGHT);
		panel.add(label);
		
		timeValue = new JTextField( 8);
		timeValue.setText("0");
		timeValue.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)	{
				double value = Double.parseDouble(timeValue.getText());
				int previous = inspectedPoint - 1;
				if (previous < 0) previous = 0;
				double thisTime = theCurve.getControlPoint(inspectedPoint).getTime();
				double prevTime = theCurve.getControlPoint(previous).getTime();
				int next = inspectedPoint + 1;
				double nextTime = 0.0;
				boolean atEnd = false;
				if (next >= theCurve.getNumberControlPoints()) atEnd = true;
				else nextTime = theCurve.getControlPoint(next).getTime();
				if (value < prevTime || (!atEnd && value > nextTime))	{
					System.out.println("Invalid time");
					timeValue.setText(doubleToString(thisTime));
					return;
				}
				theCurve.getControlPoint(inspectedPoint).setTime(value);
				theCurve.setOutOfDate(true);
			}
		});
		panel.add(timeValue);
		
		label = new JLabel("dt=", JLabel.RIGHT);
		panel.add(label);
		
		durationValue = new JTextField( 8);
		durationValue.setText("0");
		durationValue.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)	{
				double value = Double.parseDouble(durationValue.getText());
				int next = inspectedPoint + 1;
				if (next >= theCurve.getNumberControlPoints()) return;
				int n = theCurve.getNumberControlPoints();
				double thisTime = theCurve.getControlPoint(inspectedPoint).getTime();
				double nextTime = theCurve.getControlPoint(next).getTime();
				double dt = value - (nextTime - thisTime);
				for (int i = next; i< n; ++i)	{
					double foo = theCurve.getControlPoint(i).getTime();
					theCurve.getControlPoint(i).setTime(foo + dt);
				}
				theCurve.setOutOfDate(true);
			}
		});
		panel.add(durationValue);
		
		panel.add(new JButton(new SaveKeyAction()));
		panel.add(new JButton(new InsertKeyAction()));
		panel.add(new JButton(new DeleteKeyAction()));
		
		//getContentPane().add(panel, BorderLayout.NORTH);
		col.add(Box.createVerticalStrut(10));
		col.add(panel);		
		
		panel = Box.createHorizontalBox();
		panel.setSize(panelwidth, panelheight);
		label = new JLabel("Playback", JLabel.LEFT);
		panel.add(label);
		
		playAction = new PlayAction();
		playButton = new JButton( playAction);
		panel.add(playButton);
		
		label = new JLabel("t=", JLabel.RIGHT);
		panel.add(label);

		playtime = new JTextField( 8);
		playtime.setText("0");
		playtime.setEnabled(false);
		panel.add(playtime);
		col.add(Box.createVerticalStrut(10));
		col.add(panel);

		getContentPane().add(col, BorderLayout.CENTER);
		
		label = new JLabel("x=", JLabel.RIGHT);
		panel.add(label);

		playBackF = new JTextField(4);
		setPlaybackFactor(globalPlaybackFactor);
		playBackF.setText(Double.toString(playbackFactor));
		playBackF.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)	{
				double value = Double.parseDouble(playBackF.getText());
				if (value < .1 || value >= 250) {
					playBackF.setText(doubleToString(playbackFactor)); 
					return; 
				}
				playbackFactor = value;
			}
		});
		panel.add(playBackF);
		setSize(600,200);
		setLocation(200,600);
		
	}
	
	/**
	 * 
	 */
	protected void handleMoveWorld() {
		target.getTransformation().resetMatrix();
		if (moveWorld) setWorldNode(worldNode);
		else setWorldNode(null);
		System.out.println("move world is: "+moveWorld);
		updateCameraPosition();
	}

	/**
	 * 
	 */
	protected void updateFilename() {
		if (filename != null && theCurve != null)	{
			if (theCurve.getSourceFile() != null) {
				System.out.println("filename is"+theCurve.getSourceFile().toString());
				filename.setText(theCurve.getSourceFile().getName());
				resourceDir = theCurve.getSourceFile().getAbsolutePath();
			}
			else filename.setText("Untitled");		
		}
	}

	/**
	 * @param fc
	 */
	public void setInspectedCurve(FramedCurve fc) {
		// calculate time-related constants
		if (showCurve)	{
			parent.removeAuxiliaryComponent(theCurve);
		}
		theCurve = fc;
		handleShowCurve();
		inspectedPoint = 0;
		inspectKeyFrame();
		tick = 0;
		totalSeconds = fc.getTmax() - fc.getTmin();
		totalTicks = (int) totalSeconds * framesPerSecond;
		updateFilename();
	}
	
	/**
	 * 
	 */
	private void setFrameRate(int f) {
		framesPerSecond = f;
		timerInterval = 1000/framesPerSecond;
		cameraMove.setDelay(timerInterval);
	}

	private String doubleToString(double dd)	{
		return Double.toString(  (double) (   (Math.floor(1000*dd))  * 0.001d)    )   ;
	}

	/**
	 * Update key frame AND render it
	 */
	private void inspectKeyFrame() {
		updateKeyFrame();
		time = currentPoint.getTime();
		tick = tickFromTime(currentPoint.getTime());
		writeTarget(currentPoint.tt.getMatrix());
		parent.render();
	}

	/**
	 * 
	 */
	private void writeTarget(double[] m) {
		//System.out.println("Setting time to: "+currentPoint.getTime());
		if (moveWorld)	target.getTransformation().setMatrix(Rn.inverse(null,m));
		else 		target.getTransformation().setMatrix(m);
	}

	/**
	 * @return
	 */
	private double[] readTarget() {
		if (moveWorld) return Rn.inverse(null, target.getTransformation().getMatrix());
		return target.getTransformation().getMatrix();
	}
	
	/**
	 * Update the inspector but don't render it
	 */
	private void updateKeyFrame() {
		if (inspectedPoint<0) inspectedPoint = 0;
		if (inspectedPoint>theCurve.getNumberControlPoints()-1) inspectedPoint = theCurve.getNumberControlPoints()-1;
		keyNo.setText(Integer.toString(inspectedPoint));
		timeValue.setText(doubleToString(theCurve.getControlPoint(inspectedPoint).getTime()));
		int next = inspectedPoint+1;
		if (next >= theCurve.getNumberControlPoints()) {
			durationValue.setEnabled(false);
		} else {
			durationValue.setEnabled(true);
			double t0 = theCurve.getControlPoint(inspectedPoint).getTime();
			double t1 = theCurve.getControlPoint(next).getTime();
			durationValue.setText(doubleToString(t1 - t0));
		}
		currentPoint = theCurve.getControlPoint(inspectedPoint);
	}

	public void beginInspection()	{
		parent.getViewingComponent().addKeyListener(cdkl);
		origCamPath = parent.getCameraPath();
		parent.setCameraPath(camPath);
		//System.out.println("Camera is: "+CameraUtility.getCamera(parent));
		setVisible(true);
		repaint();
		inspectKeyFrame();
	}
	
	public void endInspection()	{
		parent.setCameraPath(origCamPath);
		//System.out.println("Camera is: "+fciCamera);
		setVisible(false);
		repaint();		
		parent.getViewingComponent().removeKeyListener(cdkl);
	}
	
	class SaveKeyAction extends AbstractAction		{
		
		SaveKeyAction()	{
			super();
			putValue(AbstractAction.NAME, "Save");
		}
		
		public void actionPerformed(ActionEvent e) {
			currentPoint.tt.setMatrix(readTarget());
			theCurve.setOutOfDate(true);
		}

}

	class DeleteKeyAction extends AbstractAction		{
		
		DeleteKeyAction()	{
			super();
			putValue(AbstractAction.NAME, "Delete");
		}
		
		public void actionPerformed(ActionEvent e) {
			theCurve.deleteControlPoint(inspectedPoint);
			inspectKeyFrame();
		}
		
	}

	class InsertKeyAction extends AbstractAction		{
		
		InsertKeyAction()	{
			super();
			putValue(AbstractAction.NAME, "Insert");
		}
		
		public void actionPerformed(ActionEvent e) {
			//int which = theCurve.getSegmentAtTime(time);
			if (inspectedPoint == theCurve.getNumberControlPoints()-1) return;
			double tm = (theCurve.getControlPoint(inspectedPoint).getTime() + theCurve.getControlPoint(inspectedPoint+1).getTime())*.5;
			FactoredTransformation tt = new FactoredTransformation();
			theCurve.getValueAtTime(tm, tt );
			FramedCurve.ControlPoint ncp = new FramedCurve.ControlPoint(tt, tm);
			theCurve.addControlPoint(ncp);
			inspectedPoint++;
			inspectKeyFrame();
		}
		
	}

	class AdvanceAction extends AbstractAction		{
		
		AdvanceAction()	{
			super();
			putValue(AbstractAction.NAME, ">");
		}
		
		public void actionPerformed(ActionEvent e) {
			inspectedPoint++;
			inspectedPoint = inspectedPoint % theCurve.getNumberControlPoints();
			inspectKeyFrame();
		}
		
	}

	class RetreatAction extends AbstractAction		{
		
		RetreatAction()	{
			super();
			putValue(AbstractAction.NAME, "<");
		}
		
		public void actionPerformed(ActionEvent e) {
			inspectedPoint--;
			if (inspectedPoint < 0) inspectedPoint += theCurve.getNumberControlPoints();;
			inspectKeyFrame();
		}
		
	}

	class SaveAllAction extends AbstractAction		{
		Component parent;
		SaveAllAction()	{
			super();
			putValue(AbstractAction.NAME, "Save As...");
		}
		
		public void actionPerformed(ActionEvent e) {
			saveFramedCurve(theCurve);
		}

	}

	/**
	 * @return
	 */
	protected FramedCurve openFramedCurve() {
		JFileChooser fc = new JFileChooser(resourceDir);
		//System.out.println("FCI resource dir is: "+resourceDir);
		int result = fc.showOpenDialog(this);
		FramedCurve aCurve = null;
		if (result == JFileChooser.APPROVE_OPTION)	{
			File file = fc.getSelectedFile();
			aCurve = FramedCurve.readFromFile(file);
		} else {
			System.out.println("Unable to open file");
		}
		return aCurve;
	}

	/**
	 * 
	 */
	private void saveFramedCurve(FramedCurve theCurve) {
		JFileChooser fc = new JFileChooser(resourceDir);
		int result = fc.showSaveDialog(this);
		if (result == JFileChooser.APPROVE_OPTION)	{
			File file = fc.getSelectedFile();
			theCurve.writeToFile(file);
			theCurve.setSourceFile(file);
		}
	}
	
	boolean isPaused = true;
	class PlayAction extends AbstractAction		{
		PlayAction()	{
			super();
			putValue(AbstractAction.NAME, "Play");
		}
		
		public void actionPerformed(ActionEvent e) {
			togglePlay();
		}
	}
	
	private double tickFromTime(double t)	{
		return totalTicks * (t - theCurve.getTmin())/(theCurve.getTmax() - theCurve.getTmin());
	}
	
	double tick = 0;
	FactoredTransformation tt = new FactoredTransformation();
	int count = 0;
	protected double time;
	public void updateTime()	{
		// time is a parameter between 0 and 1
		time = (tick % totalTicks)/((double) totalTicks);
		double factor = theCurve.getTmax() - theCurve.getTmin();
		time = theCurve.getTmin() + time * factor;
		updateCameraPosition();
		tick += playbackFactor;
		if (tick >= totalTicks) {
			tick=0;
			if (!loopPlay)	{
				cameraMove.stop();
				// TODO Remove this hack!
				if (cameraEnd != null) cameraEnd.start();
				playAction.putValue(AbstractAction.NAME, "Play");
			}
		}
	}
	
	public void updateCameraPosition()		{
		int which = theCurve.getSegmentAtTime(time);
		if (which != inspectedPoint) {
			inspectedPoint = which;
			updateKeyFrame();
		}
		theCurve.getValueAtTime(time, tt);
		playtime.setText(doubleToString(time));
		writeTarget(tt.getMatrix());
		parent.render();
		count++;
	}
	
	public void updateData()		{
		// for subclasses to implement
		System.out.println("Updating data");
	}

	/**
	 * 
	 */
	private void handleShowCurve() {
		if (showCurve) {
			parent.addAuxiliaryComponent(theCurve);
			//System.out.println("Removing frame curve. Index is: "+world.indexOf(frameCurve));
		}
		else {
			parent.removeAuxiliaryComponent(theCurve);
			//System.out.println("Adding frame curve. Index is: "+world.indexOf(frameCurve));
		}
		parent.render();
		parent.getViewingComponent().requestFocus();
	}

	/**
	 * 
	 */
	public void togglePlay() {
		isPaused = !isPaused;
		updatePlayState();
	}

	/**
	 * 
	 */
	private void updatePlayState() {
		if (isPaused) {
			cameraMove.stop();
			playAction.putValue(AbstractAction.NAME, "Play");
		} else	{
			totalSeconds = theCurve.getTmax() - theCurve.getTmin();
			totalTicks = (int) totalSeconds * framesPerSecond;

			cameraMove.start();
			playAction.putValue(AbstractAction.NAME, "Pause");
		}
		parent.render();
	}

	public boolean isLoopPlay() {
		return loopPlay;
	}
	public void setLoopPlay(boolean loopPlay) {
		this.loopPlay = loopPlay;
	}

	public double[] guessCameraFocusOnPath(FramedCurve fc, Viewer v)	{
		if (fc == null || v == null) return null;
		JOGLPickAction pickAction = new JOGLPickAction(v);
		double[] ndc = {0.0, ycoord};
		pickAction.setPickPoint(ndc);
		int n = fc.getNumberControlPoints();
		double[] focus = new double[n];
		SceneGraphComponent camNode = CameraUtility.getCameraNode(v);
		Graphics3D gc = new Graphics3D(v);
		StringBuffer sb = new StringBuffer();
		sb.append("focus\n");
		for (int i =0; i<n; ++i)	{
			FramedCurve.ControlPoint cp = fc.getControlPoint(i);
			time = cp.getTime();
			updateCameraPosition();
			List picks = (List) pickAction.visit();			
			if (picks!= null && (picks.size() > 0))	{
				PickPoint pp = (PickPoint) picks.get(0);
				gc.setCurrentPath(pp.getPickPath());
				double[] camPt = Rn.matrixTimesVector(null, gc.getObjectToCamera(), pp.getPointObject());
				focus[i] = Math.abs(camPt[2]);
				sb.append(time+" "+focus[i]*3390.0+"\n");
			}  
			else 
				sb.append(time+" 3390.0\n");
		}
		System.out.println(sb.toString());
		return focus;
	}
	/**
	 * @author Charles Gunn
	 *
	 */
	private class MyKeyListener extends KeyAdapter {
		private boolean showHelp = false;
		
		HelpOverlay helpOverlay;	
		long beginCurveTime = 0;
		boolean active = false;
		double startTime;
		/**
		 * 
		 */
		MyKeyListener() {
			super();
			
			//helpOverlay = new HelpOverlay(parent);
//			helpOverlay.registerKeyStroke(KeyStroke.getKeyStroke(KeyEvent.VK_1,0), "Encompass");
//			if ((theViewer.getViewingComponent() instanceof GLCanvas))
//				((GLDrawable) theViewer.getViewingComponent()).addGLEventListener(helpOverlay);

		}
		public void keyPressed(KeyEvent e)	{
				switch(e.getKeyCode())	{
					
				case KeyEvent.VK_0:
					tick = 0;
				    // revert to original speed
				    setPlaybackFactor(globalPlaybackFactor);
					isPaused = true;
					updatePlayState();
					if (cameraEnd != null) cameraEnd.stop();
					updateTime();
					System.out.println("Enter shift-P to resume playing");
					break;
					
				case KeyEvent.VK_6:
					if (e.isShiftDown()) playbackFactor /= 1.2;
					else playbackFactor *= 1.2;
					setPlaybackFactor(playbackFactor);
					System.out.println("playbackFactor is "+playbackFactor);
					break;
	
				case KeyEvent.VK_H:
					System.out.println("	0:  reset to beginning and pause.");
					System.out.println("shift-M:	generate camera focus animation parameters for current path (to System.out)");
					System.out.println("shift-P: toggle play/pause");
					System.out.println("shift-U: update global anim data file");
					System.out.println("period:  append key-frame");
					System.out.println("shift-period: append key-frame and end");
					System.out.println("	6:  increase speed");
					System.out.println("shift-6:  decrease speed");
					break;
					
				case KeyEvent.VK_M:
					if (e.isShiftDown()) guessCameraFocusOnPath(theCurve, parent);
					break;
					
				case KeyEvent.VK_G:
					if (e.isShiftDown()) System.gc();
					System.out.println("Garbage collecting");
					break;
					
//				case KeyEvent.VK_L:		// double-code the L key to force textures and do gc
//					System.gc();
//					System.out.println("Garbage collecting");
//					break;
//					
				case KeyEvent.VK_P:
					if (e.isShiftDown()) togglePlay();
					break;
					
				case KeyEvent.VK_U:
					if (e.isShiftDown()) updateData();
					break;
					
				case KeyEvent.VK_PERIOD:
						// we add the new control points to the end of the inspected curve
						// TODO figure out how to insert the control points.
						if (!active) {
							beginCurveTime = e.getWhen();
							startTime = theCurve.tmax;
							active = true;
							System.out.println("Key frame saving activated");
							return;
						}
					System.out.println("time is: "+e.getWhen());
					System.out.println("Camera node is: "+Rn.matrixToString(myCameraNode.getTransformation().getMatrix()));
					
						FactoredTransformation tt = new FactoredTransformation(parent.getSignature());
						//TODO apply inverse of objectToWorld transform here
						tt.setMatrix(readTarget());
						double t = (e.getWhen() - beginCurveTime)/1000.0 + startTime;
						theCurve.addControlPoint(new FramedCurve.ControlPoint(tt,t));
						// save the curve
						if (e.isShiftDown())	{
							//saveFramedCurve(theCurve);
							//updateFilename();
							active = false;
							System.out.println("Key frame saving de-activated");
						}
						break;
				}
			}
		

	}

	public double getPlaybackFactor() {
		return playbackFactor;
	}
	public void setPlaybackFactor(double playbackFactor) {
		this.playbackFactor = playbackFactor;
		playBackF.setText(Double.toString(playbackFactor));

	}
	public SceneGraphComponent getWorldSGC() {
		return worldSGC;
	}
	public void setWorldSGC(SceneGraphComponent worldSGC) {
		this.worldSGC = worldSGC;
	}
	/**
	 * @return Returns the worldNode.
	 */
	public SceneGraphComponent getWorldNode() {
		return worldNode;
	}
	/**
	 * @param worldNode The worldNode to set.
	 */
	public void setWorldNode(SceneGraphComponent wn) {
		//worldNode = wn;
		if (wn != null) {
			worldNode = wn;
			moveWorld = true;
			target = worldNode;
		}
		else {
			moveWorld = false;
			target = myCameraNode;
		}
		System.out.println("setWorldNode: "+moveWorld);
		// TODO if this is a switch, clean up the old target
	}
}
