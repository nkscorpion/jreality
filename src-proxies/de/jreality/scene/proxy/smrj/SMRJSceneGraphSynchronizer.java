/*
 * Created on 12-Nov-2004
 *
 * This file is part of the jReality package.
 * 
 * This program is free software; you can redistribute and/or modify 
 * it under the terms of the GNU General Public License as published 
 * by the Free Software Foundation; either version 2 of the license, or
 * any later version.
 * 
 * This program is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITTNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with this program; if not, write to the 
 * Free Software Foundation, Inc.,
 * 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307
 * USA 
 */
package de.jreality.scene.proxy.smrj;

import java.util.Iterator;
import java.util.List;

import de.jreality.scene.*;
import de.jreality.scene.Appearance;
import de.jreality.scene.Geometry;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.IndexedLineSet;
import de.jreality.scene.PointSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.FactoredTransformation;
import de.jreality.scene.data.Attribute;
import de.jreality.scene.data.ByteBufferList;
import de.jreality.scene.data.DataList;
import de.jreality.scene.event.*;
import de.jreality.scene.proxy.scene.*;

/**
 * 
 * This class registers itself to all nodes for keeping the remote scenegraph up-to-date.
 * 
 * TODO: implement this as 1-1 in the factory
 * 
 * @author weissman
 */
public class SMRJSceneGraphSynchronizer extends SceneGraphVisitor implements TransformationListener, AppearanceListener, GeometryListener, SceneContainerListener {
		
	SMRJMirrorScene rmc;
	SMRJMirrorFactory factory;
	private boolean debug;

	public SMRJSceneGraphSynchronizer(SMRJMirrorScene rmc) {
		this.rmc = rmc;
		factory = (SMRJMirrorFactory) rmc.getProxyFactory();
	}
	
	public void visit(final FactoredTransformation t) {
		t.addTransformationListener(this);
	}

	public void visit(final Appearance a) {
		a.addAppearanceListener(this);
	}

	public void visit(final Geometry g) {
		g.addGeometryListener(this);
	}
	
	public void visit(SceneGraphComponent sg) {
		sg.addSceneContainerListener(this);
	}
	
	public void transformationMatrixChanged(TransformationEvent ev) {
      	((RemoteTransformation)rmc.getProxy(ev.getSourceNode())).setMatrix(ev.getTransformationMatrix());
	}

	public void appearanceChanged(AppearanceEvent ev) {
		Appearance src = (Appearance) ev.getSourceNode();
        RemoteAppearance dst = (RemoteAppearance) rmc.getProxy(src);
        List lst= src.getChildNodes();
        for (int ix= 0, num= lst.size(); ix < num; ix++) {
            de.jreality.scene.AppearanceAttribute aa= (de.jreality.scene.AppearanceAttribute)lst.get(ix);
            dst.setAttribute(
                    aa.getAttributeName(),
                    aa.getValue(),
                    aa.getAttributeType()
            );
        }
	}

	public void geometryChanged(GeometryEvent ev) {
        Geometry src = ev.getGeometry();
        RemoteGeometry dst = (RemoteGeometry) rmc.getProxy(src);
        for (Iterator i = ev.getChangedFaceAttributes().iterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            DataList dl = ((IndexedFaceSet) src).getFaceAttributes(a);
            if (ByteBufferList.canCopy(dl)) {
            	ByteBufferList copy = ByteBufferList.createByteBufferCopy(dl);
                ((RemoteIndexedFaceSet) dst).setFaceAttributes(a, copy);
                ByteBufferList.releaseList(copy);
            } else {
                ((RemoteIndexedFaceSet) dst).setFaceAttributes(a, dl);
            }            
        }
        for (Iterator i = ev.getChangedEdgeAttributes().iterator(); i.hasNext();) {
            Attribute a = (Attribute) i.next();
            DataList dl = ((IndexedLineSet) src).getEdgeAttributes(a);
            if (ByteBufferList.canCopy(dl)) {
            	ByteBufferList copy = ByteBufferList.createByteBufferCopy(dl);
                ((RemoteIndexedLineSet) dst).setEdgeAttributes(a, copy);
                ByteBufferList.releaseList(copy);
            } else {
                ((RemoteIndexedLineSet) dst).setEdgeAttributes(a, dl);
            }
        }
        for (Iterator i = ev.getChangedVertexAttributes().iterator(); i
                .hasNext();) {
            Attribute a = (Attribute) i.next();
            DataList dl = ((PointSet) src).getVertexAttributes(a);
            if (ByteBufferList.canCopy(dl)) {
            	ByteBufferList copy = ByteBufferList.createByteBufferCopy(dl);
                ((RemotePointSet) dst).setVertexAttributes(a, copy);
                ByteBufferList.releaseList(copy);
            } else {
                ((RemotePointSet) dst).setVertexAttributes(a, dl);
            }
        }
        for (Iterator i = ev.getChangedGeometryAttributes().iterator(); i
                .hasNext();) {
            Attribute a = (Attribute) i.next();
            dst.setGeometryAttributes(a, src.getGeometryAttributes(a));
        }
    }

		public void childAdded(SceneContainerEvent ev) {
   			((RemoteSceneGraphComponent)rmc.getProxyImpl(ev.getParentElement()))
            .add((RemoteSceneGraphNode) rmc.createProxyScene(ev.getNewChildElement()));
	}

	public void childRemoved(SceneContainerEvent ev) {
        ((RemoteSceneGraphComponent)rmc.getProxyImpl(ev.getParentElement()))
        .remove((RemoteSceneGraphNode) rmc.getProxyImpl(ev.getOldChildElement()));
	}

	public void childReplaced(SceneContainerEvent ev) {
		childRemoved(ev); childAdded(ev);
	}
  
}
