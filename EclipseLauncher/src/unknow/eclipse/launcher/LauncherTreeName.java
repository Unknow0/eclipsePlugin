package unknow.eclipse.launcher;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IDecoratorManager;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class LauncherTreeName extends LabelProvider
	{
	private final Map<String,Image> cache=new HashMap<>();

	private IDecoratorManager decoratorManager=PlatformUI.getWorkbench().getDecoratorManager();
	private ILabelProvider provider=WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider();

	public String getText(Object obj)
		{
		if(obj instanceof LauncherTreeNode)
			return ((LauncherTreeNode<?>)obj).toString();
		if(obj instanceof IProject)
			((IProject)obj).getName();
		return obj.toString();
		}

	public Image getImage(Object obj)
		{
		if(obj instanceof LauncherTreeNode)
			obj=((LauncherTreeNode<?>)obj).obj();
		Image img=provider.getImage(obj);
		if(img!=null)
			return decoratorManager.decorateImage(img, obj);

		try
			{
			IConfigurationElement[] imgCfg=Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.debug.ui.launchConfigurationTypeImages");
			String type=null;
			if(obj instanceof ILaunchConfigurationType)
				type=((ILaunchConfigurationType)obj).getIdentifier();
			if(obj instanceof ILaunchConfiguration)
				type=((ILaunchConfiguration)obj).getType().getIdentifier();
			Image image=cache.get(type);
			if(image!=null)
				return image;

			for(int i=0; i<imgCfg.length; i++)
				{
				String configTypeID=imgCfg[i].getAttribute("configTypeID");
				String icon=imgCfg[i].getAttribute("icon");
				if(configTypeID.equals(type))
					{
					ImageDescriptor imgDesc=LauncherViewActivator.imageDescriptorFromPlugin(imgCfg[i].getContributor().getName(), icon);
					image=imgDesc.createImage();
					cache.put(type, image);
					return image;
					}
				}
			}
		catch (CoreException e)
			{
			e.printStackTrace();
			}
		return null;
		}
	}
