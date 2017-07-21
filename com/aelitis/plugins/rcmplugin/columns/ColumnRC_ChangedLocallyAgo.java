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


import com.biglybt.core.util.DisplayFormatters;
import com.biglybt.core.util.SystemTime;
import com.biglybt.core.util.TimeFormatter;
import com.biglybt.pif.ui.tables.TableCell;
import com.biglybt.pif.ui.tables.TableCellRefreshListener;
import com.biglybt.pif.ui.tables.TableColumn;

import com.biglybt.core.content.RelatedContent;
import com.biglybt.ui.common.table.TableColumnCore;


/**
 * @author Olivier Chalouhi
 * @created Oct 7, 2008
 *
 */
public class 
ColumnRC_ChangedLocallyAgo
	implements TableCellRefreshListener
{	
	public static String COLUMN_ID = "rc_changedlocallyon";
	
	public 
	ColumnRC_ChangedLocallyAgo(
		TableColumn column )
	{
		column.initialize(TableColumn.ALIGN_TRAIL, TableColumn.POSITION_LAST, 90 );
		column.addListeners(this);
		column.setRefreshInterval(TableColumn.INTERVAL_GRAPHIC);
		column.setType(TableColumn.TYPE_TEXT_ONLY);

		if ( column instanceof TableColumnCore ){
			((TableColumnCore)column).setUseCoreDataSource( true );
		}
	}

	@Override
	public void refresh(TableCell cell) {
		RelatedContent rc = (RelatedContent) cell.getDataSource();
		if (rc == null) {
			return;
		}

		long date = rc.getChangedLocallyOn();

		long display_secs = (SystemTime.getCurrentTime() - date) / 1000;
		
		int	round_secs = 10;
		
		long sort_value = display_secs/round_secs;
		
		if ( cell.setSortValue( sort_value )){
		
			cell.setToolTip(date <= 0?"--":DisplayFormatters.formatCustomDateOnly( date ) + ", v=" + rc.getVersion());
			cell.setText( date <= 0?"--":TimeFormatter.format( sort_value * round_secs ));
		}
	}
}
