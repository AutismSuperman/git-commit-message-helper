package com.fulinlin.localization;

import com.intellij.AbstractBundle;
import com.intellij.DynamicBundle;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Supplier;

public class PluginBundle extends AbstractBundle {
    public static final PluginBundle INSTANCE = new PluginBundle();

    private PluginBundle() {
        super("i18n.info");
    }

    public static @NotNull String get(@NotNull @PropertyKey(resourceBundle = "i18n.info") String key,
                                      Object @NotNull ... params) {
        return INSTANCE.getMessage(key, params);
    }

    public static @NotNull Supplier<@Nls String> lazy(@NotNull @PropertyKey(resourceBundle = "i18n.info") String key,
                                                      Object @NotNull ... params) {
        return INSTANCE.getLazyMessage(key, params);
    }

    @Override
    protected ResourceBundle findBundle( @NotNull @NonNls String pathToBundle, @NotNull ClassLoader loader, ResourceBundle.@NotNull Control control) {
        var base =  ResourceBundle.getBundle(pathToBundle, Locale.ENGLISH, loader, control);
        var ideLocale = DynamicBundle.getLocale();
        if (!ideLocale.equals(Locale.ENGLISH)) {
            // load your bundle from baseName_<language>.properties, e.g. "baseName_zh.properties"
            //var localizedPath = pathToBundle + "_" + ideLocale.getLanguage();
            var localeBundle = ResourceBundle.getBundle(pathToBundle, ideLocale, loader, control);
            if (localeBundle != null && !base.equals(localeBundle)) {
                setParent(localeBundle, base);
                return localeBundle;
            }
        }
        return base;
    }

    /**
     * Borrows code from {@code com.intellij.DynamicBundle} to set the parent bundle using reflection.
     */
    private static void setParent(ResourceBundle localeBundle, ResourceBundle base) {
        try {
            Method method = ResourceBundle.class.getDeclaredMethod("setParent", ResourceBundle.class);
            method.setAccessible(true);
            MethodHandles.lookup().unreflect(method).bindTo(localeBundle).invoke(base);
        } catch (Throwable e) {
            // ignored, better handle this in production code
        }
    }

}