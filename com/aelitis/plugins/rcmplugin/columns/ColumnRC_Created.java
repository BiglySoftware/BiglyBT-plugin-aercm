/**
 * Copyright (C) 2008 Vuze Inc., All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */

package com.aelitis.plugins.rcmplugin.columns;


import com.biglybt.pif.ui.tables.TableCell;
import com.biglybt.pif.ui.tables.TableCellRefreshListener;
import com.biglybt.pif.ui.tables.TableColumn;
import com.biglybt.pif.ui.tables.TableColumnExtraInfoListener;
import com.biglybt.pif.ui.tables.TableColumnInfo;
import com.biglybt.core.content.RelatedContent;
import com.biglybt.ui.swt.views.table.utils.TableColumnCreator;
import com.biglybt.ui.swt.views.tableitems.ColumnDateSizer;


/**
 * @author Olivier Chalouhi
 * @created Oct 7, 2008
 *
 */
public class 
ColumnRC_Created
	implements TableCellRefreshListener, TableColumnExtraInfoListener
{	
	public static String COLUMN_ID = "rc_created";
	
	@Override
	public void fillTableColumnInfo(TableColumnInfo info) {
		info.addCategories(new String[] {
				TableColumn.CAT_TIME,
		});
		info.setProficiency(TableColumnInfo.PROFICIENCY_BEGINNER);
	}
	
	public 
	ColumnRC_Created(
		TableColumn column )
	{
		column.setWidth(TableColumnCreator.DATE_COLUMN_WIDTH);
		column.setPosition( TableColumn.POSITION_LAST );
		column.addListeners(this);
	}

	@Override
	public void refresh(TableCell cell) {
		RelatedContent rc = (RelatedContent) cell.getDataSource();
		if (rc == null) {
			return;
		}

		long date = rc.getPublishDate();

		TableColumn tc = cell.getTableColumn();

		if (tc instanceof ColumnDateSizer) {

			((ColumnDateSizer) tc).refresh(cell,  date  );
		}
	}
}
