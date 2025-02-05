package org.example;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Rimal on 9/3/2015.
 */
public class JsonUtils {

  private static final Logger logger = LoggerFactory.getLogger(JsonUtils.class);
  private static final ObjectMapper MAPPER;
  private static final ObjectMapper MAPPER_INCLUDE_NULL;
  private static final JsonFactory JSON_FACTORY;
  private static final String TRANSLATIONS_JSON_KEY = "translations";


  static {
    MAPPER = new ObjectMapper();
    setDefaultMapperProperties(MAPPER);
    JSON_FACTORY = MAPPER.getFactory();

    MAPPER_INCLUDE_NULL = new ObjectMapper();
    setDefaultMapperProperties(MAPPER_INCLUDE_NULL);
    MAPPER_INCLUDE_NULL.setSerializationInclusion(JsonInclude.Include.ALWAYS);
  }
  public static String getTranslationsForLocale(String locale, String translationMetadata) {
    if (null != translationMetadata) {
      JSONObject jsonObject = new JSONObject(translationMetadata);
      if (jsonObject.has(TRANSLATIONS_JSON_KEY)) {
        jsonObject = (JSONObject) jsonObject.get(TRANSLATIONS_JSON_KEY);
        if (null != jsonObject && jsonObject.has(locale) && null != jsonObject.get(locale)) {
          return jsonObject.get(locale).toString();
        }
      }
    }
    return null;
  }

  private static void setDefaultMapperProperties(ObjectMapper mapper) {
    mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true);
    mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
    mapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_CONTROL_CHARS, true);
    mapper.configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, true);
    mapper.setNodeFactory(JsonNodeFactory.withExactBigDecimals(true));
  }

  public static boolean isValidJson(final String json) {
    Assert.notNull(json, "Json cannot be null");
    JsonNode rootNode = getRootJsonNode(json);
    return Objects.nonNull(rootNode);
  }

  public static boolean isValidJsonArray(final String json) {
    JsonNode node = getRootJsonNode(json);
    return Objects.nonNull(node) ? node.isArray() : false;
  }

  public static boolean isValidJsonObject(final String json) {
    JsonNode node = getRootJsonNode(json);
    return Objects.nonNull(node) ? node.isObject() : false;
  }

  public static JsonNode getRootJsonNode(final String json) {
    Assert.notNull(json, "Json cannot be null");
    JsonNode rootNode = null;
    if (json != null && !json.isEmpty()) {
      try {
        rootNode = MAPPER.readTree(json);
      } catch (JsonParseException e) {

      } catch (IOException e) {
        logger.error("Error encountered while checking if json was valid", e);
      }
    }
    return rootNode;
  }

  public static boolean isJsonArray(String json) {
    try {
      JsonNode jsonNode = MAPPER.readTree(json);
      return jsonNode.getNodeType().equals(JsonNodeType.ARRAY);
    } catch (Exception e) {
      logger.error("Error encountered while parsing json ", e);
    }
    return false;
  }

  /**
   * Double quotes is escaped when sending the session header , so as an alternative will be following the below format
   * {user=fsfwerwer23423423sfs,token=332fsgfgfgwerwq3rqr}
   *
   * @param formStr
   * @return
   */
  public static boolean isValidFormData(final String formStr) {
    Assert.notNull(formStr, "FormStr cannot be null");
    boolean valid = false;
    if (formStr != null && !formStr.isEmpty()) {
      Pattern p = Pattern.compile("\\{(.+=.+,?)+\\}");
      Matcher m = p.matcher(formStr);
      if (m.matches()) {
        valid = true;
      }
    }
    return valid;
  }


  public static Map<String, String> getValuesFromHeaderForm(String formStr) {
    Map<String, String> dataMap = new LinkedHashMap<>();
    if (formStr != null && !formStr.isEmpty()) {
      Pattern p = Pattern.compile("\\{(.+=.+,?)+\\}");
      Matcher m = p.matcher(formStr);
      if (m.find()) {
        String[] formData = m.group(1).split(",");
        for (String data : formData) {
          String[] dataField = data.split("=");
          if (dataField.length == 2) {
            dataMap.put(dataField[0], dataField[1]);
          }
        }
      }
    }
    return dataMap;
  }

  public static <T> T parseJsonResponse(String jsonString, Class<T> responseClass) {
    T response = null;
    if (jsonString != null && !jsonString.isEmpty()) {
      try {
        JsonParser jsonParser = JSON_FACTORY.createParser(jsonString);
        response = jsonParser.readValueAs(responseClass);
      } catch (IOException e) {
        logger.error("Error encountered while parsing json: " + jsonString + " to class: " + responseClass.getName(), e);
      }
    }
    return response;
  }

  public static <T> T parseJsonResponse(String jsonString, TypeReference<T> typeReference) {
    T response = null;
    try {
      response = MAPPER.readValue(jsonString, typeReference);
    } catch (IOException e) {
      logger.error("Error encountered while parsing JSON Response", e);
    }
    return response;
  }

  /**
   * Overrides the default configuration and includes null values in the Object.
   * Ex: {"key1": "value1", "key2" : null} will be returned as
   * Default Config: {"key1": "value1"}
   * Overridden Config: {"key1": "value1", "key2" : null}
   **/

  public static String getJsonIncludeNull(Object object) {
    if (null != object) {
      try {
        return MAPPER_INCLUDE_NULL.writeValueAsString(object);
      } catch (Exception e) {
        logger.error("Error encountered while getting json for " + object.getClass(), e);
      }
    }
    return null;
  }

  public static String getJson(Object object) {
    if (null != object) {
      try {
        return MAPPER.writeValueAsString(object);
      } catch (Exception e) {
        logger.error("Error encountered while getting json for " + object.getClass(), e);
      }
    }

    return null;
  }

  public static HashMap getJsonMap(Object object) {
    if (null != object) {
      return MAPPER.convertValue(object, HashMap.class);
    }

    return null;
  }

  public static Map<String, String> getMapFromJson(String json) {
    if (json != null && !json.isEmpty()) {
      try {
        return MAPPER.readValue(json, new TypeReference<Map<String, String>>() {
        });
      } catch (IOException e) {
        logger.error("Error encountered while getting map from json", e);
      }
    }

    return null;
  }

  public static Map<String, Object> getNestedMapFromJson(String json) {
    if (json != null && !json.isEmpty()) {
      try {
        return MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
        });
      } catch (IOException e) {
        logger.error("Error encountered while getting nested map from json", e);
      }
    }

    return null;
  }

  /*public static Map<String, List<String>> getValuesForFlattenedKeys(String json, List<String> keys) {
    Map<String, List<String>> keyToValue = new HashMap<>(keys.size());
    for (String key : keys) {
      try {
        JSONObject jsonObject = null;
        JSONArray jsonArray = null;
        try {
          jsonObject = new JSONObject(json);
        } catch (Exception e) {
          jsonArray = new JSONArray(json);
        }

        //remove all []
        key = key.replaceAll("\\[\\]", "");

        //if value already there in map, then no need to get value again
        if (null != keyToValue.get(key)) {
          continue;
        }

        String keyToSearch = key;

        //replace [0] with ->0
        Pattern p = Pattern.compile("\\[([0-9]+)\\]");
        Matcher m = p.matcher(keyToSearch);
        if (m.find()) {
          keyToSearch = m.replaceAll("->" + m.group(1));
        }

        List<String> values = new ArrayList<>();
        String[] pathArr = keyToSearch.split("->");
        int i = 0;
        for (String path : pathArr) {
          if (StringUtils.isNotBlank(path)) {
            i++;
            if (i < pathArr.length) {
              if (StringUtils.isNumeric(path)) {
                jsonObject = (JSONObject) jsonArray.get(Integer.valueOf(path));
                jsonArray = null;
              } else {
                try {
                  jsonObject = jsonObject.getJSONObject(path);
                  jsonArray = null;
                } catch (JSONException e) {
                  jsonArray = jsonObject.getJSONArray(path);
                  jsonObject = null;
                }
              }
            } else { //last leg from where we will get values
              if (jsonObject != null) {
                values.add(jsonObject.getString(path.trim()));
              } else if (jsonArray != null) {
                int j = 0;
                while (!jsonArray.isNull(j)) {
                  jsonObject = (JSONObject) jsonArray.get(j);
                  values.add(jsonObject.getString(path.trim()));
                  j++;
                }
              }
            }
          }
        }

        keyToValue.put(key, values);
      } catch (JSONException e) {
        logger.error("Exception while parsing json", e);
      }
    }

    return keyToValue;
  }*/


  /*public static void main(String[] args) {
    String json = "{\n" +
        "\t\"a\": {\n" +
        "\t\t\"aa\": 11,\n" +
        "\t\t\"ab\": 12\n" +
        "\t},\n" +
        "\t\"b\": {\n" +
        "\t\t\"ba\": 21\n" +
        "\t},\n" +
        "\t\"c\": \"3\",\n" +
        "\t\"d\": {\n" +
        "\t\t\"d1\": [{\n" +
        "\t\t\t\"da\": \"411\",\n" +
        "\t\t\t\"db\": \"412\"\n" +
        "\t\t}, {\n" +
        "\t\t\t\"da\": \"421\",\n" +
        "\t\t\t\"db\": \"422\"\n" +
        "\t\t}]\n" +
        "\t},\n" +
        "\t\"e\": [\"e1\"]\n" +
        "}";
    Map<String, String> map = getFlattenedKeyMapFromJson(json);

    for (Map.Entry o : map.entrySet()) {
      System.out.println(o.getKey() + " :: " + o.getValue());
    }


    String json1 = json;
    List<String> keys = Arrays.asList("a->aa", "a->aa[]", "d->d1->db", "e", "e[0]");*//*



    String json1 = "{\"results\":{\"msgs\":[],\"exception\":false,\"stId\":1,\"order\":{\"gId\":\"HK93608-892538\",\"address\":{\"id\":1284545,\"nm\":\"Khushboo Shivhare\",\"line1\":\"B-34\\/A, Sector-14, Old DLF Colony\",\"landmark\":null,\"cntNum\":\"9717279944\",\"altCntNum\":null,\"stateId\":null,\"stateNm\":\"HARYANA\",\"cityId\":null,\"cityNm\":\"GURGAON\",\"plcId\":null,\"plcNm\":null,\"pinId\":null,\"pin\":\"122001\"},\"orderDt\":\"2014-10-17T22:39:00\",\"oprLineItems\":[{\"id\":2689803,\"sv_nm\":\"Lakme Eyeconic Kajal,   Black \",\"wmsVarId\":\"LAKNTF4-01\",\"mrp\":200.0,\"offer_pr\":180,\"codCh\":0.0,\"rpDis\":5.3,\"orderLvlDis\":0.0,\"shippingCh\":0.0,\"disOP\":0.0,\"varAs\":10,\"srcEnty\":\"11217\",\"status\":\"Delivered\",\"estDispatchDate\":null,\"estDeliveryDate\":null,\"promisedExpiryDate\":null,\"netOprLiAmnt\":174.7,\"batch_id\":null,\"courierNm\":\"Grofers\",\"trackLink\":null,\"awbNumber\":\"ec00300875\",\"navKey\":\"VRNT-11217\",\"urlFragment\":\"\\/lakme-eyeconic-kajal\\/SP-7522\",\"primaryImage\":{\"lp\":null,\"alt\":\"Lakme Eyeconic Kajal,   Black \",\"cap\":null,\"fallbk\":false,\"xxt_link\":\"http:\\/\\/img3.hkrtcdn.com\\/1209\\/pck_120892_c_xxt.jpg\",\"xt_link\":\"http:\\/\\/img3.hkrtcdn.com\\/1209\\/pck_120892_c_xt.jpg\",\"t_link\":\"http:\\/\\/img3.hkrtcdn.com\\/1209\\/pck_120892_c_t.jpg\",\"xxs_link\":\"http:\\/\\/img3.hkrtcdn.com\\/1209\\/pck_120892_c_xxs.jpg\",\"xs_link\":\"http:\\/\\/img3.hkrtcdn.com\\/1209\\/pck_120892_c_xs.jpg\",\"s_link\":\"http:\\/\\/img3.hkrtcdn.com\\/1209\\/pck_120892_c_s.jpg\",\"m_link\":\"http:\\/\\/img3.hkrtcdn.com\\/1209\\/pck_120892_c_m.jpg\",\"l_link\":\"http:\\/\\/img3.hkrtcdn.com\\/1209\\/pck_120892_c_l.jpg\",\"xxl_link\":\"http:\\/\\/img3.hkrtcdn.com\\/1209\\/pck_120892_c_xxl.jpg\"},\"returnable\":false,\"dispatchDate\":null,\"deliveryDate\":\"2014-10-22T18:00:19\",\"oliDate\":\"2014-10-17T22:39:01\",\"sp_typ\":null},{\"id\":2689804,\"sv_nm\":\"Tresemme Climate Control Shampoo,  600 ml  UV Blocker \",\"wmsVarId\":\"TRSME01-02\",\"mrp\":335.0,\"offer_pr\":285,\"codCh\":0.0,\"rpDis\":8.4,\"orderLvlDis\":0.0,\"shippingCh\":0.0,\"disOP\":0.0,\"varAs\":10,\"srcEnty\":\"48189\",\"status\":\"Delivered\",\"estDispatchDate\":null,\"estDeliveryDate\":null,\"promisedExpiryDate\":null,\"netOprLiAmnt\":276.6,\"batch_id\":null,\"courierNm\":\"Grofers\",\"trackLink\":null,\"awbNumber\":\"ec00300875\",\"navKey\":\"VRNT-48189\",\"urlFragment\":\"\\/tresemme-climate-control-shampoo\\/SP-14629\",\"primaryImage\":{\"lp\":null,\"alt\":\"Tresemme Climate Control Shampoo,  600 ml  UV Blocker \",\"cap\":null,\"fallbk\":false,\"xxt_link\":\"http:\\/\\/img3.hkrtcdn.com\\/1209\\/pck_120892_c_xxt.jpg\",\"xt_link\":\"http:\\/\\/img3.hkrtcdn.com\\/1209\\/pck_120892_c_xt.jpg\",\"t_link\":\"http:\\/\\/img3.hkrtcdn.com\\/1209\\/pck_120892_c_t.jpg\",\"xxs_link\":\"http:\\/\\/img3.hkrtcdn.com\\/1209\\/pck_120892_c_xxs.jpg\",\"xs_link\":\"http:\\/\\/img3.hkrtcdn.com\\/1209\\/pck_120892_c_xs.jpg\",\"s_link\":\"http:\\/\\/img3.hkrtcdn.com\\/1209\\/pck_120892_c_s.jpg\",\"m_link\":\"http:\\/\\/img3.hkrtcdn.com\\/1209\\/pck_120892_c_m.jpg\",\"l_link\":\"http:\\/\\/img3.hkrtcdn.com\\/1209\\/pck_120892_c_l.jpg\",\"xxl_link\":\"http:\\/\\/img3.hkrtcdn.com\\/1209\\/pck_120892_c_xxl.jpg\"},\"returnable\":false,\"dispatchDate\":null,\"deliveryDate\":\"2014-10-22T18:00:19\",\"oliDate\":\"2014-10-17T22:39:01\",\"sp_typ\":null},{\"id\":2689805,\"sv_nm\":\"Lakme Eyeconic Kajal,   Black \",\"wmsVarId\":\"LAKNTF4-01\",\"mrp\":200.0,\"offer_pr\":180,\"codCh\":0.0,\"rpDis\":5.3,\"orderLvlDis\":0.0,\"shippingCh\":0.0,\"disOP\":0.0,\"varAs\":10,\"srcEnty\":\"11217\",\"status\":\"Delivered\",\"estDispatchDate\":null,\"estDeliveryDate\":null,\"promisedExpiryDate\":null,\"netOprLiAmnt\":174.7,\"batch_id\":null,\"courierNm\":\"Grofers\",\"trackLink\":null,\"awbNumber\":\"ec00300875\",\"navKey\":\"VRNT-11217\",\"urlFragment\":\"\\/lakme-eyeconic-kajal\\/SP-7522\",\"primaryImage\":{\"lp\":null,\"alt\":\"Lakme Eyeconic Kajal,   Black \",\"cap\":null,\"fallbk\":false,\"xxt_link\":\"http:\\/\\/img3.hkrtcdn.com\\/1209\\/pck_120892_c_xxt.jpg\",\"xt_link\":\"http:\\/\\/img3.hkrtcdn.com\\/1209\\/pck_120892_c_xt.jpg\",\"t_link\":\"http:\\/\\/img3.hkrtcdn.com\\/1209\\/pck_120892_c_t.jpg\",\"xxs_link\":\"http:\\/\\/img3.hkrtcdn.com\\/1209\\/pck_120892_c_xxs.jpg\",\"xs_link\":\"http:\\/\\/img3.hkrtcdn.com\\/1209\\/pck_120892_c_xs.jpg\",\"s_link\":\"http:\\/\\/img3.hkrtcdn.com\\/1209\\/pck_120892_c_s.jpg\",\"m_link\":\"http:\\/\\/img3.hkrtcdn.com\\/1209\\/pck_120892_c_m.jpg\",\"l_link\":\"http:\\/\\/img3.hkrtcdn.com\\/1209\\/pck_120892_c_l.jpg\",\"xxl_link\":\"http:\\/\\/img3.hkrtcdn.com\\/1209\\/pck_120892_c_xxl.jpg\"},\"returnable\":false,\"dispatchDate\":null,\"deliveryDate\":\"2014-10-22T18:00:19\",\"oliDate\":\"2014-10-17T22:39:01\",\"sp_typ\":null}],\"orderAmount\":626.0,\"paymentType\":1000}},\"statusCode\":200}";
    List<String> keys = Arrays.asList(
        "results->order->oprLineItems[]->primaryImage->xs_link",
        "results->order->oprLineItems[]->trackLink"
    );



    String json1 = "{\"serviceable_cities\": [\"DELHI\", \"NEW DELHI\", \"GURGAON\", \"NOIDA\", \"FARIDABAD\", \"GHAZIABAD\", \"MUMBAI\", \"NAVI MUMBAI\", \"BANGALORE\", \"BENGALURU\", \"HYDERABAD\", \"CHENNAI\", \"KOLKATA\", \"PUNE\", \"THANE\", \"AHMEDABAD\", \"LUCKNOW\", \"INDORE\", \"CHANDIGARH\", \"AMBALA\", \"PANCHKULA\", \"GREATER NOIDA\"]}";
    List<String> keys = Collections.singletonList("serviceable_cities");*//*



    String json1 = "[{\"_charges\": {\"_report_delivery\": 30}, \"_lab_name\": \"Quest Diagnostics\", \"_lab_id\": 22, \"_price\": 890, \"_home_visit_available\": true, \"_lab_accreditation\": \"NABL,CAP,NGSP\", \"_packages\": [{\"_discount_percent\": 77, \"_type\": \"PACKAGE\", \"_price\": 8000.0, \"_rank\": 0, \"_catalog_commission_percent\": 50.9537, \"_reporting_tat\": {\"before 11:00 AM\": 72, \"after 11:00 AM\": 48}, \"_test_name\": \"Personal Wellness CheckUp (With Personalized Doctor Guidance Report)\", \"_catalog_price\": 3670.0, \"_discounted_price\": 1799, \"_home_visit_charges\": 0.0, \"_description\": null, \"_times_availed\": 836, \"_precaution\": null, \"_status\": \"active\", \"_test_sub_name\": null, \"_test_id\": 2609, \"_tests_count\": 55}], \"_rated_by\": 100, \"_lab_rating\": 2.5, \"_tests\": [{\"_discount_percent\": 10.0, \"_type\": \"TEST\", \"_price\": 890.0, \"_rank\": 0, \"_catalog_commission_percent\": 25.0, \"_reporting_tat\": null, \"_test_name\": \"Thyroid Profile Free\", \"_catalog_price\": 890.0, \"_discounted_price\": 801, \"_home_visit_charges\": 0.0, \"_description\": null, \"_times_availed\": 26, \"_precaution\": null, \"_status\": \"active\", \"_test_sub_name\": \"FT3, FT4, TSH\", \"_test_id\": 1321, \"_tests_count\": 0}], \"_lab_logo_url\": \"https://res.cloudinary.com/onemg/image/upload/v1459774398/diagnostics/Quest_Logo-2.jpg\", \"_discount_percent\": 10, \"_discounted_price\": 801}, {\"_charges\": {\"_report_delivery\": 30}, \"_lab_name\": \"Metropolis Laboratories\", \"_lab_id\": 19, \"_price\": 800, \"_home_visit_available\": true, \"_lab_accreditation\": \"NABL,CAP,SANAS\", \"_rated_by\": 81, \"_lab_rating\": 3.9, \"_tests\": [{\"_discount_percent\": 10.0, \"_type\": \"TEST\", \"_price\": 800.0, \"_rank\": 0, \"_catalog_commission_percent\": 25.0, \"_reporting_tat\": null, \"_test_name\": \"Thyroid Profile Free\", \"_catalog_price\": 800.0, \"_discounted_price\": 720, \"_home_visit_charges\": 0.0, \"_description\": null, \"_times_availed\": 23, \"_precaution\": null, \"_status\": \"active\", \"_test_sub_name\": \"FT3, FT4, TSH\", \"_test_id\": 1321, \"_tests_count\": 0}], \"_lab_logo_url\": \"https://res.cloudinary.com/onemg/image/upload/v1443004139/diagnostics/Metropolis-logo.png\", \"_discount_percent\": 10, \"_discounted_price\": 720}, {\"_charges\": {\"_report_delivery\": 0}, \"_lab_name\": \"Wellness Pathcare\", \"_lab_id\": 13, \"_price\": 650, \"_home_visit_available\": true, \"_lab_accreditation\": \"ISO\", \"_rated_by\": 127, \"_lab_rating\": 3.5, \"_tests\": [{\"_discount_percent\": 13.0, \"_type\": \"TEST\", \"_price\": 650.0, \"_rank\": 0, \"_catalog_commission_percent\": 23.0, \"_reporting_tat\": null, \"_test_name\": \"Thyroid Profile Free\", \"_catalog_price\": 650.0, \"_discounted_price\": 565, \"_home_visit_charges\": 0.0, \"_description\": null, \"_times_availed\": 19, \"_precaution\": null, \"_status\": \"active\", \"_test_sub_name\": \"FT3, FT4, TSH\", \"_test_id\": 1321, \"_tests_count\": 0}], \"_lab_logo_url\": \"https://res.cloudinary.com/onemg/image/upload/v1457001569/diagnostics/wellness.jpg\", \"_discount_percent\": 13, \"_discounted_price\": 565}, {\"_charges\": {\"_report_delivery\": 30}, \"_lab_name\": \"Oncquest Laboratories\", \"_lab_id\": 23, \"_price\": 750, \"_home_visit_available\": true, \"_lab_accreditation\": \"NABL,CAP,ISO\", \"_rated_by\": 41, \"_lab_rating\": 3.0, \"_tests\": [{\"_discount_percent\": 0.0, \"_type\": \"TEST\", \"_price\": 750.0, \"_rank\": 0, \"_catalog_commission_percent\": 20.0, \"_reporting_tat\": null, \"_test_name\": \"Thyroid Profile Free\", \"_catalog_price\": 750.0, \"_discounted_price\": 750, \"_home_visit_charges\": 0.0, \"_description\": null, \"_times_availed\": 16, \"_precaution\": null, \"_status\": \"active\", \"_test_sub_name\": \"FT3, FT4, TSH\", \"_test_id\": 1321, \"_tests_count\": 0}], \"_lab_logo_url\": \"https://res.cloudinary.com/onemg/image/upload/v1445426424/diagnostics/oncuest_logo.jpg\", \"_discount_percent\": 0, \"_discounted_price\": 750}, {\"_charges\": {\"_report_delivery\": 30}, \"_lab_name\": \"Vimta Labs Limited\", \"_lab_id\": 45, \"_price\": 750, \"_home_visit_available\": true, \"_lab_accreditation\": \"NABL\", \"_rated_by\": 7, \"_lab_rating\": 3.0, \"_tests\": [{\"_discount_percent\": 15.0, \"_type\": \"TEST\", \"_price\": 750.0, \"_rank\": 0, \"_catalog_commission_percent\": 15.0, \"_reporting_tat\": null, \"_test_name\": \"Thyroid Profile Free\", \"_catalog_price\": 750.0, \"_discounted_price\": 637, \"_home_visit_charges\": 0.0, \"_description\": null, \"_times_availed\": 1, \"_precaution\": null, \"_status\": \"active\", \"_test_sub_name\": \"FT3, FT4, TSH\", \"_test_id\": 1321, \"_tests_count\": 0}], \"_lab_logo_url\": \"https://res.cloudinary.com/onemg/image/upload/v1459765483/diagnostics/vimita_1.png\", \"_discount_percent\": 15, \"_discounted_price\": 637}]";
    List<String> keys = Collections.singletonList("_lab_name[]");



    String json1 = "{\"page_title\":\"Used Electronics & Appliances for sale in India | OLX\",\"web_page_url\":\"https:\\/\\/www.olx.in\\/electronics-appliances\\/?search%5Bphotos%5D=0\",\"category_id\":\"99\",\"params\":{\"region\":0,\"subregion\":0,\"category\":99,\"shopId\":0,\"city\":0,\"region_name\":null,\"city_name\":null,\"params\":{\"search\":[]}},\"total_ads\":249693,\"top_header_labels\":{\"category_icon\":\"https:\\/\\/s2.olx.in\\/static\\/olxin\\/naspersclassifieds-regional\\/olxsa-atlas-web-olxin\\/static\\/img\\/i2\\/categories\\/99_small_2.png?v=62\",\"location_label\":\"All India\"},\"page\":1,\"total_pages\":500,\"ads_on_page\":10,\"view\":\"list\",\"next_page_url\":\"https:\\/\\/www.olx.in\\/i2\\/electronics-appliances\\/?json=1&search%5Bphotos%5D=0&page=2\",\"ads\":[{\"id\":\"1009870737\",\"url\":\"https:\\/\\/www.olx.in\\/item\\/lenovo-g450-in-good-condition-only-2-year-old-ID16ljwH.html\",\"preview_url\":\"https:\\/\\/www.olx.in\\/i2\\/item\\/lenovo-g450-in-good-condition-only-2-year-old-ID16ljwH.html?json=1\",\"title\":\"Lenovo G450 in good condition Only 2 year old\",\"created\":\"Yesterday\",\"age\":0,\"description\":\"(((Urgent sell)))\\nLenovo G450 in good condition \\nOnly 2 year old (price fixed)\\n*no bargain\\n*300 hard disc\\n*2GB Ram\\n*dual core processer\\n*genuine windows 10\\n*orginal charger\\n*Laptop bag\\n*nvidia graphics\\n*DVD rw\\n*WiFi\\n*Bluetooth\\n*14.1 inch screen\\n*3 houre battery backup\\n*web camera\\n*No damage \\n*No scratch\\nThis is very good working condition\\nIf you want buy please call me\\nAddress\\n Vijaya bank layout bilekahalli banneraghatta main Road Bangalore\\nLand mark vijaya bank layout petrol bunk\",\"highlighted\":1,\"urgent\":0,\"topAd\":1,\"category_id\":1505,\"params\":[],\"subtitle\":[],\"business\":0,\"hide_user_ads_button\":0,\"phone_confirmed\":0,\"hide_user_phone\":0,\"header\":\"Featured Ads\",\"header_type\":\"promoted\",\"has_phone\":1,\"has_email\":1,\"map_zoom\":\"13\",\"map_lat\":\"12.90129000\",\"map_lon\":\"77.60280000\",\"map_radius\":1,\"map_show_detailed\":false,\"city_label\":\"Bengaluru, Bilekahalli\",\"person\":\"Lenovo laptop\",\"user_label\":\"Lenovo laptop\",\"user_ads_id\":\"4qJRJ\",\"user_id\":\"4qJRJ\",\"numeric_user_id\":\"65478183\",\"user_ads_url\":\"https:\\/\\/www.olx.in\\/i2\\/all-results\\/user\\/4qJRJ\\/?json=1&search%5Buser_id%5D=65478183\",\"list_label\":\"? 11,500\",\"list_label_ad\":\"? 11,500\",\"photos\":[{\"3\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_1_1000x700_lenovo-g450-in-good-condition-only-2-year-old-bengaluru.jpg\",\"0\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_1_644x461_lenovo-g450-in-good-condition-only-2-year-old-bengaluru.jpg\",\"2\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_1_261x203_lenovo-g450-in-good-condition-only-2-year-old-bengaluru.jpg\",\"1\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_1_94x72_lenovo-g450-in-good-condition-only-2-year-old-bengaluru.jpg\",\"9\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_1_144x108_lenovo-g450-in-good-condition-only-2-year-old-bengaluru.jpg\"},{\"3\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_2_1000x700_lenovo-g450-in-good-condition-only-2-year-old-upload-photos.jpg\",\"0\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_2_644x461_lenovo-g450-in-good-condition-only-2-year-old-upload-photos.jpg\",\"2\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_2_261x203_lenovo-g450-in-good-condition-only-2-year-old-upload-photos.jpg\",\"1\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_2_94x72_lenovo-g450-in-good-condition-only-2-year-old-upload-photos.jpg\",\"9\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_2_144x108_lenovo-g450-in-good-condition-only-2-year-old-upload-photos.jpg\"},{\"3\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_3_1000x700_lenovo-g450-in-good-condition-only-2-year-old-laptops.jpg\",\"0\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_3_644x461_lenovo-g450-in-good-condition-only-2-year-old-laptops.jpg\",\"2\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_3_261x203_lenovo-g450-in-good-condition-only-2-year-old-laptops.jpg\",\"1\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_3_94x72_lenovo-g450-in-good-condition-only-2-year-old-laptops.jpg\",\"9\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_3_144x108_lenovo-g450-in-good-condition-only-2-year-old-laptops.jpg\"},{\"3\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_4_1000x700_lenovo-g450-in-good-condition-only-2-year-old-electronics-appliances.jpg\",\"0\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_4_644x461_lenovo-g450-in-good-condition-only-2-year-old-electronics-appliances.jpg\",\"2\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_4_261x203_lenovo-g450-in-good-condition-only-2-year-old-electronics-appliances.jpg\",\"1\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_4_94x72_lenovo-g450-in-good-condition-only-2-year-old-electronics-appliances.jpg\",\"9\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_4_144x108_lenovo-g450-in-good-condition-only-2-year-old-electronics-appliances.jpg\"},{\"3\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_5_1000x700_lenovo-g450-in-good-condition-only-2-year-old-karnataka.jpg\",\"0\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_5_644x461_lenovo-g450-in-good-condition-only-2-year-old-karnataka.jpg\",\"2\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_5_261x203_lenovo-g450-in-good-condition-only-2-year-old-karnataka.jpg\",\"1\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_5_94x72_lenovo-g450-in-good-condition-only-2-year-old-karnataka.jpg\",\"9\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_5_144x108_lenovo-g450-in-good-condition-only-2-year-old-karnataka.jpg\"},{\"3\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_6_1000x700_lenovo-g450-in-good-condition-only-2-year-old-.jpg\",\"0\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_6_644x461_lenovo-g450-in-good-condition-only-2-year-old-.jpg\",\"2\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_6_261x203_lenovo-g450-in-good-condition-only-2-year-old-.jpg\",\"1\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_6_94x72_lenovo-g450-in-good-condition-only-2-year-old-.jpg\",\"9\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_6_144x108_lenovo-g450-in-good-condition-only-2-year-old-.jpg\"},{\"3\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_7_1000x700_lenovo-g450-in-good-condition-only-2-year-old-.jpg\",\"0\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_7_644x461_lenovo-g450-in-good-condition-only-2-year-old-.jpg\",\"2\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_7_261x203_lenovo-g450-in-good-condition-only-2-year-old-.jpg\",\"1\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_7_94x72_lenovo-g450-in-good-condition-only-2-year-old-.jpg\",\"9\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_7_144x108_lenovo-g450-in-good-condition-only-2-year-old-.jpg\"},{\"3\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_8_1000x700_lenovo-g450-in-good-condition-only-2-year-old-.jpg\",\"0\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_8_644x461_lenovo-g450-in-good-condition-only-2-year-old-.jpg\",\"2\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_8_261x203_lenovo-g450-in-good-condition-only-2-year-old-.jpg\",\"1\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_8_94x72_lenovo-g450-in-good-condition-only-2-year-old-.jpg\",\"9\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210188027_8_144x108_lenovo-g450-in-good-condition-only-2-year-old-.jpg\"}],\"user_online_status\":1,\"user_online_message\":\"Online\",\"chat_options\":1,\"page_title\":\"Lenovo G450 in good condition Only 2 year old Bengaluru  � OLX.in\",\"user_active_ads_count\":2,\"views_count\":0,\"phone\":\"+918970085363\",\"city_id\":\"58803\",\"badges\":[{\"type\":\"phone_confirmed\",\"visibility\":1,\"title\":\"Phone Number Verified Badge\",\"text\":\"This seller's mobile number has been verified via OTP\"},{\"type\":\"email_confirmed\",\"visibility\":1,\"title\":\"Email Verified Badge\",\"text\":\"This seller�s email has been verified\"},{\"type\":\"facebook_confirmed\",\"visibility\":0,\"title\":\"Facebook Profile Verified Badge\",\"text\":\"This seller�s FB profile has been verified\"}],\"user_pic_url\":\"\",\"total_comments\":0,\"community_id\":0,\"is_community_ad\":0,\"user_community_member_since\":\"\",\"user_member_since\":\"\",\"block_user_card\":0},{\"id\":\"990739111\",\"url\":\"https:\\/\\/www.olx.in\\/item\\/led-lcd-services-old-lcd-led-buying-selling-ID1532w5.html\",\"preview_url\":\"https:\\/\\/www.olx.in\\/i2\\/item\\/led-lcd-services-old-lcd-led-buying-selling-ID1532w5.html?json=1\",\"title\":\"Led & lcd services . old lcd & led buying & selling\",\"created\":\"16  Jun\",\"age\":64,\"description\":\"All brands of Lcd & led servicing center. Servicing at your door step.lcd sheet or film work is done here. Panel problems , mother board problems , power board problems are slove here.\",\"highlighted\":1,\"urgent\":0,\"topAd\":1,\"category_id\":1523,\"params\":[],\"subtitle\":[],\"business\":0,\"hide_user_ads_button\":0,\"phone_confirmed\":0,\"hide_user_phone\":0,\"has_phone\":1,\"has_email\":1,\"map_zoom\":\"13\",\"map_lat\":\"16.30424000\",\"map_lon\":\"80.44006000\",\"map_radius\":1,\"map_show_detailed\":false,\"city_label\":\"Guntur, Arundelpeta\",\"person\":\"Ashok kumar\",\"user_label\":\"Ashok kumar\",\"user_ads_id\":\"5B4QT\",\"user_id\":\"5B4QT\",\"numeric_user_id\":\"82718471\",\"user_ads_url\":\"https:\\/\\/www.olx.in\\/i2\\/all-results\\/user\\/5B4QT\\/?json=1&search%5Buser_id%5D=82718471\",\"list_label\":\"? 300\",\"list_label_ad\":\"? 300\",\"photos\":[{\"3\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_1_1000x700_led-lcd-services-old-lcd-led-buying-selling-guntur.jpg\",\"0\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_1_644x461_led-lcd-services-old-lcd-led-buying-selling-guntur.jpg\",\"2\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_1_261x203_led-lcd-services-old-lcd-led-buying-selling-guntur.jpg\",\"1\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_1_94x72_led-lcd-services-old-lcd-led-buying-selling-guntur.jpg\",\"9\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_1_144x108_led-lcd-services-old-lcd-led-buying-selling-guntur.jpg\"},{\"3\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_2_1000x700_led-lcd-services-old-lcd-led-buying-selling-upload-photos.jpg\",\"0\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_2_644x461_led-lcd-services-old-lcd-led-buying-selling-upload-photos.jpg\",\"2\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_2_261x203_led-lcd-services-old-lcd-led-buying-selling-upload-photos.jpg\",\"1\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_2_94x72_led-lcd-services-old-lcd-led-buying-selling-upload-photos.jpg\",\"9\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_2_144x108_led-lcd-services-old-lcd-led-buying-selling-upload-photos.jpg\"},{\"3\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_3_1000x700_led-lcd-services-old-lcd-led-buying-selling-tv.jpg\",\"0\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_3_644x461_led-lcd-services-old-lcd-led-buying-selling-tv.jpg\",\"2\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_3_261x203_led-lcd-services-old-lcd-led-buying-selling-tv.jpg\",\"1\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_3_94x72_led-lcd-services-old-lcd-led-buying-selling-tv.jpg\",\"9\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_3_144x108_led-lcd-services-old-lcd-led-buying-selling-tv.jpg\"},{\"3\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_4_1000x700_led-lcd-services-old-lcd-led-buying-selling-electronics-appliances.jpg\",\"0\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_4_644x461_led-lcd-services-old-lcd-led-buying-selling-electronics-appliances.jpg\",\"2\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_4_261x203_led-lcd-services-old-lcd-led-buying-selling-electronics-appliances.jpg\",\"1\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_4_94x72_led-lcd-services-old-lcd-led-buying-selling-electronics-appliances.jpg\",\"9\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_4_144x108_led-lcd-services-old-lcd-led-buying-selling-electronics-appliances.jpg\"},{\"3\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_5_1000x700_led-lcd-services-old-lcd-led-buying-selling-andhra-pradesh.jpg\",\"0\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_5_644x461_led-lcd-services-old-lcd-led-buying-selling-andhra-pradesh.jpg\",\"2\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_5_261x203_led-lcd-services-old-lcd-led-buying-selling-andhra-pradesh.jpg\",\"1\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_5_94x72_led-lcd-services-old-lcd-led-buying-selling-andhra-pradesh.jpg\",\"9\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_5_144x108_led-lcd-services-old-lcd-led-buying-selling-andhra-pradesh.jpg\"},{\"3\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_6_1000x700_led-lcd-services-old-lcd-led-buying-selling-.jpg\",\"0\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_6_644x461_led-lcd-services-old-lcd-led-buying-selling-.jpg\",\"2\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_6_261x203_led-lcd-services-old-lcd-led-buying-selling-.jpg\",\"1\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_6_94x72_led-lcd-services-old-lcd-led-buying-selling-.jpg\",\"9\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_6_144x108_led-lcd-services-old-lcd-led-buying-selling-.jpg\"},{\"3\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_7_1000x700_led-lcd-services-old-lcd-led-buying-selling-.jpg\",\"0\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_7_644x461_led-lcd-services-old-lcd-led-buying-selling-.jpg\",\"2\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_7_261x203_led-lcd-services-old-lcd-led-buying-selling-.jpg\",\"1\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_7_94x72_led-lcd-services-old-lcd-led-buying-selling-.jpg\",\"9\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_7_144x108_led-lcd-services-old-lcd-led-buying-selling-.jpg\"},{\"3\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_8_1000x700_led-lcd-services-old-lcd-led-buying-selling-.jpg\",\"0\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_8_644x461_led-lcd-services-old-lcd-led-buying-selling-.jpg\",\"2\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_8_261x203_led-lcd-services-old-lcd-led-buying-selling-.jpg\",\"1\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_8_94x72_led-lcd-services-old-lcd-led-buying-selling-.jpg\",\"9\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/191900069_8_144x108_led-lcd-services-old-lcd-led-buying-selling-.jpg\"}],\"user_online_status\":0,\"user_online_message\":\"Away\",\"chat_options\":1,\"page_title\":\"Led & lcd services . old lcd & led buying & selling Guntur  � OLX.in\",\"user_active_ads_count\":1,\"views_count\":0,\"phone\":\"+919848971331\",\"city_id\":\"58524\",\"badges\":[{\"type\":\"phone_confirmed\",\"visibility\":1,\"title\":\"Phone Number Verified Badge\",\"text\":\"This seller's mobile number has been verified via OTP\"},{\"type\":\"email_confirmed\",\"visibility\":0,\"title\":\"Email Verified Badge\",\"text\":\"This seller�s email has been verified\"},{\"type\":\"facebook_confirmed\",\"visibility\":0,\"title\":\"Facebook Profile Verified Badge\",\"text\":\"This seller�s FB profile has been verified\"}],\"user_pic_url\":\"\",\"total_comments\":0,\"community_id\":0,\"is_community_ad\":0,\"user_community_member_since\":\"\",\"user_member_since\":\"\",\"block_user_card\":0},{\"id\":\"1005697727\",\"url\":\"https:\\/\\/www.olx.in\\/item\\/lcd-branded-wipro-look-wise-new-bawa-computer-sunday-open-ID163NW5.html\",\"preview_url\":\"https:\\/\\/www.olx.in\\/i2\\/item\\/lcd-branded-wipro-look-wise-new-bawa-computer-sunday-open-ID163NW5.html?json=1\",\"title\":\"lcd branded wipro look wise new bawa computer sunday open\",\"created\":\"11  Jun\",\"age\":18,\"description\":\"i have wipro lcd in new condtion 16 inch to 19 inch \\r\\nstarting rang 2500 for more detail and price for lcd can me \\r\\nworking in good condtion\",\"highlighted\":1,\"urgent\":0,\"topAd\":1,\"category_id\":1503,\"params\":[],\"subtitle\":[],\"business\":0,\"hide_user_ads_button\":0,\"phone_confirmed\":0,\"hide_user_phone\":0,\"has_phone\":1,\"has_email\":1,\"map_zoom\":\"13\",\"map_lat\":\"30.89387590\",\"map_lon\":\"75.85719680\",\"map_radius\":1,\"map_show_detailed\":false,\"city_label\":\"Ludhiana, Basti Jodhewal\",\"person\":\"bawa computer\",\"user_label\":\"bawa computer\",\"user_ads_id\":\"6luKJ\",\"user_id\":\"6luKJ\",\"numeric_user_id\":\"93781121\",\"user_ads_url\":\"https:\\/\\/www.olx.in\\/i2\\/all-results\\/user\\/6luKJ\\/?json=1&search%5Buser_id%5D=93781121\",\"list_label\":\"? 2,500\",\"list_label_ad\":\"? 2,500\",\"photos\":[{\"3\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/205501921_1_1000x700_lcd-branded-wipro-look-wise-new-bawa-computer-sunday-open-ludhiana_rev001.jpg\",\"0\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/205501921_1_644x461_lcd-branded-wipro-look-wise-new-bawa-computer-sunday-open-ludhiana_rev001.jpg\",\"2\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/205501921_1_261x203_lcd-branded-wipro-look-wise-new-bawa-computer-sunday-open-ludhiana_rev001.jpg\",\"1\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/205501921_1_94x72_lcd-branded-wipro-look-wise-new-bawa-computer-sunday-open-ludhiana_rev001.jpg\",\"9\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/205501921_1_144x108_lcd-branded-wipro-look-wise-new-bawa-computer-sunday-open-ludhiana_rev001.jpg\"},{\"3\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/205501921_2_1000x700_lcd-branded-wipro-look-wise-new-bawa-computer-sunday-open-upload-photos_rev001.jpg\",\"0\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/205501921_2_644x461_lcd-branded-wipro-look-wise-new-bawa-computer-sunday-open-upload-photos_rev001.jpg\",\"2\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/205501921_2_261x203_lcd-branded-wipro-look-wise-new-bawa-computer-sunday-open-upload-photos_rev001.jpg\",\"1\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/205501921_2_94x72_lcd-branded-wipro-look-wise-new-bawa-computer-sunday-open-upload-photos_rev001.jpg\",\"9\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/205501921_2_144x108_lcd-branded-wipro-look-wise-new-bawa-computer-sunday-open-upload-photos_rev001.jpg\"},{\"3\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/205501921_3_1000x700_lcd-branded-wipro-look-wise-new-bawa-computer-sunday-open-computers_rev001.jpg\",\"0\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/205501921_3_644x461_lcd-branded-wipro-look-wise-new-bawa-computer-sunday-open-computers_rev001.jpg\",\"2\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/205501921_3_261x203_lcd-branded-wipro-look-wise-new-bawa-computer-sunday-open-computers_rev001.jpg\",\"1\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/205501921_3_94x72_lcd-branded-wipro-look-wise-new-bawa-computer-sunday-open-computers_rev001.jpg\",\"9\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/205501921_3_144x108_lcd-branded-wipro-look-wise-new-bawa-computer-sunday-open-computers_rev001.jpg\"}],\"user_online_status\":0,\"user_online_message\":\"Away\",\"chat_options\":1,\"page_title\":\"lcd branded wipro look wise new bawa computer sunday open Ludhiana  � OLX.in\",\"user_active_ads_count\":9,\"views_count\":0,\"phone\":\"+919988267948\",\"city_id\":\"59085\",\"badges\":[{\"type\":\"phone_confirmed\",\"visibility\":1,\"title\":\"Phone Number Verified Badge\",\"text\":\"This seller's mobile number has been verified via OTP\"},{\"type\":\"email_confirmed\",\"visibility\":1,\"title\":\"Email Verified Badge\",\"text\":\"This seller�s email has been verified\"},{\"type\":\"facebook_confirmed\",\"visibility\":0,\"title\":\"Facebook Profile Verified Badge\",\"text\":\"This seller�s FB profile has been verified\"}],\"user_pic_url\":\"\",\"total_comments\":0,\"community_id\":0,\"is_community_ad\":0,\"user_community_member_since\":\"\",\"user_member_since\":\"\",\"block_user_card\":0},{\"id\":\"988046411\",\"url\":\"https:\\/\\/www.olx.in\\/item\\/everest-stabilizer-single-booster-ID14RK0r.html\",\"preview_url\":\"https:\\/\\/www.olx.in\\/i2\\/item\\/everest-stabilizer-single-booster-ID14RK0r.html?json=1\",\"title\":\"everest stabilizer single booster\",\"created\":\"04:09 pm\",\"age\":70,\"description\":\"good working condition 2 nos available each 800rs\",\"highlighted\":0,\"urgent\":0,\"topAd\":0,\"category_id\":1417,\"params\":[],\"subtitle\":[],\"business\":0,\"hide_user_ads_button\":0,\"phone_confirmed\":1,\"hide_user_phone\":0,\"header\":\"All Ads\",\"header_type\":\"all\",\"has_phone\":1,\"has_email\":1,\"map_zoom\":\"13\",\"map_lat\":\"13.04931000\",\"map_lon\":\"80.16555000\",\"map_radius\":1,\"map_show_detailed\":false,\"city_label\":\"Chennai, Alapakkam Thirumurugan Nagar\",\"person\":\"Ravi Kumar\",\"user_label\":\"Ravi Kumar\",\"user_ads_id\":\"65SGT\",\"user_id\":\"65SGT\",\"numeric_user_id\":\"90059891\",\"user_ads_url\":\"https:\\/\\/www.olx.in\\/i2\\/all-results\\/user\\/65SGT\\/?json=1&search%5Buser_id%5D=90059891\",\"list_label\":\"? 800\",\"list_label_ad\":\"? 800\",\"photos\":[{\"3\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/189785531_1_1000x700_everest-stabilizer-single-booster-chennai_rev002.jpg\",\"0\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/189785531_1_644x461_everest-stabilizer-single-booster-chennai_rev002.jpg\",\"2\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/189785531_1_261x203_everest-stabilizer-single-booster-chennai_rev002.jpg\",\"1\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/189785531_1_94x72_everest-stabilizer-single-booster-chennai_rev002.jpg\",\"9\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/189785531_1_144x108_everest-stabilizer-single-booster-chennai_rev002.jpg\"},{\"3\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/189785531_2_1000x700_everest-stabilizer-single-booster-upload-photos_rev002.jpg\",\"0\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/189785531_2_644x461_everest-stabilizer-single-booster-upload-photos_rev002.jpg\",\"2\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/189785531_2_261x203_everest-stabilizer-single-booster-upload-photos_rev002.jpg\",\"1\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/189785531_2_94x72_everest-stabilizer-single-booster-upload-photos_rev002.jpg\",\"9\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/189785531_2_144x108_everest-stabilizer-single-booster-upload-photos_rev002.jpg\"}],\"user_online_status\":1,\"user_online_message\":\"Online\",\"chat_options\":1,\"page_title\":\"everest stabilizer single booster Chennai  � OLX.in\",\"user_active_ads_count\":3,\"views_count\":\"1125\",\"phone\":\"+918680091998\",\"city_id\":\"59162\",\"badges\":[{\"type\":\"phone_confirmed\",\"visibility\":1,\"title\":\"Phone Number Verified Badge\",\"text\":\"This seller's mobile number has been verified via OTP\"},{\"type\":\"email_confirmed\",\"visibility\":0,\"title\":\"Email Verified Badge\",\"text\":\"This seller�s email has been verified\"},{\"type\":\"facebook_confirmed\",\"visibility\":0,\"title\":\"Facebook Profile Verified Badge\",\"text\":\"This seller�s FB profile has been verified\"}],\"user_pic_url\":\"\",\"total_comments\":0,\"community_id\":0,\"is_community_ad\":0,\"user_community_member_since\":\"\",\"user_member_since\":\"\",\"block_user_card\":0},{\"id\":\"1010078561\",\"url\":\"https:\\/\\/www.olx.in\\/item\\/printer-black-color-ID16mbzH.html\",\"preview_url\":\"https:\\/\\/www.olx.in\\/i2\\/item\\/printer-black-color-ID16mbzH.html?json=1\",\"title\":\"printer black color\",\"created\":\"04:09 pm\",\"age\":0,\"description\":\"Brand Turbo ink refill kit\\r\\nCanon PG 745 cartridge\\r\\nColor Type Black\\r\\nModel Name Turbo refill kit for Canon pg 745\\r\\nModel Series for Canon PG 745 cartridge\\r\\nColor black\\r\\nVolume - 60 ml\\r\\nNew black color\",\"highlighted\":0,\"urgent\":0,\"topAd\":0,\"category_id\":1509,\"params\":[],\"subtitle\":[],\"business\":0,\"hide_user_ads_button\":0,\"phone_confirmed\":1,\"hide_user_phone\":0,\"has_phone\":1,\"has_email\":1,\"map_zoom\":\"11\",\"map_lat\":\"22.57487000\",\"map_lon\":\"88.37660000\",\"map_radius\":1,\"map_show_detailed\":false,\"city_label\":\"Kolkata\",\"person\":\"palash mandal\",\"user_label\":\"palash mandal\",\"user_ads_id\":\"6HpbR\",\"user_id\":\"6HpbR\",\"numeric_user_id\":\"99002955\",\"user_ads_url\":\"https:\\/\\/www.olx.in\\/i2\\/all-results\\/user\\/6HpbR\\/?json=1&search%5Buser_id%5D=99002955\",\"list_label\":\"? 280\",\"list_label_ad\":\"? 280\",\"photos\":[{\"3\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414291_2_1000x700_printer-black-color-upload-photos_rev001.jpg\",\"0\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414291_2_644x461_printer-black-color-upload-photos_rev001.jpg\",\"2\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414291_2_261x203_printer-black-color-upload-photos_rev001.jpg\",\"1\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414291_2_94x72_printer-black-color-upload-photos_rev001.jpg\",\"9\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414291_2_144x108_printer-black-color-upload-photos_rev001.jpg\"},{\"3\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414291_3_1000x700_printer-black-color-printers_rev001.jpg\",\"0\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414291_3_644x461_printer-black-color-printers_rev001.jpg\",\"2\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414291_3_261x203_printer-black-color-printers_rev001.jpg\",\"1\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414291_3_94x72_printer-black-color-printers_rev001.jpg\",\"9\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414291_3_144x108_printer-black-color-printers_rev001.jpg\"},{\"3\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414291_4_1000x700_printer-black-color-electronics-appliances_rev001.jpg\",\"0\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414291_4_644x461_printer-black-color-electronics-appliances_rev001.jpg\",\"2\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414291_4_261x203_printer-black-color-electronics-appliances_rev001.jpg\",\"1\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414291_4_94x72_printer-black-color-electronics-appliances_rev001.jpg\",\"9\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414291_4_144x108_printer-black-color-electronics-appliances_rev001.jpg\"},{\"3\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414291_5_1000x700_printer-black-color-west-bengal_rev001.jpg\",\"0\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414291_5_644x461_printer-black-color-west-bengal_rev001.jpg\",\"2\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414291_5_261x203_printer-black-color-west-bengal_rev001.jpg\",\"1\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414291_5_94x72_printer-black-color-west-bengal_rev001.jpg\",\"9\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414291_5_144x108_printer-black-color-west-bengal_rev001.jpg\"},{\"3\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414291_1_1000x700_printer-black-color-kolkata_rev001.jpg\",\"0\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414291_1_644x461_printer-black-color-kolkata_rev001.jpg\",\"2\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414291_1_261x203_printer-black-color-kolkata_rev001.jpg\",\"1\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414291_1_94x72_printer-black-color-kolkata_rev001.jpg\",\"9\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414291_1_144x108_printer-black-color-kolkata_rev001.jpg\"}],\"user_online_status\":1,\"user_online_message\":\"Online\",\"chat_options\":1,\"page_title\":\"printer black color Kolkata � OLX.in\",\"user_active_ads_count\":1,\"views_count\":0,\"phone\":\"+919874591210\",\"city_id\":\"157275\",\"badges\":[{\"type\":\"phone_confirmed\",\"visibility\":1,\"title\":\"Phone Number Verified Badge\",\"text\":\"This seller's mobile number has been verified via OTP\"},{\"type\":\"email_confirmed\",\"visibility\":0,\"title\":\"Email Verified Badge\",\"text\":\"This seller�s email has been verified\"},{\"type\":\"facebook_confirmed\",\"visibility\":0,\"title\":\"Facebook Profile Verified Badge\",\"text\":\"This seller�s FB profile has been verified\"}],\"user_pic_url\":\"\",\"total_comments\":0,\"community_id\":0,\"is_community_ad\":0,\"user_community_member_since\":\"\",\"user_member_since\":\"\",\"block_user_card\":0},{\"id\":\"1010078403\",\"url\":\"https:\\/\\/www.olx.in\\/item\\/ifb-20sc2-20-litre-1200-watt-convection-microwave-oven-metallic-silve-ID16mbx9.html\",\"preview_url\":\"https:\\/\\/www.olx.in\\/i2\\/item\\/ifb-20sc2-20-litre-1200-watt-convection-microwave-oven-metallic-silve-ID16mbx9.html?json=1\",\"title\":\"IFB 20SC2 20-Litre 1200-Watt Convection Microwave Oven (Metallic Silve\",\"created\":\"04:09 pm\",\"age\":0,\"description\":\"This is New product, it was gifted by my company on 25th June, and i didn't use it, buy Rs 1000 less from the market price.\",\"highlighted\":0,\"urgent\":0,\"topAd\":0,\"category_id\":1417,\"params\":[],\"subtitle\":[],\"business\":0,\"hide_user_ads_button\":0,\"phone_confirmed\":1,\"hide_user_phone\":0,\"has_phone\":1,\"has_email\":1,\"map_zoom\":\"13\",\"map_lat\":\"12.88644000\",\"map_lon\":\"77.59668000\",\"map_radius\":1,\"map_show_detailed\":false,\"city_label\":\"Bengaluru, Arakere\",\"person\":\"Nasim Alam\",\"user_label\":\"Nasim Alam\",\"user_ads_id\":\"6HpaD\",\"user_id\":\"6HpaD\",\"numeric_user_id\":\"99002879\",\"user_ads_url\":\"https:\\/\\/www.olx.in\\/i2\\/all-results\\/user\\/6HpaD\\/?json=1&search%5Buser_id%5D=99002879\",\"list_label\":\"? 8,000\",\"list_label_ad\":\"? 8,000\",\"photos\":[{\"3\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/210414629_1_1000x700_ifb-20sc2-20-litre-1200-watt-convection-microwave-oven-metallic-silve-bengaluru.jpg\",\"0\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/210414629_1_644x461_ifb-20sc2-20-litre-1200-watt-convection-microwave-oven-metallic-silve-bengaluru.jpg\",\"2\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/210414629_1_261x203_ifb-20sc2-20-litre-1200-watt-convection-microwave-oven-metallic-silve-bengaluru.jpg\",\"1\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/210414629_1_94x72_ifb-20sc2-20-litre-1200-watt-convection-microwave-oven-metallic-silve-bengaluru.jpg\",\"9\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/210414629_1_144x108_ifb-20sc2-20-litre-1200-watt-convection-microwave-oven-metallic-silve-bengaluru.jpg\"},{\"3\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/210414629_2_1000x700_ifb-20sc2-20-litre-1200-watt-convection-microwave-oven-metallic-silve-upload-photos.jpg\",\"0\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/210414629_2_644x461_ifb-20sc2-20-litre-1200-watt-convection-microwave-oven-metallic-silve-upload-photos.jpg\",\"2\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/210414629_2_261x203_ifb-20sc2-20-litre-1200-watt-convection-microwave-oven-metallic-silve-upload-photos.jpg\",\"1\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/210414629_2_94x72_ifb-20sc2-20-litre-1200-watt-convection-microwave-oven-metallic-silve-upload-photos.jpg\",\"9\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/210414629_2_144x108_ifb-20sc2-20-litre-1200-watt-convection-microwave-oven-metallic-silve-upload-photos.jpg\"},{\"3\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/210414629_3_1000x700_ifb-20sc2-20-litre-1200-watt-convection-microwave-oven-metallic-silve-kitchen-other-appliances.jpg\",\"0\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/210414629_3_644x461_ifb-20sc2-20-litre-1200-watt-convection-microwave-oven-metallic-silve-kitchen-other-appliances.jpg\",\"2\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/210414629_3_261x203_ifb-20sc2-20-litre-1200-watt-convection-microwave-oven-metallic-silve-kitchen-other-appliances.jpg\",\"1\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/210414629_3_94x72_ifb-20sc2-20-litre-1200-watt-convection-microwave-oven-metallic-silve-kitchen-other-appliances.jpg\",\"9\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/210414629_3_144x108_ifb-20sc2-20-litre-1200-watt-convection-microwave-oven-metallic-silve-kitchen-other-appliances.jpg\"},{\"3\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/210414629_4_1000x700_ifb-20sc2-20-litre-1200-watt-convection-microwave-oven-metallic-silve-electronics-appliances.jpg\",\"0\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/210414629_4_644x461_ifb-20sc2-20-litre-1200-watt-convection-microwave-oven-metallic-silve-electronics-appliances.jpg\",\"2\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/210414629_4_261x203_ifb-20sc2-20-litre-1200-watt-convection-microwave-oven-metallic-silve-electronics-appliances.jpg\",\"1\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/210414629_4_94x72_ifb-20sc2-20-litre-1200-watt-convection-microwave-oven-metallic-silve-electronics-appliances.jpg\",\"9\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/210414629_4_144x108_ifb-20sc2-20-litre-1200-watt-convection-microwave-oven-metallic-silve-electronics-appliances.jpg\"},{\"3\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/210414629_5_1000x700_ifb-20sc2-20-litre-1200-watt-convection-microwave-oven-metallic-silve-karnataka.jpg\",\"0\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/210414629_5_644x461_ifb-20sc2-20-litre-1200-watt-convection-microwave-oven-metallic-silve-karnataka.jpg\",\"2\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/210414629_5_261x203_ifb-20sc2-20-litre-1200-watt-convection-microwave-oven-metallic-silve-karnataka.jpg\",\"1\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/210414629_5_94x72_ifb-20sc2-20-litre-1200-watt-convection-microwave-oven-metallic-silve-karnataka.jpg\",\"9\":\"https:\\/\\/img02.olx.in\\/images_olxin\\/210414629_5_144x108_ifb-20sc2-20-litre-1200-watt-convection-microwave-oven-metallic-silve-karnataka.jpg\"}],\"user_online_status\":1,\"user_online_message\":\"Online\",\"chat_options\":1,\"page_title\":\"IFB 20SC2 20-Litre 1200-Watt Convection Microwave Oven (Metallic Silve Bengaluru  � OLX.in\",\"user_active_ads_count\":1,\"views_count\":\"1\",\"phone\":\"+918105971507\",\"city_id\":\"58803\",\"badges\":[{\"type\":\"phone_confirmed\",\"visibility\":1,\"title\":\"Phone Number Verified Badge\",\"text\":\"This seller's mobile number has been verified via OTP\"},{\"type\":\"email_confirmed\",\"visibility\":0,\"title\":\"Email Verified Badge\",\"text\":\"This seller�s email has been verified\"},{\"type\":\"facebook_confirmed\",\"visibility\":0,\"title\":\"Facebook Profile Verified Badge\",\"text\":\"This seller�s FB profile has been verified\"}],\"user_pic_url\":\"\",\"total_comments\":0,\"community_id\":0,\"is_community_ad\":0,\"user_community_member_since\":\"\",\"user_member_since\":\"\",\"block_user_card\":0},{\"id\":\"1000451779\",\"url\":\"https:\\/\\/www.olx.in\\/item\\/videocon-dvd-player-and-video-game-ID15HNd1.html\",\"preview_url\":\"https:\\/\\/www.olx.in\\/i2\\/item\\/videocon-dvd-player-and-video-game-ID15HNd1.html?json=1\",\"title\":\"Videocon Dvd player and video game .\",\"created\":\"04:09 pm\",\"age\":39,\"description\":\"Videocon dvd player and video game attach with 2 game remote in very good condition available\",\"highlighted\":0,\"urgent\":0,\"topAd\":0,\"category_id\":1525,\"params\":[],\"subtitle\":[],\"business\":0,\"hide_user_ads_button\":0,\"phone_confirmed\":0,\"hide_user_phone\":0,\"has_phone\":1,\"has_email\":1,\"map_zoom\":\"11\",\"map_lat\":\"23.36896000\",\"map_lon\":\"85.33606000\",\"map_radius\":1,\"map_show_detailed\":false,\"city_label\":\"Ranchi\",\"person\":\"Saurabh\",\"user_label\":\"Saurabh\",\"user_ads_id\":\"ODGP\",\"user_id\":\"ODGP\",\"numeric_user_id\":\"12068971\",\"user_ads_url\":\"https:\\/\\/www.olx.in\\/i2\\/all-results\\/user\\/ODGP\\/?json=1&search%5Buser_id%5D=12068971\",\"list_label\":\"? 1,700\",\"list_label_ad\":\"? 1,700\",\"photos\":[{\"3\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/199716809_1_1000x700_videocon-dvd-player-and-video-game-ranchi.jpg\",\"0\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/199716809_1_644x461_videocon-dvd-player-and-video-game-ranchi.jpg\",\"2\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/199716809_1_261x203_videocon-dvd-player-and-video-game-ranchi.jpg\",\"1\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/199716809_1_94x72_videocon-dvd-player-and-video-game-ranchi.jpg\",\"9\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/199716809_1_144x108_videocon-dvd-player-and-video-game-ranchi.jpg\"},{\"3\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/199716809_2_1000x700_videocon-dvd-player-and-video-game-upload-photos.jpg\",\"0\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/199716809_2_644x461_videocon-dvd-player-and-video-game-upload-photos.jpg\",\"2\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/199716809_2_261x203_videocon-dvd-player-and-video-game-upload-photos.jpg\",\"1\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/199716809_2_94x72_videocon-dvd-player-and-video-game-upload-photos.jpg\",\"9\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/199716809_2_144x108_videocon-dvd-player-and-video-game-upload-photos.jpg\"},{\"3\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/199716809_3_1000x700_videocon-dvd-player-and-video-game-video-audio.jpg\",\"0\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/199716809_3_644x461_videocon-dvd-player-and-video-game-video-audio.jpg\",\"2\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/199716809_3_261x203_videocon-dvd-player-and-video-game-video-audio.jpg\",\"1\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/199716809_3_94x72_videocon-dvd-player-and-video-game-video-audio.jpg\",\"9\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/199716809_3_144x108_videocon-dvd-player-and-video-game-video-audio.jpg\"},{\"3\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/199716809_4_1000x700_videocon-dvd-player-and-video-game-electronics-appliances.jpg\",\"0\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/199716809_4_644x461_videocon-dvd-player-and-video-game-electronics-appliances.jpg\",\"2\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/199716809_4_261x203_videocon-dvd-player-and-video-game-electronics-appliances.jpg\",\"1\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/199716809_4_94x72_videocon-dvd-player-and-video-game-electronics-appliances.jpg\",\"9\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/199716809_4_144x108_videocon-dvd-player-and-video-game-electronics-appliances.jpg\"}],\"user_online_status\":1,\"user_online_message\":\"Online\",\"chat_options\":1,\"page_title\":\"Videocon Dvd player and video game . Ranchi � OLX.in\",\"user_active_ads_count\":2,\"views_count\":\"409\",\"phone\":\"+918102304368\",\"city_id\":\"58797\",\"badges\":[{\"type\":\"phone_confirmed\",\"visibility\":0,\"title\":\"Phone Number Verified Badge\",\"text\":\"This seller's mobile number has been verified via OTP\"},{\"type\":\"email_confirmed\",\"visibility\":1,\"title\":\"Email Verified Badge\",\"text\":\"This seller�s email has been verified\"},{\"type\":\"facebook_confirmed\",\"visibility\":1,\"title\":\"Facebook Profile Verified Badge\",\"text\":\"This seller�s FB profile has been verified\"}],\"user_pic_url\":\"https:\\/\\/graph.facebook.com\\/v2.2\\/875536449167144\\/picture\\/?width=160&height=160\",\"total_comments\":0,\"community_id\":0,\"is_community_ad\":0,\"user_community_member_since\":\"\",\"user_member_since\":\"\",\"block_user_card\":0},{\"id\":\"1010078139\",\"url\":\"https:\\/\\/www.olx.in\\/item\\/d-link-wireless-data-modem-seald-pack-ID16mbsT.html\",\"preview_url\":\"https:\\/\\/www.olx.in\\/i2\\/item\\/d-link-wireless-data-modem-seald-pack-ID16mbsT.html?json=1\",\"title\":\"D link wireless data modem seald pack\",\"created\":\"04:08 pm\",\"age\":0,\"description\":\"D link wireless data modem \\nNew seald pack in best price\",\"highlighted\":0,\"urgent\":0,\"topAd\":0,\"category_id\":1507,\"params\":[],\"subtitle\":[],\"business\":0,\"hide_user_ads_button\":0,\"phone_confirmed\":1,\"hide_user_phone\":0,\"has_phone\":1,\"has_email\":1,\"map_zoom\":\"13\",\"map_lat\":\"11.01955000\",\"map_lon\":\"76.96994000\",\"map_radius\":1,\"map_show_detailed\":false,\"city_label\":\"Coimbatore, Gandhipuram\",\"person\":\"Sri V2 systems\",\"user_label\":\"Sri V2 systems\",\"user_ads_id\":\"6plAJ\",\"user_id\":\"6plAJ\",\"numeric_user_id\":\"94699217\",\"user_ads_url\":\"https:\\/\\/www.olx.in\\/i2\\/all-results\\/user\\/6plAJ\\/?json=1&search%5Buser_id%5D=94699217\",\"list_label\":\"? 799\",\"list_label_ad\":\"? 799\",\"photos\":[{\"3\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210413659_1_1000x700_d-link-wireless-data-modem-seald-pack-coimbatore.jpg\",\"0\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210413659_1_644x461_d-link-wireless-data-modem-seald-pack-coimbatore.jpg\",\"2\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210413659_1_261x203_d-link-wireless-data-modem-seald-pack-coimbatore.jpg\",\"1\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210413659_1_94x72_d-link-wireless-data-modem-seald-pack-coimbatore.jpg\",\"9\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210413659_1_144x108_d-link-wireless-data-modem-seald-pack-coimbatore.jpg\"},{\"3\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210413659_2_1000x700_d-link-wireless-data-modem-seald-pack-upload-photos.jpg\",\"0\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210413659_2_644x461_d-link-wireless-data-modem-seald-pack-upload-photos.jpg\",\"2\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210413659_2_261x203_d-link-wireless-data-modem-seald-pack-upload-photos.jpg\",\"1\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210413659_2_94x72_d-link-wireless-data-modem-seald-pack-upload-photos.jpg\",\"9\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210413659_2_144x108_d-link-wireless-data-modem-seald-pack-upload-photos.jpg\"}],\"user_online_status\":1,\"user_online_message\":\"Online\",\"chat_options\":1,\"page_title\":\"D link wireless data modem seald pack Coimbatore  � OLX.in\",\"user_active_ads_count\":3,\"views_count\":0,\"phone\":\"+919865919622\",\"city_id\":\"59164\",\"badges\":[{\"type\":\"phone_confirmed\",\"visibility\":1,\"title\":\"Phone Number Verified Badge\",\"text\":\"This seller's mobile number has been verified via OTP\"},{\"type\":\"email_confirmed\",\"visibility\":0,\"title\":\"Email Verified Badge\",\"text\":\"This seller�s email has been verified\"},{\"type\":\"facebook_confirmed\",\"visibility\":0,\"title\":\"Facebook Profile Verified Badge\",\"text\":\"This seller�s FB profile has been verified\"}],\"user_pic_url\":\"\",\"total_comments\":0,\"community_id\":0,\"is_community_ad\":0,\"user_community_member_since\":\"\",\"user_member_since\":\"\",\"block_user_card\":0},{\"id\":\"1010078409\",\"url\":\"https:\\/\\/www.olx.in\\/item\\/color-tv-of-20-inch-ID16mbxf.html\",\"preview_url\":\"https:\\/\\/www.olx.in\\/i2\\/item\\/color-tv-of-20-inch-ID16mbxf.html?json=1\",\"title\":\"Color tv of 20 inch\",\"created\":\"04:08 pm\",\"age\":0,\"description\":\"Company svl, size 20 inch\",\"highlighted\":0,\"urgent\":0,\"topAd\":0,\"category_id\":1523,\"params\":[],\"subtitle\":[],\"business\":0,\"hide_user_ads_button\":0,\"phone_confirmed\":1,\"hide_user_phone\":0,\"has_phone\":1,\"has_email\":1,\"map_zoom\":\"12\",\"map_lat\":\"26.71688000\",\"map_lon\":\"88.43179000\",\"map_radius\":1,\"map_show_detailed\":false,\"city_label\":\"Siliguri\",\"person\":\"Rajesh Bansal\",\"user_label\":\"Rajesh Bansal\",\"user_ads_id\":\"5lF41\",\"user_id\":\"5lF41\",\"numeric_user_id\":\"79044421\",\"user_ads_url\":\"https:\\/\\/www.olx.in\\/i2\\/all-results\\/user\\/5lF41\\/?json=1&search%5Buser_id%5D=79044421\",\"list_label\":\"? 5,000\",\"list_label_ad\":\"? 5,000\",\"photos\":[{\"3\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414837_1_1000x700_color-tv-of-20-inch-siligurimc.jpg\",\"0\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414837_1_644x461_color-tv-of-20-inch-siligurimc.jpg\",\"2\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414837_1_261x203_color-tv-of-20-inch-siligurimc.jpg\",\"1\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414837_1_94x72_color-tv-of-20-inch-siligurimc.jpg\",\"9\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414837_1_144x108_color-tv-of-20-inch-siligurimc.jpg\"}],\"user_online_status\":1,\"user_online_message\":\"Online\",\"chat_options\":1,\"page_title\":\"Color tv of 20 inch Siliguri � OLX.in\",\"user_active_ads_count\":2,\"views_count\":\"1\",\"phone\":\"+919832095209\",\"city_id\":\"221382\",\"badges\":[{\"type\":\"phone_confirmed\",\"visibility\":1,\"title\":\"Phone Number Verified Badge\",\"text\":\"This seller's mobile number has been verified via OTP\"},{\"type\":\"email_confirmed\",\"visibility\":0,\"title\":\"Email Verified Badge\",\"text\":\"This seller�s email has been verified\"},{\"type\":\"facebook_confirmed\",\"visibility\":0,\"title\":\"Facebook Profile Verified Badge\",\"text\":\"This seller�s FB profile has been verified\"}],\"user_pic_url\":\"\",\"total_comments\":0,\"community_id\":0,\"is_community_ad\":0,\"user_community_member_since\":\"\",\"user_member_since\":\"\",\"block_user_card\":0},{\"id\":\"1010078143\",\"url\":\"https:\\/\\/www.olx.in\\/item\\/sony-handycam-hdr-cx150-parsi-owned-ID16mbsX.html\",\"preview_url\":\"https:\\/\\/www.olx.in\\/i2\\/item\\/sony-handycam-hdr-cx150-parsi-owned-ID16mbsX.html?json=1\",\"title\":\"Sony Handycam HDR CX150 Parsi Owned\",\"created\":\"04:08 pm\",\"age\":0,\"description\":\"Parsi Owned Sparingly Used Sony Handicap with all Accesories, Bag, Filter 16gm Memory Card.\\r\\n\\r\\nTop class condition as good as new.\",\"highlighted\":0,\"urgent\":0,\"topAd\":0,\"category_id\":1517,\"params\":[],\"subtitle\":[],\"business\":0,\"hide_user_ads_button\":0,\"phone_confirmed\":1,\"hide_user_phone\":0,\"has_phone\":1,\"has_email\":1,\"map_zoom\":\"13\",\"map_lat\":\"18.99541000\",\"map_lon\":\"72.83440000\",\"map_radius\":1,\"map_show_detailed\":false,\"city_label\":\"Mumbai, Parel\",\"person\":\"Sheriyar R Dotivala\",\"user_label\":\"Sheriyar R Dotivala\",\"user_ads_id\":\"6EPUl\",\"user_id\":\"6EPUl\",\"numeric_user_id\":\"98390673\",\"user_ads_url\":\"https:\\/\\/www.olx.in\\/i2\\/all-results\\/user\\/6EPUl\\/?json=1&search%5Buser_id%5D=98390673\",\"list_label\":\"? 18,000\",\"list_label_ad\":\"? 18,000\",\"photos\":[{\"3\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210414655_1_1000x700_sony-handycam-hdr-cx150-parsi-owned-mumbai.jpg\",\"0\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210414655_1_644x461_sony-handycam-hdr-cx150-parsi-owned-mumbai.jpg\",\"2\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210414655_1_261x203_sony-handycam-hdr-cx150-parsi-owned-mumbai.jpg\",\"1\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210414655_1_94x72_sony-handycam-hdr-cx150-parsi-owned-mumbai.jpg\",\"9\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210414655_1_144x108_sony-handycam-hdr-cx150-parsi-owned-mumbai.jpg\"},{\"3\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210414655_2_1000x700_sony-handycam-hdr-cx150-parsi-owned-upload-photos.jpg\",\"0\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210414655_2_644x461_sony-handycam-hdr-cx150-parsi-owned-upload-photos.jpg\",\"2\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210414655_2_261x203_sony-handycam-hdr-cx150-parsi-owned-upload-photos.jpg\",\"1\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210414655_2_94x72_sony-handycam-hdr-cx150-parsi-owned-upload-photos.jpg\",\"9\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210414655_2_144x108_sony-handycam-hdr-cx150-parsi-owned-upload-photos.jpg\"},{\"3\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210414655_3_1000x700_sony-handycam-hdr-cx150-parsi-owned-cameras.jpg\",\"0\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210414655_3_644x461_sony-handycam-hdr-cx150-parsi-owned-cameras.jpg\",\"2\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210414655_3_261x203_sony-handycam-hdr-cx150-parsi-owned-cameras.jpg\",\"1\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210414655_3_94x72_sony-handycam-hdr-cx150-parsi-owned-cameras.jpg\",\"9\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210414655_3_144x108_sony-handycam-hdr-cx150-parsi-owned-cameras.jpg\"},{\"3\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210414655_4_1000x700_sony-handycam-hdr-cx150-parsi-owned-electronics-appliances.jpg\",\"0\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210414655_4_644x461_sony-handycam-hdr-cx150-parsi-owned-electronics-appliances.jpg\",\"2\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210414655_4_261x203_sony-handycam-hdr-cx150-parsi-owned-electronics-appliances.jpg\",\"1\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210414655_4_94x72_sony-handycam-hdr-cx150-parsi-owned-electronics-appliances.jpg\",\"9\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210414655_4_144x108_sony-handycam-hdr-cx150-parsi-owned-electronics-appliances.jpg\"},{\"3\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210414655_5_1000x700_sony-handycam-hdr-cx150-parsi-owned-maharashtra.jpg\",\"0\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210414655_5_644x461_sony-handycam-hdr-cx150-parsi-owned-maharashtra.jpg\",\"2\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210414655_5_261x203_sony-handycam-hdr-cx150-parsi-owned-maharashtra.jpg\",\"1\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210414655_5_94x72_sony-handycam-hdr-cx150-parsi-owned-maharashtra.jpg\",\"9\":\"https:\\/\\/img05.olx.in\\/images_olxin\\/210414655_5_144x108_sony-handycam-hdr-cx150-parsi-owned-maharashtra.jpg\"}],\"user_online_status\":1,\"user_online_message\":\"Online\",\"chat_options\":1,\"page_title\":\"Sony Handycam HDR CX150 Parsi Owned Mumbai  � OLX.in\",\"user_active_ads_count\":2,\"views_count\":\"1\",\"phone\":\"+919819311715\",\"city_id\":\"58997\",\"badges\":[{\"type\":\"phone_confirmed\",\"visibility\":1,\"title\":\"Phone Number Verified Badge\",\"text\":\"This seller's mobile number has been verified via OTP\"},{\"type\":\"email_confirmed\",\"visibility\":0,\"title\":\"Email Verified Badge\",\"text\":\"This seller�s email has been verified\"},{\"type\":\"facebook_confirmed\",\"visibility\":0,\"title\":\"Facebook Profile Verified Badge\",\"text\":\"This seller�s FB profile has been verified\"}],\"user_pic_url\":\"\",\"total_comments\":0,\"community_id\":0,\"is_community_ad\":0,\"user_community_member_since\":\"\",\"user_member_since\":\"\",\"block_user_card\":0},{\"id\":\"1010078335\",\"url\":\"https:\\/\\/www.olx.in\\/item\\/water-filter-aqua-guard-classic-ID16mbv3.html\",\"preview_url\":\"https:\\/\\/www.olx.in\\/i2\\/item\\/water-filter-aqua-guard-classic-ID16mbv3.html?json=1\",\"title\":\"Water filter Aqua guard classic\",\"created\":\"04:08 pm\",\"age\":0,\"description\":\"Water filter Aqua guard classic\",\"highlighted\":0,\"urgent\":0,\"topAd\":0,\"category_id\":1417,\"params\":[],\"subtitle\":[],\"business\":0,\"hide_user_ads_button\":0,\"phone_confirmed\":1,\"hide_user_phone\":0,\"has_phone\":1,\"has_email\":1,\"map_zoom\":\"13\",\"map_lat\":\"15.60113000\",\"map_lon\":\"73.81377000\",\"map_radius\":1,\"map_show_detailed\":false,\"city_label\":\"Mapusa\",\"person\":\"Rita\",\"user_label\":\"Rita\",\"user_ads_id\":\"1aLq5\",\"user_id\":\"1aLq5\",\"numeric_user_id\":\"17341901\",\"user_ads_url\":\"https:\\/\\/www.olx.in\\/i2\\/all-results\\/user\\/1aLq5\\/?json=1&search%5Buser_id%5D=17341901\",\"list_label\":\"? 6,000\",\"list_label_ad\":\"? 6,000\",\"photos\":[{\"3\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414791_1_1000x700_water-filter-aqua-guard-classic-mapusa_rev001.jpg\",\"0\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414791_1_644x461_water-filter-aqua-guard-classic-mapusa_rev001.jpg\",\"2\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414791_1_261x203_water-filter-aqua-guard-classic-mapusa_rev001.jpg\",\"1\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414791_1_94x72_water-filter-aqua-guard-classic-mapusa_rev001.jpg\",\"9\":\"https:\\/\\/img03.olx.in\\/images_olxin\\/210414791_1_144x108_water-filter-aqua-guard-classic-mapusa_rev001.jpg\"}],\"user_online_status\":1,\"user_online_message\":\"Online\",\"chat_options\":1,\"page_title\":\"Water filter Aqua guard classic Mapusa � OLX.in\",\"user_active_ads_count\":4,\"views_count\":0,\"phone\":\"+918793523135\",\"city_id\":\"393607\",\"badges\":[{\"type\":\"phone_confirmed\",\"visibility\":1,\"title\":\"Phone Number Verified Badge\",\"text\":\"This seller's mobile number has been verified via OTP\"},{\"type\":\"email_confirmed\",\"visibility\":1,\"title\":\"Email Verified Badge\",\"text\":\"This seller�s email has been verified\"},{\"type\":\"facebook_confirmed\",\"visibility\":0,\"title\":\"Facebook Profile Verified Badge\",\"text\":\"This seller�s FB profile has been verified\"}],\"user_pic_url\":\"\",\"total_comments\":0,\"community_id\":0,\"is_community_ad\":0,\"user_community_member_since\":\"\",\"user_member_since\":\"\",\"block_user_card\":0},{\"id\":\"1010078429\",\"url\":\"https:\\/\\/www.olx.in\\/item\\/steel-bartan-stand-ID16mbxz.html\",\"preview_url\":\"https:\\/\\/www.olx.in\\/i2\\/item\\/steel-bartan-stand-ID16mbxz.html?json=1\",\"title\":\"Steel Bartan stand\",\"created\":\"04:08 pm\",\"age\":0,\"description\":\"Steel made \\nStrong \\nBrand new \\nLarge size\",\"highlighted\":0,\"urgent\":0,\"topAd\":0,\"category_id\":1417,\"params\":[],\"subtitle\":[],\"business\":0,\"hide_user_ads_button\":0,\"phone_confirmed\":1,\"hide_user_phone\":0,\"has_phone\":1,\"has_email\":1,\"map_zoom\":\"13\",\"map_lat\":\"28.69199000\",\"map_lon\":\"77.19866000\",\"map_radius\":1,\"map_show_detailed\":false,\"city_label\":\"Delhi, Old Gupta Colony\",\"person\":\"Rajan\",\"user_label\":\"Rajan\",\"user_ads_id\":\"6CfmX\",\"user_id\":\"6CfmX\",\"numeric_user_id\":\"97773563\",\"user_ads_url\":\"https:\\/\\/www.olx.in\\/i2\\/all-results\\/user\\/6CfmX\\/?json=1&search%5Buser_id%5D=97773563\",\"list_label\":\"? 2,200\",\"list_label_ad\":\"? 2,200\",\"photos\":[{\"3\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210415137_2_1000x700_steel-bartan-stand-upload-photos_rev003.jpg\",\"0\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210415137_2_644x461_steel-bartan-stand-upload-photos_rev003.jpg\",\"2\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210415137_2_261x203_steel-bartan-stand-upload-photos_rev003.jpg\",\"1\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210415137_2_94x72_steel-bartan-stand-upload-photos_rev003.jpg\",\"9\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210415137_2_144x108_steel-bartan-stand-upload-photos_rev003.jpg\"}],\"user_online_status\":1,\"user_online_message\":\"Online\",\"chat_options\":1,\"page_title\":\"Steel Bartan stand Delhi  � OLX.in\",\"user_active_ads_count\":10,\"views_count\":0,\"phone\":\"+919927009278\",\"city_id\":\"58659\",\"badges\":[{\"type\":\"phone_confirmed\",\"visibility\":1,\"title\":\"Phone Number Verified Badge\",\"text\":\"This seller's mobile number has been verified via OTP\"},{\"type\":\"email_confirmed\",\"visibility\":1,\"title\":\"Email Verified Badge\",\"text\":\"This seller�s email has been verified\"},{\"type\":\"facebook_confirmed\",\"visibility\":0,\"title\":\"Facebook Profile Verified Badge\",\"text\":\"This seller�s FB profile has been verified\"}],\"user_pic_url\":\"\",\"total_comments\":0,\"community_id\":0,\"is_community_ad\":0,\"user_community_member_since\":\"\",\"user_member_since\":\"\",\"block_user_card\":0},{\"id\":\"1010078373\",\"url\":\"https:\\/\\/www.olx.in\\/item\\/smily-aircoller-in-good-condition-2-year-old-ID16mbvF.html\",\"preview_url\":\"https:\\/\\/www.olx.in\\/i2\\/item\\/smily-aircoller-in-good-condition-2-year-old-ID16mbvF.html?json=1\",\"title\":\"Smily aircoller in good condition 2 year old\",\"created\":\"04:08 pm\",\"age\":0,\"description\":\"I want to sell my smily aircooler in very good condition its 2 year old and in good running condition and im selling this because i wantto upgrade it\",\"highlighted\":0,\"urgent\":0,\"topAd\":0,\"category_id\":1417,\"params\":[],\"subtitle\":[],\"business\":0,\"hide_user_ads_button\":0,\"phone_confirmed\":0,\"hide_user_phone\":0,\"has_phone\":1,\"has_email\":1,\"map_zoom\":\"11\",\"map_lat\":\"26.84453000\",\"map_lon\":\"80.94701000\",\"map_radius\":1,\"map_show_detailed\":false,\"city_label\":\"Lucknow\",\"person\":\"Hariom\",\"user_label\":\"Hariom\",\"user_ads_id\":\"6Hpal\",\"user_id\":\"6Hpal\",\"numeric_user_id\":\"99002861\",\"user_ads_url\":\"https:\\/\\/www.olx.in\\/i2\\/all-results\\/user\\/6Hpal\\/?json=1&search%5Buser_id%5D=99002861\",\"list_label\":\"? 1,800\",\"list_label_ad\":\"? 1,800\",\"photos\":[{\"3\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414347_1_1000x700_smily-aircoller-in-good-condition-2-year-old-lucknow.jpg\",\"0\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414347_1_644x461_smily-aircoller-in-good-condition-2-year-old-lucknow.jpg\",\"2\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414347_1_261x203_smily-aircoller-in-good-condition-2-year-old-lucknow.jpg\",\"1\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414347_1_94x72_smily-aircoller-in-good-condition-2-year-old-lucknow.jpg\",\"9\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414347_1_144x108_smily-aircoller-in-good-condition-2-year-old-lucknow.jpg\"},{\"3\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414347_2_1000x700_smily-aircoller-in-good-condition-2-year-old-upload-photos.jpg\",\"0\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414347_2_644x461_smily-aircoller-in-good-condition-2-year-old-upload-photos.jpg\",\"2\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414347_2_261x203_smily-aircoller-in-good-condition-2-year-old-upload-photos.jpg\",\"1\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414347_2_94x72_smily-aircoller-in-good-condition-2-year-old-upload-photos.jpg\",\"9\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414347_2_144x108_smily-aircoller-in-good-condition-2-year-old-upload-photos.jpg\"},{\"3\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414347_3_1000x700_smily-aircoller-in-good-condition-2-year-old-kitchen-other-appliances.jpg\",\"0\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414347_3_644x461_smily-aircoller-in-good-condition-2-year-old-kitchen-other-appliances.jpg\",\"2\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414347_3_261x203_smily-aircoller-in-good-condition-2-year-old-kitchen-other-appliances.jpg\",\"1\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414347_3_94x72_smily-aircoller-in-good-condition-2-year-old-kitchen-other-appliances.jpg\",\"9\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414347_3_144x108_smily-aircoller-in-good-condition-2-year-old-kitchen-other-appliances.jpg\"},{\"3\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414347_4_1000x700_smily-aircoller-in-good-condition-2-year-old-electronics-appliances.jpg\",\"0\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414347_4_644x461_smily-aircoller-in-good-condition-2-year-old-electronics-appliances.jpg\",\"2\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414347_4_261x203_smily-aircoller-in-good-condition-2-year-old-electronics-appliances.jpg\",\"1\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414347_4_94x72_smily-aircoller-in-good-condition-2-year-old-electronics-appliances.jpg\",\"9\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414347_4_144x108_smily-aircoller-in-good-condition-2-year-old-electronics-appliances.jpg\"},{\"3\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414347_5_1000x700_smily-aircoller-in-good-condition-2-year-old-uttar-pradesh.jpg\",\"0\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414347_5_644x461_smily-aircoller-in-good-condition-2-year-old-uttar-pradesh.jpg\",\"2\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414347_5_261x203_smily-aircoller-in-good-condition-2-year-old-uttar-pradesh.jpg\",\"1\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414347_5_94x72_smily-aircoller-in-good-condition-2-year-old-uttar-pradesh.jpg\",\"9\":\"https:\\/\\/img01.olx.in\\/images_olxin\\/210414347_5_144x108_smily-aircoller-in-good-condition-2-year-old-uttar-pradesh.jpg\"}],\"user_online_status\":0,\"user_online_message\":\"Away\",\"chat_options\":1,\"page_title\":\"Smily aircoller in good condition 2 year old Lucknow � OLX.in\",\"user_active_ads_count\":1,\"views_count\":0,\"phone\":\"+917499980990\",\"city_id\":\"59306\",\"badges\":[{\"type\":\"phone_confirmed\",\"visibility\":0,\"title\":\"Phone Number Verified Badge\",\"text\":\"This seller's mobile number has been verified via OTP\"},{\"type\":\"email_confirmed\",\"visibility\":1,\"title\":\"Email Verified Badge\",\"text\":\"This seller�s email has been verified\"},{\"type\":\"facebook_confirmed\",\"visibility\":0,\"title\":\"Facebook Profile Verified Badge\",\"text\":\"This seller�s FB profile has been verified\"}],\"user_pic_url\":\"\",\"total_comments\":0,\"community_id\":0,\"is_community_ad\":0,\"user_community_member_since\":\"\",\"user_member_since\":\"\",\"block_user_card\":0}]}";
    List<String> keys = Collections.singletonList("ads[]->photos[]->1");


    String json1 = "{\n" +
        "\tstatus: \"success\",\n" +
        "    data: [{\n" +
        "\t\ttrainDataFound: \"trainRunningDataFound\",\n" +
        "\t\tstartDate: \"22 Oct 2016\",\n" +
        "\t\tstartDayDiff: \"-3\",\n" +
        "\t\tdeparted: true,\n" +
        "\t\tcurStn: \"FKA\",\n" +
        "\t\tterminated: true,\n" +
        "\t\tidMsg: \"0\",\n" +
        "\t\tcncldFrmStn: \"null\",\n" +
        "\t\tcncldToStn: \"null\",\n" +
        "\t\ttotalJourney: \"10 hrs 55 min\",\n" +
        "\t\tlastUpdated: \"23 Oct 2016 1:25\",\n" +
        "\t\tstations: [{\n" +
        "\t\t\tstnCode: \"DLI\",\n" +
        "\t\t\tidTrnSch: \"3799283\",\n" +
        "\t\t\tactArr: \"00:00\",\n" +
        "\t\t\tactDep: \"14:20\",\n" +
        "\t\t\tdayCnt: 0,\n" +
        "\t\t\tschArrTime: \"00:00\",\n" +
        "\t\t\tschDepTime: \"14:20\",\n" +
        "\t\t\tschDayCnt: 0,\n" +
        "\t\t\tdelayArr: 0,\n" +
        "\t\t\tdelayDep: 0,\n" +
        "\t\t\tarr: false,\n" +
        "\t\t\tdep: true,\n" +
        "\t\t\tdistance: 0,\n" +
        "\t\t\tjourneyDate: \"22 Oct 2016\",\n" +
        "\t\t\tactArrDate: \"22 Oct 2016\",\n" +
        "\t\t\tactDepDate: \"22 Oct 2016\",\n" +
        "\t\t\tdayDiff: \"-3\",\n" +
        "\t\t\tstoppingStn: true,\n" +
        "\t\t\tdvrtdStn: false,\n" +
        "\t\t\ttravelled: true,\n" +
        "\t\t\tupdWaitngArr: false,\n" +
        "\t\t\tupdWaitngDep: false,\n" +
        "\t\t\tpfNo: 0\n" +
        "\t\t}, {\n" +
        "\t\t\tstnCode: \"SZM\",\n" +
        "\t\t\tidTrnSch: \"3799284\",\n" +
        "\t\t\tactArr: \"14:35\",\n" +
        "\t\t\tactDep: \"14:37\",\n" +
        "\t\t\tdayCnt: 0,\n" +
        "\t\t\tschArrTime: \"14:31\",\n" +
        "\t\t\tschDepTime: \"14:33\",\n" +
        "\t\t\tschDayCnt: 0,\n" +
        "\t\t\tdelayArr: 4,\n" +
        "\t\t\tdelayDep: 4,\n" +
        "\t\t\tarr: true,\n" +
        "\t\t\tdep: true,\n" +
        "\t\t\tdistance: 2,\n" +
        "\t\t\tjourneyDate: \"22 Oct 2016\",\n" +
        "\t\t\tactArrDate: \"22 Oct 2016\",\n" +
        "\t\t\tactDepDate: \"22 Oct 2016\",\n" +
        "\t\t\tdayDiff: \"-3\",\n" +
        "\t\t\tstoppingStn: true,\n" +
        "\t\t\tdvrtdStn: false,\n" +
        "\t\t\ttravelled: true,\n" +
        "\t\t\tupdWaitngArr: false,\n" +
        "\t\t\tupdWaitngDep: false,\n" +
        "\t\t\tpfNo: 0\n" +
        "\t\t}, {\n" +
        "\t\t\tstnCode: \"NUR\",\n" +
        "\t\t\tidTrnSch: \"3799285\",\n" +
        "\t\t\tactArr: \"15:03\",\n" +
        "\t\t\tactDep: \"15:05\",\n" +
        "\t\t\tdayCnt: 0,\n" +
        "\t\t\tschArrTime: \"14:54\",\n" +
        "\t\t\tschDepTime: \"14:56\",\n" +
        "\t\t\tschDayCnt: 0,\n" +
        "\t\t\tdelayArr: 9,\n" +
        "\t\t\tdelayDep: 9,\n" +
        "\t\t\tarr: true,\n" +
        "\t\t\tdep: true,\n" +
        "\t\t\tdistance: 25,\n" +
        "\t\t\tjourneyDate: \"22 Oct 2016\",\n" +
        "\t\t\tactArrDate: \"22 Oct 2016\",\n" +
        "\t\t\tactDepDate: \"22 Oct 2016\",\n" +
        "\t\t\tdayDiff: \"-3\",\n" +
        "\t\t\tstoppingStn: true,\n" +
        "\t\t\tdvrtdStn: false,\n" +
        "\t\t\ttravelled: true,\n" +
        "\t\t\tupdWaitngArr: false,\n" +
        "\t\t\tupdWaitngDep: false,\n" +
        "\t\t\tpfNo: 0\n" +
        "\t\t}, {\n" +
        "\t\t\tstnCode: \"SNP\",\n" +
        "\t\t\tidTrnSch: \"3799286\",\n" +
        "\t\t\tactArr: \"15:20\",\n" +
        "\t\t\tactDep: \"15:22\",\n" +
        "\t\t\tdayCnt: 0,\n" +
        "\t\t\tschArrTime: \"15:16\",\n" +
        "\t\t\tschDepTime: \"15:18\",\n" +
        "\t\t\tschDayCnt: 0,\n" +
        "\t\t\tdelayArr: 4,\n" +
        "\t\t\tdelayDep: 4,\n" +
        "\t\t\tarr: true,\n" +
        "\t\t\tdep: true,\n" +
        "\t\t\tdistance: 43,\n" +
        "\t\t\tjourneyDate: \"22 Oct 2016\",\n" +
        "\t\t\tactArrDate: \"22 Oct 2016\",\n" +
        "\t\t\tactDepDate: \"22 Oct 2016\",\n" +
        "\t\t\tdayDiff: \"-3\",\n" +
        "\t\t\tstoppingStn: true,\n" +
        "\t\t\tdvrtdStn: false,\n" +
        "\t\t\ttravelled: true,\n" +
        "\t\t\tupdWaitngArr: false,\n" +
        "\t\t\tupdWaitngDep: false,\n" +
        "\t\t\tpfNo: 0\n" +
        "\t\t}, {\n" +
        "\t\t\tstnCode: \"GNU\",\n" +
        "\t\t\tidTrnSch: \"3799287\",\n" +
        "\t\t\tactArr: \"15:42\",\n" +
        "\t\t\tactDep: \"15:57\",\n" +
        "\t\t\tdayCnt: 0,\n" +
        "\t\t\tschArrTime: \"15:35\",\n" +
        "\t\t\tschDepTime: \"15:37\",\n" +
        "\t\t\tschDayCnt: 0,\n" +
        "\t\t\tdelayArr: 7,\n" +
        "\t\t\tdelayDep: 20,\n" +
        "\t\t\tarr: true,\n" +
        "\t\t\tdep: true,\n" +
        "\t\t\tdistance: 59,\n" +
        "\t\t\tjourneyDate: \"22 Oct 2016\",\n" +
        "\t\t\tactArrDate: \"22 Oct 2016\",\n" +
        "\t\t\tactDepDate: \"22 Oct 2016\",\n" +
        "\t\t\tdayDiff: \"-3\",\n" +
        "\t\t\tstoppingStn: true,\n" +
        "\t\t\tdvrtdStn: false,\n" +
        "\t\t\ttravelled: true,\n" +
        "\t\t\tupdWaitngArr: false,\n" +
        "\t\t\tupdWaitngDep: false,\n" +
        "\t\t\tpfNo: 0\n" +
        "\t\t}, {\n" +
        "\t\t\tstnCode: \"SMK\",\n" +
        "\t\t\tidTrnSch: \"3799288\",\n" +
        "\t\t\tactArr: \"16:08\",\n" +
        "\t\t\tactDep: \"16:10\",\n" +
        "\t\t\tdayCnt: 0,\n" +
        "\t\t\tschArrTime: \"16:03\",\n" +
        "\t\t\tschDepTime: \"16:05\",\n" +
        "\t\t\tschDayCnt: 0,\n" +
        "\t\t\tdelayArr: 5,\n" +
        "\t\t\tdelayDep: 5,\n" +
        "\t\t\tarr: true,\n" +
        "\t\t\tdep: true,\n" +
        "\t\t\tdistance: 71,\n" +
        "\t\t\tjourneyDate: \"22 Oct 2016\",\n" +
        "\t\t\tactArrDate: \"22 Oct 2016\",\n" +
        "\t\t\tactDepDate: \"22 Oct 2016\",\n" +
        "\t\t\tdayDiff: \"-3\",\n" +
        "\t\t\tstoppingStn: true,\n" +
        "\t\t\tdvrtdStn: false,\n" +
        "\t\t\ttravelled: true,\n" +
        "\t\t\tupdWaitngArr: false,\n" +
        "\t\t\tupdWaitngDep: false,\n" +
        "\t\t\tpfNo: 0\n" +
        "\t\t}, {\n" +
        "\t\t\tstnCode: \"PNP\",\n" +
        "\t\t\tidTrnSch: \"3799289\",\n" +
        "\t\t\tactArr: \"16:23\",\n" +
        "\t\t\tactDep: \"16:25\",\n" +
        "\t\t\tdayCnt: 0,\n" +
        "\t\t\tschArrTime: \"16:23\",\n" +
        "\t\t\tschDepTime: \"16:25\",\n" +
        "\t\t\tschDayCnt: 0,\n" +
        "\t\t\tdelayArr: 0,\n" +
        "\t\t\tdelayDep: 0,\n" +
        "\t\t\tarr: true,\n" +
        "\t\t\tdep: true,\n" +
        "\t\t\tdistance: 88,\n" +
        "\t\t\tjourneyDate: \"22 Oct 2016\",\n" +
        "\t\t\tactArrDate: \"22 Oct 2016\",\n" +
        "\t\t\tactDepDate: \"22 Oct 2016\",\n" +
        "\t\t\tdayDiff: \"-3\",\n" +
        "\t\t\tstoppingStn: true,\n" +
        "\t\t\tdvrtdStn: false,\n" +
        "\t\t\ttravelled: true,\n" +
        "\t\t\tupdWaitngArr: false,\n" +
        "\t\t\tupdWaitngDep: false,\n" +
        "\t\t\tpfNo: 0\n" +
        "\t\t}, {\n" +
        "\t\t\tstnCode: \"GRA\",\n" +
        "\t\t\tidTrnSch: \"3799290\",\n" +
        "\t\t\tactArr: \"16:39\",\n" +
        "\t\t\tactDep: \"16:41\",\n" +
        "\t\t\tdayCnt: 0,\n" +
        "\t\t\tschArrTime: \"16:37\",\n" +
        "\t\t\tschDepTime: \"16:39\",\n" +
        "\t\t\tschDayCnt: 0,\n" +
        "\t\t\tdelayArr: 2,\n" +
        "\t\t\tdelayDep: 2,\n" +
        "\t\t\tarr: true,\n" +
        "\t\t\tdep: true,\n" +
        "\t\t\tdistance: 105,\n" +
        "\t\t\tjourneyDate: \"22 Oct 2016\",\n" +
        "\t\t\tactArrDate: \"22 Oct 2016\",\n" +
        "\t\t\tactDepDate: \"22 Oct 2016\",\n" +
        "\t\t\tdayDiff: \"-3\",\n" +
        "\t\t\tstoppingStn: true,\n" +
        "\t\t\tdvrtdStn: false,\n" +
        "\t\t\ttravelled: true,\n" +
        "\t\t\tupdWaitngArr: false,\n" +
        "\t\t\tupdWaitngDep: false,\n" +
        "\t\t\tpfNo: 0\n" +
        "\t\t}, {\n" +
        "\t\t\tstnCode: \"KUN\",\n" +
        "\t\t\tidTrnSch: \"3799291\",\n" +
        "\t\t\tactArr: \"16:55\",\n" +
        "\t\t\tactDep: \"16:58\",\n" +
        "\t\t\tdayCnt: 0,\n" +
        "\t\t\tschArrTime: \"16:54\",\n" +
        "\t\t\tschDepTime: \"16:56\",\n" +
        "\t\t\tschDayCnt: 0,\n" +
        "\t\t\tdelayArr: 1,\n" +
        "\t\t\tdelayDep: 2,\n" +
        "\t\t\tarr: true,\n" +
        "\t\t\tdep: true,\n" +
        "\t\t\tdistance: 122,\n" +
        "\t\t\tjourneyDate: \"22 Oct 2016\",\n" +
        "\t\t\tactArrDate: \"22 Oct 2016\",\n" +
        "\t\t\tactDepDate: \"22 Oct 2016\",\n" +
        "\t\t\tdayDiff: \"-3\",\n" +
        "\t\t\tstoppingStn: true,\n" +
        "\t\t\tdvrtdStn: false,\n" +
        "\t\t\ttravelled: true,\n" +
        "\t\t\tupdWaitngArr: false,\n" +
        "\t\t\tupdWaitngDep: false,\n" +
        "\t\t\tpfNo: 0\n" +
        "\t\t}, {\n" +
        "\t\t\tstnCode: \"TRR\",\n" +
        "\t\t\tidTrnSch: \"3799292\",\n" +
        "\t\t\tactArr: \"17:10\",\n" +
        "\t\t\tactDep: \"17:12\",\n" +
        "\t\t\tdayCnt: 0,\n" +
        "\t\t\tschArrTime: \"17:08\",\n" +
        "\t\t\tschDepTime: \"17:10\",\n" +
        "\t\t\tschDayCnt: 0,\n" +
        "\t\t\tdelayArr: 2,\n" +
        "\t\t\tdelayDep: 2,\n" +
        "\t\t\tarr: true,\n" +
        "\t\t\tdep: true,\n" +
        "\t\t\tdistance: 134,\n" +
        "\t\t\tjourneyDate: \"22 Oct 2016\",\n" +
        "\t\t\tactArrDate: \"22 Oct 2016\",\n" +
        "\t\t\tactDepDate: \"22 Oct 2016\",\n" +
        "\t\t\tdayDiff: \"-3\",\n" +
        "\t\t\tstoppingStn: true,\n" +
        "\t\t\tdvrtdStn: false,\n" +
        "\t\t\ttravelled: true,\n" +
        "\t\t\tupdWaitngArr: false,\n" +
        "\t\t\tupdWaitngDep: false,\n" +
        "\t\t\tpfNo: 0\n" +
        "\t\t}, {\n" +
        "\t\t\tstnCode: \"KKDE\",\n" +
        "\t\t\tidTrnSch: \"3799293\",\n" +
        "\t\t\tactArr: \"17:29\",\n" +
        "\t\t\tactDep: \"17:31\",\n" +
        "\t\t\tdayCnt: 0,\n" +
        "\t\t\tschArrTime: \"17:29\",\n" +
        "\t\t\tschDepTime: \"17:31\",\n" +
        "\t\t\tschDayCnt: 0,\n" +
        "\t\t\tdelayArr: 0,\n" +
        "\t\t\tdelayDep: 0,\n" +
        "\t\t\tarr: true,\n" +
        "\t\t\tdep: true,\n" +
        "\t\t\tdistance: 155,\n" +
        "\t\t\tjourneyDate: \"22 Oct 2016\",\n" +
        "\t\t\tactArrDate: \"22 Oct 2016\",\n" +
        "\t\t\tactDepDate: \"22 Oct 2016\",\n" +
        "\t\t\tdayDiff: \"-3\",\n" +
        "\t\t\tstoppingStn: true,\n" +
        "\t\t\tdvrtdStn: false,\n" +
        "\t\t\ttravelled: true,\n" +
        "\t\t\tupdWaitngArr: false,\n" +
        "\t\t\tupdWaitngDep: false,\n" +
        "\t\t\tpfNo: 0\n" +
        "\t\t}, {\n" +
        "\t\t\tstnCode: \"UMB\",\n" +
        "\t\t\tidTrnSch: \"3799294\",\n" +
        "\t\t\tactArr: \"18:15\",\n" +
        "\t\t\tactDep: \"18:30\",\n" +
        "\t\t\tdayCnt: 0,\n" +
        "\t\t\tschArrTime: \"18:15\",\n" +
        "\t\t\tschDepTime: \"18:25\",\n" +
        "\t\t\tschDayCnt: 0,\n" +
        "\t\t\tdelayArr: 0,\n" +
        "\t\t\tdelayDep: 5,\n" +
        "\t\t\tarr: true,\n" +
        "\t\t\tdep: true,\n" +
        "\t\t\tdistance: 197,\n" +
        "\t\t\tjourneyDate: \"22 Oct 2016\",\n" +
        "\t\t\tactArrDate: \"22 Oct 2016\",\n" +
        "\t\t\tactDepDate: \"22 Oct 2016\",\n" +
        "\t\t\tdayDiff: \"-3\",\n" +
        "\t\t\tstoppingStn: true,\n" +
        "\t\t\tdvrtdStn: false,\n" +
        "\t\t\ttravelled: true,\n" +
        "\t\t\tupdWaitngArr: false,\n" +
        "\t\t\tupdWaitngDep: false,\n" +
        "\t\t\tpfNo: 0\n" +
        "\t\t}, {\n" +
        "\t\t\tstnCode: \"UBC\",\n" +
        "\t\t\tidTrnSch: \"3799295\",\n" +
        "\t\t\tactArr: \"18:47\",\n" +
        "\t\t\tactDep: \"18:49\",\n" +
        "\t\t\tdayCnt: 0,\n" +
        "\t\t\tschArrTime: \"18:36\",\n" +
        "\t\t\tschDepTime: \"18:38\",\n" +
        "\t\t\tschDayCnt: 0,\n" +
        "\t\t\tdelayArr: 11,\n" +
        "\t\t\tdelayDep: 11,\n" +
        "\t\t\tarr: true,\n" +
        "\t\t\tdep: true,\n" +
        "\t\t\tdistance: 204,\n" +
        "\t\t\tjourneyDate: \"22 Oct 2016\",\n" +
        "\t\t\tactArrDate: \"22 Oct 2016\",\n" +
        "\t\t\tactDepDate: \"22 Oct 2016\",\n" +
        "\t\t\tdayDiff: \"-3\",\n" +
        "\t\t\tstoppingStn: true,\n" +
        "\t\t\tdvrtdStn: false,\n" +
        "\t\t\ttravelled: true,\n" +
        "\t\t\tupdWaitngArr: false,\n" +
        "\t\t\tupdWaitngDep: false,\n" +
        "\t\t\tpfNo: 0\n" +
        "\t\t}, {\n" +
        "\t\t\tstnCode: \"RPJ\",\n" +
        "\t\t\tidTrnSch: \"3799296\",\n" +
        "\t\t\tactArr: \"19:06\",\n" +
        "\t\t\tactDep: \"19:08\",\n" +
        "\t\t\tdayCnt: 0,\n" +
        "\t\t\tschArrTime: \"18:55\",\n" +
        "\t\t\tschDepTime: \"18:57\",\n" +
        "\t\t\tschDayCnt: 0,\n" +
        "\t\t\tdelayArr: 11,\n" +
        "\t\t\tdelayDep: 11,\n" +
        "\t\t\tarr: true,\n" +
        "\t\t\tdep: true,\n" +
        "\t\t\tdistance: 225,\n" +
        "\t\t\tjourneyDate: \"22 Oct 2016\",\n" +
        "\t\t\tactArrDate: \"22 Oct 2016\",\n" +
        "\t\t\tactDepDate: \"22 Oct 2016\",\n" +
        "\t\t\tdayDiff: \"-3\",\n" +
        "\t\t\tstoppingStn: true,\n" +
        "\t\t\tdvrtdStn: false,\n" +
        "\t\t\ttravelled: true,\n" +
        "\t\t\tupdWaitngArr: false,\n" +
        "\t\t\tupdWaitngDep: false,\n" +
        "\t\t\tpfNo: 0\n" +
        "\t\t}, {\n" +
        "\t\t\tstnCode: \"PTA\",\n" +
        "\t\t\tidTrnSch: \"3799297\",\n" +
        "\t\t\tactArr: \"19:43\",\n" +
        "\t\t\tactDep: \"19:45\",\n" +
        "\t\t\tdayCnt: 0,\n" +
        "\t\t\tschArrTime: \"19:18\",\n" +
        "\t\t\tschDepTime: \"19:20\",\n" +
        "\t\t\tschDayCnt: 0,\n" +
        "\t\t\tdelayArr: 25,\n" +
        "\t\t\tdelayDep: 25,\n" +
        "\t\t\tarr: true,\n" +
        "\t\t\tdep: true,\n" +
        "\t\t\tdistance: 250,\n" +
        "\t\t\tjourneyDate: \"22 Oct 2016\",\n" +
        "\t\t\tactArrDate: \"22 Oct 2016\",\n" +
        "\t\t\tactDepDate: \"22 Oct 2016\",\n" +
        "\t\t\tdayDiff: \"-3\",\n" +
        "\t\t\tstoppingStn: true,\n" +
        "\t\t\tdvrtdStn: false,\n" +
        "\t\t\ttravelled: true,\n" +
        "\t\t\tupdWaitngArr: false,\n" +
        "\t\t\tupdWaitngDep: false,\n" +
        "\t\t\tpfNo: 0\n" +
        "\t\t}, {\n" +
        "\t\t\tstnCode: \"NBA\",\n" +
        "\t\t\tidTrnSch: \"3799298\",\n" +
        "\t\t\tactArr: \"20:10\",\n" +
        "\t\t\tactDep: \"20:12\",\n" +
        "\t\t\tdayCnt: 0,\n" +
        "\t\t\tschArrTime: \"19:44\",\n" +
        "\t\t\tschDepTime: \"19:46\",\n" +
        "\t\t\tschDayCnt: 0,\n" +
        "\t\t\tdelayArr: 26,\n" +
        "\t\t\tdelayDep: 26,\n" +
        "\t\t\tarr: true,\n" +
        "\t\t\tdep: true,\n" +
        "\t\t\tdistance: 275,\n" +
        "\t\t\tjourneyDate: \"22 Oct 2016\",\n" +
        "\t\t\tactArrDate: \"22 Oct 2016\",\n" +
        "\t\t\tactDepDate: \"22 Oct 2016\",\n" +
        "\t\t\tdayDiff: \"-3\",\n" +
        "\t\t\tstoppingStn: true,\n" +
        "\t\t\tdvrtdStn: false,\n" +
        "\t\t\ttravelled: true,\n" +
        "\t\t\tupdWaitngArr: false,\n" +
        "\t\t\tupdWaitngDep: false,\n" +
        "\t\t\tpfNo: 0\n" +
        "\t\t}, {\n" +
        "\t\t\tstnCode: \"DUI\",\n" +
        "\t\t\tidTrnSch: \"3799299\",\n" +
        "\t\t\tactArr: \"20:41\",\n" +
        "\t\t\tactDep: \"20:43\",\n" +
        "\t\t\tdayCnt: 0,\n" +
        "\t\t\tschArrTime: \"20:11\",\n" +
        "\t\t\tschDepTime: \"20:13\",\n" +
        "\t\t\tschDayCnt: 0,\n" +
        "\t\t\tdelayArr: 30,\n" +
        "\t\t\tdelayDep: 30,\n" +
        "\t\t\tarr: true,\n" +
        "\t\t\tdep: true,\n" +
        "\t\t\tdistance: 303,\n" +
        "\t\t\tjourneyDate: \"22 Oct 2016\",\n" +
        "\t\t\tactArrDate: \"22 Oct 2016\",\n" +
        "\t\t\tactDepDate: \"22 Oct 2016\",\n" +
        "\t\t\tdayDiff: \"-3\",\n" +
        "\t\t\tstoppingStn: true,\n" +
        "\t\t\tdvrtdStn: false,\n" +
        "\t\t\ttravelled: true,\n" +
        "\t\t\tupdWaitngArr: false,\n" +
        "\t\t\tupdWaitngDep: false,\n" +
        "\t\t\tpfNo: 0\n" +
        "\t\t}, {\n" +
        "\t\t\tstnCode: \"BNN\",\n" +
        "\t\t\tidTrnSch: \"3799300\",\n" +
        "\t\t\tactArr: \"21:09\",\n" +
        "\t\t\tactDep: \"21:11\",\n" +
        "\t\t\tdayCnt: 0,\n" +
        "\t\t\tschArrTime: \"20:46\",\n" +
        "\t\t\tschDepTime: \"20:47\",\n" +
        "\t\t\tschDayCnt: 0,\n" +
        "\t\t\tdelayArr: 23,\n" +
        "\t\t\tdelayDep: 24,\n" +
        "\t\t\tarr: true,\n" +
        "\t\t\tdep: true,\n" +
        "\t\t\tdistance: 333,\n" +
        "\t\t\tjourneyDate: \"22 Oct 2016\",\n" +
        "\t\t\tactArrDate: \"22 Oct 2016\",\n" +
        "\t\t\tactDepDate: \"22 Oct 2016\",\n" +
        "\t\t\tdayDiff: \"-3\",\n" +
        "\t\t\tstoppingStn: true,\n" +
        "\t\t\tdvrtdStn: false,\n" +
        "\t\t\ttravelled: true,\n" +
        "\t\t\tupdWaitngArr: false,\n" +
        "\t\t\tupdWaitngDep: false,\n" +
        "\t\t\tpfNo: 0\n" +
        "\t\t}, {\n" +
        "\t\t\tstnCode: \"TAPA\",\n" +
        "\t\t\tidTrnSch: \"3799301\",\n" +
        "\t\t\tactArr: \"21:27\",\n" +
        "\t\t\tactDep: \"21:29\",\n" +
        "\t\t\tdayCnt: 0,\n" +
        "\t\t\tschArrTime: \"21:04\",\n" +
        "\t\t\tschDepTime: \"21:06\",\n" +
        "\t\t\tschDayCnt: 0,\n" +
        "\t\t\tdelayArr: 23,\n" +
        "\t\t\tdelayDep: 23,\n" +
        "\t\t\tarr: true,\n" +
        "\t\t\tdep: true,\n" +
        "\t\t\tdistance: 352,\n" +
        "\t\t\tjourneyDate: \"22 Oct 2016\",\n" +
        "\t\t\tactArrDate: \"22 Oct 2016\",\n" +
        "\t\t\tactDepDate: \"22 Oct 2016\",\n" +
        "\t\t\tdayDiff: \"-3\",\n" +
        "\t\t\tstoppingStn: true,\n" +
        "\t\t\tdvrtdStn: false,\n" +
        "\t\t\ttravelled: true,\n" +
        "\t\t\tupdWaitngArr: false,\n" +
        "\t\t\tupdWaitngDep: false,\n" +
        "\t\t\tpfNo: 0\n" +
        "\t\t}, {\n" +
        "\t\t\tstnCode: \"PUL\",\n" +
        "\t\t\tidTrnSch: \"3799302\",\n" +
        "\t\t\tactArr: \"21:41\",\n" +
        "\t\t\tactDep: \"21:43\",\n" +
        "\t\t\tdayCnt: 0,\n" +
        "\t\t\tschArrTime: \"21:20\",\n" +
        "\t\t\tschDepTime: \"21:21\",\n" +
        "\t\t\tschDayCnt: 0,\n" +
        "\t\t\tdelayArr: 21,\n" +
        "\t\t\tdelayDep: 22,\n" +
        "\t\t\tarr: true,\n" +
        "\t\t\tdep: true,\n" +
        "\t\t\tdistance: 366,\n" +
        "\t\t\tjourneyDate: \"22 Oct 2016\",\n" +
        "\t\t\tactArrDate: \"22 Oct 2016\",\n" +
        "\t\t\tactDepDate: \"22 Oct 2016\",\n" +
        "\t\t\tdayDiff: \"-3\",\n" +
        "\t\t\tstoppingStn: true,\n" +
        "\t\t\tdvrtdStn: false,\n" +
        "\t\t\ttravelled: true,\n" +
        "\t\t\tupdWaitngArr: false,\n" +
        "\t\t\tupdWaitngDep: false,\n" +
        "\t\t\tpfNo: 0\n" +
        "\t\t}, {\n" +
        "\t\t\tstnCode: \"BCU\",\n" +
        "\t\t\tidTrnSch: \"3799303\",\n" +
        "\t\t\tactArr: \"21:53\",\n" +
        "\t\t\tactDep: \"21:55\",\n" +
        "\t\t\tdayCnt: 0,\n" +
        "\t\t\tschArrTime: \"21:36\",\n" +
        "\t\t\tschDepTime: \"21:37\",\n" +
        "\t\t\tschDayCnt: 0,\n" +
        "\t\t\tdelayArr: 17,\n" +
        "\t\t\tdelayDep: 18,\n" +
        "\t\t\tarr: true,\n" +
        "\t\t\tdep: true,\n" +
        "\t\t\tdistance: 381,\n" +
        "\t\t\tjourneyDate: \"22 Oct 2016\",\n" +
        "\t\t\tactArrDate: \"22 Oct 2016\",\n" +
        "\t\t\tactDepDate: \"22 Oct 2016\",\n" +
        "\t\t\tdayDiff: \"-3\",\n" +
        "\t\t\tstoppingStn: true,\n" +
        "\t\t\tdvrtdStn: false,\n" +
        "\t\t\ttravelled: true,\n" +
        "\t\t\tupdWaitngArr: false,\n" +
        "\t\t\tupdWaitngDep: false,\n" +
        "\t\t\tpfNo: 0\n" +
        "\t\t}, {\n" +
        "\t\t\tstnCode: \"BTI\",\n" +
        "\t\t\tidTrnSch: \"3799304\",\n" +
        "\t\t\tactArr: \"22:10\",\n" +
        "\t\t\tactDep: \"22:20\",\n" +
        "\t\t\tdayCnt: 0,\n" +
        "\t\t\tschArrTime: \"22:10\",\n" +
        "\t\t\tschDepTime: \"22:20\",\n" +
        "\t\t\tschDayCnt: 0,\n" +
        "\t\t\tdelayArr: 0,\n" +
        "\t\t\tdelayDep: 0,\n" +
        "\t\t\tarr: true,\n" +
        "\t\t\tdep: true,\n" +
        "\t\t\tdistance: 398,\n" +
        "\t\t\tjourneyDate: \"22 Oct 2016\",\n" +
        "\t\t\tactArrDate: \"22 Oct 2016\",\n" +
        "\t\t\tactDepDate: \"22 Oct 2016\",\n" +
        "\t\t\tdayDiff: \"-3\",\n" +
        "\t\t\tstoppingStn: true,\n" +
        "\t\t\tdvrtdStn: false,\n" +
        "\t\t\ttravelled: true,\n" +
        "\t\t\tupdWaitngArr: false,\n" +
        "\t\t\tupdWaitngDep: false,\n" +
        "\t\t\tpfNo: 0\n" +
        "\t\t}, {\n" +
        "\t\t\tstnCode: \"GNA\",\n" +
        "\t\t\tidTrnSch: \"3799305\",\n" +
        "\t\t\tactArr: \"22:40\",\n" +
        "\t\t\tactDep: \"22:42\",\n" +
        "\t\t\tdayCnt: 0,\n" +
        "\t\t\tschArrTime: \"22:31\",\n" +
        "\t\t\tschDepTime: \"22:33\",\n" +
        "\t\t\tschDayCnt: 0,\n" +
        "\t\t\tdelayArr: 9,\n" +
        "\t\t\tdelayDep: 9,\n" +
        "\t\t\tarr: true,\n" +
        "\t\t\tdep: true,\n" +
        "\t\t\tdistance: 410,\n" +
        "\t\t\tjourneyDate: \"22 Oct 2016\",\n" +
        "\t\t\tactArrDate: \"22 Oct 2016\",\n" +
        "\t\t\tactDepDate: \"22 Oct 2016\",\n" +
        "\t\t\tdayDiff: \"-3\",\n" +
        "\t\t\tstoppingStn: true,\n" +
        "\t\t\tdvrtdStn: false,\n" +
        "\t\t\ttravelled: true,\n" +
        "\t\t\tupdWaitngArr: false,\n" +
        "\t\t\tupdWaitngDep: false,\n" +
        "\t\t\tpfNo: 0\n" +
        "\t\t}, {\n" +
        "\t\t\tstnCode: \"GJUT\",\n" +
        "\t\t\tidTrnSch: \"3799306\",\n" +
        "\t\t\tactArr: \"23:22\",\n" +
        "\t\t\tactDep: \"23:24\",\n" +
        "\t\t\tdayCnt: 0,\n" +
        "\t\t\tschArrTime: \"22:58\",\n" +
        "\t\t\tschDepTime: \"23:00\",\n" +
        "\t\t\tschDayCnt: 0,\n" +
        "\t\t\tdelayArr: 24,\n" +
        "\t\t\tdelayDep: 24,\n" +
        "\t\t\tarr: true,\n" +
        "\t\t\tdep: true,\n" +
        "\t\t\tdistance: 424,\n" +
        "\t\t\tjourneyDate: \"22 Oct 2016\",\n" +
        "\t\t\tactArrDate: \"22 Oct 2016\",\n" +
        "\t\t\tactDepDate: \"22 Oct 2016\",\n" +
        "\t\t\tdayDiff: \"-3\",\n" +
        "\t\t\tstoppingStn: true,\n" +
        "\t\t\tdvrtdStn: false,\n" +
        "\t\t\ttravelled: true,\n" +
        "\t\t\tupdWaitngArr: false,\n" +
        "\t\t\tupdWaitngDep: false,\n" +
        "\t\t\tpfNo: 0\n" +
        "\t\t}, {\n" +
        "\t\t\tstnCode: \"KKP\",\n" +
        "\t\t\tidTrnSch: \"3799307\",\n" +
        "\t\t\tactArr: \"23:40\",\n" +
        "\t\t\tactDep: \"23:42\",\n" +
        "\t\t\tdayCnt: 0,\n" +
        "\t\t\tschArrTime: \"23:18\",\n" +
        "\t\t\tschDepTime: \"23:20\",\n" +
        "\t\t\tschDayCnt: 0,\n" +
        "\t\t\tdelayArr: 22,\n" +
        "\t\t\tdelayDep: 22,\n" +
        "\t\t\tarr: true,\n" +
        "\t\t\tdep: true,\n" +
        "\t\t\tdistance: 440,\n" +
        "\t\t\tjourneyDate: \"22 Oct 2016\",\n" +
        "\t\t\tactArrDate: \"22 Oct 2016\",\n" +
        "\t\t\tactDepDate: \"22 Oct 2016\",\n" +
        "\t\t\tdayDiff: \"-3\",\n" +
        "\t\t\tstoppingStn: true,\n" +
        "\t\t\tdvrtdStn: false,\n" +
        "\t\t\ttravelled: true,\n" +
        "\t\t\tupdWaitngArr: false,\n" +
        "\t\t\tupdWaitngDep: false,\n" +
        "\t\t\tpfNo: 0\n" +
        "\t\t}, {\n" +
        "\t\t\tstnCode: \"BRW\",\n" +
        "\t\t\tidTrnSch: \"3799308\",\n" +
        "\t\t\tactArr: \"00:05\",\n" +
        "\t\t\tactDep: \"00:20\",\n" +
        "\t\t\tdayCnt: 1,\n" +
        "\t\t\tschArrTime: \"23:35\",\n" +
        "\t\t\tschDepTime: \"23:37\",\n" +
        "\t\t\tschDayCnt: 0,\n" +
        "\t\t\tdelayArr: 30,\n" +
        "\t\t\tdelayDep: 43,\n" +
        "\t\t\tarr: true,\n" +
        "\t\t\tdep: true,\n" +
        "\t\t\tdistance: 458,\n" +
        "\t\t\tjourneyDate: \"22 Oct 2016\",\n" +
        "\t\t\tactArrDate: \"23 Oct 2016\",\n" +
        "\t\t\tactDepDate: \"23 Oct 2016\",\n" +
        "\t\t\tdayDiff: \"-3\",\n" +
        "\t\t\tstoppingStn: true,\n" +
        "\t\t\tdvrtdStn: false,\n" +
        "\t\t\ttravelled: true,\n" +
        "\t\t\tupdWaitngArr: false,\n" +
        "\t\t\tupdWaitngDep: false,\n" +
        "\t\t\tpfNo: 0\n" +
        "\t\t}, {\n" +
        "\t\t\tstnCode: \"MKS\",\n" +
        "\t\t\tidTrnSch: \"3799309\",\n" +
        "\t\t\tactArr: \"00:30\",\n" +
        "\t\t\tactDep: \"00:31\",\n" +
        "\t\t\tdayCnt: 1,\n" +
        "\t\t\tschArrTime: \"23:50\",\n" +
        "\t\t\tschDepTime: \"23:52\",\n" +
        "\t\t\tschDayCnt: 0,\n" +
        "\t\t\tdelayArr: 40,\n" +
        "\t\t\tdelayDep: 39,\n" +
        "\t\t\tarr: true,\n" +
        "\t\t\tdep: true,\n" +
        "\t\t\tdistance: 473,\n" +
        "\t\t\tjourneyDate: \"22 Oct 2016\",\n" +
        "\t\t\tactArrDate: \"23 Oct 2016\",\n" +
        "\t\t\tactDepDate: \"23 Oct 2016\",\n" +
        "\t\t\tdayDiff: \"-3\",\n" +
        "\t\t\tstoppingStn: true,\n" +
        "\t\t\tdvrtdStn: false,\n" +
        "\t\t\ttravelled: true,\n" +
        "\t\t\tupdWaitngArr: false,\n" +
        "\t\t\tupdWaitngDep: false,\n" +
        "\t\t\tpfNo: 0\n" +
        "\t\t}, {\n" +
        "\t\t\tstnCode: \"LKW\",\n" +
        "\t\t\tidTrnSch: \"3799310\",\n" +
        "\t\t\tactArr: \"00:43\",\n" +
        "\t\t\tactDep: \"00:44\",\n" +
        "\t\t\tdayCnt: 1,\n" +
        "\t\t\tschArrTime: \"00:08\",\n" +
        "\t\t\tschDepTime: \"00:10\",\n" +
        "\t\t\tschDayCnt: 1,\n" +
        "\t\t\tdelayArr: 35,\n" +
        "\t\t\tdelayDep: 34,\n" +
        "\t\t\tarr: true,\n" +
        "\t\t\tdep: true,\n" +
        "\t\t\tdistance: 490,\n" +
        "\t\t\tjourneyDate: \"23 Oct 2016\",\n" +
        "\t\t\tactArrDate: \"23 Oct 2016\",\n" +
        "\t\t\tactDepDate: \"23 Oct 2016\",\n" +
        "\t\t\tdayDiff: \"-2\",\n" +
        "\t\t\tstoppingStn: true,\n" +
        "\t\t\tdvrtdStn: false,\n" +
        "\t\t\ttravelled: true,\n" +
        "\t\t\tupdWaitngArr: false,\n" +
        "\t\t\tupdWaitngDep: false,\n" +
        "\t\t\tpfNo: 0\n" +
        "\t\t}, {\n" +
        "\t\t\tstnCode: \"FKA\",\n" +
        "\t\t\tidTrnSch: \"3799311\",\n" +
        "\t\t\tactArr: \"01:15\",\n" +
        "\t\t\tactDep: \"00:00\",\n" +
        "\t\t\tdayCnt: 1,\n" +
        "\t\t\tschArrTime: \"01:00\",\n" +
        "\t\t\tschDepTime: \"00:00\",\n" +
        "\t\t\tschDayCnt: 1,\n" +
        "\t\t\tdelayArr: 15,\n" +
        "\t\t\tdelayDep: 0,\n" +
        "\t\t\tarr: true,\n" +
        "\t\t\tdep: false,\n" +
        "\t\t\tdistance: 521,\n" +
        "\t\t\tjourneyDate: \"23 Oct 2016\",\n" +
        "\t\t\tactArrDate: \"23 Oct 2016\",\n" +
        "\t\t\tactDepDate: \"\",\n" +
        "\t\t\tdayDiff: \"-2\",\n" +
        "\t\t\tstoppingStn: true,\n" +
        "\t\t\tdvrtdStn: false,\n" +
        "\t\t\ttravelled: true,\n" +
        "\t\t\tupdWaitngArr: false,\n" +
        "\t\t\tupdWaitngDep: false,\n" +
        "\t\t\tpfNo: 0\n" +
        "\t\t}],\n" +
        "\t\ttotalLateMins: 15,\n" +
        "\t\tisRunningDataAvailable: true\n" +
        "\t}]\n" +
        "}";
    List<String> keys = Arrays.asList("status", "spottrain", "data->trainDataFound");

    System.out.println("Values for keys: " + keys + ":");
    for (Map.Entry o : getValuesForFlattenedKeys(json1, keys, Arrays.asList("d->d1", "d->d1->db")).entrySet()) {
      System.out.println(o.getKey() + " :: " + o.getValue());
    }

    System.out.println(isValidJson("{\"sn\":\"{\\\"data\\\":{\\\"id\\\":\\\"rimal@gmail.com\\\",\\\"session\\\":\\\"session1\\\"}}\"}"));
  }*/

}