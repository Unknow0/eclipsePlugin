package unknow.eclipse.launcher;

import java.util.Arrays;

import org.eclipse.jface.viewers.ITreeContentProvider;

import unknow.eclipse.launcher.LauncherTreeNode.RootNode;

public class LauncherTree implements ITreeContentProvider
	{
	private LauncherTreeNode<?> root=new RootNode();

	public LauncherTree()
		{
		}

	public void clear()
		{
		root.dispose();
		}

	@Override
	public Object[] getChildren(Object parent)
		{
		if(parent instanceof LauncherTreeNode)
			return ((LauncherTreeNode<?>)parent).child();
		return root.child();
		}

	@Override
	public Object getParent(Object child)
		{
		return ((LauncherTreeNode<?>)child).parent();
		}

	@Override
	public boolean hasChildren(Object parent)
		{
		return getChildren(parent).length>0;
		}

	@Override
	public Object[] getElements(Object parent)
		{
		return getChildren(parent);
		}
	}
