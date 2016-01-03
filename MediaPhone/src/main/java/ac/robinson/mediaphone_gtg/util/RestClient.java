package ac.robinson.mediaphone_gtg.util;

import android.content.Context;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;

public class RestClient {

	private static final String BASE_URL = "http://www.girltalkgirl.org/resource%d/%s/";
	private static AsyncHttpClient CLIENT = new AsyncHttpClient();
	private static int DEFAULT_TIMEOUT = CLIENT.getConnectTimeout();

	public static void cancel(Context context) {
		CLIENT.cancelRequests(context, true);
	}

	public static void get(int resourceId, String resourceLanguage, AsyncHttpResponseHandler responseHandler) {
		get(-1, resourceId, resourceLanguage, responseHandler);
	}

	// pass a negative number to reset to default timeout; otherwise give a value in milliseconds
	public static void get(int timeout, int resourceId, String resourceLanguage, AsyncHttpResponseHandler
			responseHandler) {
		CLIENT.setConnectTimeout(timeout < 0 ? DEFAULT_TIMEOUT : timeout);
		CLIENT.setUserAgent("GTG-android");
		CLIENT.get(getUrl(resourceId, resourceLanguage), responseHandler);
	}

	public static void post(int timeout, int resourceId, String resourceLanguage, AsyncHttpResponseHandler 
			responseHandler) {
		CLIENT.setConnectTimeout(timeout < 0 ? DEFAULT_TIMEOUT : timeout);
		CLIENT.setUserAgent("GTG-android");
		CLIENT.post(getUrl(resourceId, resourceLanguage), responseHandler);
	}

	private static String getUrl(int resourceId, String resourceLanguage) {
		return String.format(BASE_URL, resourceId, resourceLanguage);
	}
}
