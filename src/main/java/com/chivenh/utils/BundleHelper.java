package com.chivenh.utils;

import com.intellij.DynamicBundle;
import org.jetbrains.annotations.NotNull;

/**
 * BundleHelper
 *
 * @author Chivenh
 * @created 2023年08月24日 08:57
 */
@SuppressWarnings({ "unused" })

public class BundleHelper extends DynamicBundle {

	private static final BundleHelper INSTANCE = new BundleHelper("messages.git-msg");

	protected BundleHelper(@NotNull String pathToBundle) {
		super(pathToBundle);
	}

	public static String message(String key,Object ... params){
		return INSTANCE.getMessage(key,params);
	}
}
