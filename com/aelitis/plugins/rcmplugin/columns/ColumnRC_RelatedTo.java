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
import com.biglybt.ui.UIFunctions;
import com.biglybt.ui.common.table.TableColumnCore;
import com.biglybt.ui.mdi.MultipleDocumentInterface;
import com.biglybt.ui.swt.UIFunctionsManagerSWT;

import com.biglybt.pif.download.Download;
import com.biglybt.pif.ui.menus.MenuItemListener;
import com.biglybt.pif.ui.tables.*;
import com.biglybt.pifimpl.local.PluginCoreUtils;

/**
 * @author TuxPaper
 * @created Feb 26, 2009
 *
 */
public class ColumnRC_RelatedTo
	implements TableCellRefreshListener 
{
	public static final String COLUMN_ID = "rc_relto";

	/**
	 * 
	 * @param sTableID
	 */
	public ColumnRC_RelatedTo(TableColumn column) {
		column.initialize(TableColumn.ALIGN_LEAD, TableColumn.POSITION_INVISIBLE, 400);
		column.addListeners(this);
		column.setRefreshInterval(TableColumn.INTERVAL_GRAPHIC);
		column.setType(TableColumn.TYPE_TEXT_ONLY);
		
		if ( column instanceof TableColumnCore ){
			((TableColumnCore)column).setUseCoreDataSource( true );
		}
		
		TableContextMenuItem item = column.addContextMenuItem("rcm.menu.openRelatedDM");
		
		
		item.addListener(new MenuItemListener() {

			@Override
			public void selected(com.biglybt.pif.ui.menus.MenuItem menu,
			                     Object target) {
				if (target instanceof RelatedContent) {
					RelatedContent rc = (RelatedContent) target;

					Download relatedToDownload = rc.getRelatedToDownload();
					if (relatedToDownload != null) {
						UIFunctionsManagerSWT.getUIFunctionsSWT().getMDI().showEntryByID(
								MultipleDocumentInterface.SIDEBAR_SECTION_TORRENT_DETAILS,
								PluginCoreUtils.unwrap(relatedToDownload));
					}
				}
			};
		});

	}

	@Override
	public void refresh(TableCell cell) {
		RelatedContent rc = (RelatedContent) cell.getDataSource();
		if (rc == null) {
			return;
		}

		String text = "";
		
		Download relatedToDownload = rc.getRelatedToDownload();
		if (relatedToDownload != null) {
			text = rc.getRelatedToDownload().getName();
		}
		
		if ( text == null || text.length() == 0 ){
			
			return;
		}

		cell.setText(text);
	}
}
