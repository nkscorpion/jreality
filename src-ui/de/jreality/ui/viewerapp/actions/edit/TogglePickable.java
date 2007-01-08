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


package de.jreality.ui.viewerapp.actions.edit;

import java.awt.event.ActionEvent;

import de.jreality.scene.Geometry;
import de.jreality.shader.CommonAttributes;
import de.jreality.ui.viewerapp.SelectionEvent;
import de.jreality.ui.viewerapp.SelectionManager;
import de.jreality.ui.viewerapp.actions.AbstractSelectionListenerAction;
import de.jreality.util.PickUtility;


/**
 * Toggles the pickable property of a selected SceneGraphComponent 
 * (if no SceneGraphComponent is selected, this action is disabled).
 * 
 * @author msommer
 */
public class TogglePickable extends AbstractSelectionListenerAction {
  
  public TogglePickable(String name, SelectionManager sm) {
    super(name, sm);
    setShortDescription("Toggle pickability of selection");
  }
  
//  public TogglePickable(String name, ViewerApp v) {
//    this(name, v.getSelectionManager());
//  }
  
  @Override
  public void actionPerformed(ActionEvent e) {
    Geometry g = getSelection().getLastComponent().getGeometry();
    if (g!=null) {
      Boolean b = (Boolean)g.getGeometryAttributes(CommonAttributes.PICKABLE);
      if (b==null) b = true;  //default
      PickUtility.setPickable(g, !b);
    }
  }

  @Override
  public boolean isEnabled(SelectionEvent e) {
    return e.componentSelected();
  }
  
}