package de.jreality.writer;

/**
 * @author gonska
 * 
 */


/**TODO 
 * Labels
 */
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.WeakHashMap;

import de.jreality.math.FactoredMatrix;
import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.Pn;
import de.jreality.reader.vrml.VRMLHelper;
import de.jreality.scene.Appearance;
import de.jreality.scene.Camera;
import de.jreality.scene.Cylinder;
import de.jreality.scene.DirectionalLight;
import de.jreality.scene.Geometry;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.IndexedLineSet;
import de.jreality.scene.Light;
import de.jreality.scene.PointLight;
import de.jreality.scene.PointSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphNode;
import de.jreality.scene.SceneGraphVisitor;
import de.jreality.scene.Sphere;
import de.jreality.scene.SpotLight;
import de.jreality.scene.Transformation;
import de.jreality.scene.data.Attribute;
import de.jreality.shader.CommonAttributes;
import de.jreality.shader.DefaultGeometryShader;
import de.jreality.shader.DefaultLineShader;
import de.jreality.shader.DefaultPointShader;
import de.jreality.shader.DefaultPolygonShader;
import de.jreality.shader.DefaultTextShader;
import de.jreality.shader.EffectiveAppearance;
import de.jreality.shader.ImageData;
import de.jreality.shader.ShaderUtility;
import de.jreality.shader.Texture2D;
import de.jreality.util.ImageUtility;

// TODO: handle Tubes To Cylinders,Sheres To Spheres
public class WriterVRML 
{
	boolean useDefs = true;
	private VRMLWriterHelper wHelp= new VRMLWriterHelper();
	private DefaultGeometryShader dgs;
	private DefaultPolygonShader dps;
	private DefaultLineShader dls;
	private DefaultPointShader dvs;
	private DefaultTextShader dpts;
	private DefaultTextShader dlts;
	private DefaultTextShader dvts;
	private String fileStem = String.format("texture-%10d-", System.currentTimeMillis());
	private static final int PER_VERTEX=0,PER_PART=1,PER_FACE=2,OVERALL=3;
	private PrintWriter out=null;
	private static final String spacing="  ";// for outlay
	private static String hist="";// for outlay

	// -----------------constructor----------------------
	public WriterVRML(OutputStream outS) {	
		out=new PrintWriter( outS );
	}
	public WriterVRML(FileWriter outS) {
		out=new PrintWriter( outS );
	}
	public WriterVRML(PrintWriter outS) {
		out=outS;
	}
	public static void write(SceneGraphComponent sceneRoot, FileOutputStream stream)   {
		WriterVRML writer = new WriterVRML(stream);
		writer.write(sceneRoot);
	}

	String writePath = "";
	WeakHashMap<ImageData, String> textureMaps = new WeakHashMap<ImageData, String>();
	public void setWritePath(String path)	{
		writePath = path;
		if (!writePath.endsWith("/")) writePath = writePath + "/";
	}

	public void setWriteTextureFiles(boolean writeTextureFiles2) {
		writeTextureFiles = writeTextureFiles2;
	}
//	---------------------------------------

	public void write( SceneGraphNode sgn ){
		out.println("#VRML V1.0 ascii");
		// what should be defined
		if(useDefs){
			wHelp.inspect(sgn);
		}
		sgn.accept(new MyVisitor());
		out.flush();
	}
//	------------------ 
	private void updateShaders(EffectiveAppearance eap) {
		dgs = ShaderUtility.createDefaultGeometryShader(eap);

		if (dgs.getPointShader() instanceof DefaultPointShader){	
			dvs = (DefaultPointShader) dgs.getPointShader();
			if (dvs.getTextShader() instanceof DefaultTextShader){
				dvts=(DefaultTextShader)dvs.getTextShader();
			}
			else dvts=null;
		}
		else dvs = null;
		if (dgs.getLineShader() instanceof DefaultLineShader){ 
			dls = (DefaultLineShader) dgs.getLineShader();
			if (dls.getTextShader() instanceof DefaultTextShader){
				dlts=(DefaultTextShader)dls.getTextShader();
			}
			else dlts=null;
		}
		else dls = null;
		if (dgs.getPolygonShader() instanceof DefaultPolygonShader){
			dps = (DefaultPolygonShader) dgs.getPolygonShader();
			if (dps.getTextShader() instanceof DefaultTextShader)
				dpts=(DefaultTextShader) dps.getTextShader();
			else dpts=null;
		}
		else dps = null;


	}
//	---------------------
//	---------------------------- start writing --------------------
	private void writeGeoFaces(IndexedFaceSet f,String hist) {
		// write the coordinates:
		double[][] coords=VRMLWriterHelper.getDoubleDoubleVertexAttr(f, Attribute.COORDINATES);
		if(coords==null)return;
		writeCoordinates(coords,hist);
		// writes the Normals depending on smooth or flat shading:
		if(dps.getSmoothShading()){
			double[][] vNormals=VRMLWriterHelper.getDoubleDoubleVertexAttr(f, Attribute.NORMALS);
			if(vNormals!=null){
				writeNormalBinding(PER_VERTEX,hist);
				writeNormals(vNormals,hist);
			}	
		}
		else{
			double[][] fNormals=VRMLWriterHelper.getDoubleDoubleFaceAttr(f, Attribute.NORMALS);
			if(fNormals!=null){
				writeNormalBinding(PER_FACE,hist);
				writeNormals(fNormals,hist);
			}	
		}
		// writes Colors
		double[][] fColors=VRMLWriterHelper.getDoubleDoubleFaceAttr(f, Attribute.COLORS);
		if(fColors!=null){
			writeMaterialBinding(PER_FACE,hist);
			writeColors(fColors,hist);
		}
		else{
			double[][] vColors=VRMLWriterHelper.getDoubleDoubleVertexAttr(f, Attribute.COLORS);
			if(vColors!=null){
				writeMaterialBinding(PER_VERTEX,hist);
				writeColors(vColors,hist);
			}
		}

		/**	IndexedFaceSet {
		 * 	coordIndex         0  # MFLong indies
		 * 	materialIndex      -1 # MFLong egal
		 * 	normalIndex        -1 # MFLong egal
		 * 	textureCoordIndex  -1 # MFLong spaeter
		 * 	} */	

		out.println(hist+"IndexedFaceSet { # "+ f.getName());
		// writes the FaceIndices:
		int[][] indices=VRMLWriterHelper.getIntIntFaceAttr(f, Attribute.INDICES);
		if(indices!=null)		writeIndices(indices, hist+spacing);
		out.println(hist+"}");
		// write Labels:
		if(dpts.getShowLabels()){
			String[] labels=VRMLWriterHelper.getLabelPointAttr(f);
			if (labels!=null)				
				writeFaceLabels(coords,indices,labels,hist);	
		}
	}
	private void writeGeoLines(IndexedLineSet l,String hist) {
		double[][] lcoords=VRMLWriterHelper.getDoubleDoubleVertexAttr(l, Attribute.COORDINATES);
		int[][] lindices=		VRMLWriterHelper.getIntIntEdgeAttr(l, Attribute.INDICES);
		// write coords
		if(lcoords==null) return;
		writeCoordinates(lcoords,hist);
		// writes Edge Colors if given:
		// TODO
		// writes Colors
		double[][] lColors=VRMLWriterHelper.getDoubleDoubleEdgeAttr(l, Attribute.COLORS);
		if(lColors!=null){
			writeMaterialBinding(PER_PART,hist);
			writeColors(lColors,hist);
		}
		else{
			double[][] vColors=VRMLWriterHelper.getDoubleDoubleVertexAttr(l, Attribute.COLORS);
			if(vColors!=null){
				writeMaterialBinding(PER_VERTEX,hist);
				writeColors(vColors,hist);
			}
		}
		// write object
		/**		IndexedLineSet {
		 *		coordIndex         0  # ok
		 *		materialIndex      -1 # egal
		 *		normalIndex        -1 # egal
		 *		textureCoordIndex  -1 # egal
		 *		} */
		out.println(hist+"IndexedLineSet { # "+ l.getName());
		// writes the edgeIndices
		writeIndices(lindices, hist+spacing);
		out.println(hist+"}");
		// write labels
		if(dlts.getShowLabels()){
			String[] labels=VRMLWriterHelper.getLabelEdgeAttr(l);
			if (labels!=null)				
				writeEdgeLabels(lcoords,lindices,labels,hist);	
		}
	}
	private void writeGeoPoints(PointSet p,String hist) {
		// write coords
		double[][] coords=VRMLWriterHelper.getDoubleDoubleVertexAttr(p, Attribute.COORDINATES);
		if(coords==null)return;
		writeCoordinates(coords,hist);
		// writes Vertex Colors
		double[][] vColors=VRMLWriterHelper.getDoubleDoubleVertexAttr(p, Attribute.COLORS);
		if(vColors!=null){
			writeMaterialBinding(PER_VERTEX,hist);
			writeColors(vColors,hist);
		}
		// write object
		/**		PointSet {
		 *		startIndex  0 	# default ok
		 *		numPoints   -1    # ok
		 *		} */
		out.println(hist+"PointSet { # "+ p.getName());
		out.println(hist+ spacing + "numPoints "+ p.getNumPoints());
		out.println(hist+"}");
		// write labels
		if(dvts.getShowLabels()){
				String[] labels=VRMLWriterHelper.getLabelPointAttr(p);
				if (labels!=null)				
					writeLabelsAtPoints(coords, labels, hist);	
			}
	}
	private   void writeCoordinates(double[][] points,String hist) {
		out.println(hist+"Coordinate3 { point [");
		String hist2=hist+spacing;
		if (points[0].length == 4) points = Pn.dehomogenize(points, points);
		for(int i=0;i<points.length;i++){
			VRMLWriterHelper.writeDoubleArray(points[i],hist2,",",3,out);
		}
		out.println(hist+"]}");
	}	
	private   void writeNormals(double[][] normals,String hist) {
		out.println(hist+"Normal { vector  [");
		String hist2=hist+spacing;
		if (normals[0].length == 4) normals = Pn.dehomogenize(normals, normals);
		for(int i=0;i<normals.length;i++){
			VRMLWriterHelper.writeDoubleArray(normals[i],hist2,",",3,out);
		}
		out.println(hist+"]}");	
	}
	private   void writeColors(double[][] Colors,String hist) {
		out.println(hist+"Material { ");
		out.println(hist+spacing+"diffuseColor  [");
		String hist2=hist+spacing+spacing;
		for(int i=0;i<Colors.length;i++){
			VRMLWriterHelper.writeDoubleArray(Colors[i],hist2,",",3,out);
		}
		out.println(hist+"]}");
		// siehe transparency
		// material nur diffuse
	}	
	private   void writeMaterialBinding(int binding,String hist ){
		out.print(hist+"MaterialBinding { value ");
		if(binding==PER_VERTEX)
			out.println("PER_VERTEX }");
		else if(binding==PER_FACE)
			out.println("PER_FACE }");
		else if(binding==PER_PART)
			out.println("PER_PART }");
		else if(binding==OVERALL)
			out.println("OVERALL }");
		else out.println("DEFAULT }");
	} 
	private   void writeNormalBinding(int binding,String hist){
		out.print(hist+"NormalBinding { value ");
		if(binding==PER_VERTEX)
			out.println("PER_VERTEX }");
		else if(binding==PER_FACE)
			out.println("PER_FACE }");
		else out.println("DEFAULT }");
	} 
	private   void writeTexture(Texture2D tex,String hist) {
		/**		WRAP ENUM
		 *		REPEAT  Repeats texture outside 0-1 texture coordinate range
		 *		CLAMP   Clamps texture coordinates to lie within 0-1 range
		 *		FILE FORMAT/DEFAULTS
		 *		Texture2 {
		 *		filename    ""        # SFString egal
		 *		image       0 0 0     # SFImage
		 *		wrapS       REPEAT    # SFEnum later 
		 *		wrapT       REPEAT    # SFEnum later
		 *		}		*/
		String hist2=hist+spacing;
		out.println(hist+"Texture2 {");
		if (writeTextureFiles)	{
			String fileName = textureMaps.get(tex.getImage());
			if (fileName == null)	{
				fileName = fileStem+String.format("%04d", textureCount)+".png";
				String fullName = writePath+fileName;
				textureCount++;
				ImageUtility.writeBufferedImage( new File(fullName),(BufferedImage) tex.getImage().getImage());				
				textureMaps.put(tex.getImage(), fileName);
			}
			out.println(hist+"filename "+"\""+fileName+"\" ");
		} else 
			writeImage(tex,hist2);
		out.print(hist+"wrapS ");
		writeTexWrap(tex.getRepeatS());
		out.print(hist+"wrapT ");
		writeTexWrap(tex.getRepeatT());
		out.println(hist+"}");
		writeTexTrans(hist2,tex);

	}
	private   void writeTexWrap(int wrap) {
		switch (wrap) {
		case Texture2D.GL_CLAMP_TO_EDGE:
			System.out.println("texture wrap:only clamp & repeat are supported");
		case Texture2D.GL_CLAMP:
			out.println("CLAMP");				
			break;
		case Texture2D.GL_MIRRORED_REPEAT:
			System.out.println("texture wrap:only clamp & repeat are supported");
		case Texture2D.GL_REPEAT:
			out.println("REPEAT");				
			break;
		default:
		}
	}

	private   void writeFaceLabels(double[][]coords,int[][]indis,String[]labs,String hist) {
		double[][] centers= new double[indis.length][3];// coords for the labels
		double[] midpoint;
		int[] face;
		for (int i = 0; i < indis.length; i++) {// all faces
			face=indis[i];
			midpoint=new double[]{0,0,0};
			int j;
			for (j = 0; j < face.length; j++)// add vertices
				for (int k = 0; k < 3; k++)
					midpoint[k]+=coords[face[j]][k];
			for (int k = 0; k < 3; k++) // calc the mean
				centers[i][k]=midpoint[k]/j;
		}
		writeLabelsAtPoints(centers,labs,hist);
	}

	private   void writeEdgeLabels(double[][]coords,int[][]indis,String[]labs,String hist) {
		double[][] centers= new double[indis.length][3];// coords for the labels
		int[] firstEdge;
		for (int i = 0; i < indis.length; i++) {// all line-Objects
			firstEdge=indis[i];
			for (int k = 0; k < 3; k++) // calc the mean of the first segment
				centers[i][k]=coords[indis[i][0]][k]/2+coords[indis[i][1]][k]/2;
		}
		writeLabelsAtPoints(centers,labs,hist);
	}
	private   void writeLabelsAtPoints(double[][]centers,String[] labs,String hist) {
		/*AsciiText {
        	string         ""    # MFString
        	spacing        1     # SFFloat
        	justification  LEFT  # SFEnum
        	width          0     # MFFloat
   		}*/
		/*Translation {
        	translation  0 0 0    # SFVec3f
   		} */
		for (int i = 0; i < centers.length; i++) {
			out.println(hist+"Translation { translation ");
			VRMLWriterHelper.writeDoubleArray(centers[i], "", " }", 3,out);
			out.println(hist+"AsciiText { width 1 string \""+labs[i]+"\"}");
			out.println(hist+"Translation { translation ");
			VRMLWriterHelper.writeDoubleArray(new double[]{-centers[i][0],-centers[i][1],-centers[i][2]}, "", " }", 3,out);
		}

	}
	// --------------- helper Classes ---------------------
	
	static boolean writeTextureFiles = false;
	int textureCount = 0;
	private  void writeImage(Texture2D tex,String hist) {
		String hist2=hist+spacing;
		ImageData id=tex.getImage();
		byte[] data= id.getByteArray();
		int w=id.getWidth();
		int h=id.getHeight();
		int dim= data.length/(w*h);
		// write image
		out.print(hist+"image ");
		out.println(""+w+" "+h+" "+dim);
		for (int i = 0; i < w*h; i++) {
			int mergeVal=0;
			// calculate hexvalue from colors
			for (int k = 0; k < dim; k++) {
				int val=data[i*4+k];
				if (val<0)val=val+256;
				mergeVal*=256;
				mergeVal+=val;
			}
			out.println(hist2+"0x"+ Integer.toHexString(mergeVal).toUpperCase());
		}
	}
	private  void writeDoubleMatrix(double[] d,int width, int depth, String hist) {
		double[] n=new double[width];
		for (int i=0;i<depth;i++){
			System.arraycopy(d,i*width,n,0,4);
			VRMLWriterHelper.writeDoubleArray(n,hist,"",n.length,out);
		}
	}
	private  void writeInfoString(String info,String hist) {
		/*Info {
	          string  "<Undefined info>"      # SFString
	     }	*/
		out.println(hist+"Info { string \"" +info+ "\" }");	
	}
	private  void writeIndices(int[][] in,String hist) {
		out.println(hist+"coordIndex ["); 		
		for (int i=0;i<in.length;i++){
			int le=in[i].length;
			out.print(hist+spacing);
			for(int j=0;j<le;j++){
				out.print(""+in[i][j]+", ");
			}
			out.println(" -1, ");
		}
		out.println(hist+"]");
	}
	private  void writeMaterial(Color a,Color d,Color s,double t,String hist){
		String hist2=hist+spacing;
		out.println(hist+"Material { ");
		// have to set all colors at once, otherwise unset colors return to defaultValue
		if(a!=null)	out.println(hist2+"ambientColor " + VRMLWriterHelper.ColorToString(a) );
		if(d!=null)	out.println(hist2+"diffuseColor " + VRMLWriterHelper.ColorToString(d) );
		if(s!=null)	out.println(hist2+"specularColor " + VRMLWriterHelper.ColorToString(s) );
		if(t!=0){ out.println(hist2+"transparency " + t );	}
		out.println(hist+"}");
	}
	private  void writeTexTrans(String hist,Texture2D tex){
		String hist2=hist+spacing;
		/**		Texture2Transform {
		 *		translation  0 0      # SFVec2f
		 *		rotation     0        # SFFloat
		 *		scaleFactor  1 1      # SFVec2f
		 *		center       0 0      # SFVec2f
		 *		}		*/
		Matrix mat= tex.getTextureMatrix();
		FactoredMatrix matrix= new FactoredMatrix(mat.getArray());
		double[] trans=matrix.getTranslation();
		double ang=matrix.getRotationAngle();
		double[] rotA=matrix.getRotationAxis();
		double[] scale = matrix.getStretch();

		out.println(hist+"Texture2Transform {");
		out.println(hist2+"translation  "+trans[0]/trans[3]+" "+trans[1]/trans[3]);
		out.println(hist2+"rotation  "+ang);
		out.println(hist2+"scaleFactor "+scale[0]+" "+scale[1]);
		out.println(hist2+"center 0 0");
		out.println(hist+"}");
	}
	private  void writeTexCoords(double[][] texCoords,String hist) {
		/**		TextureCoordinate2 {
		 *		point  0 0    # MFVec2f
		 *		} */
		String hist2=hist+spacing;
		out.println(hist+"TextureCoordinate2 { point [");
		for(int i=0;i<texCoords.length;i++)
			VRMLWriterHelper.writeDoubleArray(texCoords[i],hist2,",",2,out);
		out.println(hist+"]}");
	}	
	
	// -------------- Visitor -------------------
	private class MyVisitor extends SceneGraphVisitor{
		private EffectiveAppearance effApp= EffectiveAppearance.create();
		private boolean firstTime=true;
		
		public MyVisitor() {}
		public MyVisitor(MyVisitor mv) {
			effApp=mv.effApp;
		}		
		public void visit(SceneGraphComponent c) {
			if(!c.isVisible())return;
			out.println(""+hist+"Separator { # "+c.getName());
			String oldhist= hist;
			hist=hist+spacing;
			if(firstTime){
//				 write INFO STRING
				if (c.getAppearance()!=null){
					Object obj = c.getAppearance().getAttribute(
							CommonAttributes.INFO_STRING, String.class);
					if (obj instanceof String && obj!=null){
						String info= (String)obj;
						writeInfoString(info, hist+spacing);
					}
				}
				// defaults:
				/*		ShapeHints {
			          vertexOrdering  UNKNOWN_ORDERING      # SFEnum
			          shapeType       UNKNOWN_SHAPE_TYPE    # SFEnum
			          faceType        CONVEX                # SFEnum
			          creaseAngle     0.5                   # SFFloat
			     }*/
				out.println(""+hist+"ShapeHints { # some default Parameters ");
				out.println(""+hist+spacing+"vertexOrdering  UNKNOWN_ORDERING");
				out.println(""+hist+spacing+"shapeType       UNKNOWN_SHAPE_TYPE");
				out.println(""+hist+spacing+"faceType        CONVEX");
				out.println(""+hist+"}");
				
				firstTime=false;
			}
			c.childrenAccept(new MyVisitor(this));			
			super.visit(c);
			hist=oldhist;
			out.println(""+hist+"}");
		}
		public void visit(Appearance a) {
			effApp=effApp.create(a);
			super.visit(a);
		}
		// ----- geometrys -----
		public void visit(Sphere s) {// finished
			super.visit(s);
			if ( !dgs.getShowFaces())	return;
			// first write the appearance colors and texture outside the DEF/USE
			writeMaterialBinding(OVERALL,hist);
			Color amb = dps.getAmbientColor();
			Color spec= dps.getSpecularColor();
			Color diff= dps.getDiffuseColor();
			double tra= dps.getTransparency();
			if (tra!=0|diff!=null|spec!=null|amb!=null){
				writeMaterial(amb,diff,spec,tra,hist);
			}
			/**	Sphere {
			 *    radius  1     # SFFloat
			 *	}		*/
			out.println(hist+"Sphere { radius  1}");
		}
		public void visit(Cylinder c) {//finished
			super.visit(c);			
			if ( !dgs.getShowFaces())	return;
			// first write the appearance colors and texture outside the DEF/USE
			writeMaterialBinding(OVERALL,hist);
			Color amb = dps.getAmbientColor();
			Color spec= dps.getSpecularColor();
			Color diff= dps.getDiffuseColor();
			double tra= dps.getTransparency();
			if (tra!=0|diff!=null|spec!=null|amb!=null){
				writeMaterial(amb,diff,spec,tra,hist);
			}
			/**	PARTS
			 *     SIDES   The cylindrical part
			 *     TOP     The top circular face
			 *     BOTTOM  The bottom circular face
			 *     ALL     All parts
			 *FILE FORMAT/DEFAULTS
			 *     Cylinder {
			 *          parts   ALL   # SFBitMask
			 *          radius  1     # SFFloat
			 *          height  2     # SFFloat
			 *     }		*/
			double[] r=MatrixBuilder.euclidean().rotateFromTo(new double[]{0,1,0}, new double[]{1,0,0}).getArray();
			out.println(hist+" Separator { # Cylinder");
			String oldHist= hist;		hist=hist+spacing;
			out.println(hist+"MatrixTransform { matrix");
			writeDoubleMatrix(r,4,4,hist+spacing);
			out.println(hist+"}");
			out.print(hist+"Cylinder { ");
			out.print("parts SIDES ");
			out.print("radius  1 ");
			out.print("height  1 ");
			out.println("}");
			hist=oldHist;
			out.println(""+hist+"} ");
		}
		public void visit(Geometry g) {//finished
			updateShaders(effApp);
			super.visit(g);
		}
		public void visit(PointSet p) {
			super.visit(p);
			if ( !dvs.getSpheresDraw() || !dgs.getShowPoints()|| p.getNumPoints()==0) return;
			// first write the appearance colors and texture outside the DEF/USE
			writeMaterialBinding(OVERALL,hist);
			Color diff=dvs.getDiffuseColor();
			if (diff!=null){	writeMaterial(null,diff,null,0,hist);	}
			//	check if allready defined
			if(useDefs){
				if (wHelp.isDefinedPointSet(p)){
					out.println(""+hist+"USE "+ VRMLWriterHelper.str( p.hashCode()+"POINT")+" ");
					return;
				}
			}
			if (useDefs && wHelp.isMultipleUsedPointSet(p)){
				out.print(""+hist+"DEF "+VRMLWriterHelper.str(p.hashCode()+"POINT")+" ");
				wHelp.setDefinedPointSet(p);
				}
			else out.print(""+hist);
			out.println(" Separator { ");
			writeGeoPoints( p,hist+spacing);
			out.println(""+hist+"} ");					
		}
		public void visit(IndexedLineSet g) {
			super.visit(g);
			if ( !dls.getTubeDraw() || !dgs.getShowLines() || g.getNumEdges()==0) return;
			// first write the appearance colors and texture outside the DEF/USE
			writeMaterialBinding(OVERALL,hist);
			Color diff=dls.getDiffuseColor();
			if (diff!=null){	writeMaterial(null,diff,null,0,hist);	}
			//	check if allready defined
			if(useDefs){
				if (wHelp.isDefinedLineSet(g)){
					out.println(""+hist+"USE "+ VRMLWriterHelper.str( g.hashCode()+"LINE")+" ");
					return;
				}
			}
			if (useDefs && wHelp.isMultipleUsedLineSet(g)){
				out.print(""+hist+"DEF "+VRMLWriterHelper.str(g.hashCode()+"LINE")+" ");
				wHelp.setDefinedLineSet(g);
				}
			else out.print(""+hist);
			out.println(" Separator { ");
				writeGeoLines( g,hist+spacing);
			out.println(""+hist+"} ");		
		}
		public void visit(IndexedFaceSet g) {
			super.visit(g);
			if ( !dgs.getShowFaces()|| g.getNumFaces()==0)	return;
			// first write the appearance colors and texture outside the DEF/USE
			writeMaterialBinding(OVERALL,hist);
			Color amb = dps.getAmbientColor();
			Color spec= dps.getSpecularColor();
			Color diff= dps.getDiffuseColor();
			double tra= dps.getTransparency();
			if (tra!=0|diff!=null|spec!=null|amb!=null){
				writeMaterial(amb,diff,spec,tra,hist);
			}
			try {
				// check texture
				double[][] texCoords=VRMLWriterHelper.getDoubleDoubleVertexAttr(g, Attribute.TEXTURE_COORDINATES);
				if (texCoords==null)
					throw new IOException("missing texture component");			
				Texture2D tex=dps.getTexture2d();
				if (tex==null) throw new IOException("missing texture component");
				Matrix mat= tex.getTextureMatrix();
				if (mat==null) throw new IOException("missing texture component");
				ImageData id=tex.getImage();
				if (id==null) throw new IOException("missing texture component");
				// write texture:
				writeTexCoords(texCoords,hist);	
				writeTexture(tex,hist);
			} catch (IOException e) {
				// falls es nur nicht genug componenten fuer die Textur gibt 
				// werfe keinen Fehler
			}
			//			check if allready defined
			if(useDefs){
				if (wHelp.isDefinedFaceSet(g)){
					out.println(""+hist+"USE "+ VRMLWriterHelper.str( g.hashCode()+"POLYGON")+" ");
					return;
				}
			}
			if (useDefs && wHelp.isMultipleUsedFaceSet(g)){
				out.print(""+hist+"DEF "+VRMLWriterHelper.str(g.hashCode()+"POLYGON")+" ");
				wHelp.setDefinedFaceSet(g);
				}
			else out.print(""+hist);
			out.println(" Separator { ");
			writeGeoFaces( g,hist+spacing);
			out.println(""+hist+"} ");
		}
		// ---- Lights ----
		public void visit(Light l) {//finished
			super.visit(l);
		}
		public void visit(DirectionalLight l) {
			/*	DirectionalLight {
		          on         TRUE       # SFBool
		          intensity  1          # SFFloat
		          color      1 1 1      # SFColor
		          direction  0 0 -1     # SFVec3f
		     } */
			double di=l.getIntensity();
			double[] dc=VRMLWriterHelper.colorToDoubleArray(l.getColor());
			out.println(hist+"DirectionalLight { # "+ l.getName());
			String oldHist= hist;		hist=hist+spacing;
			out.println(hist + "intensity " +di);
			out.print(hist + "color " );
			VRMLWriterHelper.writeDoubleArray(dc, "", "", 3,out);
			out.println(hist + "direction  0 0 1");
			hist=oldHist;
			out.println(hist+"}");
			super.visit(l);
		}
		public void visit(PointLight l) {
			/* PointLight {
		          on         TRUE       # SFBool
		          intensity  1          # SFFloat
		          color      1 1 1      # SFColor
		          location   0 0 1      # SFVec3f
		     }  */
			double di=l.getIntensity();
			double[] dc=VRMLWriterHelper.colorToDoubleArray(l.getColor());
			out.println(hist+"PointLight { # "+ l.getName());
			String oldHist= hist;		hist=hist+spacing;
			out.println(hist + "intensity " +di);
			out.print(hist + "color " );
			VRMLWriterHelper.writeDoubleArray(dc, "", "", 3,out);
			out.println(hist + "location   0 0 0");
			hist=oldHist;
			out.println(hist+"}");			
			super.visit(l);
		}
		public void visit(SpotLight li) {
			/*  SpotLight {
	          on           TRUE     # SFBool
	          intensity    1        # SFFloat
	          color        1 1 1    # SFVec3f
	          location     0 0 1    # SFVec3f
	          direction    0 0 -1   # SFVec3f
	          dropOffRate  0        # SFFloat
	          cutOffAngle  0.785398 # SFFloat
	     }  */
			double di=li.getIntensity();
			double[] dc=VRMLWriterHelper.colorToDoubleArray(li.getColor());
			out.println(hist+"SpotLight { # "+ li.getName());
			String oldHist= hist;		hist=hist+spacing;
			out.println(hist + "intensity " +di);
			out.print(hist + "color " );
			VRMLWriterHelper.writeDoubleArray(dc, "", "", 3,out);
			out.println(hist + "location 0 0 0 ");
			out.println(hist + "dropOffRate 0" );
			out.println(hist + "direction  0 0 1");
			out.println(hist + "cutOffAngle "+li.getConeAngle() );
			hist=oldHist;
			out.println(hist+"}");
			super.visit(li);
		}
		// ----------- cam ------------
		public void visit(Camera c) {
			if (c.isPerspective()){
				//TODO: orientation
				out.println(hist+"PerspectiveCamera { ");
				String oldHist= hist;		hist=hist+spacing;
				out.println(hist+"focalDistance "+c.getFocalLength());
				double hAngle=c.getFieldOfView()*Math.PI /180;
				out.println(hist+"heightAngle "+hAngle);
				hist=oldHist;
				out.println(hist+"}");
			}
			else {System.out.println("WriterVRML.writeCam(not completely implemented)");}
			super.visit(c);
		}
		// ---------- trafo ---------
		public void visit(Transformation t) {
			out.println(hist+"MatrixTransform { matrix");
			writeDoubleMatrix(t.getMatrix(),4,4,hist+spacing);
			out.println(hist+"}");
			super.visit(t);
		}
	}		
}
