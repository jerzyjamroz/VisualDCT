/*
 * Copyright (c) 2004 by Cosylab d.o.o.
 *
 * The full license specifying the redistribution, modification, usage and other
 * rights and obligations is included with the distribution of this project in
 * the file license.html. If the license is not included you may find a copy at
 * http://www.cosylab.com/legal/abeans_license.htm or may write to Cosylab, d.o.o.
 *
 * THIS SOFTWARE IS PROVIDED AS-IS WITHOUT WARRANTY OF ANY KIND, NOT EVEN THE
 * IMPLIED WARRANTY OF MERCHANTABILITY. THE AUTHOR OF THIS SOFTWARE, ASSUMES
 * _NO_ RESPONSIBILITY FOR ANY CONSEQUENCE RESULTING FROM THE USE, MODIFICATION,
 * OR REDISTRIBUTION OF THIS SOFTWARE.
 */

package com.cosylab.vdct.appplugin;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragGestureListener;
import java.awt.dnd.DragSource;
import java.awt.dnd.DragSourceAdapter;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;

import java.io.IOException;

import java.util.Arrays;

import javax.swing.DefaultListModel;
import javax.swing.JList;


/**
 * <code>ArchiverList</code> holds all available Records that can be dragged
 * into the <code>ArchiverTree</code>.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 * @version $Id$
 *
 * @since VERSION
 */
public class AppList extends JList
{
	private AppTreeNode[] draggedValues;

	/**
	 * Creates a new ArchiverList object.
	 */
	public AppList()
	{
		super();
		initialize();
	}

	private void initialize()
	{
		initializeAsDragSource();
		new DropTarget(this, new DropTargetListHandler());

		DefaultListModel model = new DefaultListModel();
		this.setModel(model);
	}

	private void initializeAsDragSource()
	{
		DragSource dragSource = DragSource.getDefaultDragSource();
		dragSource.createDefaultDragGestureRecognizer(this,
		    DnDConstants.ACTION_COPY_OR_MOVE,
		    new DragGestureListener() {
				/*
				 *  (non-Javadoc)
				 * @see java.awt.dnd.DragGestureListener#dragGestureRecognized(java.awt.dnd.DragGestureEvent)
				 */
				public void dragGestureRecognized(DragGestureEvent event)
				{
					draggedValues = getSelectedRecords();

					//				    draggedValues = getSelectedValues();
					Transferable transferable = new AppTransferable(draggedValues);
					event.startDrag(new Cursor(Cursor.MOVE_CURSOR),
					    transferable,
					    new DragSourceAdapter() {
							/*
							 *  (non-Javadoc)
							 * @see java.awt.dnd.DragSourceListener#dragDropEnd(java.awt.dnd.DragSourceDropEvent)
							 */
							public void dragDropEnd(DragSourceDropEvent dsde)
							{
								if (dsde.getDropSuccess()) {
									int action = dsde.getDropAction();

									if (action == DnDConstants.ACTION_MOVE) {
										for (int i = 0;
										    i < draggedValues.length; i++) {
											getDefaultModel().removeElement(draggedValues[i]);
										}
									}
								}
							}
						});
				}
			});
	}

	/**
	 * Returns the DefaultListModel of this list.
	 *
	 * @return the model
	 */
	public DefaultListModel getDefaultModel()
	{
		return (DefaultListModel)getModel();
	}

	/**
	 * Returns the selected records as an array of
	 * <code>ArchiverTreeRecordNode</code>.
	 *
	 * @return selected records
	 */
	public AppTreeNode[] getSelectedRecords()
	{
		Object[] objects = getSelectedValues();
		AppTreeNode[] records = new AppTreeNode[objects.length];
		System.arraycopy(objects, 0, records, 0, objects.length);

		return records;
	}

	private class DropTargetListHandler extends DropTargetAdapter
	{
		private boolean isDragAcceptable(DropTargetDragEvent dtde)
		{
			DataFlavor[] f = dtde.getCurrentDataFlavors();

			boolean accept = false;

			for (int i = 0; i < f.length; i++) {
				if (Arrays.asList(AppTransferable.flavors).contains(f[i])) {
					accept = true;

					break;
				}
			}

			return (accept
			& (dtde.getDropAction() & DnDConstants.ACTION_MOVE) != 0);
		}

		private boolean isDropAcceptable(DropTargetDropEvent dtde)
		{
			DataFlavor[] f = dtde.getCurrentDataFlavors();
			boolean accept = false;

			for (int i = 0; i < f.length; i++) {
				if (Arrays.asList(AppTransferable.flavors).contains(f[i])) {
					accept = true;

					break;
				}
			}

			return (accept
			& (dtde.getDropAction() & DnDConstants.ACTION_MOVE) != 0);
		}

		/* (non-Javadoc)
		 * @see java.awt.dnd.DropTargetListener#dragEnter(java.awt.dnd.DropTargetDragEvent)
		 */
		public void dragEnter(DropTargetDragEvent dtde)
		{
			if (!isDragAcceptable(dtde)) {
				dtde.rejectDrag();

				return;
			}
		}

		/* (non-Javadoc)
		 * @see java.awt.dnd.DropTargetListener#dropActionChanged(java.awt.dnd.DropTargetDragEvent)
		 */
		public void dropActionChanged(DropTargetDragEvent dtde)
		{
			if (!isDragAcceptable(dtde)) {
				dtde.rejectDrag();

				return;
			}
		}

		/* (non-Javadoc)
		 * @see java.awt.dnd.DropTargetListener#drop(java.awt.dnd.DropTargetDropEvent)
		 */
		public void drop(DropTargetDropEvent dtde)
		{
			if (!isDropAcceptable(dtde)) {
				dtde.rejectDrop();

				return;
			}

			dtde.acceptDrop(DnDConstants.ACTION_MOVE);

			Transferable transferable = dtde.getTransferable();

			DataFlavor[] flavors = transferable.getTransferDataFlavors();
			Point location = dtde.getLocation();

			for (int i = 0; i < flavors.length; i++) {
				DataFlavor df = flavors[i];

				try {
					if (df.equals(AppTransferable.flavors[0])) {
						AppTreeNode[] nodes = (AppTreeNode[])transferable
							.getTransferData(df);

						for (int j = 0; j < nodes.length; j++) {
						    Point p = indexToLocation(getDefaultModel().size()-1);
						    int index = 0;
						    if (location.y > p.y) {
						        index = getDefaultModel().size();
						    } else {
						        index = locationToIndex(location);
						    }
						    AppTreeElement elem = nodes[j].getTreeUserElement();
						    if (elem instanceof Channel) {
						        getDefaultModel().add(index,
								    nodes[j]);
						    } else if (elem instanceof Group) {
						        addRecordsFromGroupNode(nodes[j], index);
						    }
						}
					}
				} catch (UnsupportedFlavorException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			dtde.dropComplete(true);
		}
	}
	
	private void addRecordsFromGroupNode(AppTreeNode groupNode, int location) {
	    
	    AppTreeNode node;
	    AppTreeElement elem;
	    for (int i = 0; i < groupNode.getChildCount(); i++) {
	        node = (AppTreeNode) groupNode.getChildAt(i);
	        elem = node.getTreeUserElement();
	        if (elem instanceof Channel) {
				getDefaultModel().add(location, node);
	        } else if (elem instanceof Group) {
	            addRecordsFromGroupNode(node, location);
	        }
	        
	    }
	    
	}
}

/* __oOo__ */
