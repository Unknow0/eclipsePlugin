package unknow.eclipse.launcher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.ui.IWorkingSet;

public abstract class LauncherTreeNode<T>
	{
	private static Map<Object,LauncherTreeNode<?>> nodesByObj=new HashMap<>();
	private static Map<String,LauncherTreeNode<?>> nodesById=new HashMap<>();
	private LauncherTreeNode<?> parent;
	private LauncherTreeNode<?>[] child;
	private T obj;
	private String id;

	@SuppressWarnings("unchecked")
	public static <T> LauncherTreeNode<T> get(T t)
		{
		return (LauncherTreeNode<T>)nodesByObj.get(t);
		}

	@SuppressWarnings("unchecked")
	public static <T> LauncherTreeNode<T> get(String id)
		{
		return (LauncherTreeNode<T>)nodesById.get(id);
		}

	public LauncherTreeNode(LauncherTreeNode<?> parent, T obj)
		{
		this.parent=parent;
		this.obj=obj;
		this.id=getId(this);
		nodesByObj.put(obj, this);
		nodesById.put(id, this);
		}

	public String getId()
		{
		return id;
		}

	public LauncherTreeNode<?> parent()
		{
		return parent;
		}

	public boolean hasChild()
		{
		return child!=null&&child.length>0;
		}

	public LauncherTreeNode<?>[] child()
		{
		if(child==null)
			child=buildChild();
		return child;
		}

	public void dispose()
		{
		if(child!=null)
			{
			for(LauncherTreeNode<?> c:child)
				c.dispose();
			child=null;
			}
		nodesByObj.remove(obj);
		nodesById.remove(id);
		if(parent!=null)
			{
			List<LauncherTreeNode<?>> nc=new ArrayList<>(parent.child.length);
			for(LauncherTreeNode<?> n:parent.child)
				{
				if(n!=this)
					nc.add(n);
				}
			parent.child=nc.toArray(new LauncherTreeNode[0]);
			}
		}

	private static String getId(LauncherTreeNode<?> n)
		{
		if(n.obj instanceof IProject)
			return "P:"+n.toString();
		if(n.obj instanceof IWorkingSet)
			return "W:"+n.toString();
		if(n.obj instanceof ILaunchConfiguration)
			return "L:"+n.toString();
		return String.valueOf(n.obj);
		}

	public T obj()
		{
		return obj;
		}

	public abstract String toString();

	protected abstract LauncherTreeNode<?>[] buildChild();

	public static class RootNode extends LauncherTreeNode<Object>
		{

		public RootNode()
			{
			super(null, null);
			child();
			}

		@Override
		protected LauncherTreeNode<?>[] buildChild()
			{
			List<IWorkingSet> ws=Resources.getInstance().workingset();
			LauncherTreeNode<?>[] child=new LauncherTreeNode[ws.size()];
			for(int i=0; i<child.length; i++)
				child[i]=new WSNode(this, ws.get(i));
			return child;
			}

		@Override
		public String toString()
			{
			return "ROOT";
			}

		@Override
		public void dispose()
			{
			super.dispose();
			child();
			}
		}

	private static class WSNode extends LauncherTreeNode<IWorkingSet>
		{
		public WSNode(LauncherTreeNode<?> parent, IWorkingSet obj)
			{
			super(parent, obj);
			}

		@Override
		protected LauncherTreeNode<?>[] buildChild()
			{
			List<IProject> projects=Resources.getInstance().projects(obj());
			LauncherTreeNode<?>[] child=new LauncherTreeNode[projects.size()];
			for(int i=0; i<child.length; i++)
				child[i]=new ProjectNode(this, projects.get(i));
			return child;
			}

		@Override
		public String toString()
			{
			return obj().getLabel();
			}
		}

	private static class ProjectNode extends LauncherTreeNode<IProject>
		{
		public ProjectNode(LauncherTreeNode<?> parent, IProject project)
			{
			super(parent, project);
			}

		@Override
		public String toString()
			{
			return obj().getName();
			}

		@Override
		protected LauncherTreeNode<?>[] buildChild()
			{
			ILaunchConfiguration[] launcher=Resources.getInstance().launcher(obj());
			LauncherTreeNode<?>[] child=new LauncherTreeNode<?>[launcher.length];
			for(int i=0; i<child.length; i++)
				child[i]=new LaunchNode(this, launcher[i]);
			return child;
			}
		}

	private static class LaunchNode extends LauncherTreeNode<ILaunchConfiguration>
		{

		public LaunchNode(LauncherTreeNode<?> parent, ILaunchConfiguration obj)
			{
			super(parent, obj);
			}

		@Override
		public String toString()
			{
			return obj().getName();
			}

		@Override
		protected LauncherTreeNode<?>[] buildChild()
			{
			return new LauncherTreeNode[0];
			}
		}
	}