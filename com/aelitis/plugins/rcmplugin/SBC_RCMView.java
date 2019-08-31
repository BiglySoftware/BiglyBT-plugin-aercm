/**
 * Created on Feb 24, 2009
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


package com.aelitis.plugins.rcmplugin;


import java.lang.reflect.Field;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.biglybt.core.Core;
import com.biglybt.core.CoreRunningListener;
import com.biglybt.ui.common.table.*;
import com.biglybt.ui.selectedcontent.DownloadUrlInfo;
import com.biglybt.ui.selectedcontent.ISelectedContent;
import com.biglybt.ui.selectedcontent.SelectedContent;
import com.biglybt.ui.selectedcontent.SelectedContentManager;
import com.biglybt.ui.swt.skin.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.*;
import com.biglybt.core.config.COConfigurationManager;
import com.biglybt.core.config.ParameterListener;
import com.biglybt.core.internat.MessageText;
import com.biglybt.core.util.*;
import com.biglybt.pif.ui.UIManager;
import com.biglybt.pif.ui.UIPluginViewToolBarListener;
import com.biglybt.pif.ui.tables.TableColumn;
import com.biglybt.pif.ui.tables.TableColumnCreationListener;
import com.biglybt.pif.ui.tables.TableManager;
import com.biglybt.pif.ui.toolbar.UIToolBarItem;
import com.biglybt.pif.utils.search.SearchProvider;
import com.biglybt.pifimpl.local.PluginInitializer;
import com.biglybt.ui.swt.Utils;
import com.biglybt.ui.swt.mainwindow.ClipboardCopy;
import com.biglybt.ui.swt.mainwindow.TorrentOpener;
import com.biglybt.ui.swt.views.table.TableViewSWT;
import com.biglybt.ui.swt.views.table.TableViewSWTMenuFillListener;
import com.biglybt.ui.swt.views.table.impl.TableViewFactory;

import com.biglybt.core.CoreFactory;
import com.biglybt.core.content.ContentException;
import com.biglybt.core.content.RelatedContent;
import com.biglybt.core.content.RelatedContentLookupListener;
import com.biglybt.core.content.RelatedContentManager;
import com.biglybt.core.content.RelatedContentManagerListener;
import com.biglybt.ui.UIFunctions;
import com.biglybt.ui.UIFunctionsManager;
import com.biglybt.ui.common.ToolBarItem;
import com.biglybt.ui.common.table.impl.TableColumnManager;
import com.biglybt.ui.common.updater.UIUpdatable;
import com.biglybt.ui.mdi.MdiEntry;
import com.biglybt.ui.swt.UIFunctionsManagerSWT;
import com.biglybt.ui.swt.mdi.MultipleDocumentInterfaceSWT;
import com.biglybt.ui.swt.views.skin.InfoBarUtil;
import com.biglybt.ui.swt.views.skin.SkinView;
import com.biglybt.ui.swt.views.stats.GeneralOpsPanel;
import com.biglybt.ui.swt.views.stats.GeneralOpsPanel.Node;
import com.biglybt.ui.swt.views.stats.GeneralOpsPanel.NodeEvent;
import com.biglybt.ui.swt.views.stats.GeneralOpsPanel.State;
import com.aelitis.plugins.rcmplugin.RelatedContentUISWT.RCMItemContent;
import com.aelitis.plugins.rcmplugin.RelatedContentUISWT.RCMItemSubView;
import com.aelitis.plugins.rcmplugin.RelatedContentUISWT.RCMItemSubscriptions;
import com.aelitis.plugins.rcmplugin.columns.*;


public class 
SBC_RCMView
	extends SkinView
	implements UIUpdatable, UIPluginViewToolBarListener, TableViewFilterCheck<RelatedContent>
{
	public static final String TABLE_RCM = "RCM";

	private static boolean columnsAdded = false;

	private static RelatedContentManager	manager;
	
	static{
		try{
			manager = RelatedContentManager.getSingleton();
			
		}catch( Throwable e ){
			
			Debug.out(e);
		}
	}
	
	private TableViewSWT<RelatedContent> 	tv_related_content;

	private OpsContainer					general_ops_panel;
	
	private MdiEntry 			mdi_entry;
	private CTabFolder 			tab_folder;
	private boolean				space_reserved;
	
	
	private Text txtFilter;

	private RelatedContentManagerListener current_rcm_listener;

	protected int minSeeds;

	private boolean showUnknownSeeds = true;

	private long createdMsAgo;

	private int minRank;

	private int minSize;
	
	private boolean showIndirect = true;
	
	private Object ds;

	private ParameterListener paramSourceListener;
	
	private List<RelatedContent>	last_selected_content = new ArrayList<RelatedContent>();
	private TableLifeCycleListener tableLifeCycleListener;

	public
	SBC_RCMView()
	{
	}
	
	@Override
	public Object
	skinObjectInitialShow(
            SWTSkinObject skinObject, Object params )
	{
		CoreFactory.addCoreRunningListener(
			new CoreRunningListener()
			{
				@Override
				public void
				coreRunning(
					Core core )
				{
					initColumns( core );
				}
			});

		MultipleDocumentInterfaceSWT mdi = UIFunctionsManagerSWT.getUIFunctionsSWT().getMDISWT();
		
		boolean	show_info_bar = true;
		
		if ( mdi != null ){

			if ( ds instanceof RCMItemSubView ){
				
				manager.reserveTemporarySpace();
			
				space_reserved = true;
			
				show_info_bar = false;
				
			}else if ( ds instanceof RCMItemContent ){
				
				RCMItemContent	ic = (RCMItemContent)ds;
				
				mdi_entry = ic.getSideBarEntry();
								
				manager.reserveTemporarySpace();
				
				space_reserved = true;
				
			}else if ( ds instanceof RCMItemSubscriptions ){
				
				mdi_entry = ((RCMItemSubscriptions) ds).getSideBarEntry();
				manager.reserveTemporarySpace();
				
				space_reserved = true;
				
			}else{
				
				mdi_entry = mdi.getEntry( RelatedContentUISWT.SIDEBAR_SECTION_RELATED_CONTENT );
			}
			
			if ( mdi_entry != null ){
			
				mdi_entry.addToolbarEnabler(this);
			}
		}

		SWTSkinObjectTextbox soFilterBox = (SWTSkinObjectTextbox) getSkinObject("filterbox");
		if (soFilterBox != null) {
			txtFilter = soFilterBox.getTextControl();
		}

		final SWTSkinObject soFilterArea = getSkinObject("filterarea");
		if (soFilterArea != null) {
			
			SWTSkinObjectButton soSearchButton = (SWTSkinObjectButton) getSkinObject("search-button");
			
			if ( soSearchButton != null ){

				soSearchButton.addSelectionListener(
						new SWTSkinButtonUtility.ButtonListenerAdapter()
						{
							@Override
							public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject,
									int stateMask){
								
								RelatedContentUISWT.getSingleton().findBySearch();
							}
						});
			}
			
			SWTSkinObjectButton soRSSButton = (SWTSkinObjectButton) getSkinObject("rss-button");
			
			if ( soRSSButton != null ){
				
				boolean visible = false;
				
				if ( ds instanceof RCMItemContent ){
					
					RCMItemContent ic = (RCMItemContent)ds;
					
					String[] exprs = ic.getExpressions();
					
					final SearchProvider sp = ic.getPlugin().getSearchProvider();

					if ( sp != null && exprs != null && exprs.length == 1 ){
						
						String original_expr = exprs[0];
						
						visible = true;
						
						soRSSButton.addSelectionListener(
							new SWTSkinButtonUtility.ButtonListenerAdapter()
							{
								private String
								getLatestExpression()
								{
									String[] expressions = ic.getExpressions();
									
									return( (expressions==null||expressions.length==0)?original_expr:expressions[0]);
								}

								
								@Override
								public void pressed(SWTSkinButtonUtility buttonUtility, SWTSkinObject skinObject,
										int stateMask){
									
									String expression = getLatestExpression();
									
									String[] networks = ic.getNetworks();
									
									String net_str = RCMPlugin.getNetworkString( networks );

									boolean	is_popularity = expression.equals( RCMPlugin.POPULARITY_SEARCH_EXPR );
									
									String name = is_popularity?MessageText.getString("rcm.pop" ):("'" + expression + "'" );
									
									String	subscription_name = MessageText.getString( "rcm.search.provider" ) + ": " + name + net_str;

									Map<String,Object>	properties = new HashMap<String, Object>();
									
									properties.put( SearchProvider.SP_SEARCH_NAME, subscription_name );
									properties.put( SearchProvider.SP_SEARCH_TERM, expression );
									properties.put( SearchProvider.SP_NETWORKS, networks );
									
									try{
										RCMPlugin plugin = ic.getPlugin();
										
										plugin.getPluginInterface().getUtilities().getSubscriptionManager().requestSubscription(
											sp,
											properties );
										
									}catch( Throwable e ){
										
										Debug.out( e );
									}
								}
							});
					}
				}
				
				soRSSButton.setVisible( visible );
			}
			
			SWTSkinObjectToggle soFilterButton = (SWTSkinObjectToggle) getSkinObject("filter-button");
			if (soFilterButton != null) {
				soFilterButton.addSelectionListener(new SWTSkinToggleListener() {
					@Override
					public void toggleChanged(SWTSkinObjectToggle so, boolean toggled) {
						soFilterArea.setVisible(toggled);
						Utils.relayout(soFilterArea.getControl().getParent());
					}
				});
			}
			
			Composite parent = (Composite) soFilterArea.getControl();
	
			Label label;
			FormData fd;
			GridLayout layout;
			int sepHeight = 20;
			
			Composite cRow = new Composite(parent, SWT.NONE);
			fd = Utils.getFilledFormData();
			cRow.setLayoutData(fd);
			RowLayout rowLayout = new RowLayout(SWT.HORIZONTAL);
			rowLayout.spacing = 5;
			rowLayout.marginBottom = rowLayout.marginTop = rowLayout.marginLeft = rowLayout.marginRight = 0; 
			rowLayout.center = true;
			cRow.setLayout(rowLayout);
			
			

			/////
			
			
			Composite cMinSeeds = new Composite(cRow, SWT.NONE);
			layout = new GridLayout(2, false);
			layout.marginWidth = 0;
			layout.marginBottom = layout.marginTop = layout.marginLeft = layout.marginRight = 0;
			cMinSeeds.setLayout(layout);
			
			Label lblMinSeeds = new Label(cMinSeeds, SWT.NONE);
			lblMinSeeds.setText(MessageText.getString("rcmview.filter.minSeeds"));
			Spinner spinMinSeeds = new Spinner(cMinSeeds, SWT.BORDER);
			spinMinSeeds.setMinimum(0);
			spinMinSeeds.setSelection(minSeeds);
			spinMinSeeds.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					minSeeds = ((Spinner) event.widget).getSelection();
					refilter();
				}
			});
			
			
			label = new Label(cRow, SWT.VERTICAL | SWT.SEPARATOR);
			label.setLayoutData(new RowData(-1, sepHeight));

			Composite cCreatedAgo = new Composite(cRow, SWT.NONE);
			layout = new GridLayout(2, false);
			layout.marginWidth = 0;
			layout.marginBottom = layout.marginTop = layout.marginLeft = layout.marginRight = 0;
			cCreatedAgo.setLayout(layout);
			Label lblCreatedAgo = new Label(cCreatedAgo, SWT.NONE);
			lblCreatedAgo.setText(MessageText.getString("rcmview.filter.createdAgo"));
			Spinner spinCreatedAgo = new Spinner(cCreatedAgo, SWT.BORDER);
			spinCreatedAgo.setMinimum(0);
			spinCreatedAgo.setMaximum(999);
			spinCreatedAgo.setSelection((int) (createdMsAgo / 86400000L));
			spinCreatedAgo.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					((Spinner) event.widget).setMaximum(999);
					createdMsAgo = ((Spinner) event.widget).getSelection() * 86400L*1000L;
					refilter();
				}
			});
			
				// min rank
			
			label = new Label(cRow, SWT.VERTICAL | SWT.SEPARATOR);
			label.setLayoutData(new RowData(-1, sepHeight));

			Composite cMinRank = new Composite(cRow, SWT.NONE);
			layout = new GridLayout(2, false);
			layout.marginWidth = 0;
			layout.marginBottom = layout.marginTop = layout.marginLeft = layout.marginRight = 0;
			cMinRank.setLayout(layout);
			Label lblMinRank = new Label(cMinRank, SWT.NONE);
			lblMinRank.setText(MessageText.getString("rcmview.filter.minRank"));
			Spinner spinMinRank = new Spinner(cMinRank, SWT.BORDER);
			spinMinRank.setMinimum(0);
			spinMinRank.setSelection(minRank);
			spinMinRank.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					minRank = ((Spinner) event.widget).getSelection();
					refilter();
				}
			});

				// min size
			
			label = new Label(cRow, SWT.VERTICAL | SWT.SEPARATOR);
			label.setLayoutData(new RowData(-1, sepHeight));

			Composite cMinSize = new Composite(cRow, SWT.NONE);
			layout = new GridLayout(2, false);
			layout.marginWidth = 0;
			layout.marginBottom = layout.marginTop = layout.marginLeft = layout.marginRight = 0;
			cMinSize.setLayout(layout);
			Label lblMinSize = new Label(cMinSize, SWT.NONE);
			lblMinSize.setText(MessageText.getString("rcmview.filter.minSize"));
			Spinner spinMinSize = new Spinner(cMinSize, SWT.BORDER);
			spinMinSize.setMinimum(0);
			spinMinSize.setMaximum(100*1024*1024);	// 100 TB should do...
			spinMinSize.setSelection(minSize);
			spinMinSize.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					minSize = ((Spinner) event.widget).getSelection();
					refilter();
				}
			});
			
				// show indirect
			
			label = new Label(cRow, SWT.VERTICAL | SWT.SEPARATOR);
			label.setLayoutData(new RowData(-1, sepHeight));

			Button chkShowPrivate = new Button(cRow, SWT.CHECK);
			chkShowPrivate.setText( MessageText.getString( "rcm.header.show_indirect" ));
			chkShowPrivate.setSelection(showIndirect );
			chkShowPrivate.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					showIndirect = ((Button) event.widget).getSelection();
					refilter();
				}
			});
			
			label = new Label(cRow, SWT.VERTICAL | SWT.SEPARATOR);
			label.setLayoutData(new RowData(-1, sepHeight));

			Button chkShowUnknownSeeds = new Button(cRow, SWT.CHECK);
			chkShowUnknownSeeds.setText(MessageText.getString("rcmview.filter.showUnknown"));
			chkShowUnknownSeeds.setSelection(showUnknownSeeds);
			chkShowUnknownSeeds.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					showUnknownSeeds = ((Button) event.widget).getSelection();
					refilter();
				}
			});
			
			final Button searchMore = new Button(cRow, SWT.PUSH);
			searchMore.setText( MessageText.getString( "rcm.menu.searchmore" ));
			searchMore.addListener(SWT.Selection, new Listener() {
				@Override
				public void handleEvent(Event event) {
					searchMore.setEnabled( false );
					
					SimpleTimer.addEvent(
						"enabler",
						SystemTime.getOffsetTime( 5*1000 ),
						new TimerEventPerformer() {
							
							@Override
							public void perform(TimerEvent event){
								Utils.execSWTThread(
									new Runnable()
									{
										@Override
										public void run()
										{
									
											if ( !searchMore.isDisposed()){
								
												searchMore.setEnabled( true );
											}
										}
									});
							}
						});
					
					if ( ds instanceof RCMItemContent ){
						
						((RCMItemContent)ds).search();
					}
				}});
			
			searchMore.setVisible( ds instanceof RCMItemContent );
			
			parent.layout(true);
		}

		if ( true || show_info_bar ){
			new InfoBarUtil(skinObject, "rcmview.infobar", false,
					"rcm.infobar", "rcm.view.infobar") {
				@Override
				public boolean allowShow() {
					return true;
				}
			};
		}
		
		return null;
	}
	
	private boolean 
	isOurContent(
		RelatedContent c) 
	{
		boolean show = 
			((c.getSeeds() >= minSeeds) || (showUnknownSeeds && c.getSeeds() < 0)) && 
			(createdMsAgo == 0 || (SystemTime.getCurrentTime() - c.getPublishDate() < createdMsAgo)) &&
			((c.getRank() >= minRank)) &&
			(c.getSize()==-1||(c.getSize() >= 1024L*1024*minSize)) &&
			(showIndirect || c.getHash() != null);
		
		if ( show ){
			
			show = RelatedContentUISWT.getSingleton().getPlugin().isVisible( c );
		}
		
		return( show );
	}


	protected void refilter() {
		if (tv_related_content != null) {
			tv_related_content.refilter();
		}
	}


	private void 
	initColumns(
		Core core )
	{
		synchronized( SBC_RCMView.class ){
			
			if ( columnsAdded ){
			
				return;
			}
		
			columnsAdded = true;
		}
		
		UIManager uiManager = PluginInitializer.getDefaultInterface().getUIManager();
		
		TableManager tableManager = uiManager.getTableManager();
		
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_New.COLUMN_ID,
					new TableColumnCreationListener() {
						@Override
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_New(column);
						}
					});

		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Rank.COLUMN_ID,
					new TableColumnCreationListener() {
						@Override
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Rank(column);
						}
					});
		
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Level.COLUMN_ID,
					new TableColumnCreationListener() {
						@Override
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Level(column);
						}
					});
			
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Title.COLUMN_ID,
					new TableColumnCreationListener() {
						@Override
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Title(column);
						}
					});

		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Actions.COLUMN_ID,
					new TableColumnCreationListener() {
						@Override
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Actions(column);
						}
					});
			
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Hash.COLUMN_ID,
					new TableColumnCreationListener() {
						@Override
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Hash(column);
						}
					});
			
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Tracker.COLUMN_ID,
					new TableColumnCreationListener() {
						@Override
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Tracker(column);
						}
					});
			
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Size.COLUMN_ID,
					new TableColumnCreationListener() {
						@Override
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Size(column);
						}
					});
		
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Created.COLUMN_ID,
					new TableColumnCreationListener() {
						@Override
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Created(column);
						}
					});
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_ChangedLocallyAgo.COLUMN_ID,
					new TableColumnCreationListener() {
						@Override
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_ChangedLocallyAgo(column);
						}
					});
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Seeds.COLUMN_ID,
					new TableColumnCreationListener() {
						@Override
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Seeds(column);
						}
					});

		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Peers.COLUMN_ID,
					new TableColumnCreationListener() {
						@Override
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Peers(column);
						}
					});

		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_FirstSeen.COLUMN_ID,
					new TableColumnCreationListener() {
						@Override
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_FirstSeen(column);
						}
					});
		
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_LastSeen.COLUMN_ID,
					new TableColumnCreationListener() {
						@Override
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_LastSeen(column);
						}
					});

		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_RelatedTo.COLUMN_ID,
					new TableColumnCreationListener() {
						@Override
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_RelatedTo(column);
						}
					});
		
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Tags.COLUMN_ID,
					new TableColumnCreationListener() {
						@Override
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Tags(column);
						}
					});
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Networks.COLUMN_ID,
					new TableColumnCreationListener() {
						@Override
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Networks(column);
						}
					});
		
		tableManager.registerColumn(
				RelatedContent.class, 
				ColumnRC_Rating.COLUMN_ID,
					new TableColumnCreationListener() {
						@Override
						public void tableColumnCreated(TableColumn column) {
							new ColumnRC_Rating(column);
						}
					});
	}

	@Override
	public Object
	dataSourceChanged(
		SWTSkinObject skinObject, Object params) 
	{
		//hideView();
		
		ds = params;
		
		//showView();
		
		return( super.dataSourceChanged(skinObject, params));
	}

	
	private void
	showView()
	{
		RelatedContentUISWT ui = RelatedContentUISWT.getSingleton();
		
		if ( ui != null && !ui.getPlugin().hasFTUXBeenShown()){
			
			ui.showFTUX(getSkinObject("rcm-list"));
			
		}else{
			
			SWTSkinObject so_list = getSkinObject("rcm-list");

			if ( so_list != null ){
				
				so_list.setVisible(true);
			}
		}


		SWTSkinObject so_list = getSkinObject("rcm-list");
		
		if ( so_list != null ){
			
			Composite composite = (Composite) so_list.getControl();
			
			tab_folder = new CTabFolder(composite, SWT.LEFT);

			tab_folder.setLayoutData( Utils.getFilledFormData());
			
			CTabItem list_item = new CTabItem(tab_folder, SWT.NULL);

			list_item.setText( MessageText.getString( "rcm.list" ));

			Composite list_composite = new Composite( tab_folder, SWT.NULL );

			list_item.setControl( list_composite );
		
			list_composite.setLayout(new FormLayout());
			list_composite.setLayoutData( Utils.getFilledFormData());											

			CTabItem explore_item = new CTabItem(tab_folder, SWT.NULL);

			explore_item.setText( MessageText.getString( "rcm.explore" ));

			Composite explore_composite = new Composite( tab_folder, SWT.NULL );

			explore_composite.setLayout(new FillLayout());
			explore_composite.setLayoutData( Utils.getFilledFormData());

			explore_item.setControl( explore_composite );
						
			tab_folder.setSelection( list_item );			
								
			TableViewSWT<RelatedContent> table = initTable( list_composite );
			
			initExplore( table, explore_composite );
						
			composite.layout( true );
		}
		
		paramSourceListener = new ParameterListener() {
			@Override
			public void parameterChanged(String parameterName) {
				refilter();
			}
		};
		COConfigurationManager.addParameterListener(RCMPlugin.PARAM_SOURCES_LIST, paramSourceListener);
	}
	
	private void
	hideView()
	{
		synchronized( this ){
			
			if ( tv_related_content != null ){
				
				tv_related_content.delete();
				
				tv_related_content = null;
			}
			
			if ( general_ops_panel != null ){
				
				general_ops_panel.delete();
				
				general_ops_panel = null;
			}
			
			if (manager != null && current_rcm_listener != null) {
				
				manager.removeListener( current_rcm_listener );
				
				current_rcm_listener = null;
			}
		}

		Utils.disposeSWTObjects(new Object[] {
			tab_folder,
		});

		COConfigurationManager.removeParameterListener(RCMPlugin.PARAM_SOURCES_LIST, paramSourceListener);
	}
	
	@Override
	public Object
	skinObjectShown(
		SWTSkinObject 	skinObject, 
		Object 			params ) 
	{
		super.skinObjectShown(skinObject, params);

		showView();
		
		return null;
	}

	@Override
	public Object
	skinObjectHidden(
		SWTSkinObject 	skinObject, 
		Object 			params ) 
	{
		hideView();
		
		return( super.skinObjectHidden(skinObject, params));
	}

	@Override
	public Object
	skinObjectDestroyed(
		SWTSkinObject 	skinObject, 
		Object 			params ) 
	{
		synchronized( this ){
			
			if ( tv_related_content != null ){
				
				tv_related_content.delete();
				
				tv_related_content = null;
			}
			
			if ( general_ops_panel != null ){
				
				general_ops_panel.delete();
				
				general_ops_panel = null;
			}

			if (manager != null && current_rcm_listener != null) {
				
				manager.removeListener( current_rcm_listener );
				
				current_rcm_listener = null;
			}
		}
		
		Utils.disposeSWTObjects(new Object[] {
			tab_folder,
		});

		if ( space_reserved ){
		
			space_reserved = false;
			
			manager.releaseTemporarySpace();
		}
		
		return( super.skinObjectDestroyed(skinObject, params));
	}
	
	private TableViewSWT<RelatedContent> 
	initTable(
		Composite control ) 
	{
		TableColumnManager tcManager = TableColumnManager.getInstance();

		tv_related_content = TableViewFactory.createTableViewSWT(
				RelatedContent.class, 
				TABLE_RCM,
				TABLE_RCM, 
				new TableColumnCore[0],
				ColumnRC_Rank.COLUMN_ID, 
				SWT.MULTI | SWT.FULL_SELECTION | SWT.VIRTUAL );
		
		if (txtFilter != null) {
			tv_related_content.enableFilterCheck(txtFilter, this);
		}
		
		tv_related_content.setRowDefaultHeight(16);
		
		SWTSkinObject soSizeSlider = getSkinObject("table-size-slider");
		if (soSizeSlider instanceof SWTSkinObjectContainer) {
			SWTSkinObjectContainer so = (SWTSkinObjectContainer) soSizeSlider;
			if (!tv_related_content.enableSizeSlider(so.getComposite(), 16, 100)) {
				so.setVisible(false);
			}
		}
		
		tcManager.setDefaultColumnNames(TABLE_RCM, new String[] {
					ColumnRC_New.COLUMN_ID,
					ColumnRC_Rank.COLUMN_ID,
					ColumnRC_Title.COLUMN_ID,
					ColumnRC_Actions.COLUMN_ID,
					ColumnRC_Size.COLUMN_ID,
					ColumnRC_Created.COLUMN_ID,
					ColumnRC_Seeds.COLUMN_ID,
					ColumnRC_Peers.COLUMN_ID,
					ColumnRC_Tags.COLUMN_ID,
					ColumnRC_Rating.COLUMN_ID,
		});
		
		if ( ds instanceof RCMItemContent ){
			
			if (((RCMItemContent)ds).isPopularity()){
		
					// force view to be sorted by peers, descending
						
				tcManager.setDefaultSortColumnName(TABLE_RCM, ColumnRC_Peers.COLUMN_ID);
				
				TableColumnCore tcc = tcManager.getTableColumnCore( TABLE_RCM, ColumnRC_Peers.COLUMN_ID );
				
				if ( tcc != null ){
					
					tcc.setSortAscending( false);
				}
			}
		}
		
		Composite table_parent = new Composite(control, SWT.NONE);
		table_parent.setLayoutData(Utils.getFilledFormData());
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = layout.verticalSpacing = layout.horizontalSpacing = 0;
		table_parent.setLayout(layout);

		tv_related_content.addSelectionListener(new TableSelectionListener() {

			@Override
			public void
			selected(
				TableRowCore[] _rows)
			{
				updateSelectedContent();
			}

			@Override
			public void mouseExit(TableRowCore row) {
			}

			@Override
			public void mouseEnter(TableRowCore row) {
			}

			@Override
			public void focusChanged(TableRowCore focus) {
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.refreshIconBar();
				}
			}

			@Override
			public void deselected(TableRowCore[] rows) {
				updateSelectedContent();
			}

			@Override
			public void defaultSelected(TableRowCore[] rows, int stateMask) {
			}
			
			private void
			updateSelectedContent()
			{
				TableRowCore[] rows = tv_related_content.getSelectedRows();
				
				ArrayList<ISelectedContent>	valid = new ArrayList<ISelectedContent>();

				last_selected_content.clear();
				
				for (int i=0;i<rows.length;i++){
					
					final RelatedContent rc = (RelatedContent)rows[i].getDataSource();
					
					last_selected_content.add( rc );
					
					if ( rc.getHash() != null && rc.getHash().length > 0 ){
						
						SelectedContent sc = new SelectedContent(Base32.encode(rc.getHash()), rc.getTitle());
						
						sc.setDownloadInfo(new DownloadUrlInfo(	RCMPlugin.getMagnetURI( rc )));
						
						valid.add(sc);
					}else{
						
						valid.add( new SelectedContent());
					}
				}
				
				ISelectedContent[] sels = valid.toArray( new ISelectedContent[valid.size()] );
				
				SelectedContentManager.changeCurrentlySelectedContent("IconBarEnabler",
						sels, tv_related_content);
				
				UIFunctions uiFunctions = UIFunctionsManager.getUIFunctions();
				if (uiFunctions != null) {
					uiFunctions.refreshIconBar();
				}
			}
		}, false);

		tableLifeCycleListener = new MyTableLifeCycleListener();
		tv_related_content.addLifeCycleListener(tableLifeCycleListener);

		
		tv_related_content.addMenuFillListener(
			new TableViewSWTMenuFillListener() 
			{
				@Override
				public void
				fillMenu(String sColumnName, Menu menu)
				{
					Object[] _related_content = tv_related_content.getSelectedDataSources().toArray();

					final RelatedContent[] related_content = new RelatedContent[_related_content.length];

					System.arraycopy(_related_content, 0, related_content, 0, related_content.length);

					addMenus( menu, related_content );
					
					new MenuItem(menu, SWT.SEPARATOR );

					final MenuItem remove_item = new MenuItem(menu, SWT.PUSH);

					remove_item.setText(MessageText.getString("azbuddy.ui.menu.remove"));

					Utils.setMenuItemImage( remove_item, "delete" );

					remove_item.addSelectionListener(new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							userDelete(related_content);
						}

					});
				}

				@Override
				public void
				addThisColumnSubMenu(
					String columnName, Menu menuThisColumn) 
				{
				}
			});


		tv_related_content.addKeyListener(
				new KeyListener()
				{
					@Override
					public void
					keyPressed(
						KeyEvent e )
					{
						if ( e.stateMask == 0 && e.keyCode == SWT.DEL ){
							
							Object[] selected;
							
							synchronized (this) {
								
								if ( tv_related_content == null ){
									
									selected = new Object[0];
									
								}else{
								
									selected = tv_related_content.getSelectedDataSources().toArray();
								}
							}
							
							RelatedContent[] content = new RelatedContent[ selected.length ];
							
							for ( int i=0;i<content.length;i++){
								
								content[i] = (RelatedContent)selected[i];
							}
							
							userDelete( content );
							
							e.doit = false;
						}
					}
					
					@Override
					public void
					keyReleased(
						KeyEvent arg0 ) 
					{
					}
				});
		
		if (ds instanceof RCMItemSubView) {
  		tv_related_content.addCountChangeListener(new TableCountChangeListener() {
  			
  			@Override
			  public void rowRemoved(TableRowCore row) {
  				updateCount();
  			}
  			
  			@Override
			  public void rowAdded(TableRowCore row) {
  				updateCount();
  			}

				private void updateCount() {
					int size = tv_related_content == null ? 0 : tv_related_content.size(false);
					((RCMItemSubView) ds).setCount(size);
				}
  		});
  		((RCMItemSubView) ds).setCount(0);
		}
		
		tv_related_content.initialize( table_parent );
		
		return( tv_related_content );
	}
	
	private void
	addMenus(
		Menu				menu,
		RelatedContent[]	related_content )
	{
		RelatedContentUISWT ui = RelatedContentUISWT.getSingleton();
		
		final Image	swarm_image = ui==null?null:ui.getSwarmImage();

		final MenuItem assoc_item = new MenuItem(menu, SWT.PUSH);

		if ( swarm_image != null && !swarm_image.isDisposed()){
		
			assoc_item.setImage( swarm_image );
		}
		
		assoc_item.setText(MessageText.getString("rcm.menu.discovermore"));

		final ArrayList<RelatedContent> assoc_ok = new ArrayList<RelatedContent>();
		
		for ( RelatedContent c: related_content ){
			
			byte[] hash = c.getHash();
			
			if ( hash != null && hash.length > 0 ){
				
				assoc_ok.add( c );
			}
		}
		
		assoc_item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e ){
				
				int	 i = 0;
				
				RelatedContentUISWT ui = RelatedContentUISWT.getSingleton();
				
				if ( ui != null ){
					
					for ( RelatedContent c: assoc_ok ){
					
						ui.addSearch( c.getHash(), c.getNetworks(), c.getTitle());
						
						i++;
						
						if ( i > 8 ){
							
							break;
						}
					}
				}
			};
		});
		
		if ( assoc_ok.size() == 0 ){
			
			assoc_item.setEnabled( false );
		}
		
		MenuItem item;
	
		new MenuItem(menu, SWT.SEPARATOR );

		item = new MenuItem(menu, SWT.PUSH);
		item.setText(MessageText.getString("rcm.menu.google.hash"));
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String s = ByteFormatter.encodeString(related_content[0].getHash());
				String URL = "https://google.com/search?q=" + UrlUtils.encode(s);
				launchURL(URL);
			};
		});

		if ( related_content.length==1 ){
			byte[] hash = related_content[0].getHash();
			item.setEnabled(hash!=null&&hash.length > 0 );
		}else{
			item.setEnabled(false);
		}
		
		item = new MenuItem(menu, SWT.PUSH);
		item.setText(MessageText.getString("rcm.menu.gis"));
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String s = related_content[0].getTitle();
				s = s.replaceAll("[-_]", " ");
				String URL = "http://images.google.com/images?q=" + UrlUtils.encode(s);
				launchURL(URL);
			}

		});

		item.setEnabled( related_content.length==1 );
		
		item = new MenuItem(menu, SWT.PUSH);
		item.setText(MessageText.getString("rcm.menu.google"));
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String s = related_content[0].getTitle();
				s = s.replaceAll("[-_]", " ");
				String URL = "https://google.com/search?q=" + UrlUtils.encode(s);
				launchURL(URL);
			};
		});

		item.setEnabled( related_content.length==1 );

		item = new MenuItem(menu, SWT.PUSH);
		item.setText(MessageText.getString("rcm.menu.bis"));
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				String s = related_content[0].getTitle();
				s = s.replaceAll("[-_]", " ");
				String URL = "http://www.bing.com/images/search?q=" + UrlUtils.encode(s);
				launchURL(URL);
			};
		});

		item.setEnabled( related_content.length==1 );

		new MenuItem(menu, SWT.SEPARATOR );
		
		item = new MenuItem(menu, SWT.PUSH);
		item.setText(MessageText.getString("rcm.menu.uri"));
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				ClipboardCopy.copyToClipBoard( RCMPlugin.getMagnetURI(related_content[0]));
			};
		});
		
		if ( related_content.length==1 ){
			byte[] hash = related_content[0].getHash();
			item.setEnabled(hash!=null&&hash.length > 0 );
		}else{
			item.setEnabled(false);
		}
		
		item = new MenuItem(menu, SWT.PUSH);
		item.setText(MessageText.getString("rcm.menu.uri.i2p"));
		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				
				String[] magnet_uri = { RCMPlugin.getMagnetURI(related_content[0]) };
				
				UrlUtils.extractNetworks( magnet_uri );
				
				String i2p_only_uri = magnet_uri[0] + "&net=" + UrlUtils.encode( AENetworkClassifier.AT_I2P );
				
				ClipboardCopy.copyToClipBoard( i2p_only_uri );
			};
		});
		
		if ( related_content.length==1 ){
			byte[] hash = related_content[0].getHash();
			item.setEnabled(hash!=null&&hash.length > 0 );
		}else{
			item.setEnabled(false);
		}
	}
	private void
	initExplore(
		TableViewSWT<RelatedContent>		table,
		Composite							comp )
	{
		general_ops_panel = new OpsContainer( table, comp );
	}
	
	private class
	OpsContainer
	{
		private final Object	gop_lock = new Object();
		
		private TableViewSWT<RelatedContent>		table;
		private GeneralOpsPanel						gop;
		
		private Map<RelatedContent,RCMActivity>		activities = new TreeMap<>(
				(o1,o2)->{
					int result =  o1.getSeeds() - o2.getSeeds();
					
					if ( result == 0 ){
						
						result = o1.getTitle().compareTo( o2.getTitle());
					}
					
					return( result );
				});
		
		private Set<RelatedContent>	found_rc	= new HashSet<>();
		
		private
		OpsContainer(
			TableViewSWT<RelatedContent>		_table,
			Composite							comp )
		{
			table	= _table;
			
			gop = new GeneralOpsPanel( comp );
		}
		
		private void
		delete()
		{	
		}
		
		private void
		refresh()
		{
			TableRowCore[] rows = table.getRows();
			
			synchronized( gop_lock ){

				for ( TableRowCore row: rows ){
					
					RelatedContent rc = (RelatedContent)row.getDataSource();
					
						
					if ( found_rc.contains( rc )){
						
						continue;
					}
						
					found_rc.add( rc );
					
					if ( rc.getHash() == null || rc instanceof RelatedContentUISWT.SubsRelatedContent ){
						
						continue;
					}
					
					if ( activities.get( rc ) == null ){
						
						RCMActivity act = new RCMActivity( rc );
						
						activities.put( rc, act );
						
						if ( activities.size() > 10 ){
							
							Iterator<RCMActivity> it = activities.values().iterator();
						
							RCMActivity to_delete = it.next();
								
							it.remove();

							if ( to_delete == act ){
								
								act = null;
								
							}else{
																
								gop.activityChanged( to_delete,  true );
							}						
						}
						
						if ( act != null ){
						
							gop.activityChanged( act,  false );
						}
					}
				}
			}
			
			gop.refresh();
		}
	
		private class
		RCMActivity
			implements GeneralOpsPanel.Activity, GeneralOpsPanel.State
		{
			private RCMActivityNode	root;
			
			private
			RCMActivity(
				RelatedContent	_rc )
			{
				root = new RCMActivityNode(_rc);
				
				root.addChild(new RCMActivityNode(_rc));
			}
			
			public String
			getDescription()
			{
				return( "" );
			}
			
			public int
			getType()
			{
				return( GeneralOpsPanel.Activity.TYPE_3 );
			}
			
			public boolean
			isQueued()
			{
				return( false );
			}
			
			public State
			getCurrentState()
			{
				return( this );
			}
			
			public Node
			getRootNode()
			{
				return( root );
			}
			
			public int
			getDepth()
			{
				return( 2 );
			}
			
			public String
			getResult()
			{
				return( "" );
			}
		}
	
		private class
		RCMActivityNode
			implements GeneralOpsPanel.Node
		{
			private RelatedContent		rc;
		
			private CopyOnWriteList<Node>		kids = new CopyOnWriteList<>();
			
			private boolean	searched 	= false;
			private boolean search_done = false;
			
			private Map<RelatedContent,RCMActivityNode>		results = new TreeMap<>(
					(o1,o2)->{
						int result =  o1.getSeeds() - o2.getSeeds();
						
						if ( result == 0 ){
							
							result = o1.getTitle().compareTo( o2.getTitle());
						}
						
						return( result );
					});
			
			private
			RCMActivityNode(
				RelatedContent	_rc )
			{
				rc		= _rc;
			}
			
			public String
			getName()
			{
				return( rc.getTitle());
			}
			
			@Override
			public Object 
			eventOccurred(NodeEvent ev)
			{
				if ( ev.getType() == NodeEvent.ET_MENU ){
					
					Menu menu = (Menu)ev.getData();
					
					MenuItem mi = new MenuItem( menu, SWT.PUSH );
					
					mi.setText( MessageText.getString("v3.MainWindow.button.download"));
					
					new MenuItem(menu, SWT.SEPARATOR );
					
					addMenus( menu, new RelatedContent[]{ rc });
					
					mi.addSelectionListener(
						SelectionListener.widgetSelectedAdapter(
							(e)->{
								
								TorrentOpener.openTorrent( RCMPlugin.getMagnetURI( rc ));
							}));
						
				}else if ( ev.getType() == NodeEvent.ET_CLICKED ){
					
					if ( searched ){
						
						return( null );
					}
					
					searched = true;
					
					try{
						manager.lookupContent( 
							rc.getHash(), 
							rc.getNetworks(),
							new RelatedContentLookupListener(){
								
								@Override
								public void lookupStart(){
								}
								
								@Override
								public void lookupFailed(ContentException error){
									search_done = true;
								}
								
								@Override
								public void lookupComplete(){
									search_done = true;
								}
								
								@Override
								public void contentFound(RelatedContent[] content){
									
									synchronized( gop_lock ){
										
										for ( RelatedContent rc: content ){
											
											if ( found_rc.contains( rc )){
												
												continue;
											}
											
											found_rc.add( rc );
											
											if ( rc.getHash() == null || rc instanceof RelatedContentUISWT.SubsRelatedContent ){
												
												continue;
											}
											
											RCMActivityNode node = new RCMActivityNode( rc );
											
											results.put( rc, node );
											
											if ( results.size() > 10 ){
																					
												Iterator<RCMActivityNode> it = results.values().iterator();
																				
												RCMActivityNode to_delete = it.next();
												
												it.remove();
																								
												if ( to_delete == node ){
													
													node = null;
													
												}else{
													
													removeChild( to_delete );
												}
												
											}
											
											if ( node != null ){
											
												addChild( node );
											}
										}
									}
								}
							});
					}catch( Throwable e){
						
						Debug.out( e );
					}
				}
				
				return( null );
			}
			
			@Override
			public int 
			getType()
			{
				return( searched?search_done?TYPE_3:TYPE_2:TYPE_1 );
			}
			
			private void
			addChild(
				Node	n )
			{				
				kids.add( n );
			}
			
			private void
			removeChild(
				Node	n )
			{				
				kids.remove( n );
			}
			
			public List<Node>
			getChildren()
			{
				return( kids.getList());
			}
		}
	}
	
	private void userDelete(RelatedContent[] related_content) {
		TableRowCore focusedRow = tv_related_content.getFocusedRow();
		TableRowCore focusRow = null;
		if (focusedRow != null) {
			int i = tv_related_content.indexOf(focusedRow);
			int size = tv_related_content.size(false);
			if (i < size - 1) {
				focusRow = tv_related_content.getRow(i + 1);
			} else if (i > 0) {
				focusRow = tv_related_content.getRow(i - 1);
			}
		}
		
		List<RelatedContent> real_rc = new ArrayList<>();
		
		for ( RelatedContent rc: related_content ){
		
			if ( rc instanceof RelatedContentUISWT.SubsRelatedContent ){
				
				tv_related_content.removeDataSource( rc );
				
			}else{
				
				real_rc.add( rc );
			}
		}
		
		if ( real_rc.size() > 0 ){
			manager.delete(real_rc.toArray( new RelatedContent[ real_rc.size()]));
		}
		
		if (focusRow != null) {
	  		tv_related_content.setSelectedRows(new TableRowCore[] {
	  			focusRow
	  		});
		}
	};

	@Override
	public String
	getUpdateUIName() 
	{
		return( "RCMView" );
	}

	@Override
	public void
	updateUI() 
	{
		if ( tv_related_content != null ){
			
			tv_related_content.refreshTable( false );
		}
		
		if ( general_ops_panel != null ){
			
			general_ops_panel.refresh();
		}
	}


	// @see com.biglybt.ui.swt.views.table.TableViewFilterCheck#filterCheck(java.lang.Object, java.lang.String, boolean)
	@Override
	public boolean filterCheck(RelatedContent ds, String filter, boolean regex) {
		if (!isOurContent(ds)) {
			return false;
		}

		if ( filter == null || filter.length() == 0 ){
			
			return( true );
		}

		try {
			String name = ds.getTitle();
			String s = regex ? filter : "\\Q" + filter.replaceAll("[|;]", "\\\\E|\\\\Q") + "\\E";
  		Pattern pattern = Pattern.compile(s, Pattern.CASE_INSENSITIVE);
  
  		return pattern.matcher(name).find();
		} catch (Exception e) {
			return true;
		}
	}
	
	// @see com.biglybt.ui.swt.views.table.TableViewFilterCheck#filterSet(java.lang.String)
	@Override
	public void filterSet(String filter) {
	}

	// @see com.biglybt.pif.ui.toolbar.UIToolBarActivationListener#toolBarItemActivated(ToolBarItem, long, java.lang.Object)
	@Override
	public boolean
	toolBarItemActivated(
		ToolBarItem item, 
		long activationType,
		Object datasource ) 
	{
		if ( tv_related_content == null || !tv_related_content.isVisible()){
			
			return( false );
		}
		if (item.getID().equals("remove")) {
			
			Object[] _related_content = tv_related_content.getSelectedDataSources().toArray();
			
			if ( _related_content.length > 0 ){
				
				RelatedContent[] related_content = new RelatedContent[_related_content.length];
				
				System.arraycopy( _related_content, 0, related_content, 0, related_content.length );
				
				userDelete(related_content);
			
				return true;
			}
		}
		
		return false;
	}

	@Override
	public void refreshToolBarItems(Map<String, Long> list) {
		if (tv_related_content == null || !tv_related_content.isVisible()) {
			return;
		}
		
			// make sure we're operating on a selection we understand...
		
		ISelectedContent[] content = SelectedContentManager.getCurrentlySelectedContent();
		
		for ( ISelectedContent c: content ){
			
			if ( c.getDownloadManager() != null ){
				
				return;
			}
		}
		
		list.put("remove", tv_related_content.getSelectedDataSources().size() > 0 ? UIToolBarItem.STATE_ENABLED : 0);
	}

	private void launchURL(String s) {
		Program program = Program.findProgram(".html");
		if (program != null && program.getName().contains("Chrome")) {
			try {
				Field field = Program.class.getDeclaredField("command");
				field.setAccessible(true);
				String command = (String) field.get(program);
				command = command.replaceAll("%[1lL]", Matcher.quoteReplacement(s));
				command = command.replace(" --", "");
				PluginInitializer.getDefaultInterface().getUtilities().createProcess(command + " -incognito");
			} catch (Exception e1) {
				e1.printStackTrace();
				Utils.launch(s);
			}
		} else {
			Utils.launch(s);
		}
	};

	private class MyTableLifeCycleListener implements TableLifeCycleListener {
		private Set<RelatedContent> content_set = new HashSet<>();

		private int liveness_marker;

		private boolean initial_selection_handled = false;

		@Override
		public void tableLifeCycleEventOccurred(TableView tv, int eventType, Map<String, Object> data) {
			switch (eventType) {
				case EVENT_TABLELIFECYCLE_INITIALIZED:
					tableViewInitialized();
					break;
				case EVENT_TABLELIFECYCLE_DESTROYED:
					tableViewDestroyed();
					break;
			}
		}


		private void
		tableViewInitialized() {
			final int current_liveness_marker = ++liveness_marker;

			current_rcm_listener =
					new RelatedContentManagerListener() {
						@Override
						public void
						contentFound(
								RelatedContent[] content) {
						}

						@Override
						public void
						contentChanged(
								RelatedContent[] content) {
							if (tv_related_content == null) {
								return;
							}

							final List<RelatedContent> hits = new ArrayList<>(content.length);

							synchronized (content_set) {

								if (liveness_marker != current_liveness_marker) {

									return;
								}

								for (RelatedContent c : content) {

									if (content_set.contains(c)) {

										hits.add(c);
									}
								}
							}

							if (hits.size() > 0) {

								for (RelatedContent rc : hits) {

									TableRowCore row = tv_related_content.getRow(rc);

									if (row != null) {

										row.refresh(true);
									}
								}
							}
						}

						@Override
						public void
						contentRemoved(
								final RelatedContent[] content) {
							final List<RelatedContent> hits = new ArrayList<>(content.length);

							synchronized (content_set) {

								if (liveness_marker != current_liveness_marker) {

									return;
								}

								for (RelatedContent c : content) {

									if (content_set.remove(c)) {

										hits.add(c);
									}
								}
							}

							if (hits.size() > 0) {

								if (tv_related_content != null) {

									tv_related_content.removeDataSources(hits.toArray(new RelatedContent[hits.size()]));
								}
							}
						}

						@Override
						public void
						contentChanged() {
							if (tv_related_content != null) {

								tv_related_content.refreshTable(false);
							}
						}

						@Override
						public void
						contentReset() {
							if (tv_related_content != null) {

								tv_related_content.removeAllTableRows();
							}
						}
					};

			manager.addListener(current_rcm_listener);

			Object data_source = mdi_entry == null ? ds : mdi_entry.getDatasource();

			if (data_source instanceof RelatedContentEnumerator) {

				final TableViewSWT<RelatedContent> f_table = tv_related_content;

				((RelatedContentEnumerator) data_source).enumerate(
						new RelatedContentEnumerator.RelatedContentEnumeratorListener() {
							@Override
							public void
							contentFound(
									RelatedContent[] content) {
								ArrayList<RelatedContent> new_content = null;

								synchronized (content_set) {

									if (liveness_marker != current_liveness_marker) {

										return;
									}

									for (RelatedContent c : content) {

										if (content_set.contains(c)) {

											if (new_content == null) {

												new_content = new ArrayList<RelatedContent>(content.length);

												for (RelatedContent c2 : content) {

													if (c == c2) {

														break;
													}

													if (isOurContent(c2)) {

														new_content.add(c2);
													}
												}
											}
										} else {

											if (new_content != null) {

												if (isOurContent(c)) {

													new_content.add(c);
												}
											}
										}
									}

									if (new_content != null) {

										content = new_content.toArray(new RelatedContent[new_content.size()]);
									}

									content_set.addAll(Arrays.asList(content));
								}

								if (content.length > 0) {

									final RelatedContent[] f_content = content;

									Utils.execSWTThread(
											new Runnable() {
												@Override
												public void
												run() {
													if (tv_related_content == f_table) {

														synchronized (content_set) {

															if (liveness_marker != current_liveness_marker) {

																return;
															}
														}

														f_table.addDataSources(f_content);

														if (!initial_selection_handled) {

															// get user's last selection back, if any

															initial_selection_handled = true;

															final List<TableRowCore> selected_rows = new ArrayList<TableRowCore>();

															if (last_selected_content.size() > 0) {

																f_table.processDataSourceQueueSync();
															}

															for (RelatedContent rc : last_selected_content) {

																TableRowCore row = f_table.getRow(rc);

																if (row != null) {

																	selected_rows.add(row);
																}
															}

															if (selected_rows.size() > 0) {

																Utils.execSWTThreadLater(
																		1,
																		new Runnable() {
																			@Override
																			public void
																			run() {
																				// selection visible logic requires viewport to be initialised before it can scroll
																				// properly so we need to defer this action

																				f_table.setSelectedRows(selected_rows.toArray(new TableRowCore[selected_rows.size()]));
																			}
																		});
															}
														}
													}
												}
											});
								}
							}

						});
			}
		}

		private void
		tableViewDestroyed() {
			manager.removeListener(current_rcm_listener);

			synchronized (content_set) {

				liveness_marker++;

				content_set.clear();
			}
		}
	}
}
