package com.cosylab.vdct.graphics.objects;

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

import java.awt.*;
import java.util.*;
import com.cosylab.vdct.Constants;
import com.cosylab.vdct.graphics.*;
import com.cosylab.vdct.vdb.*;
import com.cosylab.vdct.dbd.DBDConstants;

import com.cosylab.vdct.inspector.*;

import com.cosylab.vdct.graphics.popup.*;
import javax.swing.*;
import java.awt.event.*;

import com.cosylab.vdct.events.*;
import com.cosylab.vdct.events.commands.*;

/**
 * Insert the type's description here.
 * Creation date: (21.12.2000 20:46:35)
 * @author Matej Sekoranja
 */
public abstract class LinkManagerObject extends ContainerObject implements Hub, Inspectable, Popupable {

	class PopupMenuHandler implements ActionListener {
		public void actionPerformed(ActionEvent e) {
		    LinkCommand cmd = (LinkCommand)CommandManager.getInstance().getCommand("LinkCommand");
		    cmd.setData(LinkManagerObject.this, LinkManagerObject.this.getField(e.getActionCommand()));
	 		cmd.execute();
		}
	}

	public final static String nullString = "";

	// GUI linking support
    private boolean target = false;
	public final static String inlinkString = "INLINK";
	public final static String outlinkString = "OUTLINK";
	public final static String fwdlinkString = "FWDLINK";
	public final static String varlinkString = "VARIABLE";

/**
 * LinkManagerObject constructor comment.
 * @param parent com.cosylab.vdct.graphics.objects.ContainerObject
 */
public LinkManagerObject(ContainerObject parent)
{
	super(parent);
}

/**
 */
public abstract VDBFieldData getField(String name);

/**
 * Insert the method's description here.
 * Creation date: (27.1.2001 16:12:03)
 * @param field com.cosylab.vdct.vdb.VDBFieldData
 */
public abstract void fieldChanged(VDBFieldData field);

/**
 * Insert the method's description here.
 * Creation date: (5.2.2001 9:42:29)
 * @param e java.util.Enumeration list of VDBFieldData fields
 * @param prevGroup java.lang.String
 * @param group java.lang.String
 */
public void fixEPICSOutLinks(Enumeration e, String prevGroup, String group) {
	if (prevGroup.equals(group)) return;
	
	String prefix;
	if (group.equals(nullString)) prefix=nullString;
	else prefix=group+Constants.GROUP_SEPARATOR;

	String old; 
	int type; VDBFieldData field;
	while (e.hasMoreElements()) {
		field = (VDBFieldData)e.nextElement();
		type = LinkProperties.getType(field);
		if (type != LinkProperties.VARIABLE_FIELD) {
			old = field.getValue();
			if (!old.equals(nullString) && !old.startsWith(Constants.HARDWARE_LINK) &&
				old.startsWith(prevGroup)) {
				if (prevGroup.equals(nullString))
					field.setValue(prefix+old);
				else
					field.setValue(prefix+old.substring(prevGroup.length()+1));
			}
		}
	}

}
/**
 * Goes through link fields (in, out, var, fwd) and cheks
 * if ther are OK, if not it fixes it
 * When record is moved, renames, etc. value of in, out, fwd
 * should be changed, but visual link is still preserved :)
 * (linked list). It compares start point end end point and ...
 * Creation date: (2.5.2001 19:37:46)
 */
public void fixLinks() {

	Object unknownField;
	EPICSLinkOut source;
	EPICSVarLink varlink;
	String targetName;
	
	Enumeration e = getSubObjectsV().elements();
	while (e.hasMoreElements())
	{
		unknownField = e.nextElement();
			
			// go and find source
			if (unknownField instanceof EPICSVarLink)
			{
				varlink = (EPICSVarLink)unknownField;
				targetName = varlink.getFieldData().getFullName();

				
				Enumeration e2 = varlink.getStartPoints().elements();
				while (e2.hasMoreElements())
				{
					unknownField = e2.nextElement();
					if (unknownField instanceof EPICSLinkOut) 
						source = (EPICSLinkOut)unknownField;  
					else
						continue;	// nothing to fix

		
					// now I got source and target, compare values
					String oldTarget = LinkProperties.getTarget(source.getFieldData());
					if (!oldTarget.equalsIgnoreCase(targetName))
					{
						// not the same, fix it gently as a doctor :)
						String value = source.getFieldData().getValue();
						value = targetName + com.cosylab.vdct.util.StringUtils.removeBegining(value, oldTarget);
						source.getFieldData().setValueSilently(value);
						source.fixLinkProperties();
					}
				}
			}

/*
			else if (unknownField instanceof EPICSLinkOut)
			{
				source = (EPICSLinkOut)unknownField;
				InLink inlink = EPICSLinkOut.getEndPoint(source);
				if (inlink!=null && inlink instanceof EPICSVarLink)
				{
					varlink = (EPICSVarLink)inlink;
					targetName = varlink.getFieldData().getFullName();
					// now I got source and target, compare values
					String oldTarget = LinkProperties.getTarget(source.getFieldData());
					if (!oldTarget.equalsIgnoreCase(targetName))
					{
						// not the same, fix it gently as a doctor :)
						String value = source.getFieldData().getValue();
						value = targetName + com.cosylab.vdct.util.StringUtils.removeBegining(value, oldTarget);
						source.getFieldData().setValueSilently(value);
						source.fixLinkProperties();
					}
				}
			}

*/
			
	}
	
}

/**
 * Insert the method's description here.
 * Creation date: (30.1.2001 9:36:15)
 * @return boolean
 * @param field com.cosylab.vdct.vdb.VDBFieldData
 */
public boolean manageLink(VDBFieldData field) {

	int type = LinkProperties.getType(field);
	if (type == LinkProperties.VARIABLE_FIELD)
	{
		if (this.containsObject(field.getName()))
		{
			EPICSVarLink link = (EPICSVarLink)getSubObject(field.getName());
			link.validateLink();
			return true;			
		}
		return false;
	}	
	else
	{
		
		if (this.containsObject(field.getName()))
		{
			// existing link
			EPICSLinkOut link = (EPICSLinkOut)getSubObject(field.getName());
			link.valueChanged();
			link.setDestroyed(false);
			return true;
			
		}
		else
		{
			if (field.getValue().startsWith(Constants.HARDWARE_LINK) ||
				field.getValue().startsWith("@") ||    // !!!??
				field.getValue().equals(nullString) ||
				Character.isDigit(field.getValue().charAt(0))) 
				return false; 	//!!!
			// new link
			LinkProperties properties = new LinkProperties(field);
			InLink varlink = EPICSLinkOut.getTarget(properties);
			// can point to null? OK, cross will be showed

			EPICSLinkOut outlink = null;
			
			if (type==LinkProperties.INLINK_FIELD)
				outlink = new EPICSInLink(this, field);
			else if (type==LinkProperties.OUTLINK_FIELD)
				outlink = new EPICSOutLink(this, field);
			else /*if (type==LinkProperties.FWDLINK_FIELD)*/
				outlink = new EPICSFwdLink(this, field);
		
			addLink(outlink);
			/*if (!properties.isIsInterGroupLink())
			{
				String id = EPICSLinkOut.generateConnectorID(outlink);
				Connector connector = new Connector(id, this, outlink, varlink);
				if (varlink!=null)
				{
					connector.setX((outlink.getOutX()+varlink.getInX())/2);
					connector.setY((outlink.getOutY()+varlink.getInY())/2);
				}
				addSubObject(id, connector);
			}
			else*/
			{
				if (varlink!=null) varlink.setOutput(outlink, null);
				outlink.setInput(varlink);
			}

			return true;
		}
	}
}
/**
 * Insert the method's description here.
 * Creation date: (1.2.2001 17:38:36)
 * @param dx int
 * @param dy int
 */
public void moveConnectors(int dx, int dy) {
	
  ViewState view = ViewState.getInstance();
  Enumeration e = subObjectsV.elements();
  Connector con; Object obj;
  while (e.hasMoreElements()) {
	obj = e.nextElement();
	if (obj instanceof Connector) {
		con = (Connector)obj;
		InLink endpoint = EPICSLinkOut.getEndPoint(con);
		/*OutLink startpoint = EPICSLinkOut.getStartPoint(con);
		EPICSLinkOut lo = null;
		if (!(startpoint instanceof EPICSLinkOut))
			lo = (EPICSLinkOut)startpoint;*/
		if (((endpoint instanceof EPICSLink) &&
			(view.isSelected(((EPICSLink)endpoint).getParent())) /*||
			((lo!=null) && lo.getLinkProperties().isIsInterGroupLink())*/)
			||
			((endpoint instanceof LinkManagerObject) && view.isSelected(endpoint)))
			con.move(dx, dy);
	}
  }
}

/**
 * Insert the method's description here.
 * Creation date: (21.12.2000 21:58:56)
 * @param g java.awt.Graphics
 * @param hilited boolean
 */
public void postDraw(Graphics g, boolean hilited) {
	Enumeration e = subObjectsV.elements();
	VisibleObject vo;
	while (e.hasMoreElements()) {
		vo = (VisibleObject)(e.nextElement());
		if (vo instanceof Connector)
			vo.paint(g, hilited);
	}
	
}

/**
 * Insert the method's description here.
 * Creation date: (3.2.2001 13:25:42)
 * @return boolean
 */
public boolean isTarget() {
	return target;
}

/**
 * Insert the method's description here.
 * Creation date: (3.2.2001 13:25:42)
 * @param newTarget boolean
 */
public void setTarget(boolean newTarget) {
	target = newTarget;
}

/**
 * Insert the method's description here.
 * Creation date: (2.2.2001 20:31:29)
 * @return java.util.Vector
 */
public Vector getLinkMenus(Enumeration vdbFields) {
	Vector items = new Vector();
	ActionListener l = createPopupmenuHandler();
	VDBFieldData field;
	JMenuItem menuitem;
	
	if (isTarget()) {
		int count = 0;
		JMenu varlinkItem = new JMenu(varlinkString);
		JMenu menu = varlinkItem;
		
		while (vdbFields.hasMoreElements()) {
			field = (VDBFieldData)(vdbFields.nextElement());
/*			switch (field.getType()) {
				case DBDConstants.DBF_CHAR: 
				case DBDConstants.DBF_UCHAR: 
				case DBDConstants.DBF_SHORT: 
				case DBDConstants.DBF_USHORT: 
				case DBDConstants.DBF_LONG: 
				case DBDConstants.DBF_ULONG: 
				case DBDConstants.DBF_FLOAT: 
				case DBDConstants.DBF_DOUBLE: 
				case DBDConstants.DBF_STRING:
				case DBDConstants.DBF_NOACCESS:		// added by request of APS
				case DBDConstants.DBF_ENUM:
				case DBDConstants.DBF_MENU:
				case DBDConstants.DBF_DEVICE:  // ?
				  menuitem = new JMenuItem(field.getName());
				  menuitem.addActionListener(l);
				  menu = PopUpMenu.addItem(menuitem, menu, count);
				  count++; 
			}
*/
			if (field.getType()!=DBDConstants.DBF_INLINK &&
				field.getType()!=DBDConstants.DBF_OUTLINK &&
				field.getType()!=DBDConstants.DBF_FWDLINK)
			{
				  menuitem = new JMenuItem(field.getName());
				  menuitem.addActionListener(l);
				  menu = PopUpMenu.addItem(menuitem, menu, count);
				  count++; 
			}

		}
		if (count > 0) items.addElement(varlinkItem);
		
	}
	else {
		
		JMenu inlinks = new JMenu(inlinkString);
		JMenu outlinks = new JMenu(outlinkString);
		JMenu fwdlinks = new JMenu(fwdlinkString);
		
		//boolean isSoft = recordData.canBePV_LINK(); !!! can be added

		JMenu inMenu = inlinks;	
		JMenu outMenu = outlinks;	
		JMenu fwdMenu = fwdlinks;	
		
		int inpItems, outItems, fwdItems;
		inpItems=outItems=fwdItems=0;

		while (vdbFields.hasMoreElements()) {
			field = (VDBFieldData)(vdbFields.nextElement());
			if (field.getValue().equals(nullString)) {
				switch (field.getType()) {
					case DBDConstants.DBF_INLINK:
						 menuitem = new JMenuItem(field.getName());
						 menuitem.addActionListener(l);
						 inlinks = PopUpMenu.addItem(menuitem, inlinks, inpItems); 
						 inpItems++;
						 break;
					case DBDConstants.DBF_OUTLINK: 
						 menuitem = new JMenuItem(field.getName());
						 menuitem.addActionListener(l);
						 outlinks = PopUpMenu.addItem(menuitem, outlinks, outItems); 
						 outItems++;
						 break;
					case DBDConstants.DBF_FWDLINK:
						 menuitem = new JMenuItem(field.getName());
						 menuitem.addActionListener(l);
						 fwdlinks = PopUpMenu.addItem(menuitem, fwdlinks, fwdItems); 
						 fwdItems++;
						 break;
				}
			}
		}

		if (inMenu.getItemCount() > 0)
			items.addElement(inMenu);
		if (outMenu.getItemCount() > 0)
			items.addElement(outMenu);
		if (fwdMenu.getItemCount() > 0)
			items.addElement(fwdMenu);

	}
		
	return items;
}

/**
 * Insert the method's description here.
 * Creation date: (2.2.2001 23:00:51)
 * @return com.cosylab.vdct.graphics.objects.LinkManagerObject.PopupMenuHandler
 */
private com.cosylab.vdct.graphics.objects.LinkManagerObject.PopupMenuHandler createPopupmenuHandler() {
	return new PopupMenuHandler();
}



}