require 'java' 
import 'java.io.IOException' 
import 'java.net.URL' 
import 'java.awt.Color' 

import 'de.jreality.geometry.Primitives' 
import 'de.jreality.reader.Readers' 
import 'de.jreality.ui.viewerapp.ViewerApp' 
import 'de.jreality.scene.data.AttributeEntityUtility' 

class MainWindow 
include_package 'de.jreality.shader' 
include_package 'de.jreality.util' 
include_package 'de.jreality.scene' 
include_package 'de.jreality.math' 

def initialize 
# Scene, Viewerapp setting 
  myscene = SceneGraphUtility.createFullSceneGraphComponent("myscene") 
  myscene.setGeometry(Primitives.cylinder(20)) 
  va = ViewerApp.display(myscene) 
  va.setAttachNavigator(true) 
  va.setExternalNavigator(false) 
  va.update 
  CameraUtility.encompass(va.getViewerSwitch) 
  ap = myscene.getAppearance 
# Geometry setting 
  dgs = ShaderUtility.createDefaultGeometryShader(ap, true) 
  dgs.setShowLines(false) 
  dgs.setShowPoints(false) 
  dps = dgs.createPolygonShader("default") 
  dps.setDiffuseColor(Color.white) 
# Texture setting with matrix scaling 
  tex2d = AttributeEntityUtility.createAttributeEntity(Texture2D.java_class, CommonAttributes::POLYGON_SHADER + "." + CommonAttributes::TEXTURE_2D,ap, true) 
  is = URL.new("http://www3.math.tu-berlin.de/jreality/download/data/gridSmall.jpg") 
  id = ImageData.load(Input.new(is)) 
  tex2d.setImage(id) 
  matrix = Matrix.new 
  MatrixBuilder.euclidean.scale(10.0, 5.0, 1.0).assignTo(matrix) 
  tex2d.setTextureMatrix(matrix) 
end 
end 
MainWindow.new 
