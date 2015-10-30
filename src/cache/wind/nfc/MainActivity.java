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

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.Theme;

import cache.wind.nfc.nfc.NfcManager;
import cache.wind.nfc.ui.AboutPage;
import cache.wind.nfc.ui.MainPage;
import cache.wind.nfc.ui.NfcPage;

public class MainActivity extends AppCompatActivity {

	private NfcManager mNfcManager;
	private TextView mTextArea;
	private boolean mSafeExit;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		setSupportActionBar((android.support.v7.widget.Toolbar) findViewById(R.id.supportToolbar));

		mNfcManager = new NfcManager(this);
		mTextArea = (TextView)findViewById(R.id.textArea);
		mTextArea.setMovementMethod(LinkMovementMethod.getInstance());

		onNewIntent(getIntent());
	}

	@Override
	public void onBackPressed() {
		if (mSafeExit) {
			super.onBackPressed();
		}
	}

	@Override
	public void setIntent(Intent intent) {
		if (NfcPage.isSendByMe(intent)) {
			loadNfcPage(intent, mTextArea);
			invalidateOptionsMenu();
		} else if (AboutPage.isSendByMe(intent)) {
			showAboutDialog();
		} else {
			super.setIntent(intent);
		}
	}

	@Override
	protected void onPause() {
		mNfcManager.onPause();
		super.onPause();
	}

	@Override
	protected void onResume() {
		super.onResume();
		mNfcManager.onResume();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		if (hasFocus) {
			if (mNfcManager.updateStatus()) {
				loadDefaultPage(mTextArea);
				invalidateOptionsMenu();
			}

			// 有些ROM将关闭系统状态下拉面板的BACK事件发给最顶层窗口
			// 这里加入一个延迟避免意外退出
			new Handler().postDelayed(new Runnable() {
				public void run() {
					mSafeExit = true;
				}
			}, 800);
		} else {
			mSafeExit = false;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		loadDefaultPage(mTextArea);
		invalidateOptionsMenu();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if (!mNfcManager.readCard(intent, new NfcPage(this))) {
			loadDefaultPage(mTextArea);
			invalidateOptionsMenu();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		if (isCurrentPage(SPEC.PAGE.INFO, mTextArea)) {
			menu.findItem(R.id.action_copy).setVisible(true);
			menu.findItem(R.id.action_share).setVisible(true);
			menu.findItem(R.id.action_clear).setVisible(true);
		} else {
			menu.findItem(R.id.action_copy).setVisible(false);
			menu.findItem(R.id.action_share).setVisible(false);
			menu.findItem(R.id.action_clear).setVisible(false);
		}
		return super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
			case R.id.action_copy:
				copyTextAreaContent(mTextArea);
				break;
			case R.id.action_share:
				shareTextAreaContent(mTextArea);
				break;
			case R.id.action_clear:
				loadDefaultPage(mTextArea);
				break;
			case R.id.action_about:
				showAboutDialog();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void copyTextAreaContent(TextView textArea) {
		final CharSequence text = textArea.getText();
		if (!TextUtils.isEmpty(text)) {
			int sdkInt = android.os.Build.VERSION.SDK_INT;
			if (sdkInt >= android.os.Build.VERSION_CODES.HONEYCOMB){
				android.content.ClipboardManager clipboard =  (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
				ClipData clip = ClipData.newPlainText(getString(R.string.app_name), text.toString());
				clipboard.setPrimaryClip(clip);
			} else{
				//noinspection deprecation
				android.text.ClipboardManager clipboard = (android.text.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
				clipboard.setText(text.toString());
			}

			NfcReaderApplication.showMessage(R.string.info_main_copied);
		}
	}

	private void shareTextAreaContent(TextView textArea) {
		final CharSequence text = textArea.getText();
		if (!TextUtils.isEmpty(text)) {
			Intent intent = new Intent();
			intent.setAction(Intent.ACTION_SEND);
			intent.putExtra(Intent.EXTRA_SUBJECT, NfcReaderApplication.name());
			intent.putExtra(Intent.EXTRA_TEXT, text.toString());
			intent.setType("text/plain");
			textArea.getContext().startActivity(intent);
		}
	}

	private void showAboutDialog() {
		new MaterialDialog.Builder(this)
				.theme(Theme.LIGHT)
				.content(AboutPage.getContent())
				.title(NfcReaderApplication.name() + " " + NfcReaderApplication.version())
				.positiveText(android.R.string.ok).show();
	}

	private void loadDefaultPage(TextView textArea) {
		resetTextArea(textArea, SPEC.PAGE.DEFAULT, Gravity.CENTER);
		textArea.setText(MainPage.getContent(this));

		int padding = getResources().getDimensionPixelSize(R.dimen.padding_default);
		textArea.setPadding(padding, padding, padding, padding);
	}

	@SuppressLint("RtlHardcoded")
	private void loadNfcPage(Intent intent, TextView textArea) {
		final CharSequence info = NfcPage.getContent(this, intent);

		if (NfcPage.isNormalInfo(intent)) {
			resetTextArea(textArea, SPEC.PAGE.INFO, Gravity.LEFT);

			int padding = getResources().getDimensionPixelSize(R.dimen.padding_window);
			textArea.setPadding(padding, padding, padding, padding);
		} else {
			resetTextArea(textArea, SPEC.PAGE.ABOUT, Gravity.CENTER);

			int padding = getResources().getDimensionPixelSize(R.dimen.padding_default);
			textArea.setPadding(padding, padding, padding, padding);
		}

		textArea.setText(info);
	}

	private boolean isCurrentPage(SPEC.PAGE which, TextView textArea) {
		Object obj = textArea.getTag();

		if (obj == null)
			return which.equals(SPEC.PAGE.DEFAULT);

		return which.equals(obj);
	}

	private void resetTextArea(TextView textArea, SPEC.PAGE type, int gravity) {
		((View) textArea.getParent()).scrollTo(0, 0);

		textArea.setTag(type);
		textArea.setGravity(gravity);
	}
}
