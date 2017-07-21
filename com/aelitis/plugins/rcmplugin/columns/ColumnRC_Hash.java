/**
 * Created on Feb 26, 2009
 *
 * Copyright 2008 Vuze, Inc.  All rights reserved.
 * 
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.aelitis.plugins.rcmplugin.columns;

import com.biglybt.core.content.RelatedContent;
import com.biglybt.ui.common.table.TableColumnCore;

import org.eclipse.swt.SWT;
import com.biglybt.core.internat.MessageText;
import com.biglybt.core.util.ByteFormatter;
import com.biglybt.pif.ui.tables.*;
import com.biglybt.ui.swt.mainwindow.TorrentOpener;
import com.biglybt.ui.swt.views.table.TableCellSWT;

/**
 * @author TuxPaper
 * @created Feb 26, 2009
 *
 */
public class ColumnRC_Hash
	implements TableCellRefreshListener, TableCellAddedListener // , TableCellMouseListener
{
	public static final String COLUMN_ID = "rc_hash";

	/**
	 * 
	 * @param sTableID
	 */
	public ColumnRC_Hash(TableColumn column) {
		column.initialize(TableColumn.ALIGN_LEAD, TableColumn.POSITION_INVISIBLE, 215);
		column.addListeners(this);
		column.setRefreshInterval(TableColumn.INTERVAL_GRAPHIC);
		column.setType(TableColumn.TYPE_TEXT_ONLY);
		if ( column instanceof TableColumnCore ){
			((TableColumnCore)column).setUseCoreDataSource( true );
		}
	}

	@Override
	public void cellAdded(TableCell cell) {
		
		RelatedContent rc = (RelatedContent) cell.getDataSource();
		
		if ( cell instanceof TableCellSWT && rc != null && rc.getHash() != null ){
		
			((TableCellSWT)cell).setCursorID( SWT.CURSOR_HAND );
			
			cell.setToolTip( MessageText.getString( "rcm.rc_hash.tt" ));
		}
	}
	
	@Override
	public void refresh(TableCell cell) {
		RelatedContent rc = (RelatedContent) cell.getDataSource();
		if (rc == null) {
			return;
		}

		byte[] hash = rc.getHash();
		
		if ( hash == null ){
			
			return;
		}

		cell.setText(ByteFormatter.encodeString(hash));
	}
	
	public void cellMouseTrigger(final TableCellMouseEvent event) {
		if (event.eventType == TableRowMouseEvent.EVENT_MOUSEDOWN
				&& event.button == 1) {
			RelatedContent rc = (RelatedContent) event.cell.getDataSource();
			
			byte[]  hash = rc.getHash();
			
			if ( hash != null ){
				
				rc.setUnread( false );
				
				TorrentOpener.openTorrent( ByteFormatter.encodeString( hash ));
			}
		}
	}
}
