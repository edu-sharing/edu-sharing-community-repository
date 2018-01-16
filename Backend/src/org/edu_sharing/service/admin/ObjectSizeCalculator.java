package org.edu_sharing.service.admin;

import java.io.IOException;

import com.javamex.classmexer.MemoryUtil;
import com.javamex.classmexer.MemoryUtil.VisibilityFilter;

public class ObjectSizeCalculator {

	public static long calculate(Object o) {
		return MemoryUtil.deepMemoryUsageOf(o);
	}
}
