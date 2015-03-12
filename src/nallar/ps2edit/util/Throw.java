package nallar.ps2edit.util;

public class Throw {
	/**
	 * Throws the passed throwable, even if it is a checked exception. Does not return. Return type given so
	 * catch (CheckedException e) {
	 * throw Throw.sneaky(e);
	 * }
	 * can be used.
	 * <p>
	 * I hate checked exceptions. Sue me.
	 *
	 * @param throwable Throwable to throw
	 * @return Never returns.
	 */
	public static RuntimeException sneaky(Throwable throwable) {
		throw Throw.<RuntimeException>throwIgnoreCheckedErasure(throwable);
	}

	@SuppressWarnings("unchecked")
	private static <T extends Throwable> RuntimeException throwIgnoreCheckedErasure(Throwable toThrow) throws T {
		throw (T) toThrow;
	}
}
