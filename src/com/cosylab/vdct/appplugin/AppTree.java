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

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
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
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.AbstractCellEditor;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellEditor;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;


/**
 * <code>ArchiverTree</code> is an archiver for EPICS records.   This object
 * enables Drag&Drap actions of the ArchiverTreeRecordNodes.
 *
 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
 * @version $Id$
 *
 * @since VERSION
 */
public abstract class AppTree extends JTree
{
	protected JPopupMenu popup;
	protected AppTreeNode rootNode;
	private CellRenderer cellRenderer;
	protected DefaultEditor defaultEditor;
	private AppTreeChannelNode[] draggedValues;
	protected ArrayList treeListeners = new ArrayList();
	protected AppFrame appFrame;

	/**
	 * Creates a new ArchiverTree object.
	 *
	 * @param app DOCUMENT ME!
	 */
	public AppTree(AppFrame app)
	{
		super();
		this.appFrame = app;
		initialize();
	}

	private void initialize()
	{
		new DropTarget(this, new DropTargetTreeHandler());
		initializeAsDragSource();

		addMouseListener(new TreeMouseHandler());
		cellRenderer = new CellRenderer();
		defaultEditor = new DefaultEditor();
		this.setCellRenderer(cellRenderer);
		this.setEditable(true);
		this.setCellEditor(new CellEditor(this, cellRenderer, defaultEditor));
		initialization();
	}

	protected abstract void initialization();

	/**
	 * Returns DefaultTreeModel of this tree.
	 *
	 * @return the model
	 */
	public DefaultTreeModel getDefaultModel()
	{
		return (DefaultTreeModel)getModel();
	}

	/**
	 * Sets the root for this tree.
	 *
	 * @param root
	 */
	public void setRoot(AppTreeNode root)
	{
		this.rootNode = root;
		getDefaultModel().setRoot(root);
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @return DOCUMENT ME!
	 */
	public AppTreeNode getRoot()
	{
		return rootNode;
	}

	/**
	 * DOCUMENT ME!
	 */
	public void reset()
	{
		stopEditing();

		getDefaultModel().setRoot(rootNode);
		getDefaultModel().reload();
	}

	protected void fireChannelRemoved(AppTreeChannelNode[] channel)
	{
		ChannelRemovedEvent evt = new ChannelRemovedEvent(this, channel);

		for (int i = 0; i < treeListeners.size(); i++) {
			((TreeListener)treeListeners.get(i)).channelRemoved(evt);
		}
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @param listener DOCUMENT ME!
	 */
	public void addTreeListener(TreeListener listener)
	{
		treeListeners.add(listener);
	}

	/**
	 * DOCUMENT ME!
	 *
	 * @param listener DOCUMENT ME!
	 */
	public void removeTreeListener(TreeListener listener)
	{
		treeListeners.remove(listener);
	}

	/**
	 * Construct the static part of the popup menu. Adds only those items that
	 * are present all the time.
	 *
	 * @return
	 */
	protected JPopupMenu getPopup()
	{
		if (popup == null) {
			popup = new JPopupMenu();
		}

		return popup;
	}

	/**
	 * Prepares the popup for the specific item. This method prepares the popup
	 * items for the currently selected path.
	 *
	 * @param path
	 */
	protected abstract void popupInit(TreePath path);

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
					final TreePath[] paths = getSelectionModel()
						.getSelectionPaths();
					ArrayList values = new ArrayList();

					if (paths != null) {
						for (int i = 0; i < paths.length; i++) {
							int count = paths[i].getPathCount();

							for (int j = 0; j < count; j++) {
								Object ob = paths[i].getPathComponent(j);

								if (ob != null
								    && ob instanceof AppTreeChannelNode) {
									values.add(ob);
								}
							}
						}
					}

					draggedValues = new AppTreeChannelNode[values.size()];
					values.toArray(draggedValues);

					Transferable transferable = new RecordTransferable(draggedValues);
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
											((DefaultTreeModel)getModel())
											.removeNodeFromParent((AppTreeNode)draggedValues[i]);
										}
									}
								}
							}
						});
				}
			});
	}

	/**
	 * Returns all selected ArchiverTreeRecordNodes.
	 *
	 * @return array of ArchiverTreeRecordNode.
	 */
	public AppTreeChannelNode[] getSelectionRecords()
	{
		ArrayList nodes = new ArrayList();
		TreePath[] paths = getSelectionPaths();

		if (paths == null) {
			return null;
		}

		for (int i = 0; i < paths.length; i++) {
			AppTreeNode lastNode = (AppTreeNode)paths[i].getLastPathComponent();

			if (lastNode instanceof AppTreeChannelNode) {
				nodes.add(lastNode);
			}
		}

		AppTreeChannelNode[] recordNode = new AppTreeChannelNode[nodes.size()];

		return (AppTreeChannelNode[])nodes.toArray(recordNode);
	}

	/**
	 * Adds ArchiverTreeRecordNodes as records to the selected path.
	 *
	 * @param records nodes to be added
	 *
	 * @return true if nodes were added, false if the selected path was not an
	 *         AppTreeNode containing an ArchiverTreeGroup
	 */
	boolean addRecords(AppTreeChannelNode[] records)
	{
		AppTreeNode group = findClosestGroupNode(getSelectionPath());

		if (group == null) {
			JOptionPane.showMessageDialog(appFrame,
			    "En element inside a group has to be selected \n in order to add a channel.",
			    "Invalid selection", JOptionPane.WARNING_MESSAGE);

			return false;
		}

		addElements(records, group);

		return true;
	}

	/**
	 * Adds ArchiverTreeRecordNodes to the parent node.
	 *
	 * @param children nodes to be added
	 * @param parent parent node for the children
	 */
	void addElements(AppTreeChannelNode[] children, AppTreeNode parent)
	{
		for (int j = 0; j < children.length; j++) {
			parent.add(children[j]);
		}

		((DefaultTreeModel)getModel()).reload(parent);
	}

	/**
	 * Finds the node in the path, which holds an ArchiverTreeGroup as a user
	 * object.
	 *
	 * @param path TreePath to be searched
	 *
	 * @return node of the ArchiverTreeGroup
	 */
	private AppTreeNode findClosestGroupNode(TreePath path)
	{
		AppTreeNode node;
		Object object;

		if (path != null) {
			for (int i = path.getPathCount() - 1; i >= 0; i--) {
				node = (AppTreeNode)path.getPathComponent(i);
				object = node.getUserObject();

				if (object instanceof Group) {
					return node;
				}
			}
		}

		return null;
	}

	/**
	 * <code>TreeMouseHandler</code> handles mouse events on the tree.
	 *
	 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
	 * @version $Id$
	 *
	 * @since VERSION
	 */
	private class TreeMouseHandler extends MouseAdapter
	{
		/*
		 *  (non-Javadoc)
		 * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
		 */
		public void mouseReleased(MouseEvent e)
		{
			if ((e.getClickCount() == 3) && SwingUtilities.isLeftMouseButton(e)) {
				stopEditing();
			}

			if ((e.getClickCount() == 2) && SwingUtilities.isLeftMouseButton(e)) {
				TreePath path = getPathForLocation(e.getX(), e.getY());

				if (path != null) {
					AppTreeNode node = (AppTreeNode)path.getLastPathComponent();

					if (node.getTreeUserElement().isEditable()) {
						AppTree.this.startEditingAtPath(path);
					}
				}
			} else if (e.getClickCount() == 1) {
				boolean ctrlDown = (e.getModifiersEx()
					& InputEvent.CTRL_DOWN_MASK) == InputEvent.CTRL_DOWN_MASK;

				if (SwingUtilities.isLeftMouseButton(e)) {
					// clear selection check, if CTRL is not down
					if (!ctrlDown) {
						TreePath p = getPathForLocation(e.getX(), e.getY());

						if (p == null) {
							getSelectionModel().clearSelection();
						}
					}
				} else if (SwingUtilities.isRightMouseButton(e)) {
					TreePath p = getPathForLocation(e.getX(), e.getY());

					if (p != null) {
						// reset/update selection, select currently clicked
						if (!getSelectionModel().isPathSelected(p)) {
							if (!ctrlDown) {
								getSelectionModel().clearSelection();
							}

							getSelectionModel().addSelectionPath(p);
						}
					} else {
						// clear selection
						getSelectionModel().clearSelection();
					}

					popupInit(p);
					getPopup().show(AppTree.this, e.getX(), e.getY());
				}
			}
		}
	}

	/**
	 * <code>DropTargetTreeHandler</code> handles DnD-Drop events on the tree.
	 *
	 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
	 * @version $Id$
	 *
	 * @since VERSION
	 */
	private class DropTargetTreeHandler extends DropTargetAdapter
	{
		private boolean isDragAcceptable(DropTargetDragEvent dtde)
		{
			DataFlavor[] f = dtde.getCurrentDataFlavors();

			boolean accept = false;

			for (int i = 0; i < f.length; i++) {
				if (Arrays.asList(RecordTransferable.flavors).contains(f[i])) {
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
				if (Arrays.asList(RecordTransferable.flavors).contains(f[i])) {
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

			Point point = dtde.getLocation();

			AppTreeNode groupNode = findClosestGroupNode(getClosestPathForLocation(
				        (int)(point.getX() + 0.5), (int)(point.getY() + 0.5)));

			if (groupNode != null) {
				for (int i = 0; i < flavors.length; i++) {
					DataFlavor df = flavors[i];

					try {
						if (df.equals(RecordTransferable.flavors[0])) {
							addElements((AppTreeChannelNode[])transferable
							    .getTransferData(df), groupNode);
						}
					} catch (UnsupportedFlavorException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			} else {
				JOptionPane.showMessageDialog(AppTree.this,
				    "This drop is not allowed!", "Invalid drop",
				    JOptionPane.WARNING_MESSAGE);

				return;
			}

			dtde.dropComplete(true);
		}
	}

	/**
	 * <code>CellRenderer</code> is a cell renderer for the tree. It sets the
	 * icon for the node and provides correct name fot it.
	 *
	 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
	 * @version $Id$
	 *
	 * @since VERSION
	 */
	private class CellRenderer extends DefaultTreeCellRenderer
		implements TreeCellRenderer
	{
		/* (non-Javadoc)
		 * @see javax.swing.tree.TreeCellRenderer#getTreeCellRendererComponent(javax.swing.JTree, java.lang.Object, boolean, boolean, boolean, int, boolean)
		 */
		public Component getTreeCellRendererComponent(JTree tree, Object value,
		    boolean selected, boolean expanded, boolean leaf, int row,
		    boolean hasFocus)
		{
			super.getTreeCellRendererComponent(tree, value, selected, expanded,
			    leaf, row, hasFocus);
			this.setBackground(new Color(255, 255, 225));

			if (value instanceof AppTreeNode) {
				AppTreeNode node = (AppTreeNode)value;
				this.setText(node.getTreeUserElement().toString());
				this.setIcon(CellUtilities.getIcon((AppTreeNode)value));
			}

			return this;
		}
	}

	/**
	 * <code>DefaultEditor</code> is a default editor for tree cell editing. It
	 * provides an empty EditorComponent and should be used together with
	 * CellEditor.
	 *
	 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
	 * @version $Id$
	 *
	 * @since VERSION
	 */
	protected class DefaultEditor extends AbstractCellEditor
		implements TreeCellEditor
	{
		private EditorComponent editor;
		private Object value;

		/**
		 * Creates a new DefaultEditor object.
		 */
		public DefaultEditor()
		{
			editor = new EditorComponent();
		}

		/* (non-Javadoc)
		 * @see javax.swing.tree.TreeCellEditor#getTreeCellEditorComponent(javax.swing.JTree, java.lang.Object, boolean, boolean, boolean, int)
		 */
		public Component getTreeCellEditorComponent(JTree tree, Object value,
		    boolean isSelected, boolean expanded, boolean leaf, int row)
		{
			this.value = value;

			return editor;
		}

		/* (non-Javadoc)
		 * @see javax.swing.CellEditor#getCellEditorValue()
		 */
		public Object getCellEditorValue()
		{
			// should never be invoked if there wasn't any interference
			return value;
		}

		/**
		 * DOCUMENT ME!
		 *
		 * @param component DOCUMENT ME!
		 */
		public void setEditorComponent(EditorComponent component)
		{
			this.editor = component;
		}
	}

	/**
	 * <code>CellEditor</code> is a cell editor for the tree. It enables the
	 * use of  EditorComponent.
	 *
	 * @author <a href="mailto:jaka.bobnar@cosylab.com">Jaka Bobnar</a>
	 * @version $Id$
	 *
	 * @since VERSION
	 */
	private class CellEditor extends DefaultTreeCellEditor
	{
		/**
		 * Creates a new CellEditor object.
		 *
		 * @param tree DOCUMENT ME!
		 * @param renderer DOCUMENT ME!
		 * @param editor DOCUMENT ME!
		 */
		public CellEditor(JTree tree, DefaultTreeCellRenderer renderer,
		    TreeCellEditor editor)
		{
			super(tree, renderer, editor);
		}

		/*
		 *  (non-Javadoc)
		 * @see javax.swing.tree.TreeCellEditor#getTreeCellEditorComponent(javax.swing.JTree, java.lang.Object, boolean, boolean, boolean, int)
		 */
		public Component getTreeCellEditorComponent(JTree tree,
		    final Object value, boolean isSelected, boolean expanded,
		    boolean leaf, int row)
		{
			if (!((AppTreeNode)value).getTreeUserElement().isEditable()) {
				return renderer.getTreeCellRendererComponent(tree, value,
				    isSelected, expanded, leaf, row, true);
			}

			setTree(tree);
			lastRow = row;
			determineOffset(tree, value, isSelected, expanded, leaf, row);

			if (editingComponent != null) {
				editingContainer.remove(editingComponent);
			}

			editingIcon = CellUtilities.getIcon((AppTreeNode)value);

			editingComponent = realEditor.getTreeCellEditorComponent(tree,
				    value, isSelected, expanded, leaf, row);

			((EditorComponent)editingComponent).setup((AppTreeNode)value, this,
			    AppTree.this);

			TreePath newPath = tree.getPathForRow(row);

			canEdit = ((lastPath != null) && (newPath != null)
				&& lastPath.equals(newPath));

			Font font = getFont();

			if (font == null) {
				if (renderer != null) {
					font = renderer.getFont();
				}

				if (font == null) {
					font = tree.getFont();
				}
			}

			editingContainer.setFont(font);
			prepareForEditing();

			return editingContainer;
		}

		/*
		 *  (non-Javadoc)
		 * @see javax.swing.CellEditor#getCellEditorValue()
		 */
		public Object getCellEditorValue()
		{
			try {
				return ((AppTreeNode)lastPath.getLastPathComponent())
				.getUserObject();
			} catch (Exception e) {
				//ignore ClassCastException - shouldn't happened if no 3rd party
				//objects interfered with the tree
			}

			return super.getCellEditorValue();
		}
	}
}

/* __oOo__ */
