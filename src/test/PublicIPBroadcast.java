package test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;


public class PublicIPBroadcast {

	public String getIPv6Address() {
		String ipv = null;
		String MATCHER = "IPv6 Address";
		try {
			String line;
			Process p = Runtime.getRuntime().exec("ipconfig");
			BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
			while ((line = input.readLine()) != null) {
				if (line.contains(MATCHER)) {
					line = line.replaceAll(" ", "");
					line = line.substring(line.indexOf(":") + 1);
					// line = line.replace(REPLACESTRING, "");
					return line;
				}
			}
			input.close();
		} catch (Exception err) {
			err.printStackTrace();
		}
		return ipv;
	}
	
	public void postIPToServer(String ipadd) throws ClientProtocolException, IOException {
		HttpClient httpclient = HttpClients.createDefault();
        HttpPost httppost = new HttpPost("http://inventeontechnologies.com/highness/ipadd.php");

        // Request parameters and other properties.
        List<NameValuePair> params = new ArrayList<NameValuePair>(2);
        params.add(new BasicNameValuePair("ipadd", ipadd));
        httppost.setEntity(new UrlEncodedFormEntity(params, "UTF-8"));

        //Execute and get the response.
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();

        if (entity != null) {
            InputStream instream = entity.getContent();
            try {
                // do something useful
            } finally {
                instream.close();
            }
        }

	}

	
	public static void main(String[] args) {
		PublicIPBroadcast caller = new PublicIPBroadcast();
		String ip = caller.getIPv6Address();
		try {
			caller.postIPToServer(ip);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
