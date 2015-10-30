/* NFC Reader is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or
(at your option) any later version.

NFC Reader is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Wget.  If not, see <http://www.gnu.org/licenses/>.

Additional permission under GNU GPL version 3 section 7 */

package cache.wind.nfc;

import android.app.Application;
import android.util.DisplayMetrics;
import android.widget.Toast;

import java.io.InputStream;
import java.lang.Thread.UncaughtExceptionHandler;

public final class NfcReaderApplication extends Application implements
		UncaughtExceptionHandler {
	private static NfcReaderApplication instance;

	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		System.exit(0);
	}

	@Override
	public void onCreate() {
		super.onCreate();

		Thread.setDefaultUncaughtExceptionHandler(this);

		instance = this;
	}

	public static String name() {
		return getStringResource(R.string.app_name);
	}

	public static String version() {
		return BuildConfig.VERSION_NAME;
	}

	public static void showMessage(int fmt, Object... msgs) {
		String msg = String.format(getStringResource(fmt), msgs);
		Toast.makeText(instance, msg, Toast.LENGTH_LONG).show();
	}

	public static int getDimensionResourcePixelSize(int resId) {
		return instance.getResources().getDimensionPixelSize(resId);
	}

	public static int getColorResource(int resId) {
		return instance.getResources().getColor(resId);
	}

	public static String getStringResource(int resId) {
		return instance.getString(resId);
	}

	public static DisplayMetrics getDisplayMetrics() {
		return instance.getResources().getDisplayMetrics();
	}

	public static byte[] loadRawResource(int resId) {
		InputStream is = null;
		try {
			is = instance.getResources().openRawResource(resId);

			int len = is.available();
			byte[] raw = new byte[(int) len];

			int offset = 0;
			while (offset < raw.length) {
				int n = is.read(raw, offset, raw.length - offset);
				if (n < 0)
					break;

				offset += n;
			}
			return raw;
		} catch (Throwable e) {
			return null;
		} finally {
			try {
				is.close();
			} catch (Throwable ee) {
			}
		}
	}
}
