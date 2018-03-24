package net.ps2stats;

import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

public class BufferUtil {
	public static void closeMappedBuffer(MappedByteBuffer f) {
		// wow this is stupid.
		// works on java 7 through 9
		try {
			((DirectBuffer) f).cleaner().clean();
		} catch (NoSuchMethodError ignored) {
			Method cleanerMethod;
			try {
				cleanerMethod = f.getClass().getMethod("cleaner");
			} catch (Error | Exception ignored2) {
				try {
					Field attField = f.getClass().getDeclaredField("att");
					attField.setAccessible(true);
					f = (MappedByteBuffer) attField.get(f);
					cleanerMethod = f.getClass().getMethod("cleaner");
				} catch (Error | Exception e) {
					throw new Error(e);
				}
			}

			try {
				cleanerMethod.setAccessible(true);
				Object cleaner = cleanerMethod.invoke(f);
				if (cleaner instanceof Runnable) {
					((Runnable) cleaner).run();
				} else {
					Method cleanMethod = cleaner.getClass().getMethod("clean");
					cleanMethod.setAccessible(true);
					cleanMethod.invoke(cleaner);
				}
			} catch (Exception ignored2) {
				try {
					Class unsafeClass = Class.forName("sun.misc.Unsafe");
					// we do not need to check for a specific class, we can call the Unsafe method with any buffer class
					MethodHandle unmapper = MethodHandles.lookup().findVirtual(unsafeClass, "invokeCleaner",
                        MethodType.methodType(Void.TYPE, ByteBuffer.class));
					// fetch the unsafe instance and bind it to the virtual MethodHandle
					Field fUnsafe = null;
					fUnsafe = unsafeClass.getDeclaredField("theUnsafe");
					fUnsafe.setAccessible(true);
					Unsafe theUnsafe = (Unsafe) fUnsafe.get(null);
					unmapper.bindTo(theUnsafe).invokeExact((ByteBuffer) f);
				}
				catch (Throwable t) {
					throw new Error(t);
				}
			}
		}
	}
}
