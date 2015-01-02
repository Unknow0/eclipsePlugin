/*******************************************************************************
 * Copyright (c) 2014 Unknow.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0.html
 * 
 * Contributors:
 * Unknow - initial API and implementation
 ******************************************************************************/
package unknow.eclipse.launcher;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.debug.internal.ui.actions.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.resource.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.model.*;
import org.eclipse.ui.part.*;
import org.osgi.framework.*;

import unknow.eclipse.launcher.NameProvider.TreeNode;
import unknow.eclipse.launcher.NameProvider.Type;

public class LauncherView extends ViewPart
	{
	private TreeViewer viewer;
	private Action launch;
	private Action debug;
	@SuppressWarnings("restriction")
	private OpenRunConfigurations preference;

	private AbstractContentProvider viewContentProvider;

	private final Map<ImageDescriptor,Image> cache=new HashMap<ImageDescriptor,Image>();

	private class ViewLabelProvider extends LabelProvider
		{
		IDecoratorManager decoratorManager=getSite().getWorkbenchWindow().getWorkbench().getDecoratorManager();
		ILabelProvider provider=WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider();

		public String getText(Object obj)
			{
			return obj.toString();
			}

		public Image getImage(Object obj)
			{
			NameProvider.TreeNode n=(NameProvider.TreeNode)obj;

			System.out.println(n.o);
			Image img=provider.getImage(n.o);
			if(img!=null)
				return decoratorManager.decorateImage(img, n.o);

			try
				{
				IConfigurationElement[] imgCfg=Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.debug.ui.launchConfigurationTypeImages");
				String type=n.type==Type.BUILDTYPE?((ILaunchConfigurationType)n.o).getIdentifier():((ILaunchConfiguration)n.o).getType().getIdentifier();
				for(int i=0; i<imgCfg.length; i++)
					{
					String configTypeID=imgCfg[i].getAttribute("configTypeID");
					String icon=imgCfg[i].getAttribute("icon");
					if(configTypeID.equals(type))
						{
						ImageDescriptor imgDesc=LauncherViewActivator.imageDescriptorFromPlugin(imgCfg[i].getContributor().getName(), icon);
						Image image=cache.get(imgDesc);
						if(image==null)
							{
							image=imgDesc.createImage();
							cache.put(imgDesc, image);
							}
						return image;
						}
					}
				}
			catch (CoreException e)
				{
				e.printStackTrace();
				}
			return getDefaultImage();
			}
		}

	class NameSorter extends ViewerSorter
		{
		}

	public LauncherView() throws CoreException, InvalidSyntaxException, ClassNotFoundException, InstantiationException, IllegalAccessException
		{
		viewContentProvider=new NameProvider(this);

		viewContentProvider.resetData();
		}

	public void refresh()
		{
		viewer.refresh();
		}

	public void createPartControl(Composite parent)
		{
		viewer=new TreeViewer(parent, SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL);
//		drillDownAdapter=new DrillDownAdapter(viewer);
		viewer.setContentProvider(viewContentProvider);
		viewer.setLabelProvider(new ViewLabelProvider());
		viewer.setSorter(new NameSorter());
		viewer.setInput(getViewSite());

		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();
		}

	private void hookContextMenu()
		{
		MenuManager menuMgr=new MenuManager("#truc");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener()
			{
				public void menuAboutToShow(IMenuManager manager)
					{
					LauncherView.this.fillContextMenu(manager);
					}
			});
		Menu menu=menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
		}

	private void contributeToActionBars()
		{
		IActionBars bars=getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
		}

	private void fillLocalPullDown(IMenuManager manager)
		{
		// v sur la barre => changer le mode d'affichage
		}

	private void fillContextMenu(IMenuManager manager)
		{
		ISelection selection=viewer.getSelection();
		TreeNode n=(TreeNode)((IStructuredSelection)selection).getFirstElement();
		if(n!=null&&n.o instanceof ILaunchConfiguration)
			{
			manager.add(launch);
			manager.add(debug);
			}
		manager.add(preference);
		}

	private void fillLocalToolBar(IToolBarManager manager)
		{
//		manager.add(action1);
		}

	@SuppressWarnings("restriction")
	private void makeActions()
		{
		launch=new Action("Run")
			{
				public void run()
					{
					ISelection selection=viewer.getSelection();
					TreeNode n=(TreeNode)((IStructuredSelection)selection).getFirstElement();
					if(n.o instanceof ILaunchConfiguration)
						{
						try
							{
							((ILaunchConfiguration)n.o).launch(ILaunchManager.RUN_MODE, null, true, true);
							}
						catch (CoreException e)
							{
							throw new RuntimeException(e);
							}
						}
					}
			};
		debug=new Action("Debug")
			{
				public void run()
					{
					ISelection selection=viewer.getSelection();
					TreeNode n=(TreeNode)((IStructuredSelection)selection).getFirstElement();
					if(n.o instanceof ILaunchConfiguration)
						{
						try
							{
							((ILaunchConfiguration)n.o).launch(ILaunchManager.DEBUG_MODE, null, true, true);
							}
						catch (CoreException e)
							{
							throw new RuntimeException(e);
							}
						}
					}
			};
		preference=new OpenRunConfigurations();
		}

	private void hookDoubleClickAction()
		{
		viewer.addDoubleClickListener(new IDoubleClickListener()
			{
				public void doubleClick(DoubleClickEvent event)
					{
					launch.run();
					}
			});
		}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus()
		{
		viewer.getControl().setFocus();
		}
	}
