package unknow.eclipse.launcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.ui.ILocalWorkingSetManager;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.IWorkingSetManager;
import org.eclipse.ui.PlatformUI;

public class Resources implements IPropertyChangeListener, ILaunchConfigurationListener
	{
	private static final Set<String> EXCLUDED_WS=new HashSet<>(Arrays.asList("Java Test Sources", "Java Main Sources"));

	private static final Pattern var=Pattern.compile("\\$\\{([^:]+):([^}]+)}");
	private static Resources self=new Resources();
	private IWorkingSetManager manager;
	private IWorkspaceRoot root;

	private ILocalWorkingSetManager localManager;
	private IWorkingSet otherProject;

	private Map<IProject,List<ILaunchConfiguration>> launch=new HashMap<>();

	private Resources()
		{
		localManager=PlatformUI.getWorkbench().createLocalWorkingSetManager();
		otherProject=localManager.createWorkingSet("Other Project", new IAdaptable[0]);

		manager=PlatformUI.getWorkbench().getWorkingSetManager();
		IWorkspace workspace=ResourcesPlugin.getWorkspace();
		root=workspace.getRoot();
		manager.addPropertyChangeListener(this);
		update();
		}

	public void update()
		{
		launch.clear();
		try
			{
			ILaunchManager launchManager=DebugPlugin.getDefault().getLaunchManager();
			launchManager.addLaunchConfigurationListener(this);
			for(ILaunchConfiguration c:launchManager.getLaunchConfigurations())
				{
				IProject p=project(c);
				List<ILaunchConfiguration> list=launch.get(p);
				if(list==null)
					launch.put(p, list=new ArrayList<>());
				list.add(c);
				}
			}
		catch (Exception e)
			{
			e.printStackTrace();
			}

		Set<IProject> projects=new HashSet<IProject>();
		for(IProject p:project())
			projects.add(p);

		for(IWorkingSet w:manager.getAllWorkingSets())
			projects.removeAll(projects(w));

		otherProject.setElements(projects.toArray(new IAdaptable[0]));
		}

	public static Resources getInstance()
		{
		return self;
		}

	public List<IWorkingSet> workingset()
		{
		IWorkingSet[] allWorkingSets=manager.getAllWorkingSets();
		List<IWorkingSet> list=new ArrayList<>(allWorkingSets.length+1);
		if(valid(otherProject))
			list.add(otherProject);
		for(IWorkingSet w:allWorkingSets)
			{
			if(valid(w))
				list.add(w);
			}
		return list;
		}

	private boolean valid(IWorkingSet w)
		{
		if(w.isEmpty())
			return false;
		for(IAdaptable e:w.getElements())
			{
			if(e.getAdapter(IProject.class)!=null)
				return true;
			}
		return false;
		}

	public IProject[] project()
		{
		return root.getProjects();
		}

	public List<IProject> projects(IWorkingSet w)
		{
		IAdaptable[] elements=w.getElements();
		List<IProject> list=new ArrayList<>(elements.length);
		for(int i=0; i<elements.length; i++)
			{
			IProject p=elements[i].getAdapter(IProject.class);
			if(valid(p))
				list.add(p);
			}
		return list;
		}

	private boolean valid(IProject p)
		{
		List<ILaunchConfiguration> list=launch.get(p);
		return list!=null&&!list.isEmpty();
		}

	public List<ILaunchConfiguration> launcher(IProject parent)
		{
		List<ILaunchConfiguration> list=launch.get(parent);
		return list==null?Collections.emptyList():list;
		}

	private IProject project(ILaunchConfiguration conf) throws Exception
		{
		String p=conf.getAttribute("org.eclipse.jdt.launching.PROJECT_ATTR", (String)null);
		if(p!=null)
			return root.getProject(p);
		if(p==null)
			{
			p=conf.getAttribute("org.eclipse.jdt.launching.WORKING_DIRECTORY", (String)null);
			if(p!=null)
				{
				Matcher m=var.matcher(p);
				if(m.matches())
					{
					switch (m.group(1))
						{
						case "project_loc":
							return root.getProject(m.group(2));
						case "workspace_loc":
							ProjectLocator loc=new ProjectLocator(m.group(2));
							root.accept(loc);
						}
					}
				}
			}
//		else
//			project=projects.get(p);
//		System.err.println(conf.getName());
//		for(Map.Entry<String,Object> e:conf.getAttributes().entrySet())
//			{
//			System.err.println("	"+e.getKey()+": "+e.getValue());
//			}
		return null /*project*/;
		}

	@Override
	public void propertyChange(PropertyChangeEvent e)
		{
		if(e.getNewValue()==otherProject||!(e.getNewValue() instanceof IWorkingSet))
			return;
		IWorkingSet s=(IWorkingSet)e.getNewValue();
		if(EXCLUDED_WS.contains(s.getName()))
			return;
		System.out.println("refresh property changed: "+s.getName());
		LauncherView.refresh();
		}

	@Override
	public void launchConfigurationAdded(ILaunchConfiguration l)
		{
		System.out.println("refresh launchAdded: "+l.getName());
		LauncherView.refresh();
		}

	@Override
	public void launchConfigurationChanged(ILaunchConfiguration l)
		{
		System.out.println("refresh launcheChanged: "+l.getName());
		LauncherView.refresh();
		}

	@Override
	public void launchConfigurationRemoved(ILaunchConfiguration l)
		{
		System.out.println("refresh launcheRemoved: "+l.getName());
		LauncherView.refresh();
		}

	private static class ProjectLocator implements IResourceVisitor
		{
		private String path;
		private IProject project;
		private int l=0;

		public ProjectLocator(String path)
			{
			this.path=path;
			}

		@Override
		public boolean visit(IResource r) throws CoreException
			{
			String p=r.getFullPath().toString();
			if(p.length()<path.length()&&path.startsWith(p)||p.startsWith(path))
				{
				if(l>p.length())
					{
					l=p.length();
					project=r.getProject();
					}
				return true;
				}
			return false;
			}
		}
	}