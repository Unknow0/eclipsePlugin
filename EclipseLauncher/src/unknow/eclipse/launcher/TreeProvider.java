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
import org.eclipse.jface.util.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.ui.*;
import org.eclipse.ui.internal.*;
import org.osgi.framework.*;

public class TreeProvider implements IStructuredContentProvider, ITreeContentProvider, ILaunchConfigurationListener, IPropertyChangeListener {
	private final TreeNode root = new TreeNode(0);
	protected final LauncherView view;

	protected Map<String, IProject> projectByPath = new HashMap<String, IProject>();
	protected Map<String, IProject> projects = new HashMap<String, IProject>();
	protected List<IWorkingSet> workingSets = new ArrayList<IWorkingSet>();
	protected Map<IProject, IWorkingSet> workingSetByProject = new HashMap<IProject, IWorkingSet>();
	protected Map<ILaunchConfigurationType, List<ILaunchConfiguration>> launchers = new HashMap<ILaunchConfigurationType, List<ILaunchConfiguration>>();
	protected Map<IProject, List<ILaunchConfiguration>> launchersByProject = new HashMap<IProject, List<ILaunchConfiguration>>();

	private Map<Object, TreeNode> nodesByObject = new IdentityHashMap<Object, TreeNode>();

	private Type[] order = new Type[] { Type.WORKINGSET, Type.PROJECT };
	private IWorkingSetManager manager;
	private IWorkingSet otherProject;

	public TreeProvider(LauncherView sampleView) throws CoreException, InvalidSyntaxException {
		this.view = sampleView;
		manager = PlatformUI.getWorkbench().getWorkingSetManager();
		otherProject = manager.createWorkingSet("Other Project", new IAdaptable[0]);
		loadData();
		manager.addPropertyChangeListener(this);
	}

	public Object[] getElements(Object parent) {
		return getChildren(parent.equals(this.view.getViewSite()) ? root : parent);
	}

	private List<TreeNode> childCache = new ArrayList<TreeNode>();

	public Object[] getChildren(Object parent) {
		childCache.clear();
		TreeNode n = (TreeNode) parent;
		for (TreeNode c : n.childs) {
			if (c.type == Type.LAUNCHER || hasChildren(c))
				childCache.add(c);
		}
		return childCache.isEmpty() ? new Object[0] : childCache.toArray();
	}

	public boolean hasChildren(Object parent) {
		TreeNode n = (TreeNode) parent;
		for (TreeNode c : n.childs) {
			if (c.type == Type.LAUNCHER || hasChildren(c))
				return true;
		}
		return false;
	}

	protected void loadData() throws CoreException {
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
			IProject project = getProject(l[i]);

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
		System.out.flush();
	}

	public void inputChanged(Viewer v, Object oldInput, Object newInput) {
	}

	public void dispose() {
	}

	public Object getParent(Object child) {
		if (child instanceof TreeNode)
			return ((TreeNode) child).parent;
		return null;
	}

	public IProject getProject(ILaunchConfiguration conf) throws CoreException {
		System.err.println(conf.getName());
		for (Map.Entry<String, Object> e : conf.getAttributes().entrySet()) {
			System.err.println("	" + e.getKey() + ": " + e.getValue());
		}
		String p = conf.getAttribute("org.eclipse.jdt.launching.PROJECT_ATTR", (String) null);
		IProject project;
		if (p == null) {
			p = conf.getAttribute("org.eclipse.jdt.launching.WORKING_DIRECTORY", (String) null);
			project = projectByPath.get(p);
		} else
			project = projects.get(p);
		return project;
	}

	public void launchConfigurationAdded(ILaunchConfiguration conf) {
		try {
			IProject project = getProject(conf);
			TreeNode treeNode = nodesByObject.get(project);
			if (treeNode == null) {
				// TODO
			}
			TreeNode n = new TreeNode(treeNode, treeNode.depth + 1, conf, Type.LAUNCHER);
			treeNode.add(n);
		} catch (CoreException e) {
			throw new RuntimeException(e);
		}
	}

	public void launchConfigurationRemoved(ILaunchConfiguration conf) {
		TreeNode n = nodesByObject.get(conf);
		n.dispose();
	}

	public void launchConfigurationChanged(ILaunchConfiguration conf) {
	}

	public static enum Type {
		WORKINGSET, PROJECT, BUILDTYPE, LAUNCHER
	}

	public class TreeNode {
		int depth;
		Type type;
		Object o;
		TreeNode parent;
		List<TreeNode> childs = new ArrayList<TreeNode>();

		public TreeNode(int depth) {
			this.depth = depth;
		}

		public TreeNode(TreeNode parent, int depth, Object o, Type type) {
			this.parent = parent;
			this.depth = depth;
			this.o = o;
			this.type = type;
			nodesByObject.put(o, this);
			buildChild();
		}

		public TreeNode[] getChild() {
			return childs.toArray(new TreeNode[0]);
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
					n = new TreeNode(this, depth + 1, w, Type.WORKINGSET);
					n.buildChild();
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
					n = new TreeNode(this, depth + 1, p, Type.PROJECT);
					n.buildChild();
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
						childs.add(new TreeNode(this, depth + 1, l, Type.LAUNCHER));
				}
				break;
			}
		}

		public String toString() {
			switch (type) {
			case WORKINGSET:
				return ((IWorkingSet) o).getName();
			case PROJECT:
				return ((IProject) o).getName();
			case LAUNCHER:
				return ((ILaunchConfiguration) o).getName();
			default:
				return o.toString();
			}
		}

		public void dispose() {
			if (parent != null)
				parent.childs.remove(this);
			nodesByObject.remove(o);
			view.getTree().remove(this);
			for (TreeNode n : childs)
				n.dispose();
		}

		public void add(TreeNode c) {
			TreeNode n = this;
			while (!hasChildren(n) && n.parent != null)
				n = n.parent;

			childs.add(c);
			view.getTree().add(n == root ? view.getViewSite() : n.parent, n);
		}
	}

	@SuppressWarnings("restriction")
	public void propertyChange(PropertyChangeEvent e) {
		if (IWorkingSetManager.CHANGE_WORKING_SET_ADD.equals(e.getProperty())) {
			TreeNode n = new TreeNode(root, 1, e.getNewValue(), Type.WORKINGSET);
			n.buildChild();
			root.add(n);
		} else if (IWorkingSetManager.CHANGE_WORKING_SET_REMOVE.equals(e.getProperty())) {
			TreeNode n = nodesByObject.remove(e.getOldValue());
			n.dispose();
		} else if (IWorkingSetManager.CHANGE_WORKING_SET_CONTENT_CHANGE.equals(e.getProperty())) {
			TreeNode n = nodesByObject.get(e.getNewValue());
			if (n == null)
				n = new TreeNode(root, 1, e.getNewValue(), Type.WORKINGSET);
			n.buildChild();
			view.getTree().refresh(n.parent);
		} else if (IWorkingSetManager.CHANGE_WORKING_SET_NAME_CHANGE.equals(e.getProperty()) || WorkingSetManager.CHANGE_WORKING_SET_LABEL_CHANGE.equals(e.getProperty())) {
			TreeNode n = nodesByObject.get(e.getNewValue());
			if (n == null) {
				n = new TreeNode(root, 1, e.getNewValue(), Type.WORKINGSET);
				n.buildChild();
				root.add(n);
			} else
				view.getTree().refresh(n);
		}
	}
}
