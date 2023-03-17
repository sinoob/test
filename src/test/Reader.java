package test;

import java.io.IOException;

import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.boonis.greenbook.data.BillFilterData;
import com.boonis.greenbook.data.SalesBillData;
import com.boonis.greenbook.data.SalesBillPrintConfigData;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Reader {

	String baseUrl;

	public Reader(String s) {
		baseUrl = s;
	}

	public SalesBillData readBillData(long billNumber) throws RestClientException, JsonParseException, JsonMappingException, IOException {
		String url = baseUrl + "websvc/findBillDetails";

		RestTemplate restTemplate = new RestTemplate();
		BillFilterData filter = new BillFilterData();
		filter.setBillNumber(billNumber);
		String result = restTemplate.postForObject(url, filter, String.class);

		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		SalesBillData data = mapper.readValue(result, SalesBillData.class);

		return data;
	}

	public SalesBillPrintConfigData readConfigData() throws RestClientException, JsonParseException, JsonMappingException, IOException {
		String url = baseUrl + "websvc/findSalesBillPrintConfig";
		
		RestTemplate restTemplate = new RestTemplate();
		BillFilterData filter = new BillFilterData();
		String result = restTemplate.postForObject(url, filter, String.class);
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		SalesBillPrintConfigData data = mapper.readValue(result, SalesBillPrintConfigData.class);
		
		return data;
	}
	
}
