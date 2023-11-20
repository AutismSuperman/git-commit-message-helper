package icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.Icon;

public interface PluginIcons {

    Icon ICON = IconLoader.findIcon("/icons/icon.png");

    Icon EDIT = IconLoader.getIcon("/icons/edit.svg");

    Icon HISTORY = IconLoader.getIcon("/icons/history.svg");
}
