/**
 * Created on Aug 25, 2009
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.Display;
import com.biglybt.core.internat.MessageText;
import com.biglybt.core.util.Debug;
import com.biglybt.pif.ui.tables.*;
import com.biglybt.ui.swt.mainwindow.TorrentOpener;
import com.biglybt.ui.swt.shells.GCStringPrinter;
import com.biglybt.ui.swt.shells.GCStringPrinter.URLInfo;
import com.biglybt.ui.swt.views.table.TableCellSWT;
import com.biglybt.ui.swt.views.table.TableCellSWTPaintListener;

import com.biglybt.core.content.RelatedContent;
import com.biglybt.core.metasearch.Engine;
import com.biglybt.core.subs.Subscription;
import com.biglybt.core.vuzefile.VuzeFile;
import com.biglybt.core.vuzefile.VuzeFileComponent;
import com.biglybt.core.vuzefile.VuzeFileHandler;
import com.biglybt.ui.UIFunctionsManager;
import com.biglybt.ui.common.table.TableColumnCore;
import com.biglybt.ui.swt.skin.SWTSkinFactory;
import com.biglybt.ui.swt.skin.SWTSkinProperties;
import com.aelitis.plugins.rcmplugin.RCMPlugin;
import com.aelitis.plugins.rcmplugin.RelatedContentUISWT;

/**
 * @author TuxPaper
 * @created Aug 25, 2009
 *
 */
public class ColumnRC_Actions
	implements TableCellRefreshListener, TableCellMouseMoveListener,
	TableCellSWTPaintListener
{
	public static final String COLUMN_ID = "rc_actions";

	private static Font font = null;

	private Color colorLinkNormal;

	private Color colorLinkHover;


	
	public ColumnRC_Actions(TableColumn column) {
		column.initialize(TableColumn.ALIGN_LEAD, TableColumn.POSITION_LAST, 215);
		column.addListeners(this);
		column.setRefreshInterval(TableColumn.INTERVAL_GRAPHIC);
		column.setType(TableColumn.TYPE_GRAPHIC);

		if (column instanceof TableColumnCore) {
			((TableColumnCore) column).setUseCoreDataSource(true);
			((TableColumnCore) column).addCellOtherListener("SWTPaint", this);
		}

		SWTSkinProperties skinProperties = SWTSkinFactory.getInstance().getSkinProperties();
		colorLinkNormal = skinProperties.getColor("color.links.normal");
		colorLinkHover = skinProperties.getColor("color.links.hover");
	}

	@Override
	public void cellPaint(GC gc, TableCellSWT cell) {
		String text = (String) cell.getSortValue();
		if (text == null) {
			return;
		}

		if (text != null && text.length() > 0) {
			if (font == null) {
				FontData[] fontData = gc.getFont().getFontData();
				fontData[0].setStyle(SWT.BOLD);
				font = new Font(gc.getDevice(), fontData);
			}
			gc.setFont(font);

			Rectangle bounds = getDrawBounds(cell);

			GCStringPrinter sp = new GCStringPrinter(gc, text, bounds, true, true,
					SWT.WRAP | SWT.CENTER);

			sp.calculateMetrics();

			if (sp.hasHitUrl()) {
				URLInfo[] hitUrlInfo = sp.getHitUrlInfo();
				for (int i = 0; i < hitUrlInfo.length; i++) {
					URLInfo info = hitUrlInfo[i];
					// handle fake row when showing in column editor

					info.urlUnderline = cell.getTableRow() == null
							|| cell.getTableRow().isSelected();
					if (info.urlUnderline) {
						info.urlColor = null;
					} else {
						info.urlColor = colorLinkNormal;
					}
				}
				int[] mouseOfs = cell.getMouseOffset();
				if (mouseOfs != null) {
					Rectangle realBounds = cell.getBounds();
					URLInfo hitUrl = sp.getHitUrl(mouseOfs[0] + realBounds.x, mouseOfs[1]
							+ realBounds.y);
					if (hitUrl != null) {
						hitUrl.urlColor = colorLinkHover;
					}
				}
			}

			sp.printString(GCStringPrinter.FLAG_FULLLINESONLY);
		}
	}

	// @see com.biglybt.pif.ui.tables.TableCellAddedListener#cellAdded(com.biglybt.pif.ui.tables.TableCell)
	public void cellAdded(TableCell cell) {
		cell.setMarginHeight(0);
	}

	// @see com.biglybt.pif.ui.tables.TableCellRefreshListener#refresh(com.biglybt.pif.ui.tables.TableCell)
	@Override
	public void refresh(TableCell cell) {
		RelatedContent rc = (RelatedContent) cell.getDataSource();
		if (rc == null) {
			return;
		}
		boolean downloadable = rc.getHash() != null;

		String s;
		
		if ( rc instanceof RelatedContentUISWT.SubsRelatedContent ){
			
			Subscription subs = ((RelatedContentUISWT.SubsRelatedContent)rc).getSubscription();
			
			if ( subs.isSearchTemplate()){
				
				s = "<A HREF=\"install\">" + "Install" + "</A>";
			
			}else{
			
				s = "<A HREF=\"subscribe\">" + "Subscribe" + "</A>";
			}
		}else{
			s = "<A HREF=\"search\">" + MessageText.getString("Button.search") + "</A>";
			if (downloadable) {
				s += " | <A HREF=\"dl\">"
					+ MessageText.getString("v3.MainWindow.button.download") + "</A>";
			}
		}
		
		if (!cell.setSortValue(s) && cell.isValid()) {
			return;
		}
	}

	// @see com.biglybt.pif.ui.tables.TableCellMouseListener#cellMouseTrigger(com.biglybt.pif.ui.tables.TableCellMouseEvent)
	@Override
	public void cellMouseTrigger(TableCellMouseEvent event) {
		RelatedContent rc = (RelatedContent) event.cell.getDataSource();
		if (rc == null) {
			return;
		}

		boolean invalidateAndRefresh = event.eventType == event.EVENT_MOUSEEXIT;

		Rectangle bounds = ((TableCellSWT) event.cell).getBounds();
		String text = (String) event.cell.getSortValue();
		if (text == null) {
			return;
		}

		GCStringPrinter sp = null;
		GC gc = new GC(Display.getDefault());
		try {
			if (font != null) {
				gc.setFont(font);
			}
			Rectangle drawBounds = getDrawBounds((TableCellSWT) event.cell);
			sp = new GCStringPrinter(gc, text, drawBounds, true, true, SWT.WRAP
					| SWT.CENTER);
			sp.calculateMetrics();
		} catch (Exception e) {
			Debug.out(e);
		} finally {
			gc.dispose();
		}

		if (sp != null) {
			URLInfo hitUrl = sp.getHitUrl(event.x + bounds.x, event.y + bounds.y);
			int newCursor;
			if (hitUrl != null) {
				if (event.eventType == TableCellMouseEvent.EVENT_MOUSEUP && event.button == 1) {
					if (hitUrl.url.equals("dl")) {
						byte[]  hash = rc.getHash();
						
						if ( hash != null && hash.length > 0 ){
							
							rc.setUnread( false );
							
							TorrentOpener.openTorrent( RCMPlugin.getMagnetURI( rc ));
						}
						
					} else if (hitUrl.url.equals("search")) {
						rc.setUnread( false );
						
						String	title = rc.getTitle();
						
						UIFunctionsManager.getUIFunctions().doSearch( title );
					} else if (hitUrl.url.equals("subscribe")) {
						rc.setUnread( false );
					} else if (hitUrl.url.equals("install")) {
						Subscription subs = ((RelatedContentUISWT.SubsRelatedContent)rc).getSubscription();
						
						try{
							VuzeFile vf = subs.getSearchTemplateVuzeFile();
							
							if ( vf != null ){
							
								if ( !subs.isSubscribed()){
								
										// subscribing to it will cause the search template to
										// be installed
									
									subs.setSubscribed( true );
									
								}else{
									
										// force re-installation
									
									VuzeFileHandler.getSingleton().handleFiles( new VuzeFile[]{ vf }, VuzeFileComponent.COMP_TYPE_NONE );
									
									
									for ( VuzeFileComponent comp: vf.getComponents()){
										
										Engine engine = (Engine)comp.getData( Engine.VUZE_FILE_COMPONENT_ENGINE_KEY );
										
										if ( engine != null && engine.getSelectionState() == Engine.SEL_STATE_DESELECTED ){
											
											engine.setSelectionState( Engine.SEL_STATE_MANUAL_SELECTED );
										}
									}
								}
							}
						}catch( Throwable e ){
							
							Debug.out( e );
						}
					}
				}

				newCursor = SWT.CURSOR_HAND;
			} else {
				newCursor = SWT.CURSOR_ARROW;
			}

			int oldCursor = ((TableCellSWT) event.cell).getCursorID();
			if (oldCursor != newCursor) {
				invalidateAndRefresh = true;
				((TableCellSWT) event.cell).setCursorID(newCursor);
			}
		}

		if (invalidateAndRefresh) {
			event.cell.invalidate();
			((TableCellSWT)event.cell).redraw();
		}
	}

	private Rectangle getDrawBounds(TableCellSWT cell) {
		Rectangle bounds = cell.getBounds();

		return bounds;
	}
}
