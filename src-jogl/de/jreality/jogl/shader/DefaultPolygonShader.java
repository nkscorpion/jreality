/*
 * Created on May 7, 2004
 *
 */
package de.jreality.jogl.shader;

import java.awt.Color;

import net.java.games.jogl.GL;
import net.java.games.jogl.GLCanvas;
import de.jreality.jogl.ElementBinding;
import de.jreality.jogl.JOGLRenderer;
import de.jreality.scene.Appearance;
import de.jreality.scene.CommonAttributes;
import de.jreality.scene.Geometry;
import de.jreality.scene.ReflectionMap;
import de.jreality.scene.Texture2D;
import de.jreality.util.EffectiveAppearance;
import de.jreality.util.NameSpace;
import de.jreality.util.Rn;
import de.jreality.util.ShaderUtility;

/**
 * @author Charles Gunn
 *
 */
public class DefaultPolygonShader implements PolygonShader {

	public static final int FRONT_AND_BACK = GL.GL_FRONT_AND_BACK;
	public static final int FRONT = GL.GL_FRONT;
	public static final int BACK = GL.GL_BACK;
	
	boolean		smoothShading = true; 		// interpolate shaded values between vertices
	Texture2D texture2D;
  Texture2D lightMap;
	ReflectionMap reflectionMap;
	int frontBack = FRONT_AND_BACK;
	public VertexShader vertexShader = null;
	AbstractJOGLShader glShader = null;
	static double[] idmat = Rn.identityMatrix(4);
	/**
		 * 
		 */
		public DefaultPolygonShader() {
			super();
			vertexShader = new DefaultVertexShader();
		}

		
	public static DefaultPolygonShader createFromEffectiveAppearance(EffectiveAppearance eap, String name)	{
		DefaultPolygonShader dgs = new DefaultPolygonShader();
		dgs.setFromEffectiveAppearance(eap, name);
		return dgs;
	}
	
	static int count = 0;
	public void  setFromEffectiveAppearance(EffectiveAppearance eap, String name)	{
		vertexShader = ShaderLookup.getVertexShaderAttr(eap, name, "default");

		smoothShading = eap.getAttribute(NameSpace.name(name,CommonAttributes.SMOOTH_SHADING), CommonAttributes.SMOOTH_SHADING_DEFAULT);	
		Object foo = eap.getAttribute(NameSpace.name(name,"texture2d"), null, Texture2D.class);
		if (foo instanceof Texture2D)	texture2D = (Texture2D) foo;
		foo = eap.getAttribute(NameSpace.name(name,"reflectionMap"), null, ReflectionMap.class);
		if (foo instanceof ReflectionMap)	reflectionMap = (ReflectionMap) foo;
    foo = eap.getAttribute(NameSpace.name(name,"lightMap"), null, Texture2D.class);
    if (foo instanceof Texture2D) lightMap = (Texture2D) foo;
	
		//TODO this is a hack. 
		if (eap.getAttribute(NameSpace.name(name,"useGLShader"), false) == true)	{
			Object obj =  eap.getAttribute(NameSpace.name(name,"GLShader"), null, AbstractJOGLShader.class);
			if (obj instanceof AbstractJOGLShader) {
				glShader = (AbstractJOGLShader) obj;
			}
		} else glShader = null;
	
	}

		/**
		 * @return
		 */
		public boolean isSmoothShading() {
			return smoothShading;
		}

	/**
	 * @return
	 */
	public Color getDiffuseColor() {
		return vertexShader.getDiffuseColor(); //diffuseColor;
	}

	public float[] getDiffuseColorAsFloat() {
		return vertexShader.getDiffuseColorAsFloat();
	}

	/**
	 * @return
	 */
	public Texture2D getTexture2D() {
		return texture2D;
	}

	public void setSmoothShading(boolean b) {
		smoothShading = b;
	}
	
	public int getFrontBack() {
		return frontBack;
	}
	public void setFrontBack(int frontBack) {
		this.frontBack = frontBack;
	}
	
	public void render(JOGLRenderer jr)	{
		GLCanvas theCanvas = jr.getCanvas();
		GL gl = theCanvas.getGL();
		
		if (isSmoothShading()) gl.glShadeModel(GL.GL_SMOOTH);
		else		gl.glShadeModel(GL.GL_FLAT);
		
		//gl.glMaterialfv(frontBack, GL.GL_DIFFUSE, getDiffuseColorAsFloat());
//		gl.glEnable(GL.GL_COLOR_MATERIAL);
//		gl.glColorMaterial(frontBack, GL.GL_DIFFUSE);
//		gl.glColor4fv( getDiffuseColorAsFloat());
		//System.out.println("Alpha channel is "+diffuseColorAsFloat[3]);
		//System.out.println("transparency is "+transparency);
		//float[] testcolor = {.3f, .5f, .7f, 1.0f * ((float) transparency)};
		//gl.glMaterialfv(GL.GL_BACK, GL.GL_DIFFUSE, testcolor);
		gl.glActiveTexture(GL.GL_TEXTURE0);
		gl.glDisable(GL.GL_TEXTURE_2D);
		gl.glDisable(GL.GL_TEXTURE_CUBE_MAP);
		gl.glDisable(GL.GL_TEXTURE_GEN_S);
		gl.glDisable(GL.GL_TEXTURE_GEN_T);
		gl.glDisable(GL.GL_TEXTURE_GEN_R);
		gl.glActiveTexture(GL.GL_TEXTURE1);
		gl.glDisable(GL.GL_TEXTURE_2D);
		gl.glDisable(GL.GL_TEXTURE_CUBE_MAP);
		gl.glDisable(GL.GL_TEXTURE_GEN_S);
		gl.glDisable(GL.GL_TEXTURE_GEN_T);
		gl.glDisable(GL.GL_TEXTURE_GEN_R);
		int texUnit = GL.GL_TEXTURE0;

		if (texture2D != null)	{
			gl.glActiveTexture(texUnit);
			texUnit++;
			Texture2DLoaderJOGL.render(theCanvas, texture2D);
			int[] res = new int[1];
			gl.glGetTexParameteriv(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_RESIDENT, res);
			//System.out.println("Texture is resident: "+res[0]);
			if (res[0] == 0)	{ jr.texResident = false; }
			gl.glEnable(GL.GL_TEXTURE_2D);
		} //else
    if (lightMap != null)  {
        Texture2DLoaderJOGL.render(theCanvas, lightMap, 1);
        //int[] res = new int[2];
        //gl.glGetTexParameteriv(GL.GL_TEXTURE_2D, GL.GL_TEXTURE_RESIDENT, res);
        //System.out.println("Texture is resident: "+res[0]);
        //if (res[0] == 0)    { jr.texResident = false; }
        gl.glEnable(GL.GL_TEXTURE_2D);
    } //else
		if (reflectionMap != null)	{
			gl.glActiveTexture(texUnit);
			texUnit++;
			Texture2DLoaderJOGL.render(jr, reflectionMap);
			int[] res = new int[1];
			gl.glGetTexParameteriv(GL.GL_TEXTURE_CUBE_MAP, GL.GL_TEXTURE_RESIDENT, res);
			//System.out.println("Texture is resident: "+res[0]);
			if (res[0] == 0)	{ jr.texResident = false; }
			gl.glEnable(GL.GL_TEXTURE_CUBE_MAP);
			//System.out.println("cube map enabled");
		} 
		vertexShader.setFrontBack(frontBack);
		vertexShader.render(jr);
		if (glShader != null) glShader.activate(theCanvas);
	}

	public boolean providesProxyGeometry() {		
		return false;
	}
	public int  proxyGeometryFor(Geometry original, JOGLRenderer jr, int sig) {
		return -1;
	}

}
