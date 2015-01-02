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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.ui.*;
import org.osgi.framework.*;

public class NameProvider extends AbstractContentProvider
	{
	private final TreeNode root=new TreeNode(0, "root");

	private Map<String,IProject> projectByPath=new HashMap<String,IProject>();
	private Map<String,IProject> projects=new HashMap<String,IProject>();
	private List<IWorkingSet> workingSets=new ArrayList<IWorkingSet>();
	private Map<IProject,IWorkingSet> workingSetByProject=new HashMap<IProject,IWorkingSet>();
	private Map<ILaunchConfigurationType,List<ILaunchConfiguration>> launchers=new HashMap<ILaunchConfigurationType,List<ILaunchConfiguration>>();
	private Map<IProject,List<ILaunchConfiguration>> launchersByProject=new HashMap<IProject,List<ILaunchConfiguration>>();

	private Type[] order=new Type[] {Type.WORKINGSET, Type.PROJECT};

	public NameProvider(LauncherView sampleView) throws CoreException, InvalidSyntaxException
		{
		super(sampleView);
		}

	public Object[] getElements(Object parent)
		{
		if(parent.equals(this.view.getViewSite()))
			{
			return root.getChild();
			}
		return getChildren(parent);
		}

	public Object[] getChildren(Object parent)
		{
		if(hasChildren(parent))
			return ((TreeNode)parent).getChild();
		return new Object[0];
		}

	public boolean hasChildren(Object parent)
		{
		return !((TreeNode)parent).childs.isEmpty();
		}

	protected void resetData() throws CoreException
		{
		workingSets.clear();
		workingSetByProject.clear();
		projects.clear();
		projectByPath.clear();
		launchers.clear();
		launchersByProject.clear();

		IWorkingSetManager manager=PlatformUI.getWorkbench().getWorkingSetManager();
		IWorkingSet[] ws=manager.getWorkingSets();
		for(int i=0; i<ws.length; i++)
			{
			workingSets.add(ws[i]);
			IAdaptable[] elements=ws[i].getElements();
			for(int j=0; j<elements.length; j++)
				{
				IProject project=(IProject)elements[j].getAdapter(IProject.class);
				projects.put(project.getName(), project);
				System.err.println(project.getName()+": loc "+project.getLocation()+" "+project.getFullPath());
				projectByPath.put(project.getLocation().toString(), project);
				projectByPath.put("${workspace_loc:"+project.getFullPath()+"}", project);
				workingSetByProject.put(project, ws[i]);
				}
			}

		ILaunchManager launchManager=DebugPlugin.getDefault().getLaunchManager();
		launchManager.addLaunchConfigurationListener(this);

		ILaunchConfiguration[] l=launchManager.getLaunchConfigurations();
		for(int i=0; i<l.length; i++)
			{
			System.err.println(l[i].getName());
			for(Map.Entry<String,Object> e:l[i].getAttributes().entrySet())
				{
				System.err.println("	"+e.getKey()+": "+e.getValue());
				}
			String p=l[i].getAttribute("org.eclipse.jdt.launching.PROJECT_ATTR", (String)null);
			IProject project;
			if(p==null)
				{
				p=l[i].getAttribute("org.eclipse.jdt.launching.WORKING_DIRECTORY", (String)null);
				project=projectByPath.get(p);
				}
			else
				project=projects.get(p);

			List<ILaunchConfiguration> list=launchers.get(l[i].getType());
			List<ILaunchConfiguration> listByProject=launchersByProject.get(project);
			if(list==null)
				{
				list=new ArrayList<ILaunchConfiguration>();
				launchers.put(l[i].getType(), list);
				}
			if(listByProject==null)
				{
				listByProject=new ArrayList<ILaunchConfiguration>();
				launchersByProject.put(project, listByProject);
				}
			list.add(l[i]);
			listByProject.add(l[i]);
			}
		root.buildChild();
		}

	public static enum Type
		{
		WORKINGSET, PROJECT, BUILDTYPE, LAUNCHER
		}

	public class TreeNode
		{
		int depth;
		Type type;
		Object o;
		String name;
		List<TreeNode> childs=new ArrayList<TreeNode>();

		public TreeNode(int depth, String name)
			{
			this.depth=depth;
			this.name=name;
			}

		public TreeNode(int depth, String name, Object o, Type type)
			{
			this.depth=depth;
			this.name=name;
			this.o=o;
			this.type=type;
			}

		public TreeNode[] getChild()
			{
			return childs.toArray(new TreeNode[0]);
			}

		public void buildChild()
			{
			childs.clear();
			switch (depth==order.length?Type.LAUNCHER:order[depth])
				{
				case WORKINGSET:
					for(IWorkingSet w:workingSets) {
						TreeNode n=new TreeNode(depth+1, w.getName(), w, Type.WORKINGSET);
						n.buildChild();
						if(!n.childs.isEmpty())
							childs.add(n);
					}
//					childs.add(new TreeNode(depth+1, "Other Project", null, Type.WORKINGSET));
					break;
				case PROJECT:
					IAdaptable[] elements;
					if(type==Type.WORKINGSET)
						if(o!=null)
							elements=((IWorkingSet)o).getElements();
						else
							elements=new IAdaptable[0];
					else
						elements=new IAdaptable[0];

					for(int i=0; i<elements.length; i++)
						{
						IProject p=(IProject)elements[i].getAdapter(IProject.class);
						TreeNode n=new TreeNode(depth+1, p.getName(), p, Type.PROJECT);
						n.buildChild();
						if(!n.childs.isEmpty())
							childs.add(n);
						}
					break;
				case BUILDTYPE:
					break;
				case LAUNCHER:
					List<ILaunchConfiguration> list=null;
					if(type==Type.PROJECT)
						list=launchersByProject.get(o);
					else if(type==Type.BUILDTYPE)
						list=launchers.get(o);

					if(list!=null)
						{
						for(ILaunchConfiguration l:list) {
							TreeNode n=new TreeNode(depth+1, l.getName(), l, Type.LAUNCHER);
							n.buildChild();
							if(!n.childs.isEmpty())
								childs.add(n);
						}
						}
					break;
				}
			}

		public String toString()
			{
			return name;
			}
		}
	}
