package dfva_java.client;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Security;

public class BaseClient {
	protected Settings settings;
	private HttpClient   httpClient;
	protected Crypto crypto;
	protected static final Logger logger =
	        Logger.getLogger("dfva_java");
	public BaseClient(Settings settings) {
		this.settings = settings;
		this.crypto = new Crypto(settings);
		Security.addProvider(new BouncyCastleProvider());
		this.httpClient = HttpClientBuilder.create().build();
	}
	
	private String shaString(){
		String dev="";
		if (this.settings.algorithm.toLowerCase().equals("sha512") ){
			dev = "SHA-512";
		}else if(this.settings.algorithm.toLowerCase().equals("sha384")){
			dev = "SHA-512";
		}else{
			dev = "SHA-256";
		}
		return dev;
	}
	
	private String getHashSum(String data) throws NoSuchAlgorithmException{
		MessageDigest md = MessageDigest.getInstance(this.shaString());
		md.update(data.getBytes());
		 byte byteData[] = md.digest();
		
		    //convert the byte to hex format method 1
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < byteData.length; i++) {
		 sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
		}
		return sb.toString();
	}
	
	protected String getHashSum(byte [] data) throws NoSuchAlgorithmException{
		MessageDigest md = MessageDigest.getInstance(this.shaString());
		md.update(data);
		 byte byteData[] = md.digest();
		
		    //convert the byte to hex format method 1
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < byteData.length; i++) {
		 sb.append(Integer.toString((byteData[i] & 0xff) + 0x100, 16).substring(1));
		}
		return sb.toString();
	}
	
	public String getTime(){
		TimeZone tz = TimeZone.getTimeZone("America/Costa_Rica");
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
		df.setTimeZone(tz);
		String nowAsISO = df.format(new Date());
		return nowAsISO;
	}
	
	public String readInputStream(InputStream inputStream) throws IOException{
		ByteArrayOutputStream result = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int length;
		while ((length = inputStream.read(buffer)) != -1) {
		    result.write(buffer, 0, length);
		}
		// StandardCharsets.UTF_8.name() > JDK 7
		return result.toString("UTF-8");
		
	}
	
//	private PublicKey getPublicKey() throws CertificateException, UnsupportedEncodingException{
//		ByteArrayInputStream fin = new ByteArrayInputStream(this.settings.publicCertificate.getBytes("UTF-8"));
//		CertificateFactory f = CertificateFactory.getInstance("X.509");
//		X509Certificate certificate = (X509Certificate)f.generateCertificate(fin);	
//		return certificate.getPublicKey();
//
//	}
	
	public JsonObject getDefaltParams(JsonObject data) throws NoSuchAlgorithmException{
		JsonObjectBuilder obj = Json.createObjectBuilder()
				.add("institution", this.settings.institution)
				.add("algorithm", this.settings.algorithm)
				.add("public_certificate", this.settings.publicCertificate);	
		
		String edata = this.crypto.encrypt(data.toString());
		obj.add("data_hash", this.getHashSum(edata));
		obj.add("data", edata);
		return obj.build();
	}
	
	protected JsonObject post(String url, String data){
		return post(url, data, true);
	}

	
	protected JsonObject post(String url, String data, Boolean dodecrypt){
	
		HttpResponse  response;
		JsonObject result= null;
		JsonReader jsonReader;
		
		try {
			HttpPost post = new HttpPost(url);
			StringEntity postingString = new StringEntity(data);
			post.setEntity(postingString);
			post.setHeader("Content-type", "application/json");
			response = this.httpClient.execute(post);
			if (response != null) {
				
				jsonReader = Json.createReader(
						response.getEntity().getContent());
				result= jsonReader.readObject();
				if(dodecrypt){
					jsonReader = Json.createReader(new StringReader(
							this.crypto.decrypt( result.getString("data") ) ));
					result = jsonReader.readObject();
				}
            }
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error contactando a DFVA", e);
		}
		
		return result;
	}
	
}
