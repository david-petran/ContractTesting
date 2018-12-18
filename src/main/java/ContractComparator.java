package support;

import gherkin.deps.com.google.gson.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class ContractComparator {

    private boolean jsonValid;
    private List<String> differences;

    public ContractComparator(String response, JsonObject contract){
        JsonParser parser = new JsonParser();
        JsonObject responseAsJson = parser.parse(response).getAsJsonObject();

        differences = new ArrayList<>();
        jsonValid = jsonObjectMatches(responseAsJson, contract, "root");
    }

    public boolean isJsonValid(){
        return jsonValid;
    }

    public List<String> getDifferences() {
        return differences;
    }

    private boolean jsonObjectMatches(JsonObject responseObject, JsonObject contractObject, String name){
        boolean result = true;
        for(Map.Entry<String, JsonElement> contractEntry: contractObject.entrySet()) {
            if(responseObject.has(contractEntry.getKey())){
                JsonElement responseEntry = responseObject.get(contractEntry.getKey());
                result = result && jsonMatches(responseEntry, contractEntry.getValue(), name+"."+contractEntry.getKey());
            } else {
                result = false;
                differences.add("The contract requires an object \"" + contractEntry.getKey() + "\" with type: "+ contractEntry.getValue().toString() + " but it was not present in the response");
            }
        }
        return result;
    }

    private boolean jsonArrayMatches(JsonArray responseArray, JsonArray contractArray, String name){
        boolean result = true;
        int count = 0;
        for(Iterator<JsonElement> responseArrayObject= responseArray.iterator(); responseArrayObject.hasNext();){
            JsonObject contractMask = (JsonObject)contractArray.get(0);
            JsonObject responseArrayObjectObject = (JsonObject)responseArrayObject.next();
            boolean matches = jsonMatches(responseArrayObjectObject, contractMask, name+"["+count+"]");
            result = result && matches;
            if(!matches){
                differences.add("The contract requires all objects in array \"" + name + "\" comply with " + contractArray.get(0).toString() + ", but was: " + responseArrayObjectObject.toString());
            }
            count++;
        }
        return result;
    }

    private boolean jsonElementMatches(JsonElement responseElement, JsonElement contractElement, String name){
        String allowedType = contractElement.getAsString();
        boolean result =true;

        switch(allowedType) {
            case "STRING":
                if(!((JsonPrimitive)responseElement).isString()){
                    result = false;
                }
            break;
            case "INTEGER":
                if(!((JsonPrimitive)responseElement).isNumber()){
                    result = false;
                }
            break;
            case "BOOLEAN":
                if(!((JsonPrimitive)responseElement).isBoolean()){
                    result = false;
                }
            break;
            default:
                result = false;
            break;
        }
        if(!result){
            String type = "null";
            if(((JsonPrimitive)responseElement).isNumber()){
                type = "numeric";
            } else if (((JsonPrimitive)responseElement).isString()){
                type = "string";
            } else if (((JsonPrimitive)responseElement).isBoolean()){
                type = "boolean";
            }
            differences.add("The contract requires the element \"" + name + "\" has type "+ allowedType +" but the response element was "+ type + ": " + responseElement);
        }

        return result;
    }

    private boolean jsonMatches(JsonElement responseElement, JsonElement contractElement, String name){
        boolean isValid;
        if(contractElement instanceof JsonObject && responseElement instanceof JsonObject){
            isValid = jsonObjectMatches((JsonObject) responseElement, (JsonObject) contractElement, name);
        } else if(contractElement instanceof JsonArray && responseElement instanceof JsonArray){
            isValid = jsonArrayMatches((JsonArray) responseElement, (JsonArray) contractElement, name);
        } else {
            isValid = jsonElementMatches(responseElement, contractElement, name);
        }

        return isValid;
    }


}
