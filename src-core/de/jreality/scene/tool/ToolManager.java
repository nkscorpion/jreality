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


package de.jreality.scene.tool;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphNode;
import de.jreality.scene.SceneGraphPath;

/**
 * 
 * @author weissman
 *  
 */
public class ToolManager {

    private final HashSet toolsWithPick = new HashSet();
    private final HashMap toolToPaths = new HashMap();
    
    boolean addTool(Tool tool, SceneGraphPath path) {
    	boolean first = pathsForTool(tool).isEmpty();
        if (!pathsForTool(tool).contains(path))
        	pathsForTool(tool).add(path);	// clone path, perhaps?
        else
            throw new IllegalStateException("Tool "+tool+" already registered with path="+path);
        if (!tool.getActivationSlots().isEmpty() && !toolsWithPick.contains(tool))
        	toolsWithPick.add(tool);
        return first;
    }
    
    boolean removeTool(Tool tool, SceneGraphPath path) {
      if (pathsForTool(tool).contains(path))
        pathsForTool(tool).remove(path);
      else
        throw new IllegalStateException();
      if (pathsForTool(tool).isEmpty()) {
      	if (!tool.getActivationSlots().isEmpty())
          toolsWithPick.remove(tool);
        return true;
      }
      return false;
    }
    
    void cleanUp() {
        toolToPaths.clear();
        toolsWithPick.clear();        
    }

    /**
     * @return all tools in the viewer's scene
     */
    Set getTools() {
        return Collections.unmodifiableSet(toolToPaths.keySet());
    }

    /**
     * @param candidate
     * @return
     */
    boolean needsPick(Tool candidate) {
        return toolsWithPick.contains(candidate);
    }

    private List pathsForTool(Tool t) {
        if (!toolToPaths.containsKey(t)) {
            toolToPaths.put(t, new LinkedList());
        }
        return (List) toolToPaths.get(t);
    }

    SceneGraphPath getPathForTool(Tool tool, SceneGraphPath pickPath) {
        if (pickPath == null) {
            if (pathsForTool(tool).size() != 1)
                    throw new IllegalStateException(
                            "ambigous path without pick");
            return (SceneGraphPath) pathsForTool(tool).get(0);
        }
        for (Iterator i = pathsForTool(tool).iterator(); i.hasNext();) {
            SceneGraphPath path = (SceneGraphPath) i.next();
            if (pickPath.startsWith(path)) return path;
        }
        return null;
    }

    Collection selectToolsForPath(SceneGraphPath pickPath, int depth, HashSet candidates) {  
        for(Iterator iter = pickPath.reverseIterator(depth); iter.hasNext();) {
            SceneGraphNode node = (SceneGraphNode) iter.next();
            List tools;
            if (node instanceof SceneGraphComponent) {
              tools = ((SceneGraphComponent) node).getTools();
            } else continue;
            List copy = new LinkedList();
            copy.addAll(tools);
            copy.retainAll(candidates);
            if (!copy.isEmpty()) return copy;
        }
        return Collections.EMPTY_SET;
    }

}
