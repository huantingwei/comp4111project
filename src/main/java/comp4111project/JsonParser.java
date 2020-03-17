package comp4111project;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


public class JsonParser {
	
	private int BUFFER_SIZE = 16384;
	
	public HashMap readToMap(InputStream instream) throws JsonParseException, JsonMappingException, IOException {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    	int nRead;
    	byte[] data = new byte[BUFFER_SIZE];
		while ((nRead = instream.read(data, 0, data.length)) != -1) {
			buffer.write(data, 0, nRead);
		}
    	ObjectMapper objectMapper = new ObjectMapper();
    	return objectMapper.readValue(data, HashMap.class);
	}
	
	public String readToString(InputStream instream) throws IOException {
    	InputStreamReader insReader = new InputStreamReader(instream);
    	BufferedReader reader = new BufferedReader(insReader);
        StringBuffer sb = new StringBuffer();
        String str;
        while((str = reader.readLine())!= null){
           sb.append(str);
        }
        return sb.toString();
	}
}
