/*
 * Created on 28-Feb-2005
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
package de.jreality.reader;

import junit.framework.TestCase;


/**
 *
 * TODO: comment this
 *
 * @author weissman
 *
 */
public class OBJReaderTest extends TestCase {

    public static void main(String[] args) {
        junit.swingui.TestRunner.run(OBJReaderTest.class);
    }
    
    public void testOBJReader() throws Exception {
        //String fileName = "/home/gollwas/bolt1.obj";
        //String fileName = "/home/gollwas/cube2.obj";
        String fileName = "/home/gollwas/obj/cessna.obj";
        OBJReader.readFromFile(fileName);
    }

//    public void testMTLReader() throws Exception {
//        String fileName = "/home/gollwas/Buddy-Mesh.mtl";
//        System.out.println(MTLReader.readFromFile(fileName));
//    }

}
