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


package de.jreality.toolsystem.raw;

import de.jreality.scene.Viewer;
import de.jreality.scene.tool.AxisState;
import de.jreality.scene.tool.InputSlot;
import de.jreality.toolsystem.ToolEvent;
import de.jreality.toolsystem.ToolEventQueue;

/**
 * @author weissman
 *
 **/
public class DeviceSystemTimer implements RawDevice, PollingDevice {

    static class MyToolEvent extends ToolEvent {
    	private static final long serialVersionUID = -1752817756822635827L;
		
    	public MyToolEvent(Object source, InputSlot device, AxisState axis) {
			super(source, device, axis);
		}
			protected boolean compareAxisStates(AxisState axis1, AxisState axis2) {
                 return true;
             }
             protected void replaceWith(ToolEvent replacement) {
               this.axis = new AxisState(this.axis.intValue() + replacement.getAxisState().intValue());
               this.trafo = replacement.getTransformation();
               this.time = replacement.getTimeStamp();
           }
	}

	private ToolEventQueue queue;
    
    String myDeviceName = "tick";
    
    private InputSlot device;
    
    long lastEvent = -1l;
    
    public ToolEvent mapRawDevice(String rawDeviceName, InputSlot inputDevice) {
        if (!rawDeviceName.equals(myDeviceName)) throw new IllegalArgumentException("no such raw axis");
        device = inputDevice;
        return new ToolEvent(this, inputDevice, AxisState.ORIGIN);
    }

    public void poll() {
      if (queue == null) return;
      long ct = System.currentTimeMillis();
      int delta = (int)(lastEvent == -1l ? 0 : ct - lastEvent);
      lastEvent = ct;
      ToolEvent e = new MyToolEvent(this, device, new AxisState(delta));
      queue.addEvent(e);
    }

    public void setEventQueue(ToolEventQueue queue) {
        this.queue = queue;
    }

    public void dispose() {
    }

    public void initialize(Viewer viewer) {
    }

    public String getName() {
        return "SystemTimer";
    }
    
    public String toString() {
      return "RawDevice: SystemTimer";
    }

}
