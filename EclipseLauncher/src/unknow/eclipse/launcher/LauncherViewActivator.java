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

import org.eclipse.core.runtime.*;
import org.eclipse.ui.plugin.*;
import org.osgi.framework.*;

public class LauncherViewActivator extends AbstractUIPlugin
	{
	public static final String ID=LauncherViewActivator.class.getCanonicalName();
	public static LauncherViewActivator self;

	public void start(BundleContext ctx) throws Exception
		{
		super.start(ctx);
		self=this;
		}

	public static void info(String msg)
		{
		info(msg, null);
		}

	public static void info(String msg, Exception e)
		{
		self.getLog().log(new Status(Status.INFO, ID, msg, e));
		}
	}
