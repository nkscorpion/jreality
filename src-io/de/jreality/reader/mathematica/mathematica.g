//**************************************************
// * Mathematica Parser
// */


header {
/**
* this code is generated by ANTLR from the 'mathematica.g'-file
* @author Bernd Gonska
* @version 1.0
*/
package de.jreality.reader.mathematica;
import java.awt.*;
import java.util.*;
import java.util.List;
import de.jreality.geometry.*;
import de.jreality.math.*;
import de.jreality.scene.data.*;
import de.jreality.scene.*;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.shader.*;
import de.jreality.util.LoggingSystem;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
}

class MathematicaParser extends Parser;
options {
	k = 2;							// two token lookahead
}

{
/**
* wandelt Graphic3D- Objekte die aus Mathematica mittels 
* 			<Graphics3D -Object> >> <Pfad>\ <fileName.m>
* herausgeschrieben wurden in einen SceneGraphen von JReality um.
*
* Die Daten haben im file folgende Form(sind uebrigens direkter Mathematica-Text):
* Graphics3D[ {} , {} ]
* in der ersten Klammer steht ein "{..}"-Klammer Baum von gemischten Listen aus 
* graphischen Objekten und graphischen Directiven( wie Farben ect.)
* Bsp: Graphics3D[ 		{o1,o2,d1,{d2,o3,{o4},d3,o5}} 			,{} ]   
*					(d1-d3 Directiven , o1-o5 graphische Objekte)
* in der 2. Klammer steht eine Liste von Optionen die die ganze Scene beinflussen.
* Bsp: Graphics3D[ 		{o1,o2,d1,{d2,o3,{o4},d3,o5}} 			,{Op1,Opt2,..} ]
* 			Von dieser Beschreibung sind manche Dinge Optional !
*
* Vorgehen: 
* erste Klammer:
*		Klammern werden zu SceneGraphComponents die den Inhalt als Unterbaeume tragen.
* 		die graphischen Objekte werden je zu einer SceneGr.C. mit entsprechender Geometrie
*			manche Objekte werden hierbei zusammengefast*
*		die graphischen Directiven werden in der jeweils betroffenen Geometrie gesetzt(Farben)
*			oder als Appearance in den zur Geom. gehoerenden Knoten gesetzt.(Punktgroesse ect.)
*					manche Directiven werden ueberlesen!!!
* zweite Klammer:
*		die meissten Optionen werden ueberlesen!!!
*		Ansonsten werden sie den Root Knoten beeinflussen.
*
* Polygone die in einer Liste bis auf Flaechen-Farb-Direktiven untereinander stehen,
*	 	werden zu einer indexedFaceSet
*   Linien die in einer Liste bis auf einfache-Farb-Direktiven untereinander stehen,
*	 	werden zu einer indexedLineSet
*   Punkte die in einer Liste bis auf einfache-Farb-Direktiven untereinander stehen,
*	 	werden zu einem PointSet
* 	 TexteMarken werden zu gelabelten EINELEMENTIGEN Pointsets.(sie werden nicht verbunden)
*
*   desweiteren werden auf Wunsch doppelte Punkte aus Line- und Face- Sets aussortiert
* 		das ermoeglicht u.a. smoothshading bei Flaechen aus getrennt angegebenen Polygonen(das ist ueblich)
*
*   Farben werden je nach Bedarf als Farbliste in Line-, Point-, und Face- Sets eingebunden.
*   	Vorzugsweise aber als Farbe in der Appearance des Unterknotens.
*
* Internes:
* ueberall die aktuelle Appearance, FlaechenFarbe(fC), und Punkt/LinienFarbe(plC) durchreichen
*
* TODO:
* alle Optionen sind noch unbehandelt (siehe dort: "optionPrimitives")
* Standard Lichter und Camera werden nicht uebernommen 
* 		(der Scenegraph hat keine Lichter oder Camera)
* ein paar Directiven werden auch ignoriert (siehe dort: "directive")
* Auch wenn Appearances wie Directiven fuer ganze gruppen gelten koennen,
*		werden sie erst im Knoten der Geometrie eingesetzt.


* Problem: manchmal werden Coordinaten so ausgerechnet das sie noch einen 
*	 Imaginaerteil haben ( meist = 0) der noch dahinter steht
*	Loesung :ein + ...*I ignorieren!
*			   Doubles werden ja immer durch "," getrennt! Also geht das!
*			   sollte jemand komplexe Zahlen lesen wollen
*				oder doubles ohne Komma parsen wollen 
*				so erstellt man dann noch eine extra RegelMethode
*/

	// this is what is returned from the parsing process
	private SceneGraphComponent root = new SceneGraphComponent();	
	
	private SceneGraphComponent current = root;		// aktuell zu erweiternder Knoten 
	private Logger log = LoggingSystem.getLogger(MathematicaParser.class);
//	private CoordinateSystemFactory box;
	private Appearance startApp =new Appearance();
	private Color plCDefault= new Color(127,127,127);	// default- Punkt und Linienfarbe
	private Color fCDefault = new Color(255,255,255);	// default- Flaechenfarbe
	
	private boolean optGeo=false;
	public void setOptimizeGeometry(boolean flag){optGeo=flag;}
	
//	private Object defaultEdgeForm = Color.black;
	private Object defaultEdgeForm = new Double(1); 

// ---------------------------- total Sizes of Scene --------------------------
	private double[][] borderValue= new double [3][]; // maximale Werte der Scenenkoordinaten in x-,y- und z-Richtung
	private boolean gotFirstPoint =false;
	private void resetBorder (double [] v){	  // erweitert den borderValue moeglicherweise durch diesen Punkt
		if (gotFirstPoint){ // set Borders for viewed size
			for (int i=0;i<3;i++){
				if (v[i] < borderValue[0][i])
					borderValue[0][i] = v[i];
				if (borderValue[1][i] < v[i])
					borderValue[1][i] = v[i];
			}
		}
		else{
			borderValue[0]=v;
			borderValue[1]=v;
			gotFirstPoint=true;		
		}
	}

// -------------------------------- default Lights ----------------------------
	public static SceneGraphComponent getDefaultLightNode (){
		return	MathematicaHelper.getDefaultLightNode();
	} 


/**
* konstructs a parser who can translate a
* mathematica-file to the corresponding SceneGraph
* @param    see superclass
* example: MathematicaParser p=
*	    new MathematicaParser(new MathematicaLexer(
*	     new FileReader(new File("file.m"))));
*/
}


// ------------------------------------------------------------------------------
// -------------------------------- Parser --------------------------------------
// ------------------------------------------------------------------------------
/**
* starts the parsing Process
* @param none sourcefile set by creating the object
* @returns SceneGraphComponent root of generated scene
*/
start returns [SceneGraphComponent r=new SceneGraphComponent()]
{
	root.setName("Mathematica");
	log.setLevel(Level.FINE);	
}
	:(  graphics3D
	  | OPEN_BRACE listOfGraphics CLOSE_BRACE)
		{
		r = root;
		if(r==null)r= new SceneGraphComponent();
		}
	;

private
listOfGraphics
	:(	( graphics3D
		 |OPEN_BRACE
			{						
			// neuen Knoten erstellen der die Listenelemente enthaelt, 
			SceneGraphComponent newPart = new SceneGraphComponent();
			newPart.setName("Graphic");
			SceneGraphComponent oldPart = current;
			current.addChild(newPart);
			current=newPart;
			}		 
		 	listOfGraphics 
		 	CLOSE_BRACE
			{current=oldPart;}		 
		 )
	 	(COLON listOfGraphics)*
	 )?
	;

private
graphics3D
	:"Graphics3D"
	  OPEN_BRACKET  
	  	( 	  
		  	(OPEN_BRACE
				(objectList[startApp,defaultEdgeForm])?	// eine Abfolge graphischer Objecte
		    CLOSE_BRACE)		    	
	  		| faceThing[startApp,defaultEdgeForm]	// ein Flaechen-beeinflussendes Object
	  		| plThing[startApp]  					// ein Punkt/Linien-beeinflussendes Object
	  		| appThing[startApp]					// ein App.-beeinflussendes Object
	  	)

//	  	(COLON optionen)? // Optionen werden komplett ignoriert
	  	(
	  		COLON egal
	  		{log.fine("options are not supported");}
	  	)?     // Optionen werden komplett ignoriert
	  CLOSE_BRACKET 
	; 
		
	  

// ---------------------------------- 3D Objects ---------------------------------------------

private
list[Appearance app, Object edgeF]
{Appearance app2=MathematicaHelper.copyApp(app);}
	// eine Klammer mit mglw. einer Abfolge von 3d-Objekten
	:	OPEN_BRACE						
			{						
			// neuen Knoten erstellen der die Listenelemente haelt, 
			SceneGraphComponent newPart = new SceneGraphComponent();
			newPart.setName("Object");
//			newPart.setAppearance(app2);
			SceneGraphComponent oldPart = current;
			current.addChild(newPart);
			current=newPart;
			}
	        (objectList[app2, edgeF])?	// das innere der Klammer einhaengen
	    CLOSE_BRACE
			{current=oldPart;}
	;
	
private
objectList [Appearance app, Object edgeF]
// abarbeiten einer Abfolge von 3d-Objecten(und Directiven)
{Appearance app2=MathematicaHelper.copyApp(app);}
	:(
		  list[app2 ,edgeF]		// Listen koennen Listen enthalten
		| edgeF=edgeFormThing
		| app2=faceThing[app2,edgeF]	// FlaechenElemente
		| app2=plThing[app2]	// Punkt/Linien Elemente
		| app2=appThing [app2]		// Directiven die Appearance beeinflussen sollen
	 )
	 ( COLON 
		(	
			  list[app2,edgeF]
		 	| edgeF=edgeFormThing
			| app2 =faceThing[app2,edgeF]
			| app2=plThing[app2]
			| app2=appThing[app2]
	 	)
	 )*
	;	
	
private
faceThing [Appearance app, Object edgeF] returns[Appearance app2]
{app2=MathematicaHelper.copyApp(app);}
	:	cuboid[app2,edgeF]					// Wuerfel 
	|	app2=polygonBlock[app2,edgeF]		// Abfolge von Polygonen (IndexedFaceSet)
	|	app2=faceColor[app2]				// Farbe die Flaechen beeinflusst
	;


private
plThing [Appearance app] returns[Appearance app2]
{
 app2=MathematicaHelper.copyApp(app);
 Color c= null;
}
	:	c= color	{app2=MathematicaHelper.setPLColor(app2,c);}	// Farbe fuer folgende Punkte, Linien und Texte
	|	app2= lineBlock [app2]			// Abfolge von Linien (IndexedLineSet)
	|	app2= pointBlock [app2]			// Abfolge von Punkten (PointSet)
	|	text [app2]					// Text an einem Punkt im Raum (einelementiges labeld PointSet)
	;
	
private
appThing [Appearance appOld] returns [ Appearance app]
{app=MathematicaHelper.copyApp(appOld);}
	:	app=directiveBlock[app]			// Abfolge von graphischen Direktiven (aendert eine Appearance)
	;
	
// ----------------------------- Graphic Primitives ------------------------------------------------------------
private 
cuboid [Appearance app,Object edgeF]
	// ein achsenparalleler Wuerfel, gegeben durch Zentrum(Kantenlaenge 1) oder zusaetslich durch leangen
	:"Cuboid"
	 OPEN_BRACKET 
			{double[] v2=new double [3]; 
			v2[0]=v2[1]=v2[2]=1;
			double[] v=new double[3];
			}
			v=vektor ( COLON v2=vektordata )? 
	 CLOSE_BRACKET 
			{
			// realisiert durch gestreckten kantenlosen Einheitswuerfel
			 SceneGraphComponent geo=new SceneGraphComponent();
			 current.addChild(geo);
			 geo.setGeometry(Primitives.cube());
	 		 geo.setName("Cuboid");
	 		 Appearance cubicApp =MathematicaHelper.copyApp(app);
	 		 if (edgeF!=null){
	 		 	//cubicApp.setAttribute(CommonAttributes.EDGE_DRAW, true);
			 	//cubicApp.setAttribute(CommonAttributes.TUBES_DRAW, true);
				cubicApp.setAttribute(CommonAttributes.TUBE_RADIUS,1/150);
				cubicApp.setAttribute(CommonAttributes.LINE_WIDTH,1);	 	
				if (edgeF instanceof Color){
			 	  cubicApp.setAttribute(CommonAttributes.LINE_SHADER+"."+
				 	CommonAttributes.POLYGON_SHADER+"."+
				 	CommonAttributes.DIFFUSE_COLOR, (Color)edgeF);
			 	  cubicApp.setAttribute(CommonAttributes.LINE_SHADER+"."+
				 	CommonAttributes.DIFFUSE_COLOR, (Color)edgeF);				 	
				 }
			 }
	 		 else{
		 		 cubicApp.setAttribute(CommonAttributes.EDGE_DRAW, false);
				 //cubicApp.setAttribute(CommonAttributes.TUBES_DRAW, false);
			 }
			 geo.setAppearance(cubicApp);
			 MatrixBuilder.euclidean().scale(v2[0],v2[1],v2[2])
			    .translate(v[0],v[1],v[2]).assignTo(geo);
 			}
 	;
 	
private
text [Appearance app]
{double[] v=new double[3]; String t;}
// ein Stueck Text im Raum 
	:"Text"		OPEN_BRACKET 
					s:STRING COLON v=vektordata 	
				CLOSE_BRACKET 
					{
					 // realisiert durch ein einelementiges Pointset mit einem LabelPunkt 
					 // mit verschwindend kleinem Radius!
					 t=s.getText();
					 PointSetFactory psf = new PointSetFactory();
					 double [][] data = new double [1][];
					 data[0]=v;
					 psf.setVertexCount(1);
					 psf.setVertexCoordinates(data);
					 String [] labs= new String[1];
					 labs[0]=s.getText();
					 psf.setVertexLabels(labs);
					 psf.update();
		
					 SceneGraphComponent geo=new SceneGraphComponent();
					 Appearance pointApp =MathematicaHelper.copyApp(app);
					 pointApp.setAttribute(CommonAttributes.SPHERES_DRAW, true);
					 //pointApp.setAttribute(CommonAttributes.VERTEX_DRAW, true);
					 pointApp.setAttribute(CommonAttributes.POINT_RADIUS, 0.0001);
				
					 geo.setAppearance(pointApp);
					 geo.setGeometry(psf.getPointSet());
					 geo.setName("Label");
					 current.addChild(geo);
					}
	;

private
pointBlock [Appearance app] returns [Appearance app2]
// eine Abfolge von Punkten wird zu einer PointSet
// je nach dem ob Farben ZWISCHEN den Punkten stehen wird eine 
// Farbliste eingelesen, oder die App. eingefaerbt
{
 app2=MathematicaHelper.copyApp(app);
 List<double[]> points= new LinkedList<double[]>(); 
 double[] v;
 List<Color> colors= new LinkedList<Color>();
 Color plC=MathematicaHelper.getPLColor(app2,plCDefault);
 boolean colorFlag=false;
 boolean colorNeeded =false;
 }
	:
	( "Point"
	   OPEN_BRACKET
				{v=new double[3];}
				v=vektor
				{points.add(v);
				 colors.add(plC);}
	   CLOSE_BRACKET 
	)
	(
	COLON
	(
	
	   plC=color {colorFlag=true; app2=MathematicaHelper.setPLColor(app2,plC);}
	 |( "Point"  {if (colorFlag) colorNeeded= true;}
	   OPEN_BRACKET
				{v=new double [3];}
				v=vektor
				{points.add(v);
				 colors.add(plC);}
	   CLOSE_BRACKET 
	   )
	 )
	)*
	{
		PointSetFactory psf = new PointSetFactory();
		double [][] data = new double [points.size()][];
		double [][] colorData = new double[points.size()][];
		int i=0;
		for(double[] d : points){
			data[i]=d;
			i++;
		}
		i=0;
		for(Color d : colors){
			colorData[i]=MathematicaHelper.getRGBColor(d);
			i++;
		}
		psf.setVertexCount(points.size());
		psf.setVertexCoordinates(data);
		Appearance pointApp =MathematicaHelper.copyApp(app2);
	    if (colorNeeded) 			// brauchen wir eine Farbliste?
			psf.setVertexColors(colorData);
		else
			pointApp= MathematicaHelper.setPLColor(pointApp,plC);
		psf.update();
		SceneGraphComponent geo=new SceneGraphComponent();
//		pointApp.setAttribute(CommonAttributes.SPHERES_DRAW, true);
		geo.setAppearance(pointApp);
		geo.setGeometry(psf.getPointSet());
		geo.setName("Points");
		current.addChild(geo);
		MathematicaHelper.simplifyColor(app2);
	}
	;  

private
lineBlock [Appearance app] returns[Appearance app2]
// liest eine Abfolge von Linien in eine IndexedLineSet.
// schmeist doppelte Punkte durch umindizierung raus.
// Farben wie bei pointBlock.
{
 app2=MathematicaHelper.copyApp(app);
 Color plC=MathematicaHelper.getPLColor(app2,plCDefault);						// Punkt/Linienfarbe
 List<double[]> coordinates= new LinkedList<double[]>();// alle Punkte in einer Liste
 List<double[]> line=new LinkedList<double[]>();					// alle Punkte einer Linie
 List<Color> colors= new LinkedList<Color>();// FarbListe
 int count=0;								// Anzahl aller bisher gesammelten Punkte
 int[] lineIndices;							// liste aller Indices einer Linie
 List<int[]> linesIndices= new LinkedList<int[]>();			// Liste aller IndiceeListen
 boolean colorFlag=false;
 boolean colorNeeded =false;
 }
 :	"Line"
	 OPEN_BRACKET
				line=lineset 			// das ist ein Vector von double[3]
				{
					lineIndices=new int[line.size()];
					int i=0;
					for(double[] d: line ){
						coordinates.add(d);  // Punkte zu einer Liste machen
				    	lineIndices[i]=i;			   // indizirung merken					
						i++;
					}
			    	count=line.size();
					linesIndices.add(lineIndices);
				    colors.add(plC);
				}
	 CLOSE_BRACKET 
	(
	  COLON
	  (
	    (plC=color {colorFlag=true; app2=MathematicaHelper.setPLColor(app2,plC);})
	   |( "Line"
	     OPEN_BRACKET
				line=lineset 			// das ist ein Vector von double[3]
				{if (colorFlag) colorNeeded= true;}
				{
					lineIndices=new int[line.size()];
					i=0;
					for(double[] d: line ){
						coordinates.add(d);  // Punkte zu einer Liste machen
				    	lineIndices[i]=i+count;			   // indizirung merken					
						i++;
					}
			    	count+=line.size();
					linesIndices.add(lineIndices);
					colors.add(plC);
				}
	     CLOSE_BRACKET )
	  )   
	)*
	{
		double [][] data= new double[coordinates.size()][];
		double [][] colorData = new double[linesIndices.size()][];
		i=0;
		for(double[] d: coordinates ){
			data[i]= d;
			i++;
		}
		int[][] indices= new int[linesIndices.size()][];
		i=0;
		for(int[] d: linesIndices){
			indices[i]=d;
			i++;
		}
		i=0;
		for(Color d: colors ){
			colorData[i]=MathematicaHelper.getRGBColor(d);		
			i++;
		}
		IndexedLineSetFactory lineset=new IndexedLineSetFactory();
		lineset.setLineCount(linesIndices.size());
		lineset.setVertexCount(count);
		lineset.setEdgeIndices(indices);
		lineset.setVertexCoordinates(data);
		lineset.update();
		Appearance lineApp =MathematicaHelper.copyApp(app2);
		if (colorNeeded)
			lineset.getIndexedLineSet().setEdgeAttributes(
			 Attribute.COLORS,new DoubleArrayArray.Array( colorData ));
		else	lineApp= MathematicaHelper.setPLColor(lineApp,plC);
		lineset.update();
		SceneGraphComponent geo=new SceneGraphComponent();
		geo.setAppearance(lineApp);
		IndexedLineSet ils=lineset.getIndexedLineSet();
		ils=RemoveDuplicateInfo.removeDuplicateVertices(ils,Attribute.COLORS);
		geo.setGeometry(ils);
		geo.setName("Lines");
		current.addChild(geo);
		MathematicaHelper.simplifyColor(app2);
	}
	;  


private 
polygonBlock [Appearance app, Object edgeF] returns[Appearance app2]
// liest eine Abfolge von Polygonen 
// schmeist doppelte Punkte durch umindizierung raus
// Farben wie pointBlock und lineBlock
{
 app2=MathematicaHelper.copyApp(app);
 Color fC=MathematicaHelper.getFColor(app2,fCDefault);
 List<double[]> coordinates= new LinkedList<double[]>();// alle PunktListen vereint in einer
 List<double[]> poly=new LinkedList<double[]>();			// alle Punkte in einem Polygon
 int[] polyIndices;					// alle indices eines Polygons
 List<Color> colors= new LinkedList<Color>();		// FarbListe
 List<int[]> polysIndices= new LinkedList<int[]>();// IndexListen-Liste
 int count=0;						// zaehlt die Punkte mit
 boolean colorFlag=false;
 boolean colorNeeded =false;
 }
	:"Polygon"
	 OPEN_BRACKET
				poly=lineset 			// das ist ein Vector von double[3]
				{
					polyIndices=new int[poly.size()];
					int i=0;
					for(double[] d : poly){
						coordinates.add(d);  //Punkte zu einer Liste machen
						polyIndices[i]=i;	 // indizirung merken
						i++;
					}
			    	count=poly.size();
					polysIndices.add(polyIndices);
					colors.add(fC);
				}
	 CLOSE_BRACKET 
	( COLON
	 (
	   (   app2=faceColor[app2] {colorFlag=true; fC=MathematicaHelper.getFColor(app2,fCDefault);} )
	  |("Polygon"
	    OPEN_BRACKET
				poly=lineset 			// das ist ein Vector von double[3]
				{if (colorFlag) colorNeeded= true;}
				{
					polyIndices=new int[poly.size()];
					i=0;
					for(double[] d : poly){
						coordinates.add(d);  //Punkte zu einer Liste machen
						polyIndices[i]=i+count;	 // indizirung merken
						i++;
					}
			    	count+=poly.size();
					polysIndices.add(polyIndices);
					colors.add(fC);
				}
	    CLOSE_BRACKET
	  ))
	)*
	{
		double [][] data= new double[count][];
		double [][] colorData = new double[polysIndices.size()][];	
		i=0;
		for(double[] d : coordinates){
			data[i]=((double[]) coordinates.get(i));
			i++;
		}
		int[][] indices= new int[polysIndices.size()][];
		i=0;
		for(int[] d : polysIndices){// Indices als doppelListe von Doubles machen
			indices[i]=d;
			i++;
		}
		i=0;
		for(Color d : colors){
			colorData[i]=MathematicaHelper.getRGBColor(d);
			i++;
		}
		IndexedFaceSetFactory faceSet = new IndexedFaceSetFactory();
		faceSet.setVertexCount(count);
		faceSet.setFaceCount(polysIndices.size());
		faceSet.setFaceIndices(indices);
		faceSet.setVertexCoordinates(data);
		Appearance faceApp =MathematicaHelper.copyApp(app2);
		if (colorNeeded)
			faceSet.setFaceColors(colorData);
	//TODO do not colorice if color == inherit
		else	faceApp.setAttribute(CommonAttributes.POLYGON_SHADER+"."+
			 	CommonAttributes.DIFFUSE_COLOR, fC);
		if (edgeF!=null){
			faceApp.setAttribute(CommonAttributes.TUBE_RADIUS,1/150);
			faceApp.setAttribute(CommonAttributes.LINE_WIDTH,1);	 	
			if (edgeF instanceof Color){
		 		faceApp.setAttribute(CommonAttributes.LINE_SHADER+"."+
			 		CommonAttributes.POLYGON_SHADER+"."+
			 		CommonAttributes.DIFFUSE_COLOR, edgeF);
		 		faceApp.setAttribute(CommonAttributes.LINE_SHADER+"."+
				 	CommonAttributes.DIFFUSE_COLOR, edgeF);				 	
			}
			faceSet.setGenerateEdgesFromFaces(true);
		}
		else faceSet.setGenerateEdgesFromFaces(false);
		faceSet.setGenerateFaceNormals(true);
		faceSet.update();
		IndexedFaceSet ifs= faceSet.getIndexedFaceSet();
		ifs=RemoveDuplicateInfo.removeDuplicateVertices(ifs,Attribute.COLORS);
		IndexedFaceSetUtility.assignSmoothVertexNormals(ifs,-1);
		SceneGraphComponent geo=new SceneGraphComponent();	// Komponenten erstellen und einhaengen
		geo.setAppearance(faceApp);
		current.addChild(geo);
		geo.setName("Faces");
		geo.setGeometry(ifs);
	}
	;
	
// -------------------------------------------------- Farben --------------------------------------------
private
edgeFormThing returns[Object c]
// gibt die Farbe der PolygonRaender an
// keine Farbe bedeutet keine Raender
{c=null;}
	: "EdgeForm" OPEN_BRACKET
			(c=color)?
				CLOSE_BRACKET 
	;

private
faceColor [Appearance app] returns[Appearance app2]
// Farben fuer Flaechen sind in 'SurfaceColor[]' gekapselt
{
 app2=MathematicaHelper.copyApp(app);
 Color specular; double d; Color fC= new Color(255,0,0);
}
	: "SurfaceColor" OPEN_BRACKET
			fC=color
				{app2.setAttribute(CommonAttributes.POLYGON_SHADER+"."+
				 	CommonAttributes.DIFFUSE_COLOR, fC);}
			( COLON specular=color	
				{app2.setAttribute(CommonAttributes.POLYGON_SHADER+"."+CommonAttributes.SPECULAR_COLOR,specular);}
			 (	COLON 	d=doublething 
			 	{app2.setAttribute(CommonAttributes.POLYGON_SHADER+"."+CommonAttributes.SPECULAR_EXPONENT,d);}
			 )?
			)?
		CLOSE_BRACKET 
	;

private
color returns[Color c]
// liest eine Farbe 
// Farben haben verschiedene Darstellungen
{c= new Color(0,255,0);}
		: "RGBColor" OPEN_BRACKET  // Red-Green-Blue
					{double r,g,b; r=b=g=0;}
					r=doublething COLON g=doublething COLON b=doublething
				CLOSE_BRACKET 
					{
					 float red,green,blue;
					 red=(float) r; green=(float) g; blue=(float) b;
					 c= new Color(red,green,blue);
					 }
		| "Hue" 	OPEN_BRACKET // Hue-Saturation-Brightness
					{double h; double s; double b; h=s=b=0.5;}
					h= doublething 
					(COLON s=doublething COLON b=doublething )?
				CLOSE_BRACKET 
					{
					 float hue,sat,bri;
					 hue=(float) h; sat=(float) s; bri=(float) b;
					 c = Color.getHSBColor(hue,sat,bri);
					}
		| "GrayLevel" OPEN_BRACKET // Schwarz-Weiss
					{double gr=0;} gr=doublething 
				CLOSE_BRACKET 
					{
					float grey=(float) gr;
					c= new Color(grey,grey,grey);
					}
		| "CMYKColor" OPEN_BRACKET // Cyan-Magenta-Yellow-black
					{double cy,ma,ye,k; cy=ma=ye=k=0; }
					cy=doublething COLON 
					ma=doublething COLON 
					ye=doublething COLON 
					k=doublething 
				CLOSE_BRACKET 
					{
					 float r,g,b;
					 r=(float) ((1-cy)*(1-k));
					 g=(float) ((1-ma)*(1-k));
					 b=(float) ((1-ye)*(1-k));
					 c= new Color(r,g,b);
					}
		;
// -------------------------------------------- Daten ------------------------------------
private
lineset returns[LinkedList<double[]> v]
// Koordinaten in einer Liste zu Vector(double[3])
{
double [] point =new double[3];
double [] point2=new double[3];
double [] point3=new double [3];
v=new LinkedList<double[]>();}
		: OPEN_BRACE
		  point=vektor
		    {
		    v.add(point);
		    point3[0]=point[0];// have to save first point seperate and insert later again to first position (dont ask !)
		    point3[1]=point[1];
		    point3[2]=point[2];		    
			}
		  (COLON 
			{point2= new double[3];
			}
			point2=vektor
			{
		    v.add(point2);
			}
		  )*
		 CLOSE_BRACE
		 {
		 v.set(0,point3);
		 }
		;

private
vektor returns[double[] res]
// ein KoordinatenTripel(Punkt) zu double[3]
{res =new double [3];
double res1,res2,res3;}
	: 	OPEN_BRACE 
			res1=doublething COLON res2=doublething COLON res3=doublething 
		CLOSE_BRACE
			{
			 res[0]=res1;
			 res[1]=res2;
			 res[2]=res3;
			 resetBorder (res);
			}
	;

private	
vektordata returns[double[] res]
// das gleiche wie vektor, beeinflusst aber nicht die Borderberechnung(Scenengroese)
{res =new double [3];
double res1,res2,res3;}
	: 	OPEN_BRACE 
			res1=doublething COLON res2=doublething COLON res3=doublething 
		CLOSE_BRACE
			{
			 res[0]=res1;
			 res[1]=res2;
			 res[2]=res3;
			}
	;

// --------------------------------graphische Directiven ---------------------------------------- 

private 
directiveBlock[Appearance appOld] returns [ Appearance app]
// eine Abfolge von Direktiven die die Appearance beeinflussen(keine Farben, keine EdgeForm)
{app =MathematicaHelper.copyApp(appOld);}
	: app=directive[app]
	  (
	  	COLON
	  	app=directive[app]
	  )*
	;

/** 
* TODO: interpret more directives(do this in mathematica.g)
*/
private 
directive[Appearance appGiven] returns [Appearance app]
// Direktiven die die Appearance beeinflussen(keine Farben)
// Bemerkung: Der Aufruf 'dumb' ignoriert alles in der Klammer.
{app = MathematicaHelper.copyApp(appGiven); 
Color col;}
	:"AbsolutePointSize" 								
		// Dicke der Punkte
		OPEN_BRACKET
			{double d=0;} d=doublething
			{
			app.setAttribute(CommonAttributes.POINT_RADIUS,d/40);
			app.setAttribute(CommonAttributes.POINT_SIZE,d);
			}
			CLOSE_BRACKET 
	|"AbsoluteThickness"	
		// Dicke der Linien
		OPEN_BRACKET  
			{double d=0;} d=doublething
		CLOSE_BRACKET 
			{
			app.setAttribute(CommonAttributes.TUBE_RADIUS,d/40);
			app.setAttribute(CommonAttributes.LINE_WIDTH,d);
			}
// -- ab hier nicht behandelte directives
	|"Dashing" OPEN_BRACKET (dumb)? CLOSE_BRACKET
		{log.fine("Dashing not implemented");}
		// 2D: strichelt Linien (unnoetig da wir im 3d sind)
	|"FaceForm" OPEN_BRACKET (dumb)? CLOSE_BRACKET
		{log.fine("FaceForm not implemented");}
		// faerbt Flaechen 2-seitig verschieden (koennen wir garnicht)
	|"PointSize" OPEN_BRACKET (dumb)? CLOSE_BRACKET 
		{log.fine("PointSize not implemented");}
		// Groese eines Punktes als Anteil an der Graphengroese (zu aufwendig)
		// schlecht: brauche die Groese des Graphen in der Mitte der Auswertung
	|"Thickness" OPEN_BRACKET (dumb)?	CLOSE_BRACKET 
		{log.fine("Thicknes not implemented");}
		// Dicke einer Linie als Anteil an der Graphengroese (zu aufwendig)
		// schlecht: brauche die Groese des Graphen in der Mitte der Auswertung
	|"AbsoluteDashing" OPEN_BRACKET  (dumb)? CLOSE_BRACKET 
		{log.fine("AbsoluteDashing not implemented");}
		// 3D: strichelt Lienien (sehr aufwendig)
	;

// ----------------------------------------------- Optionen ------------------------------------------

private
optionen
// Optionen beeinflussen die gesammte Scene
	: OPEN_BRACE 			// Block von Optionen
	  		( optionen (COLON optionen)* )? 
	  CLOSE_BRACE
	| optionPrimitive		// eine einfache Option
	;

/** 
* TODO: interpret more Options(do this in mathematica.g)
*/
private
optionPrimitive
// im Momment werden alle Optionen ueberlesen!

// einfache Optionen
// die meisten werden schlicht ignoriert
// Bemerkung: egal ueberspring alles bis zur naechsten Option bzw dem Ende des Blocks
// Bemerkung: man kann offensichtlich auch auf Quellen verweisen (option :> $Identifier)
//		ich habe aber keine Ahnung was das bedeutet. ignoriere es also

// -- moeglich/Sinnvoll:
	:	"Boxed"			(DDOT LARGER	egal| MINUS LARGER	egal)
				{log.fine(" Boxed not implemented");}
	|	"Axes" 			(DDOT LARGER	egal| MINUS LARGER	egal)
				{log.fine(" Axes not implemented");}
	|	"AxesLabel"				(DDOT LARGER	egal| MINUS LARGER	egal)
				{log.fine(" AxesLabel not implemented");}
	|	"Prolog"				(DDOT LARGER	egal| MINUS LARGER	egal)
				{log.fine(" Prolog not implemented");}
			// irgendwelche graphischen objekte die zuerst berechnet werden
			// ( koennte man auch noch Parsen und extra an die Root haengen)
	|	"Epilog"				(DDOT LARGER	egal| MINUS LARGER	egal)
				{log.fine(" Epilog not implemented");}
			// irgendwelche graphischen objekte die zuletzt berechnet werden
			// ( koennte man auch noch Parsen und extra an die Root haengen)
	|	"ViewPoint"				(DDOT LARGER	egal| MINUS LARGER	egal)
				{log.fine(" ViewPoint not implemented");}
			// CameraFokus (sollen wir das ueberhaupt uebernehmen)
	|	"ViewCenter"			(DDOT LARGER	egal| MINUS LARGER	egal)
				{log.fine(" ViewCenter not implemented");}
			//	???
	|	"FaceGrids"				(DDOT LARGER	egal| MINUS LARGER	egal)
				{log.fine(" FaceGrids not implemented");}
			//	3d Linien-Gitter in der bounding-box
	|	"Ticks"					(DDOT LARGER	egal| MINUS LARGER	egal)
				{log.fine(" Ticks not implemented");}
			// Ticks sind die Markierungen an den Achsen.
			// Farbe, Dicke, Abstaende, Labels... alles einstellbar
			// (sehr viele moegliche Angaben, manche ganz sinnvoll)
	|	"TextStyle"				(DDOT LARGER	egal| MINUS LARGER	egal)
				{log.fine(" TextStyle not implemented");}
			//	SchriftDarstellung(brauch man das?)
	|	"BoxRatios"				(DDOT LARGER	egal| MINUS LARGER	egal)
				{log.fine(" BoxRatios not implemented");}
			//3D: Laengenverzerrung der Achsen in der Darstellung(linear)
 	|	"Lighting"				(DDOT LARGER	egal| MINUS LARGER	egal)
				{log.fine(" Lighting not implemented");}
 			// soll es Lichtquellen geben (ja/nein), oder gar eine Farbfunktionen
	|	"LightSources"			(DDOT LARGER	egal| MINUS LARGER	egal)
				{log.fine(" LightSources not implemented");}
			// Liste von orientierten Lichtquellen
	|	"AmbientLight"			(DDOT LARGER	egal| MINUS LARGER	egal)
				{log.fine(" AmbientLight not implemented");}
			// 
	|	"AxesEdge"				(DDOT LARGER	egal| MINUS LARGER	egal)
				{log.fine(" AxesEdge not implemented");}
			//	welche Axen der Box sollen gezeichnet werden

// -- eher schwierig:
	|	"PlotRange" 			(DDOT LARGER	egal| MINUS LARGER	egal)
				{log.fine(" PlotRange not implemented");}
			// Abschneiden bei zu extremen Werten. Wie soll man abschneiden?(durch die Polygone?)
	|	"DefaultColor"			(DDOT LARGER	egal | MINUS LARGER	egal)
				{log.fine(" DefaultColor not implemented");}
			// StandardFarbe (kann nicht mehr die gesetzten Farben in einem Block aendern)
	|	"Background"			(DDOT LARGER	egal | MINUS LARGER	egal)
				{log.fine(" Background not implemented");}
			// Hintergrundfarbe (fuer den Viewer schlecht da file.m nur Teil der Scene ist)
			// wie sollte ich das Aendern?
	|	"ColorOutput" 			(DDOT LARGER	egal | MINUS LARGER	egal)
				{log.fine(" ColorOutput not implemented");}
			// wie Farben ausgegeben werden sollen (sehr speziell)
	|	"AxesStyle"				(DDOT LARGER	egal | MINUS LARGER	egal)
				{log.fine(" AxesStyle not implemented");}
			// gibt den Axen Directiven(Farbe,dicke ...) brauchen wir das wir das?
	|	"BoxStyle"				(DDOT LARGER	egal | MINUS LARGER	egal)
				{log.fine(" BoxStyle not implemented");}
			// gibt wie "AxesStyle" der Bounding Box graphische Direktiven

// -- unsinnig/unmoeglich
	|	"PlotLabel" 			(DDOT LARGER	egal | MINUS LARGER	egal)
				{log.fine(" PlotLabel not implemented");}
			// 2D: Label an den Achsen (wir sind im 3d !!!)
	|	"AspectRatio"			(DDOT LARGER	egal | MINUS LARGER	egal)
				{log.fine(" AspectRatio not implemented");}
			//2D: Laengenverzerrung der Achsen in der Darstellung(2d ist egal)
	|	"DefaultFont"			(DDOT LARGER	egal | MINUS LARGER	egal)
				{log.fine(" DefaultFont not implemented");}
			// SchriftArt (veraltete Mathematica Version)
	|	"PlotRegion"			(DDOT LARGER	egal | MINUS LARGER	egal)
				{log.fine(" PlotRegion not implemented");}
			// groese des Fensters in Mathematica (ist egal) 			
	|	"ViewVertical"			(DDOT LARGER	egal | MINUS LARGER	egal)
				{log.fine(" ViewVertical not implemented");}
			// dreht irgendwie das dargestellte Bild
	|	"SphericalRegion"		(DDOT LARGER	egal | MINUS LARGER	egal)
				{log.fine(" SphericalRegion not implemented");}
			//	merkwuerdige Art die mit Graphic ausgefuellte Flaeche im Fenster zu setzen
			// (ist mathematica speziefisch, brauchen wir nicht)
	|	"Shading"				(DDOT LARGER	egal | MINUS LARGER	egal)
				{log.fine(" Shading not implemented");}
			// irgendwas mit Farbe anzeigen (eigentlich fuer SurfaceGraphics nicht fuer Graphics3D)
	|	"RenderAll"				(DDOT LARGER	egal | MINUS LARGER	egal)
				{log.fine(" RenderAll not implemented");}
			// berechnet Alles oder nur sichtbare Teile bei der Bilderzeugung in Mathematica
			// (ist egal da wir ja keine Bilder sondern eine Scene erzeugen wollen)
	|	"PolygonIntersections"	(DDOT LARGER	egal | MINUS LARGER	egal)
				{log.fine(" PolygonIntersections not implemented");}
			// Teilt Polygone so das sie sich nicht mehr schneiden.
			// Fuer die PostScript-Erzeugung fuer das Bild.

// -- keine Ahnung
	|	"DisplayFunction"		(DDOT LARGER	egal | MINUS LARGER	egal)
				{log.fine(" DisplayFunction not implemented");}
			// ???
	|	"Plot3Matrix"			(DDOT LARGER	egal | MINUS LARGER	egal)
				{log.fine(" Plot3Matrix not implemented");}
			// (Alte mathematica Version)
	|	"ImageSize"				(DDOT LARGER	egal | MINUS LARGER	egal)
				{log.fine(" ImageSize not implemented");}
			// Render Informationen
	|	"FormatType"			(DDOT LARGER	egal | MINUS LARGER	egal)
				{log.fine(" FormatType not implemented");}
			// irgendwas fuer OutputStreams 			
	;
	
// -------------------------------------------------- Kleinkram -------------------------------------------

private
egal
// ueberliest den Rest bis zur naechsten Option. Laest das Komma stehen!
// endet auch beim Klammerende
	: (~(  COLON | OPEN_BRACE | OPEN_BRACKET | CLOSE_BRACE | LPAREN ))*
		(   OPEN_BRACE	 	(dumb)*		CLOSE_BRACE   		(egal)?
		 |	OPEN_BRACKET 	(dumb)*		CLOSE_BRACKET   	(egal)?
		 |  LPAREN			(dumb)*		RPAREN				(egal)?  	)?
  ;

private
integerthing returns[int i]
// liest ein Integer aus
{i=0;String sig="";}
	: (PLUS | MINUS {sig="-";} )?
	  s:INTEGER_THING {i=Integer.parseInt(sig + s.getText());}
	;




private
doublething returns[double d=0]
	: d=doubleHelp
	  (doubleHelp STAR "I") ?
	;



	
private
doubleHelp  returns[double d=0]
// liest ein double aus
	{double e=0; String sig="";}
    : (PLUS | MINUS {sig="-";} )?
    ( s:INTEGER_THING 
    		{d=Double.parseDouble(sig + s.getText());}
      (DOT
      	(s2:INTEGER_THING 
    		{ d=Double.parseDouble(sig + s.getText()+ "." + s2.getText());}
         )?
      )?
	| DOT s3:INTEGER_THING 
			{d=Double.parseDouble(sig + "0." + s3.getText());}
    )
    (e=exponent_thing {d=d*Math.pow(10,e);})?
    ;

private 
exponent_thing returns[int e]
// liest den exponenten fuer double_thing
{e=0; String sig="";}
    : STAR HAT 
    (PLUS | MINUS {sig="-";} )?
     s:INTEGER_THING
     	{e=Integer.parseInt(sig + s.getText() );}
	;
	
private
dumb
// ueberliset alles bis zum Klammerende auch mit Unterklammern
	:(  (~(	LPAREN | RPAREN | OPEN_BRACE | OPEN_BRACKET | CLOSE_BRACE | CLOSE_BRACKET))+
		(   OPEN_BRACE	 	(dumb)*	CLOSE_BRACE   
		 |	OPEN_BRACKET 	(dumb)*	CLOSE_BRACKET 
		 |  LPAREN			(dumb)*		RPAREN	)?  )
	 |  (   OPEN_BRACE	 	(dumb)*	CLOSE_BRACE   
		 |  OPEN_BRACKET 	(dumb)*	CLOSE_BRACKET
		 |  LPAREN			(dumb)*		RPAREN  )
  ;
	
/** ************************************************************************
*   ******************* The Mathematica Lexer ******************************
*   ************************************************************************
* this class is only for class MathematicaParser
*/

class MathematicaLexer extends Lexer;
options {
	charVocabulary = '\3'..'\377';
	k=2;
	testLiterals=false;
}
	/** Terminal Symbols */
OPEN_BRACE:		'{';
CLOSE_BRACE:	'}';
OPEN_BRACKET:	'[';
CLOSE_BRACKET:	']';
LPAREN:			'(';
RPAREN:			')';
BACKS:			'\\';
SLASH:			'/';
COLON:			',';
DOLLAR: 		'$';
MINUS:			'-';
PLUS:			'+';
LARGER:			'>';
SMALER:			'<';
DOT:			'.';
HAT:			'^';
STAR:			'*';
DDOT: 			':';

T1: '!';
T2: '@';
T3: '#';
T4: '%';
T5: '&';
T6: '=';
T7: ';';
T8: '"';
T9: '?';

ID
options {
	paraphrase = "an identifier";
	testLiterals=true;
}
	:	('a'..'z'|'A'..'Z'|'_') (ID_LETTER)*
	;

private 
ID_LETTER:
	('a'..'z'|'A'..'Z'|'_'|'0'..'9')
	;
	
INTEGER_THING
	: (DIGIT)+
	;
		
private
DIGIT:
	('0'..'9')
	;
	
STRING:
		'"'! (ESC | ~('"'|'\\'))* '"'!
	;
private
ESC:
		'\\'! ('\\' | '"')
	;

WS_:
		( ' '
		| '\t'
		| '\f'
		// handle newlines
		|	(options {
					generateAmbigWarnings=false;
				}
		: "\r\n"	// Evil DOS
			| '\r'		// MacINTosh
			| '\n'		// Unix (the right way)
			{newline(); } )	
		)+ { $setType(Token.SKIP); }
;
