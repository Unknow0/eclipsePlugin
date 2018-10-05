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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.ui.actions.OpenRunConfigurations;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.osgi.framework.InvalidSyntaxException;

@SuppressWarnings("restriction")
public class LauncherView extends ViewPart
	{
	private static LauncherView self;
	private TreeViewer treeViewer;
	private Action launch;
	private Action debug;
	private OpenRunConfigurations preference;

	private LauncherTree viewContentProvider;

	public LauncherView() throws CoreException, InvalidSyntaxException, ClassNotFoundException, InstantiationException, IllegalAccessException
		{
		viewContentProvider=new LauncherTree();
		self=this;
		}

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException
		{
		super.init(site, memento);
		LauncherTreeState.load(memento.getString("tree"));
		}

	@Override
	public void saveState(IMemento memento)
		{
		super.saveState(memento);
		memento.putString("tree", LauncherTreeState.getState());
		}

	@Override
	public void createPartControl(Composite parent)
		{
		treeViewer=new TreeViewer(parent, SWT.MULTI|SWT.H_SCROLL|SWT.V_SCROLL);

		treeViewer.setContentProvider(viewContentProvider);
		treeViewer.setLabelProvider(new LauncherTreeName());
		treeViewer.setInput(getViewSite());

		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();

		LauncherTreeState.apply(treeViewer);
		}

	private void hookContextMenu()
		{
		MenuManager menuMgr=new MenuManager("#truc", "unknow.eclipse.launcher.menu");
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
		LauncherTreeNode<?> n=(LauncherTreeNode<?>)((IStructuredSelection)selection).getFirstElement();
		if(n!=null&&n.obj() instanceof ILaunchConfiguration)
			{
			manager.add(launch);
			manager.add(debug);
			}
		manager.add(preference);
		manager.add(new Action("refresh")
			{
			@Override
			public void run()
				{
				treeViewer.refresh();
				}
			});
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
				LauncherTreeNode<?> n=(LauncherTreeNode<?>)((IStructuredSelection)selection).getFirstElement();
				if(n.obj() instanceof ILaunchConfiguration)
					{
					try
						{
						((ILaunchConfiguration)n.obj()).launch(ILaunchManager.RUN_MODE, null, true, true);
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
				LauncherTreeNode<?> n=(LauncherTreeNode<?>)((IStructuredSelection)selection).getFirstElement();
				if(n.obj() instanceof ILaunchConfiguration)
					{
					try
						{
						((ILaunchConfiguration)n.obj()).launch(ILaunchManager.DEBUG_MODE, null, true, true);
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
				ISelection selection=treeViewer.getSelection();
				LauncherTreeNode<?> n=(LauncherTreeNode<?>)((IStructuredSelection)selection).getFirstElement();
				if(n.obj() instanceof ILaunchConfiguration)
					{
					launch.run();
					}
				else
					{
					treeViewer.setExpandedState(n, !treeViewer.getExpandedState(n));
					}
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

	public static void refresh()
		{
		Display.getDefault().syncExec(new Runnable()
			{
			public void run()
				{
				Resources.getInstance().update();
				self.viewContentProvider.clear();
				self.treeViewer.getTree().clearAll(true);
				self.treeViewer.refresh();
				LauncherTreeState.apply(self.treeViewer);
				}
			});
		}
	}
