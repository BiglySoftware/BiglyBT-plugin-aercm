package com.aelitis.plugins.rcmplugin;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

import com.aelitis.plugins.rcmplugin.RelatedContentUISWT.RCMItemSubView;
import com.aelitis.plugins.rcmplugin.RelatedContentUISWT.RCMItemSubViewEmpty;
import com.aelitis.plugins.rcmplugin.RelatedContentUISWT.RCMItemSubViewListener;
import com.biglybt.core.content.RelatedContentManager;
import com.biglybt.core.internat.MessageText;
import com.biglybt.core.tag.Tag;
import com.biglybt.core.util.AENetworkClassifier;
import com.biglybt.pifimpl.local.PluginCoreUtils;
import com.biglybt.ui.common.viewtitleinfo.ViewTitleInfo;
import com.biglybt.ui.common.viewtitleinfo.ViewTitleInfoManager;
import com.biglybt.ui.mdi.MdiEntry;
import com.biglybt.ui.mdi.MdiEntryVitalityImage;
import com.biglybt.ui.swt.Utils;
import com.biglybt.ui.swt.imageloader.ImageLoader;
import com.biglybt.ui.swt.pif.UISWTView;
import com.biglybt.ui.swt.skin.SWTSkin;
import com.biglybt.ui.swt.skin.SWTSkinObject;
import com.biglybt.ui.swt.skin.SWTSkinObjectListener;

import com.biglybt.pif.disk.DiskManagerFileInfo;
import com.biglybt.pif.download.Download;
import com.biglybt.pif.torrent.Torrent;

public class RCM_SubViewHolder
	implements ViewTitleInfo
{
	private static final boolean TUX_CONTINUOUS_SEARCH = false;
	private final RCMPlugin		plugin;
	private final SWTSkin		skin;
	private final UISWTView 	view;
	private MdiEntryVitalityImage spinnerVitality;


	private RCMItemSubView		current_data_source;
	
	private Download			dl 		= null;
	private DiskManagerFileInfo	dl_file = null;
	private String[] search_strings;

	private boolean				related_mode = true;

	private ImageLabel			status_img_label;
	private Label 				status_label;
	private Button 				button_more;
	private Button[]			buttons_toggle;
	
	private boolean 			current_rm;
	private Download			current_dl;
	private DiskManagerFileInfo	current_file;
	
	private int					current_count;
	
	private Image[]			vitality_images;
	private Image			 swarm_image;
	
	private boolean initialized;
	private Composite parent;
	private Object datasource;
	
	public
	RCM_SubViewHolder(
		RCMPlugin	_plugin,
		UISWTView 	_view,
		SWTSkin		_skin  )
	{
		plugin	= _plugin;
		view 	= _view;
		skin	= _skin;
		if (view instanceof MdiEntry) {
			spinnerVitality = ((MdiEntry) view).addVitalityImage(RelatedContentUISWT.SPINNER_IMAGE_ID);
			spinnerVitality.setVisible(false);
		}
	}
	
	protected void
	initialise(
		Composite		parent,
		Image swarm_image)
	{		
		
		this.parent = parent;
		this.swarm_image = swarm_image;
		vitality_images = ImageLoader.getInstance().getImages( RelatedContentUISWT.SPINNER_IMAGE_ID );


		Composite header = new Composite( parent, SWT.NONE );
		
		GridLayout header_layout = new GridLayout(5, false);
		header_layout.marginHeight = 0;
		
		header.setLayout( header_layout );
					
		header.setLayoutData( new GridData( GridData.FILL_HORIZONTAL ));
		
		status_img_label = new ImageLabel( header, swarm_image );

		status_label = new Label(header, SWT.NONE);
		
		final Button button_related = new Button( header, SWT.RADIO );
		
		button_related.setText( MessageText.getString( "rcm.subview.relto" ));
		
		button_related.setSelection( true );
					
		final Button button_size = new Button( header, SWT.RADIO );
		
		button_size.setText( MessageText.getString( "rcm.subview.filesize" ));

		Composite cMore = new Composite(header, SWT.NONE);
		cMore.setLayout(new FillLayout());
		GridData gridData = new GridData(SWT.RIGHT, SWT.FILL, true, false);
		cMore.setLayoutData(gridData);

		button_more = new Button(cMore,
				TUX_CONTINUOUS_SEARCH ? SWT.TOGGLE : SWT.PUSH);
		button_more.setText(MessageText.getString("rcm.menu.searchmore"));
		button_more.addListener(SWT.Selection, e -> {
			if (current_data_source == null) {
				return;
			}
			if (!TUX_CONTINUOUS_SEARCH) {
				current_data_source.search();
				return;
			}
			Button btn = (Button) e.widget;
			if (current_data_source.isSearching()) {
				if (current_data_source.isKeepLookingUp()) {
					current_data_source.keepLookingUp(false);
					btn.setSelection(false);
				} else {
					current_data_source.keepLookingUp(true);
					btn.setSelection(true);
				}
			} else {
				current_data_source.search();
				btn.setSelection(false);
			}
		});

		Listener but_list  = 
			new Listener()
			{
				@Override
				public void
				handleEvent(
					Event arg0) 
				{
					related_mode = button_related.getSelection();
					
					doSearch();
				}
			};

		button_related.addListener( SWT.Selection, but_list );
		button_size.addListener( SWT.Selection, but_list );
		
		buttons_toggle = new Button[]{ button_related, button_size };
		
		buttons_toggle[0].setEnabled( dl != null );
		buttons_toggle[1].setEnabled( dl_file != null );
				
		
		Composite skin_area = new Composite( parent, SWT.NULL );
		
		skin_area.setLayout( new FormLayout());
		
		skin_area.setLayoutData( new GridData( GridData.FILL_BOTH ));
		
		skin.initialize( skin_area, "subskin" );
		
		if (dl != null || dl_file != null || search_strings != null) {
			doSearch();
		}

		initialized = true;

		SWTSkinObject so = skin.getSkinObjectByID( "rcmsubskinview" );
		
		RCMItemSubView	ds = current_data_source;
		
		if ( ds == null ){
			
			ds = new RCMItemSubViewEmpty( plugin );
		}
		
		so.triggerListeners( SWTSkinObjectListener.EVENT_DATASOURCE_CHANGED, ds );
				
		so.setVisible( true );

		skin.layout();
		
	}

	protected void
	setDataSource(
		Object		obj )
	{
		Utils.execSWTThread(()->{ swt_setDataSource( obj );});
	}
	
	protected void
	swt_setDataSource(
		Object		obj )
	{
		if (parent == null || !parent.isVisible()) {
			// not visible.. store for later
			datasource = obj;
			if (view != null) {
  			String title = MessageText.getString("rcm.subview.torrentdetails.name");
  			view.setTitle(title);
			}
			return;
		}
		dl 		= null;
		dl_file = null;
		search_strings = null;
		
		if ( obj instanceof Object[]){
			
			Object[] ds = (Object[])obj;
			
			if ( ds.length > 0 ){
				
				if ( ds[0] instanceof Download ){
	
					dl = (Download)ds[0];
					
				}else if ( ds[0] instanceof DiskManagerFileInfo ){
					
					dl_file = (DiskManagerFileInfo)ds[0];
				}else if (ds[0] instanceof Tag) {
					search_strings = new String[ds.length];
					for (int i = 0; i < ds.length; i++) {
						Tag tag = (Tag) ds[i];

						search_strings[i] = "tag:" + tag.getTagName(true);
					}
				}
			}
		}else{
			
			if ( obj instanceof Download ){
				
				dl = (Download)obj;
				
			}else if ( obj instanceof DiskManagerFileInfo ){
				
				dl_file = (DiskManagerFileInfo)obj;
			}
		}
		
		if ( dl_file != null ){
			
			try{
				dl = dl_file.getDownload();
				
			}catch( Throwable e ){	
			}
			
			if ( dl_file.getLength() < RelatedContentManager.FILE_ASSOC_MIN_SIZE ){
				
				dl_file = null;
			}
		}
		
		if ( dl_file == null ){
			
			related_mode = true;
			
			if ( dl != null ){
				
				if ( dl.getDiskManagerFileCount() == 1 ){
					
					dl_file = dl.getDiskManagerFileInfo( 0 );
					
					if ( dl_file.getLength() < RelatedContentManager.FILE_ASSOC_MIN_SIZE ){
						
						dl_file = null;
					}
				}
			}
		}else{
			
			related_mode = false;
		}
		
		if (initialized) {
			doSearch();
		}
	}
	
	private void
	doSearch()
	{	
		Utils.execSWTThread(
				new Runnable()
				{
					@Override
					public void
					run() 
					{
						boolean isSearch = search_strings != null;
						if ( buttons_toggle != null ){
							buttons_toggle[0].setVisible(!isSearch);
							buttons_toggle[1].setVisible(!isSearch);
						}
						if (status_label != null) {
							status_label.setText(isSearch ? Arrays.toString(search_strings) : "");
  						
							status_label.getParent().layout();
						}
						if ( buttons_toggle != null ){
							
							if ( related_mode ){
								
								buttons_toggle[0].setSelection( true );
								buttons_toggle[1].setSelection( false );
								
							}else{
								
								buttons_toggle[1].setSelection( true );
								buttons_toggle[0].setSelection( false );
							}
							
							buttons_toggle[0].setEnabled( dl != null );
							buttons_toggle[1].setEnabled( dl_file != null );
						}
					}
				});
			
		if ( current_rm == related_mode && current_dl == dl ){
			
			if ( current_file == null && dl_file == null  && search_strings == null){
				
				return;
				
			}else if ( current_file != null && dl_file != null ){
			
					// can't test on object equality for the files as the plugin interface
					// generates wrappers on demand...
				
				if ( current_file.getIndex() == dl_file.getIndex()){
					
					return;
				}
			}
		}
		
		current_rm 		= related_mode;
		current_dl		= dl;
		current_file	= dl_file;
		
		current_count	= 0;
		
		if ( current_data_source != null ){
			
			current_data_source.destroy();
		}
		
		final SWTSkinObject so = skin.getSkinObjectByID( "rcmsubskinview" );

		final RCMItemSubView	new_subview;
		
		if ( related_mode && dl != null ){
			
			Torrent torrent = dl.getTorrent();
			
			if ( torrent == null ){
				
				new_subview = new RelatedContentUISWT.RCMItemSubViewEmpty( plugin );
				
			}else{
				
				String[] networks = PluginCoreUtils.unwrap( dl ).getDownloadState().getNetworks();
				
				new_subview = new RCMItemSubView( plugin, torrent.getHash(), networks );
			}
							
		}else if ( !related_mode && dl_file != null && dl != null ){
				
			Torrent torrent = dl.getTorrent();
			
			if ( torrent == null ){
				
				new_subview = new RelatedContentUISWT.RCMItemSubViewEmpty( plugin );
				
			}else{
				
				String[] networks = PluginCoreUtils.unwrap( dl ).getDownloadState().getNetworks();
	
				long file_size = dl_file.getLength();
											
				new_subview = new RCMItemSubView( plugin, torrent.getHash(), networks, file_size );
			}
											
		}else if (search_strings != null){
			String[] networks = AENetworkClassifier.getDefaultNetworks();

			String name = Arrays.toString(search_strings);
			String net_str = RCMPlugin.getNetworkString( networks );
			
			try {
				byte[]	dummy_hash = (name + net_str ).getBytes( "UTF-8" );

				new_subview = new RCMItemSubView( plugin, dummy_hash, networks, search_strings );
			} catch (UnsupportedEncodingException e) {
				return;
			}
			
		} else {
			new_subview = new RelatedContentUISWT.RCMItemSubViewEmpty( plugin );
		}
		
		if ( !( new_subview instanceof RCMItemSubViewEmpty )){
			
			new_subview.setListener(new RCMItemSubViewListener() {
					private int vi_index;
					
					@Override
					public boolean
					searching()
					{
						if ( 	current_data_source != new_subview ||
								( status_img_label != null && status_img_label.isDisposed())){
							
							return( false );
						}

					if (spinnerVitality != null) {
						spinnerVitality.setVisible(true);
					}

					Utils.execSWTThread(() -> {
						if (TUX_CONTINUOUS_SEARCH) {
							current_data_source.keepLookingUp(button_more.getSelection());
						} else {
							if ( button_more != null && !button_more.isDisposed()){
								button_more.setEnabled(false);
							}
						}

						if (status_img_label != null && !status_img_label.isDisposed()
								&& vitality_images.length > 0) {
							status_img_label.setImage(
									vitality_images[vi_index++ % vitality_images.length]);
						}
					});
						
						return( true );
					}
					
					@Override
					public void
					complete()
					{
						if ( 	current_data_source != new_subview ||
								( status_img_label != null && status_img_label.isDisposed())){
															
							return;
						}

					if (spinnerVitality != null) {
						spinnerVitality.setVisible(false);
					}

					Utils.execSWTThread(() -> {
						if (status_img_label != null && !status_img_label.isDisposed()) {
							status_img_label.setImage(swarm_image);
						}
						if (!TUX_CONTINUOUS_SEARCH && button_more != null
								&& !button_more.isDisposed()) {
							button_more.setEnabled(true);
						}
					});
				}

					@Override
					public void updateCount(int num) {
						if (num == current_count) {
							return;
						}
						
						current_count = num;
						
						ViewTitleInfoManager.refreshTitleInfo( RCM_SubViewHolder.this );
					}
				});
			
			new_subview.setMdiEntry( null );	// trigger search start
		}
		
		if ( so != null ){
			
			Utils.execSWTThread(
					new Runnable()
					{
						@Override
						public void
						run() 
						{
								// not great but need this to pick up teh new datasource
								// properly...
							
							so.setVisible( false );
							
							so.triggerListeners( SWTSkinObjectListener.EVENT_DATASOURCE_CHANGED, new_subview );
							
							so.setVisible( true );
						}
					});
		}
		
		current_data_source = new_subview;
	}
	
	@Override
	public Object 
	getTitleInfoProperty(
		int propertyID)
	{
		if ( propertyID == ViewTitleInfo.TITLE_INDICATOR_TEXT ){
			
			if ( current_count > 0 ){
				
				return( String.valueOf( current_count ));
			}
		}
		
		return( null );
	}
	
	protected void
	destroy()
	{			
		SWTSkinObject so = skin.getSkinObjectByID( "rcmsubskinview" );
		
		if ( so != null ){
		
			skin.removeSkinObject( so );
		}
		
		if ( current_data_source != null ){
			
			current_data_source.destroy();
		}
		
		if (vitality_images != null) {
			vitality_images = null;
			ImageLoader.getInstance().releaseImage( RelatedContentUISWT.SPINNER_IMAGE_ID );
		}
	}

	static class 
	ImageLabel 
		extends Canvas implements PaintListener
	{
		private Image		image;
		
		public 
		ImageLabel(
			Composite 	parent,
			Image		_image )
		{
			super( parent, SWT.DOUBLE_BUFFERED );
		
			image	= _image;
			
			addPaintListener(this);
		}
		
		@Override
		public void
		paintControl(
			PaintEvent e) 
		{
			if ( !image.isDisposed()){
			
				e.gc.drawImage( image, 0, 0 );
			}
		}


		@Override
		public Point
		computeSize(
			int 	wHint, 
			int 	hHint, 
			boolean changed ) 
		{
			if ( image.isDisposed()){
				return( new Point(0,0));
			}
			
			Rectangle rect = image.getBounds();
			
			return( new Point( rect.width, rect.height ));
		}

		private void
		setImage(
			Image	_image )
		{
			image	= _image;
						
			redraw();
		}
	}

	public void focusGained() {
		if (datasource != null) {
			setDataSource(datasource);
			datasource = null;
		}
	}
}

