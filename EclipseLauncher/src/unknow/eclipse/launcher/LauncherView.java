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

import java.util.HashMap;
import java.util.Map;

import javax.management.*;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.actions.OpenRunConfigurations;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.InvalidSyntaxException;

import unknow.eclipse.launcher.TreeProvider.TreeNode;
import unknow.eclipse.launcher.TreeProvider.Type;

@SuppressWarnings("restriction")
public class LauncherView extends ViewPart
	{
	private TreeViewer treeViewer;
	private Action launch;
	private Action debug;
	private OpenRunConfigurations preference;

	private TreeProvider viewContentProvider;

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
			TreeNode n=(TreeNode)obj;

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
		viewContentProvider=new TreeProvider(this);
//		viewContentProvider.resetData();
		}

	public TreeViewer getTree()
		{
		return treeViewer;
		}

	public void createPartControl(Composite parent)
		{
		treeViewer=new TreeViewer(parent, SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL);
//		drillDownAdapter=new DrillDownAdapter(viewer);
		
		treeViewer.setContentProvider(viewContentProvider);
		treeViewer.setLabelProvider(new ViewLabelProvider());
		treeViewer.setSorter(new NameSorter());
		treeViewer.setInput(getViewSite());

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
		Menu menu=menuMgr.createContextMenu(treeViewer.getControl());
		treeViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, treeViewer);
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
		ISelection selection=treeViewer.getSelection();
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

	private void makeActions()
		{
		launch=new Action("Run")
			{
				public void run()
					{
					ISelection selection=treeViewer.getSelection();
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
					ISelection selection=treeViewer.getSelection();
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
		treeViewer.addDoubleClickListener(new IDoubleClickListener()
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
		treeViewer.getControl().setFocus();
		}
	}
