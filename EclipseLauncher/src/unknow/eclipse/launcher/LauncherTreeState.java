package unknow.eclipse.launcher;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.viewers.ITreeViewerListener;
import org.eclipse.jface.viewers.TreeExpansionEvent;
import org.eclipse.jface.viewers.TreeViewer;

public class LauncherTreeState
	{
	private static List<Character> order=Arrays.asList('W', 'P', 'L');
	private static ITreeViewerListener listener=new ITreeViewerListener()
		{
		@Override
		public void treeCollapsed(TreeExpansionEvent e)
			{
			remove((LauncherTreeNode<?>)e.getElement());
			}

		private void remove(LauncherTreeNode<?> node)
			{
			set.remove(node.getId());
			if(node.hasChild())
				{
				for(LauncherTreeNode<?> n:node.child())
					remove(n);
				}
			}

		@Override
		public void treeExpanded(TreeExpansionEvent e)
			{
			set.add(((LauncherTreeNode<?>)e.getElement()).getId());
			}
		};

	private static Set<String> set=new TreeSet<>(new Comparator<String>()
		{
		@Override
		public int compare(String o1, String o2)
			{
			int cmp=order.indexOf(o1.charAt(0))-order.indexOf(o2.charAt(0));
			if(cmp==0)
				cmp=o1.compareTo(o2);
			return cmp;
			}
		});

	public static void load(String state)
		{
		if(state==null||state.isEmpty())
			return;
		for(String s:state.split(","))
			set.add(s);
		}

	public static String getState()
		{
		return String.join(",", set);
		}

	public static void apply(TreeViewer treeViewer)
		{
		treeViewer.addTreeListener(listener);
		if(set.isEmpty())
			return;

		for(String n:set)
			{
			LauncherTreeNode<?> node=LauncherTreeNode.get(n);
			if(node!=null)
				treeViewer.setExpandedState(node, true);
			}

		set.clear();
		for(Object o:treeViewer.getExpandedElements())
			set.add(((LauncherTreeNode<?>)o).getId());
		}
	}
