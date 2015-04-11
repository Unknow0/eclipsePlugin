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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.InvalidSyntaxException;

public class NameProvider extends AbstractContentProvider {
	private final TreeNode root = new TreeNode(0, "root");

	private Map<String, IProject> projectByPath = new HashMap<String, IProject>();
	private Map<String, IProject> projects = new HashMap<String, IProject>();
	private List<IWorkingSet> workingSets = new ArrayList<IWorkingSet>();
	private Map<IProject, IWorkingSet> workingSetByProject = new HashMap<IProject, IWorkingSet>();
	private Map<ILaunchConfigurationType, List<ILaunchConfiguration>> launchers = new HashMap<ILaunchConfigurationType, List<ILaunchConfiguration>>();
	private Map<IProject, List<ILaunchConfiguration>> launchersByProject = new HashMap<IProject, List<ILaunchConfiguration>>();

	private Type[] order = new Type[] { Type.WORKINGSET, Type.PROJECT };

	private IWorkingSetManager manager;
	private IWorkingSet otherProject;

	public NameProvider(LauncherView sampleView) throws CoreException, InvalidSyntaxException {
		super(sampleView);
		manager = PlatformUI.getWorkbench().getWorkingSetManager();
		otherProject = manager.createWorkingSet("Other Project", new IAdaptable[0]);
	}

	public Object[] getElements(Object parent) {
		if (parent.equals(this.view.getViewSite())) {
			return root.getChild();
		}
		return getChildren(parent);
	}

	public Object[] getChildren(Object parent) {
		if (hasChildren(parent))
			return ((TreeNode) parent).getChild();
		return new Object[0];
	}

	public boolean hasChildren(Object parent) {
		return !((TreeNode) parent).childs.isEmpty();
	}

	protected void resetData() throws CoreException {
		workingSets.clear();
		workingSetByProject.clear();
		projects.clear();
		projectByPath.clear();
		launchers.clear();
		launchersByProject.clear();

		workingSets.add(otherProject);
		IWorkingSet[] ws = manager.getAllWorkingSets();
		for (int i = 0; i < ws.length; i++) {
			workingSets.add(ws[i]);
			IAdaptable[] elements = ws[i].getElements();
			for (int j = 0; j < elements.length; j++) {
				IProject project = (IProject) elements[j].getAdapter(IProject.class);
				projects.put(project.getName(), project);
				projectByPath.put(project.getLocation().toString(), project);
				projectByPath.put("${workspace_loc:" + project.getFullPath() + "}", project);
				workingSetByProject.put(project, ws[i]);
			}
		}

		IProject[] p = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		List<IProject> pList = new ArrayList<IProject>();
		for (int i = 0; i < p.length; i++) {
			if (!workingSetByProject.containsKey(p[i])) {
				projects.put(p[i].getName(), p[i]);
				projectByPath.put("${workspace_loc:" + p[i].getFullPath() + "}", p[i]);
				workingSetByProject.put(p[i], otherProject);
				pList.add(p[i]);
			}
		}
		otherProject.setElements(pList.toArray(new IAdaptable[0]));

		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		launchManager.addLaunchConfigurationListener(this);

		ILaunchConfiguration[] l = launchManager.getLaunchConfigurations();
		for (int i = 0; i < l.length; i++) {
			String path = l[i].getAttribute("org.eclipse.jdt.launching.PROJECT_ATTR", (String) null);
			IProject project;
			if (path == null) {
				path = l[i].getAttribute("org.eclipse.jdt.launching.WORKING_DIRECTORY", (String) null);
				project = projectByPath.get(path);
			} else
				project = projects.get(path);

			List<ILaunchConfiguration> list = launchers.get(l[i].getType());
			List<ILaunchConfiguration> listByProject = launchersByProject.get(project);
			if (list == null) {
				list = new ArrayList<ILaunchConfiguration>();
				launchers.put(l[i].getType(), list);
			}
			if (listByProject == null) {
				listByProject = new ArrayList<ILaunchConfiguration>();
				launchersByProject.put(project, listByProject);
			}
			list.add(l[i]);
			listByProject.add(l[i]);
		}
		root.buildChild();
	}

	public static enum Type {
		WORKINGSET, PROJECT, BUILDTYPE, LAUNCHER
	}

	public class TreeNode {
		int depth;
		Type type;
		Object o;
		String name;
		List<TreeNode> childs = new ArrayList<TreeNode>();

		public TreeNode(int depth, String name) {
			this.depth = depth;
			this.name = name;
		}

		public TreeNode(int depth, String name, Object o, Type type) {
			this.depth = depth;
			this.name = name;
			this.o = o;
			this.type = type;
		}

		public TreeNode[] getChild() {
			return childs.toArray(new TreeNode[0]);
		}

		public void log() {
			for (int i = 0; i < depth; i++)
				System.err.print("| ");
			System.err.print("+-");
			System.err.print(name);
			System.err.print(" (");
			System.err.print(type);
			System.err.println(")");
		}

		public void buildChild() {
			if (depth > order.length)
				return;
			childs.clear();
			// log();
			TreeNode n;
			switch (depth == order.length ? Type.LAUNCHER : order[depth]) {
			case WORKINGSET:
				for (IWorkingSet w : workingSets) {
					n = new TreeNode(depth + 1, w.getName(), w, Type.WORKINGSET);
					n.buildChild();
					if (!n.childs.isEmpty())
						childs.add(n);
				}
				break;
			case PROJECT:
				IAdaptable[] elements;
				if (type == Type.WORKINGSET)
					elements = ((IWorkingSet) o).getElements();
				else
					elements = new IAdaptable[0];

				for (int i = 0; i < elements.length; i++) {
					IProject p = (IProject) elements[i].getAdapter(IProject.class);
					n = new TreeNode(depth + 1, p.getName(), p, Type.PROJECT);
					n.buildChild();
					if (!n.childs.isEmpty())
						childs.add(n);
				}
				break;
			case BUILDTYPE:
				break;
			case LAUNCHER:
				List<ILaunchConfiguration> list = null;
				if (type == Type.PROJECT)
					list = launchersByProject.get(o);
				else if (type == Type.BUILDTYPE)
					list = launchers.get(o);

				if (list != null) {
					for (ILaunchConfiguration l : list)
						childs.add(new TreeNode(depth + 1, l.getName(), l, Type.LAUNCHER));
				}
				break;
			}
		}

		public String toString() {
			return name;
		}
	}
}
