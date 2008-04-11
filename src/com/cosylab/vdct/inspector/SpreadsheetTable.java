/**
 * Copyright (c) 2007, Cosylab, Ltd., Control System Laboratory, www.cosylab.com
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

package com.cosylab.vdct.inspector;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import com.cosylab.vdct.graphics.ColorChooser;
import com.cosylab.vdct.graphics.popup.PopUpMenu;

/**
 * @author ssah
 *
 */
public class SpreadsheetTable extends JTable implements ActionListener{

    private static Graphics2D graphics = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB).createGraphics();

    SpreadsheetInspector inspector = null;
	
	private HashMap nameToRecentSplitDataIndex = null;
    private JScrollPane pane = null;

    private JPopupMenu columnPopupMenu = null;
    private JPopupMenu rowPopupMenu = null;
    private JMenuItem hideItem = null; 
	private JMenu splitMenu = null; 
    private JMenuItem joinItem = null;
	private JMenuItem sortAscItem = null; 
	private JMenuItem sortDesItem = null; 
    private JComponent sortSeparator = null;
    private JMenuItem showAllRowsItem = null;
    private JMenuItem extendCountersItem = null;

    private JCheckBoxMenuItem[] propertiesColumnMenuItem = null;

    private JMenuItem showRowsItem = null;
    private JMenuItem hideRowsItem = null;
    private JMenuItem deleteRowsItem = null;

    private int popupModelRow = -1;
    private int popupModelColumn = -1;

    SpreadsheetColumnViewModel sprModel = null;
	
    private static final int firstColumnWidth = 20;
    private static final int minColumnWidth = 32;
    private static final int columnMargin = 16;
    
    /* The space from the border of the header column that represents the space where a drag
     * causes column resize instead of column move.
     * TODO: read this value from somewhere instead of setting it as a constant. 
     */
    private static final int columnMoveMargin = 3;    

    // The number of items in visibility submenus.
    private static final int visibilityMenuItemCount = 16;    

	private static final String hide = "Hide column"; 
	private static final String sortAsc = "Sort ascending"; 
	private static final String sortDes = "Sort descending"; 
	private static final String showAll = "Show all columns"; 
	private static final String hideAll = "Hide all columns"; 
	private static final String presetColumnOrders = "Preset column orders"; 
	
	private static final String visibility = "Column visibility"; 
	private static final String extendCounters = "Increase counters"; 
	private static final String showAllRowsString = "Show all rows";
	private static final String backgroundColorString = "Set background color...";
	private static final String defaultColorString = "Default background color";
	
	private static final String split = "Split column by"; 
	private static final String whitespaces = "Whitespaces";
	private static final String customPattern = "Custom...";
	private static final String join = "Join columns"; 

	private static final String hideRow = "Hide row"; 
	private static final String hideRows = "Hide rows"; 
	private static final String showRow = "Show row"; 
	private static final String showRows = "Show rows"; 
	private static final String deleteRow = "Delete row"; 
	private static final String deleteRows = "Delete rows"; 

    private static final String extendCountersToolTip = "Increase counters in selected region.";

    private static final String colorChooserTitle = "Set the table background color";

	public SpreadsheetTable(SpreadsheetInspector inspector, JScrollPane pane, Vector data) {
		super();
	    setName("ScrollPaneTable");
	
	    setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	    setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
	    setColumnSelectionAllowed(true);
	    setBackground(new Color(204, 204, 204));
	    setShowVerticalLines(true);
	    setGridColor(Color.black);
	    setBounds(0, 0, 200, 200);
	    setRowHeight(17);
	    
	    this.inspector = inspector;
	    this.pane = pane;
	    nameToRecentSplitDataIndex = new HashMap();
	}

	/** When selecting the fields in the first column, selects the whole row.
	 */
	public void changeSelection(int rowIndex, int columnIndex, boolean toggle, boolean extend) {
		
		super.changeSelection(rowIndex, columnIndex, toggle, extend);
		
		setColumnSelectionAllowed(true);
		if (getSelectedColumn() == 0) {
			setColumnSelectionAllowed(false);
		}
	}

	public SpreadsheetColumnViewModel getSpreadsheetModel() {
		return sprModel;
	}

	/* (non-Javadoc)
	 * @see javax.swing.JTable#setModel(javax.swing.table.TableModel)
	 */
	public void setModel(SpreadsheetColumnViewModel spreadsheetModel) {
		this.sprModel = spreadsheetModel;
		setColumnModel(sprModel);
		super.setModel(spreadsheetModel);
		addListeners();
	}

	public void refresh() {
		sprModel.refresh();
		refreshPopupMenus();
        refreshPropertiesColumnMenuItemsState();
        updateBackgroundColor();            
		updateSplitMenu();
		resizeColumns();
		repaint();
		getTableHeader().repaint();
	}
	
	public void resizeColumns() {
    	
    	int colCount = getColumnCount();
    	TableColumnModel colModel = getColumnModel();
    	SpreadsheetColumn column = null;
		
    	for (int i = 0; i < colCount; i++) {
    		column = (SpreadsheetColumn)colModel.getColumn(i);
    		if (column.isDefaultWidth()) {
    			setColumnSizeToFit(i);
    		}
    	}
    }
	
	public void setColumnSizeToFit(int columnIndex) {
		
    	FontMetrics metrics = graphics.getFontMetrics(getFont());

   		int colWidth = (columnIndex == 0) ? firstColumnWidth : minColumnWidth;
    		
    	SpreadsheetColumn column = (SpreadsheetColumn)getColumnModel().getColumn(columnIndex);

    	String value = column.getHeaderValue().toString();
		colWidth = Math.max(colWidth, metrics.stringWidth(value) + columnMargin);
    		
    	int rowCount = getRowCount();
   		for (int j = 0; j < rowCount; j++) {
   			value = getValueAt(j, columnIndex).toString();
   			colWidth = Math.max(colWidth, metrics.stringWidth(value) + columnMargin);
   		}
   		column.setPreferredWidth(colWidth);
   		column.setDefaultWidth(true);
	}
	
	public JTable getThis() {
		return this;
	}
    
	private void addListeners() {
	    
	    sprModel.setDefaultBackground(getBackground());
	    sprModel.setBackground(getBackground());
	    updateBackgroundColor(); 
	    
		getTableHeader().addMouseListener(new MouseAdapter() {
			
			private int draggedColumnModelIndex = -1;
			private int draggedColumnViewIndex = -1;
			
	        public void mouseClicked(MouseEvent event) {

	        	int columnIndex = columnAtPoint(event.getPoint());
	        	int columnModelIndex = convertColumnIndexToModel(columnIndex);
	        	int posX = event.getX();
	        	int posY = event.getY();
			    if (event.getButton() == MouseEvent.BUTTON1) {

			    	/* If this is click on the area between columns, set the default column size.
			    	 */ 
			    	Point pointToLeft = new Point(posX - columnMoveMargin, posY);
			    	Point pointToRight = new Point(posX + columnMoveMargin, posY);
			    	int nearbyIndex = columnAtPoint(pointToLeft);
			    	if (nearbyIndex == columnIndex || nearbyIndex == -1) {
			    		nearbyIndex = columnAtPoint(pointToRight);
			    	}
			    	if (nearbyIndex != columnIndex && nearbyIndex >= 0) {
			    		columnIndex = Math.min(columnIndex, nearbyIndex);
			    		setColumnSizeToFit(columnIndex);
			    	} else {
			    		/* If this is a click on the first column, switch showAllRows mode, otherwise change
						 * column sorting.
			    		 */ 
			    		if (columnModelIndex == 0) {
			    		    boolean newState = !sprModel.isShowAllRows(); 
			    		    sprModel.setShowAllRows(newState);
			    			showAllRowsItem.setSelected(newState);
			    			getTableHeader().repaint();
			    		} else {
			    			
			    			sprModel.sortRows(columnModelIndex);
			    			int a = 0;
			    			// TODO: remove
			    			//sprModel.updateSortedColumn(columnModelIndex);
			    		}
			    	}
	            } else if (event.getButton() == MouseEvent.BUTTON3) {
	            	displayPopupMenu(true, getThis(), -1, columnModelIndex, posX, posY);
			    }
	        }
            
			public void mousePressed(MouseEvent event) {

	        	JTableHeader header = getTableHeader();

	            // A workaround for a viewport reset during column drag bug with the Java6_03.
	        	int column = header.columnAtPoint(event.getPoint());
	        	if (column >= 0) {
	        		setColumnSelectionInterval(column, column);
	        		final Action focusAction = getActionMap().get("focusHeader");
	        		// Older versions have no such actions.
	        		if (focusAction != null) {
	        		   focusAction.actionPerformed(new ActionEvent(this, 0, "focusHeader"));
	        		}
	        	}

	        	// Validate dragged column or forbid the drag.
	        	TableColumn draggedColumn = header.getDraggedColumn(); 
	        	if (draggedColumn != null) {
	        		int colIndex = sprModel.validateDraggedColumnIndex(draggedColumn.getModelIndex());
	        		if (colIndex >= 0) {
	        			// Change the dragged column. 
	        			int viewIndex = convertColumnIndexToView(colIndex);
	        			header.setDraggedColumn(header.getColumnModel().getColumn(viewIndex));

	        			draggedColumnModelIndex = colIndex;
		        		draggedColumnViewIndex = viewIndex;
	        		} else {
	        			header.setDraggedColumn(null);
	        		}
	        	}
	        }
	        
			public void mouseReleased(MouseEvent event) {
			    if (event.getButton() == MouseEvent.BUTTON1) {
			        // Put the empty column back if it was shifted away. 
			    	int viewIndex = convertColumnIndexToView(0);
			    	if (viewIndex > 0) {
			    		moveColumn(viewIndex, 0);
			    	}
			    	
			    	// Validate the positions of columns if there was a drag.
			    	if (draggedColumnModelIndex >= 0) {
			    		int newViewIndex = convertColumnIndexToView(draggedColumnModelIndex);
			    		if (newViewIndex != draggedColumnViewIndex) {
			    			
			    			sprModel.repositionColumn(draggedColumnViewIndex, newViewIndex);
			    			
			    			// TODO old, remove
			    			/*
			    			System.out.println("SprTable: " + draggedColumnViewIndex + "->" + newViewIndex);
			    			sprModel.refreshOnColumnDragEnd();
			    			refreshPopupMenus();
			    			refreshPropertiesColumnMenuItemsState();
			    			updateBackgroundColor();
			    			updateSplitMenu();
			    			resizeColumns();
			    			getTableHeader().repaint();
			    			*/
			    		}
			    		draggedColumnModelIndex = -1;
			    	}
			    }
	        }
		});
		
		addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent event) {
				if (event.getButton() == MouseEvent.BUTTON1 && event.getClickCount() >= 2) {
					
					Point point = event.getPoint();
					int row = rowAtPoint(point);
					int column = convertColumnIndexToModel(columnAtPoint(point));
					String comment = sprModel.getMultilineString(row, column);
					
					if (comment!= null) {
						CommentDialog dialog = inspector.getCommentDialog();
						dialog.setComment(comment);
						dialog.setLocationRelativeTo(inspector);
						dialog.setVisible(true);
						if (dialog.isConfirmed()) {
							setValueAt(dialog.getComment(), row, column);
							sprModel.fireTableCellUpdated(row, column);
						}
					}
			    }
				
				if (event.getButton() == MouseEvent.BUTTON3) {
			    	Point point = event.getPoint();
			        int row = rowAtPoint(point);
			        int column = convertColumnIndexToModel(columnAtPoint(point));
		        	displayPopupMenu(false, getThis(), row, column, event.getX(), event.getY());
			    }
	        }
		}); 

		pane.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent event) {
				if (event.getButton() == MouseEvent.BUTTON3) {
					displayPopupMenu(false, getTableScrollPane(), -1, -1, event.getX(), event.getY());
			    }
				// TODO: remove
				sprModel.displayModel();
	        }
		});
		
		sprModel.refreshView();
		refreshPopupMenus();
    	refreshPropertiesColumnMenuItemsState();
    	updateBackgroundColor();
		updateSplitMenu();
		resizeColumns();
		getTableHeader().repaint();
	}

    private void displayPopupMenu(boolean forHeader, JComponent component, int row, int column, int posX, int posY) {

    	setPopupPosition(row, column);
    	
        boolean first = column == 0;
        boolean onRow = row >= 0;
        boolean onColumn = column >= 0;
        boolean wholeRowSelected = getSelectedRow() >= 0 && getSelectedColumn() == 0;   
        
        /* If there is a row selected or the click is on the first column, bring up a row popup menu, otherwise bring
         * up a column popup menu.
         */
        if (wholeRowSelected || (first && onRow)) {
        	/* Enable quick hidden row switching when all rows are displayed and none selected.
        	 */
        	if (wholeRowSelected || !sprModel.isShowAllRows()) {
        		
                boolean multipleSelected = getSelectedRowCount() > 1;
                boolean visible = false; 
                if (!multipleSelected) {
            		int modelIndex = (getSelectedRowCount() > 0) ? getSelectedRow() : row;
    				visible = sprModel.isModelRowVisible(modelIndex);
                }
                
            	setJComponentVisible(showRowsItem, multipleSelected || !visible);
            	setJComponentVisible(hideRowsItem, multipleSelected || visible);

            	showRowsItem.setText(multipleSelected ? showRows : showRow);
            	hideRowsItem.setText(multipleSelected ? hideRows : hideRow);
            	deleteRowsItem.setText(multipleSelected ? deleteRows : deleteRow);
            	
            	rowPopupMenu.show(component, posX, posY);
        	} else {
        		boolean visible = sprModel.isRowVisible(row);
        		sprModel.setRowsVisibility(new int[] {row}, !visible);
        	}

        } else {
        	boolean onTableOrHeader = (forHeader || onRow) && onColumn;
        	boolean persistant = column <= sprModel.getSolidProperitiesColumnCount();
        	boolean splitted = onTableOrHeader && sprModel.isSplit(column);
        	boolean manipulatable = onTableOrHeader && !first && !persistant; 
        	boolean sortable = onTableOrHeader && !first;

        	setJComponentVisible(hideItem, manipulatable);
        	setJComponentVisible(splitMenu, manipulatable && !splitted);
        	setJComponentVisible(joinItem, manipulatable && splitted);
        	setJComponentVisible(sortAscItem, sortable);
        	setJComponentVisible(sortDesItem, sortable);
        	setJComponentVisible(sortSeparator, sortable);

        	extendCountersItem.setEnabled(getSelectedColumnCount() >= 1 && getSelectedRowCount() >= 2);

        	columnPopupMenu.show(component, posX, posY);
        }
    }

    private void createColumnPopupMenu() {
    	columnPopupMenu = new JPopupMenu();

		hideItem = new JMenuItem(hide);
		hideItem.addActionListener(this);
    	columnPopupMenu.add(hideItem);

		sortAscItem = new JMenuItem(sortAsc);
		sortAscItem.addActionListener(this);
    	columnPopupMenu.add(sortAscItem);

    	sortDesItem = new JMenuItem(sortDes);
    	sortDesItem.addActionListener(this);
    	columnPopupMenu.add(sortDesItem);

    	splitMenu = new JMenu(split);
    	columnPopupMenu.add(splitMenu);
    	updateSplitMenu();
    	
    	joinItem = new JMenuItem(join);
    	joinItem.addActionListener(this);
    	columnPopupMenu.add(joinItem);

	    sortSeparator = new JSeparator(); 
    	columnPopupMenu.add(sortSeparator);
    	
    	JMenuItem hideAllItem = new JMenuItem(hideAll);
    	hideAllItem.addActionListener(this);
    	columnPopupMenu.add(hideAllItem);

    	JMenuItem showAllItem = new JMenuItem(showAll);
    	showAllItem.addActionListener(this);
    	columnPopupMenu.add(showAllItem);

    	JMenu visibilityMenu = new JMenu(visibility);
    	columnPopupMenu.add(visibilityMenu);

    	columnPopupMenu.addSeparator();
    	
    	JMenu presetColumnOrderMenu = new JMenu(presetColumnOrders);

    	ArrayList list = sprModel.getColumnOrderNames();

       	for (int i = 0; i < list.size(); i++) {
       		String action = list.get(i).toString();
       		JMenuItem menuItem = new JMenuItem(action + " order");
       		menuItem.setActionCommand(action);
       		menuItem.addActionListener(this);
       		presetColumnOrderMenu.add(menuItem);
       	}

    	columnPopupMenu.add(presetColumnOrderMenu);
    	
    	columnPopupMenu.addSeparator();
    	
    	int propertiesColumnCount = sprModel.getPropertiesColumnCount();

        propertiesColumnMenuItem = new JCheckBoxMenuItem[propertiesColumnCount];
        JMenu menuToAdd = visibilityMenu;
    	
    	for (int j = 0; j < propertiesColumnCount; j++) {
   			JCheckBoxMenuItem checkBoxItem = new JCheckBoxMenuItem(sprModel.getPropertiesColumnNames(j));
   			propertiesColumnMenuItem[j] = checkBoxItem;
   			checkBoxItem.setSelected(true);
   			checkBoxItem.addActionListener(this);
   			
   	    	// Add check boxes for all but the unhideable and the first.
   			int itemPos = j - (sprModel.getSolidProperitiesColumnCount() + 1); 
   			if (itemPos >= 0) {
   				menuToAdd = PopUpMenu.addItem(checkBoxItem, menuToAdd, itemPos, visibilityMenuItemCount);
   			}
    	}
    	
    	// If no items to hide/show, the options should be disabled.
    	if (propertiesColumnCount <= sprModel.getSolidProperitiesColumnCount() + 1) {
    		hideAllItem.setEnabled(false);
    		showAllItem.setEnabled(false);
    		visibilityMenu.setEnabled(false);
    	}
    	
    	JMenuItem setBackgroundItem = new JMenuItem(backgroundColorString);
    	setBackgroundItem.addActionListener(this);
    	columnPopupMenu.add(setBackgroundItem);
    	
    	JMenuItem defaultBackgroundItem = new JMenuItem(defaultColorString);
    	defaultBackgroundItem.addActionListener(this);
    	columnPopupMenu.add(defaultBackgroundItem);

    	columnPopupMenu.addSeparator();

    	showAllRowsItem = new JCheckBoxMenuItem(showAllRowsString);
    	showAllRowsItem.setSelected(sprModel.isShowAllRows());
    	showAllRowsItem.addActionListener(this);
    	columnPopupMenu.add(showAllRowsItem);

    	columnPopupMenu.addSeparator();
    	
    	extendCountersItem = new JMenuItem(extendCounters);
    	extendCountersItem.setToolTipText(extendCountersToolTip);
    	extendCountersItem.addActionListener(this);
    	columnPopupMenu.add(extendCountersItem);
    }
    
    private void createRowPopupMenu() {
    	rowPopupMenu = new JPopupMenu();
    	hideRowsItem = new JMenuItem(hideRow);
    	hideRowsItem.addActionListener(this);
    	rowPopupMenu.add(hideRowsItem);
    	showRowsItem = new JMenuItem(showRow);
    	showRowsItem.addActionListener(this);
    	rowPopupMenu.add(showRowsItem);
    	deleteRowsItem = new JMenuItem(deleteRow);
    	deleteRowsItem.addActionListener(this);
    	rowPopupMenu.add(deleteRowsItem);
    }

	public void actionPerformed(ActionEvent event) {
		String action = event.getActionCommand();
		Object source = event.getSource();

		if (action.equals(hide)) {
			if (popupModelColumn >= 0) {
				int propertyColumn = sprModel.getPropertyColumn(popupModelColumn);
	    		propertiesColumnMenuItem[propertyColumn].setSelected(false);
				sprModel.setColumnsVisibility(new int[] {popupModelColumn}, false);

				// TODO remove
				//sprModel.setColumnVisibility(false, popupModelColumn);
	    		//propertiesColumnMenuItem[sprModel.getModelToPropertiesColumnIndex(popupModelColumn)].setSelected(false);
	    	}
		} else if (action.equals(whitespaces) || action.equals(customPattern)) {

			int index = popupModelColumn;
			if (index >= 0) {
				SplitData split = null; 
				if (action.equals(customPattern)) {
					CustomSplitDialog dialog = inspector.getCustomSplitDialog();
					dialog.setLocationRelativeTo(inspector);
					
					Vector recentSplitData = sprModel.getRecentSplitData();
					if (recentSplitData.size() > 0) {
						split = (SplitData)recentSplitData.get(0);
					} else {
						split = SplitData.getWhitespaceSplitData();
					}
					dialog.setSplitData(split);
					/* Add the data from the first row of the splitting column as starting test example. If no visible
					 * rows, add from the first among all.
					 */
                    dialog.setTestExample((sprModel.getModelRowCount() >= 0) ? sprModel.getModelValue(0, index) : "");
					dialog.setVisible(true);
					split = dialog.getSplitData();
					if (split != null) {
						recentSplitData.add(0, split);
						while (recentSplitData.size() > sprModel.getRecentSplitDataMaxCount()) {
							recentSplitData.remove(recentSplitData.size() - 1);
						}
						updateSplitMenu();
					}
					
				} else {
				    split = SplitData.getWhitespaceSplitData();
				}
				if (split != null) {
					sprModel.splitColumn(split, index);
              		refreshPopupMenus();
	    			refreshPropertiesColumnMenuItemsState();
	    			updateBackgroundColor();
	    			updateSplitMenu();
	    			resizeColumns();
	    			getTableHeader().repaint();
				}
			}
		} else if (action.equals(join)) {
			if (popupModelColumn >= 0) {
				sprModel.splitColumn(null, popupModelColumn);
           		refreshPopupMenus();
    			refreshPropertiesColumnMenuItemsState();
    			updateBackgroundColor();
    			updateSplitMenu();
    			resizeColumns();
    			getTableHeader().repaint();
	    	}
		} else if (action.equals(sortAsc) || action.equals(sortDes)) {
	    	if (popupModelColumn >= 0) {
	    		sprModel.updateSortedColumn(popupModelColumn);
	    	}
		} else if (action.equals(hideAll) || action.equals(showAll)) {
		    boolean visible = action.equals(showAll);

		    // do this on all but the first two
		    int startColumn = sprModel.getSolidProperitiesColumnCount() + 1;
		    int endColumn = propertiesColumnMenuItem.length;
		    
		    int[] columns = new int[endColumn - startColumn];
		    // do this on all but the first two
		    for (int i = startColumn; i < endColumn; i++) {
		    	//sprModel.setPropertiesColumnVisibility(visible, i);
		    	propertiesColumnMenuItem[i].setSelected(visible);
		    	columns[i - startColumn] = i;
		    }
			sprModel.setPropertyColumnsVisibility(columns, visible);
		    
		} else if (action.equals(showAllRowsString)) {
		    boolean state = ((JCheckBoxMenuItem)source).isSelected();
		    sprModel.setShowAllRows(state);
			showAllRowsItem.setSelected(state);
			getTableHeader().repaint();
		} else if (action.equals(backgroundColorString)) {
		    final JColorChooser chooser = ColorChooser.getInstance();
		    
		    chooser.setColor(getBackground());
		    ActionListener okListener = new ActionListener() {
				public void actionPerformed(ActionEvent event) {
				    sprModel.setBackground(chooser.getColor());
				    updateBackgroundColor();
				}
		    };
		    JColorChooser.createDialog(inspector, colorChooserTitle, true, chooser, okListener, null)
		        .setVisible(true); 
		} else if (action.equals(defaultColorString)) {
			sprModel.setBackground(sprModel.getDefaultBackground());
		    updateBackgroundColor();
		} else if (action.equals(extendCounters)) {
		
		    int[] selViewColumns = getSelectedColumns();
		    int[] selColumns = new int[selViewColumns.length]; 
            for (int c = 0; c < selViewColumns.length; c++) {
			    selColumns[c] = convertColumnIndexToModel(selViewColumns[c]);
			}
            sprModel.extendCounters(getSelectedRows(), selColumns);

		} else if (action.equals(hideRow) || action.equals(hideRows)
				|| action.equals(showRow) || action.equals(showRows)) {
			
			boolean visible = action.equals(showRow) || action.equals(showRows);

			int[] selRows = getSelectedRows();
			if (selRows.length == 0) {
				selRows = new int[] {popupModelRow};
			}
			sprModel.setRowsVisibility(selRows, visible);
		
		} else if (action.equals(deleteRow) || action.equals(deleteRows)) {

			int[] selRows = getSelectedRows();
			if (selRows.length == 0) {
				selRows = new int[] {popupModelRow};
			}
			sprModel.deleteRows(selRows);
			
		} else if (source instanceof JCheckBoxMenuItem) {
			int columnIndex = sprModel.getPropertiesColumnIndex(action);
			sprModel.setPropertyColumnsVisibility(new int[] {columnIndex}, ((JCheckBoxMenuItem)source).isSelected());
			// TODO remove
			//sprModel.setPropertiesColumnVisibility(((JCheckBoxMenuItem)source).isSelected(), columnIndex);
		} else {
			if (popupModelColumn >= 0) {
				int recentIndex = getRecentSplitDataIndex(action);
				if (recentIndex >= 0) {
					sprModel.splitColumnByRecentList(recentIndex, popupModelColumn);
              		updateSplitMenu();
              		refreshPopupMenus();
	    			refreshPropertiesColumnMenuItemsState();
	    			updateBackgroundColor();
	    			updateSplitMenu();
	    			resizeColumns();
	    			getTableHeader().repaint();
              		return;
				}
			}
			
			sprModel.setColumnOrder(action);
            refreshPopupMenus();
            refreshPropertiesColumnMenuItemsState();
            updateBackgroundColor();            
   			updateSplitMenu();
            resizeColumns();
    		getTableHeader().repaint();
        }
	}

	private void updateBackgroundColor() {
		Color background = sprModel.getBackground();
	    setBackground(background);
	    getTableHeader().setBackground(background);
	    pane.getViewport().setBackground(background);
	} 

	private int getRecentSplitDataIndex(String name) {
		Integer integer = (Integer)nameToRecentSplitDataIndex.get(name);
		return (integer != null) ? integer.intValue() : -1;
	}

	private void updateSplitMenu() {
    	splitMenu.removeAll();
    	JMenuItem menuItem = new JMenuItem(whitespaces);
    	menuItem.addActionListener(this);
    	splitMenu.add(menuItem);
    	splitMenu.addSeparator();

    	nameToRecentSplitDataIndex.clear();
    	
    	Vector recentSplitData = sprModel.getRecentSplitData();
    	for (int i = 0; i < recentSplitData.size(); i++) {
    		String name = recentSplitData.get(i).toString();
        	menuItem = new JMenuItem(name);
        	menuItem.addActionListener(this);
        	splitMenu.add(menuItem);
        	nameToRecentSplitDataIndex.put(name, new Integer(i));
    	}
    	
    	if (recentSplitData.size() > 0) {
        	splitMenu.addSeparator();
    	}

    	menuItem = new JMenuItem(customPattern);
    	menuItem.addActionListener(this);
    	splitMenu.add(menuItem);
    }

    private void setJComponentVisible(JComponent component, boolean visible) {
    	if (component.isVisible() != visible) {
    		component.setVisible(visible);
    	}
    }
    
	private void refreshPopupMenus() {
		createColumnPopupMenu();
		createRowPopupMenu();
	}

	public JScrollPane getTableScrollPane() {
	    return pane;
	}

	public void setPopupPosition(int row, int column) {
		popupModelRow = row;
		popupModelColumn = column;
	}
	
	private void refreshPropertiesColumnMenuItemsState() {
	    for (int i = sprModel.getSolidProperitiesColumnCount() + 1; i < propertiesColumnMenuItem.length; i++) {
		   	propertiesColumnMenuItem[i].setSelected(sprModel.isPropertiesColumnVisible(i));
		}
	} 
}