/*******************************************************************************
 * Copyright (c) 2014 Unknow.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-3.0.html
 * 
 * Contributors:
 *     Unknow - initial API and implementation
 ******************************************************************************/
package unknow.eclipse.launcher;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.jface.viewers.*;
import org.osgi.framework.*;

abstract class AbstractContentProvider implements IStructuredContentProvider, ITreeContentProvider, ILaunchConfigurationListener
	{
	protected final LauncherView view;

	public AbstractContentProvider(LauncherView sampleView) throws CoreException, InvalidSyntaxException
		{
		this.view=sampleView;
		}

	public void inputChanged(Viewer v, Object oldInput, Object newInput)
		{
		}

	public void dispose()
		{
		}

	public abstract Object[] getElements(Object parent);

	public Object getParent(Object child)
		{
		return null;
		}

	public abstract Object[] getChildren(Object parent);

	public abstract boolean hasChildren(Object parent);

	protected abstract void resetData() throws CoreException;

	public void launchConfigurationAdded(ILaunchConfiguration conf)
		{
		try
			{
			resetData();
			}
		catch (CoreException e)
			{
			throw new RuntimeException(e);
			}
		view.refresh();
		}

	public void launchConfigurationRemoved(ILaunchConfiguration conf)
		{
		try
			{
			resetData();
			}
		catch (CoreException e)
			{
			throw new RuntimeException(e);
			}
		view.refresh();
		}

	public void launchConfigurationChanged(ILaunchConfiguration configuration)
		{
		try
			{
			resetData();
			}
		catch (CoreException e)
			{
			throw new RuntimeException(e);
			}
		view.refresh();
		}
	}
