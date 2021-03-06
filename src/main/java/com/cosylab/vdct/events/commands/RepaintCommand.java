package com.cosylab.vdct.events.commands;

/**
 * Copyright (c) 2002, Cosylab, Ltd., Control System Laboratory, www.cosylab.com
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer. 
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation 
 * and/or other materials provided with the distribution. 
 * Neither the name of the Cosylab, Ltd., Control System Laboratory nor the names
 * of its contributors may be used to endorse or promote products derived 
 * from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE 
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE 
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.util.Iterator;

import com.cosylab.vdct.events.Command;
import com.cosylab.vdct.graphics.RepaintInterface;
import com.cosylab.vdct.graphics.objects.Group;

/**
 * Repaints the component in various ways. If "all" repaint is used, also
 * revalidates all visible objects.
 * 
 * Creation date: (21.12.2000 22:42:23)
 * @author Matej Sekoranja
 */
public class RepaintCommand extends Command {
	private RepaintInterface component;
	private boolean all = false;
	private boolean highlighted = false;
/**
 * Insert the method's description here.
 * Creation date: (21.12.2000 22:43:26)
 * @param component javax.swing.JComponent
 */
public RepaintCommand(RepaintInterface component) {
	this(component, false, false);
}

    /**
     *
     * @param component component
     * @param all all
     * @param highlighted highlighted
     */
    public RepaintCommand(RepaintInterface component, boolean all, boolean highlighted) {
	this.component=component;
	this.all = all;
	this.highlighted = highlighted;
}
/**
 * Insert the method's description here.
 * Creation date: (21.12.2000 22:42:23)
 */
public void execute() {
	if (all) {
		Iterator iterator = Group.getAllRoots().iterator();
		while (iterator.hasNext()) {
			((Group)iterator.next()).unconditionalValidateSubObjects(false);
		}
		component.repaintAll(highlighted);
	} else {
		component.repaint(highlighted);
	}
}
}
