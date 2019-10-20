/*
 * Copyright (C) Bigly Software.  All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package com.aelitis.plugins.rcmplugin;

import org.eclipse.swt.widgets.Composite;

import com.biglybt.ui.common.viewtitleinfo.ViewTitleInfo;
import com.biglybt.ui.swt.pif.UISWTView;
import com.biglybt.ui.swt.pif.UISWTViewEvent;
import com.biglybt.ui.swt.pif.UISWTViewEventListener;
import com.biglybt.ui.swt.skin.SWTSkin;
import com.biglybt.ui.swt.skin.SWTSkinFactory;

public class RCM_SubViewEventListener implements UISWTViewEventListener, ViewTitleInfo {

	private RCM_SubViewHolder rcm_subViewHolder;

	@Override
	public boolean
	eventOccurred(
		UISWTViewEvent event ) 
	{
		UISWTView currentView = event.getView();
		
		switch (event.getType()) {
			case UISWTViewEvent.TYPE_CREATE:{
				
				SWTSkin skin = SWTSkinFactory.getNonPersistentInstance(
						getClass().getClassLoader(),
						"com/aelitis/plugins/rcmplugin/skins",
						"skin3_rcm.properties" );
				
				RCMPlugin plugin = (RCMPlugin) event.getView().getPluginInterface().getPlugin();

				rcm_subViewHolder = new RCM_SubViewHolder(plugin, currentView, skin);
				RelatedContentUISWT.rcm_subviews.put(currentView, rcm_subViewHolder);

				event.getView().setDestroyOnDeactivate(false);

				break;
			}
			case UISWTViewEvent.TYPE_INITIALIZE:{
			
				RCM_SubViewHolder subview = RelatedContentUISWT.rcm_subviews.get(currentView);
				
				if ( subview != null ){
					
					subview.initialise((Composite)event.getData(), RelatedContentUISWT.swarm_image);
				}

				break;
			}
			case UISWTViewEvent.TYPE_DATASOURCE_CHANGED:{
				
				RCM_SubViewHolder subview = RelatedContentUISWT.rcm_subviews.get(currentView);
				
				if ( subview != null ){
					
					subview.setDataSource( event.getData());
				}
				
				break;
			}

			case UISWTViewEvent.TYPE_SHOWN: {
				RCM_SubViewHolder subview = RelatedContentUISWT.rcm_subviews.get(currentView);
				
				if ( subview != null ){
					
					subview.focusGained();
				}
				
				break;
			}
			
			case UISWTViewEvent.TYPE_DESTROY:{
				
				RCM_SubViewHolder subview = RelatedContentUISWT.rcm_subviews.remove(currentView);
			
				if ( subview != null ){
					
					subview.destroy();
				}
				
				break;
			}
		}
		return true;
	}

	@Override
	public Object getTitleInfoProperty(int propertyID) {
		if (rcm_subViewHolder != null) {
			return rcm_subViewHolder.getTitleInfoProperty(propertyID);
		}
		return null;
	}
}
